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
package ortus.boxlang.runtime.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * MiniConsole - A lightweight, cross-platform command-line input handler with history support.
 *
 * This class provides a zero-dependency solution for interactive command-line applications
 * that need features like:
 * - Arrow key navigation through command history
 * - Raw terminal input (character-by-character)
 * - Cross-platform support (Windows via PowerShell, POSIX via stty)
 * - Configurable prompts with color support
 * - Command history management
 * - Terminal control sequences (Ctrl+C, Ctrl+D, etc.)
 *
 * The console automatically detects the operating system and uses the appropriate
 * method for raw input:
 * - Windows: PowerShell-based key reading
 * - POSIX (macOS/Linux): stty + dd for direct /dev/tty access
 *
 * Example usage:
 *
 * <pre>
 * try ( MiniConsole console = new MiniConsole() ) {
 *     console.setPrompt( "MyApp> " );
 *
 *     String input;
 *     while ( ( input = console.readLine() ) != null ) {
 *         if ( "exit".equals( input ) )
 *             break;
 *
 *         // Process the input
 *         System.out.println( "You entered: " + input );
 *     }
 * }
 * </pre>
 *
 * Features:
 * - UP/DOWN arrows navigate command history
 * - Ctrl+C exits the current input
 * - Ctrl+D clears current line, or exits on empty line
 * - Backspace/Delete for editing
 * - Automatic history management (no duplicates)
 * - 256-color terminal support for prompts
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.6.0
 */
public class MiniConsole implements AutoCloseable {

	/**
	 * Default prompt text
	 */
	private static final String		DEFAULT_PROMPT	= "> ";

	/**
	 * ANSI sequence to clear current line
	 */
	private static final String		CLR_LINE		= "\r\033[2K";

	/**
	 * Operating system detection
	 */
	private static final boolean	WINDOWS			= System.getProperty( "os.name" ).toLowerCase().contains( "win" );

	/**
	 * Current prompt string
	 */
	private String					prompt			= DEFAULT_PROMPT;

	/**
	 * Command history storage (using ArrayList for performance)
	 */
	private final List<String>		history			= new ArrayList<>();

	/**
	 * Current position in command history (-1 means not navigating history)
	 */
	private int						historyIndex	= -1;

	/**
	 * Maximum number of history entries to retain
	 */
	private int						maxHistorySize	= 1000;

	/**
	 * Reusable StringBuilder for input buffering (performance optimization)
	 */
	private final StringBuilder		inputBuffer		= new StringBuilder( 256 );

	/**
	 * Constructor with default settings
	 */
	public MiniConsole() {
		// Default configuration
	}

	/**
	 * Constructor with custom prompt
	 *
	 * @param prompt The prompt string to display
	 */
	public MiniConsole( String prompt ) {
		this.prompt = prompt != null ? prompt : DEFAULT_PROMPT;
	}

	/**
	 * Set the prompt string displayed before user input
	 *
	 * @param prompt The new prompt string (null defaults to "> ")
	 */
	public MiniConsole setPrompt( String prompt ) {
		this.prompt = prompt != null ? prompt : DEFAULT_PROMPT;
		return this;
	}

	/**
	 * Get the current prompt string
	 *
	 * @return The current prompt string
	 */
	public String getPrompt() {
		return prompt;
	}

	/**
	 * Set the maximum number of history entries to retain
	 *
	 * @param maxSize Maximum history size (default: 1000)
	 */
	public MiniConsole setMaxHistorySize( int maxSize ) {
		this.maxHistorySize = Math.max( 0, maxSize );
		trimHistoryToSize();
		return this;
	}

	/**
	 * Get the maximum history size
	 *
	 * @return Maximum number of history entries
	 */
	public int getMaxHistorySize() {
		return maxHistorySize;
	}

	/**
	 * Get a copy of the command history
	 *
	 * @return List of command history entries
	 */
	public List<String> getHistory() {
		return new ArrayList<>( history );
	}

	/**
	 * Add a command to the history
	 *
	 * @param command The command to add (null/empty commands are ignored)
	 */
	public void addToHistory( String command ) {
		if ( command == null || command.trim().isEmpty() ) {
			return;
		}

		// Don't add duplicate consecutive commands
		if ( !history.isEmpty() && history.get( history.size() - 1 ).equals( command ) ) {
			return;
		}

		history.add( command );
		trimHistoryToSize();

		// Reset history navigation
		this.historyIndex = -1;
	}

