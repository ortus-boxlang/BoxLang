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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a multipart/form-data HTTP request body for uploading files and form fields.
 * <p>
 * This class provides a builder pattern for constructing multipart HTTP request bodies
 * that comply with RFC 2388 (multipart/form-data). It supports uploading:
 * <ul>
 * <li>Text fields with custom content types</li>
 * <li>Binary data (byte arrays)</li>
 * <li>Files from the filesystem</li>
 * <li>Arbitrary Java objects (serialized)</li>
 * </ul>
 * <p>
 * The body is constructed using the {@link Builder} class, which generates a unique
 * boundary string and assembles the parts into a properly formatted multipart message.
 * <p>
 * Example usage:
 * 
 * <pre>
 * 
 * HttpRequestMultipartBody body = HttpRequestMultipartBody.Builder()
 *     .addPart( "username", "john.doe" )
 *     .addPart( "avatar", myImageBytes, "image/png", "avatar.png" )
 *     .addPart( "document", myFile, "application/pdf", "report.pdf" )
 *     .build();
 *
 * String contentType = body.getContentType();
 * byte[] bodyBytes = body.getBody();
 * </pre>
 *
 * @see Builder
 */
public class HttpRequestMultipartBody {

	/**
	 * The encoded multipart body content as a byte array.
	 */
	private byte[]	bytes;

	/**
	 * The unique boundary string used to delimit parts in the multipart message.
	 * This boundary is randomly generated and must not appear in any part content.
	 */
	private String	boundary;

	/**
	 * Returns the boundary string used to delimit multipart sections.
	 *
	 * @return the boundary string
	 */
	public String getBoundary() {
		return boundary;
	}

	/**
	 * Sets the boundary string used to delimit multipart sections.
	 *
	 * @param boundary the boundary string to use
	 */
	public void setBoundary( String boundary ) {
		this.boundary = boundary;
	}

	/**
	 * Private constructor - use {@link Builder} to create instances.
	 *
	 * @param bytes    the encoded multipart body content
	 * @param boundary the boundary string used in the multipart message
	 */
	private HttpRequestMultipartBody( byte[] bytes, String boundary ) {
		this.bytes		= bytes;
		this.boundary	= boundary;
	}

	/**
	 * Returns the complete Content-Type header value for this multipart body.
	 * The returned value includes the boundary parameter required by the HTTP specification.
	 *
	 * @return Content-Type header value (e.g., "multipart/form-data; boundary=abc123")
	 */
	public String getContentType() {
		return "multipart/form-data; boundary=" + this.getBoundary();
	}

	/**
	 * Returns the encoded multipart body content as a byte array.
	 * This is the raw body content ready to be sent in an HTTP request.
	 *
	 * @return the multipart body as bytes
	 */
	public byte[] getBody() {
		return this.bytes;
	}

	/**
	 * Builder class for constructing {@link HttpRequestMultipartBody} instances using a fluent API.
	 * <p>
	 * The builder accumulates parts (text fields, binary data, files) and assembles them into
	 * a properly formatted RFC 2388 multipart/form-data body when {@link #build()} is called.
	 * <p>
	 * Each part can have:
	 * <ul>
	 * <li>A field name (required)</li>
	 * <li>Content (String, byte[], File, or any serializable Object)</li>
	 * <li>A content type (optional, defaults to "text/plain" for strings or "application/octet-stream" for binary)</li>
	 * <li>A filename (optional, used for file uploads)</li>
	 * </ul>
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * 
	 * HttpRequestMultipartBody body = new HttpRequestMultipartBody.Builder()
	 *     .addPart( "name", "John Doe" )
	 *     .addPart( "email", "john@example.com", "text/plain" )
	 *     .addPart( "photo", photoBytes, "image/jpeg", "photo.jpg" )
	 *     .build();
	 * </pre>
	 */
	public static class Builder {

