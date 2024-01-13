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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A BoxLang cache configuration
 */
public class CacheConfig {

	/**
	 * The name of the cache engine
	 */
	public Key					name		= Key.of( "default" );

	/**
	 * The default cache engine type
	 */
	public Key					type		= Key.of( "Caffeine" );

	/**
	 * The properties for the cache engine
	 */
	public IStruct				properties	= new Struct();

	/**
	 * Logger
	 */
	private static final Logger	logger		= LoggerFactory.getLogger( CacheConfig.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The key name of the cache engine
	 */
	public CacheConfig( Key name ) {
		this.name = name;
	}

	/**
	 * Constructor
	 *
	 * @param name The string name of the cache engine
	 */
	public CacheConfig( String name ) {
		this( Key.of( name ) );
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public CacheConfig process( IStruct config ) {
		// Name
		if ( config.containsKey( "name" ) ) {
			this.name = Key.of( ( String ) config.get( "name" ) );
		}

		// Type
		if ( config.containsKey( "type" ) ) {
			this.type = Key.of( ( String ) config.get( "type" ) );
		}

		// Properties
		if ( config.containsKey( "properties" ) ) {
			if ( config.get( "properties" ) instanceof Map<?, ?> castedProps ) {
				this.properties = new Struct( castedProps );
			} else {
				logger.warn( "The [runtime.caches.{}.properties] configuration is not a JSON Object, ignoring it.", this.name );
			}
		}

		return this;
	}

}
