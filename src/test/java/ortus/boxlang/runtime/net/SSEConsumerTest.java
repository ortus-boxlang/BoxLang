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
package ortus.boxlang.runtime.net;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class SSEConsumerTest {

	private ScriptingRequestBoxContext context;

	@BeforeEach
	void setUp() {
		context = new ScriptingRequestBoxContext();
	}

	@Test
	@DisplayName( "Should build SSEConsumer with required parameters" )
	void testBasicBuilder() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.isClosed() ).isFalse();
		assertThat( consumer.getReconnectAttempts() ).isEqualTo( 0 );
		assertThat( consumer.getLastEventId() ).isEqualTo( -1 );
	}

	@Test
	@DisplayName( "Should throw exception when URL is missing" )
	void testBuilderMissingUrl() {
		// Act & Assert
		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () -> {
			new SSEConsumer.Builder()
			    .context( context )
			    .build();
		} );

		assertThat( exception.getMessage() ).isEqualTo( "URL is required" );
	}

	@Test
	@DisplayName( "Should throw exception when context is missing" )
	void testBuilderMissingContext() {
		// Act & Assert
		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () -> {
			new SSEConsumer.Builder()
			    .url( "https://example.com/events" )
			    .build();
		} );

		assertThat( exception.getMessage() ).isEqualTo( "Context is required" );
	}

	@Test
	@DisplayName( "Should throw exception when URL is empty" )
	void testBuilderEmptyUrl() {
		// Act & Assert
		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () -> {
			new SSEConsumer.Builder()
			    .url( "" )
			    .context( context )
			    .build();
		} );

		assertThat( exception.getMessage() ).isEqualTo( "URL is required" );
	}

	@Test
	@DisplayName( "Should build SSEConsumer with configuration options" )
	void testBuilderWithOptions() {
		IStruct		headers		= Struct.of(
		    Key.of( "Authorization" ), "Bearer token123",
		    Key.of( "X-Custom-Header" ), "custom-value"
		);

		// Act
		SSEConsumer	consumer	= new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .headers( headers )
		    .header( "X-Additional", "additional-value" )
		    .timeout( 60 )
		    .maxReconnects( 10 )
		    .reconnectDelay( 2000 )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.isClosed() ).isFalse();
		assertThat( consumer.getReconnectAttempts() ).isEqualTo( 0 );
		assertThat( consumer.getLastEventId() ).isEqualTo( -1 );
	}

	@Test
	@DisplayName( "Should be closed after calling close()" )
	void testClose() {
		// Arrange
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .build();

		// Act
		consumer.close();

		// Assert
		assertThat( consumer.isClosed() ).isTrue();

		// Calling close again should be idempotent
		consumer.close();
		assertThat( consumer.isClosed() ).isTrue();
	}

	@Test
	@DisplayName( "Should handle builder method chaining" )
	void testBuilderChaining() {
		// Act
		SSEConsumer.Builder	builder		= new SSEConsumer.Builder();
		SSEConsumer			consumer	= builder
		    .url( "https://example.com/events" )
		    .context( context )
		    .timeout( 45 )
		    .maxReconnects( 3 )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
	}

	@Test
	@DisplayName( "Should handle multiple headers correctly" )
	void testMultipleHeaders() {
		// Arrange
		IStruct initialHeaders = Struct.of(
		    Key.of( "Header1" ), "Value1",
		    Key.of( "Header2" ), "Value2"
		);

		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .headers( initialHeaders )
		    .header( "Header3", "Value3" )
		    .header( "Header4", "Value4" )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
	}

	@Test
	@DisplayName( "Should use default values when not specified" )
	void testDefaultValues() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getReconnectAttempts() ).isEqualTo( 0 );
		assertThat( consumer.getLastEventId() ).isEqualTo( -1 );
		assertThat( consumer.isClosed() ).isFalse();
	}

	@Test
	@DisplayName( "Should track reconnection attempts" )
	void testReconnectionTracking() {
		// Arrange
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .maxReconnects( 5 )
		    .build();

		// Act & Assert - initial state
		assertThat( consumer.getReconnectAttempts() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Should track last event ID" )
	void testLastEventIdTracking() {
		// Arrange
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .build();

		// Act & Assert - initial state
		assertThat( consumer.getLastEventId() ).isEqualTo( -1 );
	}

	@Test
	@DisplayName( "Should handle null callback functions gracefully" )
	void testNullCallbacks() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .onMessage( null )
		    .onOpen( null )
		    .onClose( null )
		    .onError( null )
		    .onEvent( null )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();

		// Should be able to close without issues even with null callbacks
		consumer.close();
		assertThat( consumer.isClosed() ).isTrue();
	}

	@Test
	@DisplayName( "Should validate builder parameters correctly" )
	void testBuilderValidation() {
		SSEConsumer.Builder builder = new SSEConsumer.Builder();

		// Should accept valid timeout
		builder.timeout( 30 );
		builder.timeout( 1 );
		builder.timeout( 300 );

		// Should accept valid reconnect attempts
		builder.maxReconnects( 0 );
		builder.maxReconnects( 10 );
		builder.maxReconnects( 100 );

		// Should accept valid reconnect delay
		builder.reconnectDelay( 0 );
		builder.reconnectDelay( 1000 );
		builder.reconnectDelay( 60000 );

		// Build should work with valid parameters
		SSEConsumer consumer = builder
		    .url( "https://example.com/events" )
		    .context( context )
		    .build();

		assertThat( consumer ).isNotNull();
	}

	@Test
	@DisplayName( "Should support HTTP basic authentication" )
	void testBasicAuthentication() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .username( "testuser" )
		    .password( "testpass" )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getUsername() ).isEqualTo( "testuser" );
		assertThat( consumer.hasBasicAuth() ).isTrue();
	}

	@Test
	@DisplayName( "Should support basic auth convenience method" )
	void testBasicAuthConvenienceMethod() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .basicAuth( "admin", "secret" )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getUsername() ).isEqualTo( "admin" );
		assertThat( consumer.hasBasicAuth() ).isTrue();
	}

	@Test
	@DisplayName( "Should return false for hasBasicAuth when credentials incomplete" )
	void testIncompleteBasicAuth() {
		// Username only
		SSEConsumer consumer1 = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .username( "testuser" )
		    .build();

		assertThat( consumer1.hasBasicAuth() ).isFalse();

		// Password only
		SSEConsumer consumer2 = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .password( "testpass" )
		    .build();

		assertThat( consumer2.hasBasicAuth() ).isFalse();

		// Neither
		SSEConsumer consumer3 = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .build();

		assertThat( consumer3.hasBasicAuth() ).isFalse();
		assertThat( consumer3.getUsername() ).isNull();
	}

	@Test
	@DisplayName( "Should support proxy configuration" )
	void testProxyConfiguration() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .proxy( "proxy.example.com", 8080 )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getProxyHost() ).isEqualTo( "proxy.example.com" );
		assertThat( consumer.getProxyPort() ).isEqualTo( 8080 );
		assertThat( consumer.hasProxy() ).isTrue();
		assertThat( consumer.hasProxyAuth() ).isFalse();
	}

	@Test
	@DisplayName( "Should support proxy with authentication" )
	void testProxyWithAuthentication() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .proxy( "proxy.example.com", 3128, "proxyuser", "proxypass" )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getProxyHost() ).isEqualTo( "proxy.example.com" );
		assertThat( consumer.getProxyPort() ).isEqualTo( 3128 );
		assertThat( consumer.getProxyUsername() ).isEqualTo( "proxyuser" );
		assertThat( consumer.hasProxy() ).isTrue();
		assertThat( consumer.hasProxyAuth() ).isTrue();
	}

	@Test
	@DisplayName( "Should support separate proxy configuration methods" )
	void testSeparateProxyMethods() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .proxyHost( "corporate-proxy.com" )
		    .proxyPort( 8080 )
		    .proxyUsername( "employee" )
		    .proxyPassword( "secret" )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getProxyHost() ).isEqualTo( "corporate-proxy.com" );
		assertThat( consumer.getProxyPort() ).isEqualTo( 8080 );
		assertThat( consumer.getProxyUsername() ).isEqualTo( "employee" );
		assertThat( consumer.hasProxy() ).isTrue();
		assertThat( consumer.hasProxyAuth() ).isTrue();
	}

	@Test
	@DisplayName( "Should support proxy auth convenience method" )
	void testProxyAuthConvenienceMethod() {
		// Act
		SSEConsumer consumer = new SSEConsumer.Builder()
		    .url( "https://example.com/events" )
		    .context( context )
		    .proxyHost( "proxy.local" )
		    .proxyPort( 8080 )
		    .proxyAuth( "admin", "password123" )
		    .build();

		// Assert
		assertThat( consumer ).isNotNull();
		assertThat( consumer.getProxyHost() ).isEqualTo( "proxy.local" );
		assertThat( consumer.getProxyPort() ).isEqualTo( 8080 );
		assertThat( consumer.getProxyUsername() ).isEqualTo( "admin" );
		assertThat( consumer.hasProxy() ).isTrue();
		assertThat( consumer.hasProxyAuth() ).isTrue();
	}
}