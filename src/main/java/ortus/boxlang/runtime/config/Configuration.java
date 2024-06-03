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
package ortus.boxlang.runtime.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.config.segments.CompilerConfig;
import ortus.boxlang.runtime.config.segments.IConfigSegment;
import ortus.boxlang.runtime.config.segments.RuntimeConfig;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The configuration for the BoxLang runtime and compiler.
 */
public class Configuration implements IConfigSegment {

	/**
	 * The debug mode flag, defaulted to false
	 */
	public Boolean				debugMode	= false;

	/**
	 * The compiler configuration, defaulted to the default compiler configuration
	 */
	public CompilerConfig		compiler	= new CompilerConfig();

	/**
	 * The runtime configuration, defaulted to the default runtime configuration
	 */
	public RuntimeConfig		runtime		= new RuntimeConfig();

	/**
	 * Logger
	 */
	private static final Logger	logger		= LoggerFactory.getLogger( Configuration.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Processes a configuration struct and returns a new configuration object based on the overrides.
	 * Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return The new configuration object based on the core + overrides
	 */
	public Configuration process( IStruct config ) {
		// Debug Mode
		if ( config.containsKey( "debugMode" ) ) {
			this.debugMode = ( Boolean ) config.get( "debugMode" );
		}

		// Compiler
		if ( config.containsKey( "compiler" ) ) {
			if ( config.get( "compiler" ) instanceof Map<?, ?> castedMap ) {
				this.compiler.process( new Struct( castedMap ) );
			} else {
				logger.warn( "The [compiler] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Runtime
		if ( config.containsKey( "runtime" ) ) {
			if ( config.get( "runtime" ) instanceof Map<?, ?> castedMap ) {
				IStruct configStruct = StructCaster.cast( castedMap );
				// move our top-level module settings in to the runtime config
				if ( config.containsKey( "modules" ) ) {
					configStruct.put( Key.modules, StructCaster.cast( config.get( Key.modules ) ) );
				}
				this.runtime.process( configStruct );
			} else {
				logger.warn( "The [runtime] configuration is not a JSON Object, ignoring it." );
			}
		}

		return this;
	}

	/**
	 * Returns the configuration as a struct
	 *
	 * @return A struct representation of the configuration segment
	 */
	public IStruct asStruct() {
		return Struct.of(
		    Key.debugMode, this.debugMode,
		    Key.compiler, this.compiler.asStruct(),
		    Key.runtime, this.runtime.asStruct()
		);
	}
}
