/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The datasource manager which stores a registry of configured datasources.
 * <p>
 * Each datasource is a connection pool (or potential connection pool) to a database.
 * <p>
 * The datasource manager can look up datasources by name or by configuration. If by name, the datasource name will be prefixed with the application
 * name or web server name, if those exist.
 */
public class DatasourceService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger		logger		= LoggerFactory.getLogger( DatasourceService.class );

	/**
	 * Map of datasources registered with the service.
	 */
	private Map<Key, DataSource>	datasources	= new ConcurrentHashMap<>();

	/**
	 * Map of JDBC drivers registered with the service.
	 */
	private Map<Key, IJDBCDriver>	jdbcDrivers	= new HashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The BoxRuntime
	 */
	public DatasourceService( BoxRuntime runtime ) {
		super( runtime, Key.datasourceService );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		BoxRuntime.timerUtil.start( "datasourceservice-startup" );
		logger.debug( "+ Starting up DataSourceService..." );

		// Register the default JDBC Driver
		registerDriver( new ortus.boxlang.runtime.jdbc.drivers.GenericJDBCDriver() );

		// Announce it
		announce(
		    BoxEvent.ON_DATASOURCE_SERVCE_STARTUP,
		    Struct.of( "DatasourceService", this )
		);

		// Let it be known!
		logger.info( "+ Datasource Service started in [{}] ms", BoxRuntime.timerUtil.stopAndGetMillis( "datasourceservice-startup" ) );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force If true, forces the shutdown of the scheduler
	 */
	@Override
	public void onShutdown( Boolean force ) {
		// Announce it
		announce(
		    BoxEvent.ON_DATASOURCE_SERVICE_SHUTDOWN,
		    Struct.of( "DatasourceService", this )
		);

		// Shutdown all datasources
		clear();

		// Log it
		logger.debug( "+ Datasource Service shutdown" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Driver Registrations
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register a new driver
	 *
	 * @param driver The driver to register
	 *
	 * @return DatasourceService
	 */
	public DatasourceService registerDriver( IJDBCDriver driver ) {
		this.jdbcDrivers.put( driver.getName(), driver );
		return this;
	}

	/**
	 * Get a driver by name
	 *
	 * @param name The name of the driver
	 *
	 * @return The driver, if found, or `null`
	 */
	public IJDBCDriver getDriver( Key name ) {
		return this.jdbcDrivers.get( name );
	}

	/**
	 * Get the generic JDBC driver
	 *
	 * @return The generic JDBC driver
	 */
	public IJDBCDriver getGenericDriver() {
		return this.jdbcDrivers.get( Key.generic );
	}

	/**
	 * Has a driver been registered?
	 *
	 * @param name The name of the driver
	 *
	 * @return True if the driver is registered, false otherwise
	 */
	public Boolean hasDriver( Key name ) {
		return this.jdbcDrivers.containsKey( name );
	}

	/**
	 * Remove a driver by name
	 *
	 * @param name The name of the driver
	 */
	public Boolean removeDriver( Key name ) {
		return this.jdbcDrivers.remove( name ) != null;
	}

	/**
	 * Clear all registered drivers
	 */
	public DatasourceService clearDrivers() {
		this.jdbcDrivers.clear();
		return this;
	}

	/**
	 * How many drivers are registered?
	 */
	public Integer driverSize() {
		return this.jdbcDrivers.size();
	}

	/**
	 * Get an array of all registered driver names
	 */
	public String[] getDriverNames() {
		return this.jdbcDrivers.keySet()
		    .stream()
		    .map( Key::getName )
		    .sorted()
		    .toArray( String[]::new );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Datasource Registrations
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Registers a datasource with the manager.
	 * If the datasource is already registered it will just return it.
	 *
	 * @param config The datasource configuration object
	 *
	 * @return A new or already registered datasource
	 */
	public DataSource register( DatasourceConfig config ) {
		return this.datasources.computeIfAbsent( config.getUniqueName(), key -> new DataSource( config ) );
	}

	/**
	 * Registers a datasource with the manager.
	 * If the datasource is already registered it will just return it.
	 *
	 * @param name       The name of the datasource
	 * @param properties The datasource properties
	 *
	 * @return A new or already registered datasource
	 */
	public DataSource register( Key name, IStruct properties ) {
		return register( new DatasourceConfig( name, properties ) );
	}

	/**
	 * Registers a datasource with the manager.
	 *
	 * @param name       The name of the datasource
	 * @param datasource The datasource to register
	 *
	 * @return The datasource that was registered
	 */
	public DataSource register( Key name, DataSource datasource ) {
		return this.datasources.putIfAbsent( name, datasource );
	}

	/**
	 * Do we have a datasource registered with the manager?
	 *
	 * @param name The name of the datasource
	 *
	 * @return True if the datasource is registered, false otherwise
	 */
	public Boolean has( Key name ) {
		return this.datasources.containsKey( name );
	}

	/**
	 * Do we have a datasource registered with the manager using a configuration?
	 *
	 * @param config The datasource configuration
	 *
	 * @return True if the datasource is registered, false otherwise
	 */
	public Boolean has( DatasourceConfig config ) {
		return this.has( config.getUniqueName() );
	}

	/**
	 * Get a datasource by (Key) name.
	 *
	 * @param name Name of the datasource to retrieve - for example, `Key.of( "blog" )`
	 *
	 * @return An instance of the datasource, if found, or `null`.
	 */
	public DataSource get( Key name ) {
		return this.datasources.get( name );
	}

	/**
	 * Get a datasource by config.
	 *
	 * @param config Datasource configuration struct.
	 *
	 * @return An instance of the datasource, if found, or `null`.
	 */
	public DataSource get( DatasourceConfig config ) {
		return this.get( config.getUniqueName() );
	}

	/**
	 * Remove a datasurce by key name
	 *
	 * @param name Name of the datasource to remove
	 */
	public Boolean remove( Key name ) {
		var datasource = this.datasources.get( name );
		if ( datasource != null ) {
			datasource.shutdown();
			this.datasources.remove( name );
			return true;
		}
		return false;
	}

	/**
	 * Remove a datasurce by key name
	 *
	 * @param config Datasource configuration struct.
	 */
	public Boolean remove( DatasourceConfig config ) {
		return this.remove( config.getUniqueName() );
	}

	/**
	 * Clear all registered datasources and close all connections.
	 * <p>
	 * Will close all open connections and remove all datasources (including the default) from the manager.
	 */
	public DatasourceService clear() {
		// Shutdown all datasources in a parallel stream
		this.datasources.values().parallelStream().forEach( DataSource::shutdown );
		this.datasources.clear();
		return this;
	}

	/**
	 * How many datasources are registered?
	 */
	public Integer size() {
		return this.datasources.size();
	}

	/**
	 * Get an array of all registered datasources names
	 */
	public String[] getNames() {
		return this.datasources.keySet()
		    .stream()
		    .map( Key::getName )
		    .sorted()
		    .toArray( String[]::new );
	}

}
