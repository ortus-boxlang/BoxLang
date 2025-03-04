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
import ortus.boxlang.runtime.services.InterceptorService;

public class BoxRegisterInterceptionPointsTest {

	static BoxRuntime			instance;
	static InterceptorService	interceptorService;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		interceptorService	= instance.getInterceptorService();
	}

	@AfterAll
	public static void teardown() {
		interceptorService.removeInterceptionPoint( Key.of( "onCustomOrder" ), Key.of( "onCustomOrder2" ) );
	}

	@BeforeEach
	void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can register nothing" )
	@Test
	void testItCanRegisterNothing() {
		instance.executeSource(
		    """
		    	result = BoxRegisterInterceptionPoints()
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can register an event as a string" )
	@Test
	void testItCanRegisterAsString() {
		instance.executeSource(
		    """
		    	result = BoxRegisterInterceptionPoints( "onCustomOrder" )
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isEqualTo( true );
		assertThat( interceptorService.getInterceptionPointsNames() ).contains( "onCustomOrder" );
	}

	@DisplayName( "It can register an event as an array" )
	@Test
	void testItCanRegisterAsArray() {
		instance.executeSource(
		    """
		    	result = BoxRegisterInterceptionPoints( [ "onCustomOrder", "onCustomOrder2" ] )
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isEqualTo( true );
		assertThat( interceptorService.getInterceptionPointsNames() ).containsAtLeast( "onCustomOrder", "onCustomOrder2" );
	}

}
