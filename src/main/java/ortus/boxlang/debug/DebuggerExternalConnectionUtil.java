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
package ortus.boxlang.debug;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.logging.BoxLangLogger;

/**
 * DebuggerExternalConnectionUtil
 * <p>
 * A utility class that provides debugging support for BoxLang applications.
 * This class exposes a task queue mechanism suitable for being driven via JDI's
 * ClassType.invokeMethod(...). The debugger should call these methods using a
 * dedicated "invoker" ThreadReference (a short suspended thread).
 * <p>
 * The static methods are intentionally short: they only enqueue tasks or read results.
 * The heavy reflective invocation runs on a separate worker thread.
 * <p>
 * Usage summary:
 * - Debugger calls DebuggerExternalConnectionUtil.start() once (from a safe suspended event thread)
 * to ensure the invoker and worker threads exist.
 * - Debugger finds the invoker ThreadReference by name and uses it to invoke the
 * static methods (enqueueOnObject / enqueueStatic / pollResult) via JDI.
 * <p>
 * Notes:
 * - enqueueOnObject accepts a real Object instance as the first parameter — when
 * called from JDI the ObjectReference will be marshalled.
 * - The service stores results in an internal concurrent map keyed by task id.
 * pollResult removes the result and returns it.
 * - The service makes a best-effort to survive any thrown exceptions.
 */
public final class DebuggerExternalConnectionUtil {

	/**
	 * Logger for timing instrumentation
	 */
	private static final BoxLangLogger										logger				= LoggingService.getInstance()
	    .getLogger( DebuggerExternalConnectionUtil.class );

	/**
	 * Startup timestamp for timing calculations
	 */
	private static volatile long											startupTime			= 0;

	/**
	 * Counter for debuggerHook calls
	 */
	private static final AtomicInteger										debuggerHookCount	= new AtomicInteger( 0 );

	/**
	 * Counter for signalUserCodeStart calls
	 */
	private static final AtomicInteger										signalCount			= new AtomicInteger( 0 );

	/**
	 * The name of the invoker thread that JDI will use for method invocations
	 */
	public static final String												INVOKER_THREAD_NAME	= "BoxLang-DebuggerInvoker";

	/**
	 * Task queue for pending method invocations
	 */
	private static final BlockingQueue<Task>								QUEUE				= new LinkedBlockingQueue<>();

	/**
	 * Results map keyed by task ID
	 */
	private static final ConcurrentHashMap<String, Result>					RESULTS				= new ConcurrentHashMap<>();

	/**
	 * Object registry for weak references to objects
	 */
	private static final ConcurrentHashMap<String, WeakReference<Object>>	REGISTRY			= new ConcurrentHashMap<>();

	/**
	 * Flag indicating whether the debug threads have been started
	 */
	private static volatile boolean											started				= false;

	/**
	 * Lock object for thread synchronization
	 */
	private static final Object												startLock			= new Object();

	/**
	 * Worker thread reference
	 */
	private static Thread													workerThread;

	/**
	 * Invoker thread reference (used by JDI for method invocations)
	 */
	private static Thread													invokerThread;

	/**
	 * Private constructor to prevent instantiation
	 */
	private DebuggerExternalConnectionUtil() {
		// Static-only class
	}

	// --------------------------------------------------------------------------
	// Static Debug Thread Management (Called via JDI)
	// --------------------------------------------------------------------------

	/**
	 * Idempotent startup. Call this once (from the debugger via JDI) to ensure
	 * the invoker and worker threads exist.
	 */
	public static void start() {
		synchronized ( startLock ) {
			if ( started ) {
				return;
			}

			// Record startup time for timing calculations
			startupTime = System.currentTimeMillis();
			logger.debug( "[TIMING] DebuggerExternalConnectionUtil.start() called - recording startup time" );

			// Start worker thread that processes the task queue
			workerThread = new Thread( () -> {
				while ( !Thread.currentThread().isInterrupted() ) {
					try {
						Task	t	= QUEUE.take();
						Result	r	= executeTask( t );
						if ( r != null ) {
							RESULTS.put( t.id, r );
						}
					} catch ( InterruptedException ie ) {
						Thread.currentThread().interrupt();
						break;
					} catch ( Throwable thr ) {
						// Protect worker from dying; continue processing tasks
					}
				}
			}, "BoxLang-DebuggerWorker" );
			workerThread.setDaemon( true );
			workerThread.start();

			// Start invoker thread: a minimal thread that sleeps.
			// Debugger will suspend this thread and use it as the ThreadReference
			// argument for ClassType.invokeMethod(...). The invoker must do nothing heavy.
			invokerThread = new Thread( () -> {
				Object lock = new Object();
				synchronized ( lock ) {
					while ( true ) {
						try {
							debuggerHook();
							Thread.sleep( 100 );
						} catch ( InterruptedException ignored ) {
							break;
						}
					}
				}
			}, INVOKER_THREAD_NAME );
			invokerThread.setDaemon( true );
			invokerThread.start();

			started = true;
			logger.debug( "[TIMING] DebuggerExternalConnectionUtil.start() completed - invoker and worker threads started" );
		}
	}

