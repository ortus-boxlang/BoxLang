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
package ortus.boxlang.runtime.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxScheduledExecutor;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * The BoxLang Async Service is a service that allows you to create and manage executors.
 * Every time a new executor is created it will be automatically registered with the service.
 * You can then use the service to get the executor by name and perform operations on it.
 *
 * When you get an executor by name, you will get an {@link ExecutorRecord} which contains the executor
 * and some metadata about it. You can also get a map of all the executors and their metadata.
 *
 * The available executor types are:
 *
 * <ul>
 * <li>CACHED</li>
 * <li>FIXED</li>
 * <li>SINGLE</li>
 * <li>SCHEDULED</li>
 * <li>WORK_STEALING</li>
 * </ul>
 *
 * The default max threads is 20, you can override this by passing in a maxThreads value for some of the executors.
 * We also use a default timeout of 30 seconds for shutdown and await termination, you can override this as well.
 *
 * Please note that we do not use direct forced shutdown of the executors, we use the
 * {@link ortus.boxlang.runtime.async.tasks.BaseScheduler#shutdown()}
 * and
 * {@link ExecutorRecord#shutdownAndAwaitTermination(Long, TimeUnit)}
 * methods to allow the tasks to finish gracefully. If you want to force shutdown, you can do so by passing in a {code force = true}
 */
public class AsyncService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Executor types we support
	 */
	public enum ExecutorType {
		CACHED,  // Cached thread pool executor
		FIXED,   // Fixed-size thread pool executor
		SINGLE,  // Single-threaded executor
		SCHEDULED, // Scheduled thread pool
		WORK_STEALING,  // Work-stealing executor,
		FORK_JOIN // Fork join pool
	}

	/**
	 * Default max threads
	 */
	public static final int				DEFAULT_MAX_THREADS	= 20;

	/**
	 * Default timeout in seconds for shutdown and await termination
	 */
	public static final Long			DEFAULT_TIMEOUT		= 30L;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private Map<String, ExecutorRecord>	executors			= new ConcurrentHashMap<>();

	/**
	 * Logger
	 */
	private static final Logger			logger				= LoggerFactory.getLogger( AsyncService.class );

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The runtime singleton
	 */
	public AsyncService( BoxRuntime runtime ) {
		super( runtime );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		// register the core tasks executor: boxlang-tasks
		newScheduledExecutor( "boxlang-tasks", DEFAULT_MAX_THREADS );
		logger.info( "AsyncService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force Whether the shutdown is forced
	 */
	@Override
	public void onShutdown( Boolean force ) {
		logger.info( "AsyncService.onShutdown()" );
		shutdownAllExecutors( force, DEFAULT_TIMEOUT, TimeUnit.SECONDS );
	}

	/**
	 * The executors struct
	 *
	 * @return The executors struct
	 */
	public Map<String, ExecutorRecord> getExecutors() {
		return executors;
	}

	/**
	 * Create a new executor if it does not exist. We use the default threads
	 *
	 * @param name The name of the executor
	 * @param type The executor type
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newExecutor( String name, ExecutorType type ) {
		return newExecutor( name, type, DEFAULT_MAX_THREADS );
	}

	/**
	 * Create a new executor if it does not exist.
	 *
	 * @param name       The name of the executor
	 * @param type       The executor type
	 * @param maxThreads The max threads, if applicable
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newExecutor( String name, ExecutorType type, int maxThreads ) {
		this.executors.computeIfAbsent( name, key -> buildExecutor( name, type, maxThreads ) );
		return this.executors.get( name );
	}

	/**
	 * Verify if an executor exists
	 *
	 * @param name The name of the executor
	 *
	 * @return True if it exists, false otherwise
	 */
	public Boolean hasExecutor( String name ) {
		return this.executors.containsKey( name );
	}

	/**
	 * Get an executor by name
	 *
	 * @param name The name of the executor
	 *
	 * @throws KeyNotFoundException If the executor does not exist
	 *
	 * @return The executor
	 */
	public ExecutorRecord getExecutor( String name ) {
		if ( !hasExecutor( name ) ) {
			throw new KeyNotFoundException( "Executor [" + name + "] does not exist. Valid executors are " + this.executors.keySet().toString() );
		}
		return this.executors.get( name );
	}

	/**
	 * Delete an executor by name, if the executor has not shutdown,
	 * it will be shutdown for you via the {@code shutdownNow()} method.
	 *
	 * @param name The name of the executor
	 */
	public AsyncService deleteExecutor( String name ) {
		if ( hasExecutor( name ) ) {
			ExecutorRecord targetExecutor = this.executors.remove( name );
			if ( targetExecutor.executor().isShutdown() ) {
				targetExecutor.executor().shutdownNow();
			}
		}
		return this;
	}

	/**
	 * Shutdown an executor or force it to shutdown, you can also do this from the Executor themselves.
	 * If an un-registered executor name is passed, it will ignore it
	 *
	 * @param name    The name of the executor
	 * @param force   Use the shutdownNow() instead of the shutdown() method
	 * @param timeout The timeout to use when force=false, to make sure all tasks finish gracefully. Deafult is 30 seconds.
	 * @param unit    The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 */
	public AsyncService shutdownExecutor(
	    String name,
	    Boolean force,
	    Long timeout,
	    TimeUnit unit ) {

		timeout	= timeout == null ? AsyncService.DEFAULT_TIMEOUT : timeout;
		unit	= unit == null ? TimeUnit.SECONDS : unit;

		if ( hasExecutor( name ) ) {

			logger.atDebug().log( "+ Shutting down executor ({}), with force ({}) and timeout ({})...", name, force, timeout );
			getTimerUtil().start( "shutdown-executor-" + name );

			if ( Boolean.TRUE.equals( force ) ) {
				getExecutor( name ).executor().shutdownNow();
			} else {
				getExecutor( name ).shutdownAndAwaitTermination( timeout, unit );
			}

			logger.atDebug().log(
			    "+ Shutdown executor ({}) in [{}]",
			    name,
			    getTimerUtil().stop( "shutdown-executor-" + name )
			);
		}

		return this;
	}

	/**
	 * Shutdown all executors or force them to shutdown, you can also do this from the Executor themselves.
	 * This uses a force of false and a default timeout of 30 seconds.
	 */
	public AsyncService shutdownAllExecutors() {
		return shutdownAllExecutors( false, DEFAULT_TIMEOUT, TimeUnit.SECONDS );
	}

	/**
	 * Shutdown all executors or force them to shutdown, you can also do this from the Executor themselves.
	 * We do this in parallel to speed things up.
	 *
	 * @param force   Use the shutdownNow() instead of the shutdown() method
	 * @param timeout The timeout to use when force=false, to make sure all tasks finish gracefully. Deafult is 30 seconds.
	 * @param unit    The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 */
	public AsyncService shutdownAllExecutors(
	    Boolean force,
	    Long timeout,
	    TimeUnit unit ) {

		getTimerUtil().start( "shutdownAllExecutors" );

		logger.atDebug().log( "+ Starting to shutdown all executors..." );

		this.executors.keySet()
		    .parallelStream()
		    .forEach( executorName -> shutdownExecutor( executorName, force, timeout, unit ) );

		logger.atDebug().log(
		    "+ Shutdown all async executor services in [{}]",
		    getTimerUtil().stop( "shutdownAllExecutors" )
		);

		return this;
	}

	/**
	 * Get the executor names registered with the service
	 *
	 * @return The executor names
	 */
	public List<String> getExecutorNames() {
		return this.executors.keySet().stream().collect( java.util.stream.Collectors.toList() );
	}

	/**
	 * Get a struct map of all the executors and their stats.
	 *
	 * @return A struct of metadata about the executor or all executors
	 */
	IStruct getExecutorStatusMap() {
		return new Struct(
		    this.executors
		        .entrySet()
		        .parallelStream()
		        .collect( Collectors.toMap(
		            Map.Entry::getKey, // Key remains the same
		            entry -> entry.getValue().getStats() // Method call on the stats
		        ) )
		);
	}

	/**
	 * Get a struct map of a specific executor and its stats.
	 *
	 * @param name The name of the executor
	 *
	 * @throws KeyNotFoundException If the executor does not exist
	 *
	 * @return A struct of metadata about the executor or all executors
	 *
	 */
	IStruct getExecutorStatusMap( String name ) {
		return getExecutor( name ).getStats();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Builder Aliases
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a new cached executor
	 *
	 * @param name The name of the executor
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newCachedExecutor( String name ) {
		return newExecutor( name, ExecutorType.CACHED );
	}

	/**
	 * Build a new fixed executor
	 *
	 * @param name       The name of the executor
	 * @param maxThreads The max threads, if null it will use the default
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newFixedExecutor( String name, Integer maxThreads ) {
		return newExecutor( name, ExecutorType.FIXED, ( maxThreads == null ? DEFAULT_MAX_THREADS : maxThreads ) );
	}

	/**
	 * Build a single executor
	 *
	 * @param name The name of the executor
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newSingleExecutor( String name ) {
		return newExecutor( name, ExecutorType.SINGLE );
	}

	/**
	 * Build a scheduled executor
	 *
	 * @param name       The name of the executor
	 * @param maxThreads The max threads, if null it will use the default
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newScheduledExecutor( String name, Integer maxThreads ) {
		return newExecutor( name, ExecutorType.SCHEDULED, ( maxThreads == null ? DEFAULT_MAX_THREADS : maxThreads ) );
	}

	/**
	 * Build a scheduled executor
	 *
	 * @param name The name of the executor
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newScheduledExecutor( String name ) {
		return newExecutor( name, ExecutorType.SCHEDULED, DEFAULT_MAX_THREADS );
	}

	/**
	 * Build a work stealing executor
	 *
	 * @param name       The name of the executor
	 * @param maxThreads The max threads, if null it will use the default
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newWorkStealingExecutor( String name, Integer maxThreads ) {
		return newExecutor( name, ExecutorType.WORK_STEALING, ( maxThreads == null ? DEFAULT_MAX_THREADS : maxThreads ) );
	}

	/**
	 * Build an executor without registering it using BoxLang specs
	 *
	 * @param name       The name of the executor
	 * @param type       The executor type: CACHED, FIXED, SINGLE, SCHEDULED, WORK_STEALING
	 * @param maxThreads The max threads, if applicable
	 *
	 * @return The executor
	 */
	public static ExecutorRecord buildExecutor( String name, ExecutorType type, Integer maxThreads ) {
		ExecutorService executor = null;
		switch ( type ) {
			case CACHED :
				executor = Executors.newCachedThreadPool();
				break;
			case FIXED :
				executor = Executors.newFixedThreadPool( maxThreads );
				break;
			case SCHEDULED :
				executor = new BoxScheduledExecutor( maxThreads );
				break;
			case SINGLE :
				executor = Executors.newSingleThreadExecutor();
				break;
			case WORK_STEALING :
				executor = Executors.newWorkStealingPool( maxThreads );
				break;
			case FORK_JOIN :
				executor = maxThreads != null ? new ForkJoinPool( maxThreads ) : ForkJoinPool.commonPool();
				break;
			default :
				executor = null;
		}
		return new ExecutorRecord( executor, name, type, maxThreads );
	}

}
