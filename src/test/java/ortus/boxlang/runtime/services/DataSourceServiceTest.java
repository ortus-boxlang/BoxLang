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

package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class DataSourceServiceTest {

	static BoxRuntime			runtime;
	static DatasourceService	service;
	static Key					datasourceName;

	@BeforeAll
	public static void setUp() {
		runtime	= BoxRuntime.getInstance( true );
		service	= new DatasourceService( runtime );
		service.onConfigurationLoad();
		datasourceName = Key.of( "foobar" );
	}

	@BeforeEach
	public void setupEach() {
		service.clear();
	}

	@DisplayName( "It throws on invalid datasource connection URLs" )
	@Test
	void testThrow() {
		IStruct properties = Struct.of(
		    "driver", "other",
		    "connectionString", "jdbc:foobar:myDB"
		);
		assertThrows( BoxRuntimeException.class, () -> service.register( Key.of( "testIt" ), properties ) );
	}

	@DisplayName( "It throws on invalid datasource when no driver is passed" )
	@Test
	void testThrowIfNoDriver() {
		IStruct properties = Struct.of(
		    "connectionString", "jdbc:foobar:myDB"
		);
		assertThrows( BoxRuntimeException.class, () -> service.register( Key.of( "Invalid" ), properties ) );
	}

	@DisplayName( "It can set and get datasources with normal configurations" )
	@Test
	void testRegisterDataSource() {
		DataSource dsn = service.register(
		    Key.of( "foobar" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:DataSourceServiceTest;create=true"
		    )
		);

		assertThat( service.get( dsn.getUniqueName() ) ).isInstanceOf( DataSource.class );
		assertThat( dsn.equals( dsn ) ).isTrue();
	}

	@DisplayName( "It can get all datasource names" )
	@Test
	void testGetAllDatasourceNames() {
		DataSource dsn = service.register(
		    Key.of( "foobar" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:DataSourceServiceTest;create=true"
		    )
		);

		assertThat( service.getNames() ).asList().containsExactly( dsn.getUniqueName().getName() );
	}

	@DisplayName( "It can remove a datasource that doesn't exist" )
	@Test
	void testRemoveNonExistentDatasource() {
		assertThat( service.remove( datasourceName ) ).isFalse();
	}

	@DisplayName( "It can remove a valid datasource and shut it down" )
	@Test
	void testRemoveDatasource() throws SQLException {
		DataSource	dsn			= service.register(
		    Key.of( "foobar" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:DataSourceServiceTest;create=true"
		    )
		);

		Connection	connection	= dsn.getConnection();
		assertThat( service.remove( dsn.getUniqueName() ) ).isTrue();
		assertThat( connection.isValid( 1 ) ).isFalse();
	}

	@DisplayName( "It can clear all registered datasources" )
	@Test
	void testClear() throws SQLException {
		assertThat( service.size() ).isEqualTo( 0 );
		DataSource dsn = service.register(
		    Key.of( "foobar" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:DataSourceServiceTest;create=true"
		    )
		);

		assertThat( service.has( dsn.getUniqueName() ) ).isTrue();
		Connection connection = dsn.getConnection();
		assertThat( connection ).isInstanceOf( Connection.class );

		service.clear();
		assertThat( service.has( dsn.getUniqueName() ) ).isFalse();
		// The manager should close datasources, connection pools, and connections upon calling .clear()
		assertThat( connection.isValid( 1 ) ).isFalse();
	}

	@DisplayName( "Verify the generic driver has been installed" )
	@Test
	void testGenericDriver() {
		assertThat( service.driverSize() ).isEqualTo( 0 );

		service.onStartup();

		assertThat( service.driverSize() ).isEqualTo( 1 );
		assertThat( service.getDriverNames() ).asList().containsExactly( "Generic" );
	}

	@DisplayName( "It can remove a driver" )
	@Test
	void testRemoveDriver() {
		service.onStartup();
		assertThat( service.driverSize() ).isEqualTo( 1 );
		assertThat( service.removeDriver( Key.of( "Generic" ) ) ).isTrue();
		assertThat( service.driverSize() ).isEqualTo( 0 );
	}

	@DisplayName( "It can clear all registered drivers" )
	@Test
	void testClearDrivers() {
		service.onStartup();
		assertThat( service.driverSize() ).isEqualTo( 1 );
		service.clearDrivers();
		assertThat( service.driverSize() ).isEqualTo( 0 );
	}

}
