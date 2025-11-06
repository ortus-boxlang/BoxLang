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

import ortus.boxlang.runtime.dynamic.Attempt;

public class SSEParser {

	/**
	 * Current event data being parsed
	 */
	private StringBuilder	currentData		= new StringBuilder();

	/**
	 * Current event type
	 */
	private String			currentEvent	= null;

	/**
	 * Current event ID
	 */
	private String			currentId		= null;

	/**
	 * Parses a single line of an SSE stream.
	 *
	 * @param line The line to parse.
	 *
	 * @return An Optional containing an SSEEvent if the event is complete, otherwise empty.
	 */
	public Attempt<SSEEvent> parseLine( String line ) {
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
				// Handle reconnection time if needed
				break;
		}

		return Attempt.empty();
	}

	/**
	 * Resets the parser state for the next event.
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
 * @param data  The event data
 * @param event The event type (optional)
 * @param id    The event ID (optional)
 */
record SSEEvent( String data, String event, String id ) {
}