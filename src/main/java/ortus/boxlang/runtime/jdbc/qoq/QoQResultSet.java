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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.Calendar;

public class QoQResultSet implements java.sql.ResultSet {

	public boolean next() throws SQLException {
		return false;
	}

	public void close() throws SQLException {
	}

	public boolean wasNull() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getString( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean getBoolean( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public byte getByte( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public short getShort( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getInt( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public long getLong( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public float getFloat( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public double getDouble( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public BigDecimal getBigDecimal( int columnIndex, int scale ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public byte[] getBytes( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Date getDate( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Time getTime( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Timestamp getTimestamp( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.InputStream getAsciiStream( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.InputStream getUnicodeStream( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.InputStream getBinaryStream( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getString( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean getBoolean( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public byte getByte( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public short getShort( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getInt( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public long getLong( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public float getFloat( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public double getDouble( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public BigDecimal getBigDecimal( String columnLabel, int scale ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public byte[] getBytes( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Date getDate( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Time getTime( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Timestamp getTimestamp( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.InputStream getAsciiStream( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.InputStream getUnicodeStream( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.InputStream getBinaryStream( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SQLWarning getWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void clearWarnings() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getCursorName() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object getObject( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object getObject( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int findColumn( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.Reader getCharacterStream( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.Reader getCharacterStream( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public BigDecimal getBigDecimal( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public BigDecimal getBigDecimal( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isBeforeFirst() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isAfterLast() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isFirst() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isLast() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void beforeFirst() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void afterLast() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean first() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean last() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean absolute( int row ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean relative( int rows ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean previous() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setFetchDirection( int direction ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getFetchDirection() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setFetchSize( int rows ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getFetchSize() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getType() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getConcurrency() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean rowUpdated() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean rowInserted() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean rowDeleted() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNull( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBoolean( int columnIndex, boolean x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateByte( int columnIndex, byte x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateShort( int columnIndex, short x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateInt( int columnIndex, int x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateLong( int columnIndex, long x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateFloat( int columnIndex, float x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDouble( int columnIndex, double x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBigDecimal( int columnIndex, BigDecimal x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateString( int columnIndex, String x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBytes( int columnIndex, byte x[] ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDate( int columnIndex, java.sql.Date x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTime( int columnIndex, java.sql.Time x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTimestamp( int columnIndex, java.sql.Timestamp x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream( int columnIndex, java.io.InputStream x, int length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream( int columnIndex, java.io.InputStream x, int length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream( int columnIndex, java.io.Reader x, int length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject( int columnIndex, Object x, int scaleOrLength ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject( int columnIndex, Object x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNull( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBoolean( String columnLabel, boolean x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateByte( String columnLabel, byte x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateShort( String columnLabel, short x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateInt( String columnLabel, int x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateLong( String columnLabel, long x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateFloat( String columnLabel, float x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDouble( String columnLabel, double x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBigDecimal( String columnLabel, BigDecimal x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateString( String columnLabel, String x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBytes( String columnLabel, byte x[] ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateDate( String columnLabel, java.sql.Date x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTime( String columnLabel, java.sql.Time x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateTimestamp( String columnLabel, java.sql.Timestamp x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream( String columnLabel, java.io.InputStream x, int length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream( String columnLabel, java.io.InputStream x, int length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream( String columnLabel, java.io.Reader reader, int length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject( String columnLabel, Object x, int scaleOrLength ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateObject( String columnLabel, Object x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void insertRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void deleteRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void refreshRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void cancelRowUpdates() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void moveToInsertRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void moveToCurrentRow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object getObject( int columnIndex, java.util.Map<String, Class<?>> map ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Ref getRef( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Blob getBlob( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Clob getClob( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Array getArray( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Object getObject( String columnLabel, java.util.Map<String, Class<?>> map ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Ref getRef( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Blob getBlob( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Clob getClob( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Array getArray( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Date getDate( int columnIndex, Calendar cal ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Date getDate( String columnLabel, Calendar cal ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Time getTime( int columnIndex, Calendar cal ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Time getTime( String columnLabel, Calendar cal ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Timestamp getTimestamp( int columnIndex, Calendar cal ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.sql.Timestamp getTimestamp( String columnLabel, Calendar cal ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.net.URL getURL( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.net.URL getURL( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRef( int columnIndex, java.sql.Ref x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRef( String columnLabel, java.sql.Ref x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob( int columnIndex, java.sql.Blob x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob( String columnLabel, java.sql.Blob x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob( int columnIndex, java.sql.Clob x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob( String columnLabel, java.sql.Clob x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateArray( int columnIndex, java.sql.Array x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateArray( String columnLabel, java.sql.Array x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public RowId getRowId( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public RowId getRowId( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRowId( int columnIndex, RowId x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateRowId( String columnLabel, RowId x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isClosed() throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNString( int columnIndex, String nString ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNString( String columnLabel, String nString ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob( int columnIndex, NClob nClob ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob( String columnLabel, NClob nClob ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public NClob getNClob( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public NClob getNClob( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SQLXML getSQLXML( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public SQLXML getSQLXML( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateSQLXML( int columnIndex, SQLXML xmlObject ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateSQLXML( String columnLabel, SQLXML xmlObject ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getNString( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getNString( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.Reader getNCharacterStream( int columnIndex ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public java.io.Reader getNCharacterStream( String columnLabel ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream( int columnIndex, java.io.Reader x, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream( String columnLabel, java.io.Reader reader, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream( int columnIndex, java.io.InputStream x, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream( int columnIndex, java.io.InputStream x, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream( int columnIndex, java.io.Reader x, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream( String columnLabel, java.io.InputStream x, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream( String columnLabel, java.io.InputStream x, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream( String columnLabel, java.io.Reader reader, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob( int columnIndex, InputStream inputStream, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob( String columnLabel, InputStream inputStream, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob( int columnIndex, Reader reader, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob( String columnLabel, Reader reader, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob( int columnIndex, Reader reader, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob( String columnLabel, Reader reader, long length ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream( int columnIndex, java.io.Reader x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNCharacterStream( String columnLabel, java.io.Reader reader ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream( int columnIndex, java.io.InputStream x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream( int columnIndex, java.io.InputStream x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream( int columnIndex, java.io.Reader x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateAsciiStream( String columnLabel, java.io.InputStream x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBinaryStream( String columnLabel, java.io.InputStream x ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateCharacterStream( String columnLabel, java.io.Reader reader ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob( int columnIndex, InputStream inputStream ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateBlob( String columnLabel, InputStream inputStream ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob( int columnIndex, Reader reader ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateClob( String columnLabel, Reader reader ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob( int columnIndex, Reader reader ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void updateNClob( String columnLabel, Reader reader ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public <T> T getObject( int columnIndex, Class<T> type ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public <T> T getObject( String columnLabel, Class<T> type ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap( Class<T> iface ) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor( Class<?> iface ) throws SQLException {
		throw new UnsupportedOperationException();
	}

}
