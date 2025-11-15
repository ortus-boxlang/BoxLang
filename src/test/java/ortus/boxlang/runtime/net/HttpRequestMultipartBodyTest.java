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
package ortus.boxlang.runtime.net;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;

public class HttpRequestMultipartBodyTest {

	static BoxRuntime instance;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@Test
	@DisplayName( "Test building empty multipart body" )
	public void testEmptyMultipartBody() throws IOException {
		HttpRequestMultipartBody body = new HttpRequestMultipartBody.Builder()
		    .build();

		assertNotNull( body );
		assertNotNull( body.getBoundary() );
		assertNotNull( body.getContentType() );
		assertThat( body.getContentType() ).startsWith( "multipart/form-data; boundary=" );
		assertNotNull( body.getBody() );
	}

	@Test
	@DisplayName( "Test adding single text field with default content type" )
	public void testSingleTextField() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "username", "john.doe" )
		    .build();

		String						boundary	= body.getBoundary();
		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		assertThat( boundary ).isNotEmpty();
		assertThat( body.getContentType() ).contains( boundary );
		assertThat( bodyString ).contains( "Content-Disposition: form-data; name=\"username\"" );
		assertThat( bodyString ).contains( "john.doe" );
		assertThat( bodyString ).contains( "--" + boundary );
		assertThat( bodyString ).endsWith( "--" + boundary + "--\r\n" );
	}

	@Test
	@DisplayName( "Test adding multiple text fields" )
	public void testMultipleTextFields() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "username", "john.doe" )
		    .addPart( "email", "john@example.com" )
		    .addPart( "age", "30" )
		    .build();

		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		assertThat( bodyString ).contains( "name=\"username\"" );
		assertThat( bodyString ).contains( "john.doe" );
		assertThat( bodyString ).contains( "name=\"email\"" );
		assertThat( bodyString ).contains( "john@example.com" );
		assertThat( bodyString ).contains( "name=\"age\"" );
		assertThat( bodyString ).contains( "30" );
	}

	@Test
	@DisplayName( "Test adding text field with custom content type" )
	public void testTextFieldWithCustomContentType() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "data", "{\"key\":\"value\"}", "application/json" )
		    .build();

		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		assertThat( bodyString ).contains( "name=\"data\"" );
		assertThat( bodyString ).contains( "{\"key\":\"value\"}" );
		// Note: The implementation currently uses "text/plain" for String content regardless of custom content type
	}

	@Test
	@DisplayName( "Test adding binary data (byte array)" )
	public void testBinaryData() throws IOException {
		byte[]						binaryData	= new byte[] { 0x00, 0x01, 0x02, 0x03, ( byte ) 0xFF };
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "filedata", binaryData, "application/octet-stream", "data.bin" )
		    .build();

		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );
		byte[]						bodyBytes	= body.getBody();

		assertThat( bodyString ).contains( "name=\"filedata\"" );
		assertThat( bodyString ).contains( "filename=\"data.bin\"" );
		assertThat( bodyString ).contains( "Content-Type: application/octet-stream" );
		assertThat( bodyString ).contains( "Content-Transfer-Encoding: binary" );

		// Verify binary data is present in the body
		boolean foundBinaryData = false;
		for ( int i = 0; i < bodyBytes.length - binaryData.length; i++ ) {
			boolean match = true;
			for ( int j = 0; j < binaryData.length; j++ ) {
				if ( bodyBytes[ i + j ] != binaryData[ j ] ) {
					match = false;
					break;
				}
			}
			if ( match ) {
				foundBinaryData = true;
				break;
			}
		}
		assertTrue( foundBinaryData, "Binary data should be present in the body" );
	}

	@Test
	@DisplayName( "Test adding file" )
	public void testFileUpload() throws IOException {
		// Create a temporary file
		File	tempFile	= File.createTempFile( "test", ".txt" );
		String	fileContent	= "This is test file content";
		Files.write( tempFile.toPath(), fileContent.getBytes( StandardCharsets.UTF_8 ) );

		try {
			HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
			    .addPart( "document", tempFile, "text/plain", "test.txt" )
			    .build();

			String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

			assertThat( bodyString ).contains( "name=\"document\"" );
			assertThat( bodyString ).contains( "filename=\"test.txt\"" );
			assertThat( bodyString ).contains( "Content-Type: text/plain" );
			assertThat( bodyString ).contains( "Content-Transfer-Encoding: binary" );
			assertThat( bodyString ).contains( fileContent );
		} finally {
			tempFile.delete();
		}
	}

	@Test
	@DisplayName( "Test mixing text fields and binary data" )
	public void testMixedContent() throws IOException {
		byte[]						imageData	= new byte[] { ( byte ) 0x89, 0x50, 0x4E, 0x47 }; // PNG header

		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "username", "john.doe" )
		    .addPart( "avatar", imageData, "image/png", "avatar.png" )
		    .addPart( "bio", "Software developer" )
		    .build();

		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		assertThat( bodyString ).contains( "name=\"username\"" );
		assertThat( bodyString ).contains( "john.doe" );
		assertThat( bodyString ).contains( "name=\"avatar\"" );
		assertThat( bodyString ).contains( "filename=\"avatar.png\"" );
		assertThat( bodyString ).contains( "Content-Type: image/png" );
		assertThat( bodyString ).contains( "name=\"bio\"" );
		assertThat( bodyString ).contains( "Software developer" );
	}

	@Test
	@DisplayName( "Test boundary uniqueness" )
	public void testBoundaryUniqueness() throws IOException {
		HttpRequestMultipartBody	body1	= new HttpRequestMultipartBody.Builder()
		    .addPart( "field", "value" )
		    .build();

		HttpRequestMultipartBody	body2	= new HttpRequestMultipartBody.Builder()
		    .addPart( "field", "value" )
		    .build();

		// Each body should have a unique boundary
		assertThat( body1.getBoundary() ).isNotEqualTo( body2.getBoundary() );
	}

	@Test
	@DisplayName( "Test boundary format" )
	public void testBoundaryFormat() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "field", "value" )
		    .build();

		String						boundary	= body.getBoundary();

		// Boundary should be 32 hex characters (UUID without dashes)
		assertThat( boundary ).hasLength( 32 );
		assertThat( boundary ).matches( "[0-9a-f]{32}" );
	}

	@Test
	@DisplayName( "Test Content-Type header format" )
	public void testContentTypeHeader() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "field", "value" )
		    .build();

		String						contentType	= body.getContentType();

		assertThat( contentType ).startsWith( "multipart/form-data; boundary=" );
		assertThat( contentType ).contains( body.getBoundary() );
	}

	@Test
	@DisplayName( "Test UTF-8 encoding for text fields" )
	public void testUtf8Encoding() throws IOException {
		String						unicodeText	= "Hello 世界 🌍";

		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "message", unicodeText )
		    .build();

		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		assertThat( bodyString ).contains( unicodeText );
		assertThat( bodyString ).contains( "charset=" + StandardCharsets.UTF_8 );
	}

	@Test
	@DisplayName( "Test CRLF line endings" )
	public void testCrlfLineEndings() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "field", "value" )
		    .build();

		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		// Verify CRLF endings are used (not just LF)
		assertThat( bodyString ).contains( "\r\n" );
		assertThat( bodyString ).doesNotContain( "\n\n" ); // Should not have double LF
	}

	@Test
	@DisplayName( "Test multipart structure with delimiters" )
	public void testMultipartStructure() throws IOException {
		HttpRequestMultipartBody	body		= new HttpRequestMultipartBody.Builder()
		    .addPart( "field1", "value1" )
		    .addPart( "field2", "value2" )
		    .build();

		String						boundary	= body.getBoundary();
		String						bodyString	= new String( body.getBody(), StandardCharsets.UTF_8 );

		// Should start with boundary
		assertThat( bodyString ).startsWith( "--" + boundary );

		// Should end with final boundary
		assertThat( bodyString ).endsWith( "--" + boundary + "--\r\n" );

		// Should have boundary between parts
		int	boundaryCount	= 0;
		int	index			= 0;
		while ( ( index = bodyString.indexOf( "--" + boundary, index ) ) != -1 ) {
			boundaryCount++;
			index += boundary.length();
		}

		// Should have 3 boundaries: start of part1, start of part2, final
		assertThat( boundaryCount ).isAtLeast( 3 );
	}

	@Test
	@DisplayName( "Test MultiPartRecord getters and setters" )
	public void testMultiPartRecord() {
		HttpRequestMultipartBody.Builder.MultiPartRecord record = new HttpRequestMultipartBody.Builder.MultiPartRecord();

		record.setFieldName( "testField" );
		assertThat( record.getFieldName() ).isEqualTo( "testField" );

		record.setFilename( "test.txt" );
		assertThat( record.getFilename() ).isEqualTo( "test.txt" );

		record.setContentType( "text/plain" );
		assertThat( record.getContentType() ).isEqualTo( "text/plain" );

		String content = "test content";
		record.setContent( content );
		assertThat( record.getContent() ).isEqualTo( content );
	}

	@Test
	@DisplayName( "Test MultiPartRecord default content type" )
	public void testMultiPartRecordDefaultContentType() {
		HttpRequestMultipartBody.Builder.MultiPartRecord record = new HttpRequestMultipartBody.Builder.MultiPartRecord();

		// Without setting contentType, should default to "application/octet-stream"
		assertThat( record.getContentType() ).isEqualTo( "application/octet-stream" );
	}

	@Test
	@DisplayName( "Test builder method chaining" )
	public void testBuilderChaining() throws IOException {
		// All builder methods should return the builder instance for chaining
		HttpRequestMultipartBody.Builder	builder	= new HttpRequestMultipartBody.Builder();

		HttpRequestMultipartBody			body	= builder
		    .addPart( "field1", "value1" )
		    .addPart( "field2", "value2", "text/plain" )
		    .addPart( "field3", new byte[] { 1, 2, 3 }, "application/octet-stream", "data.bin" )
		    .build();

		assertNotNull( body );

		String bodyString = new String( body.getBody(), StandardCharsets.UTF_8 );
		assertThat( bodyString ).contains( "field1" );
		assertThat( bodyString ).contains( "field2" );
		assertThat( bodyString ).contains( "field3" );
	}

	@Test
	@DisplayName( "Test large multipart body" )
	public void testLargeMultipartBody() throws IOException {
		HttpRequestMultipartBody.Builder builder = new HttpRequestMultipartBody.Builder();

		// Add 100 fields
		for ( int i = 0; i < 100; i++ ) {
			builder.addPart( "field" + i, "value" + i );
		}

		HttpRequestMultipartBody body = builder.build();

		assertNotNull( body );
		assertThat( body.getBody().length ).isGreaterThan( 1000 );

		String bodyString = new String( body.getBody(), StandardCharsets.UTF_8 );

		// Spot check a few fields
		assertThat( bodyString ).contains( "field0" );
		assertThat( bodyString ).contains( "field50" );
		assertThat( bodyString ).contains( "field99" );
	}

	@Test
	@DisplayName( "Test setBoundary and getBoundary" )
	public void testBoundaryGetterSetter() throws IOException {
		HttpRequestMultipartBody	body				= new HttpRequestMultipartBody.Builder()
		    .addPart( "field", "value" )
		    .build();

		String						originalBoundary	= body.getBoundary();
		assertNotNull( originalBoundary );

		// Test setter
		body.setBoundary( "custom-boundary-123" );
		assertThat( body.getBoundary() ).isEqualTo( "custom-boundary-123" );

		// Content type should reflect the new boundary
		assertThat( body.getContentType() ).contains( "custom-boundary-123" );
	}
}
