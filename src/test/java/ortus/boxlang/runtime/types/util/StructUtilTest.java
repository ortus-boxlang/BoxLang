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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
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

}
