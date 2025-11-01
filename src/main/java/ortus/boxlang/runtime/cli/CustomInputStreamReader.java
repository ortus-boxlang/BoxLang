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
package ortus.boxlang.runtime.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * A specialized InputStream reader that reads the minimal number of bytes needed
 * to decode characters. This is essential for terminal input handling where
 * reading ahead can cause problems.
 *
 * <p>
 * This implementation is based on the JLine3 InputStreamReader approach and provides:
 * </p>
 * <ul>
 * <li>Minimal byte reading - only reads what's needed for character decoding</li>
 * <li>Proper UTF-8 and other charset support</li>
 * <li>Cross-platform compatibility (Windows, Linux, macOS)</li>
 * <li>Non-blocking ready checks</li>
 * </ul>
 *
 * <p>
 * Unlike the standard InputStreamReader, this implementation doesn't buffer ahead,
 * which is critical for character-by-character terminal input processing.
 * </p>
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.6.0
 */
public class CustomInputStreamReader {

	/**
	 * Buffer size for byte reading - kept small to minimize read-ahead
	 */
	private static final int	BUFFER_SIZE	= 4;

	/**
	 * The underlying input stream
	 */
	private InputStream			in;

	/**
	 * Character decoder for the current charset
	 */
	private CharsetDecoder		decoder;

	/**
	 * Byte buffer for accumulating bytes before decoding
	 */
	private ByteBuffer			bytes;

	/**
	 * Flag indicating if we've reached the end of input
	 */
	private boolean				endOfInput	= false;

	/**
	 * Constructs a new CustomInputStreamReader using the default charset.
	 *
	 * @param in the input stream from which to read bytes
	 */
	public CustomInputStreamReader( InputStream in ) {
		this( in, Charset.defaultCharset() );
	}

	/**
	 * Constructs a new CustomInputStreamReader using the specified charset.
	 *
	 * @param in      the input stream from which to read bytes
	 * @param charset the charset to use for decoding
	 */
	public CustomInputStreamReader( InputStream in, Charset charset ) {
		this.in			= in;
		this.decoder	= charset.newDecoder()
		    .onMalformedInput( CodingErrorAction.REPLACE )
		    .onUnmappableCharacter( CodingErrorAction.REPLACE );
		this.bytes		= ByteBuffer.allocate( BUFFER_SIZE );
		this.bytes.limit( 0 ); // Start with no bytes available
	}

	/**
	 * Reads a single byte from the input stream.
	 * This is the core method for reading raw bytes that will be decoded into characters.
	 *
	 * @return the byte read (0-255), or -1 if end of stream
	 *
	 * @throws IOException if an I/O error occurs
	 */
	public int readByte() throws IOException {
		if ( in == null ) {
			throw new IOException( "Reader is closed" );
		}

		int b = in.read();
		if ( b == -1 ) {
			endOfInput = true;
		}
		return b;
	}

	/**
	 * Reads a single character from the input stream, properly decoding multi-byte sequences.
	 * This method handles UTF-8 and other multi-byte character encodings correctly.
	 *
	 * @return the character read, or -1 if end of stream
	 *
	 * @throws IOException if an I/O error occurs
	 */
	public int readChar() throws IOException {
		if ( in == null ) {
			throw new IOException( "Reader is closed" );
		}

		char[]	buf	= new char[ 2 ];
		int		nb	= read( buf, 0, 2 );

		if ( nb > 0 ) {
			return buf[ 0 ];
		} else {
			return -1;
		}
	}

	/**
	 * Reads characters into an array, properly handling character encoding.
	 * This method reads the minimal number of bytes needed to fill the character buffer.
	 *
	 * @param cbuf   the destination buffer
	 * @param offset the offset at which to start storing characters
	 * @param length the maximum number of characters to read
	 *
	 * @return the number of characters read, or -1 if end of stream
	 *
	 * @throws IOException if an I/O error occurs
	 */
	public int read( char[] cbuf, int offset, int length ) throws IOException {
		if ( in == null ) {
			throw new IOException( "Reader is closed" );
		}

		if ( offset < 0 || offset > cbuf.length - length || length < 0 ) {
			throw new IndexOutOfBoundsException();
		}

		if ( length == 0 ) {
			return 0;
		}

		CharBuffer	out			= CharBuffer.wrap( cbuf, offset, length );
		CoderResult	result		= CoderResult.UNDERFLOW;
		boolean		needInput	= !bytes.hasRemaining();

		// Keep reading until we have at least one character or hit EOF
		while ( out.hasRemaining() ) {
			// Fill the buffer if needed
			if ( needInput ) {
				// Check if input is available (non-blocking check)
				try {
					if ( ( in.available() == 0 ) && ( out.position() > offset ) ) {
						// We have some characters already, return without blocking
						break;
					}
				} catch ( IOException e ) {
					// available() failed, just try to read
				}

				// Read one byte at a time to minimize read-ahead
				int	off		= bytes.arrayOffset() + bytes.limit();
				int	wasRead	= in.read( bytes.array(), off, 1 );

				if ( wasRead == -1 ) {
					endOfInput = true;
					break;
				} else if ( wasRead == 0 ) {
					break;
				}

				bytes.limit( bytes.limit() + wasRead );
			}

			// Decode bytes into characters
			result = decoder.decode( bytes, out, false );

			if ( result.isUnderflow() ) {
				// Compact the buffer if no space left
				if ( bytes.limit() == bytes.capacity() ) {
					bytes.compact();
					bytes.limit( bytes.position() );
					bytes.position( 0 );
				}
				needInput = true;
			} else if ( result.isOverflow() ) {
				// Output buffer is full
				break;
			} else {
				// Error occurred
				break;
			}
		}

		// Handle end of input
		if ( result == CoderResult.UNDERFLOW && endOfInput ) {
			result = decoder.decode( bytes, out, true );
			decoder.flush( out );
			decoder.reset();
		}

		// Handle decoder errors
		if ( result.isMalformed() || result.isUnmappable() ) {
			// Replace with the replacement character
			if ( out.hasRemaining() ) {
				out.put( '\ufffd' ); // Unicode replacement character
			}
		}

		return out.position() - offset == 0 ? -1 : out.position() - offset;
	}

	/**
	 * Checks if the reader is ready to read without blocking.
	 *
	 * @return true if data is available, false otherwise
	 *
	 * @throws IOException if an I/O error occurs
	 */
	public boolean ready() throws IOException {
		if ( in == null ) {
			throw new IOException( "Reader is closed" );
		}

		try {
			return bytes.hasRemaining() || in.available() > 0;
		} catch ( IOException e ) {
			return false;
		}
	}

	/**
	 * Closes the reader and releases resources.
	 * Note: This does NOT close the underlying input stream, as it may be System.in
	 * which should not be closed.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	public void close() throws IOException {
		decoder	= null;
		in		= null;
	}

	/**
	 * Gets the name of the character encoding being used.
	 *
	 * @return the encoding name, or null if closed
	 */
	public String getEncoding() {
		if ( decoder == null ) {
			return null;
		}
		return decoder.charset().name();
	}
}
