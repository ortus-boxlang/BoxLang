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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A BoxLang datasource configuration
 */
public class DatasourceConfig implements Comparable<DatasourceConfig> {

	/**
	 * The prefix for all datasource names
	 */
	public static final String		DATASOURCE_PREFIX	= "bx_";

	/**
	 * The prefix for all on the fly datasource names
	 */
	public static final String		ON_THE_FLY_PREFIX	= "onthefly_";

	/**
	 * The name of the datasource
	 */
	public Key						name;

	/**
	 * The driver shortname for the datasource, like <code>mysql</code>, <code>postgresql</code>, etc.
	 */
	public Key						driver;

	/**
	 * Application name : will be empty if not in use
	 */
	public Key						applicationName		= Key._EMPTY;

	/**
	 * Is this a onTheFly datasource
	 */
	public boolean					onTheFly			= false;

	/**
	 * The properties for the datasource
	 */
	public IStruct					properties			= new Struct( DEFAULTS );

	/**
	 * BoxLang Datasource Default configuration values
	 */
	private static final IStruct	DEFAULTS			= Struct.of(
	    // The maximum number of connections.
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
	private static final Logger		logger				= LoggerFactory.getLogger( DatasourceConfig.class );

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
		this.name	= name;
		this.driver	= driver;
		processProperties( properties );
	}

	/**
	 * Constructor by name and properties
	 *
	 * @param name       The key name of the datasource
	 * @param properties The datasource configuration properties.
	 */
	public DatasourceConfig( Key name, IStruct properties ) {
		this( name, Key._EMPTY, properties );
	}

	/**
	 * Constructor by name of a datasource
	 *
	 * @param name The key name of the datasource
	 */
	public DatasourceConfig( Key name ) {
		this( name, new Struct() );
	}

	/**
	 * Constructor by name of a datasource
	 *
	 * @param name The string name of the datasource
	 */
	public DatasourceConfig( String name ) {
		this( Key.of( name ) );
	}

	/**
	 * Construct a datasource configuration from a struct using validation rules
	 *
	 * @param config The configuration struct
	 *
	 * @return The datasource configuration
	 */
	public static DatasourceConfig fromStruct( IStruct config ) {
		// If we dont' have a name, create one
		config.computeIfAbsent( Key._NAME, key -> "unamed_" + UUID.randomUUID().toString() );
		// We need to have a properties struct
		config.computeIfAbsent( Key.properties, key -> new Struct() );
		// We need a driver or throw an exception
		// This is because a driver can have it's properties default automatically if needed.
		if ( !config.containsKey( "driver" ) ) {
			throw new BoxRuntimeException( "Datasource configuration is missing a driver." );
		}
		return new DatasourceConfig().process( config );
	}

	/**
	 * Is this a onTheFly datasource
	 *
	 * @return true if it is on the fly
	 */
	public Boolean isOnTheFly() {
		return this.onTheFly;
	}

	/**
	 * Seeds the ontheFly flag
	 *
	 * @return the datasource configuration
	 */
	public DatasourceConfig onTheFly() {
		this.onTheFly = true;
		return this;
	}

	/**
	 * Seeds the application name
	 *
	 * @param appName The application name
	 *
	 * @return the datasource configuration
	 */
	public DatasourceConfig withAppName( Key appName ) {
		this.applicationName = appName;
		return this;
	}

