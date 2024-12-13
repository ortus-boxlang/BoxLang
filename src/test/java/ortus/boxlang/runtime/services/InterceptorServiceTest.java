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

package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.events.UnitTestInterceptor;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

class InterceptorServiceTest {

	InterceptorService	service;

	@Spy
	@InjectMocks
	private BoxRuntime	runtime	= Mockito.spy( BoxRuntime.getInstance() );

	@BeforeEach
	public void setupBeforeEach() {
		Mockito.doReturn( new ScriptingRequestBoxContext() ).when( runtime ).getRuntimeContext();
		service = new InterceptorService( runtime );
		service.onConfigurationLoad();
	}

	@DisplayName( "Test it can get an instance of the service" )
	@Test
	void testItCanGetInstance() {
		assertThat( service ).isNotNull();
	}

	@DisplayName( "Test it can run the startup service event" )
	@Test
	void testItCanRunStartupEvent() {
		assertDoesNotThrow( () -> service.onStartup() );
	}

	@DisplayName( "Test it can run the onShutdown service event" )
	@Test
	void testItCanRunOnShutdownEvent() {
		assertDoesNotThrow( () -> service.onShutdown( false ) );
	}

	@DisplayName( "Test it can register interception points and filter duplicates" )
	@Test
	void testItCanRegisterInterceptionPoints() {
		int originalSize = service.getInterceptionPoints().size();

		service.registerInterceptionPoint( Key.of( "brad", "brad", "luis" ) );
		assertThat( service.getInterceptionPointsNames() ).contains( "luis" );
		assertThat( service.getInterceptionPointsNames() ).contains( "brad" );
		assertThat( service.getInterceptionPoints().size() ).isEqualTo( originalSize + 2 );
	}

	@DisplayName( "It can remove interception points" )
	@Test
	void testItCanRemoveInterceptionPoints() {
		service.registerInterceptionPoint( Key.of( "onRequestStart" ) );
		assertThat( service.hasInterceptionPoint( Key.of( "onRequestStart" ) ) ).isTrue();

		service.removeInterceptionPoint( Key.of( "onRequestStart" ) );
		assertThat( service.hasInterceptionPoint( Key.of( "onRequestStart" ) ) ).isFalse();
	}

	@DisplayName( "It can register new interception states with an existing point" )
	@Test
	void testItCanRegisterInterceptionStatesWithExistingPoint() {
		Key pointKey = Key.of( "onRequestStart" );

		service.registerInterceptionPoint( pointKey );
		service.registerState( pointKey );

		assertThat( service.hasState( pointKey ) ).isTrue();
		assertThat( service.getState( pointKey ).getName() ).isEqualTo( pointKey );

		service.removeState( pointKey );
		assertThat( service.hasState( pointKey ) ).isFalse();
	}

	@DisplayName( "It can register new interception states with a non-existing point" )
	@Test
	void testItCanRegisterInterceptionStatesWithNonExistingPoint() {
		Key pointKey = Key.of( "onRequestStart" );

		service.registerState( pointKey );

		assertThat( service.hasState( pointKey ) ).isTrue();
		assertThat( service.getState( pointKey ).getName() ).isEqualTo( pointKey );

		service.removeState( pointKey );
		assertThat( service.hasState( pointKey ) ).isFalse();
	}

	@DisplayName( "it can register new interceptors" )
	@Test
	void testItCanRegisterInterceptors() {
		DynamicObject	mockInterceptor	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey		= Key.of( "onRequestStart" );

		service.register(
		    mockInterceptor,
		    pointKey
		);

		assertThat( service.getState( pointKey ).exists( mockInterceptor ) ).isTrue();
	}

	@DisplayName( "it can unregister interceptors with a specific state" )
	@Test
	void testItCanUnregisterInterceptors() {
		DynamicObject	mockInterceptor	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey		= Key.of( "onRequestStart" );

		service.register(
		    mockInterceptor,
		    pointKey
		);

		assertThat( service.getState( pointKey ).exists( mockInterceptor ) ).isTrue();

		service.unregister(
		    mockInterceptor,
		    pointKey
		);

		assertThat( service.getState( pointKey ).exists( mockInterceptor ) ).isFalse();
	}

	@DisplayName( "it can unregister interceptors with all states" )
	@Test
	void testItCanUnregisterInterceptorsWithAllStates() {
		DynamicObject mockInterceptor = DynamicObject.of( new MockInterceptor() );

		service.register(
		    mockInterceptor,
		    Key.of( "onRequestStart", "onRequestEnd" )
		);

		assertThat( service.getState( Key.of( "onRequestStart" ) ).exists( mockInterceptor ) ).isTrue();
		assertThat( service.getState( Key.of( "onRequestEnd" ) ).exists( mockInterceptor ) ).isTrue();

		service.unregister( mockInterceptor );

		assertThat( service.getState( Key.of( "onRequestStart" ) ).exists( mockInterceptor ) ).isFalse();
		assertThat( service.getState( Key.of( "onRequestEnd" ) ).exists( mockInterceptor ) ).isFalse();
	}

	@DisplayName( "it can announce an event to a specific state" )
	@Test
	void testItCanAnnounceEventToSpecificState() {
		DynamicObject	mockInterceptor1	= DynamicObject.of( new MockInterceptor() );
		DynamicObject	mockInterceptor2	= DynamicObject.of( new MockInterceptor() );
		Key				pointKey			= Key.of( "onRequestStart" );

		service.register(
		    mockInterceptor1,
		    pointKey
		);
		service.register(
		    mockInterceptor2,
		    pointKey
		);

		assertThat( service.getState( pointKey ).size() ).isEqualTo( 2 );

		IStruct data = new Struct();
		data.put( "counter", 0 );

		service.announce(
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

		service.register(
		    mockInterceptor1,
		    pointKey
		);
		service.register(
		    mockInterceptor2,
		    pointKey
		);

		assertThat( service.getState( pointKey ).size() ).isEqualTo( 2 );

		IStruct data = new Struct();
		data.put( "COUNTER", 0 );

		service.announce(
		    pointKey,
		    data
		);

		assertThat( data.get( "counter" ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It can register and announce pure Java interceptors" )
	void testItCanRegisterAndAnnouncePureJavaInterceptors() {
		Key testKey = Key.of( "onUnitTest" );

		service.registerInterceptionPoint( testKey );
		service.register( new UnitTestInterceptor() );

		assertThat( service.getState( testKey ).size() ).isEqualTo( 1 );

		IStruct data = new Struct();
		data.put( "counter", 0 );
		// Announce it
		service.announce( testKey, data );
		// Assert it
		assertThat( data.get( "counter" ) ).isEqualTo( 1 );
	}

}
