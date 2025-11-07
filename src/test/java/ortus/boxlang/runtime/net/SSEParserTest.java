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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.Attempt;

public class SSEParserTest {

	private SSEParser parser;

	@BeforeEach
	void setUp() {
		parser = new SSEParser();
	}

	@Test
	@DisplayName( "Should parse simple data event" )
	void testSimpleDataEvent() {
		// Act
		Attempt<SSEParserResult>	result1	= parser.parseLine( "data: Hello World" );
		Attempt<SSEParserResult>	result2	= parser.parseLine( "" );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isTrue();

		SSEParserResult parserResult = result2.get();
		assertThat( parserResult ).isInstanceOf( SSEEvent.class );

		SSEEvent event = ( SSEEvent ) parserResult;
		assertThat( event.data() ).isEqualTo( "Hello World" );
		assertThat( event.event() ).isNull();
		assertThat( event.id() ).isNull();
	}

	@Test
	@DisplayName( "Should parse multi-line data event" )
	void testMultiLineDataEvent() {
		// Act
		parser.parseLine( "data: First line" );
		parser.parseLine( "data: Second line" );
		parser.parseLine( "data: Third line" );
		Attempt<SSEParserResult> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEParserResult parserResult = result.get();
		assertThat( parserResult ).isInstanceOf( SSEEvent.class );

		SSEEvent event = ( SSEEvent ) parserResult;
		assertThat( event.data() ).isEqualTo( "First line\nSecond line\nThird line" );
		assertThat( event.event() ).isNull();
		assertThat( event.id() ).isNull();
	}

	@Test
	@DisplayName( "Should parse complete event with all fields" )
	void testCompleteEvent() {
		// Act
		parser.parseLine( "event: message" );
		parser.parseLine( "id: 123" );
		parser.parseLine( "data: Hello World" );
		Attempt<SSEParserResult> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEParserResult parserResult = result.get();
		assertThat( parserResult ).isInstanceOf( SSEEvent.class );

		SSEEvent event = ( SSEEvent ) parserResult;
		assertThat( event.data() ).isEqualTo( "Hello World" );
		assertThat( event.event() ).isEqualTo( "message" );
		assertThat( event.id() ).isEqualTo( "123" );
	}

	@Test
	@DisplayName( "Should parse retry directive" )
	void testRetryDirective() {
		// Act
		Attempt<SSEParserResult> result = parser.parseLine( "retry: 5000" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEParserResult parserResult = result.get();
		assertThat( parserResult ).isInstanceOf( SSERetryDirective.class );

		SSERetryDirective retryDirective = ( SSERetryDirective ) parserResult;
		assertThat( retryDirective.retryDelayMs() ).isEqualTo( 5000L );
	}

	@Test
	@DisplayName( "Should handle invalid retry directive values" )
	void testInvalidRetryDirective() {
		// Act
		Attempt<SSEParserResult>	result1	= parser.parseLine( "retry: not_a_number" );
		Attempt<SSEParserResult>	result2	= parser.parseLine( "retry: " );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isFalse();
	}

	@Test
	@DisplayName( "Should ignore comments" )
	void testComments() {
		// Act
		Attempt<SSEParserResult>	result1	= parser.parseLine( ": This is a comment" );
		Attempt<SSEParserResult>	result2	= parser.parseLine( ":Another comment" );
		Attempt<SSEParserResult>	result3	= parser.parseLine( ":" );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isFalse();
		assertThat( result3.wasSuccessful() ).isFalse();
	}

	@Test
	@DisplayName( "Should ignore invalid lines" )
	void testInvalidLines() {
		// Act
		Attempt<SSEParserResult>	result1	= parser.parseLine( "data without colon" );
		Attempt<SSEParserResult>	result2	= parser.parseLine( "just some text" );
		Attempt<SSEParserResult>	result3	= parser.parseLine( "event without colon" );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isFalse();
		assertThat( result3.wasSuccessful() ).isFalse();
	}
}