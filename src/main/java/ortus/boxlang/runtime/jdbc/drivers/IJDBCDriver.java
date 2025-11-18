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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * This interface is used to define the methods that a JDBC driver must implement
 * in order to do datasource registrations, population and validation.
 * <p>
 * This is not the same as a JDBC Driver. This is a helper class that allows us to modularly
 * build JDBC Drivers that can be used to register datasources in the system.
 */
public interface IJDBCDriver {

	/**
	 * Get the driver name
	 */
	public Key getName();

	/**
	 * Get the driver type
	 */
	public DatabaseDriverType getType();

	/**
	 * Get the driver class name
	 */
	public String getClassName();

	/**
	 * Get the connection JDBC URL according to the driver type.
	 * The driver implementation should be able to build the connection URL
	 */
	public String buildConnectionURL( DatasourceConfig config );

	/**
	 * Get default properties for the driver to incorporate into the datasource config
	 */
	public IStruct getDefaultProperties();

	/**
	 * Get default custom parameters for the driver to incorporate into the datasource config
	 */
	public IStruct getDefaultCustomParams();

	/**
	 * Get the default URI delimiter
	 */
	public String getDefaultURIDelimiter();

	/**
	 * Get the default custom params delimiter
	 */
	public String getDefaultDelimiter();

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
	public String customParamsToQueryString( DatasourceConfig config );

	/**
	 * Map a SQL type to a QueryColumnType. The default implementation will use the mappings in the QueryColumnType enum.
	 * Override this method if the driver has specific mappings. Example, mapping RowId in Oracle to a String type.
	 * 
	 * @param sqlType The SQL type to map, from java.sql.Types
	 * 
	 * @return The QueryColumnType
	 */
	public QueryColumnType mapSQLTypeToQueryColumnType( int sqlType );

	/**
	 * Transform a value coming OUT of the DB according to the driver's specific needs. This allows drivers to map custom Java classes to native BL types.
	 * The default implementation will return the value as-is.
	 * 
	 * @param sqlType The SQL type of the value, from java.sql.Types
	 * @param value   The value to transform
	 * 
	 * @return The transformed value
	 */
	public Object transformValue( int sqlType, Object value );

	/**
	 * Transform a value going IN to the DB according to the driver's specific needs. This allows drivers to map custom native BL types to custom driver Java types.
	 * Ex: Oracle's custom BLOB and CLOB classes.
	 * 
	 * @param type       The SQL type of the value, from QueryColumnType
	 * @param value      The value to transform
	 * @param context    The context for type casting
	 * @param connection The BoxConnection instance
	 * 
	 * @return The transformed value
	 */
	public Object transformParamValue( QueryColumnType type, Object value, IBoxContext context, BoxConnection connection );

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
	public int mapParamTypeToSQLType( QueryColumnType type, Object value );
}
