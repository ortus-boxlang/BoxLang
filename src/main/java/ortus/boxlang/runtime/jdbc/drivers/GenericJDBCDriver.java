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
package ortus.boxlang.runtime.jdbc.drivers;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.StructUtil;

/**
 * This is the generic JDBC driver that can be used to register datasources in the system.
 * We use a generic JDBC Url connection schema to connect to the database.
 */
public class GenericJDBCDriver implements IJDBCDriver {

	/**
	 * --------------------------------------------------------------------------
	 * Driver properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The unique name of the driver
	 */
	protected Key					name;

	/**
	 * The driver type according to BoxLang standards
	 */
	protected DatabaseDriverType	type;

	/**
	 * The class name of the driver, useful metadata
	 */
	protected String				driverClassName		= "";

	/**
	 * The default delimiter for the custom parameters
	 */
	protected String				defaultDelimiter	= "&";

	/**
	 * A default port for the connection URL, if needed. Null by default.
	 */
	protected String				defaultPort;

	/**
	 * A default host for the connection URL, if needed. Localhost by default.
	 */
	protected String				defaultHost			= "localhost";

	/**
	 * The default custom params for the connection URL
	 * These are attached to the connection URL as query parameters
	 * according to the driver's requirements.
	 */
	protected IStruct				defaultCustomParams	= Struct.of();

	/**
	 * The default configuration properties.
	 * These are attached to the datasource configuration as properties
	 * Which can be used by our Connection Pool: HikariCP
	 */
	protected IStruct				defaultProperties	= Struct.of();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	public GenericJDBCDriver() {
		this.name	= new Key( "Generic" );
		this.type	= DatabaseDriverType.GENERIC;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public Key getName() {
		return this.name;
	}

	@Override
	public DatabaseDriverType getType() {
		return this.type;
	}

	/**
	 * We return an empty class, because we are using a generic JDBC driver
	 * that does not have a specific class name.
	 * <p>
	 * This will be based on the connection url built by the driver and it will be expecting
	 * the class to be in the class path.
	 * <p>
	 * Custom parameters are incorporated by the {@link DatasourceConfig} object automatically.
	 */
	@Override
	public String getClassName() {
		return this.driverClassName;
	}

	@Override
	public String buildConnectionURL( DatasourceConfig config ) {
		// Validate the driver
		String jDriver = ( String ) config.properties.getOrDefault( Key.driver, "" );
		if ( jDriver.isEmpty() ) {
			throw new IllegalArgumentException( "The driver property is required for the Generic JDBC Driver" );
		}
		// Validate the port
		Integer port = IntegerCaster.cast( config.properties.getOrDefault( Key.port, 0 ), false );
		if ( port == null || port == 0 ) {
			throw new IllegalArgumentException( "The port property is required for the Generic JDBC Driver" );
		}

		// Validate the database
		String	database	= ( String ) config.properties.getOrDefault( Key.database, "" );
		// Host we can use localhost
		String	host		= ( String ) config.properties.getOrDefault( Key.host, "localhost" );

		// Build the Generic connection URL
		return String.format(
		    "jdbc:%s://%s:%d/%s?%s",
		    jDriver,
		    host,
		    port,
		    database,
		    customParamsToQueryString( config )
		);
	}

	/**
	 * Get default properties for the driver to incorporate into the datasource config
	 */
	@Override
	public IStruct getDefaultProperties() {
		return this.defaultProperties;
	}

	/**
	 * Get default custom parameters for the driver to incorporate into the datasource config
	 */
	@Override
	public IStruct getDefaultCustomParams() {
		return this.defaultCustomParams;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the default port for the driver
	 *
	 * @return The default port
	 */
	public String getDefaultPort() {
		return this.defaultPort;
	}

	/**
	 * Get the default host for the driver
	 *
	 * @return The default host
	 */
	public String getDefaultHost() {
		return this.defaultHost;
	}

	/**
	 * Get the default delimiter for the custom parameters
	 *
	 * @return The default delimiter
	 */
	public String getDefaultDelimiter() {
		return this.defaultDelimiter;
	}

	/**
	 * This helper method is used to convert the custom parameters in the config (Key.custom)
	 * to a query string that can be used by the driver to build the connection URL.
	 * <p>
	 * We incorporate the default parameters into the custom parameters and return the query string
	 * using the driver's default delimiter.
	 *
	 * @param config The datasource config
	 *
	 * @return The custom parameters as a query string
	 */
	public String customParamsToQueryString( DatasourceConfig config ) {
		IStruct params = new Struct( this.defaultCustomParams );

		// If the custom parameters are a string, convert them to a struct
		if ( config.properties.get( Key.custom ) instanceof String castedParams ) {
			config.properties.put( Key.custom, StructUtil.fromQueryString( castedParams, this.defaultDelimiter ) );
		}

		// Add all the custom parameters to the params struct
		config.properties.getAsStruct( Key.custom ).forEach( params::put );

		// Return it as the query string needed by the driver
		return StructUtil.toQueryString( params, this.defaultDelimiter );
	}

}
