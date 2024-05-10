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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.StructUtil;

/**
 * A BoxLang datasource configuration.
 *
 * <p>
 * Inside a boxlang.json configuration file, a single datasource configuration might look something like this:
 *
 * <pre>
 * "myMysql": {
		"driver": "mysql",
		"properties": {
			"host": "${env.MYSQL_HOST:localhost}",
			"port": "${env.MYSQL_PORT:3306}",
			"database": "${env.MYSQL_DATABASE:myDB}",
			"username": "${env.MYSQL_USERNAME}",
			"password": "${env.MYSQL_PASSWORD}"
		}
	}
 * </pre>
 */
public class DatasourceConfig implements Comparable<DatasourceConfig> {

	/**
	 * The prefix for all datasource names
	 */
	public static final String		DATASOURCE_PREFIX				= "bx_";

	/**
	 * The prefix for all on the fly datasource names
	 */
	public static final String		ON_THE_FLY_PREFIX				= "onthefly_";

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
	public Key						applicationName					= Key._EMPTY;

	/**
	 * Is this a onTheFly datasource
	 */
	public boolean					onTheFly						= false;

	/**
	 * The properties for the datasource
	 */
	public IStruct					properties						= new Struct( DEFAULTS );

	/**
	 * BoxLang Datasource Default configuration values
	 * These are applied to ALL datasources
	 * Please note that most of them rely on Hikari defaults
	 * <p>
	 * References
	 * https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
	 */
	private static final IStruct	DEFAULTS						= Struct.of(
	    // The maximum number of connections.
	    // Hikari: maximumPoolSize
	    "maxConnections", 10,
	    // The minimum number of connections
	    // Hikari: minimumIdle
	    "minConnections", 10,
	    // The maximum number of idle time in milliseconds ( 1 Minute )
	    "maxIdleTime", 60000,
	    // The maximum number of wait time in milliseconds ( 30 Seconds )
	    "connectionTimeout", 30000,
	    // The maximum number of idle time in milliseconds ( 10 Minutes )
	    "idleTimeout", 60000,
	    // This property controls the maximum lifetime of a connection in the pool.
	    // An in-use connection will never be retired, only when it is closed will it then be removed
	    // We strongly recommend setting this value, and it should be several seconds shorter than any database
	    // or infrastructure imposed connection time limit
	    // 30 minutes by default
	    "maxLifetime", 1800000,
	    // This property controls how frequently HikariCP will attempt to keep a connection alive, in order to prevent it from being timed out by the database
	    // or network infrastructure
	    // This value must be less than the maxLifetime value. A "keepalive" will only occur on an idle connectionThis value must be less than the maxLifetime
	    // value. A "keepalive" will only occur on an idle connection ( 10 Minutes )
	    "keepaliveTime", 600000,
	    // The default auto-commit state of connections created by this pool
	    "autoCommit", true,
	    // Register mbeans or not. By default, this is false
	    // However, if you are using JMX, you can set this to true to get some additional monitoring information
	    "registerMbeans", false,
	    // Prep the custom properties
	    "custom", new Struct()
	);

	// List of keys to NOT set dynamically. All keys not in this list will use `addDataSourceProperty` to set the property and pass it to the JDBC driver.
	// Please use the hikariConfig setters for any hikari-specific properties.
	private List<Key>				RESERVED_CONNECTION_PROPERTIES	= List.of(
	    Key.autoCommit,
	    Key.connectionString,
	    Key.connectionTestQuery,
	    Key.connectionTimeout,
	    Key.driver,
	    Key.dsn,
	    Key.healthCheckRegistry,
	    Key.host,
	    Key.idleTimeout,
	    Key.jdbcURL,
	    Key.keepaliveTime,
	    Key.maxConnections,
	    Key.maxLifetime,
	    Key.metricRegistry,
	    Key.minConnections,
	    Key.password,
	    Key.poolName,
	    Key.port,
	    Key.username
	);

	/**
	 * Logger
	 */
	private static final Logger		logger							= LoggerFactory.getLogger( DatasourceConfig.class );

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

		this.properties.entrySet().stream().forEach( entry -> {
			// Allow environment variable substitution in string values
			if ( entry.getValue() instanceof String castedValue ) {
				this.properties.put( entry.getKey(), PlaceholderHelper.resolve( castedValue ) );
			}
		} );

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
	 * Build a HikariConfig object from the datasource properties configuration.
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
		DatasourceService	datasourceService	= BoxRuntime.getInstance().getDataSourceService();
		HikariConfig		result				= new HikariConfig();
		// If we can't find the driver, we default to the generic driver
		IJDBCDriver			driver				= datasourceService.hasDriver( this.driver ) ? datasourceService.getDriver( this.driver )
		    : datasourceService.getGenericDriver();

		// Incorporate the driver's default properties
		driver.getDefaultProperties().entrySet().stream().forEach( entry -> this.properties.putIfAbsent( entry.getKey(), entry.getValue() ) );

		// Make sure the `custom` property is a struct: Normalize it
		if ( this.properties.get( Key.custom ) instanceof String castedCustomParams ) {
			this.properties.put( Key.custom, StructUtil.fromQueryString( castedCustomParams ) );
		}

		// Build out the JDBC URL according to the driver chosen or url chosen
		result.setJdbcUrl( getOrBuildConnectionString( driver ) );

		// Standard Boxlang configuration properties into hikari equivalents
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

		// Hikari doesn't use a driver, but if present use it
		// This is mostly for legacy support
		if ( properties.containsKey( Key._CLASS ) ) {
			result.setDriverClassName( properties.getAsString( Key._CLASS ) );
		}

