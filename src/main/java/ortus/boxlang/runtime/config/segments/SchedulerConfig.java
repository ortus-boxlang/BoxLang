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
package ortus.boxlang.runtime.config.segments;

import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This config segment is used to configure the scheduler settings for the BoxLang runtime.
 */
public class SchedulerConfig implements IConfigSegment {

	/**
	 * The name of the global executor that tasks from {@code tasks.json} with no custom scheduler run on, and whose
	 * thread count sizes the private pool of every other scheduler.
	 * Ex: "executor": "scheduled-tasks"
	 * PLEASE REMEMBER TO REGISTER THE EXECUTOR IN THE RUNTIME CONFIGURATION
	 */
	public String	executor						= "scheduled-tasks";

	/**
	 * How clustered occurrences are shared out: {@code eager} (random short delay before claiming) or
	 * {@code balanced} (nodes ranked by published load, busiest waits longest).
	 */
	public String	clusterMode						= "eager";

	/**
	 * The default policy for a task whose previous occurrence is still running: {@code skip} or {@code run}.
	 */
	public String	taskOverlapPolicy				= "skip";

	/**
	 * The name of the cache to use for server fixation and clustering.
	 * Ex: "cache": "default"
	 * PLEASE REMEMBER TO REGISTER THE CACHE IN THE RUNTIME CONFIGURATION
	 */
	/**
	 * @deprecated Server fixation used to take its coordination cache from here; clustering now uses
	 *             {@code cluster.cacheName}. Setting this key still works as a bridge - it enables the cluster
	 *             service and becomes its cache - but it will be removed in a future release.
	 */
	@Deprecated
	public String	cacheName						= "default";

	/**
	 * How long a shutdown waits for in-flight runs to finish on their own before interrupting them.
	 * Set to 0 to skip the graceful wait and interrupt immediately.
	 */
	public long		shutdownGraceSeconds			= 15;

	/**
	 * How long a shutdown waits after interrupting before letting the executor be forced down.
	 * Set to 0 to go straight from the interrupt to the forced shutdown, with no second wait.
	 */
	public long		shutdownInterruptGraceSeconds	= 5;

	/**
	 * The array of schedulers to startup once the runtime starts up
	 */
	public Array	schedulers						= new Array();

	/**
	 * The path to the JSON file where bx:schedule tasks are persisted.
	 * Supports ${boxlang-home} and other placeholder variables.
	 * Defaults to ${boxlang-home}/config/tasks.json.
	 */
	public String	tasksFile						= "${boxlang-home}/config/tasks.json";

	/**
	 * When true, the scheduler service watches tasks.json for external changes and automatically
	 * reloads all affected schedulers whenever the file is modified outside of BoxLang.
	 * Self-writes (from bx:schedule actions) are suppressed via a grace window.
	 * Defaults to false.
	 */
	public Boolean	reloadOnChange					= false;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default empty constructor
	 */
	public SchedulerConfig() {
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		this.executor						= PropertyHelper.processString( config, Key.executor, this.executor );
		this.cacheName						= PropertyHelper.processString( config, Key.cacheName, this.cacheName );
		this.clusterMode					= PropertyHelper.processString( config, Key.clusterMode, this.clusterMode ).toLowerCase();
		this.taskOverlapPolicy				= PropertyHelper.processString( config, Key.taskOverlapPolicy, this.taskOverlapPolicy ).toLowerCase();
		this.shutdownGraceSeconds			= IntegerCaster.cast(
		    config.getOrDefault( Key.shutdownGraceSeconds, this.shutdownGraceSeconds )
		);
		this.shutdownInterruptGraceSeconds	= IntegerCaster.cast(
		    config.getOrDefault( Key.shutdownInterruptGraceSeconds, this.shutdownInterruptGraceSeconds )
		);
		PropertyHelper.processStringOrArrayToArray( config, Key.schedulers, this.schedulers );
		this.tasksFile = PropertyHelper.processString( config, Key.tasksFile, this.tasksFile );
		if ( config.containsKey( Key.reloadOnChange ) ) {
			this.reloadOnChange = BooleanCaster.cast( config.get( Key.reloadOnChange ) );
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.ofNonConcurrent(
		    Key.executor, this.executor,
		    Key.cacheName, this.cacheName,
		    Key.clusterMode, this.clusterMode,
		    Key.taskOverlapPolicy, this.taskOverlapPolicy,
		    Key.shutdownGraceSeconds, this.shutdownGraceSeconds,
		    Key.shutdownInterruptGraceSeconds, this.shutdownInterruptGraceSeconds,
		    Key.schedulers, Array.fromList( this.schedulers ),
		    Key.tasksFile, this.tasksFile,
		    Key.reloadOnChange, this.reloadOnChange
		);
	}

}
