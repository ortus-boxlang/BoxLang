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
public class BoxRuntimeException extends BoxLangException {

    public static final Key ExtendedInfoKey = Key.of( "extendedInfo" );

    /**
     * Custom error message; information that the default exception handler does not display.
     */
    public String           extendedInfo    = "";

    /**
     * Constructor
     *
     * @param message The message
     */
    public BoxRuntimeException( String message ) {
        this( message, null, null, null );
    }

    /**
     * Constructor
     *
     * @param message The message
     * @param cause   The cause
     */
    public BoxRuntimeException( String message, Throwable cause ) {
        this( message, null, null, cause );
    }

    /**
     * Constructor
     *
     * @param message      The message
     * @param extendedInfo The extendedInfo
     */
    public BoxRuntimeException( String message, String extendedInfo ) {
        this( message, null, extendedInfo, null );
    }

    /**
     * Constructor
     *
     * @param message      The message
     * @param detail       The detail
     * @param extendedInfo The extendedInfo
     * @param cause        The cause
     */
    public BoxRuntimeException( String message, String detail, String extendedInfo, Throwable cause ) {
        this( message, detail, "application", extendedInfo, cause );
    }

    /**
     * Constructor
     *
     * @param message      The message
     * @param detail       The detail
     * @param extendedInfo The extendedInfo
     * @param cause        The cause
     */
    public BoxRuntimeException( String message, String detail, String type, String extendedInfo, Throwable cause ) {
        super( message, detail, type, cause );
        this.extendedInfo = extendedInfo;
    }

}
