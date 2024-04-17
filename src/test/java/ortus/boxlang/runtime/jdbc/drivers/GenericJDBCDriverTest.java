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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.scopes.Key;

public class GenericJDBCDriverTest {

	@Test
	@DisplayName( "Test getName()" )
	public void testGetName() {
		GenericJDBCDriver	driver			= new GenericJDBCDriver();
		Key					expectedName	= new Key( "Generic" );
		assertThat( driver.getName() ).isEqualTo( expectedName );
	}

	@Test
	@DisplayName( "Test getType()" )
	public void testGetType() {
		GenericJDBCDriver	driver			= new GenericJDBCDriver();
		DatabaseDriverType	expectedType	= DatabaseDriverType.GENERIC;
		assertThat( driver.getType() ).isEqualTo( expectedType );
	}

	@Test
	@DisplayName( "Test getClassName()" )
	public void testGetClassName() {
		GenericJDBCDriver	driver				= new GenericJDBCDriver();
		String				expectedClassName	= "";
		assertThat( driver.getClassName() ).isEqualTo( expectedClassName );
	}

	@Test
	@DisplayName( "Test buildConnectionURL()" )
	public void testBuildConnectionURL() {
		GenericJDBCDriver	driver	= new GenericJDBCDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "driver", "mysql" );
		config.properties.put( "database", "mydb" );
		config.properties.put( "port", 3306 );
		config.properties.put( "host", "localhost" );
		config.properties.put( "custom", "ssl=true" );

		String expectedURL = "jdbc:mysql://localhost:3306/mydb?ssl=true";
		assertThat( driver.buildConnectionURL( config ) ).isEqualTo( expectedURL );
	}

	@DisplayName( "Throw an exception if the driver is not found" )
	@Test
	public void testBuildConnectionURLNoDriver() {
		GenericJDBCDriver	driver	= new GenericJDBCDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "database", "mydb" );
		config.properties.put( "port", 3306 );
		config.properties.put( "host", "localhost" );
		config.properties.put( "custom", "ssl=true" );

		try {
			driver.buildConnectionURL( config );
		} catch ( IllegalArgumentException e ) {
			assertThat( e.getMessage() ).isEqualTo( "The driver property is required for the Generic JDBC Driver" );
		}
	}

	@DisplayName( "Throw an exception if the database is not found" )
	@Test
	public void testBuildConnectionURLNoDatabase() {
		GenericJDBCDriver	driver	= new GenericJDBCDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "driver", "mysql" );
		config.properties.put( "port", 3306 );
		config.properties.put( "host", "localhost" );
		config.properties.put( "custom", "ssl=true" );

		try {
			driver.buildConnectionURL( config );
		} catch ( IllegalArgumentException e ) {
			assertThat( e.getMessage() ).isEqualTo( "The database property is required for the Generic JDBC Driver" );
		}
	}

	@DisplayName( "Throw an exception if the port is not found" )
	@Test
	public void testBuildConnectionURLNoPort() {
		GenericJDBCDriver	driver	= new GenericJDBCDriver();
		DatasourceConfig	config	= new DatasourceConfig();
		config.properties.put( "driver", "mysql" );
		config.properties.put( "database", "mydb" );
		config.properties.put( "host", "localhost" );
		config.properties.put( "custom", "ssl=true" );

		try {
			driver.buildConnectionURL( config );
		} catch ( IllegalArgumentException e ) {
			assertThat( e.getMessage() ).isEqualTo( "The port property is required for the Generic JDBC Driver" );
		}
	}

}
