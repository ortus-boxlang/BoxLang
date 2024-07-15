/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.types;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.QueryMeta;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.DuplicationUtil;

/**
 * This type represents a representation of a database query result set.
 * It provides language specific methods to access columnar data, both as value lists and within iterative loops
 */
public class Query implements IType, IReferenceable, Collection<IStruct>, Serializable {

	/**
	 * Query data as List of arrays
	 */
	private List<Object[]>				data				= Collections.synchronizedList( new ArrayList<Object[]>() );

	/**
	 * Map of column definitions
	 */
	private Map<Key, QueryColumn>		columns				= Collections.synchronizedMap( new LinkedHashMap<Key, QueryColumn>() );
	/**
	 * Metadata object
	 */
	public transient BoxMeta			$bx;

	/**
	 * Function service
	 */
	private transient FunctionService	functionService;

	/**
	 * Serialization version
	 */
	private static final long			serialVersionUID	= 1L;

	/**
	 * Metadata for the query, used to populate QueryMeta
	 */
	private IStruct						metadata;

	/**
	 * Create a new query with additional metadata
	 * 
	 * @param meta Struct of metadata, most likely JDBC metadata such as sql, cache parameters, etc.
	 */
	public Query( IStruct meta ) {
		this.functionService	= BoxRuntime.getInstance().getFunctionService();
		this.metadata			= meta == null ? new Struct( IStruct.TYPES.SORTED ) : meta;
	}

	/**
	 * Create a new query with a default (empty) metadata struct
	 */
	public Query() {
		this( new Struct( IStruct.TYPES.SORTED ) );
	}

	public static Query fromResultSet( ResultSet resultSet ) {
		return fromResultSet( resultSet, -1 );
	}

	public static Query fromResultSet( ResultSet resultSet, int maxRows ) {
		return fromResultSet( resultSet, maxRows, null );
	}

	public static Query fromResultSet( ResultSet resultSet, IStruct metadata ) {
		return fromResultSet( resultSet, -1, metadata );
	}

