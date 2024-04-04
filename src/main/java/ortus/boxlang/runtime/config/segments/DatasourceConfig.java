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
 * A BoxLang datasource configuration
 */
public class DatasourceConfig {

	/**
	 * The name of the datasource
	 */
	public Key					name;

	/**
	 * The driver shortname for the datasource
	 */
	public Key					driver;

	/**
	 * The properties for the datasource
	 */
	public IStruct				properties	= new Struct( DEFAULTS );

	/**
	 * BoxLang Datasource Defaults
	 */
	public static final IStruct	DEFAULTS	= Struct.of(
	    // The connection string
	    "connectionString", "",
	    // The host
	    "host", "",
	    // The port
	    "port", 0,
	    // The username
	    "username", "",
	    // The password
	    "password", "",
	    // The maximum number of connections
	    "maxConnections", 10,
	    // The minimum number of connections
	    "minConnections", 1,
	    // The maximum number of idle connections
	    "maxIdleConnections", 5,
	    // The maximum number of idle time in milliseconds
	    "maxIdleTime", 60000,
	    // The maximum number of wait time in milliseconds
	    "maxWaitTime", 30000
	);

	/**
	 * Logger
	 */
	private static final Logger	logger		= LoggerFactory.getLogger( DatasourceConfig.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default Empty Constructor
	 */
	public DatasourceConfig() {
		// Default all things
	}

	/**
	 * Constructor by name, driver and properties
	 *
	 * @param name       The key name of the datasource
	 * @param driver     The name of the driver
	 * @param properties The properties of the cache engine
	 */
	public DatasourceConfig( Key name, Key driver, IStruct properties ) {
		this.name		= name;
		this.driver		= driver;
		this.properties	= properties;
	}

	/**
	 * Constructor by name, driver and properties
	 *
	 * @param name   The key name of the datasource
	 * @param driver The name of the driver
	 */
	public DatasourceConfig( Key name, Key driver ) {
		this.name		= name;
		this.driver		= driver;
		this.properties	= new Struct();
	}

	/**
	 * Constructor by name, driver and properties
	 *
	 * @param name The key name of the datasource
	 */
	public DatasourceConfig( Key name ) {
		this.name = name;
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public DatasourceConfig process( IStruct config ) {
		// Name
		if ( config.containsKey( "name" ) ) {
			this.name = Key.of( ( String ) config.get( "name" ) );
		}

		// Driver
		if ( config.containsKey( "driver" ) ) {
			this.driver = Key.of( ( String ) config.get( "driver" ) );
		}

		// Properties
		if ( config.containsKey( "properties" ) ) {
			if ( config.get( "properties" ) instanceof Map<?, ?> castedProps ) {
				processProperties( new Struct( castedProps ) );
			} else {
				logger.warn( "The [runtime.datasources.{}.properties] configuration is not a JSON Object, ignoring it.", this.name );
			}
		}

		return this;
	}

	/**
	 * This processes a struct of properties for a BoxLang datasource
	 *
	 * @param properties The properties to process
	 *
	 * @return the configuration
	 */
	public DatasourceConfig processProperties( IStruct properties ) {
		// Store
		this.properties = properties;

		// Merge defaults
		DEFAULTS
		    .entrySet()
		    .stream()
		    .forEach( entry -> this.properties.putIfAbsent( entry.getKey(), entry.getValue() ) );

		return this;
	}

	/**
	 * Returns the cache configuration as a struct
	 */
	public IStruct toStruct() {
		return Struct.of(
		    "name", this.name.getName(),
		    "driver", this.driver.getName(),
		    "properties", new Struct( this.properties )
		);
	}
}
