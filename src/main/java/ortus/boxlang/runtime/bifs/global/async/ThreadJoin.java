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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Wait for a thread to complete execution" )
public class ThreadJoin extends BIF {

	/**
	 * Constructor
	 */
	public ThreadJoin() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.threadName, "" ),
		    new Argument( false, Argument.NUMERIC, Key.timeout, 0 )
		};
	}

	/**
	 * Waits for the given threads to finish running. The calling code blocks until the threads complete or the timeout elapses.
	 * If no thread name is passed, all the threads created in the request are joined.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.threadName The name of the thread to join to the calling thread. This can also be a comma-separated list of thread names. If
	 *                      empty, all threads are joined.
	 *
	 * @argument.timeout The maximum time in milliseconds to wait. Defaults to 0, which waits indefinitely. When the timeout elapses, the join is
	 *                   abandoned without an error.
	 *
	 * @throws BoxRuntimeException If a thread name is passed and no thread with that name exists.
	 *
	 * @return Nothing (null).
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String					threadNames		= arguments.getAsString( Key.threadName );
		Object					t				= arguments.get( Key.timeout );
		Integer					timeout			= t != null ? IntegerCaster.cast( arguments.get( Key.timeout ) ) : 0;
		Array					aThreadNames	= ListUtil.asList( threadNames, "," )
		    .stream()
		    .map( String::valueOf )
		    .map( String::trim )
		    .collect( BLCollector.toArray() );

		RequestThreadManager	manager			= context
		    .getRequestContextOrFail()
		    .getThreadManager();

		if ( aThreadNames.isEmpty() ) {
			manager.joinAllThreads( timeout );
		} else {
			manager.joinThreads( aThreadNames, timeout );
		}

		return null;
	}

}
