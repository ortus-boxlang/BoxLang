package ortus.boxlang.runtime.events;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.MockInterceptor;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class InterceptorPoolTest {

	InterceptorPool pool;

	@BeforeEach
	void setupBeforeEach() {
		pool = new InterceptorPool( "bdd" );
	}

	@DisplayName( "Test it can get build a pool" )
	@Test
	void testItCanBeBuilt() {
		assertThat( pool ).isNotNull();
	}

	@DisplayName( "Test it can register interception points and filter duplicates" )
	@Test
	void testItCanRegisterInterceptionPoints() {
		int originalSize = pool.getInterceptionPoints().size();

		pool.registerInterceptionPoint( Key.of( "brad", "brad", "luis" ) );
		assertThat( pool.getInterceptionPointsNames() ).contains( "luis" );
		assertThat( pool.getInterceptionPointsNames() ).contains( "brad" );
		assertThat( pool.getInterceptionPoints().size() ).isEqualTo( originalSize + 2 );
	}

	@DisplayName( "It can remove interception points" )
	@Test
	void testItCanRemoveInterceptionPoints() {
		pool.registerInterceptionPoint( Key.of( "onRequestStart" ) );
		assertThat( pool.hasInterceptionPoint( Key.of( "onRequestStart" ) ) ).isTrue();

		pool.removeInterceptionPoint( Key.of( "onRequestStart" ) );
		assertThat( pool.hasInterceptionPoint( Key.of( "onRequestStart" ) ) ).isFalse();
	}

	@DisplayName( "It can register new interception states with an existing point" )
	@Test
	void testItCanRegisterInterceptionStatesWithExistingPoint() {
		Key pointKey = Key.of( "onRequestStart" );

		pool.registerInterceptionPoint( pointKey );
		pool.registerState( pointKey );

		assertThat( pool.hasState( pointKey ) ).isTrue();
		assertThat( pool.getState( pointKey ).getName() ).isEqualTo( pointKey );

		pool.removeState( pointKey );
		assertThat( pool.hasState( pointKey ) ).isFalse();
	}

	@DisplayName( "It can register new interception states with a non-existing point" )
	@Test
	void testItCanRegisterInterceptionStatesWithNonExistingPoint() {
		Key pointKey = Key.of( "onRequestStart" );

		pool.registerState( pointKey );

		assertThat( pool.hasState( pointKey ) ).isTrue();
		assertThat( pool.getState( pointKey ).getName() ).isEqualTo( pointKey );

		pool.removeState( pointKey );
		assertThat( pool.hasState( pointKey ) ).isFalse();
	}

	@DisplayName( "it can register new interceptors" )
	@Test
	void testItCanRegisterInterceptors() {
		DynamicObject	mockInterceptor	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey		= Key.of( "onRequestStart" );

		pool.register(
		    mockInterceptor,
		    pointKey
		);

		assertThat( pool.getState( pointKey ).exists( mockInterceptor ) ).isTrue();
	}

	@DisplayName( "It can register lambdas as interceptors" )
	@Test
	void testItCanRegisterLambdasAsInterceptors() {
		Key pointKey = Key.of( "onRequestStart" );

		pool.register(
		    ( IStruct data ) -> {
			    data.put( "counter", ( int ) data.get( "counter" ) + 1 );
			    return false;
		    },
		    pointKey
		);

		IStruct data = new Struct();
		data.put( "counter", 0 );

		pool.announce(
		    pointKey,
		    data
		);

		assertThat( data.get( "counter" ) ).isEqualTo( 1 );
	}

	@DisplayName( "it can unregister interceptors with a specific state" )
	@Test
	void testItCanUnregisterInterceptors() {
		DynamicObject	mockInterceptor	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey		= Key.of( "onRequestStart" );

		pool.register(
		    mockInterceptor,
		    pointKey
		);

		assertThat( pool.getState( pointKey ).exists( mockInterceptor ) ).isTrue();

		pool.unregister(
		    mockInterceptor,
		    pointKey
		);

		assertThat( pool.getState( pointKey ).exists( mockInterceptor ) ).isFalse();
	}

	@DisplayName( "it can unregister interceptors with all states" )
	@Test
	void testItCanUnregisterInterceptorsWithAllStates() {
		DynamicObject mockInterceptor = DynamicObject.of( new MockInterceptor() );

		pool.register(
		    mockInterceptor,
		    Key.of( "onRequestStart", "onRequestEnd" )
		);

		assertThat( pool.getState( Key.of( "onRequestStart" ) ).exists( mockInterceptor ) ).isTrue();
		assertThat( pool.getState( Key.of( "onRequestEnd" ) ).exists( mockInterceptor ) ).isTrue();

		pool.unregister( mockInterceptor );

		assertThat( pool.getState( Key.of( "onRequestStart" ) ).exists( mockInterceptor ) ).isFalse();
		assertThat( pool.getState( Key.of( "onRequestEnd" ) ).exists( mockInterceptor ) ).isFalse();
	}

	@DisplayName( "it can announce an event to a specific state" )
	@Test
	void testItCanAnnounceEventToSpecificState() {
		DynamicObject	mockInterceptor1	= DynamicObject.of( new MockInterceptor() );
		DynamicObject	mockInterceptor2	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey			= Key.of( "onRequestStart" );

		pool.register(
		    mockInterceptor1,
		    pointKey
		);
		pool.register(
		    mockInterceptor2,
		    pointKey
		);

		assertThat( pool.getState( pointKey ).size() ).isEqualTo( 2 );

		IStruct data = new Struct();
		data.put( "counter", 0 );

		pool.announce(
		    pointKey,
		    data
		);

		assertThat( data.get( "counter" ) ).isEqualTo( 2 );
	}

	@DisplayName( "it can announce an event to a specific state with case-insensitivity" )
	@Test
	void testItCanAnnounceEventToSpecificStateWithNoCase() {
		DynamicObject	mockInterceptor1	= DynamicObject.of( new MockInterceptor() );
		DynamicObject	mockInterceptor2	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey			= Key.of( "onRequestStart" );

		pool.register(
		    mockInterceptor1,
		    pointKey
		);
		pool.register(
		    mockInterceptor2,
		    pointKey
		);

		assertThat( pool.getState( pointKey ).size() ).isEqualTo( 2 );

		IStruct data = new Struct();
		data.put( "COUNTER", 0 );

		pool.announce(
		    pointKey,
		    data
		);

		assertThat( data.get( "counter" ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It can register and announce pure Java interceptors" )
	void testItCanRegisterAndAnnouncePureJavaInterceptors() {
		Key testKey = Key.of( "onUnitTest" );

		pool.registerInterceptionPoint( testKey );
		pool.register( new UnitTestInterceptor() );

		assertThat( pool.getState( testKey ).size() ).isEqualTo( 1 );

		IStruct data = new Struct();
		data.put( "counter", 0 );
		// Announce it
		pool.announce( testKey, data );
		// Assert it
		assertThat( data.get( "counter" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "It can announceAsync" )
	void testAnnounceAsync() throws InterruptedException, ExecutionException {
		Key testKey = Key.of( "onUnitTest" );

		pool.registerInterceptionPoint( testKey );
		pool.register( new UnitTestInterceptor() );

		assertThat( pool.getState( testKey ).size() ).isEqualTo( 1 );

		IStruct data = new Struct();
		data.put( "counter", 0 );

		// Announce it
		CompletableFuture future = pool.announceAsync( testKey, data );

		// Assert it
		assertThat( future ).isNotNull();
		assertThat( ( ( IStruct ) future.get() ).get( "counter" ) ).isEqualTo( 1 );
	}

}
