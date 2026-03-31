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
package ortus.boxlang.runtime.components.async;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ortus.boxlang.runtime.async.tasks.BaseScheduler;
import ortus.boxlang.runtime.async.tasks.ScheduledTask;
import ortus.boxlang.runtime.async.tasks.TaskRecord;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.validation.Validator;

/**
 * The BX:schedule component manages scheduled tasks: create/update, delete, pause, resume, list,
 * pauseall, resumeall, and run immediately via the BoxLang scheduler infrastructure.
 *
 * Tasks are persisted to disk at {@code ${boxLangHome}/config/tasks.json} and automatically
 * reloaded when the runtime starts up.
 *
 * @attribute.action       The action to perform: create (new task, throws if exists), update/modify (create or overwrite), delete, run, pause, resume, list, pauseall, resumeall.
 *
 * @attribute.task         The name of the task (required for all actions except list/pauseall/resumeall).
 *
 * @attribute.scheduler    The name of the BoxLang scheduler to use. Defaults to "bxschedule".
 *
 * @attribute.group        Task group name for organizational purposes.
 *
 * @attribute.url          The URL to request when the task fires (required for create/update/modify).
 *
 * @attribute.interval     Scheduling interval: "once", "daily", "weekly", "monthly", or seconds (>=60).
 *
 * @attribute.isDaily      Shorthand for interval="daily".
 *
 * @attribute.cronTime     A cron expression (5 or 6 fields). Mutually exclusive with interval.
 *
 * @attribute.startDate    Start date constraint (yyyy-mm-dd or similar parseable format).
 *
 * @attribute.startTime    Start time (HH:mm).
 *
 * @attribute.endDate      End date constraint.
 *
 * @attribute.endTime      End time (HH:mm).
 *
 * @attribute.repeat       Maximum number of executions before the task self-disables.
 *
 * @attribute.exclude      Comma-separated dates or date ranges to skip.
 *
 * @attribute.port         HTTP port override for the URL. Defaults to 80.
 *
 * @attribute.username     HTTP basic auth username.
 *
 * @attribute.password     HTTP basic auth password.
 *
 * @attribute.proxyServer  Proxy hostname.
 *
 * @attribute.proxyPort    Proxy port.
 *
 * @attribute.proxyUser    Proxy auth username.
 *
 * @attribute.proxyPassword Proxy auth password.
 *
 * @attribute.publish      If true, write the HTTP response body to a file.
 *
 * @attribute.path         Directory for the published output file.
 *
 * @attribute.file         Output filename (required if publish=true).
 *
 * @attribute.overwrite    If true, overwrite an existing output file. Defaults to true.
 *
 * @attribute.resolveURL   If true, resolve relative URLs in the response output.
 *
 * @attribute.retryCount   Number of retries on failure 0-3. Stored as metadata. Defaults to 3.
 *
 * @attribute.onException  How to handle task exceptions: "refire", "pause", or "invokeHandler".
 *
 * @attribute.oncomplete   URL/path to invoke on task completion (success or failure).
 *
 * @attribute.eventhandler URL/path invoked when onException="invokeHandler".
 *
 * @attribute.cluster      Cluster-aware flag stored as metadata.
 *
 * @attribute.result       Variable name to store list output in.
 */
@BoxComponent( name = "schedule", allowsBody = false, description = "Manages scheduled tasks: create/update, delete, pause, resume, list, and run immediately via the BoxLang scheduler infrastructure." )
public class Schedule extends Component {

