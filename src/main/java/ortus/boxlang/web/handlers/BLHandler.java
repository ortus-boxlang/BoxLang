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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.util.FRTransService;
import ortus.boxlang.web.WebRequestBoxContext;

/**
 * Variables scope implementation in BoxLang
 */
public class BLHandler implements HttpHandler {

	@Override
	public void handleRequest( io.undertow.server.HttpServerExchange exchange ) throws Exception {
		WebRequestBoxContext	context			= null;
		DynamicObject			trans			= null;
		FRTransService			frTransService	= null;

		try {
			frTransService = FRTransService.getInstance();
			String requestPath = exchange.getRequestPath();
			trans	= frTransService.startTransaction( "Web Request", requestPath );
			context	= new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange );
			// Set default content type to text/html
			exchange.getResponseHeaders().put( new HttpString( "Content-Type" ), "text/html" );
			context.loadApplicationDescriptor( new URI( requestPath ) );

			boolean result = context.getApplicationListener().onRequestStart( context, new Object[] { requestPath } );
			if ( result ) {
				context.getApplicationListener().onRequest( context, new Object[] { requestPath } );
			}

		} catch ( AbortException e ) {
			if ( context != null )
				context.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
		} catch ( Throwable e ) {
			e.printStackTrace();

			if ( frTransService != null ) {
				if ( e instanceof Exception ee ) {
					frTransService.errorTransaction( trans, ee );
				} else {
					frTransService.errorTransaction( trans, new Exception( e ) );
				}
			}

			if ( context != null )
				context.flushBuffer( true );
			handleError( e, exchange, context );
		} finally {
			if ( context != null )
				context.flushBuffer( false );
			exchange.endExchange();
			if ( frTransService != null ) {
				frTransService.endTransaction( trans );
			}
		}
	}

	public void handleError( Throwable e, HttpServerExchange exchange, WebRequestBoxContext context ) {
		try {
			StringBuilder errorOutput = new StringBuilder();
			errorOutput.append( "<h1>BoxLang Error</h1>" )
			    .append( "<h2>Message</h2>" )
			    .append( "<pre>" )
			    .append( escapeHTML( e.getMessage() ) )
			    .append( "</pre>" );
			if ( e instanceof BoxLangException ble ) {
				errorOutput.append( "<h2>Detail</h2><pre>" )
				    .append( escapeHTML( ble.getDetail() ) )
				    .append( "</pre><h2>Type</h2>" )
				    .append( escapeHTML( ble.getType() ) );

			}
			errorOutput.append( "<h2>Tag Context</h2>" )
			    .append( "<table border='1' cellPadding='5' cellspacing='0'>" )
			    .append( "<tr><th>File</th><th>Line</th><th>Method</th></tr>" );

			Array tagContext = ExceptionUtil.buildTagContext( e );

			for ( var t : tagContext ) {
				IStruct	item		= ( IStruct ) t;
				Integer	lineNo		= item.getAsInteger( Key.line );
				String	fileName	= item.getAsString( Key.template );
				errorOutput.append( "<tr><td><b>" )
				    .append( fileName );
				if ( lineNo > 0 ) {
					errorOutput.append( "</b><br><pre>" )
					    .append( getSurroudingLinesOfCode( fileName, lineNo ) )
					    .append( "</pre>" );
				}
				errorOutput.append( "</td><td>" )
				    .append( lineNo.toString() )
				    .append( "</td><td>" )
				    .append( escapeHTML( item.getAsString( Key.id ) ) )
				    .append( "</td></tr>" );
			}
			errorOutput.append( "</table>" );

			errorOutput.append( "<h2>Stack Trace</h2>" )
			    .append( "<pre>" );

			StringWriter	sw	= new StringWriter();
			PrintWriter		pw	= new PrintWriter( sw );
			e.printStackTrace( pw );
			errorOutput.append( sw.toString() );

			errorOutput.append( "</pre>" );

			if ( context != null ) {
				context.writeToBuffer( errorOutput.toString() );
				context.flushBuffer( true );
			} else {
				// fail safe
				ByteBuffer bBuffer = ByteBuffer.wrap( errorOutput.toString().getBytes() );
				try {
					exchange.getResponseChannel().write( bBuffer );
				} catch ( IOException e2 ) {
					e2.printStackTrace();
				}
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
