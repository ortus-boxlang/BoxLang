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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

public class DataSourceTest {

	static DataSource datasource;

	@BeforeAll
	public static void setUp() {
		datasource = DataSource.fromStruct( Struct.fromMap( Map.of(
		    Key.driver, "derby",
		    Key.URL, "jdbc:derby:src/test/resources/tmp/testDB;create=true"
		) ) );
	}

	@BeforeEach
	public void setupEach() {
	}

	@Disabled( "Need ability to enable/disable tests based on available third-party services." )
	@DisplayName( "It can get a MySQL JDBC connection" )
	@Test
	void testMySQLConnection() throws SQLException {
		DataSource	myDatasource	= DataSource.fromStruct( Struct.fromMap( Map.of(
		    Key.driver, "mysql",
		    Key.username, "root",
		    Key.password, "secret",
		    Key.databaseName, "test",
		    Key.URL, "jdbc:mysql://localhost:3306"
		) ) );
		Connection	conn			= myDatasource.getConnection();
		assertThat( conn ).isInstanceOf( Connection.class );
	}

	@DisplayName( "It can get an Apache Derby JDBC connection" )
	@Test
	void testDerbyConnection() throws SQLException {
		Connection conn = datasource.getConnection();
		assertThat( conn ).isInstanceOf( Connection.class );
	}

	@DisplayName( "It closes datasource connections on shutdown" )
	@Test
	void testDatasourceClose() throws SQLException {
		DataSource	myDatasource	= DataSource.fromStruct( Struct.fromMap( Map.of(
		    Key.driver, "derby",
		    Key.username, "user",
		    Key.password, "password",
		    Key.databaseName, "test",
		    Key.URL, "jdbc:derby:src/test/resources/tmp/testDB;create=true"
		) ) );
		Connection	conn			= myDatasource.getConnection();
		assertThat( conn ).isInstanceOf( Connection.class );

		myDatasource.shutdown();
		assertThat( conn.isValid( 5 ) ).isFalse();
	}

	@DisplayName( "It can execute queries in a transaction without providing a connection" )
	@Test
	void testTransactionalQueryExecuteNoConn() {
		datasource.executeTransactionally(
		    new String[] {
		        "CREATE TABLE foo (id INTEGER)",
		        "INSERT INTO foo (id) VALUES ( 1 )"
		    }
		);
	}

	@DisplayName( "It can execute queries in a transaction, with providing a specific connection" )
	@Test
	void testTransactionalQueryExecuteWithConn() {
		datasource.executeTransactionally(
		    new String[] {
		        "CREATE TABLE foobar (id INTEGER)",
		        "INSERT INTO foobar (id) VALUES ( 1 )"
		    },
		    datasource.getConnection()
		);
		// assertThat()
	}
}
