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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

@BoxBIF( description = "Announce an event asynchronously" )
public class BoxAnnounceAsync extends BoxAnnounce {

	/**
	 * Constructor
	 */
	public BoxAnnounceAsync() {
		super();
	}

	/**
	 * Announce a BoxLang event to the global system interceptor service asynchronously. By default, the event is announced to the global interception service.
	 * The return value is a BoxLang CompletableFuture that will be completed when the event has been announced.
	 * Available pools are "global" and "request".
	 * The request pool is tied to the application listener and is only available during the request lifecycle.
	 *
	 * Example:
	 *
	 * <pre>
	 * // Announce globally
	 * var future = announceAsync( "onRequestStart", { request = request } )
	 *
	 * // Announce to the application request
	 * var future = announceAsync( "myRequestEvent", { data : myData }, "request" )
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.state The interceptor event to announce: Ex: "onRequestStart", "onRequestEnd", "onError"
	 *
	 * @argument.data The data struct to send with the event
	 *
	 * @argument.poolname The name of the interceptor pool to announce the event to. Default is "global". Available pools are "global" and "request".
	 *
	 * @return A CompletableFuture that will be completed when the event has been announced, or null if the state doesn't exist
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return getTargetPool( arguments.getAsString( Key.poolname ), context )
		    .announceAsync(
		        Key.of( arguments.getAsString( Key.state ) ),
		        arguments.getAsStruct( Key.data ),
		        context
		    );
	}

}
