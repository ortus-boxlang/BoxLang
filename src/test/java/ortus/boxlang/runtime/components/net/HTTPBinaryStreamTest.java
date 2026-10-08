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
package ortus.boxlang.runtime.components.net;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.sun.net.httpserver.HttpServer;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Tests for raw binary streaming (onBinaryChunk) and the stop, idle timeout and error behavior of streaming modes.
 */
@WireMockTest
public class HTTPBinaryStreamTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		variables.put( "sink", new ByteArrayOutputStream() );
		variables.put( "chunks", new Array() );
		variables.put( "errors", new Array() );
	}

	/**
	 * Bytes that a line based reader would corrupt or split: every value 0 to 255 including \n, \r and 0xFF
	 */
	private static byte[] binaryPayload( int size ) {
		byte[] payload = new byte[ size ];
		for ( int i = 0; i < size; i++ ) {
			payload[ i ] = ( byte ) ( i % 256 );
		}
		return payload;
	}

	@DisplayName( "Binary bytes arrive intact and are not split on newlines" )
	@Test
	public void testBinaryBytesIntact( WireMockRuntimeInfo wmRuntimeInfo ) {
		byte[] payload = binaryPayload( 30000 );
		stubFor( get( urlEqualTo( "/audio" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "audio/mpeg" ).withBody( payload ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/audio" method="GET"
					onBinaryChunk=( bytes, info ) => {
						sink.writeBytes( bytes );
						chunks.append( info.chunkNumber );
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( ( ( ByteArrayOutputStream ) variables.get( "sink" ) ).toByteArray() ).isEqualTo( payload );
		assertThat( httpResult.getAsBoolean( Key.of( "stream" ) ) ).isTrue();
		assertThat( httpResult.getAsBoolean( Key.of( "sse" ) ) ).isFalse();
		assertThat( httpResult.getAsBoolean( Key.of( "streamCompleted" ) ) ).isTrue();
		assertThat( httpResult.getAsNumber( Key.of( "totalBytes" ) ).longValue() ).isEqualTo( payload.length );
		// 30000 bytes cannot fit in one 8192 byte read
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isAtLeast( 4 );
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).get( 0 ) ).isEqualTo( 1 );
	}

	@DisplayName( "Headers are only passed to the first chunk" )
	@Test
	public void testHeadersOnFirstChunkOnly( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/audio-headers" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "audio/mpeg" ).withBody( binaryPayload( 30000 ) ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/audio-headers" method="GET"
					onBinaryChunk=( bytes, info ) => {
						chunks.append( structKeyExists( info, "headers" ) );
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Array hasHeaders = variables.getAsArray( Key.of( "chunks" ) );
		assertThat( hasHeaders.size() ).isAtLeast( 2 );
		assertThat( hasHeaders.get( 0 ) ).isEqualTo( true );
		assertThat( hasHeaders.get( 1 ) ).isEqualTo( false );
	}

	@DisplayName( "The fluent http() BIF supports onBinaryChunk" )
	@Test
	public void testFluentBinaryChunk( WireMockRuntimeInfo wmRuntimeInfo ) {
		byte[] payload = binaryPayload( 5000 );
		stubFor( get( urlEqualTo( "/audio-fluent" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "audio/mpeg" ).withBody( payload ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				result = http( "%s/audio-fluent" )
					.onBinaryChunk( ( bytes, info ) => {
						sink.writeBytes( bytes );
					} )
					.send();
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		assertThat( ( ( ByteArrayOutputStream ) variables.get( "sink" ) ).toByteArray() ).isEqualTo( payload );
	}

	@DisplayName( "Returning false from onBinaryChunk stops the stream" )
	@Test
	public void testBinaryStopOnFalse( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/audio-stop" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "audio/mpeg" ).withBody( binaryPayload( 200000 ) ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/audio-stop" method="GET"
					onBinaryChunk=( bytes, info ) => {
						chunks.append( info.chunkNumber );
						return false;
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 1 );
		assertThat( httpResult.getAsBoolean( Key.of( "streamCompleted" ) ) ).isFalse();
		// Stopping is not an error
		assertThat( httpResult.getAsNumber( Key.of( "statusCode" ) ).intValue() ).isEqualTo( 200 );
		assertThat( httpResult.getAsNumber( Key.of( "chunkCount" ) ).longValue() ).isEqualTo( 1 );
	}

	@DisplayName( "Any return value other than an explicit false keeps streaming" )
	@Test
	public void testBinaryNonFalseKeepsStreaming( WireMockRuntimeInfo wmRuntimeInfo ) {
		byte[] payload = binaryPayload( 30000 );
		stubFor( get( urlEqualTo( "/audio-keep" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "audio/mpeg" ).withBody( payload ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/audio-keep" method="GET"
					onBinaryChunk=( bytes, info ) => {
						sink.writeBytes( bytes );
						return 0;
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		assertThat( ( ( ByteArrayOutputStream ) variables.get( "sink" ) ).toByteArray() ).isEqualTo( payload );
	}

	@DisplayName( "A non 2xx binary response reports HTTP status and body, and skips the chunk callback" )
	@Test
	public void testBinaryHttpError( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/audio-401" ) )
		    .willReturn( aResponse().withStatus( 401 ).withHeader( "Content-Type", "application/json" ).withBody( "{\"error\":\"bad key\"}" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/audio-401" method="GET"
					onBinaryChunk=( bytes, info ) => {
						chunks.append( info.chunkNumber );
					}
					onError=( e, httpResult ) => {
						errors.append( e.getMessage() );
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 0 );
		assertThat( variables.getAsArray( Key.of( "errors" ) ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsArray( Key.of( "errors" ) ).get( 0 ) ).isEqualTo( "HTTP 401: {\"error\":\"bad key\"}" );
		assertThat( httpResult.getAsNumber( Key.of( "statusCode" ) ).intValue() ).isEqualTo( 401 );
		assertThat( httpResult.getAsString( Key.of( "fileContent" ) ) ).isEqualTo( "{\"error\":\"bad key\"}" );
	}

	@DisplayName( "A binary stream that stalls after the headers is aborted by the idle timeout" )
	@Test
	public void testBinaryIdleTimeout() throws Exception {
		try ( StallingServer server = new StallingServer( "audio/mpeg", binaryPayload( 1000 ), 6000 ) ) {
			long start = System.currentTimeMillis();
			// @formatter:off
			instance.executeSource(
			    String.format( """
					bx:http url="%s/stall" method="GET" timeout="1"
						onBinaryChunk=( bytes, info ) => {
							chunks.append( info.chunkNumber );
						}
						onError=( e, httpResult ) => {
							errors.append( e.getMessage() );
						}
						result="result";
				""", server.baseUrl() ),
			    context, BoxSourceType.BOXSCRIPT
			);
			// @formatter:on

			IStruct httpResult = variables.getAsStruct( result );
			// Aborted by the watchdog long before the server resumes
			assertThat( System.currentTimeMillis() - start ).isLessThan( 5000L );
			// The first chunk arrived before the stall
			assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 1 );
			assertThat( httpResult.getAsNumber( Key.of( "statusCode" ) ).intValue() ).isEqualTo( 408 );
			assertThat( httpResult.getAsBoolean( Key.of( "streamCompleted" ) ) ).isFalse();
			assertThat( variables.getAsArray( Key.of( "errors" ) ).size() ).isEqualTo( 1 );
			assertThat( ( String ) variables.getAsArray( Key.of( "errors" ) ).get( 0 ) ).contains( "idle" );
		}
	}

	@DisplayName( "A long but steady binary stream is not cut off by the idle timeout" )
	@Test
	public void testBinaryLongStreamNotCutOff( WireMockRuntimeInfo wmRuntimeInfo ) {
		byte[] payload = binaryPayload( 5000 );
		// Five chunks about 0.6 seconds apart: over 3 seconds in total but never idle for 2 seconds
		stubFor( get( urlEqualTo( "/audio-slow" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "audio/mpeg" )
		        .withBody( payload ).withChunkedDribbleDelay( 5, 3000 ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/audio-slow" method="GET" timeout="2"
					onBinaryChunk=( bytes, info ) => {
						sink.writeBytes( bytes );
					}
					onError=( e, httpResult ) => {
						errors.append( e.getMessage() );
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( variables.getAsArray( Key.of( "errors" ) ).size() ).isEqualTo( 0 );
		assertThat( httpResult.getAsNumber( Key.of( "statusCode" ) ).intValue() ).isEqualTo( 200 );
		assertThat( ( ( ByteArrayOutputStream ) variables.get( "sink" ) ).toByteArray() ).isEqualTo( payload );
	}

	@DisplayName( "Returning false from an SSE onChunk stops the stream" )
	@Test
	public void testSSEStopOnFalse( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/sse-stop" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "text/event-stream" )
		        .withBody( "data: one\n\ndata: two\n\ndata: three\n\n" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/sse-stop" method="GET"
					onChunk=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
						chunks.append( event.data );
						return false;
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).get( 0 ) ).isEqualTo( "one" );
		assertThat( httpResult.getAsBoolean( Key.of( "streamCompleted" ) ) ).isFalse();
		assertThat( httpResult.getAsNumber( Key.of( "totalEvents" ) ).longValue() ).isEqualTo( 1 );
	}

	@DisplayName( "A non 2xx SSE response reports HTTP status and body, and skips the chunk callback" )
	@Test
	public void testSSEHttpError( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/sse-401" ) )
		    .willReturn( aResponse().withStatus( 401 ).withHeader( "Content-Type", "application/json" ).withBody( "{\"error\":\"nope\"}" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/sse-401" method="GET" sse="true"
					onChunk=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
						chunks.append( event.data );
					}
					onError=( e, httpResult ) => {
						errors.append( e.getMessage() );
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 0 );
		assertThat( variables.getAsArray( Key.of( "errors" ) ).get( 0 ) ).isEqualTo( "HTTP 401: {\"error\":\"nope\"}" );
		assertThat( httpResult.getAsNumber( Key.of( "statusCode" ) ).intValue() ).isEqualTo( 401 );
	}

	@DisplayName( "An SSE stream that stalls after the headers is aborted by the idle timeout" )
	@Test
	public void testSSEIdleTimeout() throws Exception {
		try ( StallingServer server = new StallingServer( "text/event-stream", "data: one\n\n".getBytes(), 6000 ) ) {
			long start = System.currentTimeMillis();
			// @formatter:off
			instance.executeSource(
			    String.format( """
					bx:http url="%s/stall" method="GET" timeout="1"
						onChunk=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
							chunks.append( event.data );
						}
						onError=( e, httpResult ) => {
							errors.append( e.getMessage() );
						}
						result="result";
				""", server.baseUrl() ),
			    context, BoxSourceType.BOXSCRIPT
			);
			// @formatter:on

			IStruct httpResult = variables.getAsStruct( result );
			assertThat( System.currentTimeMillis() - start ).isLessThan( 5000L );
			assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 1 );
			assertThat( httpResult.getAsNumber( Key.of( "statusCode" ) ).intValue() ).isEqualTo( 408 );
			assertThat( httpResult.getAsBoolean( Key.of( "streamCompleted" ) ) ).isFalse();
			assertThat( variables.getAsArray( Key.of( "errors" ) ).size() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "Returning false closes the connection so the server stops writing" )
	@Test
	public void testStopClosesConnection() throws Exception {
		CountDownLatch	writeFailed	= new CountDownLatch( 1 );
		HttpServer		server		= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/endless", exchange -> {
			exchange.getResponseHeaders().add( "Content-Type", "audio/mpeg" );
			exchange.sendResponseHeaders( 200, 0 );
			byte[] block = binaryPayload( 8192 );
			try ( OutputStream out = exchange.getResponseBody() ) {
				// Roughly 40MB, far more than the socket buffers can absorb
				for ( int i = 0; i < 5000; i++ ) {
					out.write( block );
					out.flush();
					Thread.sleep( 2 );
				}
			} catch ( IOException e ) {
				writeFailed.countDown();
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
		} );
		server.start();
		try {
			// @formatter:off
			instance.executeSource(
			    String.format( """
					bx:http url="http://127.0.0.1:%d/endless" method="GET"
						onBinaryChunk=( bytes, info ) => {
							chunks.append( info.chunkNumber );
							return false;
						}
						result="result";
				""", server.getAddress().getPort() ),
			    context, BoxSourceType.BOXSCRIPT
			);
			// @formatter:on

			assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 1 );
			assertThat( writeFailed.await( 10, TimeUnit.SECONDS ) ).isTrue();
		} finally {
			server.stop( 0 );
		}
	}

	@DisplayName( "Existing line based onChunk behavior is unchanged" )
	@Test
	public void testLineModeUnchanged( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/lines" ) )
		    .willReturn( aResponse().withStatus( 200 ).withHeader( "Content-Type", "text/plain" ).withBody( "a\nb\nc\n" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/lines" method="GET"
					onChunk=( chunkNumber, content ) => {
						chunks.append( content );
						return false;
					}
					result="result";
			""", wmRuntimeInfo.getHttpBaseUrl() ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		// Line mode does not honor stop-on-false, every line is still delivered
		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isEqualTo( 3 );
	}

	/**
	 * Sends the response headers and one chunk right away, then stalls before sending a second chunk
	 */
	private static final class StallingServer implements AutoCloseable {

		private final HttpServer server;

		StallingServer( String contentType, byte[] firstChunk, long stallMillis ) throws IOException {
			this.server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
			this.server.createContext( "/stall", exchange -> {
				exchange.getResponseHeaders().add( "Content-Type", contentType );
				exchange.sendResponseHeaders( 200, 0 );
				try ( OutputStream out = exchange.getResponseBody() ) {
					out.write( firstChunk );
					out.flush();
					Thread.sleep( stallMillis );
					out.write( firstChunk );
					out.flush();
				} catch ( IOException | InterruptedException ignored ) {
					// The client gave up, which is what the tests expect
				}
			} );
			this.server.start();
		}

		String baseUrl() {
			return "http://127.0.0.1:" + this.server.getAddress().getPort();
		}

		@Override
		public void close() {
			this.server.stop( 0 );
		}
	}
}
