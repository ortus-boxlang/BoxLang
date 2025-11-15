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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@WireMockTest
public class HTTPSSETest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test SSE auto-detection via Content-Type header" )
	@Test
	public void testSSEAutoDetection( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-stream" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "data: Hello World\n\n" +
		                        "event: custom\n" +
		                        "data: Custom event\n\n" +
		                        "data: Final message\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "events", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/sse-stream"
					method="GET"
					onChunk=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
						events.append( event );
					}
					result="result";
			""", baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct	httpResult		= variables.getAsStruct( result );
		Array	receivedEvents	= variables.getAsArray( Key.of( "events" ) );
		assertThat( httpResult.getAsBoolean( Key.of( "stream" ) ) ).isTrue();
		assertThat( httpResult.getAsBoolean( Key.of( "sse" ) ) ).isTrue();
		assertThat( httpResult.getAsNumber( Key.of( "totalEvents" ) ).longValue() ).isEqualTo( 3 );

		assertThat( receivedEvents.size() ).isEqualTo( 3 );
		assertThat(
		    ( ( IStruct ) receivedEvents.get( 0 ) ).getAsString( Key.of( "data" ) )
		)
		    .isEqualTo( "Hello World" );
		assertThat(
		    ( ( IStruct ) receivedEvents.get( 1 ) ).getAsString( Key.of( "event" ) )
		)
		    .isEqualTo( "custom" );
		assertThat(
		    ( ( IStruct ) receivedEvents.get( 1 ) ).getAsString( Key.of( "data" ) )
		)
		    .isEqualTo( "Custom event" );
		assertThat(
		    ( ( IStruct ) receivedEvents.get( 2 ) ).getAsString( Key.of( "data" ) )
		)
		    .isEqualTo( "Final message" );
	}

	@DisplayName( "Test SSE force mode with sse=true attribute" )
	@Test
	public void testSSEForceMode( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/fake-sse" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/plain" ) // Not SSE content type
		                .withBody(
		                    "data: Forced SSE\n\n" +
		                        "data: Another event\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "events", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/sse-stream"
					method="GET"
					result="result"
					onChunk=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
					events.append( event );
					}
					sse=true
				{}
			""", baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct	httpResult		= variables.getAsStruct( result );
		Array	receivedEvents	= variables.getAsArray( Key.of( "events" ) );
		assertThat( httpResult.getAsBoolean( Key.of( "sse" ) ) ).isTrue();
		assertThat( receivedEvents.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Test SSE onMessage sugar syntax" )
	@Test
	public void testSSEOnMessageSugar( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-messages" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "data: Message 1\n\n" +
		                        "data: Message 2\n\n" +
		                        "data: Message 3\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "events", new Array() );
// @formatter:off
instance.executeSource(
		    String.format( """
						bx:http url="%s/sse-messages"
						method="GET"
						result="result"
						onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
							events.append( event );
						}
		                   {}
		                                 """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct	httpResult		= variables.getAsStruct( result );
		Array	receivedEvents	= variables.getAsArray( Key.of( "events" ) );
		assertThat( httpResult.getAsBoolean( Key.of( "sse" ) ) ).isTrue();
		assertThat( receivedEvents.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Test SSE event IDs and tracking" )
	@Test
	public void testSSEEventIDs( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-with-ids" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "id: 1\n" +
		                        "data: First\n\n" +
		                        "id: 2\n" +
		                        "data: Second\n\n" +
		                        "id: 3\n" +
		                        "data: Third\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "events", new Array() );
		variables.put( "ids", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   bx:http url="%s/sse-with-ids"
		                   	method="GET"
		                   	result="result"
		                   	onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
		                   		events.append( event );
		                   		ids.append( lastEventId );
		                   	}
							{}
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Array	receivedIds		= variables.getAsArray( Key.of( "ids" ) );
		Array	receivedEvents	= variables.getAsArray( Key.of( "events" ) );
		assertThat( receivedIds.size() ).isEqualTo( 3 );
		assertThat( receivedIds.get( 0 ) ).isEqualTo( "1" );
		assertThat( receivedIds.get( 1 ) ).isEqualTo( "2" );
		assertThat( receivedIds.get( 2 ) ).isEqualTo( "3" );

		assertThat( ( ( IStruct ) receivedEvents.get( 0 ) ).getAsString( Key.of( "id" ) ) ).isEqualTo( "1" );
		assertThat( ( ( IStruct ) receivedEvents.get( 1 ) ).getAsString( Key.of( "id" ) ) ).isEqualTo( "2" );
		assertThat( ( ( IStruct ) receivedEvents.get( 2 ) ).getAsString( Key.of( "id" ) ) ).isEqualTo( "3" );
	}

	@DisplayName( "Test SSE custom event types" )
	@Test
	public void testSSECustomEventTypes( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-custom-types" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "event: ping\n" +
		                        "data: ping data\n\n" +
		                        "event: update\n" +
		                        "data: update data\n\n" +
		                        "data: default message\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "events", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   bx:http url="%s/sse-custom-types"
		                   	method="GET"
		                   	result="result"
		                   	onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
		                   		events.append( event );
		                   	}
							{}
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Array receivedEvents = variables.getAsArray( Key.of( "events" ) );
		assertThat( receivedEvents.size() ).isEqualTo( 3 );
		assertThat( ( ( IStruct ) receivedEvents.get( 0 ) ).getAsString( Key.of( "event" ) ) ).isEqualTo( "ping" );
		assertThat( ( ( IStruct ) receivedEvents.get( 0 ) ).getAsString( Key.of( "data" ) ) ).isEqualTo( "ping data" );
		assertThat( ( ( IStruct ) receivedEvents.get( 1 ) ).getAsString( Key.of( "event" ) ) ).isEqualTo( "update" );
		assertThat( ( ( IStruct ) receivedEvents.get( 1 ) ).getAsString( Key.of( "data" ) ) ).isEqualTo( "update data" );
		// Default event type is "message"
		assertThat( ( ( IStruct ) receivedEvents.get( 2 ) ).getAsString( Key.of( "data" ) ) ).isEqualTo( "default message" );
	}

	@DisplayName( "Test SSE multi-line data" )
	@Test
	public void testSSEMultiLineData( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-multiline" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "data: Line 1\n" +
		                        "data: Line 2\n" +
		                        "data: Line 3\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "events", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   bx:http url="%s/sse-multiline"
		                   	method="GET"
		                   	result="result"
		                   	onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
		                   		events.append( event );
		                   	}
							{}
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Array receivedEvents = variables.getAsArray( Key.of( "events" ) );
		assertThat( receivedEvents.size() ).isEqualTo( 1 );
		String data = ( ( IStruct ) receivedEvents.get( 0 ) ).getAsString( Key.of( "data" ) );
		assertThat( data ).contains( "Line 1" );
		assertThat( data ).contains( "Line 2" );
		assertThat( data ).contains( "Line 3" );
	}

	@DisplayName( "Test SSE with onRequestStart callback" )
	@Test
	public void testSSEWithOnRequestStart( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-with-start" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody( "data: Hello\n\n" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "startCalled", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   bx:http
						   		url="%s/sse-with-start"
								result="result"
								onRequestStart=( result, client, httpRequest ) => {
									startCalled.append( result.sse );
								}
								onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
									// Event received
								}
							{}
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Array startCalled = variables.getAsArray( Key.of( "startCalled" ) );
		assertThat( startCalled.size() ).isEqualTo( 1 );
		assertThat( ( Boolean ) startCalled.get( 0 ) ).isTrue();
	}

	@DisplayName( "Test SSE with onComplete callback" )
	@Test
	public void testSSEWithOnComplete( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-with-complete" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "data: Event 1\n\n" +
		                        "data: Event 2\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "eventCounts", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   bx:http url="%s/sse-with-complete"
		                   	method="GET"
		                   	result="result"
		                   	onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
		                   		// Event received
		                   	}
		                   	onComplete=( httpResult, rawResponse ) => {
		                   		eventCounts.append( httpResult.totalEvents );
		                   	}
							{}
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Array eventCounts = variables.getAsArray( Key.of( "eventCounts" ) );
		assertThat( eventCounts.size() ).isEqualTo( 1 );
		assertThat( ( ( Number ) eventCounts.get( 0 ) ).longValue() ).isEqualTo( 2 );
	}

	@DisplayName( "Test SSE accumulated fileContent result" )
	@Test
	public void testSSEAccumulatedContent( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/sse-accumulated" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/event-stream" )
		                .withBody(
		                    "data: First\n\n" +
		                        "data: Second\n\n" +
		                        "data: Third\n\n"
		                )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   bx:http url="%s/sse-accumulated"
		                   	method="GET"
		                   	result="result"
		                   	onMessage=( event, lastEventId, httpResult, httpClient, rawResponse ) => {
		                   		// Just receive events
		                   	}
							{}
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on
		IStruct	httpResult	= variables.getAsStruct( result );
		String	fileContent	= httpResult.getAsString( Key.of( "fileContent" ) );

		assertThat( fileContent ).contains( "First" );
		assertThat( fileContent ).contains( "Second" );
		assertThat( fileContent ).contains( "Third" );
	}

	@DisplayName( "Test non-SSE streaming still works" )
	@Test
	public void testRegularStreamingNotAffected( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/regular-stream" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/plain" )
		                .withBody( "Regular streaming content" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		variables.put( "chunkNumbers", new Array() );
		// @formatter:off
		instance.executeSource(
		    String.format( """
				bx:http url="%s/regular-stream"
					result="result"
					onChunk=( chunkNumber, content, totalBytes, httpResult, httpClient, rawResponse ) => {
						chunkNumbers.append( chunkNumber );
					}
				{}
		    """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		IStruct	httpResult		= variables.getAsStruct( result );
		Array	chunkNumbers	= variables.getAsArray( Key.of( "chunkNumbers" ) );
		assertThat( httpResult.getAsBoolean( Key.of( "stream" ) ) ).isTrue();
		assertThat( httpResult.getAsBoolean( Key.of( "sse" ) ) ).isFalse();
		assertThat( chunkNumbers.size() ).isGreaterThan( 0 );
	}
}
