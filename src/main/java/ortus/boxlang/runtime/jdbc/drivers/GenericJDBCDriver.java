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

import java.sql.ResultSet;
import java.sql.SQLException;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.jdbc.BoxStatement;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.StructUtil;

/**
 * This is the generic JDBC driver that can be used to register datasources in the system.
 * We use a generic JDBC Url connection schema to connect to the database.
 * <p>
 * All modules that need to connect to a database should use this driver as it's
 * base class.
 * <p>
 * Make sure you take note of all the properties and methods that are available to you.
 */
public class GenericJDBCDriver implements IJDBCDriver {

	/**
	 * Bitfield to store enabled features (supports up to 64 flags).
	 */
	private long featureFlags = 0L;

	@Override
	public void setFeatures( JDBCDriverFeature... features ) {
		this.featureFlags = 0L;
		for ( JDBCDriverFeature f : features ) {
			this.featureFlags |= f.getFlag();
		}
	}

	@Override
	public boolean hasFeature( JDBCDriverFeature feature ) {
		return ( this.featureFlags & feature.getFlag() ) != 0L;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Driver properties
	 * --------------------------------------------------------------------------
	 * Each module that extends this class should override them as needed.
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
	 * The default delimiter for the addition of custom paramters to the connection URL.
	 * This delimits the start of the custom parameters.
	 * Example: jdbc:mysql://localhost:3306/mydb?param1=value1&amp;param2=value2
	 */
	protected String				defaultURIDelimiter	= "?";

	/**
	 * The default delimiter for the custom parameters attached to the connection URL
	 * Example: jdbc:mysql://localhost:3306/mydb?param1=value1&amp;param2=value2
	 * This delimits each custom parameter.
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

	/**
	 * This builds a generic connection URL for the driver.
	 * Each module that extends this class should override this method as needed.
	 */
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
		    "jdbc:%s://%s:%d/%s%s%s",
		    jDriver,
		    host,
		    port,
		    database,
		    getDefaultURIDelimiter(),
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
	 * Map a SQL type to a QueryColumnType. The default implementation will use the mappings in the QueryColumnType enum.
	 * Override this method if the driver has specific mappings. Example, mapping RowId in Oracle to a String type.
	 * 
	 * @param sqlType The SQL type to map, from java.sql.Types
	 * 
	 * @return The QueryColumnType
	 */
	@Override
	public QueryColumnType mapSQLTypeToQueryColumnType( int sqlType ) {
		return QueryColumnType.fromSQLType( sqlType );
	}

	/**
	 * Transform a value according to the driver's specific needs. This allows drivers to map custom Java classes to native BL types.
	 * The default implementation will return the value as-is.
	 * 
	 * @param sqlType   The SQL type of the value, from java.sql.Types
	 * @param value     The value to transform
	 * @param statement The BoxStatement instance
	 * 
	 * @return The transformed value
	 */
	@Override
	public Object transformValue( int sqlType, Object value, BoxStatement statement ) {
		// Handle common JDBC LOB and complex types
		if ( value instanceof java.sql.Blob blob ) {
			try {
				return blob.getBytes( 1, ( int ) blob.length() );
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading Blob data", e );
			}
		} else if ( value instanceof java.sql.Clob clob ) {
			try {
				return clob.getSubString( 1, ( int ) clob.length() );
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading Clob data", e );
			}
		} else if ( value instanceof java.sql.NClob nclob ) {
			try {
				return nclob.getSubString( 1, ( int ) nclob.length() );
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading NClob data", e );
			}
		} else if ( value instanceof java.sql.SQLXML sqlxml ) {
			try {
				return sqlxml.getString();
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading SQLXML data", e );
			}
		} else if ( value instanceof java.sql.Array array ) {
			try {
				// Convert SQL Array to Java Object array
				return array.getArray();
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading Array data", e );
			}
		} else if ( value instanceof java.sql.Struct struct ) {
			try {
				// Convert SQL Struct to Object array of attributes
				return struct.getAttributes();
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading Struct data", e );
			}
		} else if ( value instanceof java.sql.Ref ref ) {
			try {
				// Convert SQL Ref to its base type name
				return ref.getBaseTypeName();
			} catch ( Exception e ) {
				throw new RuntimeException( "Error reading Ref data", e );
			}
		} else if ( value instanceof java.sql.RowId rowId ) {
			// Convert RowId to byte array
			return rowId.getBytes();
		} else if ( value instanceof ResultSet resultSet ) {
			return Query.fromResultSet( statement, resultSet );
		}
		return value;
	}

	/**
	 * Transform a value going IN to the DB according to the driver's specific needs. This allows drivers to map custom native BL types to custom driver Java types.
	 * Ex: Oracle's custom BLOB and CLOB classes.
	 * 
	 * @param type       The SQL type of the value, from QueryColumnType
	 * @param value      The value to transform
	 * @param context    The context for type casting
	 * @param connection The BoxConnection instance
	 * 
	 * 
	 * @return The transformed value
	 */
	public Object transformParamValue( QueryColumnType type, Object value, IBoxContext context, BoxConnection connection ) {
		return QueryColumnType.toSQLType( type, value, context, connection );
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
	 * Get the URI delimiter for the custom parameters
	 *
	 * @return The URI delimiter
	 */
	public String getDefaultURIDelimiter() {
		return this.defaultURIDelimiter;
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
		IStruct params = new Struct( getDefaultCustomParams() );

		// If the custom parameters are a string, convert them to a struct
		if ( config.properties.get( Key.custom ) instanceof String castedParams ) {
			String parseDelimiter = getDefaultDelimiter();
			if ( !castedParams.contains( parseDelimiter ) && !parseDelimiter.equals( "&" ) ) {
				// No custom delimiter found; the custom parameters are probably ampersand-separated
				parseDelimiter = "&";
			}
			config.properties.put( Key.custom, StructUtil.fromQueryString( castedParams, parseDelimiter ) );
		}

		// Add all the custom parameters to the params struct
		config.properties.getAsStruct( Key.custom ).forEach( params::put );

		// Return it as the query string needed by the driver
		return StructUtil.toQueryString( params, getDefaultDelimiter() );
	}

	/**
	 * Map param type to SQL type. For the most part, these mappings are defined by the QueryColumnType enum,
	 * but some drivers may have specific needs. Oracle, or example, uses CHAR even when you ask for VARCHAR which allows
	 * char columns to match without trailing space.
	 * 
	 * @param type  The QueryColumnType of the parameter
	 * @param value The value of the parameter (in case the mapping needs to consider the value)
	 * 
	 * @return The SQL type as defined in java.sql.Types
	 */
	public int mapParamTypeToSQLType( QueryColumnType type, Object value ) {
		return type.sqlType;
	}

	/**
	 * Emit stored proc named parameter syntax according to the driver's specific needs.
	 * 
	 * @param callSQL   The StringBuilder to append the parameter syntax to
	 * @param paramName The name of the parameter
	 */
	public void emitStoredProcNamedParam( StringBuilder callSQL, String paramName ) {
		throw new UnsupportedOperationException( "Named parameters are not supported by the " + getName().getName() + " JDBC Driver yet." );
	}

	/**
	 * Pre-process a stored procedure call. This allows the driver to do any specific pre-processing
	 * before the procedure is called. This can include registering output parameters, etc.
	 * 
	 * @param conn          The BoxConnection instance
	 * @param procedureName The name of the stored procedure
	 * @param params        The parameters array
	 * @param procResults   The procedure results array
	 * @param context       The BoxLang context
	 * @param debug         Whether debug mode is enabled
	 */
	public void preProcessProcCall( BoxConnection conn, String procedureName, Array params, Array procResults, IBoxContext context, boolean debug )
	    throws SQLException {
		// Default implementation does nothing
	}

}
