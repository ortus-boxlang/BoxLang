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
package ortus.boxlang.runtime.bifs.global.cli;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Read input from the CLI console" )
public class CLIRead extends BIF {

	// DO NOT CLOSE THIS SCANNER!
	// Closing it would close System.in for the entire process.
	private static final java.util.Scanner globalScanner = new java.util.Scanner( System.in );

	/**
	 * Constructor
	 */
	public CLIRead() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.prompt )
		};
	}

	/**
	 * Reads a line of text from the CLI.
	 * You can optionally provide a prompt string to display before reading the input.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.prompt An optional prompt string to display before reading input.
	 *
	 * @return The line of text read from the CLI.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the prompt string.
		String prompt = arguments.getAsString( Key.prompt );
		// If not null, print the prompt.
		if ( prompt != null ) {
			functionService.getGlobalFunction( Key.print )
			    .invoke( context, new Object[] { prompt }, false, Key.print );
		}

		// Read a line of text from the CLI using the global shared scanner.
		try {
			return globalScanner.nextLine();
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Error reading from the CLI", e );
		}
	}

}