	/**
	 * Check if the debugger service has been started
	 *
	 * @return true if the debug threads are running
	 */
	public static boolean isStarted() {
		return started;
	}

	/**
	 * Empty hook method that the debugger can use as a breakpoint target
	 * or to verify the invoker thread is running.
	 */
	public static void debuggerHook() {
		int count = debuggerHookCount.incrementAndGet();
		// Only log every 100th call to avoid flooding logs
		if ( count <= 5 || count % 100 == 0 ) {
			long elapsed = startupTime > 0 ? System.currentTimeMillis() - startupTime : 0;
			logger.debug( "[TIMING] debuggerHook() call #{} at T+{}ms", count, elapsed );
		}
	}

	/**
	 * Signal that BoxLang runtime initialization is complete and user code execution
	 * is about to begin. The debugger can use this as a hook to enable breakpoint
	 * suspension for user code classes.
	 * <p>
	 * This method is called by BoxRuntime just before executing user templates,
	 * classes, statements, or source code. The debugger should set a MethodEntryRequest
	 * on this method and use it as the trigger to enable SUSPEND_EVENT_THREAD for
	 * targeted ClassPrepareRequests.
	 * <p>
	 * This is only called when debugMode is enabled in BoxLang configuration.
	 *
	 * @param templatePath The path to the template/script about to be executed (may be null for inline code)
	 */
	public static void signalUserCodeStart( String templatePath ) {
		int		count	= signalCount.incrementAndGet();
		long	elapsed	= startupTime > 0 ? System.currentTimeMillis() - startupTime : 0;
		logger.debug( "[TIMING] signalUserCodeStart() call #{} at T+{}ms for: {}", count, elapsed, templatePath );
		logger.debug( "[TIMING] debuggerHook() was called {} times before this signal", debuggerHookCount.get() );
	}

	// --------------------------------------------------------------------------
	// Task Enqueueing Methods (Called via JDI)
	// --------------------------------------------------------------------------

	/**
	 * Enqueue a reflective instance invocation on the provided target object.
	 * This method is intentionally short: it merely enqueues a task and returns an id.
	 *
	 * @param target         the object instance to invoke on
	 * @param methodName     simple name of the method to invoke
	 * @param paramTypeNames fully-qualified class names of parameter types
	 * @param args           argument values
	 *
	 * @return task id string
	 */
	public static String enqueueOnObject( Object target, String methodName, String[] paramTypeNames, Object[] args ) {
		Objects.requireNonNull( methodName, "methodName" );
		String	id	= genId();
		Task	t	= new Task( id, target, null, methodName, paramTypeNames, args, TaskType.INSTANCE );
		QUEUE.add( t );
		return id;
	}

	/**
	 * Enqueue a reflective static invocation.
	 *
	 * @param className      fully-qualified class name
	 * @param methodName     simple name of the method to invoke
	 * @param paramTypeNames fully-qualified class names of parameter types
	 * @param args           argument values
	 *
	 * @return task id string
	 */
	public static String enqueueStatic( String className, String methodName, String[] paramTypeNames, Object[] args ) {
		Objects.requireNonNull( className, "className" );
		Objects.requireNonNull( methodName, "methodName" );
		String	id	= genId();
		Task	t	= new Task( id, null, className, methodName, paramTypeNames, args, TaskType.STATIC );
		QUEUE.add( t );
		return id;
	}

	/**
	 * Register an object in the service registry and return an id you can later
	 * use to enqueue tasks by id. The registry uses WeakReference to avoid strong leaks.
	 *
	 * @param target the object to register
	 *
	 * @return registration id
	 */
	public static String register( Object target ) {
		Objects.requireNonNull( target, "target" );
		String id = genId();
		REGISTRY.put( id, new WeakReference<>( target ) );
		return id;
	}

	/**
	 * Enqueue by previously registered id.
	 *
	 * @param registeredId   the registration id returned by register()
	 * @param methodName     simple name of the method to invoke
	 * @param paramTypeNames fully-qualified class names of parameter types
	 * @param args           argument values
	 *
	 * @return task id string
	 */
	public static String enqueueById( String registeredId, String methodName, String[] paramTypeNames, Object[] args ) {
		Objects.requireNonNull( registeredId, "registeredId" );
		WeakReference<Object>	wr		= REGISTRY.get( registeredId );
		Object					target	= ( wr == null ) ? null : wr.get();
		if ( target == null )
			throw new IllegalStateException( "Registered object not found or has been GC'd: " + registeredId );
		return enqueueOnObject( target, methodName, paramTypeNames, args );
	}

