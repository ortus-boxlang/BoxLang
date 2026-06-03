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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

class QueriesConfigTest {

	@BeforeAll
	public static void setUp() {
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "It can create a QueriesConfig with defaults" )
	@Test
	void testDefaultConstructor() {
		QueriesConfig config = new QueriesConfig();

		assertThat( config.timeout ).isEqualTo( 0 );
		assertThat( config.returnType ).isEqualTo( "query" );
		assertThat( config.fetchSize ).isEqualTo( 0 );
		assertThat( config.maxrows ).isEqualTo( 0 );
		assertThat( config.cacheProvider ).isEqualTo( "default" );
	}

	@DisplayName( "It can process a configuration struct" )
	@Test
	void testProcess() {
		QueriesConfig	config	= new QueriesConfig();
		IStruct			input	= Struct.of(
		    Key.timeout, 60,
		    Key.returnType, "array",
		    Key.fetchSize, 500,
		    Key.maxRows, 2000,
		    Key.cacheProvider, "redis"
		);

		config.process( input );

		assertThat( config.timeout ).isEqualTo( 60 );
		assertThat( config.returnType ).isEqualTo( "array" );
		assertThat( config.fetchSize ).isEqualTo( 500 );
		assertThat( config.maxrows ).isEqualTo( 2000 );
		assertThat( config.cacheProvider ).isEqualTo( "redis" );
	}

	@DisplayName( "It can process partial configuration" )
	@Test
	void testProcessPartial() {
		QueriesConfig	config	= new QueriesConfig();
		IStruct			input	= Struct.of(
		    Key.timeout, 120,
		    Key.returnType, "struct"
		);

		config.process( input );

		assertThat( config.timeout ).isEqualTo( 120 );
		assertThat( config.returnType ).isEqualTo( "struct" );
		// Defaults should remain
		assertThat( config.fetchSize ).isEqualTo( 0 );
		assertThat( config.maxrows ).isEqualTo( 0 );
		assertThat( config.cacheProvider ).isEqualTo( "default" );
	}

	@DisplayName( "It rejects invalid returnType values" )
	@Test
	void testInvalidReturnType() {
		QueriesConfig	config	= new QueriesConfig();
		IStruct			input	= Struct.of(
		    Key.returnType, "invalid"
		);

		assertThrows( BoxValidationException.class, () -> config.process( input ) );
	}

	@DisplayName( "It accepts all valid returnType values" )
	@Test
	void testValidReturnTypes() {
		QueriesConfig config;

		// Test "query"
		config = new QueriesConfig();
		config.process( Struct.of( Key.returnType, "query" ) );
		assertThat( config.returnType ).isEqualTo( "query" );

		// Test "array"
		config = new QueriesConfig();
		config.process( Struct.of( Key.returnType, "array" ) );
		assertThat( config.returnType ).isEqualTo( "array" );

		// Test "struct"
		config = new QueriesConfig();
		config.process( Struct.of( Key.returnType, "struct" ) );
		assertThat( config.returnType ).isEqualTo( "struct" );
	}

	@DisplayName( "It can convert to struct" )
	@Test
	void testAsStruct() {
		QueriesConfig config = new QueriesConfig();
		config.timeout			= 45;
		config.returnType		= "array";
		config.fetchSize		= 250;
		config.maxrows			= 5000;
		config.cacheProvider	= "custom";

		IStruct result = config.asStruct();

		assertThat( result ).isNotNull();
		assertThat( result.get( Key.timeout ) ).isEqualTo( 45 );
		assertThat( result.get( Key.returnType ) ).isEqualTo( "array" );
		assertThat( result.get( Key.fetchSize ) ).isEqualTo( 250 );
		assertThat( result.get( Key.maxRows ) ).isEqualTo( 5000 );
		assertThat( result.get( Key.cacheProvider ) ).isEqualTo( "custom" );
	}

	@DisplayName( "It handles empty configuration struct" )
	@Test
	void testEmptyConfig() {
		QueriesConfig	config	= new QueriesConfig();
		IStruct			input	= Struct.of();

		config.process( input );

		// All defaults should remain
		assertThat( config.timeout ).isEqualTo( 0 );
		assertThat( config.returnType ).isEqualTo( "query" );
		assertThat( config.fetchSize ).isEqualTo( 0 );
		assertThat( config.maxrows ).isEqualTo( 0 );
		assertThat( config.cacheProvider ).isEqualTo( "default" );
	}

}