	/**
	 * Clear the command history
	 */
	public void clearHistory() {
		history.clear();
		historyIndex = -1;
	}

	/**
	 * Display the command history to System.out
	 */
	public void showHistory() {
		if ( history.isEmpty() ) {
			System.out.println( "No command history available." );
			return;
		}

		System.out.println( "Command History:" );
		for ( int i = 0; i < history.size(); i++ ) {
			System.out.printf( "%3d: %s%n", i + 1, history.get( i ) );
		}
	}

	/**
	 * Get the last command from history
	 *
	 * @return The last command, or null if no history
	 */
	public String getLastCommand() {
		if ( history.isEmpty() ) {
			return null;
		}
		return history.get( history.size() - 1 );
	}

	/**
	 * Get a specific command from history by number (1-based)
	 *
	 * @param historyNum The history number (1-based)
	 *
	 * @return The command, or null if not found
	 */
	public String getHistoryCommand( int historyNum ) {
		if ( historyNum < 1 || historyNum > history.size() ) {
			return null;
		}
		return history.get( historyNum - 1 );
	}

	/**
	 * Read a line of input with full editing capabilities
	 *
	 * @return The input line, or null on EOF/exit
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public String readLine() throws IOException {
		if ( WINDOWS ) {
			return readLineWindows();
		} else {
			return readLinePosix();
		}
	}

	/**
	 * Read a line with a custom prompt (doesn't change the default prompt)
	 *
	 * @param customPrompt Temporary prompt for this read operation
	 *
	 * @return The input line, or null on EOF/exit
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public String readLine( String customPrompt ) throws IOException {
		String oldPrompt = this.prompt;
		try {
			this.prompt = customPrompt != null ? customPrompt : DEFAULT_PROMPT;
			return readLine();
		} finally {
			this.prompt = oldPrompt;
		}
	}

	/**
	 * Create a 256-color foreground ANSI sequence
	 *
	 * @param colorIndex Color index (0-255)
	 *
	 * @return ANSI escape sequence for foreground color
	 */
	public static String fg256( int colorIndex ) {
		return "\033[38;5;" + colorIndex + "m";
	}

	/**
	 * Create a 256-color background ANSI sequence
	 *
	 * @param colorIndex Color index (0-255)
	 *
	 * @return ANSI escape sequence for background color
	 */
	public static String bg256( int colorIndex ) {
		return "\033[48;5;" + colorIndex + "m";
	}

	/**
	 * ANSI reset sequence to clear all formatting
	 *
	 * @return ANSI reset sequence
	 */
	public static String reset() {
		return "\033[0m";
	}

	@Override
	public void close() {
		// Nothing to clean up at this level - handled by inner classes
	}

	// ================================================================================
	// PRIVATE IMPLEMENTATION
	// ================================================================================

	/**
	 * Windows-specific input handling using PowerShell
	 */
	private String readLineWindows() throws IOException {
		try ( WinKeys keys = new WinKeys() ) {
			System.out.print( prompt );
			System.out.flush();

			// Reuse buffer for performance
			inputBuffer.setLength( 0 );

			for ( ;; ) {
				String token = keys.nextToken();
				if ( token == null )
					return null;

				switch ( token ) {
					case "ENTER" -> {
						System.out.print( "\r\n" );
						historyIndex = -1;
						String result = inputBuffer.toString();
						addToHistory( result );
						return result;
					}
					case "BACKSPACE" -> {
						if ( inputBuffer.length() > 0 ) {
							inputBuffer.deleteCharAt( inputBuffer.length() - 1 );
							redraw( prompt, inputBuffer );
						}
					}
					case "UP" -> {
						String prev = navigateHistoryPrevious();
						if ( prev != null ) {
							inputBuffer.setLength( 0 );
							inputBuffer.append( prev );
							redraw( prompt, inputBuffer );
						}
					}
					case "DOWN" -> {
						String next = navigateHistoryNext();
						if ( next != null ) {
							inputBuffer.setLength( 0 );
							inputBuffer.append( next );
							redraw( prompt, inputBuffer );
						}
					}
					case "CTRL_C" -> {
						System.out.println();
						return null;
					}
					case "CTRL_D" -> {
						if ( inputBuffer.length() == 0 ) {
							System.out.println();
							return null;
						}
						inputBuffer.setLength( 0 );
						redraw( prompt, inputBuffer );
					}
					default -> {
						if ( token.startsWith( "CHAR:" ) ) {
							int code = Integer.parseInt( token.substring( 5 ) );
							if ( code >= 32 && code < 127 ) {
								inputBuffer.append( ( char ) code );
								redraw( prompt, inputBuffer );
							}
						}
					}
				}
			}
		}
	}

