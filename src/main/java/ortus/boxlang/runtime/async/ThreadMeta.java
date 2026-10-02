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
package ortus.boxlang.runtime.async;

import java.util.Collection;
import java.util.Set;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Struct;

/**
 * The metadata a request exposes for one of its threads: {@code thread.status} inside the thread,
 * {@code myThread.elapsedTime} or {@code bxthread.myThread.stackTrace} anywhere in the request.
 * <p>
 * Status, elapsed time and the stack trace describe a live thread, so they are computed when read
 * instead of stored: a stack trace is expensive to capture and a running thread consults its own
 * metadata on every unscoped variable lookup. Every other key is a plain stored value. Once the
 * thread completes the final values are stored and nothing is computed again.
 * <p>
 * Status values:
 * <ul>
 * <li>NOT_STARTED: The thread has been queued but is not processing yet.</li>
 * <li>RUNNNG: The thread is running normally.</li>
 * <li>BLOCKED: The thread is blocked waiting for a monitor lock.</li>
 * <li>WAITING: The thread is waiting or sleeping, including a join on other threads.</li>
 * <li>INTERRUPTED: The thread was interrupted and is still unwinding; set by the manager, never computed.</li>
 * <li>TERMINATED: The thread stopped running due to a terminate action, an error, or an administrator action.</li>
 * <li>COMPLETED: The thread ended normally.</li>
 * </ul>
 */
public class ThreadMeta extends Struct {

	/**
	 * The thread this metadata describes
	 */
	private final Thread		thread;

	/**
	 * When the thread was registered, in epoch millis
	 */
	private final long			startTicks;

	/**
	 * Once true the stored values are final
	 */
	private boolean				completed	= false;

	/**
	 * The one status the manager sets explicitly while the thread is still alive
	 */
	private static final String	INTERRUPTED	= "INTERRUPTED";

	/**
	 * Constructor
	 *
	 * @param name       The thread name
	 * @param thread     The thread being described
	 * @param startTicks When the thread was registered, in epoch millis
	 */
	public ThreadMeta( Key name, Thread thread, long startTicks ) {
		super();
		this.thread		= thread;
		this.startTicks	= startTicks;
		put( Key.targetThread, thread );
		put( Key.id, thread.threadId() );
		put( Key._NAME, name );
		put( Key.elapsedTime, 0L );
		put( Key.error, null );
		put( Key.virtual, thread.isVirtual() );
		put( Key.daemon, thread.isDaemon() );
		put( Key.threadGroup, thread.getThreadGroup().getName() );
		put( Key.output, "" );
		put( Key.stackTrace, "" );
		put( Key.interrupted, false );
		put( Key.priority, switch ( thread.getPriority() ) {
			case Thread.MIN_PRIORITY -> "LOW";
			case Thread.NORM_PRIORITY -> "NORMAL";
			case Thread.MAX_PRIORITY -> "HIGH";
			default -> "UNKNOWN";
		} );
		put( Key.startTime, new DateTime() );
		put( Key.status, "NOT_STARTED" );
	}

	/**
	 * Stores the thread's final state. After this call nothing is computed on read.
	 * Synchronised with {@link #refresh} so a read in flight cannot overwrite the final values.
	 *
	 * @param output      The output the thread produced
	 * @param exception   The exception that terminated the thread, or null
	 * @param interrupted Whether the thread was interrupted
	 */
	public synchronized void complete( String output, Throwable exception, boolean interrupted ) {
		put( Key.interrupted, interrupted );
		put( Key.error, exception );
		put( Key.output, output );
		put( Key.status, exception == null ? "COMPLETED" : "TERMINATED" );
		put( Key.elapsedTime, System.currentTimeMillis() - this.startTicks );
		// An interrupted thread may still be unwinding, so keep where it was
		put( Key.stackTrace, interrupted && this.thread.isAlive() ? stackTraceOf( this.thread ) : "" );
		this.completed = true;
	}

	/**
	 * The stack trace of a thread as one frame per line
	 *
	 * @param thread The thread to inspect
	 *
	 * @return The stack trace, empty if the thread has no frames
	 */
	private static String stackTraceOf( Thread thread ) {
		StringBuilder builder = new StringBuilder();
		for ( StackTraceElement element : thread.getStackTrace() ) {
			builder.append( element.toString() ).append( "\n" );
		}
		return builder.toString();
	}

	// Read paths: compute the live keys before the struct answers

	@Override
	public Object get( Object key ) {
		refresh( key );
		return super.get( key );
	}

	@Override
	public Object get( String key ) {
		refresh( key );
		return super.get( key );
	}

	@Override
	public Object getRaw( Key key ) {
		refresh( key );
		return super.getRaw( key );
	}

	@Override
	public Object getOrDefault( Key key, Object defaultValue ) {
		refresh( key );
		return super.getOrDefault( key, defaultValue );
	}

	/**
	 * Iteration (forEach, duplicate, structValueArray) reads the map directly instead of dereferencing keys
	 */
	@Override
	public Set<Entry<Key, Object>> entrySet() {
		refreshAll();
		return super.entrySet();
	}

	@Override
	public Collection<Object> values() {
		refreshAll();
		return super.values();
	}

	/**
	 * Store the current value of a live key, if that is what is being read
	 *
	 * @param key The key being read, as a Key or a String
	 */
	private synchronized void refresh( Object key ) {
		if ( this.completed || key == null ) {
			return;
		}
		Key target = key instanceof Key keyKey ? keyKey : Key.of( key.toString() );
		if ( target.equals( Key.status ) ) {
			// an interrupt is recorded by the manager and stays until the thread completes
			if ( !INTERRUPTED.equals( super.getRaw( Key.status ) ) ) {
				put( Key.status, currentStatus() );
			}
		} else if ( target.equals( Key.elapsedTime ) ) {
			put( Key.elapsedTime, System.currentTimeMillis() - this.startTicks );
		} else if ( target.equals( Key.stackTrace ) ) {
			put( Key.stackTrace, this.thread.isAlive() ? stackTraceOf( this.thread ) : "" );
		}
	}

	private void refreshAll() {
		refresh( Key.status );
		refresh( Key.elapsedTime );
		refresh( Key.stackTrace );
	}

	/**
	 * The thread's status as BoxLang names it
	 */
	private String currentStatus() {
		boolean failed = super.get( ( Object ) Key.error ) != null;
		return switch ( this.thread.getState() ) {
			case NEW -> "NOT_STARTED";
			case RUNNABLE -> "RUNNNG";
			case TERMINATED -> ( failed ? "TERMINATED" : "COMPLETED" );
			case BLOCKED -> "BLOCKED";
			case WAITING, TIMED_WAITING -> "WAITING";
			default -> "UNKNOWN";
		};
	}

}
