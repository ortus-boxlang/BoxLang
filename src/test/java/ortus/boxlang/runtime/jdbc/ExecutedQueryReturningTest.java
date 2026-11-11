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
package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.drivers.GenericJDBCDriver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

public class ExecutedQueryReturningTest {

	private static IBoxContext context;

	@BeforeAll
	public static void setUp() {
		BoxRuntime runtime = BoxRuntime.getInstance( true );
		context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
	}

	@DisplayName( "It materializes hidden RETURNING rows while preserving generated-key metadata" )
	@Test
	public void testMaterializesReturningRowsFromGeneratedKeys() throws SQLException {
		PendingQuery	pendingQuery	= new PendingQuery(
		    context,
		    "INSERT INTO users (name) VALUES ('Michael'), ('Brad') RETURNING id AS user_id, name AS display_name",
		    new ArrayList<>()
		);
		ResultSet		resultSet		= mockReturningResultSet();
		BoxStatement	statement		= mockGeneratedKeysStatement( resultSet, 2 );

		ExecutedQuery	executedQuery	= ExecutedQuery.fromPendingQuery( pendingQuery, statement, 10, false, null, context );

		Query			results			= executedQuery.getResults();
		assertThat( results.size() ).isEqualTo( 2 );
		assertThat( results.getRowAsStruct( 0 ).get( "user_id" ) ).isEqualTo( 101 );
		assertThat( results.getRowAsStruct( 1 ).get( "display_name" ) ).isEqualTo( "Brad" );

		IStruct	metadata		= executedQuery.getQueryMeta();
		Array	returningRows	= metadata.getAsArray( Key.returningRows );
		assertThat( returningRows ).hasSize( 2 );
		assertThat( ( ( IStruct ) returningRows.get( 0 ) ).get( "display_name" ) ).isEqualTo( "Michael" );
		assertThat( metadata.get( Key.generatedKey ) ).isEqualTo( 101 );
		assertThat( ( ( Array ) metadata.getAsArray( Key.generatedKeys ).get( 0 ) ).toArray() ).isEqualTo( new Object[] { 101, 102 } );
		assertThat( metadata.get( Key.recordCount ) ).isEqualTo( 2 );
		assertThat( metadata.get( Key.updateCount ) ).isEqualTo( 2 );
		assertThat( metadata.containsKey( Key.of( "user_id" ) ) ).isFalse();
		assertThat( metadata.containsKey( Key.of( "display_name" ) ) ).isFalse();
	}

	@DisplayName( "It populates returningRows when RETURNING is a normal result set" )
	@Test
	public void testPopulatesReturningRowsFromNormalResultSet() throws SQLException {
		PendingQuery	pendingQuery	= new PendingQuery(
		    context,
		    "UPDATE users SET active = true RETURNING id AS user_id, name AS display_name",
		    new ArrayList<>()
		);
		BoxStatement	statement		= mockResultStatement( mockReturningResultSet(), 2 );

		ExecutedQuery	executedQuery	= ExecutedQuery.fromPendingQuery( pendingQuery, statement, 10, true, null, context );

		assertThat( executedQuery.getResults().size() ).isEqualTo( 2 );
		assertThat( executedQuery.getQueryMeta().getAsArray( Key.returningRows ) ).hasSize( 2 );
		assertThat( executedQuery.getQueryMeta().get( Key.recordCount ) ).isEqualTo( 2 );
		assertThat( executedQuery.getQueryMeta().get( Key.updateCount ) ).isEqualTo( 2 );
	}

	@DisplayName( "It includes an empty returningRows array when no rows are returned" )
	@Test
	public void testIncludesEmptyReturningRows() throws SQLException {
		PendingQuery	pendingQuery	= new PendingQuery(
		    context,
		    "DELETE FROM users WHERE id = -1 RETURNING id AS user_id, name AS display_name",
		    new ArrayList<>()
		);
		ResultSet		resultSet		= mockResultSet(
		    new String[] { "user_id", "display_name" },
		    new int[] { Types.INTEGER, Types.VARCHAR },
		    new Object[][] {}
		);

		ExecutedQuery	executedQuery	= ExecutedQuery.fromPendingQuery(
		    pendingQuery,
		    mockGeneratedKeysStatement( resultSet, 0 ),
		    10,
		    false,
		    null,
		    context
		);

		assertThat( executedQuery.getResults().size() ).isEqualTo( 0 );
		assertThat( executedQuery.getResults().getColumns() ).hasSize( 2 );
		assertThat( executedQuery.getQueryMeta().getAsArray( Key.returningRows ) ).isEmpty();
		assertThat( executedQuery.getQueryMeta().get( Key.recordCount ) ).isEqualTo( 0 );
		assertThat( executedQuery.getQueryMeta().get( Key.updateCount ) ).isEqualTo( 0 );
	}

	@DisplayName( "It does not convert Oracle ROWID generated keys into query rows" )
	@Test
	public void testDoesNotConvertRowIds() throws SQLException {
		PendingQuery	pendingQuery	= new PendingQuery(
		    context,
		    "INSERT INTO users (name) VALUES ('Michael') RETURNING id",
		    new ArrayList<>()
		);
		ResultSet		resultSet		= mockResultSet(
		    new String[] { "ROWID" },
		    new int[] { Types.VARCHAR },
		    new Object[][] { { "AAAPr9AAEAAAADpAAA" } }
		);

		ExecutedQuery	executedQuery	= ExecutedQuery.fromPendingQuery(
		    pendingQuery,
		    mockGeneratedKeysStatement( resultSet, 1 ),
		    10,
		    false,
		    null,
		    context
		);

		assertThat( executedQuery.getResults().size() ).isEqualTo( 0 );
		assertThat( executedQuery.getQueryMeta().getAsArray( Key.returningRows ) ).isEmpty();
		assertThat( executedQuery.getQueryMeta().get( Key.rowID ) ).isEqualTo( "AAAPr9AAEAAAADpAAA" );
		assertThat( executedQuery.getQueryMeta().get( Key.generatedKey ) ).isEqualTo( "AAAPr9AAEAAAADpAAA" );
	}

