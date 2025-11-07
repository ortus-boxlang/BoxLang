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
				result = serverSideEventConsumer(
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
				result = serverSideEventConsumer(
					url = "https://example.com/events"
				);
			""", context);
			// @formatter:on
		} );
	}
}