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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** TAR output stream. */
public class TarOutputStream extends OutputStream {

	private final OutputStream	output;
	private long				bytesWritten;
	private long				currentFileSize;
	private TarEntry			currentEntry;

	/** Creates a TAR output stream. */
	public TarOutputStream( OutputStream output ) {
		this.output = output;
	}

	/**
	 * Writes the next entry header and selects it as the current entry.
	 *
	 * @param entry The entry to write
	 * 
	 * @throws IOException If the previous entry is incomplete or the header cannot be written
	 */
	public void putNextEntry( TarEntry entry ) throws IOException {
		closeCurrentEntry();
		byte[] entryName = entry.getName().getBytes( StandardCharsets.UTF_8 );
		if ( entryName.length > TarHeader.NAMELEN + TarHeader.USTAR_FILENAME_PREFIX || entry.getName()
		    .substring( Math.max( 0, entry.getName().lastIndexOf( '/' ) + 1 ) ).getBytes( StandardCharsets.UTF_8 ).length > TarHeader.NAMELEN ) {
			writeLongNameEntry( entryName );
		}
		writeEntryHeader( entry );
	}

	/** Writes one normal TAR entry header and selects it as current. */
	private void writeEntryHeader( TarEntry entry ) throws IOException {
		byte[] header = new byte[ TarConstants.HEADER_BLOCK ];
		entry.writeEntryHeader( header );
		write( header );
		currentEntry = entry;
	}

	/** Writes a GNU long-name metadata entry. */
	private void writeLongNameEntry( byte[] entryName ) throws IOException {
		TarHeader longNameHeader = TarHeader.createHeader( "././@LongLink", entryName.length + 1L, 0, false, 0644 );
		longNameHeader.typeflag = TarHeader.LF_GNU_LONGNAME;
		TarEntry longNameEntry = new TarEntry( longNameHeader );
		writeEntryHeader( longNameEntry );
		write( entryName );
		write( 0 );
		closeCurrentEntry();
	}

	/**
	 * Writes one byte to the underlying TAR stream.
	 *
	 * @param value The byte value
	 * 
	 * @throws IOException If the underlying stream cannot be written
	 */
	@Override
	public void write( int value ) throws IOException {
		output.write( value );
		bytesWritten++;
		if ( currentEntry != null )
			currentFileSize++;
	}

	/**
	 * Writes bytes to the current TAR entry.
	 *
	 * @param buffer The source buffer
	 * @param offset The source offset
	 * @param length The number of bytes to write
	 * 
	 * @throws IOException If the entry size is exceeded or the stream cannot be written
	 */
	@Override
	public void write( byte[] buffer, int offset, int length ) throws IOException {
		if ( currentEntry != null && currentFileSize + length > currentEntry.getSize() )
			throw new IOException( "TAR entry exceeds declared size" );
		output.write( buffer, offset, length );
		bytesWritten += length;
		if ( currentEntry != null )
			currentFileSize += length;
	}

	/**
	 * Completes the current entry and writes record padding.
	 *
	 * @throws IOException If the current entry is incomplete
	 */
	private void closeCurrentEntry() throws IOException {
		if ( currentEntry == null )
			return;
		if ( currentFileSize < currentEntry.getSize() )
			throw new IOException( "TAR entry was not fully written" );
		currentEntry	= null;
		currentFileSize	= 0;
		int padding = ( int ) ( ( TarConstants.DATA_BLOCK - bytesWritten % TarConstants.DATA_BLOCK ) % TarConstants.DATA_BLOCK );
		if ( padding > 0 )
			write( new byte[ padding ] );
	}

	/**
	 * Writes the TAR end markers and closes the underlying stream.
	 *
	 * @throws IOException If the stream cannot be closed
	 */
	@Override
	public void close() throws IOException {
		closeCurrentEntry();
		output.write( new byte[ TarConstants.EOF_BLOCK ] );
		output.close();
	}
}