		/**
		 * Default MIME type for text fields.
		 */
		private final String	DEFAULT_MIMETYPE	= "text/plain";

		/**
		 * Carriage return + line feed sequence required by multipart/form-data spec.
		 */
		private final String	CRLF				= "\r\n";

		/**
		 * Boundary delimiter prefix ("--").
		 */
		private final String	BOUNDARY_PREFIX		= "--";

		/**
		 * Represents a single part in a multipart/form-data request.
		 * Each part contains a field name, optional filename, content type, and the actual content.
		 */
		public static class MultiPartRecord {

			/**
			 * The form field name for this part.
			 */
			private String	fieldName;

			/**
			 * Optional filename for file upload parts.
			 */
			private String	filename;

			/**
			 * MIME type of the content.
			 */
			private String	contentType;

			/**
			 * The actual content (String, byte[], File, or serializable Object).
			 */
			private Object	content;

			/**
			 * Returns the form field name.
			 *
			 * @return the field name
			 */
			public String getFieldName() {
				return fieldName;
			}

			/**
			 * Sets the form field name.
			 *
			 * @param fieldName the field name
			 */
			public void setFieldName( String fieldName ) {
				this.fieldName = fieldName;
			}

			/**
			 * Returns the filename for file upload parts.
			 *
			 * @return the filename, or null if not a file upload
			 */
			public String getFilename() {
				return filename;
			}

			/**
			 * Sets the filename for file upload parts.
			 *
			 * @param filename the filename
			 */
			public void setFilename( String filename ) {
				this.filename = filename;
			}

			/**
			 * Returns the content type for this part.
			 * If not explicitly set, defaults to "application/octet-stream".
			 *
			 * @return the content type
			 */
			public String getContentType() {
				return contentType != null ? contentType : "application/octet-stream";
			}

			/**
			 * Sets the content type for this part.
			 *
			 * @param contentType the MIME type
			 */
			public void setContentType( String contentType ) {
				this.contentType = contentType;
			}

			/**
			 * Returns the content for this part.
			 *
			 * @return the content object
			 */
			public Object getContent() {
				return content;
			}

			/**
			 * Sets the content for this part.
			 *
			 * @param content the content (String, byte[], File, or any serializable Object)
			 */
			public void setContent( Object content ) {
				this.content = content;
			}
		}

		/**
		 * List of multipart records representing the parts to be included in the body.
		 */
		List<MultiPartRecord> parts;

		/**
		 * Constructs a new Builder with an empty parts list.
		 */
		public Builder() {
			this.parts = new ArrayList<>();
		}

		/**
		 * Adds a text field part with the default content type (text/plain).
		 *
		 * @param fieldName  the form field name
		 * @param fieldValue the text value
		 *
		 * @return this builder for method chaining
		 */
		public Builder addPart( String fieldName, String fieldValue ) {
			MultiPartRecord part = new MultiPartRecord();
			part.setFieldName( fieldName );
			part.setContent( fieldValue );
			part.setContentType( DEFAULT_MIMETYPE );
			this.parts.add( part );
			return this;
		}

		/**
		 * Adds a text field part with a custom content type.
		 *
		 * @param fieldName   the form field name
		 * @param fieldValue  the text value
		 * @param contentType the MIME type for this field
		 *
		 * @return this builder for method chaining
		 */
		public Builder addPart( String fieldName, String fieldValue, String contentType ) {
			MultiPartRecord part = new MultiPartRecord();
			part.setFieldName( fieldName );
			part.setContent( fieldValue );
			part.setContentType( contentType );
			this.parts.add( part );
			return this;
		}

