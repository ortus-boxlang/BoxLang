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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;

/**
 * I represent a Server
 */
public class Server {

	private static BoxRuntime runtime = BoxRuntime.getInstance();

	public static void main( String[] args ) {
		System.out.println( "Starting BoxLang Server..." );

		// Setup web root. Should this go in the runtime, or each context?
		runtime.getConfiguration().runtime.mappings
		    .put( Key.of( "/" ),
		        Paths.get( "src/main/java/ortus/boxlang/web/www/" ).toAbsolutePath().toString() );

		Undertow.Builder	builder		= Undertow.builder();
		Undertow			BLServer	= builder
		    .addHttpListener( 8080, "localhost" )
		    .setHandler( new HttpHandler() {

											    @Override
											    public void handleRequest( io.undertow.server.HttpServerExchange exchange ) throws Exception {
												    try {
													    WebBoxContext context	= new WebBoxContext( BoxRuntime.getInstance().getRuntimeContext(), exchange );

													    String		requestPath	= exchange.getRequestPath();
													    if ( requestPath.equals( "/" ) ) {
														    requestPath = "/index.cfm";
													    }

													    context.includeTemplate( requestPath );
													    context.flushBuffer( false );

												    } catch ( Throwable e ) {
													    StringBuilder errorOutput = new StringBuilder();
													    errorOutput.append( "<h1>BoxLang Error</h1>" )
													        .append( "<h2>Message</h2>" )
													        .append( "<pre>" )
													        .append( e.getMessage() )
													        .append( "</pre>" )
													        .append( "<h2>Stack Trace</h2>" )
													        .append( "<pre>" );
													    Throwable err	= e;
													    boolean	first	= true;
													    while ( err != null ) {
														    errorOutput.append( "\n" );
														    if ( !first ) {
															    errorOutput.append( "CAUSED BY: " );
														    }
														    errorOutput.append( err.getClass().getName() + " " + err.getMessage() + ": \n" )
														        .append( "\t" + Arrays.stream( e.getStackTrace() )
														            .map( Object::toString )
														            .collect( Collectors.joining( System.lineSeparator() + "\t" ) ) );

														    err	= err.getCause();
														    first = false;
													    }
													    errorOutput.append( "</pre>" );

													    exchange.getResponseSender().send( errorOutput.toString() );

													    e.printStackTrace();
													    throw e;
												    }
											    }
										    } )
		    .build();

		BLServer.start();
	}
}