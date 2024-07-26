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
package ortus.boxlang.runtime.components.async;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.RequestThreadManager;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class Thread extends Component {

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger( Thread.class );

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
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
	 *                    is terminated. If the timeout attribute is not specified, the thread runs until it finishes.
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
			throw new BoxRuntimeException( "Invalid thread action [" + action + "]. Valid actions are [join, run, sleep, terminate]" );
		}

		return DEFAULT_RETURN;
	}

	/**
	 * Run a thread
	 *
	 * @param context    The context in which the Component is being invoked
	 * @param name       The name of the thread
	 * @param priority   The priority of the thread
	 * @param attributes The attributes to the Component
	 * @param body       The body of the Component
	 */
	private void run( IBoxContext context, String name, String priority, IStruct attributes, ComponentBody body ) {
		RequestThreadManager	threadManager	= context.getParentOfType( RequestBoxContext.class ).getThreadManager();
		final Key				nameKey			= RequestThreadManager.ensureThreadName( name );
		ThreadBoxContext		tContext		= threadManager.createThreadContext( context, nameKey );

		// Startup the thread
		threadManager.startThread(
		    // The thread context to run in
		    tContext,
		    // The name of the thread as a key
		    nameKey,
		    // The thread priority
		    priority,
		    // The Runnable Proxy
		    () -> {
			    StringBuffer buffer		= new StringBuffer();
			    Throwable	exception	= null;
			    Logger		logger		= LoggerFactory.getLogger( Thread.class );
			    try {
				    processBody( tContext, body, buffer );
			    } catch ( AbortException e ) {
				    // We log it so we can potentially find out why it was aborted
				    logger.debug( "Thread [{}] aborted at stacktrace: {}", nameKey.getName(), e.getStackTrace() );
			    } catch ( Throwable e ) {
				    exception = e;
				    logger.error( "Thread [{}] terminated with exception: {}", nameKey.getName(), e.getMessage() );
				    logger.error( "-> Exception", e );
			    } finally {
				    threadManager.completeThread(
				        nameKey,
				        buffer.toString(),
				        exception,
				        java.lang.Thread.interrupted()
				    );
			    }
		    },
		    // The Struct of data to bind into the thread's scope
		    attributes
		);

	}

	/**
	 * Join a thread
	 *
	 * @param context The context in which the Component is being invoked
	 * @param name    The name of the thread
	 * @param timeout The timeout for the join
	 */
	private void join( IBoxContext context, String name, Integer timeout ) {
		timeout = timeout == null ? 0 : timeout;
		RequestThreadManager	threadManager	= context.getParentOfType( RequestBoxContext.class ).getThreadManager();
		Array					aThreadNames	= ListUtil.asList( name, "," )
		    .stream()
		    .map( String::valueOf )
		    .map( String::trim )
		    .collect( BLCollector.toArray() );

		if ( aThreadNames.isEmpty() ) {
			threadManager.joinAllThreads( timeout );
		} else {
			threadManager.joinThreads( aThreadNames, timeout );
		}
	}

	/**
	 * Terminate a thread
	 *
	 * @param context The context in which the Component is being invoked
	 * @param name    The name of the thread
	 */
	private void terminate( IBoxContext context, String name ) {
		context.getParentOfType( RequestBoxContext.class )
		    .getThreadManager()
		    .terminateThread( Key.of( name ) );
	}

	/**
	 * Sleep for a duration
	 *
	 * @param context  The context in which the Component is being invoked
	 * @param duration The duration to sleep
	 */
	private void sleep( IBoxContext context, Integer duration ) {
		context.invokeFunction( Key.sleep, new Object[] { duration } );
	}

}
