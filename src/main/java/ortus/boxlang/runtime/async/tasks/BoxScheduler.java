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
package ortus.boxlang.runtime.async.tasks;

import java.util.Optional;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * This class acts as a wrapper around the a target BoxLang scheduler not JAVA
 * It implements the {@link IScheduler} interface and provides
 * the life-cycle callbacks that are required by the BoxLang
 * scheduler system.
 */
public class BoxScheduler extends BaseScheduler {

	/**
	 * The target BoxLang scheduler
	 */
	IClassRunnable target;

	/**
	 * Constructor
	 *
	 * @param target The target scheduler
	 */
	public BoxScheduler( IClassRunnable target, IBoxContext context ) {
		super();
		this.target = target;
		setContext( context );
		prepTarget();
	}

	/**
	 * Configure the scheduler
	 */
	@Override
	public void configure() {
		this.target.dereferenceAndInvoke(
		    this.context,
		    Key.configure,
		    DynamicObject.EMPTY_ARGS,
		    false
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life - Cycle Callbacks
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Called before the scheduler is going to be shutdown
	 */
	@Override
	public void onShutdown() {
		if ( this.target.getThisScope().containsKey( Key.onShutdown ) ) {
			this.target.dereferenceAndInvoke(
			    this.context,
			    Key.onShutdown,
			    DynamicObject.EMPTY_ARGS,
			    false
			);
		} else {
			this.logger.info( "Shutting down scheduler [{}]", this.name );
		}
	}

	/**
	 * Called after the scheduler has registered all schedules
	 */
	@Override
	public void onStartup() {
		if ( this.target.getThisScope().containsKey( Key.onStartup ) ) {
			this.target.dereferenceAndInvoke(
			    this.context,
			    Key.onStartup,
			    DynamicObject.EMPTY_ARGS,
			    false
			);
		} else {
			this.logger.info( "Starting up scheduler [{}]", this.name );
		}
	}

	/**
	 * Called whenever ANY task fails
	 *
	 * @param task      The task that got executed
	 * @param exception The exception object
	 */
	@Override
	public void onAnyTaskError( ScheduledTask task, Exception exception ) {
		if ( this.target.getThisScope().containsKey( Key.onAnyTaskError ) ) {
			this.target.dereferenceAndInvoke(
			    this.context,
			    Key.onAnyTaskError,
			    new Object[] {
			        task,
			        exception
			    },
			    false
			);
		} else {
			this.logger.error(
			    "Task [{}.{}] has failed with {}",
			    getSchedulerName(),
			    task.getName(),
			    exception.getMessage(),
			    exception
			);
		}
	}

	/**
	 * Called whenever ANY task succeeds
	 *
	 * @param task   The task that got executed
	 * @param result The result (if any) that the task produced
	 */
	@Override
	public void onAnyTaskSuccess( ScheduledTask task, Optional<?> result ) {
		if ( this.target.getThisScope().containsKey( Key.onAnyTaskSuccess ) ) {
			this.target.dereferenceAndInvoke(
			    this.context,
			    Key.onAnyTaskSuccess,
			    new Object[] {
			        task,
			        result
			    },
			    false
			);
		} else {
			this.logger.info( "Task [{}.{}] has succeeded", getSchedulerName(), task.getName() );
		}
	}

	/**
	 * Called before ANY task runs
	 *
	 * @param task The task about to be executed
	 */
	@Override
	public void beforeAnyTask( ScheduledTask task ) {
		if ( this.target.getThisScope().containsKey( Key.beforeAnyTask ) ) {
			this.target.dereferenceAndInvoke(
			    this.context,
			    Key.beforeAnyTask,
			    new Object[] {
			        task
			    },
			    false
			);
		} else {
			this.logger.debug( "Task [{}.{}] is about to run", getSchedulerName(), task.getName() );
		}
	}

	/**
	 * Called after ANY task runs
	 *
	 * @param task   The task that got executed
	 *
	 * @param result The result (if any) that the task produced
	 */
	@Override
	public void afterAnyTask( ScheduledTask task, Optional<?> result ) {
		if ( this.target.getThisScope().containsKey( Key.afterAnyTask ) ) {
			this.target.dereferenceAndInvoke(
			    this.context,
			    Key.afterAnyTask,
			    new Object[] {
			        task,
			        result
			    },
			    false
			);
		} else {
			this.logger.debug(
			    "Task [{}.{}] has run with result [{}]",
			    getSchedulerName(),
			    task.getName(),
			    result.isPresent() ? result.get() : "no result"
			);
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * PRIVATE METHODS
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Preps the target scheduler for execution
	 */
	private void prepTarget() {
		VariablesScope	variablesScope	= this.target.getVariablesScope();
		BoxRuntime		runtime			= BoxRuntime.getInstance();

		// DI
		variablesScope.put( Key.scheduler, this );
		variablesScope.put( Key.boxRuntime, runtime );
		variablesScope.put( Key.asyncService, runtime.getAsyncService() );
		variablesScope.put( Key.interceptorService, runtime.getInterceptorService() );
		variablesScope.put( Key.cacheService, runtime.getCacheService() );
		variablesScope.put( Key.logger, getLogger() );
	}

}
