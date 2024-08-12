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
package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@Disabled
public class BoxRegisterInterceptorTest {

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
		instance.getInterceptorService().clearInterceptionStates();
	}

	@BeforeEach
	void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can register a class" )
	@Test
	void testItCanRegisterAClass() {
		instance.executeSource(
		    """
		    	result = boxRegisterInterceptor( new src.test.bx.Interceptor() )
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can register a dynamic object" )
	@Test
	void testItCanRegisterADynamicObject() {
		variables.put( "dynamicInterceptor", DynamicObject.of( this ) );

		instance.executeSource(
		    """
		    	result = boxRegisterInterceptor( variables.dynamicInterceptor );
		    """,
		    context );

		assertThat( ( Boolean ) variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can register a closure or lambda" )
	@Test
	void testItCanRegisterAClosureOrLambda() {
		instance.executeSource(
		    """
		      	result = boxRegisterInterceptor( () => { return true; }, "afterCacheElementRemoved" );
		    	result2 = boxRegisterInterceptor( () -> { return true; }, "afterCacheElementRemoved" );
		    """,
		    context );

		assertThat( ( Boolean ) variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can throw an exception using a closure or lambda with no states" )
	@Test
	void testItCanThrowAnExceptionUsingAClosureOrLambdaWithNoStates() {

		assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			      	result = boxRegisterInterceptor( () => { return true; } );
			    """,
			    context );
		} );
	}

}
