package ortus.boxlang.runtime.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;

/**
 *
 * <ul>
 * <li>toArray( ResultSet resultSet )</li>
 * <li>toQuery( ResultSet resultSet )</li>
 * <li>toStruct( ResultSet resultSet )</li>
 * <li>toEntityArray( ResultSet resultSet )</li>
 * </ul>
 */
public class StatementResult {

	/**
	 * The result set returned from the statement execution. This could be null
	 */
	ResultSet	resultSet;

	/**
	 * Query options used in the query execution.
	 * <p>
	 * For this class's purposes, we only care about the following options:
	 * <ul>
	 * <li>`returntype` - The type of result to return from the query:
	 * <ul>
	 * <li>"query" - Return a Query object</li>
	 * <li>"array" - Return an array of structs</li>
	 * <li>"array_of_entity" - Return an array of ORM entities</li>
	 * <li>"struct" - Returns a struct of structs (requires a `columnKey` definition).</li>
	 * </ul>
	 * </li>
	 * <li>columnkey - Name of the column to use as the key when <code>returntype</code> is <code>"struct"</code>.</li>
	 */
	IStruct		queryOptions;

	public StatementResult( ResultSet resultSet, IStruct queryOptions ) {
		this.resultSet		= resultSet;
		this.queryOptions	= queryOptions;
	}

	/**
	 * Retrieve the result set as an array of Structs.
	 *
	 * @return An array of Structs, each representing a row of the result set.
	 */
	public Struct[] toArray() throws SQLException {
		// Many queries will not return results; we should be prepared for this and return a default empty object.
		if ( resultSet == null ) {
			return new Struct[ 0 ];
		}
		List<IStruct> result = new ArrayList<>();
		while ( resultSet.next() ) {
			IStruct				row			= new Struct();
			ResultSetMetaData	metaData	= resultSet.getMetaData();
			int					columnCount	= metaData.getColumnCount();
			for ( int i = 1; i <= columnCount; i++ ) {
				String	columnName	= metaData.getColumnName( i );
				Object	columnValue	= resultSet.getObject( i );
				row.put( columnName, columnValue );
			}
			result.add( row );
		}
		return result.toArray( new Struct[ 0 ] );
	}

	/**
	 * Retrieve the result set as a query object.
	 *
	 * @return A Query object populated with the query/statement result.
	 */
	public Query toQuery() throws SQLException {
		// Many queries will not return results; we should be prepared for this and return a default empty object.
		if ( resultSet == null ) {
			return new Query();
		}
		Query result = new Query();
		while ( resultSet.next() ) {
			IStruct				row			= new Struct();
			ResultSetMetaData	metaData	= resultSet.getMetaData();
			int					columnCount	= metaData.getColumnCount();
			for ( int i = 1; i <= columnCount; i++ ) {
				String	columnName	= metaData.getColumnName( i );
				Object	columnValue	= resultSet.getObject( i );
				result.addColumn( Key.of( columnName ), QueryColumnType.fromSQLType( metaData.getColumnType( i ) ) );
				row.put( columnName, columnValue );
			}
			result.addRow( row );
		}
		return result;
	}
}
