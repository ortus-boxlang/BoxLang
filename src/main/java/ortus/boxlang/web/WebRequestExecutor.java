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
package ortus.boxlang.web;

import java.net.URI;
import java.util.Map;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.ApplicationListener;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.util.FRTransService;

/**
 * I handle running a web request
 */
public class WebRequestExecutor {

	public static void execute( HttpServerExchange exchange, String webRoot, Boolean manageFullReqestLifecycle ) {

		WebRequestBoxContext	context			= null;
		DynamicObject			trans			= null;
		FRTransService			frTransService	= null;
		ApplicationListener		appListener		= null;
		Throwable				errorToHandle	= null;
		String					requestPath		= "";

		try {
			frTransService = FRTransService.getInstance( manageFullReqestLifecycle );

			// Process path info real quick
			// Path info is sort of a servlet concept. It's just everything left in the URI that didn't match the servlet mapping
			// In undertow, we can use predicates to match the path info and store it in the exchange attachment so we can get it in the CGI scope
			Map<String, Object>	predicateContext	= exchange.getAttachment( Predicate.PREDICATE_CONTEXT );
			String				pathInfo			= ( String ) predicateContext.get( "2" );
			if ( pathInfo != null ) {
				exchange.setRelativePath( ( String ) predicateContext.get( "1" ) );
				predicateContext.put( "pathInfo", pathInfo );
			} else {
				predicateContext.put( "pathInfo", "" );
			}
			// end path info processing

			requestPath	= exchange.getRelativePath();
			trans		= frTransService.startTransaction( "Web Request", requestPath );
			context		= new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange, webRoot );
			// Set default content type to text/html
			exchange.getResponseHeaders().put( new HttpString( "Content-Type" ), "text/html;charset=UTF-8" );
			// exchange.getResponseHeaders().put( new HttpString( "Content-Encoding" ), "UTF-8" );
			context.loadApplicationDescriptor( new URI( requestPath ) );
			appListener = context.getApplicationListener();
			boolean result = appListener.onRequestStart( context, new Object[] { requestPath } );
			if ( result ) {
				appListener.onRequest( context, new Object[] { requestPath } );
			}

			context.flushBuffer( false );
		} catch ( AbortException e ) {
			if ( appListener != null ) {
				try {
					appListener.onAbort( context, new Object[] { requestPath } );
				} catch ( Throwable ae ) {
					// Opps, an error while handling onAbort
					errorToHandle = ae;
				}
			}
			if ( context != null )
				context.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
		} catch ( MissingIncludeException e ) {
			try {
				// A return of true means the error has been "handled". False means the default error handling should be used
				if ( appListener == null || !appListener.onMissingTemplate( context, new Object[] { e.getMissingFileName() } ) ) {
					// If the Application listener didn't "handle" it, then let the default handling kick in below
					errorToHandle = e;
				}
			} catch ( Throwable t ) {
				// Opps, an error while handling the missing template error
				errorToHandle = t;
			}
			if ( context != null )
				context.flushBuffer( false );
		} catch ( Throwable e ) {
			errorToHandle = e;
		} finally {
			if ( appListener != null ) {
				try {
					appListener.onRequestEnd( context, new Object[] {} );
				} catch ( Throwable e ) {
					// Opps, an error while handling onRequestEnd
					errorToHandle = e;
				}
			}
			if ( context != null )
				context.flushBuffer( false );

			if ( errorToHandle != null ) {
				try {
					// A return of true means the error has been "handled". False means the default error handling should be used
					if ( appListener == null || !appListener.onError( context, new Object[] { errorToHandle, "" } ) ) {
						WebErrorHandler.handleError( errorToHandle, exchange, context, frTransService, trans );
					}
					// This is a failsafe in case the onError blows up.
				} catch ( Throwable t ) {
					WebErrorHandler.handleError( t, exchange, context, frTransService, trans );
				}
			}
			if ( context != null )
				context.flushBuffer( false );

			// for our embedded mini-server, we must finalize the request, but in a servlet context
			// the servlet does this for us.
			if ( manageFullReqestLifecycle ) {
				if ( context != null ) {
					context.finalizeResponse();
				}
				exchange.endExchange();
			}
			if ( frTransService != null ) {
				frTransService.endTransaction( trans );
			}
		}
	}

}