		// We also support these HikariConfig-specific properties
		if ( properties.containsKey( Key.autoCommit ) ) {
			result.setAutoCommit( properties.getAsBoolean( Key.autoCommit ) );
		}
		if ( properties.containsKey( Key.idleTimeout ) ) {
			result.setIdleTimeout( properties.getAsInteger( Key.idleTimeout ).longValue() );
		}
		if ( properties.containsKey( Key.keepaliveTime ) ) {
			result.setKeepaliveTime( properties.getAsInteger( Key.keepaliveTime ).longValue() );
		}
		if ( properties.containsKey( Key.maxLifetime ) ) {
			result.setMaxLifetime( properties.getAsInteger( Key.maxLifetime ).longValue() );
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

		// ADD NON-RESERVED PROPERTIES
		// as Hikari properties
		properties.entrySet().stream()
		    .filter( entry -> !RESERVED_CONNECTION_PROPERTIES.contains( entry.getKey() ) )
		    .forEach( entry -> result.addDataSourceProperty( entry.getKey().getName(), entry.getValue() ) );

		return result;
	}

	/**
	 * Retrieve the connection string from the properties, or build it from the appropriate driver.
	 *
	 * If any of these properties are found, they will be returned as-is, in the following order:
	 * <ul>
	 * <li><code>connectionString</code></li>
	 * <li><code>dsn</code></li>
	 * <li><code>URL</code></li>
	 * <li><code>jdbcURL</code></li>
	 * </ul>
	 *
	 * If none of these properties are found then we delegate to a registered driver in the
	 * datasource service. If none, can be found, we use the generic JDBC Driver.
	 *
	 * @param driver The JDBC driver to use
	 *
	 * @return JDBC connection string, e.g. <code>jdbc:mysql://localhost:3306/foo?useSSL=false</code>
	 */
	private String getOrBuildConnectionString( IJDBCDriver driver ) {
		String connectionString = "";

		// If we have a connection string, use it without asking the driver
		// We are overriding the driver's connection string

		// Standard JDBC notation: (connectionString)
		if ( properties.containsKey( Key.connectionString ) && properties.getAsString( Key.connectionString ).length() > 0 ) {
			connectionString = addCustomParams( properties.getAsString( Key.connectionString ) );
		}
		// CFConfig notation (dsn)
		else if ( properties.containsKey( Key.dsn ) && properties.getAsString( Key.dsn ).length() > 0 ) {
			connectionString = addCustomParams( properties.getAsString( Key.dsn ) );
		}
		// Adobe CF notation (url)
		else if ( properties.containsKey( Key.URL ) && properties.getAsString( Key.URL ).length() > 0 ) {
			connectionString = addCustomParams( properties.getAsString( Key.URL ) );
		}
		// HikariConfig notation
		else if ( properties.containsKey( Key.jdbcURL ) ) {
			connectionString = addCustomParams( properties.getAsString( Key.jdbcURL ) );
		}
		// Verify if we have a registered driver. Which needs to match
		// the driver name in the module. ex: `mysql`, `postgresql`, etc.
		else {
			connectionString = driver.buildConnectionURL( this );
		}

		// Incorporate Placeholders : Just in case
		connectionString = replaceConnectionPlaceholders( connectionString );

		// Default it to the Generic JDBC Driver
		return connectionString;
	}

	/**
	 * This method is used to incorporate custom parameters into the target connection string.
	 *
	 * @param target    The target connection string
	 * @param delimiter The delimiter to use
	 *
	 * @return The connection string with custom parameters incorporated
	 */
	public String addCustomParams( String target, String delimiter ) {
		String targetCustom = "";
		if ( this.properties.get( Key.custom ) instanceof String castedCustom ) {
			targetCustom = castedCustom;
		} else {
			targetCustom = StructUtil.toQueryString( ( IStruct ) this.properties.get( Key.custom ), delimiter );
		}

		if ( targetCustom.length() > 0 ) {
			// If the target connection string already has parameters, append an ampersand
			if ( target.contains( "?" ) && !target.endsWith( "?" ) ) {
				target += delimiter;
			} else if ( !target.contains( "?" ) ) {
				target += "?";
			}

			// Append the custom parameters
			target += targetCustom;
		}

		return target;
	}

	/**
	 * This method is used to incorporate custom parameters into the target connection string.
	 * Using the default {@code &} delimiter.
	 *
	 * @param target The target connection string
	 *
	 * @return The connection string with custom parameters incorporated
	 */
	public String addCustomParams( String target ) {
		return addCustomParams( target, "&" );
	}

	/**
	 * This method is used to replace placeholders in the connection string with the appropriate values.
	 * <p>
	 * The placeholders are:
	 * <ul>
	 * <li><code>{host}</code> - The host name</li>
	 * <li><code>{port}</code> - The port number</li>
	 * <li><code>{database}</code> - The database name</li>
	 * </ul>
	 *
	 * @param target The target connection string
	 *
	 * @return The connection string with placeholders replaced
	 */
	private String replaceConnectionPlaceholders( String target ) {
		// Replace placeholders
		target	= target.replace( "{host}", ( String ) this.properties.getOrDefault( Key.host, "NOT_FOUND" ) );
		target	= target.replace( "{port}",
		    StringCaster.cast( this.properties.getOrDefault( Key.port, 0 ), true )
		);
		target	= target.replace( "{database}", ( String ) this.properties.getOrDefault( Key.database, "NOT_FOUND" ) );

		return target;
	}

}
