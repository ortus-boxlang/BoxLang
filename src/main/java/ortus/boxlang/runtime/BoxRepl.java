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

import java.io.IOException;
import java.io.InputStream;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.cli.MiniConsole;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.AbortException;

/**
 * BoxLang Read-Eval-Print-Loop (REPL) implementation.
 *
 * This class provides an interactive environment for executing BoxLang code
 * with a rich command-line interface. Built on top of the MiniConsole class,
 * it provides advanced features like:
 *
 * - Interactive prompt with BoxLang ASCII art banner
 * - Arrow key navigation through command history
 * - Line-by-line code execution with immediate feedback
 * - Automatic result display for expressions
 * - Buffer output handling
 * - Exit commands (exit/quit)
 * - Exception handling and display
 * - History shortcuts (!!, !n)
 * - Cross-platform terminal support
 *
 * The REPL is designed purely for interactive use and always displays
 * prompts and banners. For non-interactive execution, use other BoxLang
 * execution methods like BoxRuntime.executeSource().
 *
 * Usage:
 *
 * <pre>
 * BoxRuntime runtime = BoxRuntime.getInstance();
 * BoxRepl repl = new BoxRepl( runtime );
 * repl.start(); // Uses System.in for interactive session
 *
 * // Or with custom input stream (still interactive)
 * repl.start( customInputStream );
 * </pre>
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.0.0
 */
public class BoxRepl {

	/**
	 * The BoxLang runtime instance
	 */
	private final BoxRuntime	runtime;

	/**
	 * The console interface for user interaction
	 */
	private MiniConsole			console;

	/**
	 * Constructor
	 *
	 * @param runtime The BoxLang runtime instance to use for code execution
	 */
	public BoxRepl( BoxRuntime runtime ) {
		this.runtime = runtime;
	}

