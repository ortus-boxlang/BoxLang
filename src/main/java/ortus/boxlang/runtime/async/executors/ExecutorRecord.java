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
package ortus.boxlang.runtime.async.executors;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.async.tasks.ScheduledTask;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.AsyncService.ExecutorType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A record for an executor
 *
 * @param executor   The executor service
 * @param name       The name of the executor
 * @param type       The executor type
 * @param maxThreads The max threads, if applicable
 */
public record ExecutorRecord( ExecutorService executor, String name, ExecutorType type, Integer maxThreads ) {

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger( ExecutorRecord.class );

	/**
	 * Get the executor service casted as a {@link BoxScheduledExecutor}
	 *
	 * @return The executor service
	 */
	public BoxScheduledExecutor scheduledExecutor() {
		return ( BoxScheduledExecutor ) this.executor;
	}

	/**
	 * Calls the `shutdown` of the executor - which is non blocking
	 */
	public void shutdownQuiet() {
		if ( executor == null )
			return;
		logger.info( "Executor ({}) shuttingdown quiet", this.name );
		this.executor.shutdown();
	}

	/**
	 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or
	 * the current thread is interrupted, whichever happens first.
	 *
	 * @param timeout The maximum time to wait
	 * @param unit    The time unit to use, available units are: days, hours, microseconds, milliseconds, minutes, nanoseconds, and seconds. The default
	 */
	public void shutdownAndAwaitTermination( Long timeout, TimeUnit unit ) {
		if ( executor == null )
			return;
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
	 * Build out a new scheduled task bound to this executor
	 * Calling this method is not the mean that the task will be executed.
	 * It just builds out a record for the task. It will be your job to call the {@code start()} method on it to start the task.
	 *
	 * This can be used to create a task and then schedule it to run at a later time and not
	 * bound to a Scheduler.
	 *
	 * @param name The name of the task
	 *
	 * @return The new task bound to this executor
	 */
	public ScheduledTask newTask( String name ) {
		return new ScheduledTask( name, this );
	}

	/**
	 * Build out a new scheduled task bound to this executor
	 * Calling this method is not the mean that the task will be executed.
	 * It just builds out a record for the task. It will be your job to call the {@code start()} method on it to start the task.
	 *
	 * This can be used to create a task and then schedule it to run at a later time and not
	 * bound to a Scheduler.
	 *
	 * The name will be auto-generated
	 *
	 * @return The new task bound to this executor
	 */
	public ScheduledTask newTask() {
		var name = "Task-" + System.currentTimeMillis();
		return new ScheduledTask( name, this );
	}

	/**
	 * Our very own stats struct map to give you a holistic view of the executor
	 *
	 * @return The stats struct
	 */
	public IStruct getStats() {
		switch ( this.type ) {
			case SINGLE :
			case VIRTUAL : {
				return Struct.of(
				    "activeCount", 0,
				    "completedTaskCount", 0,
				    "corePoolSize", 1,
				    "isShutdown", this.executor.isShutdown(),
				    "isTerminated", this.executor.isTerminated(),
				    "isTerminating", this.executor.isTerminated(),
				    "largestPoolSize", 1,
				    "maximumPoolSize", 1,
				    "maxThreads", this.maxThreads,
				    "name", this.name,
				    "poolSize", 1,
				    "taskCount", 1,
				    "type", this.type
				);
			}
			case CACHED :
			case FIXED :
			case SCHEDULED :
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
			case WORK_STEALING :
			case FORK_JOIN :
				ForkJoinPool pool = ( ForkJoinPool ) this.executor;
				return Struct.of(
				    "activeCount", 0,
				    "completedTaskCount", 0,
				    "corePoolSize", pool.getPoolSize(),
				    "isShutdown", pool.isShutdown(),
				    "isTerminated", pool.isTerminated(),
				    "isTerminating", pool.isTerminating(),
				    "largestPoolSize", pool.getPoolSize(),
				    "maximumPoolSize", pool.getPoolSize(),
				    "maxThreads", this.maxThreads,
				    "name", this.name,
				    "poolSize", pool.getPoolSize(),
				    "taskCount", pool.getRunningThreadCount(),
				    "type", this.type,
				    "queuedTaskCount", pool.getQueuedTaskCount(),
				    "queuedSubmissionTaskCount", pool.getQueuedSubmissionCount(),
				    "hasQueuedSubmissions", pool.hasQueuedSubmissions(),
				    "stealCount", pool.getStealCount()
				);
			default :
				return new Struct();
		}
	}

	/**
	 * Submit proxy to the executor
	 *
	 * @param runnable The runnable to submit
	 *
	 * @return The future
	 */
	public Future<?> submit( Runnable runnable ) {
		return this.executor.submit( runnable );
	}

	/**
	 * Submit proxy to the executor
	 *
	 * @param callable The callable to submit
	 *
	 * @return The future
	 */
	public Future<?> submit( Callable<?> callable ) {
		return this.executor.submit( callable );
	}

	/**
	 * Method to submit a Callable to the executor and return the result
	 *
	 * @param fn The Runnable lambda to submit
	 *
	 * @return The result of the submission and retrieval
	 */
	public Object submitAndGet( Callable<? extends Object> fn ) {
		try {
			return this.executor.submit( fn ).get();
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An interruption occurred while attempting to process the requested method in parallel", e
			);

		} catch ( ExecutionException e ) {
			throw new BoxRuntimeException(
			    "An execution error occurred while attempting to process the requested method in  in parallel", e
			);
		} finally {
			shutdownQuiet();
		}
	}

	/**
	 * Method to submit a Callable to the executor and return the result
	 *
	 * @param fn The Runnable lambda to submit
	 *
	 * @return The result of the submission and retrieval
	 */
	public Object submitAndGet( ForkJoinTask<? extends Object> fn ) {
		try {
			ForkJoinPool exec = ( ForkJoinPool ) this.executor;
			return exec.submit( fn ).get();
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An interruption occurred while attempting to process the requested method in parallel", e
			);

		} catch ( ExecutionException e ) {
			throw new BoxRuntimeException(
			    "An execution error occurred while attempting to process the requested method in  in parallel", e
			);
		} finally {
			shutdownQuiet();
		}
	}

	/**
	 * Method to submit a runnable to the executor and return the result
	 *
	 * @param fn The Runnable lambda to submit
	 *
	 * @return The result of the submission and retrieval
	 */
	public Object submitAndGet( Runnable fn ) {
		try {
			return this.executor.submit( fn ).get();
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "An interruption occurred while attempting to process the requested method in parallel", e
			);

		} catch ( ExecutionException e ) {
			throw new BoxRuntimeException(
			    "An execution error occurred while attempting to process the requested method in  in parallel", e
			);
		} finally {
			shutdownQuiet();
		}
	}
}
