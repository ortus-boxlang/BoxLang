/**
 * [BoxLang]
 *
 * Copyright [2024] [Ortus Solutions, Corp]
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
package ortus.boxlang.runtime.components.web;

import java.util.Set;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.WebRequestBoxContext;

@BoxComponent
public class Location extends Component {

	public Location() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.URL, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.addToken, "boolean" ),
		    new Attribute( Key.statusCode, "integer", 302, Set.of( Validator.min( 301 ), Validator.max( 399 ) ) )
		};
	}

	/**
	 * Generates custom HTTP response headers to return to the client.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * 
	 * @atribute.URL The URL of web page to open.
	 * 
	 * @arguments.addToken clientManagement must be enabled.
	 * 
	 * @argument.statusCode The HTTP status code.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String					URL				= attributes.getAsString( Key.URL );
		Boolean					addToken		= attributes.getAsBoolean( Key.addToken );
		Integer					statusCode		= attributes.getAsInteger( Key.statusCode );

		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		HttpServerExchange		exchange		= requestContext.getExchange();

		exchange.setStatusCode( statusCode );
		exchange.getResponseHeaders().put( new HttpString( "location" ), URL );

		throw new AbortException();
	}
}
