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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * BoxLang Read-Eval-Print-Loop (REPL) implementation.
 *
 * This class provides an interactive environment for executing BoxLang code
 * from an input stream, typically System.in for command-line interaction.
 *
 * Features:
 * - Interactive prompt with BoxLang ASCII art banner
 * - Line-by-line code execution with immediate feedback
 * - Automatic result display for expressions
 * - Buffer output handling
 * - Exit commands (exit/quit)
 * - Exception handling and display
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
 */
public class BoxRepl {

	/**
	 * The BoxLang runtime instance
	 */
	private final BoxRuntime	runtime;

	/**
	 * Command history storage
	 */
	private final List<String>	commandHistory		= new ArrayList<>();

	/**
	 * Current position in command history (-1 means not navigating history)
	 */
	private int					historyIndex		= -1;

	/**
	 * Maximum number of commands to keep in history
	 */
	private static final int	MAX_HISTORY_SIZE	= 100;

	/**
	 * ANSI escape sequences for terminal control
	 */
	private static final String	ANSI_CLEAR_LINE		= "\r\033[2K";

	/**
	 * Constructor
	 *
	 * @param runtime The BoxLang runtime instance to use for code execution
	 */
	public BoxRepl( BoxRuntime runtime ) {
		this.runtime = runtime;
	}

	/**
	 * Start the REPL using System.in as the input source.
	 * This is the most common usage for interactive command-line sessions.
	 */
	public void start() {
		start( System.in );
	}

	/**
	 * Start the REPL with a custom input stream.
	 *
	 * @param sourceStream The input stream to read BoxLang code from
	 */
	public void start( InputStream sourceStream ) {
		start( sourceStream, runtime.getRuntimeContext() );
	}

	/**
	 * Start the REPL with a custom input stream and execution context.
	 *
	 * @param sourceStream The input stream to read BoxLang code from
	 * @param context      The BoxLang context to use for code execution
	 */
	public void start( InputStream sourceStream, IBoxContext context ) {
		// Create a scripting context for REPL execution
		IBoxContext	scriptingContext	= new ScriptingRequestBoxContext( context );
		String		source;
		RequestBoxContext.setCurrent( scriptingContext.getParentOfType( RequestBoxContext.class ) );
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			// Show the interactive banner
			showBanner();

			// For non-System.in streams, fall back to simple line reading
			if ( sourceStream != System.in ) {
				BufferedReader reader = new BufferedReader( new InputStreamReader( sourceStream ) );
				// Traditional REPL loop for non-interactive input
				while ( ( source = reader.readLine() ) != null ) {
					// Check for exit commands
					if ( isExitCommand( source ) ) {
						System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
						break;
					}

					// Execute the source code
					executeReplLine( source, scriptingContext );
				}
			} else {
				// Interactive REPL loop with history support
				while ( ( source = readLineWithHistory() ) != null ) {
					// Check for exit commands
					if ( isExitCommand( source ) ) {
						System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
						break;
					}

					// Execute the source code
					executeReplLine( source, scriptingContext );
				}
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error reading source stream", e );
		} finally {
			RequestBoxContext.removeCurrent();
			Thread.currentThread().setContextClassLoader( oldClassLoader );
		}
	}

	/**
	 * Display the BoxLang REPL banner and instructions.
	 */
	private void showBanner() {
		System.out.println( "🚀 ██████   ██████  ██   ██ ██       █████  ███    ██  ██████ " );
		System.out.println( "   ██   ██ ██    ██  ██ ██  ██      ██   ██ ████   ██ ██      " );
		System.out.println( "   ██████  ██    ██   ███   ██      ███████ ██ ██  ██ ██   ███" );
		System.out.println( "   ██   ██ ██    ██  ██ ██  ██      ██   ██ ██  ██ ██ ██    ██" );
		System.out.println( "   ██████   ██████  ██   ██ ███████ ██   ██ ██   ████  ██████  🎯" );
		System.out.println( "" );
		System.out.println( "✨ Welcome to the BoxLang Interactive REPL!" );
		System.out.println( "💡 Enter an expression, then hit enter" );
		System.out.println( "� Use UP/DOWN arrow keys to navigate command history" );
		System.out.println( "�🚪 Type 'exit' or 'quit' to leave, or press Ctrl-C" );
		System.out.println( "" );
		System.out.print( "📦 BoxLang> " );
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

		// Show prompt for next input
		System.out.print( "📦 BoxLang> " );
	}

	/**
	 * Display the result of executing a BoxLang expression.
	 *
	 * @param result The result object to display
	 */
	private void displayResult( Object result ) {
		// Try to convert to string first
		CastAttempt<String> stringAttempt = StringCaster.attempt( result );
		if ( stringAttempt.wasSuccessful() ) {
			System.out.println( "➡️  " + stringAttempt.get() );
		} else {
			// Handle Java arrays by converting to BoxLang Array
			if ( result.getClass().isArray() ) {
				result = Array.fromArray( ( Object[] ) result );
			}
			// Display the object's toString representation
			System.out.println( "➡️  " + result );
		}
	}

