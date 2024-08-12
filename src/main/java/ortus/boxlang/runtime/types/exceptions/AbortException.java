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
 * This exception is thrown when the current request is aborted.
 *
 * It is a custom exception that is caught by the runtime and used to control the flow of the request.
 *
 * The type of abort can be request, page, exit-tag, exit-template, or exit-loop.
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

	// other types: exit-tag, exit-template, exit-loop

	/**
	 * Is this abort type tag? Use with exit component.
	 *
	 * @return Whether this abort affects the tag
	 */
	public Boolean isTag() {
		return type.equals( "exit-tag" );
	}

	/**
	 * Is this abort type template? Use with exit component.
	 *
	 * @return Whether this abort affects the template
	 */
	public Boolean isTemplate() {
		return type.equals( "exit-template" );
	}

	/**
	 * Is this abort type loop? Use with exit component.
	 *
	 * @return Whether this abort affects the loop
	 */
	public Boolean isLoop() {
		return type.equals( "exit-loop" );
	}
}
