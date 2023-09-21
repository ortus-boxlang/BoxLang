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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.Struct;

/**
 * The runtime configuration for the BoxLang runtime
 */
public class RuntimeConfig {

	/**
	 * A struct of mappings for the runtime
	 */
	@JsonProperty( "mappings" )
	public Struct	mappings			= new Struct();

	/**
	 * The directory where the modules are located by default:
	 * {@code /{user-home}/modules}
	 */
	@JsonProperty( "modulesDirectory" )
	public String	modulesDirectory	= System.getProperty( "user.home" ) + "/modules";

	/**
	 * The cache configurations for the runtime
	 */
	@JsonProperty( "caches" )
	public Struct	caches				= new Struct();

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public RuntimeConfig() {
	}

	/**
	 * @param modulesDirectory the modulesDirectory to set
	 */
	public void setModulesDirectory( String modulesDirectory ) {
		this.modulesDirectory = PlaceholderHelper.resolve( modulesDirectory );
	}

}
