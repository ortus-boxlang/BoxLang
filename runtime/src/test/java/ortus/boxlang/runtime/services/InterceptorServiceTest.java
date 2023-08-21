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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Struct;

import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;

import tests.ortus.boxlang.runtime.services.MockInterceptor;

public class InterceptorServiceTest {

	@DisplayName( "Test it can get an instance of the service" )
	@Test
	void testItCanGetInstance() {
		InterceptorService service = InterceptorService.getInstance();
		assertThat( service ).isNotNull();
	}

	@DisplayName( "Test it can run the startup service event" )
	@Test
	void testItCanRunStartupEvent() {
		InterceptorService service = InterceptorService.getInstance();
		assertDoesNotThrow( () -> service.onStartup() );
	}

	@DisplayName( "Test it can run the onConfigurationLoad service event" )
	@Test
	void testItCanRunOnConfigurationLoadEvent() {
		InterceptorService service = InterceptorService.getInstance();
		assertDoesNotThrow( () -> service.onConfigurationLoad() );
	}

	@DisplayName( "Test it can run the onShutdown service event" )
	@Test
	void testItCanRunOnShutdownEvent() {
		InterceptorService service = InterceptorService.getInstance();
		assertDoesNotThrow( () -> service.onShutdown() );
	}

	@DisplayName( "Test it can register interception points and filter duplicates" )
	@Test
	void testItCanRegisterInterceptionPoints() {
		InterceptorService service = InterceptorService.getInstance();
		service.registerInterceptionPoint( "onRequestStart", "onRequestStart", "onRequestEnd" );
		assertThat( service.getInterceptionPoints() ).containsExactly( "onRequestStart", "onRequestEnd" );
		assertThat( service.getInterceptionPoints().size() ).isEqualTo( 2 );
	}

	@DisplayName( "It can remove interception points" )
	@Test
	void testItCanRemoveInterceptionPoints() {
		InterceptorService service = InterceptorService.getInstance();
		service.registerInterceptionPoint( "onRequestStart" );
		assertThat( service.hasInterceptionPoint( "onRequestStart" ) ).isTrue();

		service.removeInterceptionPoint( "onRequestStart" );
		assertThat( service.hasInterceptionPoint( "onRequestStart" ) ).isFalse();
	}

	@DisplayName( "It can register new interception states with an existing point" )
	@Test
	void testItCanRegisterInterceptionStatesWithExistingPoint() {
		InterceptorService service = InterceptorService.getInstance();
		service.registerInterceptionPoint( "onRequestStart" );
		service.registerState( "onRequestStart" );

		assertThat( service.hasState( "onRequestStart" ) ).isTrue();
		assertThat( service.getState( "onRequestStart" ).getName() ).isEqualTo( "onRequestStart" );

		service.removeState( "onRequestStart" );
		assertThat( service.hasState( "onRequestStart" ) ).isFalse();
	}

	@DisplayName( "It can register new interception states with a non-existing point" )
	@Test
	void testItCanRegisterInterceptionStatesWithNonExistingPoint() {
		InterceptorService service = InterceptorService.getInstance();
		service.registerState( "onRequestStart" );

		assertThat( service.hasState( "onRequestStart" ) ).isTrue();
		assertThat( service.getState( "onRequestStart" ).getName() ).isEqualTo( "onRequestStart" );

		service.removeState( "onRequestStart" );
		assertThat( service.hasState( "onRequestStart" ) ).isFalse();
	}

	@DisplayName( "it can register new interceptors" )
	@Test
	void testItCanRegisterInterceptors() {
		InterceptorService	service			= InterceptorService.getInstance();
		DynamicObject		mockInterceptor	= DynamicObject.of( new MockInterceptor() );

		service.register(
		        mockInterceptor,
		        "onRequestStart"
		);

		assertThat( service.getState( "onRequestStart" ).size() ).isEqualTo( 1 );
		assertThat( service.getState( "onRequestStart" ).exists( mockInterceptor ) ).isTrue();
	}

	@DisplayName( "it can unregister interceptors with a specific state" )
	@Test
	void testItCanUnregisterInterceptors() {
		InterceptorService	service			= InterceptorService.getInstance();
		DynamicObject		mockInterceptor	= DynamicObject.of( new MockInterceptor() );

		service.register(
		        mockInterceptor,
		        "onRequestStart"
		);

		assertThat( service.getState( "onRequestStart" ).exists( mockInterceptor ) ).isTrue();

		service.unregister(
		        mockInterceptor,
		        "onRequestStart"
		);

		assertThat( service.getState( "onRequestStart" ).exists( mockInterceptor ) ).isFalse();
	}

	@DisplayName( "it can unregister interceptors with all states" )
	@Test
	void testItCanUnregisterInterceptorsWithAllStates() {
		InterceptorService	service			= InterceptorService.getInstance();
		DynamicObject		mockInterceptor	= DynamicObject.of( new MockInterceptor() );

		service.register(
		        mockInterceptor,
		        "onRequestStart", "onRequestEnd"
		);

		assertThat( service.getState( "onRequestStart" ).exists( mockInterceptor ) ).isTrue();
		assertThat( service.getState( "onRequestEnd" ).exists( mockInterceptor ) ).isTrue();

		service.unregister( mockInterceptor );

		assertThat( service.getState( "onRequestStart" ).exists( mockInterceptor ) ).isFalse();
		assertThat( service.getState( "onRequestEnd" ).exists( mockInterceptor ) ).isFalse();
	}

	@DisplayName( "it can announce an event to a specific state" )
	@Test
	void testItCanAnnounceEventToSpecificState() throws Throwable {
		InterceptorService	service				= InterceptorService.getInstance();
		DynamicObject		mockInterceptor1	= DynamicObject.of( new MockInterceptor() );
		DynamicObject		mockInterceptor2	= DynamicObject.of( new MockInterceptor() );

		service.register(
		        mockInterceptor1,
		        "onRequestStart"
		);
		service.register(
		        mockInterceptor2,
		        "onRequestStart"
		);

		assertThat( service.getState( "onRequestStart" ).size() ).isEqualTo( 2 );

		Struct data = new Struct();
		data.put( "counter", 0 );

		service.announce(
		        "onRequestStart",
		        data
		);

		assertThat( data.get( "counter" ) ).isEqualTo( 2 );
	}

}
