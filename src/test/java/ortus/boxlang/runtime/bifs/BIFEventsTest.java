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
package ortus.boxlang.runtime.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.IInterceptorLambda;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BIFEventsTest {

	static BoxRuntime			instance;
	static InterceptorService	interceptorService;
	static Key					result	= new Key( "result" );

	IBoxContext					context;
	IScope						variables;
	/** Recorded events: each entry has an "event" key plus a copy of the announced data */
	List<IStruct>				events;
	List<IInterceptorLambda>	registered;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		interceptorService	= instance.getInterceptorService();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		events		= Collections.synchronizedList( new ArrayList<>() );
		registered	= new ArrayList<>();
	}

	@AfterEach
	public void teardownEach() {
		registered.forEach( interceptorService::unregister );
		// Unregistering leaves an empty state behind, which would make the runtime think someone is still listening
		interceptorService.removeState( BoxEvent.ON_BIF_INVOCATION.key() );
		interceptorService.removeState( BoxEvent.POST_BIF_INVOCATION.key() );
		interceptorService.removeState( BoxEvent.ON_BIF_EXCEPTION.key() );
	}

	/**
	 * Listen to a BIF event, recording only invocations of the BIF with the given name
	 */
	private void listen( BoxEvent event, String bifName, Consumer<IStruct> action ) {
		IInterceptorLambda lambda = data -> {
			if ( data.get( Key._name ).toString().equalsIgnoreCase( bifName ) ) {
				IStruct copy = new Struct( data );
				copy.put( Key.event, event.key().getName() );
				events.add( copy );
				action.accept( data );
			}
			return false;
		};
		registered.add( lambda );
		interceptorService.register( lambda, event.key() );
	}

	private List<IStruct> eventsOf( BoxEvent event ) {
		return events.stream().filter( e -> e.getAsString( Key.event ).equals( event.key().getName() ) ).toList();
	}

	@DisplayName( "post point fires once after a successful call with result and elapsed time" )
	@Test
	public void testPostInvocation() {
		listen( BoxEvent.POST_BIF_INVOCATION, "ucase", data -> {
		} );
		instance.executeSource( "result = ucase( 'luis' )", context );

		assertThat( variables.get( result ) ).isEqualTo( "LUIS" );
		List<IStruct> posts = eventsOf( BoxEvent.POST_BIF_INVOCATION );
		assertThat( posts ).hasSize( 1 );
		assertThat( posts.get( 0 ).get( Key.result ) ).isEqualTo( "LUIS" );
		assertThat( posts.get( 0 ).get( Key.bif ) ).isInstanceOf( BIF.class );
		assertThat( posts.get( 0 ).getAsLong( Key.elapsedNanos ) ).isAtLeast( 0L );
		assertThat( posts.get( 0 ).containsKey( Key.exception ) ).isFalse();
	}

	@DisplayName( "pre point fires exactly once per call, before the BIF runs" )
	@Test
	public void testPreFiresOnce() {
		listen( BoxEvent.ON_BIF_INVOCATION, "ucase", data -> {
		} );
		listen( BoxEvent.POST_BIF_INVOCATION, "ucase", data -> {
		} );
		instance.executeSource( "result = ucase( 'luis' )", context );

		assertThat( eventsOf( BoxEvent.ON_BIF_INVOCATION ) ).hasSize( 1 );
		assertThat( eventsOf( BoxEvent.ON_BIF_INVOCATION ).get( 0 ).containsKey( Key.result ) ).isFalse();
		assertThat( eventsOf( BoxEvent.POST_BIF_INVOCATION ) ).hasSize( 1 );
		// Order is pre then post
		assertThat( events.get( 0 ).getAsString( Key.event ) ).isEqualTo( "onBIFInvocation" );
		assertThat( events.get( 1 ).getAsString( Key.event ) ).isEqualTo( "postBIFInvocation" );
	}

	@DisplayName( "a throwing BIF fires the exception point, not post, and rethrows" )
	@Test
	public void testException() {
		listen( BoxEvent.POST_BIF_INVOCATION, "createObject", data -> {
		} );
		listen( BoxEvent.ON_BIF_EXCEPTION, "createObject", data -> {
		} );

		assertThrows( BoxRuntimeException.class,
		    () -> instance.executeSource( "result = createObject( 'java', 'no.such.ClassHere' )", context ) );

		assertThat( eventsOf( BoxEvent.POST_BIF_INVOCATION ) ).isEmpty();
		List<IStruct> errors = eventsOf( BoxEvent.ON_BIF_EXCEPTION );
		assertThat( errors ).hasSize( 1 );
		assertThat( errors.get( 0 ).get( Key.exception ) ).isInstanceOf( Throwable.class );
		assertThat( errors.get( 0 ).getAsLong( Key.elapsedNanos ) ).isAtLeast( 0L );
	}

	@DisplayName( "a post listener can override the result" )
	@Test
	public void testPostOverridesResult() {
		listen( BoxEvent.POST_BIF_INVOCATION, "ucase", data -> data.put( Key.result, "OVERRIDDEN" ) );
		instance.executeSource( "result = ucase( 'luis' )", context );

		assertThat( variables.get( result ) ).isEqualTo( "OVERRIDDEN" );
	}

	@DisplayName( "with no listeners nothing is announced and BIFs still work" )
	@Test
	public void testNoListeners() {
		assertThat( interceptorService.hasState( BoxEvent.POST_BIF_INVOCATION ) ).isFalse();
		assertThat( interceptorService.hasState( BoxEvent.ON_BIF_EXCEPTION ) ).isFalse();

		instance.executeSource( "result = ucase( 'luis' )", context );

		assertThat( variables.get( result ) ).isEqualTo( "LUIS" );
		assertThat( events ).isEmpty();
	}
}
