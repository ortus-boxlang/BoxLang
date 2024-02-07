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
import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class IsCustomFunctionTest {

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

	@DisplayName( "It detects custom functions" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    closure = isCustomFunction( function(){} );
		    arrowClosure = isCustomFunction( () => {} );
		    lambda = isCustomFunction( () -> {} );

		    myFunc = function() {};
		    udf = isCustomFunction( myFunc );
		       """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "closure" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "arrowClosure" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "lambda" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "udf" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-custom functions" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    anInteger = isCustomFunction( 123 );
		    aString = isCustomFunction( "abc" );
		       """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "anInteger" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "aString" ) ) ).isFalse();
	}

	@DisplayName( "It supports Lucee's type parameter" )
	@Test
	public void testTypeParameter() {
		instance.executeSource(
		    """
		    isLambdaAUDFType = isCustomFunction( () -> {}, "udf" );
		    isLambdaAClosureType = isCustomFunction( () -> {}, "closure" );
		    isLambdaALambdaType = isCustomFunction( () -> {}, "lambda" );

		    function myUDF(){};
		    isUDFaLambdaType = isCustomFunction( myUDF, "lambda" );
		    isUDFaClosureType = isCustomFunction( myUDF, "closure" );
		    isUDFaUDFType = isCustomFunction( myUDF, "udf" );

		    isClosureaLambdaType = isCustomFunction( function(){}, "lambda" );
		    isClosureaUDFType = isCustomFunction( function(){}, "udf" );
		    isClosureaClosureType = isCustomFunction( function(){}, "closure" );

		    isArrowClosureaLambdaType = isCustomFunction( () => {}, "Lambda" );
		    isArrowClosureaUDFType = isCustomFunction( () => {}, "UDF" );
		    isArrowClosureaClosureType = isCustomFunction( () => {}, "Closure" );
		       """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "isLambdaAUDFType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isLambdaAClosureType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isLambdaALambdaType" ) ) ).isTrue();

		assertThat( variables.getAsBoolean( Key.of( "isUDFaLambdaType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isUDFaClosureType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isUDFaUDFType" ) ) ).isTrue();

		assertThat( variables.getAsBoolean( Key.of( "isClosureaLambdaType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isClosureaUDFType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isClosureaClosureType" ) ) ).isTrue();

		assertThat( variables.getAsBoolean( Key.of( "isArrowClosureaLambdaType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isArrowClosureaUDFType" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isArrowClosureaClosureType" ) ) ).isTrue();
	}

	@DisplayName( "It validates the type parameter" )
	@Test
	public void testTypeParameterValidation() {
		assertThrows( Throwable.class, () -> {
			instance.executeSource(
			    """
			    result = isCustomFunction( () => {}, "brad" );
			    """,
			    context );
		} );
	}
}
