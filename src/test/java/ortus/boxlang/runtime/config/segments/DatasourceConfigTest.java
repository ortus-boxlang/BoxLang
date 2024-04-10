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

package ortus.boxlang.runtime.config.segments;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

class DatasourceConfigTest {

	@DisplayName( "It can generate hikari config" )
	@Test
	void testItCanGenerateHikariConfig() {
		DatasourceConfig	datasource		= new DatasourceConfig( Key.of( "Foo" ), Key.of( "Derby" ), Struct.of(
		    "connectionString", "jdbc:postgresql://localhost:5432/foo"
		) );
		HikariConfig		hikariConfig	= datasource.toHikariConfig();

		assert hikariConfig.getJdbcUrl().equals( "jdbc:postgresql://localhost:5432/foo" );
	}

	@DisplayName( "It can load config" )
	@Test
	void testItCanConstructConnectionString() {
		DatasourceConfig	datasource		= new DatasourceConfig( Key.of( "Foo" ), Key.of( "Derby" ), Struct.of(
		    "driver", "mysql",
		    "host", "127.0.0.1",
		    "port", 3306,
		    "database", "foo",
		    "custom", "useSSL=false"
		) );
		HikariConfig		hikariConfig	= datasource.toHikariConfig();

		assertEquals( "jdbc:mysql://127.0.0.1:3306/foo?useSSL=false", hikariConfig.getJdbcUrl() );
	}

	@DisplayName( "It can load config" )
	@Test
	void testItCanConstructMinimalConnectionString() {
		DatasourceConfig	datasource		= new DatasourceConfig( Key.of( "Foo" ), Key.of( "Derby" ), Struct.of(
		    "driver", "postgresql",
		    "host", "127.0.0.1",
		    "port", 5432
		) );
		HikariConfig		hikariConfig	= datasource.toHikariConfig();

		assertEquals( "jdbc:postgresql://127.0.0.1:5432/?", hikariConfig.getJdbcUrl() );
	}

	@DisplayName( "It can create a unique name for the datasource" )
	@Test
	void testItCanCreateUniqueName() {
		DatasourceConfig	datasource	= new DatasourceConfig( Key.of( "Foo" ), Key.of( "Derby" ), Struct.of(
		    "driver", "postgresql",
		    "host", "localhost",
		    "port", 5432,
		    "database", "foo",
		    "custom", "useSSL=false"
		) );

		Key					name		= datasource.getUniqueName();

		assertThat( name.getName() ).contains( "bx_" );
		assertThat( name.getName() ).contains( "_Foo_" );
		// third element should be a hashcode
		assertThat( name.getName().split( "_" )[ 2 ] ).matches( "\\d+" );
	}

}
