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
package ortus.boxlang.runtime.bifs.global.async;

import ortus.boxlang.runtime.async.RequestThreadManager;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Check if a thread is still alive" )
public class isThreadAlive extends BIF {

	/**
	 * Constructor
	 */
	public isThreadAlive() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._NAME )
		};
	}

	/**
	 * Tests if this thread is alive. A thread is alive if it has been started and has not yet terminated.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * if ( isThreadAlive( "myThread " ) ) {
	 *     // wiat
	 * }
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the thread to check
	 *
	 * @return True, if alive, False otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key						threadName		= Key.of( arguments.getAsString( Key._NAME ) );
		RequestThreadManager	threadManager	= context.getRequestContextOrFail().getThreadManager();

		return threadManager.isThreadAlive( threadName );
	}

}
