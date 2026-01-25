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
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.net;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Returns a fluent SOAP client for making SOAP web service requests" )
public class SOAP extends BIF {

	private static final HttpService httpService = BoxRuntime.getInstance().getHttpService();

	/**
	 * Constructor
	 */
	public SOAP() {
		super();
		declaredArguments = new Argument[] {
		    // Target WSDL URL
		    new Argument( true, Argument.STRING, Key.URL )
		};
	}

	/**
	 * Returns a fluent SOAP client for making SOAP web service requests.
	 *
	 * This BIF creates a new or retrieves an existing BoxSoapClient instance based on the provided
	 * WSDL URL. The client can be used to fluently invoke SOAP operations discovered from the WSDL.
	 * The client provides fluent methods for configuring connection settings like setTimeout(),
	 * setAuthentication(), and addHeader().
	 *
	 * Example usage:
	 *
	 * <pre>
	 * // Create a SOAP client from WSDL
	 * ws = soap( "http://example.com/service.wsdl" )
	 *
	 * // Invoke a SOAP operation: methodName, passing arguments as an array
	 * result = ws.invoke( "methodName", [ arg1, arg2 ] )
	 *
	 * // With authentication and timeout configuration
	 * ws = soap( "http://example.com/service.wsdl" )
	 *     .setTimeout( 60 )
	 *     .setAuthentication( "user", "pass" )
	 *
	 * result = ws.invoke( "methodName", [ arg1, arg2 ] )
	 * </pre>
	 *
	 * @param context   The BoxLang execution context
	 * @param arguments The arguments provided to the BIF
	 *
	 * @argument.url The URL to the WSDL document describing the web service
	 *
	 * @return A BoxSoapClient instance configured with WSDL operations
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String wsdlUrl = arguments.getAsString( Key.URL );

		// Get or create the SOAP client from the HttpService (cached)
		return httpService.getOrCreateSoapClient( wsdlUrl, context );
	}

}
