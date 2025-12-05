/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.net;

import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Returns a fluent HTTP client for making HTTP requests" )
public class HTTP extends BIF {

	private static final HttpService httpService = BoxRuntime.getInstance().getHttpService();

	/**
	 * Constructor
	 */
	public HTTP() {
		super();
		declaredArguments = new Argument[] {
		    // Target URL
		    new Argument( true, Argument.STRING, Key.URL ),
		    // Connection settings
		    new Argument( false, Argument.NUMERIC, Key.connectionTimeout, BoxHttpClient.DEFAULT_CONNECTION_TIMEOUT ),
		    new Argument( false, Argument.BOOLEAN, Key.redirect, true ),
		    new Argument( false, Argument.STRING, Key.httpVersion, BoxHttpClient.HTTP_2 ),
		    // Proxy Info
		    new Argument( false, Argument.STRING, Key.proxyServer, Set.of( Validator.requires( Key.proxyPort ) ) ),
		    new Argument( false, Argument.INTEGER, Key.proxyPort, Set.of( Validator.requires( Key.proxyServer ) ) ),
		    new Argument( false, Argument.STRING, Key.proxyUser, Set.of( Validator.requires( Key.proxyPassword ) ) ),
		    new Argument( false, Argument.STRING, Key.proxyPassword, Set.of( Validator.requires( Key.proxyUser ) ) ),
		    // Connection certificates
		    new Argument( false, Argument.STRING, Key.clientCert ),
		    new Argument( false, Argument.STRING, Key.clientCertPassword ),
		};
	}

	/**
	 * Returns a fluent HTTP client for making HTTP requests.
	 *
	 * This BIF creates a new or retrieves an existing BoxHttpClient instance based on the provided
	 * connection parameters. The client can be used to fluently build and execute HTTP requests
	 * with support for various HTTP versions, redirects, timeouts, proxy configuration, and client certificates.
	 *
	 * Example usage:
	 *
	 * <pre>
	 * // Simple GET request
	 * http( "https://api.example.com/data" )
	 *     .invoke()
	 *     .ifSuccess( result -> {
	 * 		// Handle successful response
	 * 	} )
	 *     .ifError( error -> {
	 * 		// Handle error response
	 * 	} )
	 *
	 * // Invoke and get the result immediately, throwing an exception on error
	 * result = http( "https://api.example.com/data" )
	 * 	.invokeAndGet().
	 * </pre>
	 *
	 * @param context   The BoxLang execution context
	 * @param arguments The arguments provided to the BIF
	 *
	 * @argument.connectionTimeout The connection timeout in milliseconds (default: BoxHttpClient.DEFAULT_CONNECTION_TIMEOUT)
	 *
	 * @argument.redirect Whether to automatically follow HTTP redirects (default: true)
	 *
	 * @argument.httpVersion The HTTP version to use (default: HTTP/2)
	 *
	 * @argument.proxyServer The proxy server address (requires proxyPort)
	 *
	 * @argument.proxyPort The proxy server port (requires proxyServer)
	 *
	 * @argument.proxyUser The proxy authentication username (requires proxyPassword)
	 *
	 * @argument.proxyPassword The proxy authentication password (requires proxyUser)
	 *
	 * @argument.clientCert The path to the client certificate file
	 *
	 * @argument.clientCertPassword The password for the client certificate
	 *
	 * @return A BoxHttpClient instance configured with the specified parameters
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get a new or existing BoxHttpClient
		return httpService.getOrBuildClient(
		    arguments.getAsString( Key.httpVersion ),
		    arguments.getAsBoolean( Key.redirect ),
		    arguments.getAsInteger( Key.connectionTimeout ),
		    arguments.getAsString( Key.proxyServer ),
		    arguments.getAsInteger( Key.proxyPort ),
		    arguments.getAsString( Key.proxyUser ),
		    arguments.getAsString( Key.proxyPassword ),
		    arguments.getAsString( Key.clientCert ),
		    arguments.getAsString( Key.clientCertPassword )
		)
		    .newRequest( arguments.getAsString( Key.URL ), context );
	}

}
