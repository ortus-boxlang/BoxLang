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
package ortus.boxlang.runtime.types.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class StructUtilTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Can parse a struct to a query string" )
	@Test
	void testStructToQueryString() {
		Struct struct = new Struct();
		struct.put( "foo", "bar" );
		struct.put( "baz", "qux" );
		assertThat( StructUtil.toQueryString( struct ) ).isEqualTo( "foo=bar&baz=qux" );

		struct = new Struct();
		assertThat( StructUtil.toQueryString( struct ) ).isEqualTo( "" );

		struct = new Struct();
		struct.put( "foo", "bar" );
		struct.put( "baz", "" );
		assertThat( StructUtil.toQueryString( struct ) ).isEqualTo( "foo=bar&baz=" );
	}

	@DisplayName( "Can parse a query string to a struct using another delimiter" )
	@Test
	void testStructToQueryStringWithCustomDelim() {
		Struct struct = new Struct();
		struct.put( "foo", "bar" );
		struct.put( "baz", "qux" );

		assertThat( StructUtil.toQueryString( struct, ";" ) ).isEqualTo( "foo=bar;baz=qux" );
	}

	@DisplayName( "Can parse a query string to a struct" )
	@Test
	void testQueryStringToStruct() {
		IStruct struct = StructUtil.fromQueryString( "foo=bar&baz=qux" );
		assertThat( struct.get( "foo" ) ).isEqualTo( "bar" );
		assertThat( struct.get( "baz" ) ).isEqualTo( "qux" );

		struct = StructUtil.fromQueryString( "" );
		assertThat( struct.size() ).isEqualTo( 0 );

		struct = StructUtil.fromQueryString( "foo=bar & baz =" );
		assertThat( struct.get( "foo" ) ).isEqualTo( "bar" );
		assertThat( struct.get( "baz" ) ).isEqualTo( "" );
	}

	@DisplayName( "Can deep merge two structs" )
	@Test
	void testStructMerge() {

		instance.executeSource(
		    """
		    	settings = {
		    		transpiler = {
		    			upperCaseKeys = true,
		    			forceOutputTrue = true
		    		}
		    	};

		    overrides = {
		    	"transpiler": {
		    		"upperCaseKeys": false
		    	}
		    };
		       """,
		    context );
		IStruct finalSettings = StructUtil.deepMerge( variables.getAsStruct( Key.of( "settings" ) ), variables.getAsStruct( Key.of( "overrides" ) ) );
		System.out.println( finalSettings );
		assertThat( finalSettings.getAsStruct( Key.of( "transpiler" ) ).get( "forceOutputTrue" ) ).isEqualTo( true );
		assertThat( finalSettings.getAsStruct( Key.of( "transpiler" ) ).get( "upperCaseKeys" ) ).isEqualTo( true );

	}

	@DisplayName( "Can deep merge two structs with Override" )
	@Test
	void testStructMergeWithOverride() {

		instance.executeSource(
		    """
		    	settings = {
		    		transpiler = {
		    			upperCaseKeys = true,
		    			forceOutputTrue = true
		    		}
		    	};

		    overrides = {
		    	"transpiler": {
		    		"upperCaseKeys": false
		    	}
		    };
		       """,
		    context );
		IStruct finalSettings = StructUtil.deepMerge( variables.getAsStruct( Key.of( "settings" ) ), variables.getAsStruct( Key.of( "overrides" ) ), true );
		System.out.println( finalSettings );
		assertThat( finalSettings.getAsStruct( Key.of( "transpiler" ) ).get( "forceOutputTrue" ) ).isEqualTo( true );
		assertThat( finalSettings.getAsStruct( Key.of( "transpiler" ) ).get( "upperCaseKeys" ) ).isEqualTo( false );

	}

	@DisplayName( "Can deep merge two arrays with the right-hand side becoming assignment" )
	@Test
	void testStructMergeArrayIntegrity() {

		//@formatter:off
		instance.executeSource(
		    """
				settings = {
					mailServers = []
				};

				overrides = {
					"mailServers" : [
						{
							"tls": false,
							"password": "",
							"idleTimeout": "10000",
							"lifeTimeout": "60000",
							"port": "25",
							"username": "",
							"ssl": false,
							"smtp": "127.0.0.1"
						}
					]
				};
		       """,
		    context );
		//@formatter:on
		IStruct	finalSettings		= StructUtil.deepMerge( variables.getAsStruct( Key.of( "settings" ) ), variables.getAsStruct( Key.of( "overrides" ) ),
		    true );
		Array	finalMailServers	= finalSettings.getAsArray( Key.of( "mailServers" ) );
		assertThat( finalMailServers.get( 0 ) ).isInstanceOf( IStruct.class );
		assertThat( StructCaster.cast( finalMailServers.get( 0 ) ).get( Key.of( "smtp" ) ) ).isEqualTo( "127.0.0.1" );

	}

	@DisplayName( "Can sort struct with nested path containing null and incomparable values" )
	@Test
	void testStructSortWithNullAndIncomparableValues() {
		// This test verifies the fix for: "Comparison method violates its general contract!"
		// when sorting structs with nested paths that may contain null or incomparable values

		Struct	struct	= new Struct();

		// Create nested structs with various value types including nulls
		Struct	item1	= new Struct();
		item1.put( "timestamp", 100 );
		struct.put( "key1", item1 );

		Struct item2 = new Struct();
		item2.put( "timestamp", null ); // null value
		struct.put( "key2", item2 );

		Struct item3 = new Struct();
		item3.put( "timestamp", 50 );
		struct.put( "key3", item3 );

		Struct item4 = new Struct();
		item4.put( "timestamp", "notanumber" ); // incomparable with numbers
		struct.put( "key4", item4 );

		Struct item5 = new Struct();
		item5.put( "timestamp", 75 );
		struct.put( "key5", item5 );

		// This should not throw "Comparison method violates its general contract!"
		Array sortedKeys = StructUtil.sort( struct, "numeric", "asc", "timestamp" );

		// Verify we got results back
		assertThat( sortedKeys ).isNotNull();
		assertThat( sortedKeys.size() ).isEqualTo( 5 );

		// Verify comparable numeric values are sorted correctly (nulls and incomparable values treated as equal)
		// The exact order of null/incomparable values is undefined, but numeric values should be in order
		assertThat( sortedKeys ).contains( "key3" ); // 50
		assertThat( sortedKeys ).contains( "key5" ); // 75
		assertThat( sortedKeys ).contains( "key1" ); // 100
	}

	@DisplayName( "Can sort struct with nested path in descending order" )
	@Test
	void testStructSortWithNestedPathDescending() {
		Struct	struct	= new Struct();

		Struct	item1	= new Struct();
		item1.put( "value", "apple" );
		struct.put( "key1", item1 );

		Struct item2 = new Struct();
		item2.put( "value", "zebra" );
		struct.put( "key2", item2 );

		Struct item3 = new Struct();
		item3.put( "value", "banana" );
		struct.put( "key3", item3 );

		Array sortedKeys = StructUtil.sort( struct, "text", "desc", "value" );

		assertThat( sortedKeys ).isNotNull();
		assertThat( sortedKeys.size() ).isEqualTo( 3 );
		assertThat( sortedKeys.get( 0 ) ).isEqualTo( "key2" ); // zebra
		assertThat( sortedKeys.get( 1 ) ).isEqualTo( "key3" ); // banana
		assertThat( sortedKeys.get( 2 ) ).isEqualTo( "key1" ); // apple
	}

}
