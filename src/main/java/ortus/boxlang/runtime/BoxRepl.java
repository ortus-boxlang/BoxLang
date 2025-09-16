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
	 * Maximum number of commands to keep in history
	 */
	private static final int	MAX_HISTORY_SIZE	= 100;

	/**
	 * Current position in command history (-1 means not navigating history)
	 */
	private int					historyIndex		= -1;

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

	public static void main( String[] args ) {
		BoxRuntime	runtime	= BoxRuntime.getInstance( true );
		BoxRepl		repl	= new BoxRepl( runtime );
		repl.start();
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

			// Tiny editor + 256-color prompt
			String prompt = TinyLineEditor.fg256( 39 ) + "📦 BoxLang> " + TinyLineEditor.reset();
			try ( TinyLineEditor editor = new TinyLineEditor( commandHistory ) ) {
				while ( ( source = editor.readLine( prompt ) ) != null ) {

					// History shorthands still supported
					if ( source.equals( "!!" ) ) {
						source = getLastCommand();
						if ( source != null ) {
							System.out.println( "Executing: " + source );
						} else {
							System.out.println( "No previous command found." );
							continue;
						}
					} else if ( source.startsWith( "!" ) && source.length() > 1 ) {
						try {
							int		historyNum	= Integer.parseInt( source.substring( 1 ) );
							String	h			= getHistoryCommand( historyNum );
							if ( h != null ) {
								source = h;
								System.out.println( "Executing: " + source );
							} else {
								System.out.println( "History command not found." );
								continue;
							}
						} catch ( NumberFormatException ignore ) {
						}
					} else if ( source.equals( "history" ) ) {
						showHistory();
						continue;
					}

					// Exit words still honored
					if ( isExitCommand( source ) ) {
						System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
						break;
					}

					// Add to history & execute
					if ( source != null && !source.trim().isEmpty() ) {
						addToHistory( source );
					}
					executeReplLine( source, scriptingContext );
				}
			}

			// (Prompt is printed inside editor; nothing needed here)
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
		System.out.println( " ↕️ UP/DOWN arrows may work for history (terminal dependent)" );
		System.out.println( " 📚 Type 'history' to see command history" );
		System.out.println( " 🔄 Type '!!' to repeat last command, or '!n' to repeat command n" );
		System.out.println( " 🧹 Press Ctrl+D to clear current line, or on empty line to exit" );
		System.out.println( "🚪 Type 'exit' or 'quit' to leave, or press Ctrl-C" );
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
			System.out.println( "  ➡️  " + stringAttempt.get() );
		} else {
			// Handle Java arrays by converting to BoxLang Array
			if ( result.getClass().isArray() ) {
				result = Array.fromArray( ( Object[] ) result );
			}
			// Display the object's toString representation
			System.out.println( "  ➡️  " + result );
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
	 * Get the last command from history.
	 *
	 * @return The last command, or null if no history
	 */
	private String getLastCommand() {
		if ( commandHistory.isEmpty() ) {
			return null;
		}
		return commandHistory.get( commandHistory.size() - 1 );
	}

	/**
	 * Get a specific command from history by number (1-based).
	 *
	 * @param historyNum The history number (1-based)
	 *
	 * @return The command, or null if not found
	 */
	private String getHistoryCommand( int historyNum ) {
		if ( historyNum < 1 || historyNum > commandHistory.size() ) {
			return null;
		}
		return commandHistory.get( historyNum - 1 );
	}

	/**
	 * Display the command history.
	 */
	private void showHistory() {
		if ( commandHistory.isEmpty() ) {
			System.out.println( "No command history available." );
			return;
		}

		System.out.println( "📚 Command History:" );
		for ( int i = 0; i < commandHistory.size(); i++ ) {
			System.out.printf( "%3d: %s%n", i + 1, commandHistory.get( i ) );
		}
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
	 * Read a line with arrow key support using character-by-character reading.
	 * Note: Arrow key detection may not work in all terminal environments due to
	 * Java's buffered input limitations. For full arrow key support, a native
	 * terminal library like JLine would be needed.
	 *
	 * @param reader The BufferedReader to read from
	 *
	 * @return The input line, or null if EOF is reached
	 *
	 * @throws IOException If an I/O error occurs
	 */
	private String readInputLine( BufferedReader reader ) throws IOException {
		StringBuilder	lineBuffer	= new StringBuilder();
		int				ch;

		while ( ( ch = reader.read() ) != -1 ) {
			switch ( ch ) {
				case '\n' :
				case '\r' :
					// Enter pressed - return the current line
					System.out.println(); // Move to next line
					String result = lineBuffer.toString();
					historyIndex = -1; // Reset history navigation
					return result;

				case 127 : // Backspace (DEL)
				case 8 :   // Backspace (BS)
					if ( lineBuffer.length() > 0 ) {
						lineBuffer.deleteCharAt( lineBuffer.length() - 1 );
						redrawLine( lineBuffer.toString() );
					}
					break;

				case 27 : // ESC - potential arrow key sequence
					// Try to read arrow key sequences, but this may not work reliably
					// in all terminal environments due to Java's input buffering
					if ( reader.ready() ) {
						int next1 = reader.read();
						if ( next1 == '[' && reader.ready() ) {
							int next2 = reader.read();
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
					}
					break;

				case 3 : // Ctrl+C
					System.out.println();
					System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
					return null;

				case 4 : // Ctrl+D (EOF)
					if ( lineBuffer.length() == 0 ) {
						// Ctrl+D on empty line - exit REPL
						System.out.println();
						System.out.println( "👋 Thanks for using BoxLang REPL! Happy coding! 🎉" );
						return null;
					} else {
						// Ctrl+D with content - clear the current line
						lineBuffer.setLength( 0 );
						redrawLine( "" );
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

	// --- Tiny, zero-dep cross-platform raw editor ------------------------------
	private static final class TinyLineEditor implements AutoCloseable {

		// 256-color helpers
		static String fg256( int idx ) {
			return "\033[38;5;" + idx + "m";
		}

		static String bg256( int idx ) {
			return "\033[48;5;" + idx + "m";
		}

		static String reset() {
			return "\033[0m";
		}

		static final String				CLR_LINE	= "\r\033[2K";

		// OS detection
		private static final boolean	WINDOWS		= System.getProperty( "os.name" ).toLowerCase().contains( "win" );

		// POSIX raw mode support (macOS/Linux). Uses `stty`, no libs.
		private static final class PosixRaw implements AutoCloseable {

			private final String orig;

			PosixRaw() {
				this.orig = execRead( "stty -g" );
				exec( "stty -icanon -echo min 1 time 0" ); // raw-ish
			}

			@Override
			public void close() {
				exec( "stty " + orig );
			}

			private static String execRead( String cmd ) {
				try {
					Process	p	= new ProcessBuilder( "sh", "-c", cmd ).redirectErrorStream( true ).start();
					byte[]	out	= p.getInputStream().readAllBytes();
					p.waitFor();
					return new String( out ).trim();
				} catch ( Exception e ) {
					throw new RuntimeException( "stty failed: " + cmd, e );
				}
			}

			private static void exec( String cmd ) {
				try {
					Process p = new ProcessBuilder( "sh", "-c", cmd ).redirectErrorStream( true ).start();
					p.waitFor();
				} catch ( Exception e ) {
					throw new RuntimeException( "stty failed: " + cmd, e );
				}
			}
		}

		// Windows key feeder via PowerShell (no JNI/JNA). Reads one key at a time.
		// Emits tokens like: UP,DOWN,ENTER,BACKSPACE,CTRL_C,CTRL_D,CHAR:<int>
		private static final class WinKeys implements AutoCloseable {

			private final Process			proc;
			private final BufferedReader	out;

			WinKeys() {
				try {
					String ps = String.join( " ",
					    "-NoProfile", "-Command",
					    "& {",
					    "while($true){",
					    "$k=[Console]::ReadKey($true);",
					    "if(($k.Modifiers -band [ConsoleModifiers]::Control) -and ($k.Key -eq 'C')){[Console]::Out.WriteLine('CTRL_C');break}",
					    "if(($k.Modifiers -band [ConsoleModifiers]::Control) -and ([int]$k.KeyChar -eq 4)){[Console]::Out.WriteLine('CTRL_D');continue}",
					    "switch($k.Key){",
					    "  'Enter'     { [Console]::Out.WriteLine('ENTER'); continue }",
					    "  'Backspace' { [Console]::Out.WriteLine('BACKSPACE'); continue }",
					    "  'UpArrow'   { [Console]::Out.WriteLine('UP'); continue }",
					    "  'DownArrow' { [Console]::Out.WriteLine('DOWN'); continue }",
					    "}",
					    "if([int]$k.KeyChar -gt 0){ [Console]::Out.WriteLine('CHAR:' + ([int]$k.KeyChar)) }",
					    "}",
					    "}"
					);
					this.proc	= new ProcessBuilder( "powershell", ps ).redirectErrorStream( true ).start();
					this.out	= new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
				} catch ( Exception e ) {
					throw new RuntimeException( "Failed to start PowerShell key feeder. Ensure PowerShell is available.", e );
				}
			}

			String nextToken() throws IOException {
				return out.readLine();
			}

			@Override
			public void close() {
				try {
					proc.destroyForcibly();
				} catch ( Exception ignored ) {
				}
			}
		}

		private final List<String>	history;
		private int					hIdx	= -1;

		TinyLineEditor( List<String> history ) {
			this.history = history;
		}

		/**
		 * Reads a single edited line in raw mode.
		 * Returns null on Ctrl+C or Ctrl+D on empty buffer (exit).
		 */
		String readLine( String prompt ) {
			if ( WINDOWS ) {
				try ( WinKeys keys = new WinKeys() ) {
					System.out.print( prompt );
					StringBuilder buf = new StringBuilder();
					for ( ;; ) {
						String t = keys.nextToken();
						if ( t == null )
							return null;
						switch ( t ) {
							case "ENTER" -> {
								System.out.print( "\r\n" );
								hIdx = -1;
								return buf.toString();
							}
							case "BACKSPACE" -> {
								if ( buf.length() > 0 ) {
									buf.deleteCharAt( buf.length() - 1 );
									redraw( prompt, buf );
								}
							}
							case "UP" -> {
								String prev = prevHist();
								if ( prev != null ) {
									buf.setLength( 0 );
									buf.append( prev );
									redraw( prompt, buf );
								}
							}
							case "DOWN" -> {
								String next = nextHist();
								if ( next != null ) {
									buf.setLength( 0 );
									buf.append( next );
									redraw( prompt, buf );
								}
							}
							case "CTRL_C" -> {
								System.out.println();
								return null;
							}
							case "CTRL_D" -> {
								if ( buf.length() == 0 ) {
									System.out.println();
									return null;
								}
								buf.setLength( 0 );
								redraw( prompt, buf ); // clear buffer
							}
							default -> {
								if ( t.startsWith( "CHAR:" ) ) {
									int code = Integer.parseInt( t.substring( 5 ) );
									if ( code >= 32 && code < 127 ) {
										buf.append( ( char ) code );
										redraw( prompt, buf );
									}
								}
							}
						}
					}
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
			} else {
				try ( PosixRaw raw = new PosixRaw() ) {
					System.out.print( prompt );
					System.out.flush();
					StringBuilder buf = new StringBuilder();
					for ( ;; ) {
						int b = System.in.read();
						if ( b == -1 )
							return null;

						// ENTER
						if ( b == '\r' || b == '\n' ) {
							System.out.print( "\r\n" );
							hIdx = -1;
							return buf.toString();
						}

						// Ctrl+C
						if ( b == 3 ) {
							System.out.println();
							return null;
						}

						// Ctrl+D
						if ( b == 4 ) {
							if ( buf.length() == 0 ) {
								System.out.println();
								return null;
							}
							buf.setLength( 0 );
							redraw( prompt, buf );
							continue;
						}

						// Backspace (DEL or BS)
						if ( b == 127 || b == 8 ) {
							if ( buf.length() > 0 ) {
								buf.deleteCharAt( buf.length() - 1 );
								redraw( prompt, buf );
							}
							continue;
						}

						// Escape sequences (arrows)
						if ( b == 27 ) { // ESC
							System.in.read(); // '[' or other
							int c = System.in.read();
							if ( c == 'A' ) { // Up
								String prev = prevHist();
								if ( prev != null ) {
									buf.setLength( 0 );
									buf.append( prev );
									redraw( prompt, buf );
								}
							} else if ( c == 'B' ) { // Down
								String next = nextHist();
								if ( next != null ) {
									buf.setLength( 0 );
									buf.append( next );
									redraw( prompt, buf );
								}
							} else {
								// ignore others
							}
							continue;
						}

						// Printable ASCII
						if ( b >= 32 && b < 127 ) {
							buf.append( ( char ) b );
							redraw( prompt, buf );
						}
					}
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
			}
		}

		private void redraw( String prompt, StringBuilder buf ) {
			System.out.print( CLR_LINE );
			System.out.print( prompt );
			System.out.print( buf );
			System.out.flush();
		}

		private String prevHist() {
			if ( history.isEmpty() )
				return null;
			if ( hIdx == -1 )
				hIdx = history.size() - 1;
			else if ( hIdx > 0 )
				hIdx--;
			return ( hIdx >= 0 && hIdx < history.size() ) ? history.get( hIdx ) : null;
		}

		private String nextHist() {
			if ( history.isEmpty() || hIdx == -1 )
				return null;
			if ( hIdx < history.size() - 1 ) {
				hIdx++;
				return history.get( hIdx );
			}
			hIdx = -1;
			return "";
		}

		@Override
		public void close() {
			/* Raw restored by sub-handlers; PowerShell closed. */ }
	}
	// --- end tiny editor --------------------------------------------------------

}