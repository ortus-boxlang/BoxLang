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
	 * Tests for a parameter's existence, tests its data type, and, if a default value is not assigned, optionally provides one.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
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
