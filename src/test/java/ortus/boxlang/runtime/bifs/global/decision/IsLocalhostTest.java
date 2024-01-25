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

public class IsLocalhostTest {

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

	@DisplayName( "It detects loopback IP addresses" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    localhost    = isLocalHost( "localhost" );
		    ipv4loopback = isLocalHost( "127.0.0.1" );
		    ipv6loopback = isLocalHost( "::1" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "localhost" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "ipv4loopback" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "ipv6loopback" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-loopback IP addresses or strings" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    empty        = isLocalHost( "" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "empty" ) ) ).isFalse();
	}

	@DisplayName( "It throws on non-string values" )
	@Test
	public void testThrow() {
		// throws in ACF and Lucee
		assertThrows( Throwable.class, () -> instance.executeSource( "isLocalHost( {} );", context ) );
	}

}
