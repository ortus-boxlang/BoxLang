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
package ortus.boxlang.runtime.components.threading;

import java.util.List;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.RequestThreadManager;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class Thread extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The name of the component
	 */
	public Thread() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string" ),
		    new Attribute( Key.action, "string", "run", Set.of(
		        Validator.valueOneOf( "join", "run", "sleep", "terminate" ),
		        Validator.valueRequires( "sleep", Key.duration )
		    ) ),
		    new Attribute( Key.duration, "integer", 0, Set.of(
		        Validator.min( 0 )
		    ) ),
		    new Attribute( Key.priority, "string", "normal", Set.of(
		        Validator.valueOneOf( "high", "low", "normal" )
		    ) ),
		    new Attribute( Key.timeout, "integer" )
		};
	}

	/**
	 * The thread component enables multithreaded programming in BoxLang. Threads are independent streams of execution, and multiple threads on a page can
	 * execute simultaneously and asynchronously, letting you perform asynchronous processing. Code within the thread component body executes on a
	 * separate thread while the request thread continues processing without waiting for the thread body to finish. You use this tag to run or end a
	 * thread, temporarily stop thread execution, or join together multiple threads.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.name The name of the thread.
	 *
	 * @attribute.action The action to perform. The default value is "run". The following are the possible values: "join", "run", "sleep", "terminate".
	 *
	 * @attribute.duration The number of milliseconds to pause the thread. This attribute is required if the action attribute is set to "sleep".
	 *
	 * @attribute.priority The priority of the thread. The default value is "normal". The following are the possible values: "high", "low", "normal".
	 *
	 * @attribute.timeout The number of milliseconds to wait for the thread to finish. If the thread does not finish within the specified time, the thread
	 *                    is
	 *                    terminated. If the timeout attribute is not specified, the thread runs until it finishes.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		Key		action		= Key.of( attributes.getAsString( Key.action ) );
		String	name		= attributes.getAsString( Key._NAME );
		Integer	duration	= attributes.getAsInteger( Key.duration );
		String	priority	= attributes.getAsString( Key.priority );
		Integer	timeout		= attributes.getAsInteger( Key.timeout );

		if ( action.equals( Key.join ) ) {
			join( context, name, timeout );
		} else if ( action.equals( Key.run ) ) {
			run( context, name, priority, attributes, body );
		} else if ( action.equals( Key.sleep ) ) {
			sleep( context, duration );
		} else if ( action.equals( Key.terminate ) ) {
			terminate( context, name );
		} else {
			throw new BoxRuntimeException( "Invalid action: " + action );
		}

		return DEFAULT_RETURN;
	}

	private void run( IBoxContext context, String name, String priority, IStruct attributes, ComponentBody body ) {
		RequestThreadManager threadManager = context.getParentOfType( RequestBoxContext.class ).getThreadManager();
		if ( name == null || name.isEmpty() ) {
			// generate random name
			name = "BoxLang-Thread-" + java.util.UUID.randomUUID().toString();
		}
		final Key			nameKey		= Key.of( name );
		ThreadBoxContext	tContext	= new ThreadBoxContext( context, threadManager, nameKey );
		java.lang.Thread	thread		= new java.lang.Thread( () -> {
											StringBuffer	buffer		= new StringBuffer();
											Throwable		exception	= null;
											try {
												processBody( tContext, body, buffer );
											} catch ( AbortException e ) {
												// Nothing to do here
											} catch ( Throwable e ) {
												exception = e;
											} finally {
												threadManager.completeThread( nameKey, buffer.toString(), exception );
											}
										} );

		// This may or may not be unique for the entire JVM
		thread.setName( "BL-Thread-" + name );
		tContext.setThread( thread );

		LocalScope local = ( LocalScope ) tContext.getScopeNearby( LocalScope.name );
		local.put( Key.attributes, attributes );

		threadManager.registerThread( nameKey, tContext );

		thread.start();
	}

	private void join( IBoxContext context, String name, Integer timeout ) {
		if ( name == null || name.isEmpty() ) {
			throw new BoxValidationException( "Thread name is required for join" );
		}
		timeout = timeout == null ? 0 : timeout;
		int						timeoutMSLeft	= timeout;
		long					start			= System.currentTimeMillis();
		RequestThreadManager	threadManager	= context.getParentOfType( RequestBoxContext.class ).getThreadManager();
		List<String>			threadNames		= ListUtil.asList( name, "," ).stream()
		    .map( item -> String.valueOf( item ) )
		    .map( String::trim )
		    .toList();

		for ( String threadName : threadNames ) {
			try {
				( ( ThreadBoxContext ) threadManager.getThreadData( Key.of( threadName ) ).get( Key.context ) ).getThread().join( timeoutMSLeft );
			} catch ( InterruptedException e ) {
				throw new BoxRuntimeException( "Thread join interrupted", e );
			}
			// If we have a timeout, we need to check if we're out of time
			// a timeout of zero means we do this forever
			if ( timeout > 0 ) {
				// Decrement how much time is left from the original timeout.
				timeoutMSLeft = timeout - ( int ) ( System.currentTimeMillis() - start );
				// If we're out of time, bail. Doesn't matter how many thread are left, we ran out of time
				if ( timeoutMSLeft <= 0 ) {
					return;
				}
			}
		}
	}

	private void terminate( IBoxContext context, String name ) {
		RequestThreadManager threadManager = context.getParentOfType( RequestBoxContext.class ).getThreadManager();
		// Thread.stop() is deprecated in the JVM. We can use interrupt(), but it may not do anything if the thread is not in a blocking state.
		( ( ThreadBoxContext ) threadManager.getThreadData( Key.of( name ) ).get( Key.context ) ).getThread().stop();
	}

	private void sleep( IBoxContext context, Integer duration ) {
		context.invokeFunction( Key.sleep, new Object[] { duration } );
	}

}
