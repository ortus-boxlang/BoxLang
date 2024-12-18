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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
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
		"host": "${env.MYSQL_HOST:localhost}",
		"port": "${env.MYSQL_PORT:3306}",
		"database": "${env.MYSQL_DATABASE:myDB}",
		"username": "${env.MYSQL_USERNAME}",
		"password": "${env.MYSQL_PASSWORD}"
	}
 * </pre>
 */
public class DatasourceConfig implements Comparable<DatasourceConfig>, IConfigSegment {

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
	 * PRIVATE PROPERTIES
	 */

	/**
	 * This is used to determine the connection string from the properties
	 * dynamically depending on how the code was used. This is used to support
	 * older ways of doing things
	 */
	private static final List<Key>	CONNECTION_STRING_KEYS			= List.of(
	    // Standard JDBC notation: (connectionString)
	    Key.connectionString,
	    // Adobe CF notation (url)
	    Key.URL,
	    // HikariConfig notation
	    Key.jdbcURL
	);

	/**
	 * BoxLang Datasource Default configuration values
	 * These are applied to ALL datasources
	 * Please note that most of them rely on Hikari defaults but we use seconds in BoxLang to match CFConfig standards
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
	    // Maximum time to wait for a successful connection, in seconds ( 1 Second )
	    "connectionTimeout", 1,
	    // The maximum number of idle time in seconds ( 10 Minutes = 600 )
	    // • Refers to the maximum amount of time a connection can remain idle in the pool before it is eligible for eviction.
	    // •If a connection is idle for longer than this time, it can be closed and removed from the pool, helping to free up resources.
	    // • This setting only affects connections that are not in use and have exceeded the idle duration specified.
	    // In Seconds
	    "idleTimeout", 600,
	    // This property controls the maximum lifetime of a connection in the pool.
	    // An in-use connection will never be retired, only when it is closed will it then be removed
	    // We strongly recommend setting this value, and it should be several seconds shorter than any database
	    // or infrastructure imposed connection time limit
	    // 30 minutes by default = 1800000ms = 1800 seconds
	    // In Seconds
	    "maxLifetime", 1800,
	    // This property controls how frequently HikariCP will attempt to keep a connection alive, in order to prevent it from being timed out by the database
	    // or network infrastructure
	    // This value must be less than the maxLifetime value. A "keepalive" will only occur on an idle connectionThis value must be less than the maxLifetime
	    // value. A "keepalive" will only occur on an idle connection ( 10 Minutes = 600 seconds = 600,000 ms )
	    // In Seconds
	    "keepaliveTime", 600,
	    // The default auto-commit state of connections created by this pool
	    "autoCommit", true,
	    // Register mbeans or not. By default, this is true
	    // However, if you are using JMX, you can set this to true to get some additional monitoring information
	    "registerMbeans", true,
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
	 * Constructor by a string name and properties
	 *
	 * @param name       The unique name of the datasource
	 * @param properties The datasource configuration properties.
	 */
	public DatasourceConfig( String name, IStruct properties ) {
		this( Key.of( name ), properties );
	}

	/**
	 * Constructor by name and properties
	 *
	 * @param name       The unique name of the datasource
	 * @param properties The datasource configuration properties.
	 */
	public DatasourceConfig( Key name, IStruct properties ) {
		this.name = name;
		processProperties( properties );
	}

	/**
	 * Constructor an unnamed datasource with properties.
	 * This usually happens when you are creating a datasource on the fly.
	 * The pre-generated name of the form: <code>unnamed_{randomUUID}</code>
	 *
	 * @param properties The datasource configuration properties.
	 */
	public DatasourceConfig( IStruct properties ) {
		processProperties( properties );
		this.name = Key.of( "unnamed_" + UUID.randomUUID().toString() );
	}

