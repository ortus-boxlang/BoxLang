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

import java.io.PrintWriter;
import java.io.StringWriter;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.web.WebRequestBoxContext;

/**
 * Variables scope implementation in BoxLang
 */
public class BLHandler implements HttpHandler {

	@Override
	public void handleRequest( io.undertow.server.HttpServerExchange exchange ) throws Exception {
		WebRequestBoxContext context = new WebRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange );
		try {
			String requestPath = exchange.getRequestPath();
			// Set default content type to text/html
			exchange.getResponseHeaders().put( new HttpString( "Content-Type" ), "text/html" );
			context.includeTemplate( requestPath );

		} catch ( Throwable e ) {
			// context.flushBuffer( false );
			handleError( e, exchange, context );
		} finally {
			context.flushBuffer( false );
			exchange.endExchange();
		}
	}

	public void handleError( Throwable e, HttpServerExchange exchange, WebRequestBoxContext context ) {
		StringBuilder errorOutput = new StringBuilder();
		errorOutput.append( "<h1>BoxLang Error</h1>" )
		    .append( "<h2>Message</h2>" )
		    .append( "<pre>" )
		    .append( e.getMessage() )
		    .append( "</pre>" )
		    .append( "<h2>Stack Trace</h2>" )
		    .append( "<pre>" );

		StringWriter	sw	= new StringWriter();
		PrintWriter		pw	= new PrintWriter( sw );
		e.printStackTrace( pw );
		errorOutput.append( sw.toString() );

		errorOutput.append( "</pre>" );

		context.writeToBuffer( errorOutput.toString() );

		e.printStackTrace();
	}

}
