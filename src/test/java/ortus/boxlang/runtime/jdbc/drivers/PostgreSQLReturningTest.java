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
package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import tools.JDBCTestUtils;

@EnabledIf( "tools.JDBCTestUtils#hasPostgresModule" )
public class PostgreSQLReturningTest {

	private static final Key			DATASOURCE_NAME	= Key.of( "PostgreSQLReturningDatasource" );
	private static final Key			QUERY			= Key.of( "query" );
	private static final Key			RESULT			= Key.of( "result" );

	private static BoxRuntime			instance;
	private static DataSource			datasource;

	private ScriptingRequestBoxContext	context;
	private IScope						variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		ScriptingRequestBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasource = DataSource.fromStruct(
		    DATASOURCE_NAME,
		    Struct.of(
		        "username", "boxlang",
		        "password", "123456Password",
		        "host", "localhost",
		        "port", System.getenv().getOrDefault( "POSTGRES_TEST_PORT", "54329" ),
		        "driver", "postgresql",
		        "database", "boxlang"
		    )
		);
		instance.getConfiguration().datasources.put( DATASOURCE_NAME, datasource.getConfiguration() );
		instance.getDataSourceService().register( DATASOURCE_NAME, datasource );
		datasource.execute(
		    "CREATE TABLE IF NOT EXISTS returning_test (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name VARCHAR(155), active BOOLEAN)",
		    setUpContext
		);
	}

	@AfterAll
	public static void tearDown() throws SQLException {
		ScriptingRequestBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		JDBCTestUtils.dropTestTable( datasource, tearDownContext, "returning_test", true );
		datasource.shutdown();
	}

	@BeforeEach
	public void setUpEach() {
		this.context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.context.getApplicationListener().updateSettings( Struct.of( Key.datasource, DATASOURCE_NAME.getName() ) );
		this.variables = this.context.getScopeNearby( VariablesScope.name );
		datasource.execute( "TRUNCATE TABLE returning_test RESTART IDENTITY", this.context );
	}

	@DisplayName( "It returns every row and column from a multi-row INSERT RETURNING" )
	@Test
	public void testMultiRowInsertReturning() {
		instance.executeSource(
		    """
		    query = queryExecute(
		        "INSERT INTO returning_test (name, active) VALUES ('Michael', true), ('Brad', false) RETURNING id AS returned_id, name AS returned_name",
		        {},
		        { result: "result" }
		    );
		    """,
		    this.context
		);

		Query	query	= this.variables.getAsQuery( QUERY );
		IStruct	result	= this.variables.getAsStruct( RESULT );
		Array	rows	= result.getAsArray( Key.returningRows );

		assertThat( query.size() ).isEqualTo( 2 );
		assertThat( query.getColumns() ).hasSize( 2 );
		assertThat( IntegerCaster.cast( query.getRowAsStruct( 0 ).get( "returned_id" ) ) ).isEqualTo( 1 );
		assertThat( query.getRowAsStruct( 1 ).get( "returned_name" ) ).isEqualTo( "Brad" );
		assertThat( rows ).hasSize( 2 );
		assertThat( ( ( IStruct ) rows.get( 0 ) ).get( "returned_name" ) ).isEqualTo( "Michael" );
		assertThat( IntegerCaster.cast( result.get( Key.generatedKey ) ) ).isEqualTo( 1 );
		assertThat( generatedKeys( result ) ).containsExactly( 1, 2 ).inOrder();
		assertThat( result.get( Key.recordCount ) ).isEqualTo( 2 );
		assertThat( result.get( Key.updateCount ) ).isEqualTo( 2 );
		assertThat( result.containsKey( Key.of( "returned_id" ) ) ).isFalse();
		assertThat( result.containsKey( Key.of( "returned_name" ) ) ).isFalse();
	}

	@DisplayName( "It returns rows from a bound UPDATE RETURNING" )
	@Test
	public void testBoundUpdateReturning() {
		seedRows();
		instance.executeSource(
		    """
		    query = queryExecute(
		        "UPDATE returning_test SET name = :newName WHERE active = :active RETURNING id, name",
		        { newName: "Updated", active: { value: false, sqltype: "boolean" } },
		        { result: "result" }
		    );
		    """,
		    this.context
		);

		Query	query	= this.variables.getAsQuery( QUERY );
		IStruct	result	= this.variables.getAsStruct( RESULT );

		assertThat( query.size() ).isEqualTo( 1 );
		assertThat( query.getRowAsStruct( 0 ).get( Key._NAME ) ).isEqualTo( "Updated" );
		assertThat( result.getAsArray( Key.returningRows ) ).hasSize( 1 );
		assertThat( IntegerCaster.cast( result.get( Key.generatedKey ) ) ).isEqualTo( 2 );
		assertThat( generatedKeys( result ) ).containsExactly( 2 );
		assertThat( result.get( Key.recordCount ) ).isEqualTo( 1 );
		assertThat( result.get( Key.updateCount ) ).isEqualTo( 1 );
	}

	@DisplayName( "It returns rows from a bound DELETE RETURNING" )
	@Test
	public void testBoundDeleteReturning() {
		seedRows();
		instance.executeSource(
		    """
		    query = queryExecute(
		        "DELETE FROM returning_test WHERE name = :name RETURNING id, name",
		        { name: "Brad" },
		        { result: "result" }
		    );
		    """,
		    this.context
		);

		Query	query	= this.variables.getAsQuery( QUERY );
		IStruct	result	= this.variables.getAsStruct( RESULT );

		assertThat( query.size() ).isEqualTo( 1 );
		assertThat( query.getRowAsStruct( 0 ).get( Key._NAME ) ).isEqualTo( "Brad" );
		assertThat( result.getAsArray( Key.returningRows ) ).hasSize( 1 );
		assertThat( IntegerCaster.cast( result.get( Key.generatedKey ) ) ).isEqualTo( 2 );
		assertThat( generatedKeys( result ) ).containsExactly( 2 );
		assertThat( result.get( Key.recordCount ) ).isEqualTo( 1 );
		assertThat( result.get( Key.updateCount ) ).isEqualTo( 1 );
	}

	@DisplayName( "It includes empty returningRows for zero-row RETURNING" )
	@Test
	public void testZeroRowReturning() {
		instance.executeSource(
		    """
		    query = queryExecute(
		        "DELETE FROM returning_test WHERE id = :id RETURNING id, name",
		        { id: { value: -1, sqltype: "bigint" } },
		        { result: "result" }
		    );
		    """,
		    this.context
		);

		Query	query	= this.variables.getAsQuery( QUERY );
		IStruct	result	= this.variables.getAsStruct( RESULT );

		assertThat( query.size() ).isEqualTo( 0 );
		assertThat( query.getColumns() ).hasSize( 2 );
		assertThat( result.getAsArray( Key.returningRows ) ).isEmpty();
		assertThat( result.containsKey( Key.generatedKey ) ).isFalse();
		assertThat( result.containsKey( Key.generatedKeys ) ).isFalse();
		assertThat( result.get( Key.recordCount ) ).isEqualTo( 0 );
		assertThat( result.get( Key.updateCount ) ).isEqualTo( 0 );
	}

	@DisplayName( "It leaves plain INSERT result behavior unchanged" )
	@Test
	public void testPlainInsert() {
		instance.executeSource(
		    """
		    query = queryExecute(
		        "INSERT INTO returning_test (name, active) VALUES ('Michael', true)",
		        {},
		        { result: "result" }
		    );
		    """,
		    this.context
		);

		Query	query	= this.variables.getAsQuery( QUERY );
		IStruct	result	= this.variables.getAsStruct( RESULT );

		assertThat( query.size() ).isEqualTo( 0 );
		assertThat( result.containsKey( Key.returningRows ) ).isFalse();
		assertThat( IntegerCaster.cast( result.get( Key.generatedKey ) ) ).isEqualTo( 1 );
		assertThat( generatedKeys( result ) ).containsExactly( 1 );
		assertThat( result.get( Key.recordCount ) ).isEqualTo( 1 );
		assertThat( result.get( Key.updateCount ) ).isEqualTo( 1 );
	}

	private void seedRows() {
		datasource.execute(
		    "INSERT INTO returning_test (name, active) VALUES ('Michael', true), ('Brad', false)",
		    this.context
		);
	}

	private static java.util.List<Integer> generatedKeys( IStruct result ) {
		return ( ( Array ) result.getAsArray( Key.generatedKeys ).get( 0 ) ).stream().map( IntegerCaster::cast ).toList();
	}
}
