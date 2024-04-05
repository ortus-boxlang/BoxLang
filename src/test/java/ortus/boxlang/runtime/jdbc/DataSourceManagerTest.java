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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class DataSourceManagerTest {

	static DataSourceManager	manager;

	static Key					datasourceName;

	@BeforeAll
	public static void setUp() {
		manager			= new DataSourceManager();
		datasourceName	= Key.of( "foobar" );
	}

	@BeforeEach
	public void setupEach() {
	}

	@DisplayName( "It throws on invalid datasource connection URLs" )
	@Test
	void testThrow() {
		IStruct config = Struct.of(
		    "connectionString", "jdbc:foobar:myDB"
		);
		assertThrows( RuntimeException.class, () -> manager.registerDataSource( datasourceName, config ) );
	}

	@DisplayName( "It can set and get datasources by name" )
	@Test
	void testRegisterDataSource() {
		assertThat( manager.getDataSource( datasourceName ) ).isNull();
		manager.registerDataSource( datasourceName, Struct.of(
		    "connectionString", "jdbc:derby:memory:DataSourceManagerTest;create=true"
		) );
		assertThat( manager.getDataSource( datasourceName ) ).isInstanceOf( DataSource.class );
	}

	@DisplayName( "It can get the default datasource" )
	@Test
	void testDefaultDataSource() {
		assertThat( manager.getDefaultDataSource() ).isNull();
		DataSource defaultDataSource = DataSource.fromDataSourceStruct( Struct.of(
		    "connectionString", "jdbc:derby:memory:DataSourceManagerTest;create=true"
		) );
		manager.setDefaultDataSource( defaultDataSource );

		assertThat( manager.getDefaultDataSource() ).isEqualTo( defaultDataSource );
		assertThat( manager.getDataSource( Key._DEFAULT ) ).isEqualTo( defaultDataSource );
	}

	@DisplayName( "It can clear all registered datasources" )
	@Test
	void testClear() throws SQLException {
		manager.registerDataSource( datasourceName, Struct.of(
		    "connectionString", "jdbc:derby:memory:DataSourceManagerTest;create=true"
		) );
		DataSource datasource = manager.getDataSource( datasourceName );
		assertThat( datasource ).isInstanceOf( DataSource.class );
		assert datasource != null;
		Connection connection = datasource.getConnection();
		assertThat( connection ).isInstanceOf( Connection.class );
		manager.clear( true );
		assertThat( manager.getDataSource( datasourceName ) ).isNull();
		// The manager should close datasources, connection pools, and connections upon calling .clear()
		assertThat( connection.isValid( 1 ) ).isFalse();
	}

	@DisplayName( "It can shut down" )
	@Disabled( "Disabled due to problems with async tests" )
	@Test
	void testShutdown() throws SQLException {
		manager.registerDataSource( datasourceName, Struct.of(
		    "connectionString", "jdbc:derby:memory:DataSourceManagerTest;create=true"
		) );
		DataSource datasource = manager.getDataSource( datasourceName );
		assertThat( datasource ).isInstanceOf( DataSource.class );
		assert datasource != null;
		Connection connection = datasource.getConnection();
		assertThat( connection ).isInstanceOf( Connection.class );
		// manager.shutdown();
		assertThat( manager.getDataSource( datasourceName ) ).isNull();
		// The manager should close datasources, connection pools, and connections upon calling .clear()
		assertThat( connection.isValid( 1 ) ).isFalse();
	}
}
