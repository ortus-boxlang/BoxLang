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

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class ExecutedQuery {

	private final PendingQuery		pendingQuery;
	private final PreparedStatement	statement;
	private final long				executionTime;
	private final Query				results;
	private Object					generatedKey;

	public ExecutedQuery( PendingQuery pendingQuery, PreparedStatement statement, long executionTime, boolean hasResults ) {
		this.pendingQuery	= pendingQuery;
		this.statement		= statement;
		this.executionTime	= executionTime;

		try ( ResultSet rs = this.statement.getResultSet() ) {
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

		try {
			try ( ResultSet keys = this.statement.getGeneratedKeys() ) {
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

	public Query getResults() {
		return this.results;
	}

	public Array getResultsAsArray() {
		return this.results.toStructArray();
	}

	public IStruct getResultsAsStruct( String key ) {
		Map<Object, List<IStruct>>	groupedResults	= this.results.stream().collect( groupingBy( r -> r.get( key ) ) );
		Map<Object, Object>			groupedArray	= groupedResults.entrySet().stream().collect( toMap( Map.Entry::getKey, e -> new Array( e.getValue() ) ) );
		return Struct.fromMap(
		    IStruct.TYPES.LINKED,
		    groupedArray
		);
	}

	public int getRecordCount() {
		return this.results.size();
	}

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
		result.put( "sql", this.statement.toString() );
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

	public Object getGeneratedKey() {
		return this.generatedKey;
	}
}
