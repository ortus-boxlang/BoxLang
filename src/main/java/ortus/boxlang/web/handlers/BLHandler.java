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
package ortus.boxlang.web.handlers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.ApplicationListener;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.util.FRTransService;
import ortus.boxlang.web.WebRequestBoxContext;

/**
 * Variables scope implementation in BoxLang
 */
public class BLHandler implements HttpHandler {

	@Override
	public void handleRequest( io.undertow.server.HttpServerExchange exchange ) throws Exception {
		if ( exchange.isInIoThread() ) {
			exchange.dispatch( this );
			return;
		}
		exchange.startBlocking();

		WebRequestBoxContext	context			= null;
		DynamicObject			trans			= null;
		FRTransService			frTransService	= null;
		ApplicationListener		appListener		= null;
		Throwable				errorToHandle	= null;
		String					requestPath		= "";

		try {
			frTransService = FRTransService.getInstance();

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
			context		= new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange );
			// Set default content type to text/html
			exchange.getResponseHeaders().put( new HttpString( "Content-Type" ), "text/html" );
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
						handleError( errorToHandle, exchange, context, frTransService, trans );
					}
					// This is a failsafe in case the onError blows up.
				} catch ( Throwable t ) {
					handleError( t, exchange, context, frTransService, trans );
				}
			}
			if ( context != null )
				context.flushBuffer( false );

			if ( context != null ) {
				context.finalizeResponse();
			}
			exchange.endExchange();
			if ( frTransService != null ) {
				frTransService.endTransaction( trans );
			}
		}
	}

	public void handleError( Throwable e, HttpServerExchange exchange, WebRequestBoxContext context, FRTransService frTransService, DynamicObject trans ) {
		try {
			e.printStackTrace();

			if ( frTransService != null ) {
				if ( e instanceof Exception ee ) {
					frTransService.errorTransaction( trans, ee );
				} else {
					frTransService.errorTransaction( trans, new Exception( e ) );
				}
			}

			if ( context != null ) {
				context.flushBuffer( true );
			}

			StringBuilder errorOutput = new StringBuilder();
			// styles
			errorOutput.append(
			    "<style>.bx-err {--bx-blue-grey-25: #C6C7CA;--bx-blue-grey-10: #E6E6E6;--bx-blue-grey-05: #F2F2F3;--bx-red-50:#DF2121;--bx-icon-chevron: url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='rgb(136, 145, 164)' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E\");--bx-text-danger: var(--bx-red-50);--bx-spacing: 16px;--bx-border-color: var(--bx-blue-grey-25);--bx-surface-low: var(--bx-blue-grey-10);--bx-surface-lowest: var(--bx-blue-grey-05);--bx-border-width: .0625rem;--bx-font-family-sans-serif: system-ui, \"Segoe UI\", Roboto, Oxygen, Ubuntu, Cantarell, Helvetica, Arial, \"Helvetica Neue\", sans-serif;font-family: var(--bx-font-family-sans-serif);}.bx-err header {--bx-text-color: #fff;color: var(--bx-text-color);padding: calc(var(--bx-spacing) / 2) var(--bx-spacing)}.bx-err .bx-err-body{padding:var(--bx-spacing)}.bx-err .bx-err-msg {padding:var(--bx-spacing);font-size:1.1rem;border:1px dashed var(--bx-text-danger);display:flex;gap:8px;line-height:1.3em;}.bx-err h1 {font-size: 1.4rem;margin:0px;display:flex;align-items:center;gap:8px;}.bx-err h2 {color:var(--bx-text-danger)}.bx-err summary[role=button] {--bx-background-color: var(--bx-surface-low);--bx-form-element-spacing-vertical: calc(var(--bx-spacing)/2);--bx-form-element-spacing-horizontal: var(--bx-spacing);--bx-color: #000;--bx-font-weight: 600;--bx-line-height: 1.2em;--bx-border-width:0px; padding: var(--bx-form-element-spacing-vertical) var(--bx-form-element-spacing-horizontal);border: var(--bx-border-width) solid var(--bx-border-color);border-radius: var(--bx-border-radius);outline: 0;background-color: var(--bx-background-color);color: var(--bx-color);font-weight: var(--bx-font-weight);font-size: 1.2rem;line-height: var(--bx-line-height);text-align: left;text-decoration: none;cursor: pointer;-webkit-user-select: none;-moz-user-select: none;user-select: none;list-style-type: none;}.bx-err summary + div{padding:var(--bx-spacing);}.bx-err details[open]>summary:before {transform: rotate(0);}.bx-err details summary:before {display: block;width: 1.2rem;height: 1.2rem;margin-inline-end: calc(var(--bx-spacing, 1rem)* .5);float: left;transform: rotate(-90deg);background-image: var(--bx-icon-chevron);background-position: right center;background-size: 1.2em auto;background-repeat: no-repeat;content: \"\";transition: transform .2s ease-in-out;}.bx-err summary::marker{display:none;}.bx-err summary::-webkit-details-marker{display:none;}.bx-err details {box-shadow: 1px 1px 2px 0px rgba(0, 0, 0, 0.15);margin: calc( var(--bx-spacing)*1.5) 0px;border:1px solid var(--bx-surface-low)}.bx-err :where(table) {--bx-table-border-color:var(--bx-border-color);width: 100%;border-collapse: collapse;border-spacing: 0;text-indent: 0;}.bx-err pre {background-color: var(--bx-surface-lowest);padding:16px;}.bx-err th {--bx-font-weight: 600;--bx-border-width: .12rem;--bx-th-background-color: #bff6ff;padding: calc(var(--bx-spacing) / 2) var(--bx-spacing);border-bottom: var(--bx-border-width) solid var(--bx-table-border-color);background-color: var(--bx-th-background-color);color: var(--bx-color);font-weight: var(--bx-font-weight);text-align: left;text-align: start;}.bx-err td {padding: calc(var(--bx-spacing) / 2) var(--bx-spacing);border-bottom:var(--bx-border-width) solid var(--bx-table-border-color);}</style>" );
			// header
			errorOutput.append( "<section class=\"bx-err\">" )
			    .append( "<header style=\"background-color:#01413D;\">" )
			    .append( "<h1>" )
			    .append(
			        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg id=\"Layer_2\" data-name=\"Layer 2\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 276.06 40\" style=\"height:1.4rem;\"><defs><style>.bx-svg1 {fill: #fff;stroke-width: 0px;}.bx-svg2{fill:url(#linear-gradient);}.bx-svg3{fill:url(#linear-gradient-2);}.bx-svg4{fill:url(#linear-gradient-3);}</style><linearGradient id=\"linear-gradient\" x1=\"21.39\" y1=\"77.59\" x2=\"78.67\" y2=\"29.52\" gradientUnits=\"userSpaceOnUse\"><stop offset=\"0\" stop-color=\"#00dbff\"/><stop offset=\"1\" stop-color=\"#00ff75\"/></linearGradient><linearGradient id=\"linear-gradient-2\" x1=\"1.45\" y1=\"99.56\" x2=\"10.97\" y2=\"91.58\" xlink:href=\"#linear-gradient\"/><linearGradient id=\"linear-gradient-3\" x1=\"10.92\" y1=\"105.24\" x2=\"102.26\" y2=\"28.6\" xlink:href=\"#linear-gradient\"/></defs><g id=\"Layer_1-2\" data-name=\"Layer 1-2\"><title>BoxLang</title><path class=\"bx-svg1\" d=\"M29.26,22.97c1.38,1.77,2.07,3.8,2.07,6.07,0,3.28-1.15,5.88-3.44,7.8s-5.49,2.88-9.6,2.88H0V.45h17.68c3.99,0,7.11.91,9.37,2.74s3.38,4.31,3.38,7.44c0,2.31-.61,4.23-1.82,5.76s-2.83,2.59-4.84,3.19c2.28.48,4.1,1.62,5.48,3.38h0ZM9.57,16.34h6.27c1.57,0,2.77-.35,3.61-1.03.84-.69,1.26-1.71,1.26-3.05s-.42-2.37-1.26-3.08c-.84-.71-2.04-1.06-3.61-1.06h-6.27v8.22h0ZM20.34,30.91c.88-.73,1.32-1.78,1.32-3.16s-.46-2.46-1.37-3.25-2.17-1.17-3.78-1.17h-6.94v8.67h7.05c1.6,0,2.84-.36,3.72-1.09h0Z\"/><path class=\"bx-svg1\" d=\"M104.89,39.72l-8-12.03-7.05,12.03h-10.85l12.59-19.97L78.71.45h11.13l7.89,11.86L104.67.45h10.85l-12.48,19.81,12.98,19.47h-11.13Z\"/><path class=\"bx-svg1\" d=\"M130.4,32.34h12.53v7.38h-22.1V.45h9.57v31.89h0Z\"/><path class=\"bx-svg1\" d=\"M171.97,32.79h-14.66l-2.35,6.94h-10.02L159.16.45h11.08l14.21,39.27h-10.13l-2.35-6.94h0ZM169.51,25.4l-4.87-14.38-4.81,14.38h9.68Z\"/><path class=\"bx-svg1\" d=\"M223.89,39.72h-9.57l-16-24.22v24.22h-9.57V.45h9.57l16,24.34V.45h9.57v39.27h0Z\"/><path class=\"bx-svg1\" d=\"M256.9,12.87c-.71-1.31-1.73-2.3-3.05-2.99-1.32-.69-2.88-1.03-4.67-1.03-3.1,0-5.58,1.02-7.44,3.05-1.87,2.03-2.8,4.75-2.8,8.14,0,3.62.98,6.44,2.94,8.47s4.65,3.05,8.09,3.05c2.35,0,4.34-.6,5.96-1.79,1.62-1.19,2.81-2.91,3.55-5.15h-12.14v-7.05h20.81v8.89c-.71,2.39-1.91,4.61-3.61,6.66-1.7,2.05-3.85,3.71-6.46,4.98-2.61,1.27-5.56,1.9-8.84,1.9-3.88,0-7.34-.85-10.38-2.54-3.04-1.7-5.41-4.05-7.11-7.08-1.7-3.02-2.54-6.47-2.54-10.35s.85-7.34,2.54-10.38c1.7-3.04,4.05-5.41,7.08-7.11,3.02-1.7,6.47-2.54,10.35-2.54,4.7,0,8.66,1.14,11.89,3.41,3.22,2.28,5.36,5.43,6.41,9.45h-10.57Z\"/><path class=\"bx-svg1 bx-svg2\" d=\"M51.41,6.51l3.53-2.96h0c.12.02.25.04.37.06,7.59,1.51,12.5,8.89,10.99,16.47-1.5,7.51-8.75,12.4-16.25,11.03,1.62-.37,3.17-1.11,4.53-2.25,4.55-3.82,5.15-10.61,1.32-15.16-2.19-2.61-5.35-3.92-8.51-3.84l4-3.35h0Z\"/><path class=\"bx-svg1 bx-svg3\" d=\"M39.31,33.45c-.73-.87-.62-2.17.25-2.9.87-.73,2.17-.62,2.9.25.73.87.62,2.17-.25,2.9-.87.73-2.17.62-2.9-.25Z\"/><path class=\"bx-svg1 bx-svg4\" d=\"M59.19,39.51c-6.37,1.28-12.63-.64-17.14-4.65.26-.11.51-.26.74-.46.47-.4.79-.91.95-1.46.99.57,2.03,1.05,3.13,1.42,9.37,3.22,19.57-1.77,22.78-11.14,3.22-9.37-1.77-19.57-11.14-22.78,8.16,1.34,14.63,7.59,16.24,15.68,2.16,10.76-4.81,21.22-15.56,23.38h0Z\"/><path class=\"bx-svg1\" d=\"M272.47,2.01c.85,0,1.27.49,1.27,1.08,0,.43-.24.86-.8,1.01l.84,1.42h-.82l-.78-1.37h-.33v1.37h-.7v-3.51h1.32ZM272.44,2.59h-.59v1.03h.59c.39,0,.57-.21.57-.53s-.18-.51-.57-.51h0Z\"/><path class=\"bx-svg1\" d=\"M272.45,7.31c-1.99,0-3.61-1.62-3.61-3.61s1.62-3.61,3.61-3.61,3.61,1.62,3.61,3.61-1.62,3.61-3.61,3.61ZM272.45.65c-1.68,0-3.05,1.37-3.05,3.05s1.37,3.05,3.05,3.05,3.05-1.37,3.05-3.05-1.37-3.05-3.05-3.05Z\"/></g></svg>" )
			    .append( "<span> | ERROR</span>" )
			    .append( "</h1>" )
			    .append( "</header>" );

			// error body start
			errorOutput.append( "<div class=\"bx-err-body\">" );

			errorOutput.append( "<h2>" );
			if ( e instanceof BoxLangException ble ) {
				errorOutput.append( escapeHTML( ble.getType() ) )
				    .append( " Error Occurred" );
			} else {
				errorOutput.append( "An Error Occurred" );
			}
			errorOutput.append( "</h2>" );
			// message
			errorOutput.append( "<div class=\"bx-err-msg\">" )
			    // erro icon
			    .append(
			        "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 -960 960 960\" width=\"34\"><path fill=\"red\" d=\"M480-280q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm-40-160h80v-240h-80v240Zm40 360q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z\"/></svg>" )
			    .append( "<div style=\"text-wrap: pretty;\">" )
			    // text
			    .append( escapeHTML( e.getMessage() ) )
			    .append( "</div></div>" );

			// error detail
			if ( e instanceof BoxLangException ble ) {
				if ( ble.getDetail() != null && ble.getDetail().length() == 0 ) {
					errorOutput.append( "<details open>" )
					    .append( "<summary role=\"button\">Detail<sumary>" )
					    .append( "<div><pre>" )
					    .append( ble.getDetail() )
					    .append( "</pre></div></details>" );
				}
			}
			errorOutput.append( "<details open>" )
			    .append( "<summary role=\"button\">Tag Context</summary>" )
			    .append( "<div>" )
			    .append( "<table><thead>" )
			    .append( "<tr><th>File</th><th>Method</th></tr></thead><tbody>" );

			Array tagContext = ExceptionUtil.buildTagContext( e );

			for ( var t : tagContext ) {
				IStruct	item		= ( IStruct ) t;
				Integer	lineNo		= item.getAsInteger( Key.line );
				String	fileName	= item.getAsString( Key.template );
				errorOutput.append( "<tr><td><span>" )
				    .append( fileName )
				    .append( "</span>" );
				if ( lineNo > 0 ) {
					errorOutput.append( ":<strong>" )
					    .append( lineNo.toString() )
					    .append( "</strong><pre>" )
					    .append( getSurroudingLinesOfCode( fileName, lineNo ) )
					    .append( "</pre>" );
				}
				errorOutput.append( "</td>" )
				    .append( "<td>" )
				    .append( escapeHTML( item.getAsString( Key.id ) ) )
				    .append( "</td></tr>" );
			}
			errorOutput.append( "</tbody></table>" )
			    .append( "</div></details>" );

			errorOutput.append( "<details open>" )
			    .append( "<summary role=\"button\">Stack Trace</summary>" )
			    .append( "<div><pre style=\"text-wrap: pretty;\">" );

			errorOutput.append( ExceptionUtil.getStackTraceAsString( e ).replaceAll( "\\((.*)\\)", "<strong class=\"highlight\">($1)</strong>" ) );

			errorOutput.append( "</pre></div>" )
			    .append( "</details>" );

			errorOutput.append( "</div>" )
			    .append( "</section>" );

			if ( context != null ) {
				context.writeToBuffer( errorOutput.toString() );
				context.flushBuffer( true );
			} else {
				// fail safe in case we errored out before creating the context
				ByteBuffer bBuffer = ByteBuffer.wrap( errorOutput.toString().getBytes() );
				exchange.getResponseSender().send( bBuffer );
			}

		} catch ( Throwable t ) {
			e.printStackTrace();
			t.printStackTrace();
		}
	}

	private String getSurroudingLinesOfCode( String fileName, int lineNo ) {
		// read file, if exists, and return the surrounding lines of code, 2 before and 2 after
		File srcFile = new File( fileName );
		if ( srcFile.exists() ) {
			// ...

			try {
				List<String>	lines		= Files.readAllLines( srcFile.toPath() );
				int				startLine	= Math.max( 1, lineNo - 2 );
				int				endLine		= Math.min( lines.size(), lineNo + 2 );

				StringBuilder	codeSnippet	= new StringBuilder();
				for ( int i = startLine; i <= endLine; i++ ) {
					String theLine = escapeHTML( lines.get( i - 1 ) );
					if ( i == lineNo ) {
						codeSnippet.append( "<b>" ).append( i ).append( ": " ).append( theLine ).append( "</b>" ).append( "\n" );
					} else {
						codeSnippet.append( i ).append( ": " ).append( theLine ).append( "\n" );
					}
				}

				return codeSnippet.toString();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
		return "";
	}

	private String escapeHTML( String s ) {
		if ( s == null ) {
			return "";
		}
		return s.replace( "<", "&lt;" ).replace( ">", "&gt;" );
	}

}
