/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system.java;

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
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class CreateDynamicProxyTest {

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

	@DisplayName( "It creates a proxy" )
	@Test
	public void testCreatesAProxy() {
		// @formatter:off
		instance.executeSource(
		    """
				import java:java.lang.Thread;

				jRunnable = CreateDynamicProxy(
					"src.test.java.ortus.boxlang.runtime.dynamic.javaproxy.BoxClassRunnable",
					"java.lang.Runnable"
				);

				jthread = new java:Thread( jRunnable );
				jthread.start();
				sleep( 500 );
		       """,
		context );
		// @formatter:on
		assertThat( context.getScope( ServerScope.name ).get( "runnableProxyFired" ) ).isEqualTo( true );
	}

	@DisplayName( "It allows java.lang.Object methods" )
	@Test
	public void testAllowsObjectMethods() {
		// @formatter:off
		instance.executeSource(
		    """
				import java:java.lang.Thread;

				jRunnable = CreateDynamicProxy(
					"src.test.java.ortus.boxlang.runtime.dynamic.javaproxy.BoxClassRunnable",
					"java.lang.Runnable"
				);

				// Should defer to the Object class
				result = jRunnable.toString();
				// Actually exists in the CFC
				result2 = jRunnable.hashCode();
		       """,
		context );
		// @formatter:on
		assertThat( variables.getAsString( result ) ).contains( "Boxclassrunnable$cfc@" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It casts output to match interface method return type" )
	@Test
	public void testCastsOutputToMatchInterfaceMethodReturnType() {
		// @formatter:off
		instance.executeSource(
		    """
				import java.util.Arrays;
				import java.util.stream.Collectors;
				jStream = Arrays.stream( [ "foo", "bar" ] )

				proxy = CreateDynamicProxy(
					"src.test.java.ortus.boxlang.runtime.bifs.global.system.java.ToLongFunction",
					[ "java.util.function.ToLongFunction" ]
				);
				result = jStream.collect( Collectors.summingLong( proxy ) );
				println( result)
		       """,
		context );
		// @formatter:on
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( 84 );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class,
		// @formatter:off
		()->instance.executeSource(
		    """
				import java.util.Arrays;
				import java.util.stream.Collectors;
				jStream = Arrays.stream( [ "foo", "bar" ] )

				proxy = CreateDynamicProxy(
					"src.test.java.ortus.boxlang.runtime.bifs.global.system.java.ToLongFunctionInvalidReturn",
					[ "java.util.function.ToLongFunction" ]
				);
				result = jStream.collect( Collectors.summingLong( proxy ) );
				println( result)
		       """,
		        context ) );
		// @formatter:on

		assertThat( e.getMessage() ).contains( "could not be coerced" );
	}

	@DisplayName( "It accepts optional properties parameter" )
	@Test
	public void testAcceptsPropertiesParameter() {
		// @formatter:off
		instance.executeSource(
		    """
				import java:java.lang.Thread;

				// Test with null properties (should work the same as before)
				jRunnable = CreateDynamicProxy(
					"src.test.java.ortus.boxlang.runtime.dynamic.javaproxy.BoxClassRunnable",
					"java.lang.Runnable",
					null
				);

				jthread = new java:Thread( jRunnable );
				jthread.start();
				sleep( 500 );
		       """,
		context );
		// @formatter:on
		assertThat( context.getScope( ServerScope.name ).get( "runnableProxyFired" ) ).isEqualTo( true );
	}

}
