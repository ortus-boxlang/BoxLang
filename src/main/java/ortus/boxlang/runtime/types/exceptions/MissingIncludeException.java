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
 * This exception is thrown when a an included template file cannot be located.
 */
public class MissingIncludeException extends BoxLangException {

	/**
	 * Name of file that could not be included.
	 */
	protected String missingFileName = "";

	/**
	 * Constructor
	 *
	 * @param message         The message
	 * @param missingFileName The missingFileName
	 */
	public MissingIncludeException( String message, String missingFileName ) {
		this( message, null, missingFileName, null );
	}

	/**
	 * Constructor
	 *
	 * @param message         The message
	 * @param detail          The detail
	 * @param missingFileName The missingFileName
	 * @param cause           The cause
	 */
	public MissingIncludeException( String message, String detail, String missingFileName, Throwable cause ) {
		super( message, detail, "missingInclude", cause );
		this.missingFileName = missingFileName;
	}

	/**
	 * Get the missing file name
	 *
	 * @return The missing file name
	 */
	public String getMissingFileName() {
		return missingFileName;
	}

	public IStruct dataAsStruct() {
		IStruct result = super.dataAsStruct();
		result.put( Key.missingFileName, missingFileName );
		return result;
	}

}
