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

import java.nio.file.Path;
import java.nio.file.Paths;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicates;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.handlers.BLHandler;

/**
 * I represent a Server
 */
public class Server {

	private static BoxRuntime runtime = BoxRuntime.getInstance();

	public static void main( String[] args ) {
		System.out.println( "Starting BoxLang Server..." );
		Path webRoot = Paths.get( "src/main/java/ortus/boxlang/web/www/" ).toAbsolutePath();

		// Setup web root. Should this go in the runtime, or each context?
		runtime.getConfiguration().runtime.mappings
		    .put( Key.of( "/" ),
		        webRoot.toString() );

		Undertow.Builder	builder		= Undertow.builder();
		Undertow			BLServer	= builder
		    .addHttpListener( 8080, "localhost" )
		    .setHandler( Handlers.predicate(
		        // If this predicate evaluates to true, we process via BoxLang, otherwise, we serve a static file
		        Predicates.parse( "regex( '^/(.+?\\.cf[cms])(/.*)?$' )" ),
		        new BLHandler(),
		        new ResourceHandler( new PathResourceManager( webRoot ) )
		            .setDirectoryListingEnabled( true )
		            .addWelcomeFiles( "index.cfm", "index.cfs" ) ) )
		    .build();

		BLServer.start();
	}
}