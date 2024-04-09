/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
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

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.web.WebRequestBoxContext;

@BoxBIF
public class GetPageContext extends BIF {

	/**
	 * Constructor
	 */
	public GetPageContext() {
		super();
	}

	/**
	 * 
	 * Gets the current java PageContext object that provides access to page attributes and configuration, request and response objects.
	 * If not running in a servlet, this will be a fake class attempting to provide most of the common methods.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new PageContext( context );
	}

	public class PageContext {

		private HttpServerExchange		exchange;
		private WebRequestBoxContext	context;

		public PageContext( IBoxContext context ) {
			this.context	= context.getParentOfType( WebRequestBoxContext.class );
			this.exchange	= this.context.getExchange();
		}

		public PageContext getResponse() {
			return this;
		}

		public PageContext getRequest() {
			return this;
		}

		public void setHeader( String name, String value ) {
			if ( exchange.isResponseStarted() ) {
				return;
			}
			exchange.getResponseHeaders().put( new HttpString( name ), value );
		}

		public void setContentType( String value ) {
			if ( exchange.isResponseStarted() ) {
				return;
			}
			setHeader( "Content-Type", value );
		}

		public void setStatus( int code, String text ) {
			if ( exchange.isResponseStarted() ) {
				return;
			}
			exchange.setStatusCode( code );
			exchange.setReasonPhrase( text );
		}

		public void setStatus( int code ) {
			setStatus( code, "" );
		}

		public int getStatus() {
			return exchange.getStatusCode();
		}

		public void reset() {
			context.clearBuffer();
		}

		public StringBuffer getRequestURL() {
			return new StringBuffer( exchange.getRequestURL() );
		}

		// Getting response content type. If we need to get request content type, we'll need to spoof actual request and response objects
		public String getContentType() {
			String contentType = exchange.getResponseHeaders().getFirst( "Content-Type" );
			if ( contentType == null ) {
				return "text/html";
			} else {
				return contentType;
			}
		}

	}

}
