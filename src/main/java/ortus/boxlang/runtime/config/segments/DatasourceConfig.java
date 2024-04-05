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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

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
	private Key						name;

	/**
	 * The driver shortname for the datasource
	 */
	private Key						driver;

	/**
	 * The properties for the datasource
	 */
	private IStruct					properties	= new Struct( DEFAULTS );

	/**
	 * BoxLang Datasource Default configuration values
	 */
	private static final IStruct	DEFAULTS	= Struct.of(
	    // The maximum number of connections. In Lucee, this is the same as connectionLimit
	    "maxConnections", 10,
	    // The minimum number of connections
	    "minConnections", 1,
	    // The maximum number of idle time in milliseconds
	    "maxIdleTime", 60000,
	    // The maximum number of wait time in milliseconds
	    "connectionTimeout", 30000
	);

	/**
	 * Logger
	 */
	private static final Logger		logger		= LoggerFactory.getLogger( DatasourceConfig.class );

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
	 * @param properties The datasource configuration properties.
	 */
	public DatasourceConfig( Key name, Key driver, IStruct properties ) {
		this.name		= name;
		this.driver		= driver;
		this.properties	= properties;
		if ( properties == null ) {
			this.properties = new Struct();
		} else {
			// Apply defaults, overwrite class name/driver if found, etc.
			process( properties );
		}
	}

	/**
	 * Get the datasource name as a Key instance.
	 */
	public Key getName() {
		return name;
	}

	/**
	 * Get a unique datasource name which includes a hash of the properties.
	 */
	public Key getUniqueName() {
		StringBuilder uniqueName = new StringBuilder( "datasource_" );
		if ( this.name != null ) {
			uniqueName.append( this.name.toString() );
		}
		uniqueName.append( "_" );
		uniqueName.append( properties.hashCode() );
		return Key.of( uniqueName.toString() );
	}

	/**
	 * Get the datasource driver name as a Key instance.
	 */
	public Key getDriver() {
		return driver;
	}

	/**
	 * Get the datasource configuration properties.
	 */
	public IStruct getProperties() {
		return properties;
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 * <p>
	 * Note that a <code>name</code> and <code>driver</code> keys in the properties struct will override the class properties.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	private DatasourceConfig process( IStruct config ) {
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
	private DatasourceConfig processProperties( IStruct properties ) {
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

	/**
	 * Build a HikariConfig object from the provided config struct using two main steps:
	 *
	 * <ol>
	 * <li>Configure HikariCP-specific properties, i.e. <code>jdbcUrl</code>, <code>username</code>, <code>password</code>, etc, using the appropriate
	 * setter methods on the HikariConfig object.</li>
	 * <li>Import all other properties as generic DataSource properties. Vendor-specific properties, i.e. for Derby, Oracle, etc, such as
	 * <code>"derby.locks.deadlockTimeout"</code>.</li>
	 * </ul>
	 *
	 * @TODO: Now that we have proper hikariConfig support, consider moving this to a HikariConfigBuilder class which supports CFML-style config property
	 *        names.
	 */
	public HikariConfig toHikariConfig() {
		HikariConfig result = new HikariConfig();
		// Standard CFML/Boxlang configuration properties
		if ( properties.containsKey( Key.connectionString ) ) {
			result.setJdbcUrl( properties.getAsString( Key.connectionString ) );
		}
		if ( properties.containsKey( Key.username ) ) {
			result.setUsername( properties.getAsString( Key.username ) );
		}
		if ( properties.containsKey( Key.password ) ) {
			result.setPassword( properties.getAsString( Key.password ) );
		}
		if ( properties.containsKey( Key.connectionTimeout ) ) {
			result.setConnectionTimeout( properties.getAsLong( Key.connectionTimeout ) );
		}
		if ( properties.containsKey( Key.minConnections ) ) {
			result.setMinimumIdle( properties.getAsInteger( Key.minConnections ) );
		}
		if ( properties.containsKey( Key.maxConnections ) ) {
			result.setMaximumPoolSize( properties.getAsInteger( Key.maxConnections ) );
		}

		// We also support these HikariConfig-specific properties
		if ( properties.containsKey( Key.jdbcURL ) ) {
			result.setJdbcUrl( properties.getAsString( Key.jdbcURL ) );
		}
		if ( properties.containsKey( Key.autoCommit ) ) {
			result.setAutoCommit( properties.getAsBoolean( Key.autoCommit ) );
		}
		if ( properties.containsKey( Key.idleTimeout ) ) {
			result.setIdleTimeout( properties.getAsLong( Key.idleTimeout ) );
		}
		if ( properties.containsKey( Key.keepaliveTime ) ) {
			result.setKeepaliveTime( properties.getAsLong( Key.keepaliveTime ) );
		}
		if ( properties.containsKey( Key.maxLifetime ) ) {
			result.setMaxLifetime( properties.getAsLong( Key.maxLifetime ) );
		}
		if ( properties.containsKey( Key.connectionTestQuery ) ) {
			result.setConnectionTestQuery( properties.getAsString( Key.connectionTestQuery ) );
		}
		if ( properties.containsKey( Key.metricRegistry ) ) {
			result.setMetricRegistry( properties.getAsString( Key.metricRegistry ) );
		}
		if ( properties.containsKey( Key.healthCheckRegistry ) ) {
			result.setHealthCheckRegistry( properties.getAsString( Key.healthCheckRegistry ) );
		}
		if ( properties.containsKey( Key.poolName ) ) {
			result.setPoolName( properties.getAsString( Key.poolName ) );
		}

		// List of keys to NOT set dynamically. All keys not in this list will use `addDataSourceProperty` to set the property and pass it to the JDBC driver.
		List<Key> staticConfigKeys = Arrays.asList(
		    Key.jdbcURL, Key.username, Key.password, Key.autoCommit, Key.connectionTimeout, Key.idleTimeout, Key.keepaliveTime, Key.maxLifetime,
		    Key.connectionTestQuery, Key.minConnections, Key.maxConnections, Key.metricRegistry, Key.healthCheckRegistry, Key.poolName
		); // Add other static config keys here
		properties.forEach( ( key, value ) -> {
			if ( !staticConfigKeys.contains( key ) ) {
				result.addDataSourceProperty( key.getName(), value );
			}
		} );
		return result;
	}
}
