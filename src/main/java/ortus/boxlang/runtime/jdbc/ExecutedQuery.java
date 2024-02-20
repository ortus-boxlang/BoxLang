package ortus.boxlang.runtime.jdbc;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutedQuery {

	public final PendingQuery pendingQuery;
	public final PreparedStatement statement;
	public final long executionTime;
	public final ResultSet resultSet;
	public final List<Struct> results;
	public final int recordCount;
	public final List<QueryColumn> columns;
	private Object generatedKey;

	public ExecutedQuery(PendingQuery pendingQuery, PreparedStatement statement, long executionTime) throws SQLException {
		this.pendingQuery = pendingQuery;
		this.statement = statement;
		this.executionTime = executionTime;

		this.resultSet = this.statement.getResultSet();
		this.results = new ArrayList<>();
		this.columns = new ArrayList<>();

		if (this.resultSet != null) {
			ResultSetMetaData resultSetMetaData = this.resultSet.getMetaData();
			int columnCount = resultSetMetaData.getColumnCount();

			// The column count starts from 1
			for (int i = 1; i <= columnCount; i++) {
				columns.add(new QueryColumn(
					resultSetMetaData.getColumnLabel(i),
					resultSetMetaData.getColumnType(i)
				));
			}

			while (this.resultSet.next()) {
				Struct row = new Struct(IStruct.TYPES.LINKED);
				int rowNum = this.resultSet.getRow();
				for (QueryColumn column : this.columns) {
					row.put(column.name, this.resultSet.getObject(rowNum));
				}
				this.results.add(row);
			}
		}

		this.recordCount = this.results.size();

		ResultSet generatedKeysResultSet = this.statement.getGeneratedKeys();
		if (generatedKeysResultSet.next()) {
            this.generatedKey = generatedKeysResultSet.getObject(generatedKeysResultSet.getRow());
        }
	}

	public List<Struct> getResults() {
		return this.results;
	}

	public Array getResultsAsArray() {
		return Array.fromList(this.results);
	}

	public Query getResultsAsQuery() {
		Query q = new Query();
		for (QueryColumn column : this.columns ) {
			q.addColumn( Key.of( column.name ), column.getTypeAsQueryColumnType() );
		}
		q.addAll(this.results);
		return q;
	}

	public Struct getResultStruct() {
		/**
		 * * SQL: The SQL statement that was executed. (string)
		 * * Cached: If the query was cached. (boolean)
		 * * SqlParameters: An ordered Array of cfqueryparam values. (array)
		 * * RecordCount: Total number of records in the query. (numeric)
		 * * ColumnList: Column list, comma separated. (string)
		 * * ExecutionTime: Execution time for the SQL request. (numeric)
		 * * GENERATEDKEY: CF 9+ If the query was an INSERT with an identity or auto-increment value the value of that ID is placed in this variable.
		 */
		Struct result = new Struct();
		result.put("sql", statement.toString());
		result.put("cached", false);
		result.put("sqlParameters", Array.fromList(pendingQuery.getParameterValues()));
		result.put("recordCount", this.recordCount);
		result.put("columnList", ListUtil.asString(Array.fromList(this.columns.stream().map(column -> column.name).collect(Collectors.toList())), ","));
		result.put("executionTime", this.executionTime);
		if (this.generatedKey != null) {
			result.put("generatedKey", this.generatedKey);
		}
		return result;
	}
}
