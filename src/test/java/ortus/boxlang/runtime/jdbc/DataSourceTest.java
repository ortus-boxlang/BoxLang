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
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class DataSourceTest {

	static DataSource datasource;

	@BeforeAll
	public static void setUp() {
		datasource = new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:testDatasourceDB;create=true"
		) );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155) )" );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		datasource.shutdown();
	}

	@BeforeEach
	public void resetTable() {
		assertDoesNotThrow( () -> {
			datasource.execute( "TRUNCATE TABLE developers" );
			datasource.execute( "INSERT INTO developers ( id, name ) VALUES ( 77, 'Michael Born' )" );
			datasource.execute( "INSERT INTO developers ( id, name ) VALUES ( 1, 'Luis Majano' )" );
		} );
	}

	@DisplayName( "It can get an Apache Derby JDBC connection" )
	@Test
	void testDerbyConnection() throws SQLException {
		DataSource derbyDB = new DataSource( Struct.of(
		    Key.of( "jdbcUrl" ), "jdbc:derby:memory:funkyDB;create=true"
		) );
		try ( Connection conn = derbyDB.getConnection() ) {
			assertThat( conn ).isInstanceOf( Connection.class );
		}
		derbyDB.shutdown();
	}

	@EnabledIf( "tools.JDBCTestUtils#hasMySQLDriver" )
	@DisplayName( "It can get a MySQL JDBC connection" )
	@Test
	void testMySQLConnection() throws SQLException {
		DataSource	myDataSource	= new DataSource( Struct.of(
		    "username", "root",
		    "password", "secret",
		    "jdbcUrl", "jdbc:mysql://localhost:3306"
		) );
		Connection	conn			= myDataSource.getConnection();
		assertThat( conn ).isInstanceOf( Connection.class );
	}

	@Disabled( "Need to implement this!" )
	@DisplayName( "It can get a JDBC connection regardless of key casing" )
	@Test
	void testDerbyConnectionFunnyKeyCasing() throws SQLException {
		DataSource	funkyDataSource	= new DataSource( Struct.of(
		    "JDBCurl", "jdbc:derby:src/test/resources/tmp/DataSourceTests/testDB;create=true"
		) );
		Connection	conn			= funkyDataSource.getConnection();
		assertThat( conn ).isInstanceOf( Connection.class );
	}

	@DisplayName( "It closes datasource connections on shutdown" )
	@Test
	void testDataSourceClose() throws SQLException {
		DataSource	myDataSource	= new DataSource( Struct.of(
		    "username", "user",
		    "password", "password",
		    "jdbcUrl", "jdbc:derby:src/test/resources/tmp/DataSourceTests/testDB;create=true"
		) );
		Connection	conn			= myDataSource.getConnection();
		assertThat( conn ).isInstanceOf( Connection.class );

		myDataSource.shutdown();
		assertThat( conn.isValid( 5 ) ).isFalse();
	}

	@DisplayName( "It can execute simple queries without providing a connection" )
	@Test
	void testDataSourceExecute() {
		try ( Connection conn = datasource.getConnection() ) {
			assertDoesNotThrow( () -> {
				ExecutedQuery executedQuery = datasource.execute( "SELECT * FROM developers", conn );
				assertEquals( 2, executedQuery.getRecordCount() );
			} );
		} catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

	@DisplayName( "It can execute queries with parameters without providing a connection" )
	@Test
	void testDataSourceWithParamsExecute() {
		try ( Connection conn = datasource.getConnection() ) {
			assertDoesNotThrow( () -> {
				ExecutedQuery executedQuery = datasource.execute(
				    "SELECT * FROM developers WHERE name = ?",
				    Array.of( "Michael Born" ),
				    conn
				);
				assertEquals( 1, executedQuery.getRecordCount() );
				Query results = executedQuery.getResults();
				assertEquals( 1, results.size() );
				IStruct developer = results.getRowAsStruct( 0 );
				assertEquals( 77, developer.get( "id" ) );
				assertEquals( "Michael Born", developer.get( "name" ) );
			} );
		} catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

	@DisplayName( "It can execute queries with parameters without providing a connection" )
	@Test
	void testDataSourceWithNamedParamsExecute() {
		try ( Connection conn = datasource.getConnection() ) {
			assertDoesNotThrow( () -> {
				ExecutedQuery executedQuery = datasource.execute(
				    "SELECT * FROM developers WHERE name = :name",
				    Struct.of( "name", "Michael Born" ),
				    conn
				);
				assertEquals( 1, executedQuery.getRecordCount() );
				Query results = executedQuery.getResults();
				assertEquals( 1, results.size() );
				IStruct developer = results.getRowAsStruct( 0 );
				assertEquals( 77, developer.get( "id" ) );
				assertEquals( "Michael Born", developer.get( "name" ) );
			} );
		} catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

	@DisplayName( "It throws an exception if a named param is missing" )
	@Test
	void testDatasourceWithMissingNamedParams() {
		try ( Connection conn = datasource.getConnection() ) {
			BoxRuntimeException exception = assertThrows( BoxRuntimeException.class, () -> {
				datasource.execute(
				    "SELECT * FROM developers WHERE name = :name",
				    Struct.of( "developer", "Michael Born" ),
				    conn
				);
			} );
			assertEquals( "Missing param in query: [name]. SQL: SELECT * FROM developers WHERE name = :name", exception.getMessage() );
		} catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

	@DisplayName( "It can get results in query form" )
	@Test
	void testQueryExecuteQueryResults() {
		ExecutedQuery	executedQuery	= datasource.execute( "SELECT * FROM developers WHERE id=1" );
		Query			queryResults	= executedQuery.getResults();

		assertNotEquals( 0, queryResults.size() );
		assertTrue( queryResults.hasColumn( Key.of( "id" ) ) );
		assertTrue( queryResults.hasColumn( Key.of( "name" ) ) );

		Object[] firstRow = queryResults.getRow( 0 );
		assert ( firstRow[ 0 ].equals( 1 ) );
		assert ( firstRow[ 1 ].equals( "Luis Majano" ) );
	}

	@DisplayName( "It can get results in query form" )
	@Test
	void testQueryExecuteException() {
		assertThrows( DatabaseException.class, () -> datasource.execute( "SELECT * FROM foobar WHERE id=1" ) );
	}

	@DisplayName( "It can get results in array form" )
	@Test
	void testQueryExecuteArrayResults() {
		ExecutedQuery	executedQuery	= datasource.execute( "SELECT * FROM developers WHERE id=1" );
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

	@DisplayName( "It can retrieve the generated keys from an insert query" )
	@Test
	void testGeneratedKeysOnInsert() {
		try ( Connection conn = datasource.getConnection() ) {
			assertDoesNotThrow( () -> {
				datasource.execute(
				    "CREATE TABLE developers2 (id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY(START WITH 1, INCREMENT BY 1), name VARCHAR(155) NOT NULL)" );
				ExecutedQuery executedQuery = datasource.execute( "INSERT INTO developers2 (name) VALUES ('Eric Peterson')", conn );
				assertEquals( 0, executedQuery.getRecordCount() );
				BigDecimal generatedKey = ( BigDecimal ) executedQuery.getGeneratedKey();
				assert generatedKey != null;
				assertEquals( 1, generatedKey.intValue() );
			} );
		} catch ( SQLException e ) {
			throw new RuntimeException( e );
		} finally {
			datasource.execute( "DROP TABLE developers2" );
		}
	}

	@DisplayName( "It can execute queries in a transaction, with or without providing a specific connection" )
	@Test
	void testTransactionalQueryExecute() throws SQLException {
		assertDoesNotThrow( () -> {
			datasource.executeTransactionally( new String[] {
			    "INSERT INTO developers (id) VALUES ( 11 )",
			    "INSERT INTO developers (id) VALUES ( 12 )"
			} );
		} );
		assertDoesNotThrow( () -> {
			datasource.executeTransactionally( new String[] {
			    "INSERT INTO developers (id) VALUES ( 13 )",
			    "INSERT INTO developers (id) VALUES ( 14 )"
			}, datasource.getConnection() );
		} );

		Connection conn = datasource.getConnection();
		datasource.executeTransactionally( new String[] {
		    "INSERT INTO developers (id) VALUES ( 15 )",
		    "INSERT INTO developers (id) VALUES ( 16 )"
		}, conn );

		// In the case where we pass in our own connection, it is up to us to close it.
		assertFalse( conn.isClosed(), "Caller-provided connection should NOT be closed on completion" );
	}
}
