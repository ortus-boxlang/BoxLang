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
package ortus.boxlang.runtime.types.immutable;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

/**
 * This type represents a representation of a database query result set.
 * It provides language specific methods to access columnar data, both as value lists and within iterative loops
 */
public class ImmutableQuery extends Query {

	/**
	 * Serialization version
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new query with additional metadata
	 *
	 * @param meta Struct of metadata, most likely JDBC metadata such as sql, cache parameters, etc.
	 */
	public ImmutableQuery( IStruct meta ) {
		super( meta );
	}

	/**
	 * Create a new query with a default (empty) metadata struct
	 */
	public ImmutableQuery() {
		this( new Struct( IStruct.TYPES.SORTED ) );
	}

	public ImmutableQuery( Query query ) {
		this();
		// add columns
		for ( Map.Entry<Key, QueryColumn> columnInfo : query.getColumns().entrySet() ) {
			super.addColumn( columnInfo.getValue().getName(), columnInfo.getValue().getType(), null );
		}
		// then copy data
		for ( Object[] row : query.getData() ) {
			Object[] duplicatedRow = row.clone();
			super.addRow( duplicatedRow );
		}
	}

	/**
	 * Create a new query and populate it from the given JDBC ResultSet.
	 *
	 * @param resultSet JDBC result set.
	 *
	 * @return Query object
	 */
	public static ImmutableQuery fromResultSet( ResultSet resultSet ) {
		return Query.fromResultSet( resultSet ).toImmutable();
	}

	/**
	 * Create a new query with columns and data
	 *
	 * @param columnNames List of column names
	 * @param columnTypes List of column types
	 * @param rowData     List of row data
	 *
	 * @return Query object
	 */
	public static ImmutableQuery fromArray( Array columnNames, Array columnTypes, Object rowData ) {
		return Query.fromArray( columnNames, columnTypes, rowData ).toImmutable();
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
	public ImmutableQuery addColumn( Key name, QueryColumnType type, Object[] columnData ) {
		throw new UnmodifiableException( "Cannot add columns to an ImmutableQuery" );
	}

	/**
	 * Abstraction for creating a new column so we can re-use logic easier between normal and immutable queries
	 * 
	 * @param name  column name
	 * @param type  column type
	 * @param index column index
	 * 
	 * @return QueryColumn object
	 */
	protected QueryColumn createQueryColumn( Key name, QueryColumnType type, int index ) {
		return new ImmutableQueryColumn( name, type, this, index );
	}

	/**
	 * Insert a query into this query at a specific position
	 *
	 * @param position position to insert at
	 * @param target   query to insert
	 *
	 * @throws BoxRuntimeException if the query columns do not match
	 *
	 * @return this query
	 */
	public ImmutableQuery insertQueryAt( int position, Query target ) {
		throw new UnmodifiableException( "Cannot insert queries into an ImmutableQuery" );
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as array of objects
	 *
	 * @return this query
	 */
	public int addRow( Object[] row ) {
		throw new UnmodifiableException( "Cannot add rows to an ImmutableQuery" );
	}

	/**
	 * Swap a row with another row in the query
	 *
	 * @param sourceRow      The row to swap from
	 * @param destinationRow The row to swap to
	 *
	 * @return this query
	 */
	public ImmutableQuery swapRow( int sourceRow, int destinationRow ) {
		throw new UnmodifiableException( "Cannot swap rows in an ImmutableQuery" );
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as Struct
	 *
	 * @return this query
	 */
	public int addRow( IStruct row ) {
		throw new UnmodifiableException( "Cannot add rows to an ImmutableQuery" );
	}

	/**
	 * Add empty rows to the query
	 *
	 * @param rows Number of rows to add
	 *
	 * @return Last row added
	 */
	public int addRows( int rows ) {
		throw new UnmodifiableException( "Cannot add rows to an ImmutableQuery" );
	}

	/**
	 * Deletes a column from the query.
	 *
	 * @param name the name of the column to delete
	 */
	public void deleteColumn( Key name ) {
		throw new UnmodifiableException( "Cannot delete columns from an ImmutableQuery" );
	}

	/**
	 * Delete a row from the query
	 *
	 * @param index row index, starting at 0
	 *
	 * @return this query
	 */
	public ImmutableQuery deleteRow( int index ) {
		throw new UnmodifiableException( "Cannot delete rows from an ImmutableQuery" );
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
		throw new UnmodifiableException( "Cannot add data to an ImmutableQuery" );
	}

	/**
	 * Set data for a single cell. 0-based index!
	 *
	 * @param columnName column name
	 * @param rowIndex   row index, starting at 0
	 *
	 * @return this query
	 */
	public ImmutableQuery setCell( Key columnName, int rowIndex, Object value ) {
		throw new UnmodifiableException( "Cannot set cells in an ImmutableQuery" );
	}

	/**
	 * Sort the query
	 *
	 * @param compareFunc function to use for sorting
	 */
	public void sort( Comparator<IStruct> compareFunc ) {
		throw new UnmodifiableException( "Cannot sort an ImmutableQuery" );
	}

	/***************************
	 * Collection implementation
	 ****************************/

	@Override
	public boolean add( IStruct row ) {
		throw new UnmodifiableException( "Cannot add rows to an ImmutableQuery" );
	}

	@Override
	public boolean remove( Object o ) {
		throw new UnmodifiableException( "Cannot remove rows from an ImmutableQuery" );
	}

	@Override
	public boolean addAll( Collection<? extends IStruct> rows ) {
		throw new UnmodifiableException( "Cannot add rows to an ImmutableQuery" );
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot remove rows from an ImmutableQuery" );
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot retain rows in an ImmutableQuery" );
	}

	@Override
	public void clear() {
		throw new UnmodifiableException( "Cannot clear an ImmutableQuery" );
	}

	/***************************
	 * IReferencable implementation
	 ****************************/

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new UnmodifiableException( "Cannot assign to an ImmutableQuery" );
	}

	/**
	 * Duplicate the current query.
	 *
	 * @return A copy of the current query.
	 */
	public ImmutableQuery duplicate() {
		return super.duplicate( false ).toImmutable();
	}

}
