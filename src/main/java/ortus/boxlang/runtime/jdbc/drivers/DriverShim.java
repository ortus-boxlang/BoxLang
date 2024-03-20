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
package ortus.boxlang.runtime.jdbc.drivers;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A JDBC Driver Proxy class to allow registering JDBC Drivers with <code>java.sql.DriverManager</code> from child class loaders (i.e. BoxLang
 * modules).
 *
 * This is done this way, since the JDK DriverManager does not allow registering drivers from child class loaders.
 */
public class DriverShim implements Driver {

	/**
	 * The wrapped JDBC Driver
	 */
	private Driver driver;

	/**
	 * Creates a new DriverShim instance
	 *
	 * @param d The wrapped JDBC Driver
	 */
	public DriverShim( Driver d ) {
		this.driver = d;
	}

	/**
	 * Returns the wrapped JDBC Driver
	 *
	 * @param url  The JDBC URL
	 * @param info The connection properties
	 *
	 * @return The wrapped JDBC Driver
	 */
	@Override
	public Connection connect( String url, Properties info ) throws SQLException {
		return this.driver.connect( url, info );
	}

	/**
	 * Accepts the JDBC URL
	 *
	 * @param u The JDBC URL
	 *
	 * @return The wrapped JDBC Driver
	 */
	public boolean acceptsURL( String u ) throws SQLException {
		return this.driver.acceptsURL( u );
	}

	/**
	 * Returns the wrapped JDBC Driver's major version
	 *
	 * @return The wrapped JDBC Driver's major version
	 */
	public int getMajorVersion() {
		return this.driver.getMajorVersion();
	}

	/**
	 * Returns the wrapped JDBC Driver's minor version
	 *
	 * @return The wrapped JDBC Driver's minor version
	 */
	public int getMinorVersion() {
		return this.driver.getMinorVersion();
	}

	/**
	 * Returns the wrapped JDBC Driver's property information
	 *
	 * @param u The JDBC URL
	 * @param p The connection properties
	 *
	 * @return The wrapped JDBC Driver's property information
	 */
	public DriverPropertyInfo[] getPropertyInfo( String u, Properties p ) throws SQLException {
		return this.driver.getPropertyInfo( u, p );
	}

	/**
	 * Returns the wrapped JDBC Driver's JDBC compliant status
	 *
	 * @return The wrapped JDBC Driver's JDBC compliant status
	 */
	public boolean jdbcCompliant() {
		return this.driver.jdbcCompliant();
	}

	/**
	 * Returns the wrapped JDBC Driver's parent logger
	 *
	 * @return The wrapped JDBC Driver's parent logger
	 */
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}
