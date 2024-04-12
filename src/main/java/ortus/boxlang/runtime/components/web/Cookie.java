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
import io.undertow.server.handlers.CookieImpl;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.web.WebRequestBoxContext;

@BoxComponent
public class Cookie extends Component {

	public Cookie() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.value, "string", "" ),
		    new Attribute( Key.secure, "boolean" ),
		    new Attribute( Key.httpOnly, "boolean" ),
		    new Attribute( Key.expires, "any" ),
		    new Attribute( Key.samesite, "string" ),
		    new Attribute( Key.path, "string" ),
		    new Attribute( Key.domain, "string" )
		};
	}

	/**
	 * Defines web browser cookie variables, including expiration and security options.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * 
	 * @atribute.name Name of cookie variable. Converts cookie names
	 *                to all-uppercase. Cookie names set using this tag can
	 *                include any printable ASCII characters except commas,
	 *                semicolons or white space characters.
	 * 
	 * @atribute.value Value to assign to cookie variable. Must be a string or
	 *                 variable that can be stored as a string.
	 * 
	 * @atribute.secure If browser does not support Secure Sockets Layer (SSL)
	 *                  security, the cookie is not sent. To use the cookie, the
	 *                  page must be accessed using the https protocol.
	 * 
	 * @atribute.httpOnly Specify whether cookie is http cookie or not
	 * 
	 * @atribute.expires Expiration of cookie variable.
	 * 
	 *                   - The default: the cookie expires when the user closes the
	 *                   browser, that is, the cookie is "session only".
	 *                   - A date or date/time object (for example, 10/09/97)
	 *                   - A number of days (for example, 10, or 100)
	 *                   - now: deletes cookie from client cookie.txt file
	 *                   (but does not delete the corresponding variable the
	 *                   Cookie scope of the active page).
	 *                   - never: The cookie expires in 30 years from the time it
	 *                   was created (effectively never in web years).
	 * 
	 * @atribute.samesite Tells browsers when and how to fire cookies in first- or third-party situations. SameSite is used to identify whether or not to
	 *                    allow a cookie to be accessed.
	 *                    Values:
	 *                    - strict
	 *                    - lax
	 *                    - none
	 * 
	 * @atribute.path URL, within a domain, to which the cookie applies;
	 *                typically a directory. Only pages in this path can use the
	 *                cookie. By default, all pages on the server that set the
	 *                cookie can access the cookie.
	 * 
	 * @atribute.domain Domain in which cookie is valid and to which cookie content
	 *                  can be sent from the user's system. By default, the cookie
	 *                  is only available to the server that set it. Use this
	 *                  attribute to make the cookie available to other servers.
	 * 
	 *                  Must start with a period. If the value is a subdomain, the
	 *                  valid domain is all domain names that end with this string.
	 *                  This attribute sets the available subdomains on the site
	 *                  upon which the cookie can be used.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String					name			= attributes.getAsString( Key._NAME );
		String					value			= attributes.getAsString( Key.value );
		Boolean					secure			= attributes.getAsBoolean( Key.secure );
		Boolean					httpOnly		= attributes.getAsBoolean( Key.httpOnly );
		Object					expires			= attributes.get( Key.requestTimeout );
		String					samesite		= attributes.getAsString( Key.samesite );
		String					path			= attributes.getAsString( Key.path );
		String					domain			= attributes.getAsString( Key.domain );

		WebRequestBoxContext	requestContext	= context.getParentOfType( WebRequestBoxContext.class );

		HttpServerExchange		exchange		= requestContext.getExchange();

		CookieImpl				cookieInstance	= new CookieImpl( name, value );

		if ( secure != null ) {
			cookieInstance.setSecure( secure );
		}

		if ( httpOnly != null ) {
			cookieInstance.setHttpOnly( httpOnly );
		}

		// TODO: Implement a custom method into CookieImp.class to manage expires

		if ( samesite != null ) {
			cookieInstance.setSameSiteMode( samesite );
		}

		if ( path != null ) {
			cookieInstance.setPath( path );
		}

		if ( domain != null ) {
			cookieInstance.setDomain( domain );
		}

		exchange.setResponseCookie( cookieInstance );

		return DEFAULT_RETURN;
	}
}
