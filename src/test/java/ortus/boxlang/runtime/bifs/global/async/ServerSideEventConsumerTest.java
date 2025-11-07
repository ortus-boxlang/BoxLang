package ortus.boxlang.runtime.bifs.global.async;

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
import ortus.boxlang.runtime.net.SSEConsumer;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ServerSideEventConsumerTest {

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
		// Cleanup if needed
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Should create SSEConsumer with minimal required arguments" )
	@Test
	public void testMinimalSSEConsumer() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				}
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer ).isInstanceOf( SSEConsumer.class );
		assertThat( consumer.isClosed() ).isFalse();
		assertThat( consumer.getTimeoutSeconds() ).isEqualTo( 30 );
		assertThat( consumer.getUserAgent() ).contains( "BoxLang/" );
	}

	@DisplayName( "Should throw exception when URL is missing" )
	@Test
	public void testSSEConsumerMissingURL() {
		assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			instance.executeSource("""
				result = SSEConsumer(
					onMessage = ( data, event, consumer ) -> {
						// Handle message
					}
				);
			""", context);
			// @formatter:on
		} );
	}

	@DisplayName( "Should throw exception when onMessage callback is missing" )
	@Test
	public void testSSEConsumerMissingOnMessage() {
		assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			instance.executeSource("""
				result = SSEConsumer(
					url = "https://example.com/events"
				);
			""", context);
			// @formatter:on
		} );
	}

	@DisplayName( "Should create SSEConsumer with timeout configurations" )
	@Test
	public void testSSEConsumerWithTimeouts() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				timeout = 60,
				idleTimeout = 300,
				maxReconnects = 10,
				reconnectDelay = 2000
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getTimeoutSeconds() ).isEqualTo( 60 );
		assertThat( consumer.getIdleTimeoutSeconds() ).isEqualTo( 300 );
		assertThat( consumer.getMaxReconnects() ).isEqualTo( 10 );
		assertThat( consumer.getInitialReconnectDelay() ).isEqualTo( 2000L );
	}

	@DisplayName( "Should create SSEConsumer with custom headers" )
	@Test
	public void testSSEConsumerWithHeaders() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				headers = {
					"Authorization": "Bearer token123",
					"X-Custom-Header": "custom-value"
				}
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getHeaders().size() ).isEqualTo( 2 );
		assertThat( consumer.getHeaders().getAsString( Key.of( "Authorization" ) ) ).isEqualTo( "Bearer token123" );
		assertThat( consumer.getHeaders().getAsString( Key.of( "X-Custom-Header" ) ) ).isEqualTo( "custom-value" );
	}

	@DisplayName( "Should create SSEConsumer with custom user agent" )
	@Test
	public void testSSEConsumerWithUserAgent() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				userAgent = "MyApp/1.0.0"
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getUserAgent() ).isEqualTo( "MyApp/1.0.0" );
	}

	@DisplayName( "Should create SSEConsumer with HTTP basic authentication" )
	@Test
	public void testSSEConsumerWithBasicAuth() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				username = "testuser",
				password = "testpass"
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.hasBasicAuth() ).isTrue();
		assertThat( consumer.getUsername() ).isEqualTo( "testuser" );
	}

	@DisplayName( "Should create SSEConsumer with proxy configuration" )
	@Test
	public void testSSEConsumerWithProxy() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				proxyServer = "proxy.example.com",
				proxyPort = 3128
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.hasProxy() ).isTrue();
		assertThat( consumer.getProxyServer() ).isEqualTo( "proxy.example.com" );
		assertThat( consumer.getProxyPort() ).isEqualTo( 3128 );
	}

	@DisplayName( "Should create SSEConsumer with proxy authentication" )
	@Test
	public void testSSEConsumerWithProxyAuth() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				proxyServer = "proxy.example.com",
				proxyPort = 3128,
				proxyUser = "proxyuser",
				proxyPassword = "proxypass"
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.hasProxy() ).isTrue();
		assertThat( consumer.hasProxyAuth() ).isTrue();
		assertThat( consumer.getProxyServer() ).isEqualTo( "proxy.example.com" );
		assertThat( consumer.getProxyPort() ).isEqualTo( 3128 );
		assertThat( consumer.getProxyUser() ).isEqualTo( "proxyuser" );
	}

	@DisplayName( "Should create SSEConsumer with all event callbacks" )
	@Test
	public void testSSEConsumerWithAllCallbacks() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				},
				onEvent = ( eventType, data, event, consumer ) -> {
					// Handle named event
				},
				onError = ( error, consumer ) -> {
					// Handle error
				},
				onOpen = ( consumer ) -> {
					// Handle connection open
				},
				onClose = ( consumer ) -> {
					// Handle connection close
				}
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer ).isInstanceOf( SSEConsumer.class );
		assertThat( consumer.isClosed() ).isFalse();
	}

	@DisplayName( "Should verify default values" )
	@Test
	public void testSSEConsumerDefaultValues() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				}
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();

		// Verify default values
		assertThat( consumer.getTimeoutSeconds() ).isEqualTo( 30 );
		assertThat( consumer.getIdleTimeoutSeconds() ).isEqualTo( 0 );
		assertThat( consumer.getMaxReconnects() ).isEqualTo( 5 );
		assertThat( consumer.getInitialReconnectDelay() ).isEqualTo( 1000L );
		assertThat( consumer.getUserAgent() ).contains( "BoxLang/" );
		assertThat( consumer.getHeaders() ).isEmpty();
		assertThat( consumer.hasBasicAuth() ).isFalse();
		assertThat( consumer.hasProxy() ).isFalse();
	}

	@DisplayName( "Should verify initial reconnection state" )
	@Test
	public void testSSEConsumerInitialReconnectionState() {
		// @formatter:off
		instance.executeSource("""
			result = SSEConsumer(
				url = "https://example.com/events",
				onMessage = ( data, event, consumer ) -> {
					// Handle message
				}
			);
		""", context);
		// @formatter:on

		SSEConsumer consumer = ( SSEConsumer ) variables.get( result );
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getReconnectAttempts() ).isEqualTo( 0 );
		assertThat( consumer.getLastEventId() ).isEqualTo( -1 );
		assertThat( consumer.getCurrentReconnectDelay() ).isEqualTo( 1000L );
	}
}