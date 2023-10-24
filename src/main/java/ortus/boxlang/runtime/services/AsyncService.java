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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 *
 */
public class AsyncService extends BaseService {

	/**
	 * Executor types we support
	 */
	public enum ExecutorType {
		SINGLE_THREAD,  // Single-threaded executor
		FIXED_THREAD,   // Fixed-size thread pool executor
		CACHED_THREAD,  // Cached thread pool executor
		WORK_STEALING,  // Work-stealing executor
		FORK_JOIN      // Fork-Join Pool executor
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
	 * Singleton instance
	 */
	private static AsyncService			instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private AsyncService() {
		logger.info( "AsyncService.onStartup()" );
	}

	/**
	 * Get an instance of the service
	 *
	 * @return The singleton instance
	 */
	public static synchronized AsyncService getInstance() {
		if ( instance == null ) {
			instance = new AsyncService();
		}
		return instance;
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
		logger.info( "AsyncService.onStartup()" );
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	@Override
	public void onConfigurationLoad() {
		logger.info( "AsyncService.onConfigurationLoad()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		logger.info( "AsyncService.onShutdown()" );
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
	 * Create a new executor if it does not exist.
	 *
	 * @param name       The name of the executor
	 * @param type       The executor type
	 * @param maxThreads The max threads, if applicable
	 *
	 * @return The executor record
	 */
	public ExecutorRecord newExecutor( String name, ExecutorType type, int maxThreads ) {
		return this.executors.getOrDefault( name, buildExecutor( name, type, maxThreads ) );
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
			if ( Boolean.TRUE.equals( force ) ) {
				getExecutor( name ).executor().shutdownNow();
			} else {
				getExecutor( name ).shutdownAndAwaitTermination( timeout, unit );
			}
		}
		return this;
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

		this.executors.keySet()
		    .parallelStream()
		    .forEach( executorName -> shutdownExecutor( executorName, force, timeout, unit ) );

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
	Struct getExecutorStatusMap() {
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
	Struct getExecutorStatusMap( String name ) {
		return getExecutor( name ).getStats();
	}

	/**
	 * Build an executor
	 *
	 * @param name       The name of the executor
	 * @param type       The executor type
	 * @param maxThreads The max threads, if applicable
	 *
	 * @return The executor
	 */
	private ExecutorRecord buildExecutor( String name, ExecutorType type, int maxThreads ) {
		ExecutorService executor = null;
		switch ( type ) {
			case SINGLE_THREAD :
				executor = Executors.newSingleThreadExecutor();
				break;
			case FIXED_THREAD :
				executor = Executors.newFixedThreadPool( maxThreads );
				break;
			case CACHED_THREAD :
				executor = Executors.newCachedThreadPool();
				break;
			case WORK_STEALING :
				executor = Executors.newWorkStealingPool( maxThreads );
				break;
			case FORK_JOIN :
				executor = Executors.newWorkStealingPool( maxThreads );
				break;
		}
		return new ExecutorRecord( executor, name, type, maxThreads );
	}

	/**
	 * A record for an executor
	 *
	 * @param executor   The executor service
	 * @param name       The name of the executor
	 * @param type       The executor type
	 * @param maxThreads The max threads, if applicable
	 */
	public record ExecutorRecord( ExecutorService executor, String name, ExecutorType type, int maxThreads ) {

		/**
		 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or
		 * the current thread is interrupted, whichever happens first.
		 *
		 * @param timeout The maximum time to wait
		 * @param unit    The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
		 */
		public void shutdownAndAwaitTermination( Long timeout, TimeUnit unit ) {
			timeout	= timeout == null ? AsyncService.DEFAULT_TIMEOUT : timeout;
			unit	= unit == null ? TimeUnit.SECONDS : unit;

			// Disable new tasks from being submitted
			this.executor.shutdown();
			try {
				logger.info( "Executor ({}) shutdown executed, waiting for tasks to finalize...", this.name );

				// Wait for tasks to terminate
				if ( !this.executor.awaitTermination( timeout, unit ) ) {
					logger.info( "Executor tasks did not shutdown, forcibly shutting down executor ({})...", this.name );

					// Cancel all tasks forcibly
					List<Runnable> taskList = this.executor.shutdownNow();

					logger.info( "Tasks waiting execution on executor ({}) -> tasks({})", this.name, taskList.size() );

					// Wait again now forcibly
					if ( !this.executor.awaitTermination( timeout, unit ) ) {
						logger.error( "Executor ({}) did not terminate even gracefully :(", this.name );
					}
				} else {
					logger.info( "Executor ({}) shutdown complete", this.name );
				}
			}
			// Catch if exceptions or interrupted
			catch ( InterruptedException e ) {
				logger.error( "Executor ({}) shutdown interrupted or exception thrown ({}) :)", this.name, e.getMessage() );
				// force it down!
				this.executor.shutdownNow();
				// Preserve interrupt status
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * Our very own stats struct map to give you a holistic view of the executor
		 *
		 * @return The stats struct
		 */
		public Struct getStats() {
			ThreadPoolExecutor thisExecutor = ( ThreadPoolExecutor ) this.executor;
			return Struct.of(
			    "activeCount", thisExecutor.getActiveCount(),
			    "completedTaskCount", thisExecutor.getCompletedTaskCount(),
			    "corePoolSize", thisExecutor.getCorePoolSize(),
			    "isShutdown", thisExecutor.isShutdown(),
			    "isTerminated", thisExecutor.isTerminated(),
			    "isTerminating", thisExecutor.isTerminating(),
			    "largestPoolSize", thisExecutor.getLargestPoolSize(),
			    "maximumPoolSize", thisExecutor.getMaximumPoolSize(),
			    "maxThreads", this.maxThreads,
			    "name", this.name,
			    "poolSize", thisExecutor.getPoolSize(),
			    "taskCount", thisExecutor.getTaskCount(),
			    "type", this.type
			);
		}
	}

}