	/**
	 * Constructor by name of a datasource
	 *
	 * @param name The key name of the datasource
	 */
	public DatasourceConfig( Key name ) {
		this.name = name;
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
	 * Discover the driver type from a JDBC URL.
	 *
	 * @param jdbcURL The JDBC URL
	 *
	 * @return The driver name or an empty string if not found
	 */
	public static String discoverDriverFromJdbcUrl( String jdbcURL ) {
		logger.debug( "Attempting to determine driver from JDBC URL: {}", jdbcURL );

		// check that the URL is not empty, that it has at least one : and that it starts with jdbc:
		if ( jdbcURL == null || jdbcURL.isEmpty() || !jdbcURL.contains( ":" ) || !jdbcURL.startsWith( "jdbc:" ) ) {
			return "";
		}

		// extract the driver name from the URL
		String parsedDriver = jdbcURL.split( ":" )[ 1 ];
		logger.debug( "Parsed {} driver from {}", parsedDriver, jdbcURL );
		return parsedDriver;
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
	public DatasourceConfig setOnTheFly() {
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
	 * Get the original name of the datasource - this is NOT unique and should not be used for identification.
	 *
	 * @return The original name of the datasource.
	 */
	public String getOriginalName() {
		return this.name.getName();
	}

	/**
	 * Processes the state of the configuration segment from the configuration struct.
	 * <p>
	 * Each segment is processed individually from the initial configuration struct.
	 * This is so we can handle cascading overrides from configuration loading.
	 * <p>
	 *
	 * @param config The state of the segment as a struct
	 *
	 * @return Return itself for chaining
	 */
	public DatasourceConfig process( IStruct config ) {
		// Name Override
		if ( config.containsKey( "name" ) ) {
			this.name = Key.of( ( String ) config.get( "name" ) );
		}

		// App name Override
		if ( config.containsKey( "applicationName" ) ) {
			this.applicationName = Key.of( ( String ) config.get( "applicationName" ) );
		}

		// onTheFly Override
		if ( config.containsKey( "onTheFly" ) ) {
			this.onTheFly = config.getAsBoolean( Key.of( "onTheFly" ) );
		}

		// Process the properties into the state
		if ( config.containsKey( "properties" ) && config.get( "properties" ) instanceof Map<?, ?> castedProperties ) {
			processProperties( new Struct( castedProperties ) );
		} else {
			// process the main config body as the properties
			processProperties( config );
		}

		return this;
	}

	/**
	 * This method is used to process the properties of a datasource configuration.
	 *
	 * @param properties The datasource configuration properties.
	 *
	 * @return The datasource configuration object
	 */
	public DatasourceConfig processProperties( IStruct properties ) {
		// Process the properties into the state, merge them in one by one
		properties.entrySet().stream().forEach( entry -> {
			if ( entry.getValue() instanceof String castedValue ) {
				this.properties.put( entry.getKey(), PlaceholderHelper.resolve( castedValue ) );
			} else {
				this.properties.put( entry.getKey(), entry.getValue() );
			}
		} );

		// Merge defaults into the properties
		DEFAULTS
		    .entrySet()
		    .stream()
		    .forEach( entry -> this.properties.putIfAbsent( entry.getKey(), entry.getValue() ) );

		// DBDriver Alias for CFConfig
		if ( this.properties.containsKey( Key.dbdriver ) ) {
			this.properties.computeIfAbsent( Key.driver, key -> this.properties.get( Key.dbdriver ) );
		}

		// Type Driver Alias: Adobe
		if ( this.properties.containsKey( Key.type ) ) {
			this.properties.computeIfAbsent( Key.driver, key -> this.properties.get( Key.type ) );
		}

		// Driver Alias
		String driver = this.properties.getOrDefault( Key.driver, "" ).toString();

		// if no driver/type set, attempt to determine from the JDBC connection string.
		if ( driver.isBlank() ) {
			var connectionString = getConnectionString();
			// From connection string or DSN (CFConfig)
			if ( !connectionString.isBlank() ) {
				this.properties.put( Key.driver, discoverDriverFromJdbcUrl( connectionString ) );
			} else if ( this.properties.containsKey( Key.dsn ) ) {
				this.properties.put( Key.driver, discoverDriverFromJdbcUrl( this.properties.getAsString( Key.dsn ) ) );
			}
		}

		return this;
	}

	/**
	 * Helper method, tries to get the driver from the properties
	 *
	 * @return The driver name or an empty string if not found
	 */
	public Key getDriver() {
		return Key.of( this.properties.getOrDefault( Key.driver, "" ).toString() );
	}

	/**
	 * Returns the configuration as a struct
	 *
	 * @return A struct representation of the configuration segment
	 */
	public IStruct asStruct() {
		return Struct.of(
		    "applicationName", this.applicationName.getName(),
		    "name", this.name.getName(),
		    "onTheFly", this.onTheFly,
		    "uniqueName", this.getUniqueName().getName(),
		    "properties", new Struct( this.properties )
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

		// At this point, if no driver can be determined from the 'driver' or 'type' keys or JDBC url key(s),
		// we need to throw an exception because we can't proceed.
		if ( this.properties.getOrDefault( Key.driver, "" ).toString().isBlank() ) {
			throw new IllegalArgumentException( "Datasource configuration must contain a 'driver', or a valid JDBC connection string in 'url'." );
		}

		DatasourceService	datasourceService	= BoxRuntime.getInstance().getDataSourceService();
		HikariConfig		result				= new HikariConfig();
		// If we can't find the driver, we default to the generic driver
		IJDBCDriver			driverOrDefault		= datasourceService.hasDriver( getDriver() ) ? datasourceService.getDriver( getDriver() )
		    : datasourceService.getGenericDriver();

		// Incorporate the driver's default properties
		driverOrDefault.getDefaultProperties().entrySet().stream().forEach( entry -> this.properties.putIfAbsent( entry.getKey(), entry.getValue() ) );

		// Make sure the `custom` property is a struct: Normalize it
		if ( this.properties.get( Key.custom ) instanceof String castedCustomParams ) {
			this.properties.put( Key.custom, StructUtil.fromQueryString( castedCustomParams ) );
		}
		// Incorporate the driver's default 'custom' properties
		IStruct customParams = this.properties.getAsStruct( Key.custom );
		driverOrDefault.getDefaultCustomParams().entrySet().stream().forEach( entry -> customParams.putIfAbsent( entry.getKey(), entry.getValue() ) );
		this.properties.put( Key.custom, customParams );

		// Build out the JDBC URL according to the driver chosen or url chosen
		result.setJdbcUrl( getOrBuildConnectionString( driverOrDefault ) );

		// Standard Boxlang configuration properties into hikari equivalents
		if ( properties.containsKey( Key.username ) ) {
			result.setUsername( properties.getAsString( Key.username ) );
		}
		if ( properties.containsKey( Key.password ) ) {
			result.setPassword( properties.getAsString( Key.password ) );
		}
		// Connection timeouts in seconds, but Hikari uses milliseconds
		if ( properties.containsKey( Key.connectionTimeout ) ) {
			result.setConnectionTimeout(
			    LongCaster.cast( properties.get( Key.connectionTimeout ), false ) * 1000
			);
		}
		if ( properties.containsKey( Key.minConnections ) ) {
			result.setMinimumIdle( IntegerCaster.cast( properties.get( Key.minConnections ), false ) );
		}
		if ( properties.containsKey( Key.maxConnections ) ) {
			result.setMaximumPoolSize( IntegerCaster.cast( properties.get( Key.maxConnections ), false ) );
		}

		// We also support these HikariConfig-specific properties
		if ( properties.containsKey( Key.autoCommit ) ) {
			result.setAutoCommit( properties.getAsBoolean( Key.autoCommit ) );
		}
		if ( properties.containsKey( Key.idleTimeout ) ) {
			result.setIdleTimeout(
			    LongCaster.cast( properties.get( Key.idleTimeout ), false ) * 1000
			);
		}
		if ( properties.containsKey( Key.keepaliveTime ) ) {
			result.setKeepaliveTime(
			    LongCaster.cast( properties.get( Key.keepaliveTime ), false ) * 1000
			);
		}
		if ( properties.containsKey( Key.maxLifetime ) ) {
			result.setMaxLifetime(
			    LongCaster.cast( properties.get( Key.maxLifetime ), false ) * 1000
			);
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
	 * Retrieve the connection string from the properties, or build it from the appropriate driver module.
	 *
	 * If any of these properties are found, they will be returned as-is, in the following order:
	 * <ul>
	 * <li><code>connectionString</code></li>
	 * <li><code>URL</code></li>
	 * <li><code>jdbcURL</code></li>
	 * <li><code>dsn</code> - Special case used on placeholder replacements</li>
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
		// 1. Attempt to find the connection string from the properties first.
		String connectionString = replaceConnectionPlaceholders( getConnectionString() );

		// 2. If the attempt was empty, then try to find the connection string from the DSN cfconfig element
		if ( connectionString.isEmpty() && this.properties.containsKey( Key.dsn ) ) {
			connectionString = replaceConnectionPlaceholders( this.properties.getAsString( Key.dsn ) );
		}

		// 3. If the attempt was empty, then try to build the connection string from the driver
		// This adds all the placeholders and custom parameters via the driver
		if ( connectionString.isEmpty() ) {
			connectionString = replaceConnectionPlaceholders( driver.buildConnectionURL( this ) );
		} else {
			connectionString = addCustomParams( connectionString, driver.getDefaultURIDelimiter(), driver.getDefaultDelimiter() );
		}

		// Finalize with custom params
		return connectionString;
	}

	/**
	 * This method is used to incorporate custom parameters into the target connection string.
	 *
	 * @param target       The target connection string
	 * @param URIDelimiter The URI delimiter to use
	 * @param delimiter    The delimiter to use for the custom parameters
	 *
	 * @return The connection string with custom parameters incorporated
	 */
	public String addCustomParams( String target, String URIDelimiter, String delimiter ) {
		String targetCustom = "";
		if ( this.properties.get( Key.custom ) instanceof String castedCustom ) {
			targetCustom = castedCustom;
		} else {
			targetCustom = StructUtil.toQueryString( ( IStruct ) this.properties.get( Key.custom ), delimiter );
		}

		// Append the custom parameters
		if ( targetCustom.length() > 0 ) {
			// Incorporate URI Delimiter if it doesn't exist
			if ( !target.contains( URIDelimiter ) ) {
				target += URIDelimiter;
			}
			// Now add the custom parameters
			target += targetCustom;
		}

		return target;
	}

	/**
	 * This method is used to incorporate custom parameters into the target connection string
	 * Using default delimiters of <code>?</code> and <code>&amp;</code>
	 *
	 * @param target The target connection string
	 *
	 * @return The connection string with custom parameters incorporated
	 */
	public String addCustomParams( String target ) {
		return addCustomParams( target, "?", "&" );
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
		// Short circuit if the target is empty
		if ( target.isBlank() ) {
			return target;
		}

		// Replace placeholders
		target	= target.replace(
		    "{host}",
		    StringCaster.cast( this.properties.getOrDefault( Key.host, "NOT_FOUND" ), true )
		);
		target	= target.replace(
		    "{port}",
		    StringCaster.cast( this.properties.getOrDefault( Key.port, 0 ), true )
		);
		target	= target.replace(
		    "{database}",
		    StringCaster.cast( this.properties.getOrDefault( Key.database, "NOT_FOUND" ), true )
		);

		return target;
	}

	/**
	 * Determine the connection string from the properties using our supported keys.
	 *
	 * @return The connection string or an empty string if none is found
	 */
	private String getConnectionString() {
		return CONNECTION_STRING_KEYS
		    .stream()
		    .filter( key -> this.properties.containsKey( key ) && !this.properties.getAsString( key ).isBlank() )
		    .findFirst()
		    .map( key -> this.properties.getAsString( key ) )
		    .orElse( "" );
	}

}
