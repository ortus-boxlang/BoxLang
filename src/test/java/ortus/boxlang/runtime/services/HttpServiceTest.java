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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class HttpServiceTest {

	HttpService	service;
	BoxRuntime	runtime;

	@BeforeEach
	public void setupBeforeEach() {
		runtime	= BoxRuntime.getInstance( true );
		service	= runtime.getHttpService();
		// Clear all clients before each test to ensure test isolation
		service.clearAllClients();
	}

	@AfterEach
	public void tearDown() {
		// Clear all clients after each test to ensure test isolation
		service.clearAllClients();
	}

	@DisplayName( "Test it can get an instance of the service" )
	@Test
	void testItCanGetInstance() {
		assertThat( service ).isNotNull();
		assertThat( service.getLogger() ).isNotNull();
		assertThat( service.getHttpExecutor() ).isNotNull();
	}

	@DisplayName( "Test client count starts at zero" )
	@Test
	void testClientCountStartsAtZero() {
		assertThat( service.getClientCount() ).isEqualTo( 0 );
	}

	@DisplayName( "Test getOrBuildClient creates a new client" )
	@Test
	void testGetOrBuildClientCreatesNewClient() {
		BoxHttpClient client = service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client ).isNotNull();
		assertThat( service.getClientCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Test getOrBuildClient caches clients with same configuration" )
	@Test
	void testGetOrBuildClientCachesClients() {
		BoxHttpClient	client1	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		BoxHttpClient	client2	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client1 ).isSameInstanceAs( client2 );
		assertThat( service.getClientCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Test getOrBuildClient creates different clients for different HTTP versions" )
	@Test
	void testDifferentHttpVersionsCreateDifferentClients() {
		BoxHttpClient	client1	= service.getOrBuildClient(
		    "HTTP/1.1",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		BoxHttpClient	client2	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client1 ).isNotSameInstanceAs( client2 );
		assertThat( service.getClientCount() ).isEqualTo( 2 );
	}

	@DisplayName( "Test getOrBuildClient creates different clients for different redirect settings" )
	@Test
	void testDifferentRedirectSettingsCreateDifferentClients() {
		BoxHttpClient	client1	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		BoxHttpClient	client2	= service.getOrBuildClient(
		    "HTTP/2",
		    false,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client1 ).isNotSameInstanceAs( client2 );
		assertThat( service.getClientCount() ).isEqualTo( 2 );
	}

	@DisplayName( "Test getOrBuildClient creates different clients for different timeouts" )
	@Test
	void testDifferentTimeoutsCreateDifferentClients() {
		BoxHttpClient	client1	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		BoxHttpClient	client2	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    60,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client1 ).isNotSameInstanceAs( client2 );
		assertThat( service.getClientCount() ).isEqualTo( 2 );
	}

	@DisplayName( "Test getOrBuildClient creates different clients for different debug settings" )
	@Test
	void testDifferentDebugSettingsCreateDifferentClients() {
		BoxHttpClient	client1	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		BoxHttpClient	client2	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    true
		);

		assertThat( client1 ).isNotSameInstanceAs( client2 );
		assertThat( service.getClientCount() ).isEqualTo( 2 );
	}

	@DisplayName( "Test getOrBuildClient with proxy configuration" )
	@Test
	void testGetOrBuildClientWithProxy() {
		BoxHttpClient client = service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    "proxy.example.com",
		    8080,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client ).isNotNull();
		assertThat( service.getClientCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Test getOrBuildClient with proxy authentication" )
	@Test
	void testGetOrBuildClientWithProxyAuth() {
		BoxHttpClient client = service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    "proxy.example.com",
		    8080,
		    "proxyuser",
		    "proxypass",
		    null,
		    null,
		    false
		);

		assertThat( client ).isNotNull();
		assertThat( service.getClientCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Test getOrBuildClient differentiates proxy configurations" )
	@Test
	void testDifferentProxyConfigurationsCreateDifferentClients() {
		BoxHttpClient	client1	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    "proxy1.example.com",
		    8080,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		BoxHttpClient	client2	= service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    "proxy2.example.com",
		    8080,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client1 ).isNotSameInstanceAs( client2 );
		assertThat( service.getClientCount() ).isEqualTo( 2 );
	}

	@DisplayName( "Test hasClient returns true for existing client" )
	@Test
	void testHasClientReturnsTrue() {
		service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		Key clientKey = service.buildClientKey( "HTTP/2", true, 30, null, null, null, null, null, null, false );
		assertThat( service.hasClient( clientKey ) ).isTrue();
	}

	@DisplayName( "Test hasClient returns false for non-existing client" )
	@Test
	void testHasClientReturnsFalse() {
		Key clientKey = service.buildClientKey( "HTTP/1.1", false, 60, null, null, null, null, null, null, false );
		assertThat( service.hasClient( clientKey ) ).isFalse();
	}

	@DisplayName( "Test getClient returns null for non-existing client" )
	@Test
	void testGetClientReturnsNull() {
		Key clientKey = Key.of( "non-existing-key" );
		assertThat( service.getClient( clientKey ) ).isNull();
	}

	@DisplayName( "Test removeClient removes the client" )
	@Test
	void testRemoveClient() {
		service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( service.getClientCount() ).isEqualTo( 1 );

		Key clientKey = service.buildClientKey( "HTTP/2", true, 30, null, null, null, null, null, null, false );
		service.removeClient( clientKey );

		assertThat( service.getClientCount() ).isEqualTo( 0 );
		assertThat( service.hasClient( clientKey ) ).isFalse();
	}

	@DisplayName( "Test removeClient returns service for method chaining" )
	@Test
	void testRemoveClientReturnsService() {
		service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		Key			clientKey		= service.buildClientKey( "HTTP/2", true, 30, null, null, null, null, null, null, false );
		HttpService	returnedService	= service.removeClient( clientKey );

		assertThat( returnedService ).isSameInstanceAs( service );
	}

	@DisplayName( "Test getOrBuildClient with invalid client certificate throws exception" )
	@Test
	void testGetOrBuildClientWithInvalidCertThrowsException() {
		assertThrows( BoxRuntimeException.class, () -> {
			service.getOrBuildClient(
			    "HTTP/2",
			    true,
			    30,
			    null,
			    null,
			    null,
			    null,
			    "/path/to/nonexistent/cert.p12",
			    "password",
			    false
			);
		} );
	}

	@DisplayName( "Test default HTTP version when null is provided" )
	@Test
	void testDefaultHttpVersionWhenNull() {
		BoxHttpClient client = service.getOrBuildClient(
		    null,
		    true,
		    30,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client ).isNotNull();
		// Should default to HTTP/2
		assertThat( service.getClientCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Test null timeout is handled correctly" )
	@Test
	void testNullTimeoutHandledCorrectly() {
		BoxHttpClient client = service.getOrBuildClient(
		    "HTTP/2",
		    true,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    null,
		    false
		);

		assertThat( client ).isNotNull();
		assertThat( service.getClientCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Test multiple clients can coexist" )
	@Test
	void testMultipleClientsCanCoexist() {
		BoxHttpClient	client1	= service.getOrBuildClient( "HTTP/1.1", true, 30, null, null, null, null, null, null, false );
		BoxHttpClient	client2	= service.getOrBuildClient( "HTTP/2", true, 30, null, null, null, null, null, null, false );
		BoxHttpClient	client3	= service.getOrBuildClient( "HTTP/2", false, 30, null, null, null, null, null, null, false );
		BoxHttpClient	client4	= service.getOrBuildClient( "HTTP/2", true, 60, null, null, null, null, null, null, false );

		assertThat( service.getClientCount() ).isEqualTo( 4 );
		assertThat( client1 ).isNotSameInstanceAs( client2 );
		assertThat( client2 ).isNotSameInstanceAs( client3 );
		assertThat( client3 ).isNotSameInstanceAs( client4 );
	}

}
