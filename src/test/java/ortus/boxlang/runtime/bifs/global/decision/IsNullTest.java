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

import org.junit.jupiter.api.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

import static com.google.common.truth.Truth.assertThat;

public class IsNullTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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

	@DisplayName( "It detects null values" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		       brad = function(){};
		       badBrad = function(){ return; };
		       myArray = [];
		       myStruct = {};

		    variableReference   = isNull( foo );
		    noFunctionReturn    = isNull( brad() );
		    emptyFunctionReturn = isNull( badBrad() );
		    structKey           = isNull( myStruct.brad );

		    // Returns true in Lucee, throws exception in ACF
		    arrayPosition = isNull( myArray[ 1 ] );
		                		""",
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "variableReference" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "noFunctionReturn" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyFunctionReturn" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "arrayPosition" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "structKey" ) ) ).isTrue();
	}

	@DisplayName( "It works with nullValue()" )
	@Test
	public void testNullValueMethod() {
		assertThat( ( Boolean ) instance.executeStatement( "isNull( nullValue() )", context ) ).isTrue();
	}

	@DisplayName( "It returns false for non-null values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		       foo = "bar";
		    brad = function(){ return "1"; };
		    myArray = [ 0, 1 ];
		    myStruct = { "brad" : 1 };

		    variableReference = isNull( foo );
		    functionReturn    = isNull( brad() );
		    structKey         = isNull( myStruct.brad );
		    arrayPosition     = isNull( myArray[ 1 ] );
		    emptyStruct       = isNull( {} );
		    emptyArray        = isNull( [] );
		    	""",
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "variableReference" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "functionReturn" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "arrayPosition" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "structKey" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyStruct" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyArray" ) ) ).isFalse();
	}

}
