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

import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.validation.Validatable;

/**
 * Configuration exceptions within BoxLang
 */
public class BoxValidationException extends BoxRuntimeException {

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public BoxValidationException( String message ) {
		this( message, null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public BoxValidationException( Key caller, Validatable record, String message ) {
		this( "Record [" + record.name().getName() + "] for component [" + caller.getName() + "] " + message, null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public BoxValidationException( String message, Throwable cause ) {
		super( message, "BoxValidationException", cause );
	}

}