	/**
	 * The default scheduler name used when none is specified.
	 */
	public static final String		DEFAULT_SCHEDULER_NAME	= SchedulerService.DEFAULT_SCHEDULER_NAME;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	public Schedule() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action, "string", Set.of( Validator.REQUIRED,
		        Validator.valueOneOf( "create", "update", "modify", "delete", "run", "pause", "resume", "list", "pauseall", "resumeall" )
		    ) ),
		    new Attribute( Key.task, "string" ),
		    new Attribute( Key.scheduler, "string", DEFAULT_SCHEDULER_NAME ),
		    new Attribute( Key.group, "string", "" ),
		    new Attribute( Key.URL, "string" ),
		    new Attribute( Key.interval, "string" ),
		    new Attribute( Key.isDaily, "boolean", false ),
		    new Attribute( Key.cronTime, "string" ),
		    new Attribute( Key.startDate, "string" ),
		    new Attribute( Key.startTime, "string" ),
		    new Attribute( Key.endDate, "string" ),
		    new Attribute( Key.endTime, "string" ),
		    new Attribute( Key.repeat, "integer" ),
		    new Attribute( Key.exclude, "string" ),
		    new Attribute( Key.port, "integer", 80 ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),
		    new Attribute( Key.proxyServer, "string" ),
		    new Attribute( Key.proxyPort, "integer" ),
		    new Attribute( Key.proxyUser, "string" ),
		    new Attribute( Key.proxyPassword, "string" ),
		    new Attribute( Key.publish, "boolean", false ),
		    new Attribute( Key.path, "string" ),
		    new Attribute( Key.file, "string" ),
		    new Attribute( Key.overwrite, "boolean", true ),
		    new Attribute( Key.resolveUrl, "boolean", false ),
		    new Attribute( Key.retryCount, "integer", 3 ),
		    new Attribute( Key.onException, "string", "refire", Set.of(
		        Validator.valueOneOf( "refire", "pause", "invokeHandler" )
		    ) ),
		    new Attribute( Key.onComplete, "string" ),
		    new Attribute( Key.eventHandler, "string" ),
		    new Attribute( Key.cluster, "boolean", false ),
		    new Attribute( Key.result, "string" )
		};
	}

	/**
	 * Manages scheduled tasks in the BoxLang scheduler.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String action = attributes.getAsString( Key.action ).toLowerCase();

		// Normalize action aliases
		if ( action.equals( "modify" ) ) {
			action = "update";
		}

		// All actions except list/pauseall/resumeall require a task name
		if ( !action.equals( "list" ) && !action.equals( "pauseall" ) && !action.equals( "resumeall" ) ) {
			requireTaskName( attributes );
		}

		switch ( action ) {
			case "create" :
				doCreate( context, attributes );
				break;
			case "update" :
				doUpdate( context, attributes );
				break;
			case "delete" :
				doDelete( context, attributes );
				break;
			case "run" :
				doRun( context, attributes );
				break;
			case "pause" :
				doPause( context, attributes );
				break;
			case "resume" :
				doResume( context, attributes );
				break;
			case "list" :
				doList( context, attributes );
				break;
			case "pauseall" :
				doPauseAll( context, attributes );
				break;
			case "resumeall" :
				doResumeAll( context, attributes );
				break;
			default :
				throw new BoxRuntimeException( "Invalid schedule action [" + action + "]" );
		}

		// After any task-modifying action, ensure the scheduler is started.
		// - New schedulers: startup() is called here after tasks have been registered, so the
		//   executor is created with all tasks already in place.
		// - Already-running schedulers: startupScheduler() is a no-op (guards against double-start).
		// - Read-only "list" action is excluded to avoid unintended side effects.
		if ( !action.equals( "list" ) ) {
			String				schedulerName	= attributes.getAsString( Key.scheduler );
			SchedulerService	svc				= runtime.getSchedulerService();
			var					scheduler		= svc.getScheduler( Key.of( schedulerName ) );
			if ( scheduler != null ) {
				svc.startupScheduler( scheduler );
			}
		}

		return DEFAULT_RETURN;
	}

	// --------------------------------------------------------------------------
	// Action handlers
	// --------------------------------------------------------------------------

	/**
	 * Create a new scheduled task. Throws if a task with the same name already exists in the scheduler.
	 * Use {@code action="update"} to overwrite an existing task.
	 */
	private void doCreate( IBoxContext context, IStruct attributes ) {
		String			taskName		= attributes.getAsString( Key.task );
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		BaseScheduler	scheduler		= getOrCreateScheduler( context, schedulerName );

		if ( scheduler.hasTask( taskName ) ) {
			throw new BoxRuntimeException(
			    "Task [" + taskName + "] already exists in scheduler [" + schedulerName + "]. Use action=\"update\" to overwrite it."
			);
		}

		doUpdate( context, attributes );
	}

	/**
	 * Create or overwrite a scheduled task.
	 */
	private void doUpdate( IBoxContext context, IStruct attributes ) {
		String	taskName		= attributes.getAsString( Key.task );
		String	schedulerName	= attributes.getAsString( Key.scheduler );
		String	group			= attributes.getAsString( Key.group );
		String	url				= attributes.getAsString( Key.URL );
		String	interval		= attributes.getAsString( Key.interval );
		String	cronTime		= attributes.getAsString( Key.cronTime );
		boolean	isDaily			= BooleanCaster.cast( attributes.getOrDefault( Key.isDaily, false ) );

		// Validate required attributes
		if ( url == null || url.isBlank() ) {
			throw new BoxRuntimeException( "The [url] attribute is required for schedule action [update]" );
		}

		if ( cronTime == null && interval == null && !isDaily ) {
			throw new BoxRuntimeException( "Either [interval], [cronTime], or [isDaily=true] is required for schedule action [update]" );
		}

		// Get or create scheduler
		BaseScheduler scheduler = getOrCreateScheduler( context, schedulerName );

		// Remove existing task if present (re-register)
		if ( scheduler.hasTask( taskName ) ) {
			scheduler.removeTask( taskName );
		}

		// Build the HTTP callable
		Runnable callable = buildTaskCallable( context, attributes );

		// Register task and apply full configuration
		ScheduledTask task = scheduler.task( taskName, group ).call( callable );
		applyTaskConfiguration( task, callable, attributes, runtime.getRuntimeContext() );

		// Start the task if the scheduler is already running
		if ( scheduler.hasStarted() ) {
			scheduler.startupTask( taskName );
		}

		// Persist to disk
		runtime.getSchedulerService().persistTask( attributes );
	}

	/**
	 * Delete a scheduled task.
	 */
	private void doDelete( IBoxContext context, IStruct attributes ) {
		String			taskName		= attributes.getAsString( Key.task );
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		BaseScheduler	scheduler		= getExistingSchedulerOrFail( schedulerName );

		if ( !scheduler.hasTask( taskName ) ) {
			throw new BoxRuntimeException( "Task [" + taskName + "] does not exist in scheduler [" + schedulerName + "]" );
		}

		scheduler.removeTask( taskName );
		runtime.getSchedulerService().removeTaskFromDisk( taskName, schedulerName );
	}

	/**
	 * Run a scheduled task immediately.
	 */
	private void doRun( IBoxContext context, IStruct attributes ) {
		String			taskName		= attributes.getAsString( Key.task );
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		BaseScheduler	scheduler		= getExistingSchedulerOrFail( schedulerName );
		TaskRecord		record			= scheduler.getTaskRecord( taskName );

		if ( record == null ) {
			throw new BoxRuntimeException( "Task [" + taskName + "] does not exist in scheduler [" + schedulerName + "]" );
		}

		record.task.run( true );
	}

	/**
	 * Pause a scheduled task.
	 */
	private void doPause( IBoxContext context, IStruct attributes ) {
		String			taskName		= attributes.getAsString( Key.task );
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		BaseScheduler	scheduler		= getExistingSchedulerOrFail( schedulerName );
		TaskRecord		record			= scheduler.getTaskRecord( taskName );

		if ( record == null ) {
			throw new BoxRuntimeException( "Task [" + taskName + "] does not exist in scheduler [" + schedulerName + "]" );
		}

		record.task.disable();
		record.disabled = true;
		if ( record.future != null ) {
			record.future.cancel( false );
		}

		runtime.getSchedulerService().updateTaskPausedState( taskName, schedulerName, true );
	}

	/**
	 * Resume a paused task.
	 */
	private void doResume( IBoxContext context, IStruct attributes ) {
		String			taskName		= attributes.getAsString( Key.task );
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		BaseScheduler	scheduler		= getExistingSchedulerOrFail( schedulerName );
		TaskRecord		record			= scheduler.getTaskRecord( taskName );

		if ( record == null ) {
			throw new BoxRuntimeException( "Task [" + taskName + "] does not exist in scheduler [" + schedulerName + "]" );
		}

		record.task.enable();
		record.disabled		= false;
		record.scheduledAt	= null; // Reset so startupTask will re-schedule it
		scheduler.startupTask( taskName );

		runtime.getSchedulerService().updateTaskPausedState( taskName, schedulerName, false );
	}

	/**
	 * List all tasks in a scheduler.
	 */
	private void doList( IBoxContext context, IStruct attributes ) {
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		String			resultVar		= attributes.getAsString( Key.result );
		String			group			= attributes.getAsString( Key.group );

		Array			taskList		= new Array();

		// Only list if scheduler exists
		SchedulerService	svc				= runtime.getSchedulerService();
		Key					schedulerKey	= Key.of( schedulerName );

		if ( svc.hasScheduler( schedulerKey ) ) {
			Object schedulerObj = svc.getScheduler( schedulerKey );
			if ( schedulerObj instanceof BaseScheduler ) {
				BaseScheduler scheduler = ( BaseScheduler ) schedulerObj;
				for ( TaskRecord record : scheduler.getTasks().values() ) {
					// Filter by group if specified
					if ( group != null && !group.isBlank() && !group.equals( record.group ) ) {
						continue;
					}
					taskList.add( buildTaskStruct( record ) );
				}
			}
		}

		if ( resultVar != null && !resultVar.isBlank() ) {
			ExpressionInterpreter.setVariable( context, resultVar, taskList );
		} else {
			// Write to output buffer
			try {
				context.writeToBuffer( JSONUtil.getJSONBuilder( true ).asString( taskList ) );
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Failed to write task list to output: " + e.getMessage(), e );
			}
		}
	}

	/**
	 * Pause all tasks in a scheduler.
	 */
	private void doPauseAll( IBoxContext context, IStruct attributes ) {
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		String			group			= attributes.getAsString( Key.group );

		SchedulerService	svc				= runtime.getSchedulerService();
		Key					schedulerKey	= Key.of( schedulerName );

		if ( !svc.hasScheduler( schedulerKey ) )
			return;
		Object schedulerObj = svc.getScheduler( schedulerKey );
		if ( !( schedulerObj instanceof BaseScheduler ) )
			return;

		BaseScheduler scheduler = ( BaseScheduler ) schedulerObj;

		for ( TaskRecord record : scheduler.getTasks().values() ) {
			if ( shouldIncludeTask( record, group ) ) {
				record.task.disable();
				record.disabled = true;
				if ( record.future != null ) {
					record.future.cancel( false );
				}
			}
		}
		svc.updateAllTasksPausedState( schedulerName, group, true );
	}

	/**
	 * Resume all tasks in a scheduler.
	 */
	private void doResumeAll( IBoxContext context, IStruct attributes ) {
		String			schedulerName	= attributes.getAsString( Key.scheduler );
		String			group			= attributes.getAsString( Key.group );

		SchedulerService	svc				= runtime.getSchedulerService();
		Key					schedulerKey	= Key.of( schedulerName );

		if ( !svc.hasScheduler( schedulerKey ) )
			return;
		Object schedulerObj = svc.getScheduler( schedulerKey );
		if ( !( schedulerObj instanceof BaseScheduler ) )
			return;

		BaseScheduler scheduler = ( BaseScheduler ) schedulerObj;

		for ( TaskRecord record : scheduler.getTasks().values() ) {
			if ( shouldIncludeTask( record, group ) ) {
				record.task.enable();
				record.disabled		= false;
				record.scheduledAt	= null;
				scheduler.startupTask( record.name );
			}
		}
		svc.updateAllTasksPausedState( schedulerName, group, false );
	}

	// --------------------------------------------------------------------------
	// Helper methods
	// --------------------------------------------------------------------------

	/**
	 * Require that the task attribute is present and non-blank.
	 */
	private void requireTaskName( IStruct attributes ) {
		String taskName = attributes.getAsString( Key.task );
		if ( taskName == null || taskName.isBlank() ) {
			throw new BoxRuntimeException( "The [task] attribute is required for this schedule action" );
		}
	}

	/**
	 * Get or create a named {@link BaseScheduler}. If the scheduler doesn't exist it is
	 * registered but NOT yet started — tasks must be added before startup so the executor
	 * is created with all tasks already registered. The caller is responsible for invoking
	 * {@link SchedulerService#startupScheduler} after tasks have been configured.
	 */
	public static BaseScheduler getOrCreateScheduler( IBoxContext context, String name ) {
		SchedulerService	svc				= ortus.boxlang.runtime.BoxRuntime.getInstance().getSchedulerService();
		Key					schedulerKey	= Key.of( name );

		if ( svc.hasScheduler( schedulerKey ) ) {
			Object existing = svc.getScheduler( schedulerKey );
			if ( existing instanceof BaseScheduler ) {
				return ( BaseScheduler ) existing;
			}
		}

		// Register only — do NOT start. Startup happens after tasks are added (see _invoke post-switch).
		BaseScheduler scheduler = new BaseScheduler( name, context );
		svc.registerScheduler( scheduler, false );
		return scheduler;
	}

	/**
	 * Get an existing scheduler by name, throwing if it doesn't exist or is not a BaseScheduler.
	 */
	private BaseScheduler getExistingSchedulerOrFail( String name ) {
		SchedulerService	svc				= runtime.getSchedulerService();
		Key					schedulerKey	= Key.of( name );

		if ( !svc.hasScheduler( schedulerKey ) ) {
			throw new BoxRuntimeException( "Scheduler [" + name + "] does not exist" );
		}
		Object schedulerObj = svc.getScheduler( schedulerKey );
		if ( !( schedulerObj instanceof BaseScheduler ) ) {
			throw new BoxRuntimeException( "Scheduler [" + name + "] is not a managed schedule scheduler" );
		}
		return ( BaseScheduler ) schedulerObj;
	}

	/**
	 * Build a {@link Runnable} that performs an HTTP GET request to the configured URL.
	 * Captures the runtime context (not the request context) for long-lived use.
	 */
	/**
	 * Apply the full task configuration (scheduling, callbacks, metadata) to an already-registered
	 * {@link ScheduledTask}. Used by both {@code doUpdate} and {@code registerPersistedScheduleTasks}
	 * so a reloaded task is configured identically to a freshly-created one.
	 *
	 * @param task           The task to configure.
	 * @param callable       The callable already assigned to the task (needed for the repeat wrapper).
	 * @param taskDef        Struct of task fields — accepts both live attribute structs and persisted JSON structs.
	 * @param runtimeContext The runtime context used for HTTP callbacks.
	 */
	public static void applyTaskConfiguration( ScheduledTask task, Runnable callable, IStruct taskDef, IBoxContext runtimeContext ) {
		String	cronTime	= taskDef.getAsString( Key.cronTime );
		String	interval	= taskDef.getAsString( Key.interval );
		boolean	isDaily		= BooleanCaster.cast( taskDef.getOrDefault( Key.isDaily, false ) );
		String	startDate	= taskDef.getAsString( Key.startDate );
		String	startTime	= taskDef.getAsString( Key.startTime );
		String	endDate		= taskDef.getAsString( Key.endDate );
		String	endTime		= taskDef.getAsString( Key.endTime );
		Integer	repeat		= taskDef.getAsInteger( Key.repeat );
		String	exclude		= taskDef.getAsString( Key.exclude );
		String	onException	= taskDef.getAsString( Key.onException );
		String	onComplete	= taskDef.getAsString( Key.onComplete );
		String	eventHandler = taskDef.getAsString( Key.eventHandler );
		Integer	retryCount	= taskDef.getAsInteger( Key.retryCount );
		boolean	cluster		= BooleanCaster.cast( taskDef.getOrDefault( Key.cluster, false ) );
		String	url			= taskDef.getAsString( Key.URL );

		// Apply scheduling
		if ( cronTime != null && !cronTime.isBlank() ) {
			task.cron( cronTime );
		} else if ( isDaily || "daily".equalsIgnoreCase( interval ) ) {
			try {
				if ( startTime != null && !startTime.isBlank() ) {
					task.everyDayAt( startTime );
				} else {
					task.everyDay();
				}
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Failed to apply daily scheduling: " + e.getMessage(), e );
			}
		} else if ( interval != null && !interval.isBlank() ) {
			applyInterval( task, interval, startDate, startTime, endDate, endTime );
		}

		// Apply date constraints for non-daily, non-cron schedules
		if ( cronTime == null && !isDaily && !"daily".equalsIgnoreCase( interval ) ) {
			try {
				if ( startDate != null && !startDate.isBlank() ) {
					task.startOn( startDate, startTime != null ? startTime : "00:00" );
				}
				if ( endDate != null && !endDate.isBlank() ) {
					task.endOn( endDate, endTime != null ? endTime : "00:00" );
				}
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Failed to apply date constraints: " + e.getMessage(), e );
			}
		}

		// Apply repeat limit
		if ( repeat != null && repeat > 0 ) {
			final int			maxRuns		= repeat;
			AtomicInteger		runCount	= new AtomicInteger( 0 );
			final ScheduledTask	finalTask	= task;
			final Runnable		original	= callable;
			task.call( () -> {
				original.run();
				if ( runCount.incrementAndGet() >= maxRuns ) {
					finalTask.disable();
				}
			} );
		}

		// Apply exclude dates predicate
		if ( exclude != null && !exclude.isBlank() ) {
			final List<String> excludeList = Arrays.asList( exclude.split( "," ) );
			task.when( t -> !isExcludedDate( t.getNow(), excludeList ) );
		}

		// Wire onFailure callback based on onException
		if ( "pause".equalsIgnoreCase( onException ) ) {
			task.onFailure( ( t, e ) -> t.disable() );
		} else if ( "invokeHandler".equalsIgnoreCase( onException ) && eventHandler != null && !eventHandler.isBlank() ) {
			final String handler = eventHandler;
			task.onFailure( ( t, ex ) -> httpGet( handler, null, runtimeContext ) );
		}

		// Wire after callback for oncomplete
		if ( onComplete != null && !onComplete.isBlank() ) {
			final String completeUrl = onComplete;
			task.after( ( t, result ) -> httpGet( completeUrl, null, runtimeContext ) );
		}

		// Store metadata
		task.setMetaKey( "retryCount", retryCount );
		task.setMetaKey( "cluster", cluster );
		task.setMetaKey( "onException", onException );
		task.setMetaKey( "eventhandler", eventHandler );
		task.setMetaKey( "oncomplete", onComplete );
		task.setMetaKey( "url", url );
	}

	/**
	 * Build a {@link Runnable} that performs an HTTP GET request to the configured URL.
	 * Captures the runtime context (not the request context) for long-lived use.
	 */
	public static Runnable buildTaskCallable( IBoxContext context, IStruct attributes ) {
		String	url				= attributes.getAsString( Key.URL );
		Integer	port			= attributes.getAsInteger( Key.port );
		String	username		= attributes.getAsString( Key.username );
		String	password		= attributes.getAsString( Key.password );
		String	proxyServer		= attributes.getAsString( Key.proxyServer );
		Integer	proxyPort		= attributes.getAsInteger( Key.proxyPort );
		String	proxyUser		= attributes.getAsString( Key.proxyUser );
		String	proxyPass		= attributes.getAsString( Key.proxyPassword );
		boolean	publish			= BooleanCaster.cast( attributes.getOrDefault( Key.publish, false ) );
		String	path			= attributes.getAsString( Key.path );
		String	file			= attributes.getAsString( Key.file );
		boolean	overwrite		= BooleanCaster.cast( attributes.getOrDefault( Key.overwrite, true ) );

		// Capture runtime context — NOT request context (request contexts are recycled per request)
		ortus.boxlang.runtime.BoxRuntime	rt				= ortus.boxlang.runtime.BoxRuntime.getInstance();
		IBoxContext							runtimeContext	= rt.getRuntimeContext();
		HttpService							httpService		= rt.getHttpService();

		return () -> {
			BoxHttpClient client = httpService.getOrBuildClient(
			    BoxHttpClient.HTTP_2,
			    true,
			    null,
			    proxyServer,
			    proxyPort,
			    proxyUser,
			    proxyPass,
			    null,
			    null
			);

			var request = client
			    .newRequest( url, runtimeContext )
			    .method( "GET" )
			    .port( port );

			if ( username != null && !username.isBlank() ) {
				request.withBasicAuth( username, password );
			}

			IStruct response = ( IStruct ) request.send();

			if ( publish && path != null && !path.isBlank() && file != null && !file.isBlank() ) {
				handlePublish( response, path, file, overwrite );
			}
		};
	}

	/**
	 * Write the HTTP response body to a file when publish=true.
	 */
	private static void handlePublish( IStruct response, String path, String file, boolean overwrite ) {
		try {
			String content		= "";
			Object fileContent	= response.get( Key.fileContent );
			if ( fileContent != null ) {
				content = fileContent.toString();
			}
			Path outputPath = Paths.get( path, file );
			if ( overwrite || !Files.exists( outputPath ) ) {
				Files.createDirectories( outputPath.getParent() );
				Files.writeString( outputPath, content );
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to publish schedule output: " + e.getMessage(), e );
		}
	}

	/**
	 * Apply an interval-based schedule to the task.
	 * Handles "once", "weekly", "monthly", and numeric-second intervals.
	 */
	public static void applyInterval( ScheduledTask task, String interval, String startDate, String startTime, String endDate, String endTime ) {
		if ( interval == null || interval.isBlank() ) {
			throw new BoxRuntimeException( "The [interval] attribute is required" );
		}
		try {
			switch ( interval.toLowerCase() ) {
				case "once" : {
					// Schedule to run once using a very large interval; expire after first fire
					task.every( Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS );
					String date = startDate != null ? startDate : task.getNow().toLocalDate().toString();
					String time = startTime != null ? startTime : "00:00";
					task.startOn( date, time );
					task.endOn( date, time.equals( "00:00" ) ? "00:01" : time );
					break;
				}
				case "weekly" : {
					task.everyWeek();
					break;
				}
				case "monthly" : {
					task.everyMonth();
					break;
				}
				default : {
					// Numeric seconds
					long seconds;
					try {
						seconds = Long.parseLong( interval );
					} catch ( NumberFormatException e ) {
						throw new BoxRuntimeException( "Invalid interval value [" + interval + "]. Must be 'once', 'daily', 'weekly', 'monthly', or a number of seconds >= 60." );
					}
					if ( seconds < 60 ) {
						throw new BoxRuntimeException( "Interval in seconds must be >= 60. Got: " + seconds );
					}
					task.every( seconds, TimeUnit.SECONDS );
				}
			}
		} catch ( BoxRuntimeException e ) {
			throw e;
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to apply interval [" + interval + "]: " + e.getMessage(), e );
		}
	}

	/**
	 * Perform an HTTP GET to the given URL using the runtime context.
	 */
	private static void httpGet( String url, Integer port, IBoxContext context ) {
		try {
			HttpService		httpService	= ortus.boxlang.runtime.BoxRuntime.getInstance().getHttpService();
			BoxHttpClient	client		= httpService.getOrBuildClient( BoxHttpClient.HTTP_2, true, null, null, null, null, null, null, null );
			client.newRequest( url, context ).method( "GET" ).port( port ).send();
		} catch ( Exception e ) {
			// Log but don't rethrow — callback failures should not crash the task
			ortus.boxlang.runtime.BoxRuntime.getInstance().getLoggingService().SCHEDULER_LOGGER
			    .warn( "Schedule callback HTTP request failed for URL [{}]: {}", url, e.getMessage() );
		}
	}

	/**
	 * Determine if a datetime falls on an excluded date.
	 */
	private static boolean isExcludedDate( java.time.LocalDateTime now, List<String> excludeList ) {
		String today = now.toLocalDate().toString(); // yyyy-MM-dd
		for ( String entry : excludeList ) {
			String trimmed = entry.trim();
			if ( trimmed.contains( "-" ) && trimmed.split( "-" ).length == 2 && !trimmed.matches( "\\d{4}-\\d{2}-\\d{2}" ) ) {
				// Range like "2024-01-01 to 2024-01-31" — stored simply as two dates
				// Simple check: just skip for now; full range parsing not required for MVP
			}
			if ( today.equals( trimmed ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if a task record should be included based on group filtering.
	 */
	private boolean shouldIncludeTask( TaskRecord record, String group ) {
		if ( group != null && !group.isBlank() && !group.equals( record.group ) ) {
			return false;
		}
		return true;
	}

	/**
	 * Build a struct representation of a task record for list output.
	 */
	private IStruct buildTaskStruct( TaskRecord record ) {
		IStruct meta = record.task.getMeta();
		return Struct.ofNonConcurrent(
		    "name", record.name,
		    "group", record.group,
		    "disabled", record.disabled,
		    "url", meta.getOrDefault( Key.url, "" ),
		    "cronTime", meta.getOrDefault( Key.cronExpression, "" ),
		    "retryCount", meta.getOrDefault( Key.retryCount, 3 ),
		    "scheduledAt", record.scheduledAt,
		    "registeredAt", record.registeredAt,
		    "error", record.error,
		    "errorMessage", record.errorMessage
		);
	}

}
