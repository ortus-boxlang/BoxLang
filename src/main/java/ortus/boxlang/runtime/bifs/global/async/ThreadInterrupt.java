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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Interrupt a running thread" )
public class ThreadInterrupt extends BIF {

	/**
	 * Constructor
	 */
	public ThreadInterrupt() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key._NAME, "" )
		};
	}

	/**
	 * Interrupt the specific thread by name or all threads managed by the thread manager.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * ThreadInterrupt( "myThread" );
	 * </pre>
	 * <p>
	 * This will interrupt the thread with the name "myThread".
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *
	 * ThreadInterrupt();
	 * </pre>
	 * <p>
	 * This will interrupt all threads managed by the thread manager.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the thread to interrupt. If not passed, we interrupt ALL threads.
	 *
	 * @throws BoxRuntimeException If the thread is not found.
	 */
	@Override
	public Thread _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key						threadName		= Key.of( arguments.getAsString( Key._NAME ) );
		RequestThreadManager	threadManager	= context.getParentOfType( RequestBoxContext.class ).getThreadManager();

		if ( threadName.isEmpty() ) {
			threadManager.interruptAllThreads();
		} else {
			threadManager.interruptThread( threadName );
		}

		return null;
	}

}
