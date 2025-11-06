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
		Attempt<SSEEvent>	result1	= parser.parseLine( "data: Hello World" );
		Attempt<SSEEvent>	result2	= parser.parseLine( "" );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isTrue();

		SSEEvent event = result2.get();
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
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
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
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEqualTo( "Hello World" );
		assertThat( event.event() ).isEqualTo( "message" );
		assertThat( event.id() ).isEqualTo( "123" );
	}

	@Test
	@DisplayName( "Should parse event with only event type" )
	void testEventTypeOnly() {
		// Act
		parser.parseLine( "event: heartbeat" );
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEmpty();
		assertThat( event.event() ).isEqualTo( "heartbeat" );
		assertThat( event.id() ).isNull();
	}

	@Test
	@DisplayName( "Should parse event with only ID" )
	void testIdOnly() {
		// Act
		parser.parseLine( "id: 456" );
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEmpty();
		assertThat( event.event() ).isNull();
		assertThat( event.id() ).isEqualTo( "456" );
	}

	@Test
	@DisplayName( "Should parse completely empty event" )
	void testEmptyEvent() {
		// Act
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEmpty();
		assertThat( event.event() ).isNull();
		assertThat( event.id() ).isNull();
	}

	@Test
	@DisplayName( "Should ignore comments" )
	void testComments() {
		// Act
		Attempt<SSEEvent>	result1	= parser.parseLine( ": This is a comment" );
		Attempt<SSEEvent>	result2	= parser.parseLine( ":Another comment" );
		Attempt<SSEEvent>	result3	= parser.parseLine( ":" );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isFalse();
		assertThat( result3.wasSuccessful() ).isFalse();
	}

	@Test
	@DisplayName( "Should ignore malformed lines" )
	void testMalformedLines() {
		// Act
		Attempt<SSEEvent>	result1	= parser.parseLine( "data without colon" );
		Attempt<SSEEvent>	result2	= parser.parseLine( "just some text" );
		Attempt<SSEEvent>	result3	= parser.parseLine( "event without colon" );

		// Assert
		assertThat( result1.wasSuccessful() ).isFalse();
		assertThat( result2.wasSuccessful() ).isFalse();
		assertThat( result3.wasSuccessful() ).isFalse();
	}

	@Test
	@DisplayName( "Should handle retry field" )
	void testRetryField() {
		// Act
		parser.parseLine( "retry: 5000" );
		parser.parseLine( "data: test" );
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEqualTo( "test" );
		// retry field is handled but not stored in the event
	}

	@Test
	@DisplayName( "Should reset state between events" )
	void testStateReset() {
		// First event
		parser.parseLine( "event: first" );
		parser.parseLine( "id: 100" );
		parser.parseLine( "data: First event data" );
		Attempt<SSEEvent> result1 = parser.parseLine( "" );

		// Second event
		parser.parseLine( "data: Second event data" );
		Attempt<SSEEvent> result2 = parser.parseLine( "" );

		// Assert first event
		assertThat( result1.wasSuccessful() ).isTrue();
		SSEEvent event1 = result1.get();
		assertThat( event1.data() ).isEqualTo( "First event data" );
		assertThat( event1.event() ).isEqualTo( "first" );
		assertThat( event1.id() ).isEqualTo( "100" );

		// Assert second event (should not inherit from first)
		assertThat( result2.wasSuccessful() ).isTrue();
		SSEEvent event2 = result2.get();
		assertThat( event2.data() ).isEqualTo( "Second event data" );
		assertThat( event2.event() ).isNull();
		assertThat( event2.id() ).isNull();
	}

	@Test
	@DisplayName( "Should handle values with extra spaces" )
	void testValueTrimming() {
		// Act
		parser.parseLine( "data:   Hello with spaces   " );
		parser.parseLine( "event:  message  " );
		parser.parseLine( "id:  123  " );
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEqualTo( "Hello with spaces" );
		assertThat( event.event() ).isEqualTo( "message" );
		assertThat( event.id() ).isEqualTo( "123" );
	}

	@Test
	@DisplayName( "Should handle empty field values" )
	void testEmptyValues() {
		// Act
		parser.parseLine( "data:" );
		parser.parseLine( "event:" );
		parser.parseLine( "id:" );
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEmpty();
		assertThat( event.event() ).isEmpty();
		assertThat( event.id() ).isEmpty();
	}

	@Test
	@DisplayName( "Should handle colon in field values" )
	void testColonInValues() {
		// Act
		parser.parseLine( "data: Message with: colon in value" );
		parser.parseLine( "event: type:with:colons" );
		Attempt<SSEEvent> result = parser.parseLine( "" );

		// Assert
		assertThat( result.wasSuccessful() ).isTrue();

		SSEEvent event = result.get();
		assertThat( event.data() ).isEqualTo( "Message with: colon in value" );
		assertThat( event.event() ).isEqualTo( "type:with:colons" );
	}

	@Test
	@DisplayName( "Should handle complex SSE stream scenario" )
	void testComplexScenario() {
		// Simulate a real SSE stream with comments, multiple events, and various field types

		// Comment and first event
		parser.parseLine( ": Stream starting" );
		parser.parseLine( "event: start" );
		parser.parseLine( "id: 1" );
		parser.parseLine( "data: Stream started" );
		Attempt<SSEEvent> result1 = parser.parseLine( "" );

		// Heartbeat event
		parser.parseLine( ": Keep alive" );
		parser.parseLine( "event: heartbeat" );
		Attempt<SSEEvent> result2 = parser.parseLine( "" );

		// Multi-line data event with retry
		parser.parseLine( "retry: 30000" );
		parser.parseLine( "event: message" );
		parser.parseLine( "id: 2" );
		parser.parseLine( "data: Line 1" );
		parser.parseLine( "data: Line 2" );
		parser.parseLine( "data: Line 3" );
		Attempt<SSEEvent> result3 = parser.parseLine( "" );

		// Assert all events
		assertThat( result1.wasSuccessful() ).isTrue();
		SSEEvent event1 = result1.get();
		assertThat( event1.event() ).isEqualTo( "start" );
		assertThat( event1.id() ).isEqualTo( "1" );
		assertThat( event1.data() ).isEqualTo( "Stream started" );

		assertThat( result2.wasSuccessful() ).isTrue();
		SSEEvent event2 = result2.get();
		assertThat( event2.event() ).isEqualTo( "heartbeat" );
		assertThat( event2.id() ).isNull();
		assertThat( event2.data() ).isEmpty();

		assertThat( result3.wasSuccessful() ).isTrue();
		SSEEvent event3 = result3.get();
		assertThat( event3.event() ).isEqualTo( "message" );
		assertThat( event3.id() ).isEqualTo( "2" );
		assertThat( event3.data() ).isEqualTo( "Line 1\nLine 2\nLine 3" );
	}
}