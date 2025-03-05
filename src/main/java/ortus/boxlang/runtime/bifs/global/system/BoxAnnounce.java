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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class BoxAnnounce extends BIF {

	/**
	 * Constructor
	 */
	public BoxAnnounce() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.state ),
		    new Argument( false, Argument.STRUCT, Key.data, new Struct() ),
		    new Argument( false, Argument.STRING, Key.poolname, "global", Set.of( Validator.valueOneOf( "global", "request" ) ) )
		};
	}

	/**
	 * Announce a BoxLang event to a specific interceptor pool. By default, the event is announced to the global interception service.
	 * Available pools are "global" and "request".
	 * The request pool is tied to the application listener and is only available during the request lifecycle.
	 *
	 * Example:
	 *
	 * <pre>
	 * // Announce globally
	 * boxAnnounce( "onRequestStart", { request = request } )
	 *
	 * // Announce to the application request
	 * boxAnnounce( "myRequestEvent", { data : myData }, "request" )
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
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		getTargetPool( arguments.getAsString( Key.poolname ), context )
		    .announce(
		        Key.of( arguments.getAsString( Key.state ) ),
		        arguments.getAsStruct( Key.data ),
		        context
		    );

		return true;
	}

	/**
	 * Get the target interceptor pool based on the pool name.
	 *
	 * @param poolName The name of the pool to get.
	 * @param context  The context in which the BIF is being invoked.
	 *
	 * @return The target interceptor pool.
	 */
	protected InterceptorPool getTargetPool( String poolName, IBoxContext context ) {
		return poolName.equalsIgnoreCase( "global" )
		    ? runtime.getInterceptorService()
		    : context.getParentOfType( RequestBoxContext.class ).getApplicationListener().getInterceptorPool();
	}

}
