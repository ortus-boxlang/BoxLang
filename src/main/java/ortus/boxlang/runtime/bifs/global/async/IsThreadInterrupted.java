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
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Check if a thread has been interrupted" )
public class IsThreadInterrupted extends BIF {

	/**
	 * Constructor
	 */
	public IsThreadInterrupted() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key._NAME )
		};
	}

	/**
	 * Verifies if the current thread is interrupted or not. You can also pass in a thread name to check if that thread is interrupted.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * if ( !IsThreadInterrupted() ) {
	 *     // Do work
	 * }
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the thread to check or empty for the current thread.
	 *
	 * @return True, if Interrupted, False otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key						threadName		= Key.of( arguments.getAsString( Key._NAME ) );
		RequestThreadManager	threadManager	= context.getParentOfType( RequestBoxContext.class ).getThreadManager();

		return threadName.isEmpty() ? threadManager.IsThreadInterrupted( context ) : threadManager.IsThreadInterrupted( threadName );
	}

}
