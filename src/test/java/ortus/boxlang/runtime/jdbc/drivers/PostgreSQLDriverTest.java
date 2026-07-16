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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

@EnabledIf( "tools.JDBCTestUtils#hasPostgresModule" )
public class PostgreSQLDriverTest extends AbstractDriverTest {

	public static DataSource	postgresqlDatasource;

	protected static Key		datasourceName		= Key.of( "postgresqlDatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "postgres",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "5432",
	    "driver", "postgresql",
	    "database", "myDB"
	);

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		postgresqlDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );
		PostgreSQLDriverTest.createGeneratedKeyTable( postgresqlDatasource, setUpContext );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		AbstractDriverTest.teardownTestDatasource( tearDownContext, postgresqlDatasource );
	}

	/**
	 * Create a table that uses generated keys so we can test our generated key retrieval in BL.
	 *
	 * @param dataSource Datasource object
	 * @param context    Box context
	 */
	public static void createGeneratedKeyTable( DataSource dataSource, IBoxContext context ) {
		try {
			dataSource.execute( "CREATE TABLE generatedKeyTest( id SERIAL PRIMARY KEY, name VARCHAR(155))", context );
		} catch ( DatabaseException ignored ) {
		}
	}

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "postgresqlDatasource";
	}

	@DisplayName( "INSERT ... RETURNING populates result struct with returned column names" )
	@Test
	public void testInsertReturning() {
		instance.executeStatement(
		    String.format( """
		                                queryExecute(
		                                	"CREATE TABLE IF NOT EXISTS users_returning_test (
		                                		id SERIAL PRIMARY KEY,
		                                		email VARCHAR(255) NOT NULL,
		                                		name VARCHAR(155)
		                                	)",
		                                	{},
		                                	{ "datasource": "%s" }
		                                );
		                   """, getDatasourceName() ),
		    context );

		instance.executeStatement(
		    String.format( """
		                                queryExecute(
		                                	"INSERT INTO users_returning_test (email, name) VALUES ('test@example.com', 'Test User') RETURNING id",
		                                	{},
		                                	{ "result": "variables.result", "datasource": "%s" }
		                                );
		                   """, getDatasourceName() ),
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct meta = variables.getAsStruct( result );

		// The RETURNING id column should be present in the result struct (Lucee-compatible)
		assertThat( meta.containsKey( Key.of( "id" ) ) ).isTrue();
		assertNotNull( meta.get( Key.of( "id" ) ) );

		// The generatedKey should also be set to the returned id value
		assertThat( meta.containsKey( Key.generatedKey ) ).isTrue();
		assertThat( meta.get( Key.generatedKey ) ).isEqualTo( meta.get( Key.of( "id" ) ) );

		// Cleanup
		instance.executeStatement(
		    String.format( """
		                                queryExecute(
		                                	"DROP TABLE IF EXISTS users_returning_test",
		                                	{},
		                                	{ "datasource": "%s" }
		                                );
		                   """, getDatasourceName() ),
		    context );
	}

	@DisplayName( "INSERT ... RETURNING multiple columns populates result struct with all returned column names" )
	@Test
	public void testInsertReturningMultipleColumns() {
		instance.executeStatement(
		    String.format( """
		                                queryExecute(
		                                	"CREATE TABLE IF NOT EXISTS users_returning_multi_test (
		                                		id SERIAL PRIMARY KEY,
		                                		email VARCHAR(255) NOT NULL,
		                                		name VARCHAR(155)
		                                	)",
		                                	{},
		                                	{ "datasource": "%s" }
		                                );
		                   """, getDatasourceName() ),
		    context );

		instance.executeStatement(
		    String.format(
		        """
		        q = queryExecute(
		        "INSERT INTO users_returning_multi_test (email, name) VALUES ('test@example.com', 'Test User') RETURNING id, email",
		        {},
		        { "result": "variables.result", "datasource": "%s" }
		        );
		                          """, getDatasourceName() ),
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct meta = variables.getAsStruct( result );

		// Both RETURNING columns should be present in the result struct
		assertThat( meta.containsKey( Key.of( "id" ) ) ).isTrue();
		assertThat( meta.containsKey( Key.of( "email" ) ) ).isTrue();
		assertThat( meta.get( Key.of( "email" ) ) ).isEqualTo( "test@example.com" );

		// Cleanup
		instance.executeStatement(
		    String.format( """
		                                queryExecute(
		                                	"DROP TABLE IF EXISTS users_returning_multi_test",
		                                	{},
		                                	{ "datasource": "%s" }
		                                );
		                   """, getDatasourceName() ),
		    context );
	}

	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	@Disabled
	public void testGeneratedKey() {
		instance.executeStatement(
		    String
		        .format( """
		                 println("########################################################################")
		                                                         q = queryExecute( "
		                                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Michael' ), ( 'Michael2') returning *;
		                                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Brad' ), ( 'Brad2' ) returning *;
		                                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Luis' ) returning *;
		                                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Jon' ), ( 'Jon2' ), ( 'Jon3' ) returning *;
		                                   ",
		                                                          	{},
		                                                          	{ "result": "variables.result", "datasource" : "%s" }
		                                                          );
		                 println("########################################################################")
		                            println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
		                            println(q)
		                            println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
		                            println(result)
		                            println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
		                                                                         """, getDatasourceName() ),
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct meta = variables.getAsStruct( result );

		assertThat( DoubleCaster.cast( meta.get( Key.generatedKey ), false ) ).isEqualTo( 1.0d );

		Array generatedKeys = meta.getAsArray( Key.generatedKeys );

		assertThat( generatedKeys ).hasSize( 4 );
		// These keys are coming back as BigDecimal, so let's massage them into an array of ints for easier comparision
		Integer[] firstKeys = ( ( Array ) generatedKeys.get( 0 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( firstKeys ).isEqualTo( new Integer[] { 1, 2 } );

		Integer[] secondKeys = ( ( Array ) generatedKeys.get( 1 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( secondKeys ).isEqualTo( new Integer[] { 3, 4 } );

		Integer[] thirdKeys = ( ( Array ) generatedKeys.get( 2 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( thirdKeys ).isEqualTo( new Integer[] { 5 } );

		Integer[] fourthKeys = ( ( Array ) generatedKeys.get( 3 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( fourthKeys ).isEqualTo( new Integer[] { 6, 7, 8 } );

		assertThat( meta.get( "updateCount" ) ).isEqualTo( 8 );
		Array updateCounts = meta.getAsArray( Key.of( "updateCounts" ) );
		assertThat( updateCounts.toArray() ).isEqualTo( new Integer[] { 2, 2, 1, 3 } );
	}

}
