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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Base interface for SSE parser results.
 *
 * This sealed interface allows the parser to return either complete events
 * or server directives (like retry instructions) from the SSE stream.
 *
 * This allows us to use jdk21 pattern matching to handle different result types.
 */
sealed interface SSEParserResult permits SSEEvent, SSERetryDirective {
}

/**
 * A stateful parser for Server-Sent Events (SSE) streams.
 *
 * This parser implements the W3C Server-Sent Events specification for parsing
 * event streams line by line. It maintains internal state to accumulate event
 * data across multiple lines and returns complete events when an empty line
 * is encountered.
 *
 * <p>
 * The parser supports all standard SSE fields:
 * </p>
 * <ul>
 * <li><strong>data:</strong> Event payload data (can span multiple lines)</li>
 * <li><strong>event:</strong> Event type identifier</li>
 * <li><strong>id:</strong> Event identifier</li>
 * <li><strong>retry:</strong> Reconnection time hint (parsed but not stored)</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * SSEParser parser = new SSEParser();
 *
 * // Parse each line of the SSE stream
 * Attempt&lt;SSEEvent&gt; result1 = parser.parseLine( "data: Hello" );
 * Attempt&lt;SSEEvent&gt; result2 = parser.parseLine( "event: message" );
 * Attempt&lt;SSEEvent&gt; result3 = parser.parseLine( "id: 123" );
 * Attempt&lt;SSEEvent&gt; result4 = parser.parseLine( "" ); // Empty line completes event
 *
 * if ( result4.wasSuccessful() ) {
 *     SSEEvent event = result4.get();
 *     // event.data() = "Hello"
 *     // event.event() = "message"
 *     // event.id() = "123"
 * }
 * </pre>
 *
 * <p>
 * The parser automatically handles:
 * </p>
 * <ul>
 * <li>Multi-line data fields (joined with newlines)</li>
 * <li>Comments (lines starting with ":")</li>
 * <li>Malformed lines (ignored)</li>
 * <li>State reset between events</li>
 * <li>Whitespace trimming of field values</li>
 * </ul>
 *
 * <p>
 * Thread Safety: This class is NOT thread-safe. Each parser instance
 * should be used by only one thread or external synchronization should be provided.
 * </p>
 *
 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">W3C Server-Sent Events Specification</a>
 *
 * @since 1.8.0
 */
public class SSEParser {

	/**
	 * Logger instance for the SSE parser
	 */
	private static final BoxLangLogger	logger			= BoxRuntime.getInstance().getLoggingService().RUNTIME_LOGGER;

	/**
	 * Current event data being parsed.
	 *
	 * Data fields can span multiple lines and are accumulated in this StringBuilder.
	 * Multiple data lines are joined with newline characters (\n) as per SSE specification.
	 */
	private StringBuilder				currentData		= new StringBuilder();

	/**
	 * Current event type being parsed.
	 *
	 * Corresponds to the "event:" field in SSE streams. Used to identify
	 * the type of event being sent. Remains null if no event type is specified.
	 */
	private String						currentEvent	= null;

	/**
	 * Current event ID being parsed.
	 *
	 * Corresponds to the "id:" field in SSE streams. Used to track the
	 * last received event ID for reconnection purposes. Remains null if no ID is specified.
	 */
	private String						currentId		= null;

