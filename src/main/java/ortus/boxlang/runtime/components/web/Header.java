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
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.WebRequestBoxContext;

@BoxComponent
public class Header extends Component {

	public Header() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of(
		        Validator.NON_EMPTY,
		        ( context, caller, record, records ) -> {
			        if ( !records.containsKey( Key.statusCode ) && !records.containsKey( Key._NAME ) ) {
				        throw new BoxValidationException( Key._NAME, record, "is required unless statusCode is provided." );
			        }
		        } ) ),
		    new Attribute( Key.value, "string", "" ),
		    new Attribute( Key.charset, "String", "UTF-8" ),
		    new Attribute( Key.statusCode, "integer", Set.of( Validator.min( 100 ), Validator.max( 599 ) ) ),
		    new Attribute( Key.statusText, "string", "" )
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
	 * @atribute.name Header name
	 * 
	 * @atribute.value HTTP header value
	 * 
	 * @atribute.charset The character encoding in which to encode the header value.
	 * 
	 * @atribute.statusCode The HTTP status code to return
	 * 
	 * @atribute.statusText The HTTP status text to return
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String					name			= attributes.getAsString( Key._NAME );
		String					value			= attributes.getAsString( Key.value );
		// TODO: Figure out how to use charset, if at all.
		String					charset			= attributes.getAsString( Key.charset );
		Integer					statusCode		= attributes.getAsInteger( Key.statusCode );
		String					statusText		= attributes.getAsString( Key.statusText );

		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );
		HttpServerExchange		exchange		= requestContext.getExchange();

		if ( statusCode != null ) {
			exchange.setStatusCode( statusCode );
			if ( statusText != null ) {
				exchange.setReasonPhrase( statusText );
			}
		} else {
			// TODO: check and see if we remove existing headers, or just append
			exchange.getResponseHeaders().add(
			    new HttpString( name ),
			    value
			);
		}

		return DEFAULT_RETURN;
	}
}
