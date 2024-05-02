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
import java.util.Map;

import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
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
 * java -jar boxlang.jar /path/to/template
 * // Execute a template using a relative path from the working directory of the process
 * // It can be a script or a cfc with a `main()` method
 * java -jar boxlang.jar mytemplate.bxs
 * java -jar boxlang.jar mycfc.bx
 * // Execute a template in debug mode
 * java -jar boxlang.jar --debug /path/to/template
 * // Execute code inline
 * java -jar boxlang.jar -c "2+2"
 * // Execute with a custom config file
 * java -jar boxlang.jar -config /path/to/boxlang.json /path/to/template
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

		// Parse CLI options with Env Overrides
		CLIOptions	options	= parseEnvironmentVariables( parseCommandLineOptions( args ) );

		// Debug mode?
		if ( Boolean.TRUE.equals( options.debug() ) ) {
			System.out.println( "+++ Debug mode enabled!" );
			timer.start( "BoxRunner" );
		}

		// Get a runtime going
		BoxRuntime	boxRuntime	= BoxRuntime.getInstance( options.debug(), options.configFile(), options.runtimeHome() );

		int			exitCode	= 0;

		try {
			// Show version
			if ( Boolean.TRUE.equals( options.showVersion() ) ) {
				var versionInfo = boxRuntime.getVersionInfo();
				System.out.println( "Ortus BoxLang v" + versionInfo.get( "version" ) );
				System.out.println( "BoxLang ID: " + versionInfo.get( "boxlangId" ) );
				System.out.println( "Codename: " + versionInfo.get( "codename" ) );
				System.out.println( "Built On: " + versionInfo.get( "buildDate" ) );
				System.out.println( "Copyright Ortus Solutions, Corp" );
				System.out.println( "https://boxlang.io" );
				System.out.println( "https://ortussolutions.com" );
			}
			// Print AST
			else if ( options.printAST() && options.code() != null ) {
				boxRuntime.printSourceAST( options.code() );
			}
			// Transpile to Java
			else if ( options.transpile() ) {
				boxRuntime.printTranspiledJavaCode( options.templatePath() );
			}
			// Execute a template or a class' main() method
			// TODO: Create a struct of arguments to pass to the main method according to passed CLI name-value pairs
			else if ( options.templatePath() != null ) {
				boxRuntime.executeTemplate( options.templatePath(), new Struct() );
			}
			// Execute incoming code
			else if ( options.code() != null ) {
				// Execute a string of code
				boxRuntime.executeSource( new ByteArrayInputStream( options.code().getBytes() ) );

			}
			// REPL Mode: Execute code as read from the standard input of the process
			else {
				// Execute code from the standard input
				boxRuntime.executeSource( System.in );
			}
		} catch ( BoxRuntimeException e ) {
			ExceptionUtil.printBoxLangStackTrace( e, System.err );
			exitCode = 1;
		} finally {
			// Shutdown the runtime
			boxRuntime.shutdown();
		}

		// Debug mode tracing
		if ( Boolean.TRUE.equals( options.debug() ) ) {
			System.out.println( "+++ BoxRunner executed in " + timer.stop( "BoxRunner" ) );
		}

		System.exit( exitCode );
	}

	/**
	 * Helper method to parse environment variables and set options accordingly.
	 *
	 * @param options The CLIOptions object with the parsed options
	 *
	 * @return A new CLIOptions object with the parsed options + environment overrides
	 */
	private static CLIOptions parseEnvironmentVariables( CLIOptions options ) {
		Map<String, String>	envVars		= System.getenv();
		Boolean				debug		= options.debug();
		Boolean				printAST	= options.printAST();
		Boolean				transpile	= options.transpile();

		// Check for Debug mode
		if ( envVars.containsKey( "BOXLANG_DEBUG" ) ) {
			debug = Boolean.parseBoolean( envVars.get( "BOXLANG_DEBUG" ) );
		}

		// Transpile mode
		if ( envVars.containsKey( "BOXLANG_TRANSPILE" ) ) {
			transpile = Boolean.parseBoolean( envVars.get( "BOXLANG_TRANSPILE" ) );
		}

		// AST Mode
		if ( envVars.containsKey( "BOXLANG_PRINTAST" ) ) {
			printAST = Boolean.parseBoolean( envVars.get( "BOXLANG_PRINTAST" ) );
		}
		// Custom Config File
		String	configFile	= envVars.containsKey( "BOXLANG_CONFIG" ) ? envVars.get( "BOXLANG_CONFIG" ) : options.configFile();

		// Runtime Home
		String	runtimeHome	= envVars.containsKey( "BOXLANG_HOME" ) ? envVars.get( "BOXLANG_HOME" ) : options.runtimeHome();

		return new CLIOptions(
		    options.templatePath(),
		    debug,
		    options.code(),
		    configFile,
		    printAST,
		    transpile,
		    runtimeHome,
		    options.showVersion()
		);
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
		Boolean			debug		= null;
		Boolean			printAST	= false;
		List<String>	argsList	= new ArrayList<>( Arrays.asList( args ) );
		String			current		= null;
		String			file		= null;

		String			configFile	= null;
		String			runtimeHome	= null;
		String			code		= null;
		Boolean			transpile	= false;
		Boolean			showVersion	= false;

		// Consume args in order via the `current` variable
		while ( !argsList.isEmpty() ) {
			current = argsList.remove( 0 );

			// ShowVersion mode Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--version" ) ) {
				showVersion = true;
				continue;
			}

			// Debug mode Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--debug" ) ) {
				debug = true;
				continue;
			}

			// Print AST Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--printAST" ) ) {
				printAST = true;
				continue;
			}

			// Transpile Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--transpile" ) ) {
				transpile = true;
				continue;
			}

			// Config File Flag, we find and continue to the next argument for the path
			if ( current.equalsIgnoreCase( "--config" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing config file path with --config flag, it must be the next argument. [--config /path/boxlang.json]" );
				}
				configFile = argsList.remove( 0 );
				continue;
			}

			// Runtime Home Flag, we find and continue to the next argument for the path
			if ( current.equalsIgnoreCase( "--home" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing runtime home path with --home flag, it must be the next argument. [--home /path/to/boxlang-home]" );
				}
				runtimeHome = argsList.remove( 0 );
				continue;
			}

			// Code to execute?
			// Mutually exclusive with template
			if ( current.equalsIgnoreCase( "-c" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing inline code to execute with -c flag." );
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

		return new CLIOptions( file, debug, code, configFile, printAST, transpile, runtimeHome, showVersion );
	}

	/**
	 * Command-line options for the BoxLang runtime.
	 *
	 * @param templatePath The path to the template to execute. Can be a class or template. Mutally exclusive with code
	 * @param debug        Whether or not to run in debug mode. It can be `null` if not specified
	 * @param code         The source code to execute, if any
	 * @param configFile   The path to the config file to use
	 * @param printAST     Whether or not to print the AST of the source code
	 * @param transpile    Whether or not to transpile the source code to Java
	 * @param runtimeHome  The path to the runtime home
	 * @param showVersion  Whether or not to show the version of the runtime
	 */
	public record CLIOptions(
	    String templatePath,
	    Boolean debug,
	    String code,
	    String configFile,
	    Boolean printAST,
	    Boolean transpile,
	    String runtimeHome,
	    Boolean showVersion ) {
		// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.
	}

}
