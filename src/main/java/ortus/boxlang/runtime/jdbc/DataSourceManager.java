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

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class DataSourceManager {

	/**
	 * Singleton instance of the DatasourceManager.
	 */
	private static DataSourceManager	instance	= null;

	/**
	 * Map of datasources registered with the manager.
	 * Note the lifetime of this map is the same as the lifetime of the singleton instance of this class - in other words, the lifetime of the runtime
	 * itself.
	 *
	 * @TODO: Tie the lifetime of the datasources to the lifetime of the Box application, not the runtime. i.e. we want `BoxRuntime.shutdown()` to destroy
	 *        the datasources.
	 */
	private Map<Key, DataSource>		datasources	= new HashMap<>();

	/**
	 * Private constructor. Use getInstance() instead.
	 */
	private DataSourceManager() {
	}

	/**
	 * Get the singleton instance of the DatasourceManager.
	 * <p>
	 * Will construct a new instance if one does not already exist, otherwise returns the existing instance.
	 *
	 * @return The DatasourceManager instance.
	 */
	public static DataSourceManager getInstance() {
		if ( instance == null ) {
			instance = new DataSourceManager();
		}
		return instance;
	}

	/**
	 * Register a datasource with the manager.
	 * <p>
	 * Stores a datasource in the manager for later retrieval by key name.
	 * <p>
	 * It is preferred that you use `setDefaultDatasource` to set the default datasource for the application. All other datasources should be registered
	 * with this method.
	 *
	 * @param name       Name of the datasource. This will be used in retrieving the datasource later.
	 * @param datasource Struct of datasource properties for configuring the datasource.
	 */
	public DataSourceManager registerDatasource( Key name, IStruct datasource ) {
		this.datasources.put( name, DataSource.fromStruct( datasource ) );
		return this;
	}

	/**
	 * Get a datasource by (Key) name.
	 *
	 * @param name Name of the datasource to retrieve - for example, `Key.of( "blog" )`
	 *
	 * @return An instance of the datasource, if found, or `null`.
	 */
	public DataSource getDatasource( Key name ) {
		return this.datasources.get( name );
	}

	/**
	 * Set the default datasource.
	 * <p>
	 * Chainable method to set the default datasource for this application/runtime.
	 *
	 * @param datasource The datasource to set as the default. Almost always configured via <code>this.datasource = {}</code> in the Application.cfc.
	 */
	public DataSourceManager setDefaultDatasource( DataSource datasource ) {
		this.datasources.put( Key._DEFAULT, datasource );
		return this;
	}

	/**
	 * Get the default datasource as configured in the Application.cfc.
	 *
	 * @return An instance of the default datasource, if configured, or null.
	 */
	public DataSource getDefaultDatasource() {
		return this.datasources.get( Key._DEFAULT );
	}

	/**
	 * Clear all registered datasources.
	 * <p>
	 * Will close all open connections and remove all datasources (including the default) from the manager.
	 */
	public DataSourceManager clear() {
		this.datasources.forEach( ( name, datasource ) -> {
			datasource.shutdown();
		} );
		this.datasources.clear();
		return this;
	}
}
