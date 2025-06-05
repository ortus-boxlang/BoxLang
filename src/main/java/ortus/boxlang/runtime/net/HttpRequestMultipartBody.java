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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class HttpRequestMultipartBody {

	private byte[] bytes;

	public String getBoundary() {
		return boundary;
	}

	public void setBoundary( String boundary ) {
		this.boundary = boundary;
	}

	private String boundary;

	private HttpRequestMultipartBody( byte[] bytes, String boundary ) {
		this.bytes		= bytes;
		this.boundary	= boundary;
	}

	public String getContentType() {
		return "multipart/form-data; boundary=" + this.getBoundary();
	}

	public byte[] getBody() {
		return this.bytes;
	}

	public static class Builder {

		private final String	DEFAULT_MIMETYPE	= "text/plain";
		private final String	CRLF				= "\r\n";
		private final String	BOUNDARY_PREFIX		= "--";

		public static class MultiPartRecord {

			private String	fieldName;
			private String	filename;
			private String	contentType;
			private Object	content;

			public String getFieldName() {
				return fieldName;
			}

			public void setFieldName( String fieldName ) {
				this.fieldName = fieldName;
			}

			public String getFilename() {
				return filename;
			}

			public void setFilename( String filename ) {
				this.filename = filename;
			}

			public String getContentType() {
				return contentType != null ? contentType : "application/octet-stream";
			}

			public void setContentType( String contentType ) {
				this.contentType = contentType;
			}

			public Object getContent() {
				return content;
			}

			public void setContent( Object content ) {
				this.content = content;
			}
		}

		List<MultiPartRecord> parts;

		public Builder() {
			this.parts = new ArrayList<>();
		}

		public Builder addPart( String fieldName, String fieldValue ) {
			MultiPartRecord part = new MultiPartRecord();
			part.setFieldName( fieldName );
			part.setContent( fieldValue );
			part.setContentType( DEFAULT_MIMETYPE );
			this.parts.add( part );
			return this;
		}

		public Builder addPart( String fieldName, String fieldValue, String contentType ) {
			MultiPartRecord part = new MultiPartRecord();
			part.setFieldName( fieldName );
			part.setContent( fieldValue );
			part.setContentType( contentType );
			this.parts.add( part );
			return this;
		}

		public Builder addPart( String fieldName, Object fieldValue, String contentType, String fileName ) {
			MultiPartRecord part = new MultiPartRecord();
			part.setFieldName( fieldName );
			part.setContent( fieldValue );
			part.setContentType( contentType );
			part.setFilename( fileName );
			this.parts.add( part );
			return this;
		}

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
					writer.append( CRLF ).append( castString ).append( CRLF ).flush();
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

		private String generateBoundary() {
			String uuid = UUID.randomUUID().toString();
			return uuid.replaceAll( "-", "" ).toLowerCase();
		}

	}

}