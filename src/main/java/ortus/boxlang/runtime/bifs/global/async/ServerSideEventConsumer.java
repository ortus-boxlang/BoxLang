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
package ortus.boxlang.runtime.bifs.global.async;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.net.SSEConsumer;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( alias = "SSEConsumer", description = "Represents a server-side consumer for Server-Sent Events (SSE) streams." )
public class ServerSideEventConsumer extends BIF {

	/**
	 * Constructor for ServerSideEventConsumer BIF.
	 */
	public ServerSideEventConsumer() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.URL ),
		    new Argument( false, Argument.FUNCTION, Key.onMessage ),
		    new Argument( false, Argument.FUNCTION, Key.onEvent ),
		    new Argument( false, Argument.FUNCTION, Key.onError ),
		    new Argument( false, Argument.FUNCTION, Key.onOpen ),
		    new Argument( false, Argument.FUNCTION, Key.onClose ),
		    new Argument( false, Argument.NUMERIC, Key.timeout, SSEConsumer.DEFAULT_TIMEOUT ),
		    new Argument( false, Argument.NUMERIC, Key.idleTimeout, SSEConsumer.DEFAULT_IDLE_TIMEOUT ),
		    new Argument( false, Argument.NUMERIC, Key.maxReconnects, SSEConsumer.DEFAULT_MAX_RECONNECTS ),
		    new Argument( false, Argument.NUMERIC, Key.reconnectDelay, SSEConsumer.DEFAULT_RECONNECT_DELAY ),
		    new Argument( false, Argument.STRUCT, Key.headers, Struct.EMPTY ),
		    new Argument( false, Argument.STRING, Key.userAgent, SSEConsumer.DEFAULT_USER_AGENT ),
		    new Argument( false, Argument.STRING, Key.username ),
		    new Argument( false, Argument.STRING, Key.password ),
		    new Argument( false, Argument.STRING, Key.proxyServer ),
		    new Argument( false, Argument.NUMERIC, Key.proxyPort, SSEConsumer.DEFAULT_PROXY_PORT ),
		    new Argument( false, Argument.STRING, Key.proxyUser ),
		    new Argument( false, Argument.STRING, Key.proxyPassword )
		};
	}

	/**
	 * Creates and configures a Server-Sent Events (SSE) consumer for real-time
	 * event streaming.
	 *
	 * This BIF provides a comprehensive SSE client implementation that supports:
	 * - Custom callback functions for different event types
	 * - HTTP authentication and proxy support
	 * - Automatic reconnection with configurable retry logic
	 * - Custom headers and User-Agent strings
	 * - Idle timeouts and connection management
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope containing SSE configuration parameters.
	 *
	 * @argument.url The SSE endpoint URL to connect to (required).
	 *
	 * @argument.onMessage Callback function invoked when receiving SSE data
	 *                     messages.
	 *                     Function signature: onMessage(data, event, sseConsumer)
	 *
	 * @argument.onEvent Callback function invoked when receiving named SSE events.
	 *                   Function signature: onEvent(eventType, data, event,
	 *                   sseConsumer)
	 *
	 * @argument.onError Callback function invoked when connection errors occur.
	 *                   Function signature: onError(error, sseConsumer)
	 *
	 * @argument.onOpen Callback function invoked when connection is established.
	 *                  Function signature: onOpen(sseConsumer)
	 *
	 * @argument.onClose Callback function invoked when connection is closed.
	 *                   Function signature: onClose(sseConsumer)
	 *
	 * @argument.timeout Connection timeout in seconds (default: 30).
	 *
	 * @argument.idleTimeout Maximum idle time in seconds before closing connection.
	 *                       Use 0 for no timeout (default: 0).
	 *
	 * @argument.maxReconnects Maximum number of reconnection attempts (default: 5).
	 *
	 * @argument.reconnectDelay Initial reconnection delay in milliseconds (default:
	 *                          1000).
	 *
	 * @argument.headers Additional HTTP headers to include in the request.
	 *
	 * @argument.userAgent Custom User-Agent header value (default:
	 *                     "BoxLang/version").
	 *
	 * @argument.username Username for HTTP basic authentication.
	 *
	 * @argument.password Password for HTTP basic authentication.
	 *
	 * @argument.proxyServer Proxy server hostname for HTTP requests.
	 *
	 * @argument.proxyPort Proxy server port (default: 8080).
	 *
	 * @argument.proxyUser Username for proxy authentication.
	 *
	 * @argument.proxyPassword Password for proxy authentication.
	 *
	 * @return A configured SSEConsumer instance ready for connecting to SSE
	 *         streams.
	 *
	 * @throws BoxRuntimeException If required parameters are missing or invalid.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get required arguments
		String		url			= arguments.getAsString( Key.URL );
		Function	onMessage	= arguments.getAsFunction( Key.onMessage );
		Function	onEvent		= arguments.getAsFunction( Key.onEvent );

		// If both are null, throw an error
		if ( onMessage == null && onEvent == null ) {
			throw new BoxRuntimeException(
			    "At least one of 'onMessage' or 'onEvent' callback functions must be provided." );
		}

		// Start building the SSEConsumer
		return SSEConsumer.builder()
		    .url( url )
		    .context( context )
		    .onMessage( onMessage )
		    .onError( arguments.getAsFunction( Key.onError ) )
		    .onOpen( arguments.getAsFunction( Key.onOpen ) )
		    .onClose( arguments.getAsFunction( Key.onClose ) )
		    .onEvent( onEvent )
		    .timeout( arguments.getAsInteger( Key.timeout ) )
		    .idleTimeout( arguments.getAsInteger( Key.idleTimeout ) )
		    .maxReconnects( arguments.getAsInteger( Key.maxReconnects ) )
		    .reconnectDelay( LongCaster.cast( arguments.get( Key.reconnectDelay ) ) )
		    .headers( arguments.getAsStruct( Key.headers ) )
		    .userAgent( arguments.getAsString( Key.userAgent ) )
		    .proxy( arguments.getAsString( Key.proxyServer ), arguments.getAsInteger( Key.proxyPort ) )
		    .proxyAuth( arguments.getAsString( Key.proxyUser ), arguments.getAsString( Key.proxyPassword ) )
		    .basicAuth( arguments.getAsString( Key.username ), arguments.getAsString( Key.password ) )
		    .build();
	}
}
