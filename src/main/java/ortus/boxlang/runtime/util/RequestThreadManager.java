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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ThreadComponentBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThreadScope;
import ortus.boxlang.runtime.types.Array;
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
	 * The threads we are managing will be stored here alongside a
	 * structure of data:
	 * - context : ThreadBoxContext
	 * - startTicks : When the thread started
	 * - name : The thread name
	 * - metadata : A struct of metadata about the thread
	 */
	protected Map<Key, IStruct>			threads						= new ConcurrentHashMap<>();

	/**
	 * The thread scope for the request
	 */
	protected IScope					threadScope					= new ThreadScope();

	/**
	 * A list of completed threads in the order they completed. This is used for fast lookups to unregister completed threads if we get too many.
	 */
	protected Deque<Key>				completedThreads			= new ArrayDeque<>();

	/**
	 * The associated context
	 */
	protected RequestBoxContext			context;

	/**
	 * The thread group for the threads created by this manager
	 * TODO: Move to SingleThreadExecutors for better control
	 */
	private static final ThreadGroup	THREAD_GROUP				= new ThreadGroup( "BL-Threads" );

	/**
	 * The states that a thread can be in where we update valid statuses
	 */
	private static final Set<String>	ACTION_STATES				= Set.of( "NOT_STARTED", "RUNNNG", "WAITING" );

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param context
	 * 
	 * @return
	 */
	public RequestThreadManager( RequestBoxContext context ) {
		this.context = context;
	}

	/**
	 * Registers a thread with the manager
	 *
	 * @param name    The name of the thread
	 * @param context The context of the thread
	 *
	 * @return The thread metadata
	 */
	public synchronized IStruct registerThread( Key name, ThreadComponentBoxContext context ) {
		// reject dupe thread names for this request
		if ( threads.containsKey( name ) ) {
			throw new RuntimeException( "Thread name [" + name + "] already in use for this request." );
		}
		// The actual thread
		Thread	targetThread	= context.getThread();
		// Create a thread meta struct
		IStruct	threadMeta		= Struct.of(
		    Key.targetThread, targetThread,
		    Key.id, targetThread.threadId(),
		    Key._NAME, name,
		    Key.elapsedTime, 0,
		    Key.error, null,
		    Key.virtual, targetThread.isVirtual(),
		    Key.daemon, targetThread.isDaemon(),
		    Key.threadGroup, targetThread.getThreadGroup().getName(),
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
		    /**
		     * NOT_STARTED: The thread has been queued but is not processing yet.
		     * RUNNNG: The thread is running normally.
		     * BLOCKED: The thread is blocked waiting for a monitor lock.
		     * INTERRUPTED: The thread was interrupted.
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
	 * Unregister a thread from the manager
	 * 
	 * @param name The name of the thread
	 */
	public void unregisterThread( Key name ) {
		this.threads.remove( name );
		this.threadScope.remove( name );
		this.completedThreads.remove( name );
	}

	/**
	 * Gets just the thread meta data for a thread. This is a subset of the metadata.
	 * Returns null if not found.
	 *
	 * @param name The name of the thread
	 *
	 * @return The thread meta data or null if not found
	 */
	public IStruct getThreadMeta( Key name ) {
		IStruct threadData = getThreadDataSafe( name );
		if ( threadData == null ) {
			return null;
		}

		// Only valid threads here
		IStruct	threadMeta		= threadData.getAsStruct( Key.metadata );
		String	threadStatus	= threadMeta.getAsString( Key.status );

		// If the thread was not complete last time we looked at it, let's update the status
		if ( ACTION_STATES.contains( threadStatus ) ) {
			Thread	thread		= ( ( ThreadComponentBoxContext ) threadData.get( Key.context ) ).getThread();
			IStruct	exception	= threadMeta.getAsStruct( Key.error );
			// Update status
			threadStatus = switch ( thread.getState() ) {
				case NEW -> "NOT_STARTED";
				case RUNNABLE -> "RUNNNG";
				case TERMINATED -> ( exception == null ? "COMPLETED" : "TERMINATED" );
				case BLOCKED -> "BLOCKED";
				case WAITING, TIMED_WAITING -> "WAITING";
				default -> "UNKNOWN";
			};
			threadMeta.put( Key.status, threadStatus );
			// Update elapsed time
			threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadData.getAsLong( Key.startTicks ) );
			// Grab stack trace, only if thread is running OR waiting OR blocked
			switch ( threadStatus ) {
				case "RUNNNG", "WAITING", "BLOCKED" -> {
					threadMeta.put( Key.stackTrace, getStackTraceAsString( thread ) );
				}
				default -> threadMeta.put( Key.stackTrace, "" );
			}
		}

		return threadMeta;
	}

	/**
	 * Gets the stack trace for a thread as a string.
	 * 
	 * @param thread The thread to get the stack trace for
	 * 
	 * @return The stack trace as a string
	 */
	private String getStackTraceAsString( Thread thread ) {
		StringBuilder		stackTraceBuilder	= new StringBuilder();
		StackTraceElement[]	stackTrace			= thread.getStackTrace();
		for ( StackTraceElement element : stackTrace ) {
			stackTraceBuilder.append( element.toString() ).append( "\n" );
		}
		return stackTraceBuilder.toString();
	}

	/**
	 * Gets the thread data for a thread. Throws exception if not found, unless we've
	 * reached out max completed threads, in which case it returns null.
	 *
	 * @param name The name of the thread
	 *
	 * @return The thread data
	 */
	public IStruct getThreadData( Key name ) {
		IStruct threadData = this.threads.get( name );
		if ( threadData == null ) {
			if ( this.completedThreads.size() >= ( getMaxTrackedCompletedThreads() ) ) {
				return null;
			} else {
				throwInvalidThreadException( name );
			}
		}
		return threadData;
	}

	/**
	 * Convenience method to get the current maximum tracked completed threads settings from the config
	 *
	 * @return The maximum tracked completed threads
	 */
	public int getMaxTrackedCompletedThreads() {
		return this.context.getConfig().getAsInteger( Key.maxTrackedCompletedThreads );
	}

	/**
	 * Gets the thread data for a thread. Returns null if not found.
	 *
	 * @param name The name of the thread
	 *
	 * @return The thread data
	 */
	public IStruct getThreadDataSafe( Key name ) {
		return this.threads.get( name );
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
		IStruct threadData = getThreadData( name );
		if ( threadData == null ) {
			return;
		}
		IStruct				threadMeta		= threadData.getAsStruct( Key.metadata );
		java.lang.Thread	targetThread	= ( ( ThreadComponentBoxContext ) threadData.get( Key.context ) ).getThread();

		threadMeta.put( Key.interrupted, interrupted );
		threadMeta.put( Key.error, exception );
		threadMeta.put( Key.output, output );
		threadMeta.put( Key.status, ( exception == null ? "COMPLETED" : "TERMINATED" ) );
		threadMeta.put( Key.elapsedTime, System.currentTimeMillis() - threadData.getAsLong( Key.startTicks ) );
		if ( interrupted && targetThread.isAlive() ) {
			threadMeta.put( Key.stackTrace, getStackTraceAsString( targetThread ) );
		} else {
			threadMeta.put( Key.stackTrace, "" );
		}

		// Track this completed thread
		completedThreads.add( name );

		flushCompletedThread();
	}

	/**
	 * Flushes completed threads from the manager.
	 */
	protected void flushCompletedThread() {
		// This is to prevent a memory leak if you have a long-running daemon which fires many uniquely-named threads over a period of time.
		// We don't want to just keep filling up memory, so clear our old completed threads.
		// It seems unlikely that a request would legitimately have 1000+ threads that it wants to reference back in the thread scope later
		while ( completedThreads.size() > getMaxTrackedCompletedThreads() ) {
			// Use poll just in the crazy chance the queue is empty now
			Key oldestThread = completedThreads.poll();
			if ( oldestThread != null ) {
				unregisterThread( oldestThread );
			}
		}
	}

	/**
	 * Generates a thread name according to the given name. If the name is empty or null, a random name is generated.
	 *
	 * @param name The name of the thread
	 *
	 * @return The generated thread name
	 */
	public static Key ensureThreadName( String name ) {
		// generate random name if not set or empty: anonymous thread
		if ( name == null || name.isEmpty() ) {
			name = java.util.UUID.randomUUID().toString();
		}
		return Key.of( name );
	}

	/**
	 * Build a thread context for the given context and name
	 *
	 * @param context The context initiating the thread
	 * @param name    The name of the thread
	 *
	 * @return The thread context
	 */
	public ThreadComponentBoxContext createThreadContext( IBoxContext context, Key name, IStruct attributes ) {
		// Generate a new thread context of execution
		return new ThreadComponentBoxContext( context, this, name, attributes );
	}

	/**
	 * Starts a non-virtual thread using the given context, name, priority, and task.
	 *
	 * @param context  The thread context to run in
	 * @param name     The name of the thread, if empty or null, a random name is generated
	 * @param priority The priority of the thread, can be "high", "low", or "normal", the default is "normal"
	 * @param task     The task to run in the thread, lambda or runnable
	 *
	 * @return The thread instance already started
	 */
	public Thread startThread( ThreadComponentBoxContext context, Key name, String priority, Runnable task ) {
		return startThread( context, name, priority, task, false );
	}

	/**
	 * Starts a thread using the given context, name, virtual, priority, task, and whether it's virtual or not.
	 *
	 * @param context  The thread context to run in
	 * @param name     The name of the thread, if empty or null, a random name is generated
	 * @param priority The priority of the thread, can be "high", "low", or "normal", the default is "normal"
	 * @param task     The task to run in the thread, lambda or runnable
	 * @param virtual  Whether the thread is virtual or not
	 *
	 * @return The thread instance already started
	 */
	public Thread startThread( ThreadComponentBoxContext context, Key name, String priority, Runnable task, boolean virtual ) {
		// Create a new thread definition
		java.lang.Thread thread = virtual
		    ? Thread.ofVirtual().name( DEFAULT_THREAD_PREFIX + name.getName() ).unstarted( task )
		    : new java.lang.Thread(
		        // Use the BoxLang thread group
		        getThreadGroup(),
		        // The taks to run asynch
		        task,
		        // The internal name of the thread
		        DEFAULT_THREAD_PREFIX + name.getName()
		    );

		// Set the priority of the thread if it's not the default
		thread.setPriority( switch ( priority ) {
			case "high" -> java.lang.Thread.MAX_PRIORITY;
			case "low" -> java.lang.Thread.MIN_PRIORITY;
			default -> java.lang.Thread.NORM_PRIORITY;
		} );
		// Register the thread in the context
		context.setThread( thread );
		// Finally we tell the thread manager about itself
		registerThread( name, context );
		// Up up and away
		thread.start();

		return thread;
	}

	/**
	 * This method is used to terminate a thread. It's not foolproof and the JVM
	 * could still be running the thread after this method is called.
	 * <p>
	 * We try to interrupt the thread first, then we wait for x milliseconds for the
	 * thread to stop. If it doesn't stop, we force kill it. Well at least we try to force it.
	 *
	 * @param name The name of the thread
	 *
	 * @throws BoxRuntimeException If the thread is not found
	 */
	@SuppressWarnings( "removal" )
	public void terminateThread( Key name ) {
		IStruct threadData = getThreadData( name );
		if ( threadData == null ) {
			return;
		}
		ThreadComponentBoxContext	context			= ( ThreadComponentBoxContext ) threadData.get( Key.context );
		java.lang.Thread			targetThread	= context.getThread();

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
	 * Interrupt the current thread
	 *
	 * @param context The context in which the BIF is being invoked.
	 *
	 * @return true if the thread is interrupted
	 */
	public boolean IsThreadInterrupted( IBoxContext context ) {
		return context
		    .getParentOfType( ThreadComponentBoxContext.class )
		    .getThread()
		    .isInterrupted();
	}

	/**
	 * Interrupt a named thread
	 *
	 * @param threadName The name of the thread
	 *
	 * @return true if the thread is interrupted
	 */
	public boolean IsThreadInterrupted( Key threadName ) {
		IStruct threadData = getThreadData( threadName );
		if ( threadData == null ) {
			return false;
		}
		return ( ( ThreadComponentBoxContext ) threadData.get( Key.context ) )
		    .getThread()
		    .isInterrupted();
	}

	/**
	 * Verify if a thread is alive
	 *
	 * @param threadName The name of the thread
	 *
	 * @return true if the thread is alive
	 */
	public boolean isThreadAlive( Key threadName ) {
		IStruct threadData = getThreadData( threadName );
		if ( threadData != null ) {
			return ( ( ThreadComponentBoxContext ) threadData.get( Key.context ) )
			    .getThread()
			    .isAlive();
		}
		return false;
	}

	/**
	 * Joins all threads in the request thread manager
	 *
	 * @param timeout The timeout for the join
	 */
	public void joinAllThreads( Integer timeout ) {
		joinThreads(
		    Array.fromArray( getThreadNames() ),
		    timeout
		);
	}

	/**
	 * Join an array of thread names
	 *
	 * @param names   Array of thread names to join.
	 * @param timeout The timeout for the join
	 */
	public void joinThreads( Array names, Integer timeout ) {
		int		timeoutMSLeft	= timeout;
		long	start			= System.currentTimeMillis();

		for ( Object threadName : names ) {
			// Send for joining
			joinThread( Key.of( threadName ), timeoutMSLeft );

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

	/**
	 * Join a thread by name
	 * <p>
	 * This method will join a thread by name. If the thread is not found, an exception is thrown.
	 * If the thread is found, it will be joined. If a timeout is provided, the join will be aborted
	 * if the timeout is reached.
	 * <p>
	 *
	 * @param name    The name of the thread
	 * @param timeout The timeout for the join
	 */
	public void joinThread( Key name, Integer timeout ) {
		Objects.requireNonNull( name, "Thread name is required for join" );
		try {
			IStruct threadData = getThreadData( name );
			if ( threadData == null ) {
				return;
			}
			( ( ThreadComponentBoxContext ) threadData.get( Key.context ) )
			    .getThread()
			    .join( timeout );
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException( "Thread join interrupted", e );
		}
	}

	/**
	 * Interrupt ALL threads in this manager
	 */
	public void interruptAllThreads() {
		for ( Key threadName : getThreadNames() ) {
			interruptThread( threadName );
		}
	}

	/**
	 * Interrupt a thread by name
	 *
	 * @param name The name of the thread
	 */
	public void interruptThread( Key name ) {
		IStruct threadData = getThreadData( name );
		if ( threadData == null ) {
			return;
		}
		ThreadComponentBoxContext	threadContext	= ( ThreadComponentBoxContext ) threadData.get( Key.context );
		IStruct						threadMetadata	= threadData.getAsStruct( Key.metadata );
		// Interrupt the thread
		threadContext.getThread().interrupt();
		// Then we mark the thread as interrupted
		threadMetadata.put( Key.interrupted, true );
		// Update status
		threadMetadata.put( Key.status, "INTERRUPTED" );
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
	 * Check if a thread exists by name
	 *
	 * @param name The name of the thread
	 *
	 * @return true if the thread exists
	 */
	public boolean hasThread( Key name ) {
		return this.threads.containsKey( name );
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
