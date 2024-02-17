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

import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.ast.Issue;

/**
 * Thrown when a scope is not found
 */
public class ParseException extends BoxRuntimeException {

	List<Issue> issues;

	/**
	 * Constructor
	 *
	 * @param issues List of issues encountered during parsing.
	 */
	public ParseException( List<Issue> issues, String source ) {
		super( "Error compiling [ " + source + " ]. " + issuesAsString( issues ) );

		this.issues			= issues;
		this.extendedInfo	= issuesAsString( issues );
	}

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 * @param cause   The cause
	 */
	public ParseException( String message, Throwable cause ) {
		super( message, cause );
	}

	public static String issuesAsString( List<Issue> issues ) {
		return issues.stream()
		    .map( Issue::toString )
		    .collect( Collectors.joining( "\n" ) );
	}

}