	// --------------------------------------------------------------------------
	// Result Retrieval Methods (Called via JDI)
	// --------------------------------------------------------------------------

	/**
	 * Poll result and remove it from the result map. Returns null if not ready.
	 *
	 * @param taskId the task id returned by an enqueue method
	 *
	 * @return the Result, or null if not yet available
	 */
	public static Result pollResult( String taskId ) {
		return RESULTS.remove( taskId );
	}

	/**
	 * Peek result without removing (may be null).
	 *
	 * @param taskId the task id returned by an enqueue method
	 *
	 * @return the Result, or null if not yet available
	 */
	public static Result peekResult( String taskId ) {
		return RESULTS.get( taskId );
	}

	/**
	 * Blocking get with timeout (ms). Returns null if timeout elapses without a result.
	 *
	 * @param taskId    the task id returned by an enqueue method
	 * @param timeoutMs timeout in milliseconds
	 *
	 * @return the Result, or null if timeout elapsed
	 *
	 * @throws InterruptedException if the thread is interrupted while waiting
	 */
	public static Result getResultBlocking( String taskId, long timeoutMs ) throws InterruptedException {
		long deadline = System.currentTimeMillis() + Math.max( 0, timeoutMs );
		while ( System.currentTimeMillis() < deadline ) {
			Result r = RESULTS.remove( taskId );
			if ( r != null )
				return r;
			Thread.sleep( 20 );
		}
		return null;
	}

	/**
	 * Shutdown the debugger threads (best-effort). Useful for tests.
	 */
	public static synchronized void shutdown() {
		synchronized ( startLock ) {
			started = false;
			Thread	w	= workerThread;
			Thread	i	= invokerThread;
			if ( w != null )
				w.interrupt();
			if ( i != null )
				i.interrupt();
			QUEUE.clear();
			RESULTS.clear();
			REGISTRY.clear();
		}
	}

	// --------------------------------------------------------------------------
	// Internal Task Execution
	// --------------------------------------------------------------------------

	private static Result executeTask( Task t ) {
		try {
			switch ( t.type ) {
				case INSTANCE :
					return execInstance( t );
				case STATIC :
					return execStatic( t );
				default :
					return new Result( null, new IllegalArgumentException( "Unknown task type" ) );
			}
		} catch ( Throwable thr ) {
			return new Result( null, thr );
		}
	}

	private static Result execInstance( Task t ) {
		Object target = t.target;
		if ( target == null )
			return new Result( null, new IllegalArgumentException( "target is null" ) );
		try {
			Class<?>	cls			= target.getClass();
			Class<?>[]	paramTypes	= resolveParamTypes( t.paramTypeNames );
			Method		m			= findMethod( cls, t.methodName, paramTypes );
			if ( m == null )
				return new Result( null, new NoSuchMethodException( "Method not found: " + t.methodName + " on " + cls.getName() ) );
			m.setAccessible( true );
			Object rv = m.invoke( target, safeArgsForInvocation( m.getParameterTypes(), t.args ) );
			return new Result( rv, null );
		} catch ( Throwable thr ) {
			return new Result( null, thr );
		}
	}

	private static Result execStatic( Task t ) {
		try {
			Class<?>	cls			= Class.forName( t.className );
			Class<?>[]	paramTypes	= resolveParamTypes( t.paramTypeNames );
			Method		m			= findMethod( cls, t.methodName, paramTypes );
			if ( m == null )
				return new Result( null, new NoSuchMethodException( "Static method not found: " + t.methodName + " on " + t.className ) );
			m.setAccessible( true );
			Object rv = m.invoke( null, safeArgsForInvocation( m.getParameterTypes(), t.args ) );
			return new Result( rv, null );
		} catch ( Throwable thr ) {
			return new Result( null, thr );
		}
	}

	private static Object[] safeArgsForInvocation( Class<?>[] paramTypes, Object[] providedArgs ) {
		if ( ( providedArgs == null || providedArgs.length == 0 ) && ( paramTypes == null || paramTypes.length == 0 ) )
			return new Object[ 0 ];
		Object[] out = new Object[ paramTypes.length ];
		for ( int i = 0; i < paramTypes.length; ++i ) {
			Object provided = ( providedArgs != null && i < providedArgs.length ) ? providedArgs[ i ] : null;
			out[ i ] = coerceArgIfNeeded( paramTypes[ i ], provided );
		}
		return out;
	}

