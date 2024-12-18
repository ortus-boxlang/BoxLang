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
package ortus.boxlang.runtime.types.unmodifiable;

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
public class UnmodifiableQuery extends Query implements IUnmodifiable {

	/**
	 * Serialization version
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new query with additional metadata
	 *
	 * @param meta Struct of metadata, most likely JDBC metadata such as sql, cache parameters, etc.
	 */
	public UnmodifiableQuery( IStruct meta ) {
		super( meta );
	}

	/**
	 * Create a new query with a default (empty) metadata struct
	 */
	public UnmodifiableQuery() {
		this( new Struct( IStruct.TYPES.SORTED ) );
	}

	/**
	 * Create an Unmodifiable query from a Modifiable query
	 *
	 * @param query The Modifiable query to convert
	 */
	public UnmodifiableQuery( Query query ) {
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
	public static UnmodifiableQuery fromResultSet( ResultSet resultSet ) {
		return Query.fromResultSet( resultSet ).toUnmodifiable();
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
	public static UnmodifiableQuery fromArray( Array columnNames, Array columnTypes, Object rowData ) {
		return Query.fromArray( columnNames, columnTypes, rowData ).toUnmodifiable();
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
	@Override
	public UnmodifiableQuery addColumn( Key name, QueryColumnType type, Object[] columnData ) {
		throw new UnmodifiableException( "Cannot add columns to an UnmodifiableQuery" );
	}

	/**
	 * Abstraction for creating a new column so we can re-use logic easier between normal and Unmodifiable queries
	 *
	 * @param name  column name
	 * @param type  column type
	 * @param index column index
	 *
	 * @return QueryColumn object
	 */
	@Override
	protected QueryColumn createQueryColumn( Key name, QueryColumnType type, int index ) {
		return new UnmodifiableQueryColumn( name, type, this, index );
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
	@Override
	public UnmodifiableQuery insertQueryAt( int position, Query target ) {
		throw new UnmodifiableException( "Cannot insert queries into an UnmodifiableQuery" );
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as array of objects
	 *
	 * @return this query
	 */
	@Override
	public int addRow( Object[] row ) {
		throw new UnmodifiableException( "Cannot add rows to an UnmodifiableQuery" );
	}

	/**
	 * Swap a row with another row in the query
	 *
	 * @param sourceRow      The row to swap from
	 * @param destinationRow The row to swap to
	 *
	 * @return this query
	 */
	@Override
	public UnmodifiableQuery swapRow( int sourceRow, int destinationRow ) {
		throw new UnmodifiableException( "Cannot swap rows in an UnmodifiableQuery" );
	}

	/**
	 * Add a row to the query
	 *
	 * @param row row data as Struct
	 *
	 * @return this query
	 */
	@Override
	public int addRow( IStruct row ) {
		throw new UnmodifiableException( "Cannot add rows to an UnmodifiableQuery" );
	}

	/**
	 * Add empty rows to the query
	 *
	 * @param rows Number of rows to add
	 *
	 * @return Last row added
	 */
	@Override
	public int addRows( int rows ) {
		throw new UnmodifiableException( "Cannot add rows to an UnmodifiableQuery" );
	}

	/**
	 * Deletes a column from the query.
	 *
	 * @param name the name of the column to delete
	 */
	@Override
	public void deleteColumn( Key name ) {
		throw new UnmodifiableException( "Cannot delete columns from an UnmodifiableQuery" );
	}

	/**
	 * Delete a row from the query
	 *
	 * @param index row index, starting at 0
	 *
	 * @return this query
	 */
	@Override
	public UnmodifiableQuery deleteRow( int index ) {
		throw new UnmodifiableException( "Cannot delete rows from an UnmodifiableQuery" );
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
	@Override
	public int addData( Object rowData ) {
		throw new UnmodifiableException( "Cannot add data to an UnmodifiableQuery" );
	}

	/**
	 * Set data for a single cell. 0-based index!
	 *
	 * @param columnName column name
	 * @param rowIndex   row index, starting at 0
	 *
	 * @return this query
	 */
	@Override
	public UnmodifiableQuery setCell( Key columnName, int rowIndex, Object value ) {
		throw new UnmodifiableException( "Cannot set cells in an UnmodifiableQuery" );
	}

	/**
	 * Sort the query
	 *
	 * @param compareFunc function to use for sorting
	 */
	@Override
	public void sort( Comparator<IStruct> compareFunc ) {
		throw new UnmodifiableException( "Cannot sort an UnmodifiableQuery" );
	}

	/***************************
	 * Collection implementation
	 ****************************/

	@Override
	public boolean add( IStruct row ) {
		throw new UnmodifiableException( "Cannot add rows to an UnmodifiableQuery" );
	}

	@Override
	public boolean remove( Object o ) {
		throw new UnmodifiableException( "Cannot remove rows from an UnmodifiableQuery" );
	}

	@Override
	public boolean addAll( Collection<? extends IStruct> rows ) {
		throw new UnmodifiableException( "Cannot add rows to an UnmodifiableQuery" );
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot remove rows from an UnmodifiableQuery" );
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot retain rows in an UnmodifiableQuery" );
	}

	@Override
	public void clear() {
		throw new UnmodifiableException( "Cannot clear an UnmodifiableQuery" );
	}

	/***************************
	 * IReferencable implementation
	 ****************************/

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new UnmodifiableException( "Cannot assign to an UnmodifiableQuery" );
	}

	/**
	 * Duplicate the current query.
	 *
	 * @return A copy of the current query.
	 */
	@Override
	public UnmodifiableQuery duplicate() {
		return super.duplicate( false ).toUnmodifiable();
	}

	/***************************
	 * IUnmodifiable implementation
	 ****************************/

	/**
	 * To Modifiable
	 *
	 * @return The Modifiable type
	 */
	@Override
	public Query toModifiable() {
		var q = new Query();
		// add columns
		for ( Map.Entry<Key, QueryColumn> columnInfo : getColumns().entrySet() ) {
			q.addColumn( columnInfo.getValue().getName(), columnInfo.getValue().getType(), null );
		}
		// then copy data
		for ( Object[] row : getData() ) {
			Object[] duplicatedRow = row.clone();
			q.addRow( duplicatedRow );
		}

		return q;
	}

}
