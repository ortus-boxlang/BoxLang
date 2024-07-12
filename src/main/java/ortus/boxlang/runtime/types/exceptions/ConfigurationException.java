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
 * This exception is thrown when an error is encountered during the configuration of the BoxLang runtime or its modules
 */
public class ConfigurationException extends BoxLangException {

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public ConfigurationException( String message ) {
		this( message, null );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 * @param cause   The cause
	 */
	public ConfigurationException( String message, Throwable cause ) {
		super( message, "configurationException", cause );
	}

}
