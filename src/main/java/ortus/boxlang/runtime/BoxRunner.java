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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.compiler.BXCompiler;
import ortus.boxlang.compiler.CFTranspiler;
import ortus.boxlang.compiler.FeatureAudit;
import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
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
 * boxlang /path/to/template
 * // Execute a template using a relative path from the working directory of the process
 * // It can be a script or a cfc with a `main()` method
 * boxlang mytemplate.bxs
 * boxlang mycfc.bx
 * // Execute a template in debug mode
 * boxlang --bx-debug /path/to/template
 * // Execute code inline
 * boxlang --bx-code "2+2"
 * // Execute with a custom config file
 * boxlang --bx-config /path/to/boxlang.json /path/to/template
 * </pre>
 */
public class BoxRunner {

	/**
	 * A list of action commands that can be executed by the BoxRunner: compile, cftranspile, featureAudit
	 */
	private static final List<String>	ACTION_COMMANDS				= List.of( "compile", "cftranspile", "featureaudit" );

	/**
	 * The allowed template extensions that can be executed by the BoxRunner
	 */
	private static final List<String>	ALLOWED_TEMPLATE_EXECUTIONS	= List.of( ".cfm", ".cfs", ".bxm", ".bx", ".bxs" );

	/**
	 * Main entry point for the BoxLang runtime.
	 *
	 * @param args The command-line arguments
	 *
	 * @throws IOException
	 * @throws JSONObjectException
	 */
	public static int main( String[] args ) {
		Timer		timer	= new Timer();

		// Parse CLI options with Env Overrides
		CLIOptions	options	= parseEnvironmentVariables( parseCommandLineOptions( args ) );

		// Debug mode?
		if ( options.isDebugMode() ) {
			System.out.println( "+++ Debug mode enabled!" );
			timer.start( "BoxRunner" );
		}

		// Get a runtime going using the CLI options
		BoxRuntime	boxRuntime	= BoxRuntime.getInstance( options );
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
			else if ( options.templatePath() != null ) {
				System.setProperty( "boxlang.cliTemplate", options.templatePath() );
				boxRuntime.executeTemplate( options.templatePath(), options.cliArgs().toArray( new String[ 0 ] ) );
			}
			// Execute a Module
			else if ( options.targetModule() != null ) {
				System.setProperty( "boxlang.cliModule", options.targetModule() );
				boxRuntime.executeModule( options.targetModule(), options.cliArgs().toArray( new String[ 0 ] ) );
			}
			// Execute incoming code
			else if ( options.code() != null ) {
				// Execute a string of code
				boxRuntime.executeSource( new ByteArrayInputStream( options.code().getBytes() ) );
			}
			// Action Command
			else if ( options.actionCommand() != null ) {
				runActionCommand( options );
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
		if ( options.isDebugMode() ) {
			System.out.println( "+++ BoxRunner executed in " + timer.stop( "BoxRunner" ) );
		}

		return exitCode;
	}

	/**
	 * Run an action command based on the options passed.
	 *
	 * @param options The CLIOptions object with the parsed options
	 */
	private static void runActionCommand( CLIOptions options ) {
		switch ( options.actionCommand().toLowerCase() ) {
			case "compile" :
				BXCompiler.main( options.cliArgs().toArray( new String[ 0 ] ) );
				break;
			case "cftranspile" :
				CFTranspiler.main( options.cliArgs().toArray( new String[ 0 ] ) );
				break;
			case "featureaudit" :
				FeatureAudit.main( options.cliArgs().toArray( new String[ 0 ] ) );
				break;
			default :
				throw new BoxRuntimeException( "Unknown action command: " + options.actionCommand() );
		}
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
		    options.showVersion(),
		    options.cliArgs(),
		    options.cliArgsRaw(),
		    options.targetModule(),
		    options.actionCommand()
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
		Boolean			debug			= null;
		Boolean			printAST		= false;
		List<String>	argsList		= new ArrayList<>( Arrays.asList( args ) );
		String			current			= null;
		String			file			= null;
		String			targetModule	= null;
		String			configFile		= null;
		String			runtimeHome		= null;
		String			code			= null;
		Boolean			transpile		= false;
		Boolean			showVersion		= false;
		List<String>	cliArgs			= new ArrayList<>();
		String			actionCommand	= null;

		// Consume args in order via the `current` variable
		while ( !argsList.isEmpty() ) {
			current = argsList.remove( 0 );

			// ShowVersion mode Flag, we find and break off
			if ( current.equalsIgnoreCase( "--version" ) ) {
				showVersion = true;
				break;
			}

			// Debug mode Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--bx-debug" ) ) {
				debug = true;
				continue;
			}

			// Print AST Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--bx-printAST" ) ) {
				printAST = true;
				continue;
			}

			// Transpile Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--bx-transpile" ) ) {
				transpile = true;
				continue;
			}

