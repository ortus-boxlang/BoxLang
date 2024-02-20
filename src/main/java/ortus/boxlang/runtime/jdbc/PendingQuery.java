package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class PendingQuery {

	public String sql;
	public List<QueryParameter> parameters;

	public PendingQuery(String sql, List<QueryParameter> parameters) {
		this.sql = sql;
		this.parameters = parameters;
	}

	public List<Object> getParameterValues() {
		return this.parameters.stream().map(param -> param.value).collect(Collectors.toList());
	}

	public ExecutedQuery execute(Connection conn) throws SQLException {
		PreparedStatement statement = conn.prepareStatement(this.sql, PreparedStatement.RETURN_GENERATED_KEYS);
		long startTick = System.nanoTime();
		boolean hasResults = statement.execute();
		long endTick = System.nanoTime();
		return new ExecutedQuery(
			this,
			statement,
			endTick - startTick
		);
	}

}
