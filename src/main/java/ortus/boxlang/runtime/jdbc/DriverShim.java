package ortus.boxlang.runtime.jdbc;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

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