	/**
	 * Main entry point for standalone REPL execution
	 *
	 * @param args Command line arguments (currently unused)
	 */
	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance( true );
		try {
			new BoxRepl( runtime ).start();
		} finally {
			runtime.shutdown();
			System.exit( 0 );
		}
	}

	/**
	 * Start the REPL using System.in as the input source.
	 * This is the most common usage for interactive command-line sessions.
	 *
	 * @throws IOException If an I/O error occurs during REPL operation
	 */
	public void start() {
		start( System.in );
	}

	/**
	 * Start the REPL with a custom input stream.
	 * Note: Even with custom input streams, this REPL is designed for interactive use.
	 *
	 * @param sourceStream The input stream to read BoxLang code from (currently unused, uses MiniConsole)
	 *
	 * @throws IOException If an I/O error occurs during REPL operation
	 */
	public void start( InputStream sourceStream ) {
		start( sourceStream, runtime.getRuntimeContext() );
	}

	/**
	 * Start the REPL with a custom input stream and execution context.
	 *
	 * @param sourceStream The input stream to read BoxLang code from (currently unused, uses MiniConsole)
	 * @param context      The BoxLang context to use for code execution
	 *
	 * @throws IOException If an I/O error occurs during REPL operation
	 */
	public void start( InputStream sourceStream, IBoxContext context ) {
		// Create a scripting context for REPL execution
		IBoxContext scriptingContext = new ScriptingRequestBoxContext( context );
		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			// Show the interactive banner
			showBanner();

			// Create console with custom BoxLang prompt
			String prompt = MiniConsole.color( 39 ) + "📦 BoxLang> " + MiniConsole.reset();
			console = new MiniConsole( prompt );

			// Multi-line input tracking
			StringBuilder	multiLineBuffer		= new StringBuilder();
			int				braceDepth			= 0;
			String			continuationPrompt	= MiniConsole.color( 39 ) + "        ... " + MiniConsole.reset();

			String			source;
			while ( ( source = console.readLine() ) != null ) {

				// Handle history shorthands (only when not in multi-line mode)
				if ( braceDepth == 0 && source.equals( "!!" ) ) {
					String lastCommand = console.getPreviousCommand();
					if ( lastCommand != null ) {
						source = lastCommand;
						System.out.println( "Executing: " + source );
					} else {
						System.out.println( "No previous command found." );
						continue;
					}
				} else if ( braceDepth == 0 && source.startsWith( "!" ) && source.length() > 1 ) {
					try {
						int		historyNum		= Integer.parseInt( source.substring( 1 ) );
						String	historyCommand	= console.getHistoryCommand( historyNum );
						if ( historyCommand != null ) {
							source = historyCommand;
							System.out.println( "Executing: " + source );
						} else {
							System.out.println( "History command not found." );
							continue;
						}
					} catch ( NumberFormatException ignore ) {
						// Invalid history number, treat as regular command
					}
				} else if ( braceDepth == 0 && source.equals( "history" ) ) {
					console.showHistory();
					continue;
				}
				// If the user typed "clear", then clear the console
				else if ( braceDepth == 0 && source.equals( "clear" ) ) {
					console.clear();
					showBanner();
					continue;
				}

				// Handle exit commands (only when not in multi-line mode)
				if ( braceDepth == 0 && isExitCommand( source ) ) {
					System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
					break;
				}

				// Check brace depth for multi-line support
				int currentDepthChange = calculateBraceDepth( source );
				braceDepth += currentDepthChange;

				// Add current line to buffer
				if ( multiLineBuffer.length() > 0 ) {
					multiLineBuffer.append( "\n" );
				}
				multiLineBuffer.append( source );

				// If braces are balanced, execute the complete block
				if ( braceDepth == 0 ) {
					String completeSource = multiLineBuffer.toString();
					executeReplLine( completeSource, scriptingContext );

					// Reset for next input
					multiLineBuffer.setLength( 0 );
					console.setPrompt( prompt );
				} else if ( braceDepth > 0 ) {
					// Continue reading - set continuation prompt
					console.setPrompt( continuationPrompt );
				} else {
					// Negative brace depth means unmatched closing braces
					System.out.println( "❌ Error: Unmatched closing brace '}'" );
					braceDepth = 0;
					multiLineBuffer.setLength( 0 );
					console.setPrompt( prompt );
				}
			}

		} catch ( IOException e ) {
			System.err.println( "REPL I/O Error: " + e.getMessage() );
		} finally {
			if ( console != null ) {
				console.close();
			}
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
		}
	}

	/**
	 * Display the BoxLang REPL banner and instructions.
	 */
	private void showBanner() {
		System.out.println( "   ██████   ██████  ██   ██ ██       █████  ███    ██  ██████ " );
		System.out.println( "   ██   ██ ██    ██  ██ ██  ██      ██   ██ ████   ██ ██      " );
		System.out.println( "   ██████  ██    ██   ███   ██      ███████ ██ ██  ██ ██   ███" );
		System.out.println( "   ██   ██ ██    ██  ██ ██  ██      ██   ██ ██  ██ ██ ██    ██" );
		System.out.println( "   ██████   ██████  ██   ██ ███████ ██   ██ ██   ████  ██████ " );
		System.out.println( "" );
		System.out.println( "✨ Welcome to the BoxLang Interactive REPL!" );
		System.out.println( "💡 Enter an expression, then hit enter" );
		System.out.println( "🔧 Use { } for multi-line blocks - prompt changes to '...' until balanced" );
		System.out.println( "↕️  UP/DOWN arrows navigate command history" );
		System.out.println( "📚 Type 'history' to see command history" );
		System.out.println( "🔄 Type '!!' to repeat last command, or '!n' to repeat command n" );
		System.out.println( "🧹 Press Ctrl+D to clear current line, or on empty line to exit" );
		System.out.println( "🚪 Type 'exit' or 'quit' to leave, or press Ctrl-C" );
		System.out.println( "" );
	}

	/**
	 * Check if the input is an exit command.
	 *
	 * @param input The user input to check
	 *
	 * @return true if the input is an exit command, false otherwise
	 */
	private boolean isExitCommand( String input ) {
		return input != null &&
		    ( input.toLowerCase().equals( "exit" ) || input.toLowerCase().equals( "quit" ) );
	}

	/**
	 * Execute a single line of BoxLang code in the REPL context.
	 *
	 * @param source           The BoxLang source code to execute
	 * @param scriptingContext The execution context
	 */
	private void executeReplLine( String source, IBoxContext scriptingContext ) {
		try {
			// Compile and load the statement
			BoxScript	scriptRunnable		= RunnableLoader.getInstance().loadStatement(
			    scriptingContext,
			    source,
			    BoxSourceType.BOXSCRIPT
			);

			// Execute the code
			Object		result				= scriptRunnable.invoke( scriptingContext );
			boolean		hadBufferContent	= scriptingContext.getBuffer().length() > 0;

			// Flush any buffered output
			scriptingContext.flushBuffer( false );

			// Display result if there was no buffer content and we have a result
			if ( !hadBufferContent && result != null ) {
				displayResult( result );
			} else {
				System.out.println();
			}
		} catch ( AbortException e ) {
			// Handle abort exceptions (like <cfabort>)
			scriptingContext.flushBuffer( true );
			if ( e.getCause() != null ) {
				System.out.println( "⏹️  Abort: " + e.getCause().getMessage() );
			}
		} catch ( Exception e ) {
			// Handle any other exceptions
			System.out.println( "❌ Error: " + e.getMessage() );
			// e.printStackTrace(); // Uncomment for detailed stack traces during development
		}
	}

	/**
	 * Display the result of executing a BoxLang expression.
	 *
	 * @param result The result object to display
	 */
	private void displayResult( Object result ) {
		CastAttempt<String> stringAttempt = StringCaster.attempt( result );
		if ( stringAttempt.wasSuccessful() ) {
			System.out.println( stringAttempt.get() );
		} else {
			// Handle Java arrays by converting to BoxLang Array
			if ( result.getClass().isArray() ) {
				result = Array.fromArray( ( Object[] ) result );
			}
			// Display the object's toString representation
			System.out.println( result );
		}
	}

	/**
	 * Calculate the brace depth change for a given string.
	 * Counts opening braces as +1 and closing braces as -1.
	 * Handles string literals and comments to avoid counting braces inside them.
	 *
	 * @param input The input string to analyze
	 *
	 * @return The net change in brace depth
	 */
	private int calculateBraceDepth( String input ) {
		int		depth			= 0;
		boolean	inString		= false;
		boolean	inSingleQuote	= false;
		boolean	inLineComment	= false;
		boolean	inBlockComment	= false;
		char	escapeChar		= 0;

		for ( int i = 0; i < input.length(); i++ ) {
			char c = input.charAt( i );

			// Handle escape characters in strings
			if ( escapeChar != 0 ) {
				escapeChar = 0;
				continue;
			}

			// Check for escape sequences
			if ( ( inString || inSingleQuote ) && c == '\\' ) {
				escapeChar = c;
				continue;
			}

			// Handle comments
			if ( !inString && !inSingleQuote ) {
				// Start of line comment
				if ( !inBlockComment && i < input.length() - 1 && c == '/' && input.charAt( i + 1 ) == '/' ) {
					inLineComment = true;
					i++; // Skip the second '/'
					continue;
				}
				// Start of block comment
				if ( !inLineComment && i < input.length() - 1 && c == '/' && input.charAt( i + 1 ) == '*' ) {
					inBlockComment = true;
					i++; // Skip the '*'
					continue;
				}
				// End of block comment
				if ( inBlockComment && i < input.length() - 1 && c == '*' && input.charAt( i + 1 ) == '/' ) {
					inBlockComment = false;
					i++; // Skip the '/'
					continue;
				}
			}

			// Skip characters inside comments
			if ( inLineComment || inBlockComment ) {
				continue;
			}

			// Handle string boundaries
			if ( !inSingleQuote && c == '"' ) {
				inString = !inString;
				continue;
			}
			if ( !inString && c == '\'' ) {
				inSingleQuote = !inSingleQuote;
				continue;
			}

			// Count braces only when not inside strings or comments
			if ( !inString && !inSingleQuote ) {
				if ( c == '{' ) {
					depth++;
				} else if ( c == '}' ) {
					depth--;
				}
			}
		}

		return depth;
	}

}