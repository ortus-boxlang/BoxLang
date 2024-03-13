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
package ortus.boxlang.runtime.util;

import java.lang.Thread.State;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThreadScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * I manage the threads for a request. Used by the bx:thread component and thread scopes
 * Every request has a thread manager
 */
public class RequestThreadManager {

	Map<Key, IStruct>	threads		= new ConcurrentHashMap<Key, IStruct>();

	/**
	 * The thread scope
	 */
	protected IScope	threadScope	= new ThreadScope();

	/**
	 * Registers a thread with the manager
	 *
	 * @param name    The name of the thread
	 * @param context The context of the thread
	 *
	 * @return The thread data
	 */
	public synchronized IStruct registerThread( Key name, ThreadBoxContext context ) {
		// reject dupe thread names for this request
		if ( threads.containsKey( name ) ) {
			throw new RuntimeException( "Thread name [" + name + "] already in use for this request." );
		}
		IStruct threadMeta = Struct.of(
		    // Lucee has childThreads
		    Key._NAME, name,
		    Key.elapsedTime, 0,
		    Key.error, null,
		    Key.output, "",
		    Key.stackTrace, "",
		    Key.priority, switch ( context.getThread().getPriority() ) {
			    case Thread.MIN_PRIORITY -> "LOW";
			    case Thread.NORM_PRIORITY -> "NORMAL";
			    case Thread.MAX_PRIORITY -> "HIGH";
			    default -> "UNKNOWN";
		    },
		    Key.startTime, new DateTime(),
		    /*
		     * NOT_STARTED: The thread has been queued but is not processing yet.
		     * RUNNNG: The thread is running normally.
		     * TERMINATED: The thread stopped running due to a cfthread tag with a terminate action, an error, or an administrator action.
		     * COMPLETED: The thread ended normally.
		     * WAITING: The thread has executed a cfthread tag with action="join", but one or more threads being joined has not completed.
		     */
		    Key.status, "NOT_STARTED"
		);

		// Add the thread meta by reference to the bxthread scope
		// This struct is what the actual threads "see" when they access "thread" or "threadName" or "bxthread.threadName"
		threadScope.put( name, threadMeta );

		return threads.put( name, Struct.of(
		    Key.context, context,
		    Key._NAME, name,
		    Key.startTicks, System.currentTimeMillis(),
		    Key.metadata, threadMeta
		) );
	}

	/**
	 * Gets the thread data for a thread. Returns null if none found by the provided name
	 * Do not cache the return of this method, or the thread state, execution time, and error data
	 * may be out of sync.
	 *
	 * @param name The name of the thread
	 *
	 * @return The thread data
	 */
	public IStruct getThreadMeta( Key name ) {
		IStruct threadData = threads.get( name );
		if ( threadData == null ) {
			return threadData;
		}
		IStruct	threadMeta		= threadData.getAsStruct( Key.metadata );
		String	threadStatus	= threadMeta.getAsString( Key.status );

		// If the thread was not complete last time we looked at it, let's update the status
		// TODO: Move this to a change listener on the struct so we only calculate these keys if we actually use them.
		if ( threadStatus.equals( "NOT_STARTED" ) || threadStatus.equals( "RUNNNG" ) || threadStatus.equals( "WAITING" ) ) {
			Thread	thread				= ( ( ThreadBoxContext ) threadData.get( Key.context ) ).getThread();
			// Update thread state
			State	currentThreadState	= thread.getState();
			IStruct	exception			= threadMeta.getAsStruct( Key.error );
			threadStatus = switch ( currentThreadState ) {
				case NEW -> "NOT_STARTED";
				case RUNNABLE -> "RUNNNG";
				case TERMINATED -> ( exception == null ? "COMPLETED" : "TERMINATED" );
				case BLOCKED -> "WAITING";
				case WAITING -> "WAITING";
				case TIMED_WAITING -> "WAITING";
				default -> "UNKNOWN";
			};
			threadMeta.put( Key.status, threadStatus );

			// Update elapsed time
			threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadData.getAsLong( Key.startTicks ) );

			// Grab stack trace, only if thread is running
			if ( threadStatus.equals( "RUNNNG" ) || threadStatus.equals( "WAITING" ) ) {
				StringBuilder		stackTraceBuilder	= new StringBuilder();
				StackTraceElement[]	stackTrace			= thread.getStackTrace();
				for ( StackTraceElement element : stackTrace ) {
					stackTraceBuilder.append( element.toString() ).append( "\n" );
				}
				threadMeta.put( Key.stackTrace, stackTraceBuilder.toString() );
			} else {
				threadMeta.put( Key.stackTrace, "" );
			}
		}
		return threadMeta;
	}

	/**
	 * Marks a thread as complete
	 *
	 * @param name      The name of the thread
	 * @param exception The exception that caused the thread to complete
	 *
	 * @return The thread data
	 */
	public void completeThread( Key name, String output, Throwable exception ) {
		IStruct threadData = threads.get( name );
		if ( threadData == null ) {
			return;
		}
		IStruct threadMeta = threadData.getAsStruct( Key.metadata );

		threadMeta.put( Key.error, exception );
		threadMeta.put( Key.output, output );
		threadMeta.put( Key.status, ( exception == null ? "COMPLETED" : "TERMINATED" ) );
		threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadData.getAsLong( Key.startTicks ) );
		threadMeta.put( Key.stackTrace, "" );

	}

	/**
	 * Detect if at least one thread
	 *
	 * @return true if there are threads
	 */
	public boolean hasThreads() {
		return !threads.isEmpty();
	}

	/**
	 * Gets the thread scope
	 *
	 * @return The thread scope
	 */
	public IScope getThreadScope() {
		return threadScope;
	}

	/**
	 * Gets the names of the threads
	 *
	 * @return The names of the threads
	 */
	public Key[] getThreadNames() {
		return threads.keySet().toArray( new Key[ 0 ] );
	}

}
