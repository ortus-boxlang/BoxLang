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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

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
}
