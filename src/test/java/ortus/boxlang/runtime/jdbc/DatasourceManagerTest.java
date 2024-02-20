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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class DatasourceManagerTest {

	static DataSourceManager	manager;

	static Key					datasourceName;

	@BeforeAll
	public static void setUp() {
		manager			= DataSourceManager.getInstance();
		datasourceName	= Key.of( "foobar" );
	}

	@BeforeEach
	public void setupEach() {
	}

	@DisplayName( "It throws on invalid datasource connection URLs" )
	@Test
	void testThrow() {
		IStruct config = Struct.of(
		    "jdbcUrl", "jdbc:foobar:myDB"
		);
		assertThrows( RuntimeException.class, () -> {
			manager.registerDatasource( datasourceName, config );
		} );
	}

	@DisplayName( "It can clear all registered datasources" )
	@Test
	void testClear() throws SQLException {
		manager.registerDatasource( datasourceName, Struct.of(
		    "jdbcUrl", "jdbc:derby:src/test/resources/tmp/DataSourceTests/testDB;create=true"
		) );
		assertThat( manager.getDatasource( datasourceName ) ).isInstanceOf( DataSource.class );
		Connection connection = manager.getDatasource( datasourceName ).getConnection();
		assertThat( connection ).isInstanceOf( Connection.class );
		manager.clear();
		assertThat( manager.getDatasource( datasourceName ) ).isNull();
		// The manager should close datasources, connection pools, and connections upon calling .clear()
		assertThat( connection.isValid( 1 ) ).isFalse();
	}

	@DisplayName( "It can set and get datasources by name" )
	@Test
	void testRegisterDatasource() {
		assertThat( manager.getDatasource( datasourceName ) ).isNull();
		manager.registerDatasource( datasourceName, Struct.of(
		    "jdbcUrl", "jdbc:derby:src/test/resources/tmp/DataSourceTests/testDB;create=true"
		) );
		assertThat( manager.getDatasource( datasourceName ) ).isInstanceOf( DataSource.class );
	}

	@DisplayName( "It can get the default datasource" )
	@Test
	void testDefaultDatasource() {
		assertThat( manager.getDefaultDatasource() ).isNull();
		DataSource defaultDatasource = new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:src/test/resources/tmp/DataSourceTests/testDB;create=true"
		) );
		manager.setDefaultDatasource( defaultDatasource );

		assertThat( manager.getDefaultDatasource() ).isEqualTo( defaultDatasource );
		assertThat( manager.getDatasource( Key._DEFAULT ) ).isEqualTo( defaultDatasource );
	}
}
