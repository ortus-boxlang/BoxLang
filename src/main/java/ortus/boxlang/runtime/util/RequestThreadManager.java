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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * I manage the threads for a request. Used by the bx:thread component and thread scopes
 * Every request has a thread manager
 */
public class RequestThreadManager {

	Map<Key, IStruct> threads = new ConcurrentHashMap<Key, IStruct>();

	/**
	 * Registers a thread with the manager
	 * 
	 * @param name    The name of the thread
	 * @param context The context of the thread
	 * 
	 * @return The thread data
	 */
	public synchronized IStruct regsiterThread( Key name, ThreadBoxContext context ) {
		// reject dupe thread names for this request
		if ( threads.containsKey( name ) ) {
			throw new RuntimeException( "Thread name [" + name + "] already in use for this request." );
		}
		return threads.put( name, Struct.of(
		    Key.context, context,
		    Key._NAME, name,
		    Key.startTicks, System.currentTimeMillis(),
		    Key.metadata, Struct.of(
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
		    )
		) );
	}

	/**
	 * Gets the thread data for a thread. REturns null if none found by the provided name
	 * 
	 * @param name The name of the thread
	 * 
	 * @return The thread data
	 */
	public IStruct getThread( Key name ) {
		IStruct threadData = threads.get( name );
		if ( threadData == null ) {
			return threadData;
		}
		IStruct	threadMeta		= threadData.getAsStruct( Key.metadata );
		String	threadStatus	= threadMeta.getAsString( Key.status );

		// If the thread was not complete last time we looked at it, let's update the status
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
			threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadMeta.getAsLong( Key.startTicks ) );

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
		threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadMeta.getAsLong( Key.startTicks ) );

	}

}
