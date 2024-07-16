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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.RequestThreadManager;

@BoxBIF
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
	 * Waits for the given thread object to finish running
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.threadName The name of the thread to join to the main thread.
	 *                      This can be the name of the thread or a comma-separated list of thread names.
	 *
	 * @argument.timeout The maximum time in milliseconds to wait for the thread to finish running. If the thread does not finish running within this time, the join operation will be aborted.
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
		    .getParentOfType( RequestBoxContext.class )
		    .getThreadManager();

		if ( aThreadNames.isEmpty() ) {
			manager.joinAllThreads( timeout );
		} else {
			manager.joinThreads( aThreadNames, timeout );
		}

		return null;
	}

}
