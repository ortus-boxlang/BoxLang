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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
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
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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
		assertThat( ( Boolean ) variables.get( Key.of( "emptyString" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyArray" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyStruct" ) ) ).isTrue();
	}

	@Test
	public void structMemberFunction() {
		instance.executeSource(
		    """
		    myStruct = { a : "b" };
		    myEmptyStruct = {};

		       onPopulated   = myStruct.isEmpty();
		       onEmpty = myEmptyStruct.isEmpty();
		         """,
		    context );

		assertThat( ( Boolean ) variables.get( Key.of( "onPopulated" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "onEmpty" ) ) ).isTrue();
	}

	@Test
	public void arrayMemberFunction() {
		instance.executeSource(
		    """
		    myArray 	 = { a : "b" };
		    myEmptyArray = {};

		       onPopulated   = myArray.isEmpty();
		       onEmpty 	  = myEmptyArray.isEmpty();
		         """,
		    context );

		assertThat( ( Boolean ) variables.get( Key.of( "onPopulated" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "onEmpty" ) ) ).isTrue();
	}

	@Test
	public void stringMemberFunction() {
		instance.executeSource(
		    """
		    myString 	  = "brad";
		    myEmptyString = "";

		       onPopulated   = myString.isEmpty();
		       onEmpty 	  = myEmptyString.isEmpty();
		         """,
		    context );

		assertThat( ( Boolean ) variables.get( Key.of( "onPopulated" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "onEmpty" ) ) ).isTrue();
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
		assertThat( ( Boolean ) variables.get( Key.of( "emptyQueryNoColumns" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyQueryWithColumns" ) ) ).isTrue();
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
		assertThat( ( Boolean ) variables.get( Key.of( "boolValue" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "zero" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "one" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "stringValue" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "stringWithSpaces" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "nestedArray" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "nestedStruct" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "stringArray" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "structWithValues" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "nestedStructValues" ) ) ).isFalse();
	}

}
