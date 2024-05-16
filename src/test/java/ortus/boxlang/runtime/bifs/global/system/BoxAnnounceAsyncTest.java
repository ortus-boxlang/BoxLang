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

import java.util.concurrent.CompletableFuture;

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

public class BoxAnnounceAsyncTest {

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
	void setupEach() {
		instance.getInterceptorService().registerState( Key.of( "onServerScopeCreation" ) );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can announce a non-existent event with no data" )
	@Test
	void testItCanAnnounceNonExistentEvent() {
		instance.executeSource(
		    """
		    result = boxAnnounceAsync( "invalid" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( null );
	}

	@DisplayName( "It can announce a non-existent event with data" )
	@Test
	void testItCanAnnounceNonExistentEventWithData() {
		instance.executeSource(
		    """
		    result = boxAnnounceAsync( "invalid", { "foo": "bar" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( null );
	}

	@DisplayName( "It can announce an event with no data" )
	@Test
	void testItCanAnnounceEvent() {
		instance.executeSource(
		    """
		    result = boxAnnounceAsync( "onServerScopeCreation" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( CompletableFuture.class );
	}

	@DisplayName( "It can announce an event with data" )
	@Test
	void testItCanAnnounceEventWithData() {
		instance.executeSource(
		    """
		    result = boxAnnounceAsync( "onServerScopeCreation", { "foo": "bar" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( CompletableFuture.class );
	}

	@DisplayName( "It can announce an event named params" )
	@Test
	void testItCanAnnounceEventWithDataAndNamedParams() {
		instance.executeSource(
		    """
		    result = boxAnnounceAsync( state = "onServerScopeCreation", data = { "foo": "bar" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( CompletableFuture.class );

		instance.executeSource(
		    """
		    result = boxAnnounceAsync( state = "onServerScopeCreation" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( CompletableFuture.class );
	}

}
