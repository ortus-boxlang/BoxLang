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
 * Base exception for all custom exceptions thrown by the user
 */
public class ApplicationException extends BoxLangException {

	public static final Key	ExtendedInfoKey	= Key.of( "extendedInfo" );

	/**
	 * Custom error message; information that the default exception handler does not display.
	 */
	public String			extendedInfo	= "";

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public ApplicationException( String message ) {
		this( message, null, null, null );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param errorCode The errorCode
	 */
	public ApplicationException( String message, Throwable cause ) {
		this( message, null, null, cause );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param errorCode The errorCode
	 */
	public ApplicationException( String message, String extendedInfo ) {
		this( message, null, extendedInfo, null );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param detail    The detail
	 * @param errorCode The errorCode
	 * @param cause     The cause
	 */
	public ApplicationException( String message, String detail, String extendedInfo, Throwable cause ) {
		super( message, "application", cause );
		this.detail			= detail;
		this.extendedInfo	= extendedInfo;
	}

}