	private static Object coerceArgIfNeeded( Class<?> paramType, Object provided ) {
		if ( provided == null )
			return null;
		if ( paramType.isPrimitive() ) {
			if ( paramType == int.class && provided instanceof Number )
				return ( ( Number ) provided ).intValue();
			if ( paramType == long.class && provided instanceof Number )
				return ( ( Number ) provided ).longValue();
			if ( paramType == short.class && provided instanceof Number )
				return ( ( Number ) provided ).shortValue();
			if ( paramType == byte.class && provided instanceof Number )
				return ( ( Number ) provided ).byteValue();
			if ( paramType == float.class && provided instanceof Number )
				return ( ( Number ) provided ).floatValue();
			if ( paramType == double.class && provided instanceof Number )
				return ( ( Number ) provided ).doubleValue();
			if ( paramType == boolean.class && provided instanceof Boolean )
				return ( ( Boolean ) provided ).booleanValue();
			if ( paramType == char.class && provided instanceof Character )
				return ( ( Character ) provided ).charValue();
		}
		return provided;
	}

	private static Class<?>[] resolveParamTypes( String[] names ) throws ClassNotFoundException {
		if ( names == null || names.length == 0 )
			return new Class<?>[ 0 ];
		Class<?>[] out = new Class<?>[ names.length ];
		for ( int i = 0; i < names.length; ++i ) {
			out[ i ] = resolveClassByName( names[ i ] );
		}
		return out;
	}

	private static Class<?> resolveClassByName( String name ) throws ClassNotFoundException {
		switch ( name ) {
			case "int" :
				return int.class;
			case "long" :
				return long.class;
			case "short" :
				return short.class;
			case "byte" :
				return byte.class;
			case "char" :
				return char.class;
			case "boolean" :
				return boolean.class;
			case "float" :
				return float.class;
			case "double" :
				return double.class;
			case "void" :
				return void.class;
			default :
				return Class.forName( name );
		}
	}

	private static Method findMethod( Class<?> cls, String methodName, Class<?>[] paramTypes ) {
		try {
			return cls.getDeclaredMethod( methodName, paramTypes );
		} catch ( NoSuchMethodException ignored ) {
			// Fallback: try to find a compatible method by name and parameter count/assignability
			for ( Method m : cls.getMethods() ) {
				if ( !m.getName().equals( methodName ) )
					continue;
				Class<?>[] sig = m.getParameterTypes();
				if ( sig.length != ( paramTypes == null ? 0 : paramTypes.length ) )
					continue;
				boolean ok = true;
				for ( int i = 0; i < sig.length; ++i ) {
					if ( paramTypes[ i ] == null )
						continue;
					if ( !isAssignable( sig[ i ], paramTypes[ i ] ) ) {
						ok = false;
						break;
					}
				}
				if ( ok )
					return m;
			}
			return null;
		}
	}

	private static boolean isAssignable( Class<?> target, Class<?> source ) {
		if ( target.isPrimitive() ) {
			if ( target == int.class && Integer.class.equals( source ) )
				return true;
			if ( target == long.class && Long.class.equals( source ) )
				return true;
			if ( target == boolean.class && Boolean.class.equals( source ) )
				return true;
			if ( target == byte.class && Byte.class.equals( source ) )
				return true;
			if ( target == short.class && Short.class.equals( source ) )
				return true;
			if ( target == char.class && Character.class.equals( source ) )
				return true;
			if ( target == float.class && Float.class.equals( source ) )
				return true;
			if ( target == double.class && Double.class.equals( source ) )
				return true;
			return false;
		}
		return target.isAssignableFrom( source );
	}

	private static String genId() {
		return UUID.randomUUID().toString();
	}

	// --------------------------------------------------------------------------
	// Inner Data Classes
	// --------------------------------------------------------------------------

	private enum TaskType {
		INSTANCE, STATIC
	}

	private static final class Task {

		final String	id;
		final Object	target;
		final String	className;
		final String	methodName;
		final String[]	paramTypeNames;
		final Object[]	args;
		final TaskType	type;

		Task( String id, Object target, String className, String methodName, String[] paramTypeNames, Object[] args, TaskType type ) {
			this.id				= id;
			this.target			= target;
			this.className		= className;
			this.methodName		= methodName;
			this.paramTypeNames	= paramTypeNames;
			this.args			= args;
			this.type			= type;
		}
	}

	/**
	 * Result of a task execution, containing either a value or an exception.
	 */
	public static final class Result {

		public final Object		value;
		public final Throwable	exception;
		public final long		timestampMs;

		public Result( Object v, Throwable ex ) {
			this.value			= v;
			this.exception		= ex;
			this.timestampMs	= System.currentTimeMillis();
		}

		public boolean isException() {
			return exception != null;
		}
	}
}
