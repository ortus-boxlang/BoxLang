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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CustomInputStreamReaderTest {

	@Test
	@DisplayName( "Test reading single ASCII byte" )
	public void testReadSingleByte() throws IOException {
		byte[]					input	= new byte[] { 65 }; // 'A'
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		int						b		= reader.readByte();
		assertThat( b ).isEqualTo( 65 );

		reader.close();
	}

	@Test
	@DisplayName( "Test reading multiple ASCII bytes" )
	public void testReadMultipleBytes() throws IOException {
		byte[]					input	= "Hello".getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		assertThat( reader.readByte() ).isEqualTo( 72 );	// 'H'
		assertThat( reader.readByte() ).isEqualTo( 101 );	// 'e'
		assertThat( reader.readByte() ).isEqualTo( 108 );	// 'l'
		assertThat( reader.readByte() ).isEqualTo( 108 );	// 'l'
		assertThat( reader.readByte() ).isEqualTo( 111 );	// 'o'

		reader.close();
	}

	@Test
	@DisplayName( "Test reading EOF" )
	public void testReadEOF() throws IOException {
		byte[]					input	= new byte[] { 65 }; // 'A'
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		assertThat( reader.readByte() ).isEqualTo( 65 );
		assertThat( reader.readByte() ).isEqualTo( -1 );	// EOF
		assertThat( reader.readByte() ).isEqualTo( -1 );	// Still EOF

		reader.close();
	}

	@Test
	@DisplayName( "Test reading single ASCII character" )
	public void testReadSingleChar() throws IOException {
		byte[]					input	= "A".getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		int						c		= reader.readChar();
		assertThat( c ).isEqualTo( 'A' );

		reader.close();
	}

	@Test
	@DisplayName( "Test reading UTF-8 multi-byte character" )
	public void testReadUTF8MultiByteChar() throws IOException {
		// Test with emoji and special characters
		byte[]					input	= "😀".getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in, StandardCharsets.UTF_8 );

		int						c		= reader.readChar();
		assertThat( c ).isEqualTo( 0xD83D );				// High surrogate of emoji

		reader.close();
	}

	@Test
	@DisplayName( "Test reading character array" )
	public void testReadCharArray() throws IOException {
		byte[]					input	= "Hello World".getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		char[]					buffer	= new char[ 5 ];
		int						read	= reader.read( buffer, 0, 5 );

		assertThat( read ).isEqualTo( 5 );
		assertThat( new String( buffer ) ).isEqualTo( "Hello" );

		reader.close();
	}

	@Test
	@DisplayName( "Test reading character array with offset" )
	public void testReadCharArrayWithOffset() throws IOException {
		byte[]					input	= "Test".getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		char[]					buffer	= new char[ 10 ];
		int						read	= reader.read( buffer, 2, 4 );

		assertThat( read ).isEqualTo( 4 );
		assertThat( buffer[ 2 ] ).isEqualTo( 'T' );
		assertThat( buffer[ 3 ] ).isEqualTo( 'e' );
		assertThat( buffer[ 4 ] ).isEqualTo( 's' );
		assertThat( buffer[ 5 ] ).isEqualTo( 't' );

		reader.close();
	}

	@Test
	@DisplayName( "Test ready() method" )
	public void testReady() throws IOException {
		byte[]					input	= "Test".getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		// Should be ready when data is available
		assertThat( reader.ready() ).isTrue();

		// Read all data
		reader.read( new char[ 4 ], 0, 4 );

		// Should not be ready after reading all data
		assertThat( reader.ready() ).isFalse();

		reader.close();
	}

	@Test
	@DisplayName( "Test getEncoding() method" )
	public void testGetEncoding() throws IOException {
		ByteArrayInputStream	in		= new ByteArrayInputStream( new byte[] { 65 } );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in, StandardCharsets.UTF_8 );

		assertThat( reader.getEncoding() ).isEqualTo( "UTF-8" );

		reader.close();
		assertThat( reader.getEncoding() ).isNull();
	}

	@Test
	@DisplayName( "Test reading with different charsets" )
	public void testDifferentCharsets() throws IOException {
		// Test with ISO-8859-1 (Latin-1)
		String					text	= "Test";
		byte[]					input	= text.getBytes( StandardCharsets.ISO_8859_1 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in, StandardCharsets.ISO_8859_1 );

		char[]					buffer	= new char[ 4 ];
		int						read	= reader.read( buffer, 0, 4 );

		assertThat( read ).isEqualTo( 4 );
		assertThat( new String( buffer ) ).isEqualTo( "Test" );

		reader.close();
	}

	@Test
	@DisplayName( "Test reading zero-length array" )
	public void testReadZeroLength() throws IOException {
		ByteArrayInputStream	in		= new ByteArrayInputStream( "Test".getBytes( StandardCharsets.UTF_8 ) );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		char[]					buffer	= new char[ 5 ];
		int						read	= reader.read( buffer, 0, 0 );

		assertThat( read ).isEqualTo( 0 );

		reader.close();
	}

	@Test
	@DisplayName( "Test exception on closed reader for readByte" )
	public void testReadByteAfterClose() throws IOException {
		ByteArrayInputStream	in		= new ByteArrayInputStream( new byte[] { 65 } );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		reader.close();

		try {
			reader.readByte();
			fail( "Should have thrown IOException" );
		} catch ( IOException e ) {
			assertThat( e.getMessage() ).contains( "closed" );
		}
	}

	@Test
	@DisplayName( "Test exception on closed reader for readChar" )
	public void testReadCharAfterClose() throws IOException {
		ByteArrayInputStream	in		= new ByteArrayInputStream( new byte[] { 65 } );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		reader.close();

		try {
			reader.readChar();
			fail( "Should have thrown IOException" );
		} catch ( IOException e ) {
			assertThat( e.getMessage() ).contains( "closed" );
		}
	}

	@Test
	@DisplayName( "Test exception on invalid array bounds" )
	public void testInvalidArrayBounds() throws IOException {
		ByteArrayInputStream	in		= new ByteArrayInputStream( "Test".getBytes( StandardCharsets.UTF_8 ) );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		char[]					buffer	= new char[ 5 ];

		try {
			reader.read( buffer, -1, 3 );
			fail( "Should have thrown IndexOutOfBoundsException" );
		} catch ( IndexOutOfBoundsException e ) {
			// Expected
		}

		try {
			reader.read( buffer, 0, 10 );
			fail( "Should have thrown IndexOutOfBoundsException" );
		} catch ( IndexOutOfBoundsException e ) {
			// Expected
		}

		reader.close();
	}

	@Test
	@DisplayName( "Test reading escape sequences (arrow keys)" )
	public void testReadEscapeSequences() throws IOException {
		// ESC [ A = Up arrow
		byte[]					input	= new byte[] { 27, '[', 'A' };
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		assertThat( reader.readByte() ).isEqualTo( 27 );	// ESC
		assertThat( reader.readByte() ).isEqualTo( '[' );
		assertThat( reader.readByte() ).isEqualTo( 'A' );

		reader.close();
	}

	@Test
	@DisplayName( "Test reading control characters" )
	public void testReadControlCharacters() throws IOException {
		// Test Ctrl+C (0x03), Tab (0x09), Enter (0x0D), Backspace (0x7F)
		byte[]					input	= new byte[] { 3, 9, 13, 127 };
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		assertThat( reader.readByte() ).isEqualTo( 3 );	// Ctrl+C
		assertThat( reader.readByte() ).isEqualTo( 9 );	// Tab
		assertThat( reader.readByte() ).isEqualTo( 13 );	// Enter
		assertThat( reader.readByte() ).isEqualTo( 127 );	// Backspace

		reader.close();
	}

	@Test
	@DisplayName( "Test reading mixed ASCII and UTF-8" )
	public void testReadMixedContent() throws IOException {
		// Mix of ASCII and UTF-8 characters
		String					text	= "Hello 世界";
		byte[]					input	= text.getBytes( StandardCharsets.UTF_8 );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in, StandardCharsets.UTF_8 );

		char[]					buffer	= new char[ 20 ];
		int						read	= reader.read( buffer, 0, 20 );

		assertThat( read ).isGreaterThan( 0 );
		String result = new String( buffer, 0, read );
		assertThat( result ).isEqualTo( "Hello 世界" );

		reader.close();
	}

	@Test
	@DisplayName( "Test default charset constructor" )
	public void testDefaultCharsetConstructor() throws IOException {
		byte[]					input	= "Test".getBytes( Charset.defaultCharset() );
		ByteArrayInputStream	in		= new ByteArrayInputStream( input );
		CustomInputStreamReader	reader	= new CustomInputStreamReader( in );

		assertThat( reader.getEncoding() ).isNotNull();
		assertThat( reader.getEncoding() ).isEqualTo( Charset.defaultCharset().name() );

		reader.close();
	}
}
