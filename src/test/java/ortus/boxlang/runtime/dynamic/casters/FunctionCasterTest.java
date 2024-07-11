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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.function.Predicate;

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
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.JavaMethod;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class FunctionCasterTest {

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

	@DisplayName( "It can cast a Function to a Function" )
	@Test
	void testItCanCastAFunction() {
		Function func = new SampleUDF( null, Key.of( "Func" ), null, null, null );
		assertThat( FunctionCaster.cast( func ).getName() ).isEqualTo( Key.of( "Func" ) );
	}

	@DisplayName( "It can not cast a non-function" )
	@Test
	void testItCanNotCastANonFunction() {
		Double k;
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new HashMap<>() ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( null ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new Object[] {} ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new Struct() ) );

	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		Function				func	= new SampleUDF( null, Key.of( "Func" ), null, null, null );
		CastAttempt<Function>	attempt	= FunctionCaster.attempt( func );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get().getName() ).isEqualTo( Key.of( "Func" ) );
		assertThat( attempt.ifSuccessful( ( v ) -> System.out.println( v ) ) );

		final CastAttempt<Function> attempt2 = FunctionCaster.attempt( new HashMap<>() );
		assertThat( attempt2.wasSuccessful() ).isFalse();

		assertThrows( BoxLangException.class, () -> attempt2.get() );
		assertThat( attempt2.ifSuccessful( ( v ) -> System.out.println( v ) ) );
	}

	@DisplayName( "It can cast a Java Lambda to a Function" )
	@Test
	void testItCanCastAJavaLambda() {
		Function myJavaPredicate = FunctionCaster.cast( ( Predicate<Object> ) ( t ) -> t.equals( "brad" ) );
		assertThat( myJavaPredicate ).isInstanceOf( Function.class );
		assertThat( myJavaPredicate ).isInstanceOf( JavaMethod.class );
		assertThat( context.invokeFunction( myJavaPredicate, new Object[] { "brad" } ) ).isEqualTo( true );
		assertThat( context.invokeFunction( myJavaPredicate, new Object[] { "luis" } ) ).isEqualTo( false );

		variables.put( "myJavaPredicate", myJavaPredicate );
		instance.executeSource(
		    """
		    directInvoke = myJavaPredicate( "brad" );
		    directInvoke2 = myJavaPredicate( "luis" );

		    myArry = [ "brad", "luis" ];
		    result = myArry.filter( myJavaPredicate );
		           """,
		    context );
		assertThat( variables.get( "directInvoke" ) ).isEqualTo( true );
		assertThat( variables.get( "directInvoke2" ) ).isEqualTo( false );

		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "brad" );
	}

}
