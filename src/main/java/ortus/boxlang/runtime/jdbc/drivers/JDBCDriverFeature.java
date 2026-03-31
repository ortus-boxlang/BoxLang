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

/**
 * Enum representing JDBC driver features tracked via bitfield.
 */
public enum JDBCDriverFeature {

	/**
	 * Indicates that the driver generates keys as a ResultSet instead of using statement.getGeneratedKeys().
	 * This requires additional overhead to introspect each result set and see if it looks like a generated keys set.
	 * Drivers that do this include: MSSQL
	 */
	GENERATED_KEYS_COME_AS_RESULT_SET( 1L << 0 ),

	/**
	 * Indicates that the driver supports stored procedure return codes.
	 * When enabled, if the storedproc tag has a returnCode attribute, the SQL generated will include ? = call ... to capture the return code.
	 * Drivers that support this include: MSSQL
	 */
	SUPPORTS_STORED_PROC_RETURN_CODE( 1L << 1 ),

	/**
	 * Indicates that the driver requires trailing semicolons to be trimmed from SQL statements.
	 * Some drivers throw errors when SQL statements end with semicolons.
	 * Drivers that require this include: Oracle
	 */
	TRIM_TRAILING_SEMICOLONS( 1L << 2 );

	private final long flag;

	/**
	 * Constructor
	 * 
	 * @param flag The flag value
	 */
	JDBCDriverFeature( long flag ) {
		this.flag = flag;
	}

	/**
	 * Get the flag value
	 * 
	 * @return The flag value
	 */
	public long getFlag() {
		return this.flag;
	}
}
