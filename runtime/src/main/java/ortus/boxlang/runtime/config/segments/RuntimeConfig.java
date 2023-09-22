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

import java.util.HashMap;
import java.util.Map;

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
	public Struct	mappings			= new Struct();

	/**
	 * The directory where the modules are located by default:
	 * {@code /{user-home}/modules}
	 */
	public String	modulesDirectory	= System.getProperty( "user.home" ) + "/modules";

	/**
	 * The cache configurations for the runtime
	 */
	public Struct	caches				= new Struct();

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public RuntimeConfig() {
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public RuntimeConfig process( Struct config ) {
		if ( config.containsKey( "mappings" ) ) {
			Object mappingsMap = config.get( "mappings" );
			if ( mappingsMap instanceof Map ) {
				this.mappings = new Struct( ( Map<Object, Object> ) mappingsMap );
			}
		}

		if ( config.containsKey( "modulesDirectory" ) ) {
			this.modulesDirectory = PlaceholderHelper.resolve( ( String ) config.get( "modulesDirectory" ) );
		}

		if ( config.containsKey( "caches" ) ) {
			Object cachesMap = config.get( "caches" );
			if ( cachesMap instanceof Map ) {
				new Struct( ( Map<Object, Object> ) cachesMap )
				    .entrySet()
				    .stream()
				    .map( entry -> {
					    Object cacheDefinitionMap = entry.getValue();
					    if ( cacheDefinitionMap instanceof Map ) {
						    return new CacheConfig( entry.getKey() ).process( new Struct( ( Map<Object, Object> ) cacheDefinitionMap ) );
					    }
					    return new CacheConfig( entry.getKey() );
				    } )
				    .forEach( cacheConfig -> {
					    this.caches.put( cacheConfig.name, cacheConfig );
				    } );
			}
		}

		return this;
	}

}
