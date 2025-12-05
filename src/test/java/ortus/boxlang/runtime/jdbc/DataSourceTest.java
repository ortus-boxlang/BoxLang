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
 * distributed under the License is distribu ted on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

public class DataSourceTest {

	static BoxRuntime	instance;
	static DataSource	datasource;
	static DataSource	testDB;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasource	= JDBCTestUtils.constructTestDataSource( java.lang.invoke.MethodHandles.lookup().lookupClass().getSimpleName(), setUpContext );
		testDB		= JDBCTestUtils.constructTestDataSource( "testDB", setUpContext );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		if ( datasource != null ) {
			datasource.shutdown();
		}

		if ( testDB != null ) {
			testDB.shutdown();
		}
	}

	@BeforeEach
	public void resetTable() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( datasource, context ) );
	}

	@DisplayName( "It can get an Apache Derby JDBC connection" )
	@Test
	void testDerbyConnection() throws SQLException {
		DataSource derbyDB = JDBCTestUtils.buildDatasource( "funkyDB", new Struct() );

		try ( BoxConnection conn = derbyDB.getBoxConnection() ) {
			assertThat( conn ).isInstanceOf( BoxConnection.class );
		}
		derbyDB.shutdown();
	}

	@DisplayName( "It can get a JDBC connection regardless of key casing" )
	@Test
	void testDerbyConnectionFunnyKeyCasing() throws SQLException {
		DataSource myDataSource = DataSource.fromStruct(
		    Key.of( "funkyDB" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:src/test/resources/tmp/DataSourceTests/DataSourceTest;create=true"
		    ) );
		try ( BoxConnection conn = myDataSource.getBoxConnection() ) {
			assertThat( conn ).isInstanceOf( Connection.class );
		}
		myDataSource.shutdown();
	}

	@DisplayName( "It closes datasource connections on shutdown" )
	@Test
	void testDataSourceClose() throws SQLException {

		DataSource		myDataSource	= DataSource.fromStruct(
		    Key.of( "funkyDB" ),
		    Struct.of(
		        "driver", "derby",
		        "username", "user",
		        "password", "password",
		        "connectionString", "jdbc:derby:src/test/resources/tmp/DataSourceTests/DataSourceTest;create=true"
		    ) );
		BoxConnection	conn			= myDataSource.getBoxConnection();
		assertThat( conn ).isInstanceOf( BoxConnection.class );

		myDataSource.shutdown();
		assertThat( conn.isValid( 5 ) ).isFalse();
	}

	@DisplayName( "It can execute simple queries without providing a connection" )
	@Test
	void testDataSourceExecute() {
		try ( BoxConnection conn = datasource.getBoxConnection() ) {
			assertDoesNotThrow( () -> {
				ExecutedQuery executedQuery = datasource.execute( "SELECT * FROM developers", conn, context );
				assertEquals( 4, executedQuery.getRecordCount() );
			} );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	@DisplayName( "It can execute queries with parameters without providing a connection" )
	@Test
	void testDataSourceWithParamsExecute() {
		try ( BoxConnection conn = datasource.getBoxConnection() ) {
			assertDoesNotThrow( () -> {
				ExecutedQuery executedQuery = datasource.execute(
				    "SELECT * FROM developers WHERE name = ?",
				    Array.of( "Michael Born" ),
				    conn,
				    context
				);
				assertEquals( 1, executedQuery.getRecordCount() );
				Query results = executedQuery.getResults();
				assertEquals( 1, results.size() );
				IStruct developer = results.getRowAsStruct( 0 );
				assertEquals( 77, developer.get( "id" ) );
				assertEquals( "Michael Born", developer.get( "name" ) );
			} );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	@DisplayName( "It can execute queries with parameters without providing a connection" )
	@Test
	void testDataSourceWithNamedParamsExecute() {
		try ( BoxConnection conn = datasource.getBoxConnection() ) {
			assertDoesNotThrow( () -> {
				ExecutedQuery executedQuery = datasource.execute(
				    "SELECT * FROM developers WHERE name = :name",
				    Struct.of( "name", "Michael Born" ),
				    conn,
				    context
				);
				assertEquals( 1, executedQuery.getRecordCount() );
				Query results = executedQuery.getResults();
				assertEquals( 1, results.size() );
				IStruct developer = results.getRowAsStruct( 0 );
				assertEquals( 77, developer.get( "id" ) );
				assertEquals( "Michael Born", developer.get( "name" ) );
			} );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	@DisplayName( "It throws an exception if a named param is missing" )
	@Test
	void testDatasourceWithMissingNamedParams() {
		try ( BoxConnection conn = datasource.getBoxConnection() ) {
			DatabaseException exception = assertThrows( DatabaseException.class, () -> {
				datasource.execute(
				    "SELECT * FROM developers WHERE name = :name",
				    Struct.of( "developer", "Michael Born" ),
				    conn,
				    context
				);
			} );
			assertEquals( "Named parameter [:name] not provided to query.", exception.getMessage() );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	@DisplayName( "It can get results in query form" )
	@Test
	void testQueryExecuteQueryResults() {
		ExecutedQuery	executedQuery	= datasource.execute( "SELECT * FROM developers WHERE id=1", context );
		Query			queryResults	= executedQuery.getResults();

		assertNotEquals( 0, queryResults.size() );
		assertThat( queryResults.hasColumn( Key.of( "id" ) ) ).isEqualTo( true );
		assertThat( queryResults.hasColumn( Key.of( "name" ) ) ).isEqualTo( true );

		Object[] firstRow = queryResults.getRow( 0 );
		assert ( firstRow[ 0 ].equals( 1 ) );
		assert ( firstRow[ 1 ].equals( "Luis Majano" ) );
	}

	@DisplayName( "It can get results in query form" )
	@Test
	void testQueryExecuteException() {
		assertThrows( DatabaseException.class, () -> datasource.execute( "SELECT * FROM foobar WHERE id=1", context ) );
	}

	@DisplayName( "It can get results in array form" )
	@Test
	void testQueryExecuteArrayResults() {
		ExecutedQuery	executedQuery	= datasource.execute( "SELECT * FROM developers WHERE id=1", context );
		Array			results			= executedQuery.getResultsAsArray();
		assertNotEquals( 0, results.size() );

		Object theRow = results.get( 0 );
		assert ( theRow instanceof Struct );
		Struct firstRow = ( Struct ) results.get( 0 );
		assert ( firstRow.containsKey( "id" ) );
		assert ( firstRow.containsKey( "name" ) );

		assert ( firstRow.getAsInteger( Key.of( "id" ) ) == 1 );
		assert ( firstRow.getAsString( Key.of( "name" ) ).equals( "Luis Majano" ) );
	}

	@DisplayName( "It can compare datasources" )
	@Test
	void testDataSourceComparison() {
		DataSource	datasource1	= DataSource.fromStruct(
		    Key.of( "funkyDB" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:db1;create=true"
		    )
		);

		DataSource	datasource2	= DataSource.fromStruct(
		    Key.of( "funkyDB" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:db1;create=true"
		    )
		);

		DataSource	datasource3	= DataSource.fromStruct(
		    Key.of( "testDB" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:db1;create=true"
		    )
		);

		DataSource	datasource4	= DataSource.fromStruct(
		    Key.of( "funkyDB" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:db1;create=false"
		    )
		);

		assertThat( datasource1.equals( datasource2 ) ).isEqualTo( true );
		assertThat( datasource1.equals( datasource3 ) ).isEqualTo( false );
		assertThat( datasource3.equals( datasource4 ) ).isEqualTo( false );
	}

	@DisplayName( "It can check authentication" )
	@Test
	void testAuthenticationMatch() {
		DataSource myDSN = DataSource.fromStruct(
		    Key.of( "funkyDB" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:authCheck;create=true",
		        "username", "user",
		        "password", "pa$$w0rd"
		    )
		);

		assertThat( myDSN.isAuthenticationMatch( "user", "password" ) ).isEqualTo( false );
		assertThat( myDSN.isAuthenticationMatch( "user", "pa$$w0rd" ) ).isEqualTo( true );
	}

	@DisplayName( "It can get basic pool statistics" )
	@Test
	void testPoolStats() throws SQLException {
		DataSource	derbyDB	= JDBCTestUtils.buildDatasource( "funkyDB", new Struct() );
		IStruct		stats	= derbyDB.getPoolStats();

		assertThat( stats ).containsKey( Key.of( "pendingThreads" ) );
		assertThat( stats ).containsKey( Key.of( "totalConnections" ) );
		assertThat( stats ).containsKey( Key.of( "activeConnections" ) );
		assertThat( stats ).containsKey( Key.of( "idleConnections" ) );
		assertThat( stats ).containsKey( Key.of( "maxConnections" ) );
		assertThat( stats ).containsKey( Key.of( "minConnections" ) );

		assertEquals( 0, stats.getAsInteger( Key.of( "activeConnections" ) ) );
	}

	@Disabled
	@DisplayName( "It can query a datasource by name" )
	@Test
	void testQueryDataSourceByName() {

		instance.executeSource(
		    """
		    queryExecute(
		    	"CREATE TABLE developers2 (id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY(START WITH 1, INCREMENT BY 1), name VARCHAR(155) NOT NULL)",
		    	{},
		    	{ dataSource: "testDB" }
		    );

		    queryExecute(
		    	"
		    		INSERT INTO developers2 (name)
		    		VALUES ( 'Bob' ),
		    			('Alice' )
		    	",
		    	{},
		    	{ dataSource: "testDB" }
		    );

		    result = queryExecute(
		    	" SELECT count(1) as C from developers2 ",
		    	{},
		    	{ dataSource: "testDB" }
		    ).c;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	// @Disabled( "Test failing; possibly due to modification by reference of maxConnections in configuration" )
	@DisplayName( "It can use a -1 or infinite connection limit" )
	@Test
	void testInfiniteConnections() {
		DataSource	infiniteConnectionDS	= DataSource.fromStruct(
		    "infiniteDB",
		    Struct.of(
		        "database", "infiniteDB",
		        "driver", "derby",
		        "connectionLimit", -1,
		        "connectionString", "jdbc:derby:memory:" + "infiniteDB" + ";create=true"
		    ) );
		Integer		maxPooledConnections	= infiniteConnectionDS.getConfiguration().getProperties().getAsInteger( Key.maxConnections );

		assertThat( maxPooledConnections ).isEqualTo( Integer.MAX_VALUE );
	}

	@Disabled
	@DisplayName( "It can query a datasource by struct" )
	@Test
	void testQueryDataSourceByStruct() {
		instance.executeSource(
		    """
		      queryExecute(
		      	"CREATE TABLE developers3 (id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY(START WITH 1, INCREMENT BY 1), name VARCHAR(155) NOT NULL)",
		      	{},
		      	{ dataSource: {
		    	name: "testDB",
		    	properties: {
		    		connectionString: "jdbc:derby:memory:testDB;create=true"
		    	}
		    } }
		      );

		      queryExecute(
		      	"
		      		INSERT INTO developers3 (name)
		      		VALUES ( 'Bob' ),
		      			('Alice' )
		      	",
		      	{},
		      	{ dataSource: {
		    	name: "testDB",
		    	properties: {
		    		connectionString: "jdbc:derby:memory:testDB;create=true"
		    	}
		    } }
		      );

		      result = queryExecute(
		      	" SELECT count(1) as C from developers3 ",
		      	{},
		      	{ dataSource: {
		    	name: "testDB",
		    	properties: {
		    		connectionString: "jdbc:derby:memory:testDB;create=true"
		    	}
		    } }
		      ).c;
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}
}
