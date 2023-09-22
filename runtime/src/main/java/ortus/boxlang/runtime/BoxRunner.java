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

import java.util.HashMap;
import java.util.Map;

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
		Timer timer = new Timer();

		// Verify incoming arguments
		if ( args.length == 0 ) {
			System.out.println( "No script specified. We need a script or class to execute!" );
			System.exit( 1 );
		}

		// Parse CLI options
		CLIOptions options = parseCommandLineOptions( args );

		// Debug mode?
		if ( options.debug() ) {
			System.out.println( "+++ Debug mode enabled!" );
			timer.start( "BoxRunner" );
		}

		// Get a runtime going
		BoxRuntime boxRuntime = BoxRuntime.getInstance( options.debug() );

		boxRuntime.executeTemplate( options.templatePath() );

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
		Map<String, Boolean> options = new HashMap<>( Map.of( "debug", false ) );

		// Verify Flags
		// Example: --debug
		for ( String arg : args ) {
			if ( arg.equalsIgnoreCase( "--debug" ) ) {
				options.put( "debug", true );
			}
			// Add more options handling here as needed...
		}

		return new CLIOptions( args[ 0 ], options.get( "debug" ) );
	}

	/**
	 * Command-line options for the runtime and compiler
	 *
	 * @param templatePath The path to the template to execute. Can be a class or template
	 * @param debug        Whether or not to run in debug mode.
	 */
	public record CLIOptions( String templatePath, boolean debug ) {
		// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.
	}

}