		/**
		 * Adds a part with arbitrary content (file, binary data, or serializable object).
		 * This method is typically used for file uploads.
		 *
		 * @param fieldName   the form field name
		 * @param fieldValue  the content (byte[], File, or any serializable Object)
		 * @param contentType the MIME type for this content
		 * @param fileName    the filename to report (used for file uploads)
		 *
		 * @return this builder for method chaining
		 */
		public Builder addPart( String fieldName, Object fieldValue, String contentType, String fileName ) {
			MultiPartRecord part = new MultiPartRecord();
			part.setFieldName( fieldName );
			part.setContent( fieldValue );
			part.setContentType( contentType );
			part.setFilename( fileName );
			this.parts.add( part );
			return this;
		}

		/**
		 * Builds the multipart/form-data body from all accumulated parts.
		 * <p>
		 * This method:
		 * <ol>
		 * <li>Generates a unique boundary string</li>
		 * <li>Writes each part with proper headers and content encoding</li>
		 * <li>Handles different content types (String, byte[], File, Object)</li>
		 * <li>Adds the final boundary marker</li>
		 * </ol>
		 * <p>
		 * String content is written with UTF-8 encoding. Binary content (byte[], File) is
		 * written with binary transfer encoding. Other objects are serialized using Java
		 * object serialization.
		 *
		 * @return a fully constructed {@link HttpRequestMultipartBody}
		 *
		 * @throws IOException if an I/O error occurs while reading file content or serializing objects
		 */
		public HttpRequestMultipartBody build() throws IOException {
			String					boundary	= generateBoundary();
			ByteArrayOutputStream	out			= new ByteArrayOutputStream();
			PrintWriter				writer		= new PrintWriter( new OutputStreamWriter( out, StandardCharsets.UTF_8 ), true );
			for ( MultiPartRecord record : parts ) {
				writer.append( BOUNDARY_PREFIX + boundary ).append( CRLF );

				writer.append( "Content-Disposition: form-data; name=\"" + record.getFieldName() );
				if ( record.getFilename() != null ) {
					writer.append( "\"; filename=\"" + record.getFilename() );
				}
				writer.append( "\"" ).append( CRLF ).flush();

				Object content = record.getContent();
				if ( content instanceof String castString ) {
					writer.append( "Content-Type: text/plain; charset=" + StandardCharsets.UTF_8 ).append( CRLF );
					writer.append( CRLF ).append( castString ).flush();
				} else if ( content instanceof byte[] castBytes ) {
					writer.append( "Content-Type: " + record.getContentType() ).append( CRLF );
					writer.append( "Content-Transfer-Encoding: binary" ).append( CRLF );
					writer.append( CRLF ).flush();
					out.write( castBytes );
				} else if ( content instanceof File castFile ) {
					writer.append( "Content-Type: " + record.getContentType() ).append( CRLF );
					writer.append( "Content-Transfer-Encoding: binary" ).append( CRLF );
					writer.append( CRLF ).flush();
					Files.copy( castFile.toPath(), out );
				} else {
					writer.append( "Content-Type: " + record.getContentType() ).append( CRLF );
					writer.append( "Content-Transfer-Encoding: binary" ).append( CRLF );
					writer.append( CRLF ).flush();
					ObjectOutputStream objectOutputStream = new ObjectOutputStream( out );
					objectOutputStream.writeObject( content );
					objectOutputStream.flush();
				}
				out.flush(); // Important before continuing with writer!
				writer.append( CRLF ).flush(); // CRLF is important! It indicates end of boundary.
			}
			// End of multipart/form-data.
			writer.append( BOUNDARY_PREFIX + boundary + BOUNDARY_PREFIX ).append( CRLF ).flush();

			HttpRequestMultipartBody httpRequestMultipartBody = new HttpRequestMultipartBody( out.toByteArray(), boundary );
			return httpRequestMultipartBody;
		}

		/**
		 * Generates a unique boundary string using a UUID.
		 * The boundary is a random 32-character hexadecimal string that is highly
		 * unlikely to appear in any part content.
		 *
		 * @return a unique boundary string
		 */
		private String generateBoundary() {
			String uuid = UUID.randomUUID().toString();
			return uuid.replaceAll( "-", "" ).toLowerCase();
		}

	}

}