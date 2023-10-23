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
 * <p>
 * <strong>CLI Flags and Options</strong>
 * </p>
 *
 * <ul>
 * <li><code>--debug</code> - Enables debug mode</li>
 * <li><code>-c</code> - Executes the code passed as the next argument. Mutually exclusive with a template execution.</li>
 * <li><code>-config</code> - Path to the config file to use for overrides.</li>
 * </ul>
 *
 * Here are some examples of how to use the BoxLang runtime:
 *
 * <pre>
 * // Execute a template using an absolute path
 * java -jar ortus-boxlang.jar /path/to/template
 * // Execute a template using a relative path from the working directory of the process
 * // It can be a script or a cfc with a `main()` method
 * java -jar ortus-boxlang.jar mytemplate.bxs
 * java -jar ortus-boxlang.jar mycfc.bx
 * // Execute a template in debug mode
 * java -jar ortus-boxlang.jar --debug /path/to/template
 * // Execute code inline
 * java -jar ortus-boxlang.jar -c "2+2"
 * // Execute with a custom config file
 * java -jar ortus-boxlang.jar -config /path/to/config.json /path/to/template
 * </pre>
 */
public class BoxRunner {

	/**
	 * Main entry point for the BoxLang runtime.
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
		List<String>	argsList	= new ArrayList<>( Arrays.asList( args ) );
		String			current		= null;
		String			file		= null;
		String			configFile	= null;
		String			code		= null;

		// Consume args in order
		// Example: --debug
		while ( argsList.isEmpty() ) {
			current = argsList.remove( 0 );

			// Debug mode Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--debug" ) ) {
				debug = true;
				continue;
			}

			// Config File Flag, we find and continue to the next argument for the path
			if ( current.equalsIgnoreCase( "-config" ) ) {
				if ( argsList.isEmpty() ) {
					throw new ApplicationException( "Missing config file path with -config flag, it must be the next argument. [-config /path/config.json]" );
				}
				file = argsList.remove( 0 );
				continue;
			}

			// Code to execute?
			// Mutually exclusive with template
			if ( current.equalsIgnoreCase( "-c" ) ) {
				if ( argsList.isEmpty() ) {
					throw new ApplicationException( "Missing inline code to execute with -c flag." );
				}
				code = argsList.remove( 0 );
				break;
			}

			// Template to execute?
			Path templatePath = Path.of( current );
			// If path is not already absolute, make it absolute relative to the working directory of our process
			if ( ! ( templatePath.toFile().isAbsolute() ) ) {
				templatePath = Path.of( System.getProperty( "user.dir" ), templatePath.toString() );
			}
			file = templatePath.toString();
		}

		return new CLIOptions( file, debug, code, configFile );
	}

	/**
	 * Command-line options for the runtime and compiler
	 *
	 * @param templatePath The path to the template to execute. Can be a class or template. Mutally exclusive with code
	 * @param debug        Whether or not to run in debug mode.
	 * @param code         The source code to execute, if any
	 * @param configFile   The path to the config file to use
	 */
	public record CLIOptions(
	    String templatePath,
	    boolean debug,
	    String code,
	    String configFile ) {
		// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.
	}

}
