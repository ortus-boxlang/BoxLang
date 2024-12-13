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
package ortus.boxlang.runtime.jdbc.qoq;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Properties;
import java.util.concurrent.Executor;

import ortus.boxlang.runtime.context.IBoxContext;

/**
 * This class is a dummy implementation of the {@link Connection} interface. It exists to support Query of Queries
 * so they can use the same pending query plumbing without needing new code paths and duplicate logic. The only methods
 * implement are for creating statements.
 */
public class QoQConnection implements Connection {

	private boolean		closed	= false;
	private IBoxContext	context;

	public QoQConnection( IBoxContext context ) {
		this.context = context;
	}

	public Statement createStatement() throws SQLException {
		return new QoQStatement( context, this );
	}

	public PreparedStatement prepareStatement( String sql ) throws SQLException {
		return prepareStatement( sql, Statement.NO_GENERATED_KEYS );
	}

	public CallableStatement prepareCall( String sql ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String nativeSQL( String sql ) throws SQLException {
		return sql;
	}

	public void setAutoCommit( boolean autoCommit ) throws SQLException {
	}

	public boolean getAutoCommit() throws SQLException {
		return true;
	}

	public void commit() throws SQLException {
	}

	public void rollback() throws SQLException {
	}

	public void close() throws SQLException {
		closed = true;
	}

	public boolean isClosed() throws SQLException {
		return closed;
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setReadOnly( boolean readOnly ) throws SQLException {
	}

	public boolean isReadOnly() throws SQLException {
		return false;
	}

	public void setCatalog( String catalog ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getCatalog() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setTransactionIsolation( int level ) throws SQLException {
	}

	public int getTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_READ_UNCOMMITTED;
	}

	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Statement createStatement( int resultSetType, int resultSetConcurrency ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setTypeMap( java.util.Map<String, Class<?>> map ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setHoldability( int holdability ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Savepoint setSavepoint() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Savepoint setSavepoint( String name ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void rollback( Savepoint savepoint ) throws SQLException {
	}

	public void releaseSavepoint( Savepoint savepoint ) throws SQLException {
	}

	public Statement createStatement( int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public CallableStatement prepareCall( String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, int autoGeneratedKeys ) throws SQLException {
		return new QoQPreparedStatement( context, this, sql, autoGeneratedKeys );
	}

	public PreparedStatement prepareStatement( String sql, int columnIndexes[] ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public PreparedStatement prepareStatement( String sql, String columnNames[] ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Clob createClob() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Blob createBlob() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public NClob createNClob() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SQLXML createSQLXML() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isValid( int timeout ) throws SQLException {
		return true;
	}

	public void setClientInfo( String name, String value ) throws SQLClientInfoException {
		throw new UnsupportedOperationException();
	}

	public void setClientInfo( Properties properties ) throws SQLClientInfoException {
		throw new UnsupportedOperationException();
	}

	public String getClientInfo( String name ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Properties getClientInfo() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Array createArrayOf( String typeName, Object[] elements ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Struct createStruct( String typeName, Object[] attributes ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setSchema( String schema ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getSchema() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void abort( Executor executor ) throws SQLException {

	}

	public void setNetworkTimeout( Executor executor, int milliseconds ) throws SQLException {

	}

	public int getNetworkTimeout() throws SQLException {
		return 0;
	}

	@Override
	public <T> T unwrap( Class<T> iface ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor( Class<?> iface ) throws SQLException {
		return false;
	}

}
