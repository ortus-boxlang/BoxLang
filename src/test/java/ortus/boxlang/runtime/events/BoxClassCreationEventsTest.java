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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DynamicFunction;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxClassCreationEventsTest {

	static final String			PACKAGE	= "src.test.java.TestCases.phase3.classevents.";
	static BoxRuntime			instance;
	static InterceptorService	interceptorService;
	static Key					result	= new Key( "result" );

	IBoxContext					context;
	IScope						variables;
	/** Recorded events: each entry has an "event" key plus the announced data */
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
	}

	/**
	 * Register a recording interceptor for both events, only recording classes from our test package
	 */
	private void recordEvents() {
		listen( BoxEvent.AFTER_BOX_CLASS_CREATION, data -> {
		} );
		listen( BoxEvent.AFTER_BOX_CLASS_INIT, data -> {
		} );
	}

	private void listen( BoxEvent event, java.util.function.Consumer<IStruct> action ) {
		IInterceptorLambda lambda = data -> {
			if ( data.getAsString( Key.className ).startsWith( PACKAGE ) ) {
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

	@DisplayName( "new with positional args fires creation then init" )
	@Test
	public void testPositionalNew() {
		recordEvents();
		instance.executeSource( "result = new src.test.java.TestCases.phase3.classevents.EventPerson( 'luis', 3 )", context );

		assertThat( events ).hasSize( 2 );
		IStruct	creation	= events.get( 0 );
		IStruct	init		= events.get( 1 );
		assertThat( creation.getAsString( Key.event ) ).isEqualTo( "afterBoxClassCreation" );
		assertThat( creation.getAsString( Key.className ) ).isEqualTo( PACKAGE + "EventPerson" );
		assertThat( creation.getAsBoolean( Key.noInit ) ).isFalse();
		assertThat( creation.get( Key.context ) ).isNotNull();
		assertThat( creation.get( Key.instance ) ).isSameInstanceAs( variables.get( result ) );
		// init has not run yet when the creation event fires
		assertThat( init.getAsString( Key.event ) ).isEqualTo( "afterBoxClassInit" );
		assertThat( init.get( Key.instance ) ).isSameInstanceAs( variables.get( result ) );
		assertThat( init.get( Key.result ) ).isSameInstanceAs( variables.get( result ) );
	}

	@DisplayName( "creation event fires before init runs" )
	@Test
	public void testCreationBeforeInit() {
		List<Object> nameAtCreation = new ArrayList<>();
		listen( BoxEvent.AFTER_BOX_CLASS_CREATION, data -> {
			IClassRunnable boxClass = ( IClassRunnable ) data.get( Key.instance );
			nameAtCreation.add( boxClass.getVariablesScope().get( Key._NAME ) );
		} );
		instance.executeSource( "result = new src.test.java.TestCases.phase3.classevents.EventPerson( 'luis', 3 )", context );

		assertThat( nameAtCreation ).hasSize( 1 );
		assertThat( nameAtCreation.get( 0 ) ).isNotEqualTo( "luis" );
	}

	@DisplayName( "new with named args fires creation then init" )
	@Test
	public void testNamedNew() {
		recordEvents();
		instance.executeSource( "result = new src.test.java.TestCases.phase3.classevents.EventPerson( name = 'luis', age = 3 )", context );

		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_CREATION ) ).hasSize( 1 );
		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_INIT ) ).hasSize( 1 );
		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_INIT ).get( 0 ).get( Key.result ) ).isSameInstanceAs( variables.get( result ) );
	}

	@DisplayName( "implicit constructor fires init after the setters ran" )
	@Test
	public void testImplicitConstructor() {
		List<Object> nameAtInit = new ArrayList<>();
		recordEvents();
		listen( BoxEvent.AFTER_BOX_CLASS_INIT, data -> {
			IClassRunnable boxClass = ( IClassRunnable ) data.get( Key.instance );
			nameAtInit.add( boxClass.getVariablesScope().get( Key._NAME ) );
		} );
		instance.executeSource(
		    """
		    a = new src.test.java.TestCases.phase3.classevents.EventImplicit( name = 'luis' );
		    b = new src.test.java.TestCases.phase3.classevents.EventImplicit( { name : 'majano' } );
		    """,
		    context );

		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_CREATION ) ).hasSize( 2 );
		// Two recording listeners on the init event
		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_INIT ) ).hasSize( 4 );
		assertThat( nameAtInit ).containsExactly( "luis", "majano" ).inOrder();
	}

	@DisplayName( "extends chain fires each event once, for the outermost class only" )
	@Test
	public void testExtendsChainFiresOnce() {
		recordEvents();
		instance.executeSource(
		    """
		    a = new src.test.java.TestCases.phase3.classevents.EventChild();
		    b = new src.test.java.TestCases.phase3.classevents.EventChild( argumentCollection = {} );
		    """,
		    context );

		assertThat( events ).hasSize( 4 );
		events.forEach( e -> assertThat( e.getAsString( Key.className ) ).isEqualTo( PACKAGE + "EventChild" ) );
	}

	@DisplayName( "createObject fires creation with noInit and never init" )
	@Test
	public void testCreateObject() {
		recordEvents();
		instance.executeSource( "result = createObject( 'component', 'src.test.java.TestCases.phase3.classevents.EventPerson' )", context );

		assertThat( events ).hasSize( 1 );
		assertThat( events.get( 0 ).getAsString( Key.event ) ).isEqualTo( "afterBoxClassCreation" );
		assertThat( events.get( 0 ).getAsBoolean( Key.noInit ) ).isTrue();
	}

	@DisplayName( "deserialization fires creation with noInit and never init" )
	@Test
	public void testDeserialization() {
		instance.executeSource(
		    "serialized = objectSerialize( new src.test.java.TestCases.phase3.classevents.EventPerson( 'luis', 3 ) )",
		    context );
		recordEvents();
		instance.executeSource( "result = objectDeserialize( serialized )", context );

		assertThat( events ).hasSize( 1 );
		assertThat( events.get( 0 ).getAsString( Key.event ) ).isEqualTo( "afterBoxClassCreation" );
		assertThat( events.get( 0 ).getAsBoolean( Key.noInit ) ).isTrue();
	}

	@DisplayName( "init throwing does not fire the init event" )
	@Test
	public void testInitThrows() {
		recordEvents();
		assertThrows( BoxRuntimeException.class,
		    () -> instance.executeSource( "result = new src.test.java.TestCases.phase3.classevents.EventInitThrows()", context ) );

		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_CREATION ) ).hasSize( 1 );
		assertThat( eventsOf( BoxEvent.AFTER_BOX_CLASS_INIT ) ).isEmpty();
	}

	@DisplayName( "init returning a different value surfaces in result" )
	@Test
	public void testInitReturnsOther() {
		recordEvents();
		instance.executeSource( "result = new src.test.java.TestCases.phase3.classevents.EventInitReturnsOther()", context );

		List<IStruct> inits = eventsOf( BoxEvent.AFTER_BOX_CLASS_INIT );
		// The inner EventPerson and the outer EventInitReturnsOther
		assertThat( inits ).hasSize( 2 );
		IStruct outer = inits.get( 1 );
		assertThat( outer.getAsString( Key.className ) ).isEqualTo( PACKAGE + "EventInitReturnsOther" );
		assertThat( outer.get( Key.result ) ).isNotSameInstanceAs( outer.get( Key.instance ) );
		assertThat( outer.get( Key.result ) ).isSameInstanceAs( variables.get( result ) );
		assertThat( ( ( IClassRunnable ) outer.get( Key.result ) ).bxGetName().getName() ).isEqualTo( PACKAGE + "EventPerson" );
	}

	@DisplayName( "an interceptor can inject onMissingMethod into this and variables" )
	@Test
	public void testInjectOnMissingMethod() {
		listen( BoxEvent.AFTER_BOX_CLASS_CREATION, data -> {
			IClassRunnable	boxClass	= ( IClassRunnable ) data.get( Key.instance );
			DynamicFunction	omm			= new DynamicFunction(
			    Key.onMissingMethod,
			    ( ctx, fn ) -> "found:" + ctx.getArgumentsScope().getAsString( Key.missingMethodName ),
			    new Argument[] {
			        new Argument( Key.missingMethodName ),
			        new Argument( Key.missingMethodArguments )
			    }
			);
			boxClass.getThisScope().put( Key.onMissingMethod, omm );
			boxClass.getVariablesScope().put( Key.onMissingMethod, omm );
		} );
		instance.executeSource(
		    """
		    finder = new src.test.java.TestCases.phase3.classevents.EventFinder();
		    outside = finder.findByName( "x" );
		    inside = finder.callMissing();
		    """,
		    context );

		assertThat( variables.getAsString( Key.of( "outside" ) ) ).isEqualTo( "found:findByName" );
		assertThat( variables.getAsString( Key.of( "inside" ) ) ).isEqualTo( "found:findByAge" );
	}

	@DisplayName( "no listeners means no interception state, so no event data is built" )
	@Test
	public void testNoListeners() {
		Key[] points = new Key[] { BoxEvent.AFTER_BOX_CLASS_CREATION.key(), BoxEvent.AFTER_BOX_CLASS_INIT.key() };
		// Drop any state left behind by other tests, then restore the points
		interceptorService.removeInterceptionPoint( points );
		interceptorService.registerInterceptionPoint( points );

		instance.executeSource( "result = new src.test.java.TestCases.phase3.classevents.EventPerson( 'luis', 3 )", context );

		assertThat( interceptorService.hasState( BoxEvent.AFTER_BOX_CLASS_CREATION ) ).isFalse();
		assertThat( interceptorService.hasState( BoxEvent.AFTER_BOX_CLASS_INIT ) ).isFalse();
		assertThat( variables.get( result ) ).isInstanceOf( IClassRunnable.class );
	}

}
