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
package ourtus.boxlang.ast;

/**
 * Thrown upon failure to parse either BoxLang, CFML, or CFScript.
 */
public class ParseException {
	/**
	 * Exception string message - try to be clear, precise, and accurate.
	 * Instead of "Unknown parsing error", say "Encountered unexpected ':', expected one of X, Ym or Z"
	 */
	private final String message;
	/**
	 * Line and character position of the offending source code. May optionally contain a file source.
	 */
	private final Position position;

	/**
	 * Build new parse exception with message and position.
	 *
	 * @param message Exception string message - try to be clear, precise, and accurate. "error parsing XYZ" is not enough!
	 * @param position Invalid syntax position, including start and end and (optionally) the file name.
	 */
	public ParseException(String message, Position position) {
		this.message = message;
		this.position = position;
	}

	public String getMessage() {
		return message;
	}

	public Position getPosition() {
		return position;
	}

	/**
	 * Stringify the parse exception - more useful for debugging and CLI usage.
	 * Use .toPrettyString() to print source with "pointer".
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(this.position.getSource() != null) {
			sb.append(position.getSource());
			sb.append(":");
		}
		sb.append(position.getStart().getLine());
		sb.append(" - ");
		sb.append(this.message);

		return sb.toString();
	}
}
