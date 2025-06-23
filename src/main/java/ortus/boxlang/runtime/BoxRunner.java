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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.compiler.BXCompiler;
import ortus.boxlang.compiler.CFTranspiler;
import ortus.boxlang.compiler.FeatureAudit;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.async.tasks.BoxScheduler;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
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
 * <li><code>-c</code> - Executes the code passed as the next argument. Mutually
 * exclusive with a template execution.</li>
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
	 * A list of action commands that can be executed by the BoxRunner:
	 * compile, cftranspile, featureAudit, schedule
	 */
	private static final List<String>	ACTION_COMMANDS				= List.of(
	    "compile",
	    "cftranspile",
	    "featureaudit",
	    "schedule" );

	/**
	 * The allowed template extensions that can be executed by the BoxRunner
	 */
	private static final List<String>	ALLOWED_TEMPLATE_EXECUTIONS	= List.of( ".cfm", ".cfs", ".bxm", ".bx", ".bxs" );

	/**
	 * An exit code indicator for the BoxRunner
	 */
	public static int					exitCode					= 0;

	/**
	 * Execute the BoxLang runtime with the passed arguments.
	 *
	 * @param args The command-line arguments
	 *
	 * @throws IOException
	 * @throws JSONObjectException
	 */
	public static void main( String[] args ) {
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
				System.out.println( "Ortus BoxLang™ v" + versionInfo.get( "version" ) );
				System.out.println( "BoxLang™ ID: " + versionInfo.get( "boxlangId" ) );
				System.out.println( "Codename: " + versionInfo.get( "codename" ) );
				System.out.println( "Built On: " + versionInfo.get( "buildDate" ) );
				System.out.println( "Copyright Ortus Solutions, Corp™" );
				System.out.println( "https://boxlang.io" );
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
				runActionCommand( options, boxRuntime );
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

		BoxRunner.exitCode = exitCode;
		System.exit( exitCode );
	}

	/**
	 * Prints the help message for the schedule command.
	 */
	private static void printScheduleHelp() {
		System.out.println( "⏰ BoxLang Scheduler - Run and manage BoxLang scheduler files" );
		System.out.println();
		System.out.println( "📋 USAGE:" );
		System.out.println( "  boxlang schedule <SCHEDULER_FILE>             # 🔧 Using OS binary" );
		System.out.println( "  java -jar boxlang.jar schedule <SCHEDULER_FILE> # 🐍 Using Java JAR" );
		System.out.println();
		System.out.println( "⚙️  OPTIONS:" );
		System.out.println( "  -h, --help                      ❓ Show this help message and exit" );
		System.out.println();
		System.out.println( "📂 SCHEDULER FILE REQUIREMENTS:" );
		System.out.println( "  • Must be a .bx (BoxLang) file" );
		System.out.println( "  • File should contain a BoxLang component with scheduler definitions" );
		System.out.println( "  • Scheduler will run continuously until Ctrl+C is pressed" );
		System.out.println( "  • File path can be absolute or relative to current directory" );
		System.out.println();
		System.out.println( "🔄 SCHEDULER LIFECYCLE:" );
		System.out.println( "  1. File is compiled and validated" );
		System.out.println( "  2. Scheduler component is instantiated" );
		System.out.println( "  3. Scheduler is registered with BoxLang runtime" );
		System.out.println( "  4. Scheduler tasks begin execution" );
		System.out.println( "  5. Press Ctrl+C to gracefully shutdown" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  # ⏰ Run a basic scheduler" );
		System.out.println( "  boxlang schedule ./schedulers/MainScheduler.bx" );
		System.out.println();
		System.out.println( "  # 📁 Run scheduler with absolute path" );
		System.out.println( "  boxlang schedule /opt/myapp/schedulers/TaskRunner.bx" );
		System.out.println();
		System.out.println( "  # 🔧 Run scheduler in project directory" );
		System.out.println( "  cd /my/project && boxlang schedule schedulers/CronJobs.bx" );
		System.out.println();
		System.out.println( "📖 More Information:" );
		System.out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		System.out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		System.out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		System.out.println();
	}

	/**
	 * Run an action command based on the options passed.
	 *
	 * @param options The CLIOptions object with the parsed options
	 * @param runtime The BoxRuntime object
	 */
	private static void runActionCommand( CLIOptions options, BoxRuntime runtime ) {
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
			case "schedule" :
				// Check for help first
				if ( !options.cliArgs().isEmpty() &&
				    ( options.cliArgs().getFirst().equalsIgnoreCase( "--help" ) ||
				        options.cliArgs().getFirst().equalsIgnoreCase( "-h" ) ) ) {
					printScheduleHelp();
					System.exit( 0 );
				}
				if ( options.cliArgs().isEmpty() ) {
					throw new BoxRuntimeException(
					    "schedule command requires a scheduler file path. Use: boxlang schedule --help" );
				}
				runScheduler( options.cliArgs().getFirst(), runtime );
				break;
			default :
				throw new BoxRuntimeException( "Unknown action command: " + options.actionCommand() );
		}
	}

	/**
	 * Run the passed in scheduler path. Block, show a message and wait for the
	 * user to ctrl+c to exit.
	 *
	 * @param schedulerPath The path to the scheduler to run
	 * @param runtime       The BoxRuntime object
	 */
	private static void runScheduler( String schedulerPath, BoxRuntime runtime ) {
		// Check if the scheduler path is valid
		if ( !StringUtils.endsWithAny( schedulerPath, ".bx" ) ) {
			throw new BoxRuntimeException( "Scheduler must be a .bx file, found: " + schedulerPath );
		}

		// Prep the execution
		Path					targetSchedulerPath	= Paths.get( schedulerPath ).normalize().toAbsolutePath();
		IBoxContext				runtimeContext		= runtime.getRuntimeContext();
		IBoxContext				scriptingContext	= new ScriptingRequestBoxContext( runtimeContext, targetSchedulerPath.toUri() );
		BaseApplicationListener	listener			= scriptingContext.getRequestContext().getApplicationListener();
		RequestBoxContext.setCurrent( scriptingContext.getRequestContext() );
		Throwable			errorToHandle		= null;
		SchedulerService	schedulerService	= runtime.getSchedulerService();

		// FIRE!
		try {
			System.out.println( "- Starting scheduler from file: [" + schedulerPath + "]" );

			// Compile the scheduler
			Class<IBoxRunnable>	targetSchedulerClass	= RunnableLoader.getInstance()
			    .loadClass(
			        ResolvedFilePath.of( targetSchedulerPath ),
			        scriptingContext );
			// Construct the scheduler
			IClassRunnable		targetScheduler			= ( IClassRunnable ) DynamicObject.of( targetSchedulerClass )
			    .invokeConstructor( scriptingContext )
			    .getTargetInstance();
			// Create the proxy
			IScheduler			boxScheduler			= new BoxScheduler( targetScheduler, scriptingContext );

			// Startup the listener
			boolean				result					= listener.onRequestStart( scriptingContext, new Object[] { schedulerPath } );
			if ( result ) {
				// Register the requested scheduler
				schedulerService.loadScheduler( Key.of( boxScheduler.getSchedulerName() ), boxScheduler );

				// Show the message
				System.out.println( "√ Scheduler registered successfully" );
				System.out.println( "Press Ctrl+C to stop the scheduler and exit." );
				System.out.println( "=========================================" );
				System.out.println( "" );
				CountDownLatch stopLatch = new CountDownLatch( 1 );

				// Start the scheduler
				boxScheduler.startup();

				// Add a shutdown hook to gracefully stop the scheduler
				Runtime.getRuntime().addShutdownHook( new Thread( () -> {
					System.out.println( "- Shutting down scheduler..." );
					try {
						// Force shut it down
						boxScheduler.shutdown( true );
					} catch ( Exception e ) {
						e.printStackTrace();
					} finally {
						stopLatch.countDown(); // release the latch so the main thread can exit
					}
				} ) );

				// Block the main thread
				stopLatch.await();
			}
		} catch ( AbortException e ) {
			try {
				listener.onAbort( scriptingContext, new Object[] { schedulerPath } );
			} catch ( Throwable ae ) {
				// Opps, an error while handling onAbort
				errorToHandle = ae;
			}
			scriptingContext.flushBuffer( true );
			if ( e.getCause() != null ) {
				// This will always be an instance of CustomException
				throw ( RuntimeException ) e.getCause();
			}
		} catch ( Exception e ) {
			errorToHandle = e;
		} finally {
			try {
				listener.onRequestEnd( scriptingContext, new Object[] { schedulerPath } );
			} catch ( Throwable e ) {
				// Opps, an error while handling onRequestEnd
				errorToHandle = e;
			}
			scriptingContext.flushBuffer( false );

			if ( errorToHandle != null ) {
				// Log it
				runtime.getLoggingService().getExceptionLogger().error( errorToHandle.getMessage(), errorToHandle );

				try {
					if ( !listener.onError( scriptingContext, new Object[] { errorToHandle, "" } ) ) {
						throw errorToHandle;
					}
					// This is a failsafe in case the onError blows up.
				} catch ( Throwable t ) {
					errorToHandle.printStackTrace();
					ExceptionUtil.throwException( t );
				}
			}
			scriptingContext.flushBuffer( false );
			RequestBoxContext.removeCurrent();
		}
	}

	/**
	 * Helper method to parse environment variables and set options accordingly.
	 *
	 * @param options The CLIOptions object with the parsed options
	 *
	 * @return A new CLIOptions object with the parsed options + environment
	 *         overrides
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
		String	configFile	= envVars.containsKey( "BOXLANG_CONFIG" ) ? envVars.get( "BOXLANG_CONFIG" )
		    : options.configFile();

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
		    options.actionCommand() );
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
		String			currentArgument	= null;
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
			currentArgument = argsList.remove( 0 );

			// Help Flag, we find and break off
			if ( currentArgument.equalsIgnoreCase( "--help" ) || currentArgument.equalsIgnoreCase( "-h" ) ) {
				printHelp();
				System.exit( 0 );
			}

			// ShowVersion mode Flag, we find and break off
			if ( currentArgument.equalsIgnoreCase( "--version" ) ) {
				showVersion = true;
				break;
			}

			// Debug mode Flag, we find and continue to the next argument
			if ( currentArgument.equalsIgnoreCase( "--bx-debug" ) ) {
				debug = true;
				continue;
			}

			// Print AST Flag, we find and continue to the next argument
			if ( currentArgument.equalsIgnoreCase( "--bx-printAST" ) ) {
				printAST = true;
				continue;
			}

			// Transpile Flag, we find and continue to the next argument
			if ( currentArgument.equalsIgnoreCase( "--bx-transpile" ) ) {
				transpile = true;
				continue;
			}

			// Config File Flag, we find and continue to the next argument for the path
			if ( currentArgument.equalsIgnoreCase( "--bx-config" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException(
					    "Missing config file path with --config flag, it must be the next argument. [--config /path/boxlang.json]" );
				}
				configFile = argsList.remove( 0 );
				continue;
			}

			// Runtime Home Flag, we find and continue to the next argument for the path
			if ( currentArgument.equalsIgnoreCase( "--bx-home" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException(
					    "Missing runtime home path with --home flag, it must be the next argument. [--home /path/to/boxlang-home]" );
				}
				runtimeHome = argsList.remove( 0 );
				continue;
			}

			// Code to execute?
			// Mutually exclusive with template
			if ( currentArgument.equalsIgnoreCase( "--bx-code" ) ) {
				if ( argsList.isEmpty() ) {
					throw new BoxRuntimeException( "Missing inline code to execute with --bx-code flag." );
				}
				code = argsList.remove( 0 );
				break;
			}

			// Is this an action command?
			if ( ACTION_COMMANDS.contains( currentArgument.toLowerCase() ) ) {
				actionCommand = currentArgument;
				cliArgs.addAll( argsList );
				break;
			}

			// Is it a shebang script to execute
			if ( isShebangScript( currentArgument ) ) {
				file = getSheBangScript( currentArgument );
				cliArgs.addAll( argsList );
				break;
			}

			// Template to execute?
			String targetPath = getExecutableTemplate( currentArgument );
			if ( targetPath != null ) {
				file = targetPath;
				cliArgs.addAll( argsList );
				break;
			}

			// Is this a module execution
			if ( currentArgument.startsWith( "module:" ) ) {
				// Remove the prefix
				targetModule = currentArgument.substring( 7 );
				cliArgs.addAll( argsList );
				break;
			}

			// add it to the list of arguments
			cliArgs.add( currentArgument );
		}

		return new CLIOptions(
		    file,
		    debug,
		    code,
		    configFile,
		    printAST,
		    transpile,
		    runtimeHome,
		    showVersion,
		    cliArgs,
		    args,
		    targetModule,
		    actionCommand );
	}

	/**
	 * Verifies if the passed in path is a valid template for execution
	 *
	 * @param path Possible path to the template
	 *
	 * @return The absolute path if it's valid and exists, null otherwise
	 */
	private static String getExecutableTemplate( String path ) {
		String[]	currentParts	= path.split( "\\." );
		String		currentExt		= "";

		if ( currentParts.length > 0 ) {
			currentExt = "." + currentParts[ currentParts.length - 1 ].toLowerCase();
		}
		// Do we have the extension? If not, let's assume it's a class
		boolean presumptiveExtension = false;
		if ( currentExt.isEmpty() ) {
			currentExt				= "bx";
			path					+= ".bx";
			presumptiveExtension	= true;
		}

		// Check if the extension is allowed or not
		if ( !ALLOWED_TEMPLATE_EXECUTIONS.contains( currentExt ) ) {
			return null;
		}

		// Check if the file exists
		String absPath = templateToAbsolute( path );
		try {
			Path templatePath = Paths.get( absPath );

			if ( !Files.exists( templatePath ) ) {
				if ( !presumptiveExtension ) {
					throw new BoxRuntimeException( "The template [" + path + "] does not exist." );
				}
				return null;
			}
		} catch ( InvalidPathException e ) {
			return null;
		}
		return absPath;
	}

	/**
	 * Convert a template path to an absolute path from the currently executing
	 * directory.
	 * If the passed string is not a valid path, the original string is returned
	 * unchanged.
	 *
	 * @param path The path to the template
	 *
	 * @return The absolute path to the template, or the original path if it was
	 *         already absolute
	 */
	private static String templateToAbsolute( String path ) {
		try {
			Path templatePath = Path.of( path );
			// If path is not already absolute, make it absolute relative to the working
			// directory of our process
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
			    lines.skip( 1 ).collect( Collectors.joining( System.lineSeparator() ) ).getBytes() );
			return temp.toString();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Prints the help message for the BoxRunner CLI tool.
	 */
	private static void printHelp() {
		System.out.println( "⚡ BoxLang Runtime - Execute templates, components, and manage your BoxLang projects" );
		System.out.println();
		System.out.println( "📋 USAGE:" );
		System.out.println( "  boxlang [OPTIONS] [COMMAND] [ARGS...]         # 🔧 Using OS binary" );
		System.out.println( "  java -jar boxlang.jar [OPTIONS] [FILE]        # 🐍 Using Java JAR" );
		System.out.println();
		System.out.println( "🔧 GLOBAL OPTIONS:" );
		System.out.println( "  -h, --help                      ❓ Show this help message and exit" );
		System.out.println( "      --version                   📋 Show version information and exit" );
		System.out.println( "      --bx-debug                  🐛 Enable debug mode with timing information" );
		System.out.println( "      --bx-config <PATH>          ⚙️  Use custom configuration file" );
		System.out.println( "      --bx-home <PATH>           🏠 Set BoxLang runtime home directory" );
		System.out.println( "      --bx-code <CODE>           💻 Execute inline BoxLang code" );
		System.out.println( "      --bx-printAST              🌳 Print Abstract Syntax Tree for code" );
		System.out.println( "      --bx-transpile             🔄 Transpile BoxLang code to Java" );
		System.out.println();
		System.out.println( "🚀 ACTION COMMANDS:" );
		System.out.println( "  compile                         📦 Pre-compile BoxLang templates to class files" );
		System.out.println( "                                     Use: boxlang compile --help" );
		System.out.println( "  cftranspile                     🔄 Transpile ColdFusion code to BoxLang" );
		System.out.println( "                                     Use: boxlang cftranspile --help" );
		System.out.println( "  featureaudit                    🔍 Audit code for BoxLang feature compatibility" );
		System.out.println( "                                     Use: boxlang featureaudit --help" );
		System.out.println( "  schedule <SCHEDULER_FILE>       ⏰ Run a BoxLang scheduler from file" );
		System.out.println( "                                     Use: boxlang schedule --help" );
		System.out.println();
		System.out.println( "📂 FILE EXECUTION:" );
		System.out.println( "  • Execute BoxLang templates directly by providing a file path" );
		System.out.println( "  • Supported extensions: .cfm, .cfs, .bxm, .bx, .bxs" );
		System.out.println( "  • Shebang scripts are automatically detected and executed" );
		System.out.println( "  • Components with main() methods can be executed as entry points" );
		System.out.println();
		System.out.println( "🧩 MODULE EXECUTION:" );
		System.out.println( "  • Execute BoxLang modules using the module: prefix" );
		System.out.println( "  • Example: boxlang module:myModule arg1 arg2" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  # ⚡ Execute a BoxLang template" );
		System.out.println( "  boxlang myapp.bx" );
		System.out.println();
		System.out.println( "  # 💻 Execute inline code" );
		System.out.println( "  boxlang --bx-code \"println('Hello BoxLang!')\"" );
		System.out.println();
		System.out.println( "  # 🐛 Execute with debug mode and custom config" );
		System.out.println( "  boxlang --bx-debug --bx-config ./custom.json myapp.bx" );
		System.out.println();
		System.out.println( "  # 📦 Pre-compile templates" );
		System.out.println( "  boxlang compile --source ./src --target ./compiled" );
		System.out.println();
		System.out.println( "  # 🔄 Transpile ColdFusion to BoxLang" );
		System.out.println( "  boxlang cftranspile --source ./legacy --target ./modern" );
		System.out.println();
		System.out.println( "  # 🔍 Audit code features" );
		System.out.println( "  boxlang featureaudit --source ./myapp --output report.json" );
		System.out.println();
		System.out.println( "  # ⏰ Run a scheduler" );
		System.out.println( "  boxlang schedule ./schedulers/MyScheduler.bx" );
		System.out.println();
		System.out.println( "  # 🧩 Execute a module" );
		System.out.println( "  boxlang module:myModule arg1 arg2" );
		System.out.println();
		System.out.println( "  # 🌳 Print AST for code analysis" );
		System.out.println( "  boxlang --bx-printAST --bx-code \"x = 1 + 2\"" );
		System.out.println();
		System.out.println( "🔄 REPL MODE:" );
		System.out.println( "  • When no arguments are provided, BoxLang starts in REPL mode" );
		System.out.println( "  • Interactive environment for testing and development" );
		System.out.println( "  • Type expressions and see results immediately" );
		System.out.println( "  • Press Ctrl+C to exit REPL mode" );
		System.out.println();
		System.out.println( "🌍 ENVIRONMENT VARIABLES:" );
		System.out.println( "  BOXLANG_DEBUG=true              🐛 Enable debug mode" );
		System.out.println( "  BOXLANG_CONFIG=/path/config.json ⚙️  Override configuration file" );
		System.out.println( "  BOXLANG_HOME=/path/to/home      🏠 Set runtime home directory" );
		System.out.println( "  BOXLANG_TRANSPILE=true          🔄 Enable transpile mode" );
		System.out.println( "  BOXLANG_PRINTAST=true           🌳 Enable AST printing" );
		System.out.println();
		System.out.println( "📖 More Information:" );
		System.out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		System.out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		System.out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		System.out.println( "  🌐 Website: https://boxlang.io" );
		System.out.println();
	}

	/**
	 * An exit code indicator for the BoxRunner
	 */
	public static int getExitCode() {
		return exitCode;
	}

	/**
	 * Sets the exit code for the BoxRunner
	 *
	 * @param exitCode The exit code to set
	 */
	public static void setExitCode( int exitCode ) {
		BoxRunner.exitCode = exitCode;
	}

}