			// Config File Flag, we find and continue to the next argument for the path
			if ( current.equalsIgnoreCase( "--bx-config" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing config file path with --config flag, it must be the next argument. [--config /path/boxlang.json]" );
				}
				configFile = argsList.remove( 0 );
				continue;
			}

			// Runtime Home Flag, we find and continue to the next argument for the path
			if ( current.equalsIgnoreCase( "--bx-home" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing runtime home path with --home flag, it must be the next argument. [--home /path/to/boxlang-home]" );
				}
				runtimeHome = argsList.remove( 0 );
				continue;
			}

			// Code to execute?
			// Mutually exclusive with template
			if ( current.equalsIgnoreCase( "--bx-code" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing inline code to execute with -c flag." );
				}
				code = argsList.remove( 0 );
				break;
			}

			String[]	currentParts	= current.split( "\\." );
			String		currentExt		= "";

			if ( currentParts.length > 0 ) {
				currentExt = "." + currentParts[ currentParts.length - 1 ];
			}

			// Template to execute?
			if ( actionCommand == null && ALLOWED_TEMPLATE_EXECUTIONS.contains( currentExt ) ) {
				file = templateToAbsolute( current );
				continue;
			}

			// Is it a shebang script to execute
			if ( actionCommand == null && isShebangScript( current ) ) {
				file = getSheBangScript( current );
				continue;
			}

			// Is this a module execution
			if ( current.startsWith( "module:" ) ) {
				// Remove the prefix
				targetModule = current.substring( 7 );
				continue;
			}

			// Is this an action command?
			if ( ACTION_COMMANDS.contains( current.toLowerCase() ) ) {
				actionCommand = current;
				// Add the remaining arguments into the cliArgs and break off
				cliArgs.addAll( argsList );
				continue;
			}

			// add it to the list of arguments
			cliArgs.add( current );
		}

		return new CLIOptions( file, debug, code, configFile, printAST, transpile, runtimeHome, showVersion, cliArgs, args, targetModule, actionCommand );

	}

	/**
	 * Convert a template path to an absolute path from the currently executing directory.
	 * If the passed string is not a valid path, the original string is returned unchanged.
	 *
	 * @param path The path to the template
	 *
	 * @return The absolute path to the template, or the original path if it was already absolute
	 */
	private static String templateToAbsolute( String path ) {
		try {
			Path templatePath = Path.of( path );
			// If path is not already absolute, make it absolute relative to the working directory of our process
			if ( ! ( templatePath.toFile().isAbsolute() ) ) {
				templatePath = Path.of( System.getProperty( "user.dir" ), templatePath.toString() );
			}
			return templatePath.toString();
		} catch ( InvalidPathException e ) {
			return path;
		}
	}

	/**
	 * Verify if a file is a shebang script.
	 *
	 * @param path The path to the file
	 *
	 * @return Whether or not the file is a shebang script
	 */
	private static boolean isShebangScript( String path ) {
		Path templatePath;
		try {
			templatePath = Path.of( templateToAbsolute( path ) );
		} catch ( InvalidPathException e ) {
			return false;
		}
		// return false if the file doesn't exist
		if ( !Files.exists( templatePath ) ) {
			return false;
		}

		try ( Stream<String> lines = Files.lines( templatePath ) ) {
			String firstLine = lines.findFirst().orElse( "" );
			return firstLine.startsWith( "#!" );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Remove the shebang line from a file and prep a new one to execute.
	 *
	 * @param path The path to the file
	 *
	 * @return The path to the file without the shebang line to execute
	 */
	private static String getSheBangScript( String path ) {
		Path templatePath = Path.of( templateToAbsolute( path ) );

		try ( Stream<String> lines = Files.lines( templatePath ) ) {
			Path temp = Files.createTempFile( "blscript", ".bxs" );
			Files.write(
			    temp,
			    lines.skip( 1 ).collect( Collectors.joining( System.lineSeparator() ) ).getBytes()
			);
			return temp.toString();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

}
