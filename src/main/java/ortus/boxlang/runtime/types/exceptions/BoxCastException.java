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
package ortus.boxlang.runtime.types.exceptions;

/**
 * This exception is thrown when an attempt to cast a value to a specific Java type fails.
 *
 * Most often it is seen when strongly typed arguments or attributes are used in a way that is not compatible with the expected type.
 */
public class BoxCastException extends BoxRuntimeException {

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public BoxCastException( String message ) {
		this( message, null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public BoxCastException( String message, Throwable cause ) {
		super( message, "BoxCastException", cause );
	}

}
