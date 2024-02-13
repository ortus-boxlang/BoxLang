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
 * This represents the abort component in BoxLang
 */
public class AbortException extends RuntimeException {

	public String type = "request";

	/**
	 * Constructor
	 *
	 */
	public AbortException() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param type  The type
	 * @param cause The cause
	 */
	public AbortException( String type, Throwable cause ) {
		super( cause );
		this.type = type.toLowerCase();
	}

	/**
	 * Is this abort type request?
	 * 
	 * @return Whether this abort affects the request
	 */
	public Boolean isRequest() {
		return type.equals( "request" );
	}

	/**
	 * Is this abort type page?
	 * 
	 * @return Whether this abort affects the page
	 */
	public Boolean isPage() {
		return type.equals( "page" );
	}

}
