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
import java.io.Console;
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
	private final List<String>	commandHistory	= new ArrayList<>();

	/**
	 * Current position in command history (-1 means not navigating history)
	 */
	private int					historyIndex	= -1;

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
		System.out.println( "  ██████   ██████  ██   ██ ██       █████  ███    ██  ██████ " );
		System.out.println( "   ██   ██ ██    ██  ██ ██  ██      ██   ██ ████   ██ ██      " );
		System.out.println( "   ██████  ██    ██   ███   ██      ███████ ██ ██  ██ ██   ███" );
		System.out.println( "   ██   ██ ██    ██  ██ ██  ██      ██   ██ ██  ██ ██ ██    ██" );
		System.out.println( "   ██████   ██████  ██   ██ ███████ ██   ██ ██   ████  ██████ " );
		System.out.println( "" );
		System.out.println( "✨ Welcome to the BoxLang Interactive REPL!" );
		System.out.println( "💡 Enter an expression, then hit enter" );
		System.out.println( " ↕️ UP/DOWN arrows may work for history (terminal dependent)" );
		System.out.println( " 📚 Type 'history' to see command history" );
		System.out.println( " 🔄 Type '!!' to repeat last command, or '!n' to repeat command n" );
		System.out.println( " 🧹 Press Ctrl+D to clear current line, or on empty line to exit" );
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
			System.out.println( "∅ No command history available." );
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

		// Reset history navigation
		this.historyIndex = -1;
	}

	/**
	 * Tiny, zero-dep cross-platform raw editor
	 */
	private final class TinyLineEditor implements AutoCloseable {

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

			private final String	orig;
			private final Console	console;
			private final boolean	usingConsole;
			private Process			ddProcess;
			private InputStream		rawInput;

			PosixRaw() {
				try {
					// Save original terminal settings
					this.orig			= execRead( "stty -g < /dev/tty" );

					// Try to use Console for better terminal access
					this.console		= System.console();
					this.usingConsole	= ( console != null );

					if ( !usingConsole ) {
						// Enable raw mode: no canonical input, no echo, immediate character availability
						exec( "stty -icanon -echo -isig min 1 time 0 < /dev/tty" );

						// Start dd process to read from /dev/tty in raw mode
						// This bypasses Java's System.in buffering
						this.ddProcess	= new ProcessBuilder( "dd", "if=/dev/tty", "bs=1", "count=1" )
						    .redirectError( ProcessBuilder.Redirect.DISCARD )
						    .start();
						this.rawInput	= ddProcess.getInputStream();
					} else {
						// Still set terminal to raw mode even when using Console
						exec( "stty -icanon -echo -isig min 1 time 0 < /dev/tty" );
					}

				} catch ( Exception e ) {
					throw new RuntimeException( "Failed to enable raw terminal mode. Ensure 'stty' and 'dd' are available.", e );
				}
			}

			/**
			 * Read a single byte from the terminal in raw mode
			 */
			int readByte() throws IOException {
				if ( usingConsole ) {
					// Try using Console.readPassword() with empty prompt for single character
					char[] chars = console.readPassword( "" );
					if ( chars != null && chars.length > 0 ) {
						return chars[ 0 ];
					}
					return -1;
				} else {
					// Start a new dd process for each byte read
					// This is necessary because dd exits after reading count=1
					try {
						Process	p	= new ProcessBuilder( "dd", "if=/dev/tty", "bs=1", "count=1" )
						    .redirectError( ProcessBuilder.Redirect.DISCARD )
						    .start();

						int		b	= p.getInputStream().read();
						p.waitFor();
						return b;
					} catch ( InterruptedException e ) {
						Thread.currentThread().interrupt();
						throw new IOException( "Interrupted while reading", e );
					}
				}
			}

			@Override
			public void close() {
				try {
					if ( ddProcess != null && ddProcess.isAlive() ) {
						ddProcess.destroyForcibly();
					}
					exec( "stty " + orig + " < /dev/tty" );
				} catch ( Exception e ) {
					// Best effort to restore - don't throw in close()
					System.err.println( "Warning: Failed to restore terminal settings: " + e.getMessage() );
				}
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
				return readLineWindows( prompt );
			} else {
				return readLinePosix( prompt );
			}
		}

		private String readLineWindows( String prompt ) {
			try ( WinKeys keys = new WinKeys() ) {
				System.out.print( prompt );
				System.out.flush();
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
				throw new RuntimeException( "Windows key input failed", e );
			}
		}

		private String readLinePosix( String prompt ) {
			// Try using Java Console first if available
			Console console = System.console();
			if ( console != null ) {
				try {
					return readConsoleRaw( prompt, console );
				} catch ( Exception e ) {
					System.err.println( "Console raw mode failed: " + e.getMessage() );
				}
			}

			// Fall back to stty approach
			try ( PosixRaw raw = new PosixRaw() ) {
				return readRawPosix( prompt );
			} catch ( Exception e ) {
				System.err.println( "Warning: Raw terminal mode failed, falling back to line mode. " + e.getMessage() );
				return readLinePosixFallback( prompt );
			}
		}

		private String readConsoleRaw( String prompt, Console console ) throws IOException {
			System.out.print( prompt );
			System.out.flush();
			StringBuilder buf = new StringBuilder();

			// Try to use console for better terminal integration
			for ( ;; ) {
				// Unfortunately, Java Console doesn't provide character-by-character input either
				// We need to fall back to our stty approach
				String line = console.readLine();
				if ( line == null ) {
					return null;
				}
				return line;
			}
		}

		private String readRawPosix( String prompt ) throws IOException {
			System.out.print( prompt );
			System.out.flush();
			StringBuilder buf = new StringBuilder();

			// We need to access the PosixRaw instance to use its readByte method
			// Create a new instance since we're inside the try-with-resources
			try ( PosixRaw raw = new PosixRaw() ) {
				for ( ;; ) {
					int b = raw.readByte();
					if ( b == -1 ) {
						return null; // EOF
					}

					// ENTER (CR or LF)
					if ( b == '\r' || b == '\n' ) {
						System.out.print( "\r\n" );
						System.out.flush();
						hIdx = -1;
						return buf.toString();
					}

					// Backspace (DEL or BS)
					if ( b == 127 || b == 8 ) {
						if ( buf.length() > 0 ) {
							buf.deleteCharAt( buf.length() - 1 );
							redraw( prompt, buf );
						}
						continue;
					}

					// Escape sequences (arrows, etc.)
					if ( b == 27 ) { // ESC
						// Read the next character to see if it's a CSI sequence
						int next = raw.readByte();
						if ( next == '[' ) {
							// This is a CSI sequence, read the final character
							int c = raw.readByte();
							switch ( c ) {
								case 'A' -> { // Up arrow
									String prev = prevHist();
									if ( prev != null ) {
										buf.setLength( 0 );
										buf.append( prev );
										redraw( prompt, buf );
									}
								}
								case 'B' -> { // Down arrow
									String next_hist = nextHist();
									if ( next_hist != null ) {
										buf.setLength( 0 );
										buf.append( next_hist );
										redraw( prompt, buf );
									}
								}
								case 'C' -> { // Right arrow - ignore for now
								}
								case 'D' -> { // Left arrow - ignore for now
								}
							}
						} else {
							// Not a CSI sequence, treat as regular character
							buf.append( ( char ) b );
							if ( next != -1 ) {
								buf.append( ( char ) next );
							}
							redraw( prompt, buf );
						}
						continue;
					}

					// Control+D (EOF on empty line = exit)
					if ( b == 4 && buf.length() == 0 ) {
						return null;
					}

					// Control+D (clear line if not empty)
					if ( b == 4 && buf.length() > 0 ) {
						buf.setLength( 0 );
						redraw( prompt, buf );
						continue;
					}

					// Control+C
					if ( b == 3 ) {
						System.out.print( "^C\r\n" );
						System.out.flush();
						hIdx = -1;
						return ""; // Empty line
					}

					// Regular printable character
					if ( b >= 32 && b < 127 ) {
						buf.append( ( char ) b );
						redraw( prompt, buf );
					}
				}
			}
		}

		private String readLinePosixFallback( String prompt ) {
			// Fallback to BufferedReader for systems where stty doesn't work
			try ( BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) ) ) {
				System.out.print( prompt );
				System.out.flush();
				return reader.readLine();
			} catch ( IOException e ) {
				throw new RuntimeException( "Failed to read input", e );
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