	/**
	 * Get the runtime instance used by this REPL.
	 *
	 * @return The BoxRuntime instance
	 */
	public BoxRuntime getRuntime() {
		return runtime;
	}

	/**
	 * Add a command to the history.
	 *
	 * @param command The command to add to history
	 */
	private void addToHistory( String command ) {
		if ( command == null || command.trim().isEmpty() ) {
			return;
		}

		// Don't add duplicate consecutive commands
		if ( !commandHistory.isEmpty() && commandHistory.get( commandHistory.size() - 1 ).equals( command ) ) {
			return;
		}

		commandHistory.add( command );

		// Maintain maximum history size
		if ( commandHistory.size() > MAX_HISTORY_SIZE ) {
			commandHistory.remove( 0 );
		}

		// Reset history navigation
		historyIndex = -1;
	}

	/**
	 * Clear the current line and move cursor to beginning.
	 */
	private void clearLine() {
		System.out.print( ANSI_CLEAR_LINE );
	}

	/**
	 * Redraw the prompt and current line content.
	 *
	 * @param lineContent The current line content to display
	 */
	private void redrawLine( String lineContent ) {
		clearLine();
		System.out.print( "📦 BoxLang> " + lineContent );
	}

	/**
	 * Read a line with history support and arrow key navigation.
	 * This method handles raw terminal input to support arrow keys for history navigation.
	 *
	 * @return The input line, or null if EOF is reached
	 * 
	 * @throws IOException If an I/O error occurs
	 */
	private String readLineWithHistory() throws IOException {
		StringBuilder	lineBuffer	= new StringBuilder();
		int				ch;

		while ( ( ch = System.in.read() ) != -1 ) {
			switch ( ch ) {
				case '\n' :
				case '\r' :
					// Enter pressed - return the current line
					System.out.println(); // Move to next line
					String result = lineBuffer.toString();
					if ( !result.trim().isEmpty() ) {
						addToHistory( result );
					}
					return result;

				case 127 : // Backspace (DEL)
				case 8 :   // Backspace (BS)
					if ( lineBuffer.length() > 0 ) {
						lineBuffer.deleteCharAt( lineBuffer.length() - 1 );
						redrawLine( lineBuffer.toString() );
					}
					break;

				case 27 : // ESC - potential arrow key sequence
					// Check for arrow key sequences
					int next1 = System.in.read();
					if ( next1 == '[' ) {
						int next2 = System.in.read();
						switch ( next2 ) {
							case 'A' : // Up arrow
								String prevCommand = getPreviousHistoryCommand();
								if ( prevCommand != null ) {
									lineBuffer.setLength( 0 );
									lineBuffer.append( prevCommand );
									redrawLine( lineBuffer.toString() );
								}
								break;

							case 'B' : // Down arrow
								String nextCommand = getNextHistoryCommand();
								if ( nextCommand != null ) {
									lineBuffer.setLength( 0 );
									lineBuffer.append( nextCommand );
									redrawLine( lineBuffer.toString() );
								}
								break;

							case 'C' : // Right arrow
							case 'D' : // Left arrow
								// Ignore for now - could implement cursor movement later
								break;
						}
					}
					break;

				case 3 : // Ctrl+C
					System.out.println();
					System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
					return null;

				case 4 : // Ctrl+D (EOF)
					if ( lineBuffer.length() == 0 ) {
						return null; // EOF on empty line
					}
					break;

				default :
					// Regular character - add to buffer
					if ( ch >= 32 && ch <= 126 ) { // Printable ASCII
						lineBuffer.append( ( char ) ch );
						redrawLine( lineBuffer.toString() );
					}
					break;
			}
		}

		return null; // EOF reached
	}

	/**
	 * Get the previous command in history.
	 *
	 * @return The previous command, or null if at the beginning
	 */
	private String getPreviousHistoryCommand() {
		if ( commandHistory.isEmpty() ) {
			return null;
		}

		if ( historyIndex == -1 ) {
			historyIndex = commandHistory.size() - 1;
		} else if ( historyIndex > 0 ) {
			historyIndex--;
		}

		return historyIndex >= 0 && historyIndex < commandHistory.size()
		    ? commandHistory.get( historyIndex )
		    : null;
	}

	/**
	 * Get the next command in history.
	 *
	 * @return The next command, or null if at the end
	 */
	private String getNextHistoryCommand() {
		if ( commandHistory.isEmpty() || historyIndex == -1 ) {
			return null;
		}

		if ( historyIndex < commandHistory.size() - 1 ) {
			historyIndex++;
			return commandHistory.get( historyIndex );
		} else {
			// At the end of history, return to current (empty) line
			historyIndex = -1;
			return "";
		}
	}
}