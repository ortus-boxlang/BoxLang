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
import ortus.boxlang.runtime.types.IStruct;

/**
 * This is the base exception class for all custom exceptions thrown by the user.
 *
 * All dynamically declared exceptions will extend this class
 */
public class CustomException extends BoxRuntimeException {

	/**
	 * Applies to type = "custom". String error code.
	 */
	protected String errorCode = "";

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public CustomException( String message ) {
		this( message, "", "", "", null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public CustomException( String message, Throwable cause ) {
		this( message, "", "", "", cause );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param errorCode The errorCode
	 */
	public CustomException( String message, String errorCode ) {
		this( message, "", errorCode, "", null );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param detail    The detail
	 * @param errorCode The errorCode
	 * @param cause     The cause
	 */
	public CustomException( String message, String detail, String errorCode, Object extendedInfo, Throwable cause ) {
		super( message, detail, "Custom", extendedInfo, cause );
		this.errorCode = errorCode == null ? "" : errorCode;
	}

	/**
	 * Constructor
	 *
	 * @param message      The message
	 * @param detail       The detail
	 * @param errorCode    The errorCode
	 * @param type         The type
	 * @param extendedInfo The extended info
	 * @param cause        The cause
	 */
	public CustomException( String message, String detail, String errorCode, String type, Object extendedInfo, Throwable cause ) {
		super( message, detail, type, extendedInfo, cause );
		this.errorCode = errorCode == null ? "" : errorCode;
	}

	/**
	 * Get the error code
	 *
	 * @return The error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Get the data as a struct
	 */
	@Override
	public IStruct dataAsStruct() {
		IStruct result = super.dataAsStruct();
		result.put( Key.errorcode, errorCode );
		return result;
	}

}
