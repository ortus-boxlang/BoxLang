/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.jdbc;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.sql.*;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * This class represents a query that has been executed and contains the results of executing that query.
 * It contains a reference to the {@link PendingQuery} that was executed to create this.
 */
final class ExecutedQuery {

	/**
	 * The {@link PendingQuery} executed.
	 */
	private final PendingQuery	pendingQuery;

	/**
	 * The executed sql string with the bindings used in the String.
	 */
	private final String		executedSql;

	/**
	 * The execution time of the query.
	 */
	private final long			executionTime;

	/**
	 * A Query object holding the results of the query.
	 * If there were no results, the Query object will have no rows.
	 * 
	 * @see Query
	 */
	private final Query			results;

	/**
	 * The generated key of the request, if any.
	 */
	@Nullable
	private Object				generatedKey;

	/**
	 * Creates an ExecutedQuery instance.
	 *
	 * @param pendingQuery  The {@link PendingQuery} executed.
	 * @param statement     The {@link PreparedStatement} instance executed.
	 * @param executionTime The execution time the query took.
	 * @param hasResults    Boolean flag from {@link PreparedStatement#execute()} designating if the execution returned any results.
	 */
	public ExecutedQuery( PendingQuery pendingQuery, PreparedStatement statement, long executionTime, boolean hasResults ) {
		this.pendingQuery	= pendingQuery;
		this.executedSql	= statement.toString();
		this.executionTime	= executionTime;

		try ( ResultSet rs = statement.getResultSet() ) {
			this.results = new Query();

			if ( rs != null ) {
				ResultSetMetaData	resultSetMetaData	= rs.getMetaData();
				int					columnCount			= resultSetMetaData.getColumnCount();

				// The column count starts from 1
				for ( int i = 1; i <= columnCount; i++ ) {
					this.results.addColumn(
					    Key.of( resultSetMetaData.getColumnLabel( i ) ),
					    QueryColumnType.fromSQLType( resultSetMetaData.getColumnType( i ) )
					);
				}

				while ( rs.next() ) {
					Struct row = new Struct( IStruct.TYPES.LINKED );
					for ( int i = 1; i <= columnCount; i++ ) {
						// @TODO: Fix the duplicate Key.of() call here
						row.put( Key.of( resultSetMetaData.getColumnLabel( i ) ), rs.getObject( i ) );
					}
					this.results.addRow( row );
				}
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}

		// Capture generated keys, if any.
		try {
			try ( ResultSet keys = statement.getGeneratedKeys() ) {
				if ( keys != null && keys.next() ) {
					this.generatedKey = keys.getObject( 1 );
				}
			} catch ( SQLException e ) {
				throw new DatabaseException( e.getMessage(), e );
			}
		} catch ( NullPointerException e ) {
			// This is likely due to Hikari wrapping a null ResultSet.
			// There should not be a null ResultSet returned from getGeneratedKeys
			// (https://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html#getGeneratedKeys--)
			// but some JDBC drivers do anyway.
			// Since Hikari wraps the null value, we can't get access to it,
			// so instead we have to catch it here and ignore it.
			// We do check the message to try to be very particular about what NullPointerExceptions we are catching
			if ( !e.getMessage().equals( "Cannot invoke \"java.sql.ResultSet.next()\" because \"this.delegate\" is null" ) ) {
				throw e;
			}
		}
	}

	/**
	 * Returns the Query object of results of the query.
	 * 
	 * @return A Query object of results.
	 */
	public Query getResults() {
		return this.results;
	}

	/**
	 * Returns an {@link Array} of {@link Struct} instances representing the {@link Query} results.
	 * 
	 * @return An Array of Structs representing the Query
	 */
	public Array getResultsAsArray() {
		return this.results.toStructArray();
	}

	/**
	 * Returns a {@link Struct} instance grouping the results by the given key.
	 * 
	 * @param key The column to group the results by.
	 * 
	 * @return A struct of String to Struct instances representing the Query results.
	 */
	public IStruct getResultsAsStruct( String key ) {
		Map<Object, List<IStruct>>	groupedResults	= this.results.stream().collect( groupingBy( r -> r.get( key ) ) );
		Map<Object, Object>			groupedArray	= groupedResults.entrySet().stream().collect( toMap( Map.Entry::getKey, e -> new Array( e.getValue() ) ) );
		return Struct.fromMap(
		    IStruct.TYPES.LINKED,
		    groupedArray
		);
	}

	/**
	 * Returns the total count of records returned by the query.
	 * 
	 * @return The total count of records.
	 */
	public int getRecordCount() {
		return this.results.size();
	}

	/**
	 * Returns the `result` struct returned from `queryExecute` and `cfquery`.
	 * 
	 * @return A `result` struct
	 */
	public Struct getResultStruct() {
		/*
		 * * SQL: The SQL statement that was executed. (string)
		 * * Cached: If the query was cached. (boolean)
		 * * SqlParameters: An ordered Array of cfqueryparam values. (array)
		 * * RecordCount: Total number of records in the query. (numeric)
		 * * ColumnList: Column list, comma separated. (string)
		 * * ExecutionTime: Execution time for the SQL request. (numeric)
		 * * GENERATEDKEY: CF 9+ If the query was an INSERT with an identity or auto-increment value the value of that ID is placed in this variable.
		 */
		Struct result = new Struct();
		result.put( "sql", this.executedSql );
		result.put( "cached", false );
		result.put( "sqlParameters", Array.fromList( this.pendingQuery.getParameterValues() ) );
		result.put( "recordCount", getRecordCount() );
		result.put( "columnList", this.results.getColumnList() );
		result.put( "executionTime", this.executionTime );
		if ( this.generatedKey != null ) {
			result.put( "generatedKey", this.generatedKey );
		}
		return result;
	}

	/**
	 * Returns the generated key of the query, if any
	 * 
	 * @return The generated key of the query.
	 */
	@Nullable
	public Object getGeneratedKey() {
		return this.generatedKey;
	}
}
