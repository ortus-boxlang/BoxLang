/**
 * [BoxLang]
 *
 * Copyright [2024] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.web;

import java.util.Set;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.WebRequestBoxContext;

@BoxBIF
public class Location extends BIF {

	/**
	 * Constructor
	 */
	public Location() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.URL, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( false, "boolean", Key.addToken ),
		    new Argument( false, "integer", Key.statusCode, 302, Set.of( Validator.min( 301 ), Validator.max( 399 ) ) )
		};
	}

	/**
	 * 
	 * Relocates to a different pages.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.URL The URL of web page to open.
	 * 
	 * @arguments.addToken clientManagement must be enabled.
	 * 
	 * @argument.statusCode The HTTP status code.
	 *                      Values:
	 *                      - 300
	 *                      - 301
	 *                      - 302
	 *                      - 303
	 *                      - 304
	 *                      - 305
	 *                      - 306
	 *                      - 307
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {

		String					URL				= arguments.getAsString( Key.URL );
		Boolean					addToken		= arguments.getAsBoolean( Key.addToken );
		Integer					statusCode		= arguments.getAsInteger( Key.statusCode );

		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		HttpServerExchange		exchange		= requestContext.getExchange();

		exchange.setStatusCode( statusCode );
		exchange.getResponseHeaders().put( new HttpString( "location" ), URL );

		throw new AbortException();
	}

}
