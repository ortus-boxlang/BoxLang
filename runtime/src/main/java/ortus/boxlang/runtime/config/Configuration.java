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

import com.fasterxml.jackson.annotation.JsonProperty;

import ortus.boxlang.runtime.config.segments.CompilerConfig;
import ortus.boxlang.runtime.config.segments.RuntimeConfig;

/**
 * The configuration for the BoxLang runtime and compiler
 */
public class Configuration {

	/**
	 * The compiler configuration, defaulted to the default compiler configuration
	 */
	@JsonProperty( "compiler" )
	public CompilerConfig	compiler	= new CompilerConfig();

	/**
	 * The runtime configuration, defaulted to the default runtime configuration
	 */
	@JsonProperty( "runtime" )
	public RuntimeConfig	runtime		= new RuntimeConfig();

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public Configuration() {
	}
}
