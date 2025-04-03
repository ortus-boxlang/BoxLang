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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This config segment is used to configure the scheduler settings for the BoxLang runtime.
 */
public class SchedulerConfig implements IConfigSegment {

	/**
	 * The name of the executor to use for scheduled tasks.
	 * This is used to determine which executor to use for scheduling tasks.
	 * Ex: "executor": "scheduled-tasks"
	 * PLEASE REMEMBER TO REGISTER THE EXECUTOR IN THE RUNTIME CONFIGURATION
	 */
	public String	executor	= "scheduled-tasks";

	/**
	 * The name of the cache to use for server fixation and clustering.
	 * Ex: "cache": "default"
	 * PLEASE REMEMBER TO REGISTER THE CACHE IN THE RUNTIME CONFIGURATION
	 */
	public String	cacheName	= "default";

	/**
	 * The array of schedulers to startup once the runtime starts up
	 */
	public Array	schedulers	= new Array();

	/**
	 * The array of tasks to startup once the runtime starts up
	 */
	public Array	tasks		= new Array();

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
		PropertyHelper.processString( config, Key.executor, this.executor );
		PropertyHelper.processString( config, Key.cacheName, this.cacheName );
		PropertyHelper.processStringOrArrayToArray( config, Key.schedulers, this.schedulers );
		// TODO: Process tasks on next final release

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.of(
		    Key.executor, this.executor,
		    Key.cacheName, this.cacheName,
		    Key.schedulers, Array.fromList( this.schedulers ),
		    Key.tasks, this.tasks
		);
	}

}
