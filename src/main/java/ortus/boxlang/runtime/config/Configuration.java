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
import ortus.boxlang.runtime.config.segments.RuntimeConfig;
import ortus.boxlang.runtime.types.Struct;

/**
 * The configuration for the BoxLang runtime and compiler.
 */
public class Configuration {

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
	 * Processes the configuration struct.
	 * Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	@SuppressWarnings( "unchecked" )
	public Configuration process( Struct config ) {
		// Compiler
		if ( config.containsKey( "compiler" ) ) {
			Object compilerMap = config.get( "compiler" );
			if ( compilerMap instanceof Map ) {
				this.compiler.process( new Struct( ( Map<Object, Object> ) compilerMap ) );
			} else {
				logger.warn( "The [compiler] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Runtime
		if ( config.containsKey( "runtime" ) ) {
			Object runtimeMap = config.get( "runtime" );
			if ( runtimeMap instanceof Map ) {
				this.runtime.process( new Struct( ( Map<Object, Object> ) runtimeMap ) );
			} else {
				logger.warn( "The [runtime] configuration is not a JSON Object, ignoring it." );
			}
		}

		return this;
	}
}