	/**
	 * POSIX-specific input handling using stty and dd
	 */
	private String readLinePosix() throws IOException {
		try ( PosixRaw raw = new PosixRaw() ) {
			return readRawPosix( raw );
		} catch ( Exception e ) {
			System.err.println( "Warning: Raw terminal mode failed, falling back to line mode. " + e.getMessage() );
			return readLinePosixFallback();
		}
	}

	/**
	 * Raw POSIX input processing
	 */
	private String readRawPosix( PosixRaw raw ) throws IOException {
		System.out.print( prompt );
		System.out.flush();

		// Reuse buffer for performance
		inputBuffer.setLength( 0 );

		for ( ;; ) {
			int b = raw.readByte();
			if ( b == -1 ) {
				return null; // EOF
			}

			// ENTER (CR or LF)
			if ( b == '\r' || b == '\n' ) {
				System.out.print( "\r\n" );
				System.out.flush();
				historyIndex = -1;
				String result = inputBuffer.toString();
				addToHistory( result );
				return result;
			}

			// Backspace (DEL or BS)
			if ( b == 127 || b == 8 ) {
				if ( inputBuffer.length() > 0 ) {
					inputBuffer.deleteCharAt( inputBuffer.length() - 1 );
					redraw( prompt, inputBuffer );
				}
				continue;
			}

			// Escape sequences (arrows, etc.)
			if ( b == 27 ) { // ESC
				int next = raw.readByte();

				if ( next == '[' ) {
					// CSI sequence: ESC [ ...
					handleCSISequence( raw, prompt, inputBuffer );
				} else if ( next == 'O' ) {
					// SS3 sequence: ESC O ...
					handleSS3Sequence( raw, prompt, inputBuffer );
				} else if ( next == -1 ) {
					// Just ESC by itself
					continue;
				} else {
					// Unknown escape sequence, ignore
					continue;
				}
				continue;
			}

			// Control+D (EOF on empty line = exit)
			if ( b == 4 && inputBuffer.length() == 0 ) {
				return null;
			}

			// Control+D (clear line if not empty)
			if ( b == 4 && inputBuffer.length() > 0 ) {
				inputBuffer.setLength( 0 );
				redraw( prompt, inputBuffer );
				continue;
			}

			// Control+C
			if ( b == 3 ) {
				System.out.print( "^C\r\n" );
				System.out.flush();
				historyIndex = -1;
				return ""; // Empty line
			}

			// Regular printable character
			if ( b >= 32 && b < 127 ) {
				inputBuffer.append( ( char ) b );
				redraw( prompt, inputBuffer );
			}
		}
	}

	/**
	 * Handle CSI escape sequences (ESC[...)
	 */
	private void handleCSISequence( PosixRaw raw, String prompt, StringBuilder buffer ) throws IOException {
		StringBuilder	sequence	= new StringBuilder();
		int				c;

		while ( ( c = raw.readByte() ) != -1 ) {
			sequence.append( ( char ) c );

			// Final character of CSI sequence (A-Z, a-z)
			if ( ( c >= 'A' && c <= 'Z' ) || ( c >= 'a' && c <= 'z' ) ) {
				break;
			}
		}

		String seq = sequence.toString();

		// Handle arrow keys - ignore any parameters (like 1;5A for Ctrl+Up)
		if ( seq.endsWith( "A" ) ) { // Up arrow (any variant)
			String prev = navigateHistoryPrevious();
			if ( prev != null ) {
				buffer.setLength( 0 );
				buffer.append( prev );
				redraw( prompt, buffer );
			}
		} else if ( seq.endsWith( "B" ) ) { // Down arrow (any variant)
			String next = navigateHistoryNext();
			if ( next != null ) {
				buffer.setLength( 0 );
				buffer.append( next );
				redraw( prompt, buffer );
			}
		}
		// Ignore other CSI sequences (C=right, D=left, etc.)
	}

