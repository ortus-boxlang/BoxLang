package ortus.boxlang.runtime.jdbc;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.util.ListUtil;

public class ExecutedQuery {

	public final PendingQuery		pendingQuery;
	public final PreparedStatement	statement;
	public final long				executionTime;
	public final ResultSet			resultSet;
	public final List<Struct>		results;
	public final int				recordCount;
	public final List<QueryColumn>	columns;
	private Object					generatedKey;

	public ExecutedQuery( PendingQuery pendingQuery, PreparedStatement statement, long executionTime, boolean hasResults ) {
		this.pendingQuery	= pendingQuery;
		this.statement		= statement;
		this.executionTime	= executionTime;

		try ( ResultSet rs = this.statement.getResultSet() ) {
			this.resultSet	= rs;

			this.results	= new ArrayList<>();
			this.columns	= new ArrayList<>();

			if ( this.resultSet != null ) {
				ResultSetMetaData	resultSetMetaData	= this.resultSet.getMetaData();
				int					columnCount			= resultSetMetaData.getColumnCount();

				// The column count starts from 1
				for ( int i = 1; i <= columnCount; i++ ) {
					columns.add( new QueryColumn(
					    resultSetMetaData.getColumnLabel( i ),
					    resultSetMetaData.getColumnType( i )
					) );
				}

				while ( this.resultSet.next() ) {
					Struct row = new Struct( IStruct.TYPES.LINKED );
					for ( int i = 1; i <= this.columns.size(); i++ ) {
						QueryColumn column = this.columns.get( i - 1 );
						row.put( column.name, this.resultSet.getObject( i ) );
					}
					this.results.add( row );
				}
			}

			this.recordCount = this.results.size();
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}

		// @TODO Figure out why uncommenting this breaks things
		// if (hasResults) {
		// try (ResultSet keys = this.statement.getGeneratedKeys()) {
		// if (keys != null && keys.next()) {
		// this.generatedKey = keys.getObject(keys.getRow());
		// }
		// } catch (SQLException e) {
		// throw new DatabaseException(e.getMessage(), e);
		// }
		// }
	}

	public List<Struct> getResults() {
		return this.results;
	}

	public Array getResultsAsArray() {
		return Array.fromList( this.results );
	}

	public Query getResultsAsQuery() {
		Query q = new Query();
		for ( QueryColumn column : this.columns ) {
			q.addColumn( Key.of( column.name ), column.getTypeAsQueryColumnType() );
		}
		q.addAll( this.results );
		return q;
	}

	public IStruct getResultsAsStruct( String key ) {
		Map<Object, List<Struct>>	groupedResults	= this.results.stream().collect( groupingBy( r -> r.get( key ) ) );
		Map<Object, Object>			groupedArray	= groupedResults.entrySet().stream().collect( toMap( Map.Entry::getKey, e -> new Array( e.getValue() ) ) );
		return Struct.fromMap(
		    IStruct.TYPES.LINKED,
		    groupedArray
		);
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
		result.put( "sql", statement.toString() );
		result.put( "cached", false );
		result.put( "sqlParameters", Array.fromList( pendingQuery.getParameterValues() ) );
		result.put( "recordCount", this.recordCount );
		result.put( "columnList",
		    ListUtil.asString( Array.fromList( this.columns.stream().map( column -> column.name ).collect( Collectors.toList() ) ), "," ) );
		result.put( "executionTime", this.executionTime );
		if ( this.generatedKey != null ) {
			result.put( "generatedKey", this.generatedKey );
		}
		return result;
	}
}
