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
package ortus.boxlang.runtime;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.util.Timer;

/**
 * This class is in charge of executing templates/classes with a
 * BoxLang runtime. There are several CLI options that can be passed
 * to this class to control the runtime and execution.
 *
 * <strong>CLI Options</strong>
 * <ul>
 * <li><code>--debug</code> - Enables debug mode</li>
 * </ul>
 *
 * You will execute this class with the following command:
 *
 * <pre>
 * // The first argument is ALWAYS the template to execute
 * java -jar boxlang-runtime.jar /path/to/template --debug
 * </pre>
 */
public class BoxRunner {

	/**
	 * Main entry point for the BoxLang runtime
	 *
	 * @param args The command-line arguments
	 */
	public static void main( String[] args ) {
		Timer		timer	= new Timer();

		// Parse CLI options
		CLIOptions	options	= parseCommandLineOptions( args );

		// Debug mode?
		if ( options.debug() ) {
			System.out.println( "+++ Debug mode enabled!" );
			timer.start( "BoxRunner" );
		}

		// Get a runtime going
		BoxRuntime boxRuntime = BoxRuntime.getInstance( options.debug() );

		if ( options.templatePath() != null ) {
			// Execute a file
			boxRuntime.executeTemplate( options.templatePath() );
		} else if ( options.code() != null ) {
			// Execute a string of code
			boxRuntime.executeSource( new ByteArrayInputStream( options.code().getBytes() ) );
		} else {
			// Execute code as read from the standard input of the process
			boxRuntime.executeSource( System.in );
		}

		// Bye bye! Ciao Bella!
		boxRuntime.shutdown();

		if ( options.debug() ) {
			System.out.println( "+++ BoxRunner executed in " + timer.stop( "BoxRunner" ) );
		}
	}

	/**
	 * Helper method to parse command-line arguments and set options accordingly.
	 *
	 * @param args The cli arguments used
	 *
	 * @return The CLIOptions object with the parsed options
	 */
	private static CLIOptions parseCommandLineOptions( String[] args ) {
		// Initialize options with defaults
		Boolean			debug		= false;
		List<String>	argsList	= new ArrayList<String>( Arrays.asList( args ) );
		String			current		= null;
		String			file		= null;
		String			code		= null;

		// Consume args in order
		// Example: --debug
		while ( argsList.size() > 0 ) {
			current = argsList.remove( 0 );
			if ( current.equalsIgnoreCase( "--debug" ) ) {
				debug = true;
				continue;
			}
			if ( current.equalsIgnoreCase( "-c" ) ) {
				if ( argsList.isEmpty() ) {
					throw new ApplicationException( "Missing inline code to execute with -c flag." );
				}
				code = argsList.remove( 0 );
				break;
			}

			Path templatePath = Path.of( current );
			// If path is not already absolute, make it absolute relative to the worknig directory of our process
			if ( ! ( templatePath.toFile().isAbsolute() ) ) {
				templatePath = Path.of( System.getProperty( "user.dir" ), templatePath.toString() );
			}

			file = templatePath.toString();
		}

		return new CLIOptions( file, debug, code );
	}

	/**
	 * Command-line options for the runtime and compiler
	 *
	 * @param templatePath The path to the template to execute. Can be a class or template. Mutally exclusive with code
	 * @param debug        Whether or not to run in debug mode.
	 * @param code         The source code to execute, if any
	 */
	public record CLIOptions( String templatePath, boolean debug, String code ) {
		// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.
	}

}
