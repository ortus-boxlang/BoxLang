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
package ortus.boxlang.runtime.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.BaseService;
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
public class DataSourceManager extends BaseService {

	/**
	 * Logger
	 */
	private static final Logger				logger						= LoggerFactory.getLogger( DataSourceManager.class );

	/**
	 * Map of datasources registered with the manager.
	 * Note the lifetime of this map is the same as the lifetime of the instance of this class - in other words, the lifetime of the surrounding context
	 * itself, whether that be the ApplicationBoxContext, a ScriptingBoxContext if we want to allow defining datasources in a single ad-hoc script for
	 * Lambda support, or a future ServerContext for defining datasources at the server level.
	 */
	private Map<Key, DataSource>			datasources					= new HashMap<>();

	/**
	 * Module Service Events
	 */
	private static final Map<String, Key>	DATASOURCE_MANAGER_EVENTS	= Stream.of(
	    "onDataSourceManagerStartup",
	    "onDataSourceManagerShutdown"
	).collect( Collectors.toMap(
	    eventName -> eventName,
	    Key::of
	) );

	/**
	 * Constructor
	 *
	 * @param runtime The BoxRuntime
	 */
	public DataSourceManager( BoxRuntime runtime ) {
		super( runtime );
	}

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		logger.atInfo().log( "+ Starting up DataSourceManager Service..." );

		// Announce it
		announce(
		    DATASOURCE_MANAGER_EVENTS.get( "onDataSourceManagerStartup" ),
		    Struct.of( "DataSourceManager", this )
		);
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force If true, forces the shutdown of the scheduler
	 */
	// @Override
	public void onShutdown( Boolean force ) {
		// Announce it
		announce(
		    DATASOURCE_MANAGER_EVENTS.get( "onDataSourceManagerShutdown" ),
		    Struct.of( "DataSourceManager", this )
		);
		// shut it down!
		this.shutdown();
		// Log it
		logger.atInfo().log( "+ DataSourceManager Service shutdown" );
	}

	/**
	 * Register a datasource with the manager.
	 * <p>
	 * Stores a datasource in the manager for later retrieval by key name.
	 * <p>
	 * It is preferred that you use `setDefaultDataSource` to set the default datasource for the application. All other datasources should be registered
	 * with this method.
	 *
	 * @param name       Name of the datasource. This will be used in retrieving the datasource later.
	 * @param datasource Struct of datasource properties for configuring the datasource.
	 */
	public DataSource registerDataSource( Key name, DatasourceConfig config ) {
		return this.registerDataSource( name, new DataSource( config ) );
	}

	/**
	 * Register a datasource with the manager.
	 * <p>
	 * Stores a datasource in the manager for later retrieval by key name.
	 * <p>
	 * It is preferred that you use `setDefaultDataSource` to set the default datasource for the application. All other datasources should be registered
	 * with this method.
	 *
	 * @param name       Name of the datasource. This will be used in retrieving the datasource later.
	 * @param datasource Struct of datasource properties for configuring the datasource.
	 */
	public DataSource registerDataSource( Key name, IStruct datasource ) {
		return this.registerDataSource( name, DataSource.fromDataSourceStruct( datasource ) );
	}

	/**
	 * Register a datasource with the manager.
	 * <p>
	 * Stores a datasource in the manager for later retrieval by key name.
	 * <p>
	 * It is preferred that you use `setDefaultDataSource` to set the default datasource for the application. All other datasources should be registered
	 * with this method.
	 *
	 * @param name       Name of the datasource. This will be used in retrieving the datasource later.
	 * @param datasource Struct of datasource properties for configuring the datasource.
	 */
	public DataSource registerDataSource( Key name, DataSource datasource ) {
		this.datasources.put( name, datasource );
		return datasource;
	}

	/**
	 * Get a datasource by (Key) name.
	 *
	 * @param name Name of the datasource to retrieve - for example, `Key.of( "blog" )`
	 *
	 * @return An instance of the datasource, if found, or `null`.
	 */
	public @Nullable DataSource getDataSource( Key name ) {
		return this.datasources.get( name );
	}

	/**
	 * Set the default datasource.
	 * <p>
	 * Chainable method to set the default datasource for this application/runtime.
	 *
	 * import java.util.stream.Collectors;
	 * import java.util.stream.Stream;
	 *
	 * @param datasourceName The datasource to set as the default. Almost always configured via <code>this.datasource = {}</code> in the Application.cfc.
	 */
	public DataSourceManager setDefaultDataSource( Key datasourceName ) {
		this.datasources.put( Key._DEFAULT, this.datasources.get( datasourceName ) );
		return this;
	}

	/**
	 * Set the default datasource.
	 * <p>
	 * Chainable method to set the default datasource for this application/runtime.
	 *
	 * @param datasource The datasource to set as the default. Almost always configured via <code>this.datasource = {}</code> in the Application.cfc.
	 */
	public DataSourceManager setDefaultDataSource( DataSource datasource ) {
		this.datasources.put( Key._DEFAULT, datasource );
		return this;
	}

	/**
	 * Get the default datasource as configured in the Application.cfc.
	 *
	 * @return An instance of the default datasource, if configured, or null.
	 */
	public DataSource getDefaultDataSource() {
		return this.datasources.get( Key._DEFAULT );
	}

	/**
	 * Look up and return the datasource matching the provided configuration, or register and return a new datasource if one is not found.
	 *
	 * @param config Datasource configuration struct.
	 *
	 * @return
	 */
	public DataSource getOrSetDatasource( IStruct config ) {
		DatasourceConfig datasourceConfig = new DatasourceConfig( null, null, config );
		return this.datasources.entrySet().stream()
		    .filter( entry -> entry.getValue().isConfigurationMatch( datasourceConfig ) )
		    .findFirst()
		    .map( Map.Entry::getValue )
		    .orElseGet( () -> this.registerDataSource( Key.of( datasourceConfig.getUniqueName() ), datasourceConfig ) );
	}

	/**
	 * Clear all registered datasources.
	 * <p>
	 * Will close all open connections and remove all datasources (including the default) from the manager.
	 */
	public DataSourceManager clear( boolean shutdown ) {
		if ( shutdown ) {
			this.datasources.forEach( ( name, datasource ) -> datasource.shutdown() );
		}
		this.datasources.clear();
		return this;
	}

	/**
	 * Shutdown the DatasourceManager, including closing all open datasources, connections, and connection pools.
	 */
	public void shutdown() {
		this.clear( true );
	}
}
