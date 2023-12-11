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

import ortus.boxlang.runtime.scopes.Key;

/**
 * Base exception for all expression-related errors
 */
public class ExpressionException extends BoxLangException {

	public static final Key	ErrNumberKey	= Key.of( "ErrNumber" );

	/**
	 * Internal expression error number.
	 */
	public String			errNumber		= null;

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public ExpressionException( String message ) {
		this( message, null, null, null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public ExpressionException( String message, Throwable cause ) {
		this( message, null, null, cause );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param errNumber The errNumber
	 */
	public ExpressionException( String message, String errNumber ) {
		this( message, null, errNumber, null );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param detail    The detail
	 * @param errNumber The errNumber
	 * @param cause     The cause
	 */
	public ExpressionException( String message, String detail, String errNumber, Throwable cause ) {
		super( message, detail, "expression", cause );
		this.errNumber = errNumber;
	}

}
