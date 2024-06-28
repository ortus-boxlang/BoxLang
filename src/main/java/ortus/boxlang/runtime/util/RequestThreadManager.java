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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThreadScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I manage the threads for a request. Used by the bx:thread component and thread scopes
 * Every request has a thread manager
 */
public class RequestThreadManager {

	/**
	 * --------------------------------------------------------------------------
	 * Public Constants
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The prefix for thread names
	 */
	public static final String			DEFAULT_THREAD_PREFIX		= "BL-Thread-";

	/**
	 * The default time to wait for a thread to stop when terminating
	 */
	public static final long			DEFAULT_THREAD_WAIT_TIME	= 3000;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The threads we are managing will be stored here
	 */
	protected Map<Key, IStruct>			threads						= new ConcurrentHashMap<>();

	/**
	 * The thread scope
	 */
	protected IScope					threadScope					= new ThreadScope();

	/**
	 * The thread group for the threads created by this manager
	 * TODO: Move to SingleThreadExecutors for better control
	 */
	private static final ThreadGroup	THREAD_GROUP				= new ThreadGroup( "BL-Threads" );

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
		    Key.interrupted, false,
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
		     * TERMINATED: The thread stopped running due to a bxthread tag with a terminate action, an error, or an administrator action.
		     * COMPLETED: The thread ended normally.
		     * WAITING: The thread has executed a bxthread tag with action="join", but one or more threads being joined has not completed.
		     */
		    Key.status, "NOT_STARTED"
		);

		// Add the thread meta by reference to the bxthread scope
		// This struct is what the actual threads "see" when they access "thread" or "threadName" or "bxthread.threadName"
		this.threadScope.put( name, threadMeta );

		return this.threads.put( name, Struct.of(
		    Key.context, context,
		    Key._NAME, name,
		    Key.startTicks, System.currentTimeMillis(),
		    Key.metadata, threadMeta
		) );
	}

	/**
	 * Gets just the thread meta data for a thread. This is a subset of the metadata.
	 * Returns null if not found.
	 *
	 * @param name The name of the thread
	 *
	 * @return The thread meta data
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
	 * Gets the thread data for a thread. Throws exception if not found.
	 *
	 * @param name The name of the thread
	 *
	 * @return The thread data
	 */
	public IStruct getThreadData( Key name ) {
		IStruct threadData = this.threads.get( name );
		if ( threadData == null ) {
			throwInvalidThreadException( name );
		}
		return threadData;
	}

	/**
	 * Marks a thread as complete
	 *
	 * @param name        The name of the thread
	 * @param output      The output of the thread
	 * @param exception   The exception that caused the thread to complete
	 * @param interrupted Whether the thread was interrupted
	 */
	public void completeThread( Key name, String output, Throwable exception, Boolean interrupted ) {
		IStruct threadData = this.threads.get( name );
		if ( threadData == null ) {
			return;
		}
		IStruct				threadMeta		= threadData.getAsStruct( Key.metadata );
		java.lang.Thread	targetThread	= ( ( ThreadBoxContext ) threadData.get( Key.context ) ).getThread();

		threadMeta.put( Key.interrupted, interrupted );
		threadMeta.put( Key.error, exception );
		threadMeta.put( Key.output, output );
		threadMeta.put( Key.status, ( exception == null ? "COMPLETED" : "TERMINATED" ) );
		threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadData.getAsLong( Key.startTicks ) );
		threadMeta.put( Key.stackTrace, targetThread.getStackTrace() );
	}

	/**
	 * This method is used to terminate a thread. It's not foolproof and the JVM
	 * could still be running the thread after this method is called.
	 * <p>
	 * We try to interrupt the thread first, then we wait for x milliseconds for the
	 * thread to stop. If it doesn't stop, we force kill it. Well at least we try to force it.
	 *
	 * @param name The name of the thread
	 */
	@SuppressWarnings( "removal" )
	public void terminateThread( Key name ) {
		IStruct threadData = this.threads.get( name );
		if ( threadData == null ) {
			throwInvalidThreadException( name );
			return;
		}
		ThreadBoxContext	context			= ( ThreadBoxContext ) threadData.getAsStruct( Key.context );
		java.lang.Thread	targetThread	= context.getThread();

		// Try to interrupt the thread first
		targetThread.interrupt();
		// Wait for x milliseconds for the thread to stop
		try {
			targetThread.join( DEFAULT_THREAD_WAIT_TIME );
			// Check if still alive, if so, force kill it
			if ( targetThread.isAlive() ) {
				targetThread.stop();
			}
		} catch ( InterruptedException e ) {
			// Set it again as good practice
			targetThread.interrupt();
			// Force kill the thread
			targetThread.stop();
		} finally {
			// Complete it
			completeThread( name, "", new InterruptedException( "Thread requested to terminate" ), true );
		}
	}

	/**
	 * Detect if at least one thread
	 *
	 * @return true if there are threads
	 */
	public boolean hasThreads() {
		return !this.threads.isEmpty();
	}

	/**
	 * Gets the thread scope
	 *
	 * @return The thread scope
	 */
	public IScope getThreadScope() {
		return this.threadScope;
	}

	/**
	 * Get the thread group for the threads created by this manager
	 *
	 * @return The thread group
	 */
	public ThreadGroup getThreadGroup() {
		return THREAD_GROUP;
	}

	/**
	 * Verify if the current thread is in a thread.
	 *
	 * @return true if the current thread is in a thread
	 */
	public boolean isInThread() {
		return Thread.currentThread().getThreadGroup() == THREAD_GROUP;
	}

	/**
	 * Gets the names of the threads
	 *
	 * @return The names of the threads
	 */
	public Key[] getThreadNames() {
		return this.threads.keySet().toArray( new Key[ 0 ] );
	}

	/**
	 * Throws an exception for an invalid thread
	 *
	 * @param name The name of the thread
	 */
	private void throwInvalidThreadException( Key name ) {
		throw new BoxRuntimeException( "No thread with name [" + name.getName() + "] not found. Valid names are ["
		    + Arrays.stream( getThreadNames() )
		        .map( Key::getName )
		        .collect( Collectors.joining( ", " ) )
		    + "]." );
	}

}
