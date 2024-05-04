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

import io.undertow.server.HttpHandler;
import ortus.boxlang.web.WebRequestExecutor;

/**
 * Undertow HttpHandler for BoxLang
 * This mini-server only has one web root for all requests
 */
public class BLHandler implements HttpHandler {

	private String webRoot;

	public BLHandler( String webRoot ) {
		this.webRoot = webRoot;
	}

	@Override
	public void handleRequest( io.undertow.server.HttpServerExchange exchange ) throws Exception {
		if ( exchange.isInIoThread() ) {
			exchange.dispatch( this );
			return;
		}
		exchange.startBlocking();

		// In our custom pure Undertow server, we need to track our own FR transactions
		WebRequestExecutor.execute( exchange, this.webRoot, true );

	}

}