	/**
	 * Get a unique datasource name which includes a hash of the properties
	 * Following the pattern: <code>bx_{name}_{properties_hash}</code>
	 */
	public Key getUniqueName() {
		StringBuilder uniqueName = new StringBuilder( DATASOURCE_PREFIX );

		// If we have an app name use it
		if ( !applicationName.isEmpty() ) {
			uniqueName.append( applicationName.toString() );
			uniqueName.append( "_" );
		}

		// If this is an on the fly datasource
		if ( onTheFly ) {
			uniqueName.append( ON_THE_FLY_PREFIX );
		}

		// Datasource name
		uniqueName.append( this.name.toString() );
		uniqueName.append( "_" );

		// Hash the properties
		uniqueName.append( properties.hashCode() );

		return Key.of( uniqueName.toString() );
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
	public DatasourceConfig process( IStruct config ) {
		// Name
		if ( config.containsKey( "name" ) ) {
			this.name = Key.of( ( String ) config.get( "name" ) );
		}

		// Name
		if ( config.containsKey( "applicationName" ) ) {
			this.applicationName = Key.of( ( String ) config.get( "applicationName" ) );
		}

		// onTheFly
		if ( config.containsKey( "onTheFly" ) ) {
			this.onTheFly = config.getAsBoolean( Key.of( "onTheFly" ) );
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
		    "applicationName", this.applicationName.getName(),
		    "name", this.name.getName(),
		    "driver", this.driver.getName(),
		    "onTheFly", this.onTheFly,
		    "properties", new Struct( this.properties ),
		    "uniqueName", this.getUniqueName().getName()
		);
	}

	/**
	 * Get's the hashcode according to the uniqueName
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return getUniqueName().hashCode();
	}

	/**
	 * Compares two DatasourceConfig objects
	 *
	 * @param otherConfig The other DatasourceConfig object to compare
	 *
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 */
	@Override
	public int compareTo( DatasourceConfig otherConfig ) {
		return this.getUniqueName().compareTo( otherConfig.getUniqueName() );
	}

	/**
	 * Verifies equality between two DatasourceConfig objects
	 *
	 * @param obj The other object to compare
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || obj.getClass() != this.getClass() ) {
			return false;
		}

		DatasourceConfig other = ( DatasourceConfig ) obj;
		return this.getUniqueName().equals( other.getUniqueName() );
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
	 */
	public HikariConfig toHikariConfig() {
		HikariConfig result = new HikariConfig();
		// Standard Boxlang configuration properties
		result.setJdbcUrl( getOrBuildConnectionString() );

		if ( properties.containsKey( Key.username ) ) {
			result.setUsername( properties.getAsString( Key.username ) );
		}
		if ( properties.containsKey( Key.password ) ) {
			result.setPassword( properties.getAsString( Key.password ) );
		}
		if ( properties.containsKey( Key.connectionTimeout ) ) {
			result.setConnectionTimeout( properties.getAsInteger( Key.connectionTimeout ).longValue() );
		}
		if ( properties.containsKey( Key.minConnections ) ) {
			result.setMinimumIdle( properties.getAsInteger( Key.minConnections ) );
		}
		if ( properties.containsKey( Key.maxConnections ) ) {
			result.setMaximumPoolSize( properties.getAsInteger( Key.maxConnections ) );
		}

		// We also support these HikariConfig-specific properties
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
		// Please use the hikariConfig setters for any hikari-specific properties.
		List<Key> staticConfigKeys = Arrays.asList(
		    Key.host, Key.port, Key.driver, Key.jdbcURL, Key.connectionString, Key.dsn,
		    Key.username, Key.password, Key.autoCommit, Key.connectionTimeout, Key.idleTimeout, Key.keepaliveTime,
		    Key.maxLifetime,
		    Key.connectionTestQuery, Key.minConnections, Key.maxConnections, Key.metricRegistry, Key.healthCheckRegistry, Key.poolName
		); // Add other static config keys here
		properties.forEach( ( key, value ) -> {
			if ( !staticConfigKeys.contains( key ) ) {
				result.addDataSourceProperty( key.getName(), value );
			}
		} );
		return result;
	}

	/**
	 * Retrieve the connection string from the properties, or build it from the driver, host, port, and database properties.
	 *
	 * If any of these properties are found, they will be returned as-is:
	 * <ul>
	 * <li><code>connectionString</code></li>
	 * <li><code>dsn</code></li>
	 * <li><code>jdbcURL</code></li>
	 * </ul>
	 *
	 * If none of these properties are found, the <code>driver</code>, <code>host</code>, <code>port</code>, <code>database</code>, and
	 * <code>custom</code> properties will be used to construct a JDBC URL in the
	 * following format:
	 * <code>jdbc:${driver}://${host}:${port}/${database}?${custom}</code>
	 *
	 * @return JDBC connection string, e.g. <code>jdbc:mysql://localhost:3306/foo?useSSL=false</code>
	 */
	private String getOrBuildConnectionString() {
		// Standard JDBC notation
		if ( properties.containsKey( Key.connectionString ) ) {
			return properties.getAsString( Key.connectionString );
		}

		// CFConfig notation
		if ( properties.containsKey( Key.dsn ) ) {
			return properties.getAsString( Key.dsn );
		}

		// HikariConfig notation
		if ( properties.containsKey( Key.jdbcURL ) ) {
			return properties.getAsString( Key.jdbcURL );
		}

		// Construct from driver, host, port, database, and custom
		if ( properties.containsKey( Key.driver ) && properties.containsKey( Key.host ) && properties.containsKey( Key.port ) ) {
			String	jDriver		= properties.getAsString( Key.driver );
			String	host		= properties.getAsString( Key.host );
			int		port		= properties.getAsInteger( Key.port );
			String	database	= ( String ) properties.getOrDefault( Key.database, "" );
			String	custom		= ( String ) properties.getOrDefault( Key.custom, "" );
			return String.format( "jdbc:%s://%s:%d/%s?%s", jDriver, host, port, database, custom );
		}

		throw new BoxRuntimeException(
		    "Datasource configuration is missing a connection string, and no driver/host/port parameters could be found to construct a JDBC url with." +
		        " Datasource Properties: " + properties.toString()
		);
	}

}