	/**
	 * Parses a single line of an SSE stream.
	 *
	 * This method processes one line at a time from an SSE stream, accumulating
	 * event data until a complete event is formed. Events are completed when
	 * an empty line is encountered.
	 *
	 * <p>
	 * Supported line formats:
	 * </p>
	 * <ul>
	 * <li><code>data: &lt;value&gt;</code> - Event data (can be multi-line)</li>
	 * <li><code>event: &lt;type&gt;</code> - Event type</li>
	 * <li><code>id: &lt;identifier&gt;</code> - Event ID</li>
	 * <li><code>retry: &lt;milliseconds&gt;</code> - Reconnection time hint</li>
	 * <li><code>: &lt;comment&gt;</code> - Comment (ignored)</li>
	 * <li><code>&lt;empty line&gt;</code> - Event boundary/completion</li>
	 * </ul>
	 *
	 * <p>
	 * Lines that don't contain a colon (:) are considered malformed and ignored.
	 * Field values are automatically trimmed of leading and trailing whitespace.
	 * </p>
	 *
	 * @param line The line to parse from the SSE stream. Must not be null.
	 *
	 * @return An Attempt containing an SSEParserResult. This can be either an SSEEvent if the event
	 *         is complete (empty line encountered), an SSERetryDirective if a retry instruction was
	 *         parsed, or an empty Attempt if more lines are needed or the line should be ignored.
	 *
	 * @throws NullPointerException if line is null
	 *
	 * @see SSEEvent
	 * @see SSERetryDirective
	 */
	public Attempt<SSEParserResult> parseLine( String line ) {
		// End of event - dispatch the event regardless of whether there's data
		// An SSE event can have just an ID, event type, or be completely empty
		if ( line.isEmpty() ) {
			SSEEvent event = new SSEEvent(
			    currentData.toString(),
			    currentEvent,
			    currentId
			);
			reset();
			return Attempt.of( event );
		}

		// Comment - ignore
		if ( line.startsWith( ":" ) ) {
			return Attempt.empty();
		}

		// Invalid lines - ignore, valid lines have colon
		// Ex: data: hello, event: message, id: 123
		int colonIndex = line.indexOf( ':' );
		if ( colonIndex == -1 ) {
			return Attempt.empty();
		}

		String	field	= line.substring( 0, colonIndex );
		String	value	= line.substring( colonIndex + 1 ).trim();

		switch ( field ) {
			case "data" :
				if ( this.currentData.length() > 0 ) {
					this.currentData.append( "\n" );
				}
				this.currentData.append( value );
				break;
			case "event" :
				this.currentEvent = value;
				break;
			case "id" :
				this.currentId = value;
				break;
			case "retry" :
				// Handle reconnection time directive
				try {
					long retryMs = Long.parseLong( value );
					// Immediately return the retry directive
					return Attempt.of( new SSERetryDirective( retryMs ) );
				} catch ( NumberFormatException e ) {
					logger.debug( "Invalid retry value: {}", value );
				}
				break;
		}

		return Attempt.empty();
	}

	/**
	 * Resets the parser state for the next event.
	 *
	 * This method clears all accumulated event data and resets the parser
	 * to its initial state, ready to parse the next event in the stream.
	 * It is automatically called after each complete event is dispatched.
	 *
	 * <p>
	 * Reset operations:
	 * </p>
	 * <ul>
	 * <li>Clears accumulated data buffer</li>
	 * <li>Resets event type to null</li>
	 * <li>Resets event ID to null</li>
	 * </ul>
	 */
	private void reset() {
		this.currentData	= new StringBuilder();
		this.currentEvent	= null;
		this.currentId		= null;
	}
}

/**
 * Represents a Server-Sent Event with data, event type, and ID.
 *
 * This record encapsulates a complete SSE event as parsed from an event stream.
 * All fields are optional according to the SSE specification - an event can
 * contain just data, just an event type, just an ID, or any combination thereof.
 *
 * <p>
 * Field descriptions:
 * </p>
 * <ul>
 * <li><strong>data:</strong> The event payload. If multiple data lines were present,
 * they are joined with newline characters. Empty string if no data was provided.</li>
 * <li><strong>event:</strong> The event type identifier. Null if no event type was specified.
 * Used by clients to differentiate between different types of events.</li>
 * <li><strong>id:</strong> The event identifier. Null if no ID was specified.
 * Used for reconnection - clients can send this ID to resume from this point.</li>
 * </ul>
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * // Data-only event
 * new SSEEvent("Hello World", null, null)
 *
 * // Complete event with all fields
 * new SSEEvent("User logged in", "user-action", "123")
 *
 * // Heartbeat event (no data)
 * new SSEEvent("", "heartbeat", null)
 * </pre>
 *
 * @param data  The event data payload (never null, empty string if no data)
 * @param event The event type identifier (null if not specified)
 * @param id    The event identifier (null if not specified)
 *
 * @since 1.0.0
 */
record SSEEvent( String data, String event, String id ) implements SSEParserResult {

	public IStruct toStruct() {
		return Struct.ofNonConcurrent(
		    Key.data, data,
		    Key.event, event,
		    Key.id, id
		);
	}
}

/**
 * Represents an SSE retry directive that instructs the client how long to wait before reconnecting.
 *
 * This record encapsulates a retry directive as parsed from an SSE stream's "retry:" field.
 * According to the SSE specification, the retry field specifies the reconnection time in milliseconds
 * that the client should use for subsequent reconnection attempts.
 *
 * <p>
 * The retry directive affects future reconnection behavior and should be used to update
 * the client's reconnection delay setting.
 * </p>
 *
 * @param retryDelayMs The reconnection delay in milliseconds
 *
 * @since 1.8.0
 */
record SSERetryDirective( long retryDelayMs ) implements SSEParserResult {
}