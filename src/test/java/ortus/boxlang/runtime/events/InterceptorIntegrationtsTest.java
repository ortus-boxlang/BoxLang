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
package ortus.boxlang.runtime.events;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class InterceptorIntegrationtsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Can throw an exception when registering an interceptor with a non-function" )
	@Test
	public void testRegisterInterceptorWithNonFunction() {
		assertThrows( BoxRuntimeException.class, () -> {

			// @formatter:off
			instance.executeSource(
				"""
				  boxRegisterInterceptor( "not a function" );
				""",
				context );
			// @formatter:on

		}, "Expected an exception when registering an interceptor with a non-function" );
	}

	@DisplayName( "Can listen to pre function call events with a BoxLang function" )
	@Test
	public void testPreFunctionCallEvent() {

		// @formatter:off
		instance.executeSource(
			"""
			  boxRegisterInterceptor(  ( data ) => {
				println( "Pre function call: " & data.name );
			  }, "preFunctionInvoke" )

			  function testFunction(){
				return "Hello from testFunction";
			  }

			  testFunction();

			""",
			context );
		// @formatter:on

	}

}