	@DisplayName( "It leaves a plain INSERT as an empty query with generated-key metadata" )
	@Test
	public void testLeavesPlainInsertBehaviorUnchanged() throws SQLException {
		PendingQuery	pendingQuery	= new PendingQuery(
		    context,
		    "INSERT INTO users (name) VALUES ('Michael')",
		    new ArrayList<>()
		);
		ResultSet		resultSet		= mockResultSet(
		    new String[] { "GENERATED_KEY" },
		    new int[] { Types.INTEGER },
		    new Object[][] { { 101 } }
		);

		ExecutedQuery	executedQuery	= ExecutedQuery.fromPendingQuery(
		    pendingQuery,
		    mockGeneratedKeysStatement( resultSet, 1 ),
		    10,
		    false,
		    null,
		    context
		);

		assertThat( executedQuery.getResults().size() ).isEqualTo( 0 );
		assertThat( executedQuery.getQueryMeta().containsKey( Key.returningRows ) ).isFalse();
		assertThat( executedQuery.getQueryMeta().get( Key.generatedKey ) ).isEqualTo( 101 );
		assertThat( executedQuery.getQueryMeta().get( Key.recordCount ) ).isEqualTo( 1 );
		assertThat( executedQuery.getQueryMeta().get( Key.updateCount ) ).isEqualTo( 1 );
	}

	private static BoxStatement mockGeneratedKeysStatement( ResultSet resultSet, int updateCount ) throws SQLException {
		BoxStatement statement = mockStatement();
		when( statement.getUpdateCount() ).thenReturn( updateCount, -1 );
		when( statement.getGeneratedKeys() ).thenReturn( resultSet );
		when( statement.getMoreResults() ).thenReturn( false );
		return statement;
	}

	private static BoxStatement mockResultStatement( ResultSet resultSet, int updateCount ) throws SQLException {
		BoxStatement statement = mockStatement();
		when( statement.getResultSet() ).thenReturn( resultSet );
		when( statement.getUpdateCount() ).thenReturn( updateCount, -1 );
		when( statement.getMoreResults() ).thenReturn( false );
		return statement;
	}

	private static BoxStatement mockStatement() throws SQLException {
		BoxStatement		statement		= mock( BoxStatement.class );
		BoxConnection		boxConnection	= mock( BoxConnection.class );
		DataSource			dataSource		= mock( DataSource.class );
		DatasourceConfig	configuration	= mock( DatasourceConfig.class );
		DatabaseMetaData	metadata		= mock( DatabaseMetaData.class );

		when( statement.getBoxConnection() ).thenReturn( boxConnection );
		when( statement.getConnection() ).thenReturn( boxConnection );
		when( boxConnection.getDataSource() ).thenReturn( dataSource );
		when( boxConnection.getMetaData() ).thenReturn( metadata );
		when( dataSource.getConfiguration() ).thenReturn( configuration );
		when( configuration.getDriver() ).thenReturn( new GenericJDBCDriver() );
		when( metadata.getDriverName() ).thenReturn( "PostgreSQL JDBC Driver" );
		return statement;
	}

	private static ResultSet mockReturningResultSet() throws SQLException {
		return mockResultSet(
		    new String[] { "user_id", "display_name" },
		    new int[] { Types.INTEGER, Types.VARCHAR },
		    new Object[][] { { 101, "Michael" }, { 102, "Brad" } }
		);
	}

	private static ResultSet mockResultSet( String[] labels, int[] types, Object[][] rows ) throws SQLException {
		ResultSet			resultSet	= mock( ResultSet.class );
		ResultSetMetaData	metadata	= mock( ResultSetMetaData.class );
		AtomicInteger		currentRow	= new AtomicInteger( -1 );

		when( resultSet.getMetaData() ).thenReturn( metadata );
		when( resultSet.next() ).thenAnswer( invocation -> currentRow.incrementAndGet() < rows.length );
		when( resultSet.getObject( anyInt() ) ).thenAnswer( invocation -> rows[ currentRow.get() ][ invocation.getArgument( 0, Integer.class ) - 1 ] );
		when( metadata.getColumnCount() ).thenReturn( labels.length );
		when( metadata.getColumnLabel( anyInt() ) ).thenAnswer( invocation -> labels[ invocation.getArgument( 0, Integer.class ) - 1 ] );
		when( metadata.getColumnType( anyInt() ) ).thenAnswer( invocation -> types[ invocation.getArgument( 0, Integer.class ) - 1 ] );
		when( metadata.isNullable( anyInt() ) ).thenReturn( ResultSetMetaData.columnNullable );
		when( metadata.isReadOnly( anyInt() ) ).thenReturn( false );
		when( metadata.isAutoIncrement( anyInt() ) ).thenReturn( false );
		when( metadata.getScale( anyInt() ) ).thenReturn( 0 );
		when( metadata.getColumnDisplaySize( anyInt() ) ).thenReturn( 255 );
		return resultSet;
	}
}