	/**
	 * Handle SS3 escape sequences (ESC O...)
	 */
	private void handleSS3Sequence( PosixRaw raw, String prompt, StringBuilder buffer ) throws IOException {
		int c = raw.readByte();

		switch ( c ) {
			case 'A' -> { // Up arrow
				String prev = navigateHistoryPrevious();
				if ( prev != null ) {
					buffer.setLength( 0 );
					buffer.append( prev );
					redraw( prompt, buffer );
				}
			}
			case 'B' -> { // Down arrow
				String next = navigateHistoryNext();
				if ( next != null ) {
					buffer.setLength( 0 );
					buffer.append( next );
					redraw( prompt, buffer );
				}
			}
			// Ignore other SS3 sequences (C=right, D=left, etc.)
		}
	}

	/**
	 * Fallback to line-buffered input for systems where raw mode fails
	 */
	private String readLinePosixFallback() throws IOException {
		try ( BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) ) ) {
			System.out.print( prompt );
			System.out.flush();
			String result = reader.readLine();
			addToHistory( result );
			return result;
		}
	}

	/**
	 * Redraw the current line with prompt and buffer content
	 */
	private void redraw( String promptStr, StringBuilder buffer ) {
		System.out.print( CLR_LINE );
		System.out.print( promptStr );
		System.out.print( buffer );
		System.out.flush();
	}

	/**
	 * Navigate to previous command in history
	 */
	private String navigateHistoryPrevious() {
		if ( history.isEmpty() )
			return null;

		if ( historyIndex == -1 ) {
			historyIndex = history.size() - 1;
		} else if ( historyIndex > 0 ) {
			historyIndex--;
		}

		return ( historyIndex >= 0 && historyIndex < history.size() ) ? history.get( historyIndex ) : null;
	}

	/**
	 * Navigate to next command in history
	 */
	private String navigateHistoryNext() {
		if ( history.isEmpty() || historyIndex == -1 )
			return null;

		if ( historyIndex < history.size() - 1 ) {
			historyIndex++;
			return history.get( historyIndex );
		}

		historyIndex = -1;
		return "";
	}

	/**
	 * Trim history to maximum size
	 */
	private void trimHistoryToSize() {
		while ( history.size() > maxHistorySize ) {
			history.remove( 0 );
		}
	}

	// ================================================================================
	// PLATFORM-SPECIFIC IMPLEMENTATIONS
	// ================================================================================

	/**
	 * POSIX raw terminal mode handler using stty
	 */
	private static final class PosixRaw implements AutoCloseable {

		private final String originalSettings;

		PosixRaw() {
			try {
				// Save original terminal settings
				this.originalSettings = executeCommand( "stty -g < /dev/tty" );

				// Enable raw mode: no canonical input, no echo, no signal processing
				// min 1 = return after reading 1 character
				// time 0 = no timeout
				executeCommand( "stty -icanon -echo -isig min 1 time 0 < /dev/tty" );

			} catch ( Exception e ) {
				throw new RuntimeException( "Failed to enable raw terminal mode. Ensure 'stty' is available.", e );
			}
		}

		/**
		 * Read a single byte from the terminal in raw mode
		 */
		int readByte() throws IOException {
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

		@Override
		public void close() {
			try {
				executeCommand( "stty " + originalSettings + " < /dev/tty" );
			} catch ( Exception e ) {
				// Best effort to restore - don't throw in close()
				System.err.println( "Warning: Failed to restore terminal settings: " + e.getMessage() );
			}
		}

		private static String executeCommand( String command ) {
			try {
				Process	p		= new ProcessBuilder( "sh", "-c", command ).redirectErrorStream( true ).start();
				byte[]	output	= p.getInputStream().readAllBytes();
				p.waitFor();
				return new String( output ).trim();
			} catch ( Exception e ) {
				throw new RuntimeException( "Command failed: " + command, e );
			}
		}
	}

	/**
	 * Windows key input handler using PowerShell
	 */
	private static final class WinKeys implements AutoCloseable {

		private final Process			process;
		private final BufferedReader	output;

		WinKeys() {
			try {
				String powerShellScript = String.join( " ",
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

				this.process	= new ProcessBuilder( "powershell", powerShellScript )
				    .redirectErrorStream( true )
				    .start();
				this.output		= new BufferedReader( new InputStreamReader( process.getInputStream() ) );
			} catch ( Exception e ) {
				throw new RuntimeException( "Failed to start PowerShell key feeder. Ensure PowerShell is available.", e );
			}
		}

		String nextToken() throws IOException {
			return output.readLine();
		}

		@Override
		public void close() {
			try {
				process.destroyForcibly();
			} catch ( Exception ignored ) {
				// Best effort cleanup
			}
		}
	}
}