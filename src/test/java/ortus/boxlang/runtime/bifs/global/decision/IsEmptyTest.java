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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class IsEmptyTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It detects empty values" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    emptyString           = isEmpty( "" );
		    emptyArray            = isEmpty( [] );
		    emptyStruct           = isEmpty( {} );
		      """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "emptyString" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "emptyArray" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "emptyStruct" ), false ) ).isTrue();
	}

	// @TODO: Re-enable when query support is added
	@Disabled( "No query support yet" )
	@Test
	public void testOnQueryObjects() {

		instance.executeSource(
		    """
		    // emptyQueryNoColumns   = isEmpty( queryNew( "" ) );
		    // emptyQueryWithColumns = isEmpty( queryNew( "name,age,dateModified" ) );
		      """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "emptyQueryNoColumns" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "emptyQueryWithColumns" ), false ) ).isTrue();
	}

	@DisplayName( "It returns false for non-empty values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    boolValue             = isEmpty( true );
		    zero                  = isEmpty( 0 );
		    one                   = isEmpty( 1 );
		    stringValue           = isEmpty( "2" );
		    stringWithSpaces      = isEmpty( "   " );
		    nestedArray           = isEmpty( [[[],[]]] );
		    nestedStruct          = isEmpty( { a : {}, b : {}} );
		    stringArray           = isEmpty( [ "abc" ] );
		    structWithValues      = isEmpty( { a : "b" } );
		    nestedStructValues    = isEmpty( { a : { "name" : "brad" }} );
		      """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "boolValue" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "zero" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "one" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "stringValue" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "stringWithSpaces" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "nestedArray" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "nestedStruct" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "stringArray" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "structWithValues" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "nestedStructValues" ), false ) ).isFalse();
	}

}
