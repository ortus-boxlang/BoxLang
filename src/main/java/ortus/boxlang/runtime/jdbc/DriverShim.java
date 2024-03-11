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
 */
public class DriverShim implements Driver {

	private Driver driver;

	public DriverShim( Driver d ) {
		this.driver = d;
	}

	@Override
	public Connection connect( String url, Properties info ) throws SQLException {
		return this.driver.connect( url, info );
	}

	public boolean acceptsURL( String u ) throws SQLException {
		return this.driver.acceptsURL( u );
	}

	public int getMajorVersion() {
		return this.driver.getMajorVersion();
	}

	public int getMinorVersion() {
		return this.driver.getMinorVersion();
	}

	public DriverPropertyInfo[] getPropertyInfo( String u, Properties p ) throws SQLException {
		return this.driver.getPropertyInfo( u, p );
	}

	public boolean jdbcCompliant() {
		return this.driver.jdbcCompliant();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}