	public static Query fromResultSet( ResultSet resultSet, int maxRows, IStruct metadata ) {
		Query query = new Query( metadata );

		if ( resultSet == null ) {
			return query;
		}

		try {
			ResultSetMetaData	resultSetMetaData	= resultSet.getMetaData();
			int					columnCount			= resultSetMetaData.getColumnCount();

			// The column count starts from 1
			for ( int i = 1; i <= columnCount; i++ ) {
				query.addColumn(
				    Key.of( resultSetMetaData.getColumnLabel( i ) ),
				    QueryColumnType.fromSQLType( resultSetMetaData.getColumnType( i ) ) );
			}

			int rowCount = 0;
			while ( resultSet.next() ) {
				Object[] row = new Object[ columnCount ];
				for ( int i = 1; i <= columnCount; i++ ) {
					row[ i - 1 ] = resultSet.getObject( i );
				}
				query.addRow( row );

				rowCount++;
				if ( rowCount == maxRows ) {
					break;
				}
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}

		return query;
	}

	/*
	 * Create a new query with columns and data
	 */
	public static Query fromArray( Array columnNames, Array columnTypes, Object rowData ) {
		Query	q	= new Query();
		int		i	= 0;
		for ( var columnName : columnNames ) {
			q.addColumn( Key.of( columnName ), QueryColumnType.fromString( ( String ) columnTypes.get( i ) ) );
			i++;
		}
		if ( rowData == null ) {
			return q;
		}
		q.addData( rowData );
		return q;
	}

	/**
	 * Override Query metadata - used for setting custom query meta on cached queries.
	 */
	public Query setMetadata( IStruct meta ) {
		this.metadata	= meta;
		this.$bx		= null;
		return this;
	}

	/**
	 * Get the list of column definitions for this query
	 *
	 * @return map of columns
	 */
	public Map<Key, QueryColumn> getColumns() {
		return columns;
	}

	/**
	 * Does this query have columns?
	 *
	 * @return true if query has columns
	 */
	public boolean hasColumns() {
		return !columns.isEmpty();
	}

	/**
	 * Does this query have a specific column?
	 *
	 * @return true if query has column
	 */
	public boolean hasColumn( Key name ) {
		return columns.containsKey( name );
	}

	/**
	 * Get the data for this query
	 * This method is really only for debugging and the underlying List you get will
	 * not be synchronized with the query.
	 *
	 * @return list of arrays of data
	 */
	public List<Object[]> getData() {
		return data;
	}

	/**
	 * Add a column to the query, populated with nulls
	 *
	 * @param name column name
	 * @param type column type
	 *
	 * @return this query
	 */
	public Query addColumn( Key name, QueryColumnType type ) {
		return addColumn( name, type, null );
	}

	/**
	 * Add a column to the query, populated with provided data. If the data array is
	 * shorter than the current number of rows, the remaining rows will be
	 * populated with nulls.
	 *
	 * @param name column name
	 * @param type column type
	 *
	 * @return this query
	 */
	public synchronized Query addColumn( Key name, QueryColumnType type, Object[] columnData ) {
		// check if column name already exists
		int	index		= -1;
		int	newColIndex	= getColumns().size();
		// Get index from linked map of where the key exists already
		for ( Key key : columns.keySet() ) {
			index++;
			if ( key.equals( name ) ) {
				newColIndex = index;
				break;
			}
		}
		columns.put( name, new QueryColumn( name, type, this, newColIndex ) );
		if ( !data.isEmpty() ) {
			// loop over data and replace each array with a new array having an additional
			// null at the end
			for ( int i = 0; i < data.size(); i++ ) {
				Object[]	row		= data.get( i );
				Object[]	newRow	= new Object[ row.length + 1 ];
				System.arraycopy( row, 0, newRow, 0, row.length );
				if ( columnData != null && i < columnData.length ) {
					newRow[ newColIndex ] = columnData[ i ];
				}
				data.set( i, newRow );
			}
		} else if ( columnData != null ) {
			// loop over column data and add that many rows with an array as big as their
			// are columns
			for ( Object columnDatum : columnData ) {
				Object[] row = new Object[ columns.size() ];
				row[ newColIndex ] = columnDatum;
				data.add( row );
			}
		}
		return this;
	}

	/**
	 * Get all data in a column as a Java Object[]
	 * Data is copied, so re-assignments into the array will not be reflected in the
	 * query.
	 * Mutating a complex object in the array will be reflected in the query.
	 *
	 * @param name column name
	 *
	 * @return array of column data
	 */
	public Object[] getColumnData( Key name ) {
		int			index		= getColumn( name ).getIndex();
		Object[]	columnData	= new Object[ data.size() ];
		for ( int i = 0; i < data.size(); i++ ) {
			columnData[ i ] = data.get( i )[ index ];
		}
		return columnData;
	}

	/**
	 * Get all data in a column as an BoxLang Array
	 * Data is copied, so re-assignments into the array will not be reflected in the
	 * query.
	 * Mutating a complex object in the array will be reflected in the query.
	 *
	 * @param name column name
	 *
	 * @return array of column data
	 */
	public Array getColumnDataAsArray( Key name ) {
		return Array.fromArray( getColumnData( name ) );
	}

	/**
	 * Get the index of a column
	 *
	 * @param name column name
	 *
	 * @return index of column, or -1 if not found
	 */
	public int getColumnIndex( Key name ) {
		int index = 0;
		for ( QueryColumn column : columns.values() ) {
			if ( column.getName().equals( name ) ) {
				return index;
			}
			index++;
		}
		return -1;
	}

	/**
	 * Get the QueryColumn object for a column
	 * Throws an exception if the column doesn't exist
	 *
	 * @param name column name
	 *
	 * @return QueryColumn object
	 */
	public QueryColumn getColumn( Key name ) {
		QueryColumn column = columns.get( name );
		if ( column == null ) {
			throw new BoxRuntimeException( "Column '" + name + "' does not exist in query" );
		}
		return column;
	}

	/**
	 * Get data for a row as an array. 0-based index!
	 * Array is passed by reference and changes made to it will be reflected in the
	 * query.
	 *
	 * @param index row index, starting at 0
	 *
	 * @return array of row data
	 */
	public Object[] getRow( int index ) {
		validateRow( index );
		return data.get( index );
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as array of objects
	 *
	 * @return this query
	 */
	public int addRow( Object[] row ) {
		// TODO: validate types
		int newRow;
		synchronized ( this ) {
			data.add( row );
			newRow = data.size();
		}
		return newRow;
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as a BoxLang array
	 *
	 * @return this query
	 */
	public int addRow( Array row ) {
		return addRow( row.toArray() );
	}

	/**
	 * Add an empty row to the query
	 *
	 * @return this query
	 */
	public int addEmptyRow() {
		return addRow( columns.keySet().stream().map( key -> null ).toArray() );
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as Struct
	 *
	 * @return this query
	 */
	public int addRow( IStruct row ) {
		Object[]	rowData	= new Object[ columns.size() ];
		// TODO: validate types
		int			i		= 0;
		Object		o;
		for ( QueryColumn column : columns.values() ) {
			// Missing keys in the struct go in the query as an empty string (CF compat)
			rowData[ i ] = ( o = row.get( column.getName() ) ) == null ? "" : o;
			i++;
		}
		// We're ignoring extra keys in the struct that aren't query columns. Lucee
		// compat, but not CF compat.
		return addRow( rowData );
	}

	/**
	 * Add empty rows to the query
	 *
	 * @param rows Number of rows to add
	 *
	 * @return Last row added
	 */
	public int addRows( int rows ) {
		int lastRow = 0;
		for ( int i = 0; i < rows; i++ ) {
			lastRow = addRow( ( Object[] ) new Object[ columns.size() ] );
		}
		return lastRow;
	}

	/**
	 * Deletes a column from the query.
	 *
	 * @param name the name of the column to delete
	 */
	public void deleteColumn( Key name ) {
		QueryColumn	column	= getColumn( name );
		int			index	= column.getIndex();
		columns.remove( name );
		for ( Object[] row : data ) {
			Object[] newRow = new Object[ row.length - 1 ];
			System.arraycopy( row, 0, newRow, 0, index );
			System.arraycopy( row, index + 1, newRow, index, row.length - index - 1 );
			row = newRow;
		}
	}

	/*
	 * Delete a row from the query
	 *
	 * @param index row index, starting at 0
	 *
	 * @return this query
	 */
	public Query deleteRow( int index ) {
		validateRow( index );
		data.remove( index );
		return this;
	}

	/**
	 * Helper method for queryNew() and queryAddRow() to handle the different
	 * scenarios for adding data to a query
	 *
	 * @param rowData Data to populate the query. Can be a struct (with keys
	 *                matching column names), an array of structs, or an array of
	 *                arrays (in
	 *                same order as columnList)
	 *
	 * @return index of last row added
	 */
	public int addData( Object rowData ) {
		CastAttempt<IStruct> structCastAttempt = StructCaster.attempt( rowData );
		// Add a single row as a struct
		if ( structCastAttempt.wasSuccessful() ) {
			return addRow( structCastAttempt.get() );
		}
		// Add multiple rows as an array of structs
		CastAttempt<Array> arrayCastAttempt = ArrayCaster.attempt( rowData );
		if ( arrayCastAttempt.wasSuccessful() ) {
			Array arrData = arrayCastAttempt.get();
			if ( arrData.isEmpty() ) {
				return 0;
			}
			// Test the first row to see if we have an array of arrays or an array of
			// structs
			Boolean	isArray		= ArrayCaster.attempt( arrData.getFirst() ).wasSuccessful();
			Boolean	isStruct	= StructCaster.attempt( arrData.getFirst() ).wasSuccessful();
			if ( isArray || isStruct ) {
				int lastRow = 0;
				for ( Object row : arrData ) {
					if ( isArray ) {
						// Will throw if the first row is an array, but the rest are not
						lastRow = addRow( ArrayCaster.cast( row ) );
					} else {
						// Will throw if the first row is an struct, but the rest are not
						lastRow = addRow( StructCaster.cast( row ) );
					}
				}
				return lastRow;
			} else {
				// A single array of simple values to be set into the cells of the first row
				return addRow( arrData );
			}
		}
		throw new BoxRuntimeException(
		    "rowData must be a struct, an array of structs, or an array of arrays.  " + rowData.getClass().getName()
		        + " was passed." );
	}

	/**
	 * Get data for a row as a Struct. 0-based index!
	 * Data is copied, so re-assignments into the struct will not be reflected in
	 * the query.
	 * Mutating a complex object in the array will be reflected in the query.
	 *
	 * @param index row index, starting at 0
	 *
	 * @return array of row data
	 */
	public IStruct getRowAsStruct( int index ) {
		validateRow( index );
		IStruct		struct	= new Struct( IStruct.TYPES.LINKED );
		Object[]	row		= data.get( index );
		int			i		= 0;
		for ( QueryColumn column : columns.values() ) {
			struct.put( column.getName(), row[ i ] );
			i++;
		}
		return struct;
	}

	/**
	 * Get data for a single cell. 0-based index!
	 *
	 * @param columnName column name
	 * @param rowIndex   row index, starting at 0
	 *
	 * @return cell data
	 */
	public Object getCell( Key columnName, int rowIndex ) {
		validateRow( rowIndex );
		int columnIndex = getColumn( columnName ).getIndex();
		return data.get( rowIndex )[ columnIndex ];
	}

	/**
	 * Set data for a single cell. 0-based index!
	 *
	 * @param columnName column name
	 * @param rowIndex   row index, starting at 0
	 *
	 * @return this query
	 */
	public Query setCell( Key columnName, int rowIndex, Object value ) {
		validateRow( rowIndex );
		int columnIndex = getColumn( columnName ).getIndex();
		// TODO: validate column type
		data.get( rowIndex )[ columnIndex ] = value;
		return this;
	}

	/**
	 * Validate that a row index is within bounds
	 * Throw exception if not
	 *
	 * @param index row index, 0-based
	 */
	public void validateRow( int index ) {
		if ( index < 0 || index >= data.size() ) {
			throw new BoxRuntimeException( "Row index " + index + " is out of bounds for query of size " + data.size() );
		}
	}

	/**
	 * Validate that a row index is within bounds
	 * Throw exception if not
	 *
	 * @param context context to get row from
	 *
	 * @return row index, 0-based
	 */
	public int getRowFromContext( IBoxContext context ) {
		return context.getQueryRow( this );
	}

	/**
	 * Get the list of column names as a comma-separated string
	 * TODO: Look into caching this and invalidating when columns are added/removed
	 *
	 * @return column names as string
	 */
	public String getColumnList() {
		return getColumns().keySet().stream().map( Key::getName ).collect( Collectors.joining( "," ) );
	}

	/**
	 * Get the list of column names as an array
	 *
	 * @return column names as array
	 */
	public Array getColumnArray() {
		return getColumns().keySet().stream().map( Key::getName ).collect( BLCollector.toArray() );
	}

	/**
	 * Sort the query
	 *
	 * @param compareFunc function to use for sorting
	 */
	public void sort( Comparator<IStruct> compareFunc ) {
		// data.sort( compareFunc );
		Stream<IStruct> sorted = intStream()
		    .mapToObj( index -> getRowAsStruct( index ) )
		    .sorted( compareFunc );

		data = sorted.map( row -> row.getWrapped().entrySet().stream().map( entry -> entry.getValue() ).toArray() )
		    .collect( Collectors.toList() );
	}

	/***************************
	 * Collection implementation
	 ****************************/
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public boolean contains( Object o ) {
		return data.contains( o );
	}

	@Override
	public Iterator<IStruct> iterator() {
		// TODO: Thread safe?
		return new Iterator<IStruct>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < data.size();
			}

			@Override
			public IStruct next() {
				IStruct rowData = getRowAsStruct( index );
				index++;
				return rowData;
			}
		};
	}

	@Override
	public Object[] toArray() {
		return data.toArray();
	}

	@Override
	public <T> T[] toArray( T[] a ) {
		return data.toArray( a );
	}

	/**
	 * Get the data as a Boxlang Array of Structs. Useful for queries with
	 * `returntype: "array"`.
	 */
	public Array toStructArray() {
		Iterator<IStruct>	it			= iterator();
		Array				structArray	= new Array();
		while ( it.hasNext() ) {
			structArray.add( it.next() );
		}
		return structArray;
	}

	@Override
	public boolean add( IStruct row ) {
		addRow( row );
		return true;
	}

	@Override
	public boolean remove( Object o ) {
		return data.remove( o );
	}

	@Override
	public boolean containsAll( Collection<?> c ) {
		return data.containsAll( c );
	}

	@Override
	public boolean addAll( Collection<? extends IStruct> rows ) {
		for ( IStruct row : rows ) {
			addRow( row );
		}
		return true;
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		return data.removeAll( c );
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		return data.retainAll( c );
	}

	@Override
	public void clear() {
		data.clear();
	}

	/***************************
	 * IReferencable implementation
	 ****************************/

	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {

		// Special check for $bx
		if ( name.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		if ( name.equals( Key.recordCount ) ) {
			return size();
		}
		if ( name.equals( Key.columnList ) ) {
			return getColumnList();
		}
		if ( name.equals( Key.currentRow ) ) {
			return getRowFromContext( context ) + 1;
		}
		if ( !hasColumn( name ) && safe ) {
			return null;
		}
		// qry.col returns a column reference
		return getColumn( name );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.QUERY );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.QUERY );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		getColumn( name ).setCell( getRowFromContext( context ), value );
		return value;
	}

	/***************************
	 * IType implementation
	 ****************************/

	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "[\n" );
		for ( int i = 0; i < data.size(); i++ ) {
			if ( i > 0 ) {
				sb.append( ",\n" );
			}
			sb.append( "  " );
			sb.append( getRowAsStruct( i ).asString() );
		}
		sb.append( "\n]" );
		return sb.toString();
	}

	@Override
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new QueryMeta( this );
		}
		return this.$bx;
	}

	/*
	 * Returns a IntStream of the indexes
	 */
	public IntStream intStream() {
		return IntStream.range( 0, data.size() );
	}

	/**
	 * Retrieve query metadata as a struct. Used to populate QueryMeta.
	 * 
	 * Will populate the following keys if they don't already exist:
	 * - recordCount: Number of rows in the query
	 * - columns: List of column names
	 * - _HASHCODE: Hashcode of the query
	 * 
	 * @return The metadata as a struct
	 */
	public IStruct getMetaData() {
		this.metadata.computeIfAbsent( Key.recordCount, key -> {
			return data.size();
		} );
		this.metadata.computeIfAbsent( Key.columns, key -> {
			return this.getColumns();
		} );
		this.metadata.computeIfAbsent( Key.columnList, key -> {
			return this.getColumnList();
		} );
		this.metadata.computeIfAbsent( Key._HASHCODE, key -> {
			return this.hashCode();
		} );
		return this.metadata;
	}

	/**
	 * Duplicate the current query.
	 *
	 * @return A copy of the current query.
	 */
	public Query duplicate() {
		return duplicate( false );
	}

	/**
	 * Duplicate the current query.
	 *
	 * @param deep If true, nested objects will be duplicated as well.
	 *
	 * @return A copy of the current query.
	 */
	public Query duplicate( boolean deep ) {
		Query q = new Query();

		this.getColumns().entrySet().stream().forEach( entry -> {
			q.addColumn( entry.getKey(), entry.getValue().getType() );
		} );

		if ( deep ) {
			q.addData( DuplicationUtil.duplicate( this.getData(), deep ) );
		} else {
			q.addData( this.getData() );
		}
		return q;
	}

}
