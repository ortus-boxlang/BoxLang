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
 *
 * Vendored and adapted from https://github.com/xiaoxindada/jtar.
 * Original JTar copyright 2012 Kamran Zafar; Apache License 2.0.
 */
package ortus.boxlang.runtime.util.jtar;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** TAR input stream. */
public class TarInputStream extends FilterInputStream {

	private TarEntry	currentEntry;
	private long		currentFileSize;
	private long		bytesRead;
	private boolean		defaultSkip;
	private String		pendingName;

	/** Creates a TAR input stream. */
	public TarInputStream( InputStream input ) {
		super( input );
	}

	/**
	 * Reads one byte from the current TAR entry.
	 *
	 * @return The byte read, or {@code -1} at the end of the entry
	 * 
	 * @throws IOException If the underlying stream cannot be read
	 */
	@Override
	public int read() throws IOException {
		byte[] buffer = new byte[ 1 ];
		return read( buffer, 0, 1 ) < 0 ? -1 : buffer[ 0 ] & 0xFF;
	}

	/**
	 * Reads bytes from the current TAR entry, bounded by its declared size.
	 *
	 * @param buffer The destination buffer
	 * @param offset The destination offset
	 * @param length The maximum number of bytes to read
	 * 
	 * @return The number of bytes read, or {@code -1} at the end of the entry
	 * 
	 * @throws IOException If the underlying stream cannot be read
	 */
	@Override
	public int read( byte[] buffer, int offset, int length ) throws IOException {
		if ( currentEntry != null ) {
			long remaining = currentEntry.getSize() - currentFileSize;
			if ( remaining <= 0 )
				return -1;
			length = ( int ) Math.min( length, remaining );
		}
		int read = super.read( buffer, offset, length );
		if ( read > 0 ) {
			currentFileSize	+= currentEntry == null ? 0 : read;
			bytesRead		+= read;
		}
		return read;
	}

	/**
	 * Returns the next TAR entry, processing GNU long-name and PAX path records.
	 *
	 * @return The next entry, or {@code null} at the end of the archive
	 * 
	 * @throws IOException If the archive is malformed or cannot be read
	 */
	public TarEntry getNextEntry() throws IOException {
		while ( true ) {
			closeCurrentEntry();
			byte[]	header	= new byte[ TarConstants.HEADER_BLOCK ];
			int		offset	= 0;
			while ( offset < header.length ) {
				int read = super.read( header, offset, header.length - offset );
				if ( read < 0 )
					break;
				offset		+= read;
				bytesRead	+= read;
			}
			boolean empty = true;
			for ( byte value : header ) {
				if ( value != 0 ) {
					empty = false;
					break;
				}
			}
			if ( empty )
				return null;
			currentEntry = new TarEntry( header );
			if ( currentEntry.getTypeFlag() == TarHeader.LF_GNU_LONGNAME || currentEntry.getTypeFlag() == TarHeader.LF_PAX_EXTENDED
			    || currentEntry.getTypeFlag() == TarHeader.LF_PAX_GLOBAL ) {
				byte[] data = readCurrentEntryData();
				if ( currentEntry.getTypeFlag() == TarHeader.LF_GNU_LONGNAME ) {
					String	longName	= new String( data, StandardCharsets.UTF_8 );
					int		terminator	= longName.indexOf( '\u0000' );
					pendingName = terminator < 0 ? longName : longName.substring( 0, terminator );
				} else if ( currentEntry.getTypeFlag() == TarHeader.LF_PAX_EXTENDED || currentEntry.getTypeFlag() == TarHeader.LF_PAX_GLOBAL ) {
					String paxPath = parsePaxPath( new String( data, StandardCharsets.UTF_8 ) );
					if ( paxPath != null )
						pendingName = paxPath;
				}
				continue;
			}
			if ( pendingName != null ) {
				currentEntry.setName( pendingName );
				pendingName = null;
			}
			return currentEntry;
		}
	}

	/**
	 * Reads the complete current extended-header payload.
	 *
	 * @return The extended-header bytes
	 * 
	 * @throws IOException If the payload is truncated
	 */
	private byte[] readCurrentEntryData() throws IOException {
		byte[]	data	= new byte[ ( int ) currentEntry.getSize() ];
		int		offset	= 0;
		while ( offset < data.length ) {
			int read = read( data, offset, data.length - offset );
			if ( read < 0 )
				throw new IOException( "Unexpected end of TAR extended header" );
			offset += read;
		}
		return data;
	}

	/**
	 * Parses a length-prefixed PAX payload for its path attribute.
	 *
	 * @param data The decoded PAX payload
	 * 
	 * @return The PAX path, or {@code null} when absent or malformed
	 */
	private String parsePaxPath( String data ) {
		int offset = 0;
		while ( offset < data.length() ) {
			int separator = data.indexOf( ' ', offset );
			if ( separator < 0 )
				break;
			int recordLength;
			try {
				recordLength = Integer.parseInt( data.substring( offset, separator ) );
			} catch ( NumberFormatException e ) {
				break;
			}
			if ( recordLength <= 0 || offset + recordLength > data.length() )
				break;
			String record = data.substring( separator + 1, offset + recordLength );
			if ( record.startsWith( "path=" ) )
				return record.substring( 5 ).replaceFirst( "\\n$", "" );
			offset += recordLength;
		}
		return null;
	}

	/**
	 * Finishes the current entry and consumes its data padding.
	 *
	 * @throws IOException If the entry is truncated
	 */
	private void closeCurrentEntry() throws IOException {
		if ( currentEntry == null )
			return;
		long remaining = currentEntry.getSize() - currentFileSize;
		while ( remaining > 0 ) {
			long skipped = skip( remaining );
			if ( skipped <= 0 )
				throw new IOException( "Possible tar file corruption" );
			remaining -= skipped;
		}
		currentEntry	= null;
		currentFileSize	= 0;
		long padding = ( TarConstants.DATA_BLOCK - bytesRead % TarConstants.DATA_BLOCK ) % TarConstants.DATA_BLOCK;
		if ( padding > 0 )
			skip( padding );
	}

	/**
	 * Skips bytes in the current entry or underlying TAR stream.
	 *
	 * @param count The number of bytes to skip
	 * 
	 * @return The number of bytes skipped
	 * 
	 * @throws IOException If the underlying stream cannot be skipped
	 */
	@Override
	public long skip( long count ) throws IOException {
		if ( defaultSkip ) {
			long skipped = super.skip( count );
			bytesRead += skipped;
			return skipped;
		}
		long	left	= count;
		byte[]	buffer	= new byte[ 2048 ];
		while ( left > 0 ) {
			int read = read( buffer, 0, ( int ) Math.min( left, buffer.length ) );
			if ( read < 0 )
				break;
			left -= read;
		}
		return count - left;
	}

	/**
	 * Returns the number of bytes consumed from the underlying TAR stream.
	 *
	 * @return The current stream offset in bytes
	 */
	public long getCurrentOffset() {
		return bytesRead;
	}

	/** Enables the underlying stream skip behavior. */
	public void setDefaultSkip( boolean value ) {
		defaultSkip = value;
	}

	/** Returns whether underlying skip behavior is enabled. */
	public boolean isDefaultSkip() {
		return defaultSkip;
	}
}
