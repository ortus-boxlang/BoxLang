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

import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A BoxLang Executor Configuration Segment
 * <p>
 * This segment is used to define an executor configuration for the BoxLang runtime.
 */
public class ExecutorConfig {

	/**
	 * The name of the executor
	 */
	public String	name;

	/**
	 * The type of the executor. Fixed is the default
	 */
	public String	type		= "FIXED";

	/**
	 * The number of threads, if any
	 */
	public int		maxThreads	= AsyncService.DEFAULT_MAX_THREADS;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default Empty Constructor
	 */
	public ExecutorConfig() {
		// Default all things
	}

	/**
	 * Constructor by name and type
	 *
	 * @param name The key name of the executor
	 */
	public ExecutorConfig( String name ) {
		this.name = name;
	}

	/**
	 * Constructor by name and type
	 *
	 * @param name The name key of the executor
	 */
	public ExecutorConfig( Key name ) {
		this.name = name.getName();
		;
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public ExecutorConfig process( IStruct config ) {
		if ( config.containsKey( "name" ) ) {
			this.name = PlaceholderHelper.resolve( config.get( "name" ) );
		}

		if ( config.containsKey( "type" ) ) {
			this.type = PlaceholderHelper.resolve( config.get( "type" ) ).toUpperCase();
		}

		if ( config.containsKey( "maxThreads" ) ) {
			this.maxThreads = IntegerCaster.cast( PlaceholderHelper.resolve( config.get( "maxThreads" ) ) );
		}

		return this;
	}

	/**
	 * Returns the configuration as a struct
	 * Remember that this is what the context's use to build runtime/request configs, so don't use any references
	 */
	public IStruct toStruct() {
		return Struct.of(
		    "name", this.name,
		    "type", this.type,
		    "maxThreads", this.maxThreads
		);
	}

}
