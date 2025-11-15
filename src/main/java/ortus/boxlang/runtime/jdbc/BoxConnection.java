package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class BoxConnection implements Connection {

	private final Connection	connection;
	private final DataSource	datasource;

	public BoxConnection( Connection connection, DataSource datasource ) {
		this.connection	= connection;
		this.datasource	= datasource;
	}

	public static BoxConnection of( Connection connection, DataSource datasource ) {
		return new BoxConnection( connection, datasource );
	}

	/**
	 * Get the BoxLang DataSource that created this connection
	 *
	 * @return The BoxLang DataSource instance
	 */
	public DataSource getDataSource() {
		return datasource;
	}

	/**
	 * Get the underlying JDBC Connection
	 */
	public Connection getConnection() {
		return connection;
	}

	// *************************************
	// Connection Interface methods
	// *************************************

	@Override
	public void close() throws java.sql.SQLException {
		connection.close();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T unwrap( Class<T> iface ) throws SQLException {
		if ( iface.isInstance( this.connection ) ) {
			return ( T ) this.connection;
		} else if ( this.connection != null ) {
			return this.connection.unwrap( iface );
		} else {
			throw new SQLException( "Wrapped connection is not an instance of " + iface );
		}
	}

	@Override
	public boolean isWrapperFor( Class<?> iface ) throws SQLException {
		return iface.isInstance( this.connection ) || this.connection != null && this.connection.isWrapperFor( iface );
	}

	// Delegate all other methods to the underlying connection

	@Override
	public BoxStatement createStatement() throws java.sql.SQLException {
		return new BoxStatement( this, connection.createStatement() );
	}

	@Override
	public BoxPreparedStatement prepareStatement( String sql ) throws java.sql.SQLException {
		return new BoxPreparedStatement( this, connection.prepareStatement( sql ) );
	}

	@Override
	public BoxCallableStatement prepareCall( String sql ) throws java.sql.SQLException {
		return new BoxCallableStatement( this, connection.prepareCall( sql ) );
	}

	@Override
	public String nativeSQL( String sql ) throws java.sql.SQLException {
		return connection.nativeSQL( sql );
	}

	@Override
	public void setAutoCommit( boolean autoCommit ) throws java.sql.SQLException {
		connection.setAutoCommit( autoCommit );
	}

	@Override
	public boolean getAutoCommit() throws java.sql.SQLException {
		return connection.getAutoCommit();
	}

	@Override
	public void commit() throws java.sql.SQLException {
		connection.commit();
	}

	@Override
	public void rollback() throws java.sql.SQLException {
		connection.rollback();
	}

	@Override
	public boolean isClosed() throws java.sql.SQLException {
		return connection.isClosed();
	}

	@Override
	public java.sql.DatabaseMetaData getMetaData() throws java.sql.SQLException {
		return connection.getMetaData();
	}

	@Override
	public void setReadOnly( boolean readOnly ) throws java.sql.SQLException {
		connection.setReadOnly( readOnly );
	}

	@Override
	public boolean isReadOnly() throws java.sql.SQLException {
		return connection.isReadOnly();
	}

	@Override
	public void setCatalog( String catalog ) throws java.sql.SQLException {
		connection.setCatalog( catalog );
	}

	@Override
	public String getCatalog() throws java.sql.SQLException {
		return connection.getCatalog();
	}

	@Override
	public void setTransactionIsolation( int level ) throws java.sql.SQLException {
		connection.setTransactionIsolation( level );
	}

	@Override
	public int getTransactionIsolation() throws java.sql.SQLException {
		return connection.getTransactionIsolation();
	}

	@Override
	public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
		return connection.getWarnings();
	}

	@Override
	public void clearWarnings() throws java.sql.SQLException {
		connection.clearWarnings();
	}

	@Override
	public BoxStatement createStatement( int resultSetType, int resultSetConcurrency ) throws java.sql.SQLException {
		return new BoxStatement( this, connection.createStatement( resultSetType, resultSetConcurrency ) );
	}

	@Override
	public BoxPreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency ) throws java.sql.SQLException {
		return new BoxPreparedStatement( this, connection.prepareStatement( sql, resultSetType, resultSetConcurrency ) );
	}

	@Override
	public BoxCallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency ) throws java.sql.SQLException {
		return new BoxCallableStatement( this, connection.prepareCall( sql, resultSetType, resultSetConcurrency ) );
	}

	@Override
	public java.util.Map<String, Class<?>> getTypeMap() throws java.sql.SQLException {
		return connection.getTypeMap();
	}

	@Override
	public void setTypeMap( java.util.Map<String, Class<?>> map ) throws java.sql.SQLException {
		connection.setTypeMap( map );
	}

	@Override
	public void setHoldability( int holdability ) throws java.sql.SQLException {
		connection.setHoldability( holdability );
	}

	@Override
	public int getHoldability() throws java.sql.SQLException {
		return connection.getHoldability();
	}

	@Override
	public java.sql.Savepoint setSavepoint() throws java.sql.SQLException {
		return connection.setSavepoint();
	}

	@Override
	public java.sql.Savepoint setSavepoint( String name ) throws java.sql.SQLException {
		return connection.setSavepoint( name );
	}

	@Override
	public void rollback( java.sql.Savepoint savepoint ) throws java.sql.SQLException {
		connection.rollback( savepoint );
	}

	@Override
	public void releaseSavepoint( java.sql.Savepoint savepoint ) throws java.sql.SQLException {
		connection.releaseSavepoint( savepoint );
	}

	@Override
	public BoxStatement createStatement( int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws java.sql.SQLException {
		return new BoxStatement( this, connection.createStatement( resultSetType, resultSetConcurrency, resultSetHoldability ) );
	}

	@Override
	public BoxPreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability )
	    throws java.sql.SQLException {
		return new BoxPreparedStatement( this, connection.prepareStatement( sql, resultSetType, resultSetConcurrency, resultSetHoldability ) );
	}

	@Override
	public BoxCallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability )
	    throws java.sql.SQLException {
		return new BoxCallableStatement( this, connection.prepareCall( sql, resultSetType, resultSetConcurrency, resultSetHoldability ) );
	}

	@Override
	public BoxPreparedStatement prepareStatement( String sql, int autoGeneratedKeys ) throws java.sql.SQLException {
		return new BoxPreparedStatement( this, connection.prepareStatement( sql, autoGeneratedKeys ) );
	}

	@Override
	public BoxPreparedStatement prepareStatement( String sql, int[] columnIndexes ) throws java.sql.SQLException {
		return new BoxPreparedStatement( this, connection.prepareStatement( sql, columnIndexes ) );
	}

	@Override
	public BoxPreparedStatement prepareStatement( String sql, String[] columnNames ) throws java.sql.SQLException {
		return new BoxPreparedStatement( this, connection.prepareStatement( sql, columnNames ) );
	}

	@Override
	public java.sql.Clob createClob() throws java.sql.SQLException {
		return connection.createClob();
	}

	@Override
	public java.sql.Blob createBlob() throws java.sql.SQLException {
		return connection.createBlob();
	}

	@Override
	public java.sql.NClob createNClob() throws java.sql.SQLException {
		return connection.createNClob();
	}

	@Override
	public java.sql.SQLXML createSQLXML() throws java.sql.SQLException {
		return connection.createSQLXML();
	}

	@Override
	public boolean isValid( int timeout ) throws java.sql.SQLException {
		return connection.isValid( timeout );
	}

	@Override
	public void setClientInfo( String name, String value ) throws java.sql.SQLClientInfoException {
		connection.setClientInfo( name, value );
	}

	@Override
	public void setClientInfo( java.util.Properties properties ) throws java.sql.SQLClientInfoException {
		connection.setClientInfo( properties );
	}

	@Override
	public String getClientInfo( String name ) throws java.sql.SQLException {
		return connection.getClientInfo( name );
	}

	@Override
	public java.util.Properties getClientInfo() throws java.sql.SQLException {
		return connection.getClientInfo();
	}

	@Override
	public java.sql.Array createArrayOf( String typeName, Object[] elements ) throws java.sql.SQLException {
		return connection.createArrayOf( typeName, elements );
	}

	@Override
	public java.sql.Struct createStruct( String typeName, Object[] attributes ) throws java.sql.SQLException {
		return connection.createStruct( typeName, attributes );
	}

	@Override
	public void setSchema( String schema ) throws java.sql.SQLException {
		connection.setSchema( schema );
	}

	@Override
	public String getSchema() throws java.sql.SQLException {
		return connection.getSchema();
	}

	@Override
	public void abort( java.util.concurrent.Executor executor ) throws java.sql.SQLException {
		connection.abort( executor );
	}

	@Override
	public void setNetworkTimeout( java.util.concurrent.Executor executor, int milliseconds ) throws java.sql.SQLException {
		connection.setNetworkTimeout( executor, milliseconds );
	}

	@Override
	public int getNetworkTimeout() throws java.sql.SQLException {
		return connection.getNetworkTimeout();
	}

}
