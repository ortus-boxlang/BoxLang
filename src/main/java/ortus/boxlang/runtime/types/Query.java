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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.interop.DynamicJavaInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

/**
 * This class represents a query.
 */
public class Query implements IType, IReferenceable, Collection<Struct> {

	/**
	 * Query data as List of arrays
	 */
	private List<Object[]>			data	= Collections.synchronizedList( new ArrayList<Object[]>() );

	/**
	 * Map of column definitions
	 */
	private Map<Key, QueryColumn>	columns	= Collections.synchronizedMap( new LinkedHashMap<Key, QueryColumn>() );
	/**
	 * Metadata object
	 */
	public BoxMeta					$bx;

	/**
	 * Function service
	 */
	private FunctionService			functionService;

	/**
	 * Create a new query
	 */
	Query() {
		functionService = BoxRuntime.getInstance().getFunctionService();
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
	 * This method is really only for debugging and the underlying List you get will not be syncronized with the query.
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
	 * Add a column to the query, populated with provided data. If the data array is shorter than the current number of rows, the remaining rows will be
	 * populated with nulls.
	 * 
	 * @param name column name
	 * @param type column type
	 * 
	 * @return this query
	 */
	public synchronized Query addColumn( Key name, QueryColumnType type, Object[] columnData ) {
		columns.put( name, new QueryColumn( name, type, this, getColumns().size() ) );
		// loop over data and replace each array with a new array having an additional null at the end
		for ( int i = 0; i < data.size(); i++ ) {
			Object[]	row		= data.get( i );
			Object[]	newRow	= new Object[ row.length + 1 ];
			System.arraycopy( row, 0, newRow, 0, row.length );
			if ( columnData != null && i < columnData.length ) {
				newRow[ newRow.length - 1 ] = columnData[ i ];
			}
			data.set( i, newRow );
		}
		return this;
	}

	/**
	 * Get all data in a column as a Java Object[]
	 * Data is copied, so re-assignments into the array will not be reflected in the query.
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
	 * Data is copied, so re-assignments into the array will not be reflected in the query.
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
	 * Array is passed by reference and changes made to it will be reflected in the query.
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
	 * @param row row data as Struct
	 * 
	 * @return this query
	 */
	public int addRow( Struct row ) {
		Object[]	rowData	= new Object[ columns.size() ];
		// TODO: Check for missing columns?
		// TODO: validate types
		int			i		= 0;
		for ( QueryColumn column : columns.values() ) {
			rowData[ i ] = row.get( column.getName() );
			i++;
		}
		return addRow( rowData );
	}

	/**
	 * Get data for a row as a Struct. 0-based index!
	 * Data is copied, so re-assignments into the struct will not be reflected in the query.
	 * Mutating a complex object in the array will be reflected in the query.
	 * 
	 * @param index row index, starting at 0
	 * 
	 * @return array of row data
	 */
	public Struct getRowAsStruct( int index ) {
		validateRow( index );
		Struct		struct	= new Struct();
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
	public Iterator<Struct> iterator() {
		// TODO: Thread safe?
		return new Iterator<Struct>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < data.size();
			}

			@Override
			public Struct next() {
				return getRowAsStruct( index++ );
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

	@Override
	public boolean add( Struct row ) {
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
	public boolean addAll( Collection<? extends Struct> rows ) {
		for ( Struct row : rows ) {
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
	public Object dereference( Key name, Boolean safe ) {

		// Special check for $bx
		if ( name.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		if ( name.equals( Key.recordCount ) ) {
			return size();
		}
		if ( name.equals( Key.columnList ) ) {
			return getColumns().keySet().stream().map( Key::getName ).collect( Collectors.joining( "," ) );
		}
		// TODO: Get this from context based on if in cfloop/cfoutput query="..."
		if ( name.equals( Key.currentRow ) ) {
			return 1;
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

		return DynamicJavaInteropService.invoke( this, name.getName(), safe, positionalArguments );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.QUERY );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicJavaInteropService.invoke( this, name.getName(), safe, namedArguments );
	}

	@Override
	public Object assign( Key name, Object value ) {
		// TODO: get row index from context based on if in cfloop/cfoutput query="..."
		getColumn( name ).setCell( 0, value );
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
			// TODO: Create query metadata class
			// getMetaData() in CF returns array of query column metadata objects
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;
	}

}
