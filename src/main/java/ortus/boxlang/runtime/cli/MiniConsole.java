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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ortus.boxlang.runtime.cli.providers.ITabProvider;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * MiniConsole - A lightweight, cross-platform command-line input handler with history support.
 *
 * This class provides a zero-dependency solution for interactive command-line applications
 * that need features like:
 * - Arrow key navigation through command history
 * - Raw terminal input (character-by-character)
 * - Cross-platform support (Windows, Linux, macOS)
 * - Configurable prompts with color support
 * - Command history management
 * - Terminal control sequences (Ctrl+C, Ctrl+D, etc.)
 *
 * The console uses BoxInputStreamReader for all platforms, providing a unified
 * approach for reading terminal input with proper UTF-8 support and efficient
 * character decoding.
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
 * - TAB cycles forward through completions, SHIFT+TAB cycles backward
 * - Ctrl+C exits the current input
 * - Ctrl+D clears current line, or exits on empty line
 * - Backspace/Delete for editing
 * - Automatic history management (no duplicates)
 * - 256-color terminal support for prompts
 * - UTF-8 and multi-byte character support
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.6.0
 */
public class MiniConsole implements AutoCloseable {

	/**
	 * ----------------------------------------------------------------------------
	 * Constants
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Default prompt
	 */
	private static final String DEFAULT_PROMPT = "> ";

	/**
	 * Global Ansi Codes
	 */
	public enum CODES {

		BACKGROUND( "\033[48;5;" ),
		BLINK( "\033[5m" ),
		BOLD( "\033[1m" ),
		DIM( "\033[2m" ),
		FOREGROUND( "\033[38;5;" ),
		ITALIC( "\033[3m" ),
		RESET( "\033[0m" ),
		REVERSE( "\033[7m" ),
		STRIKETHROUGH( "\033[9m" ),
		UNDERLINE( "\033[4m" ),

	    // Cursor movement
		CURSOR_UP( "\033[A" ),
		CURSOR_DOWN( "\033[B" ),
		CURSOR_RIGHT( "\033[C" ),
		CURSOR_LEFT( "\033[D" ),
		CURSOR_HOME( "\033[H" ),
		CURSOR_SAVE( "\033[s" ),
		CURSOR_RESTORE( "\033[u" ),

	    // Screen clearing
		CLEAR_SCREEN( "\033[2J" ),
		CLEAR_LINE( "\033[2K" ),
		CLEAR_TO_END( "\033[0J" ),
		CLEAR_TO_START( "\033[1J" ),

	    // Standard colors (foreground)
		BLACK( "\033[30m" ),
		RED( "\033[31m" ),
		GREEN( "\033[32m" ),
		YELLOW( "\033[33m" ),
		BLUE( "\033[34m" ),
		MAGENTA( "\033[35m" ),
		CYAN( "\033[36m" ),
		WHITE( "\033[37m" ),

	    // Bright colors (foreground)
		BRIGHT_BLACK( "\033[90m" ),
		BRIGHT_RED( "\033[91m" ),
		BRIGHT_GREEN( "\033[92m" ),
		BRIGHT_YELLOW( "\033[93m" ),
		BRIGHT_BLUE( "\033[94m" ),
		BRIGHT_MAGENTA( "\033[95m" ),
		BRIGHT_CYAN( "\033[96m" ),
		BRIGHT_WHITE( "\033[97m" ),

	    // Background colors
		BG_BLACK( "\033[40m" ),
		BG_RED( "\033[41m" ),
		BG_GREEN( "\033[42m" ),
		BG_YELLOW( "\033[43m" ),
		BG_BLUE( "\033[44m" ),
		BG_MAGENTA( "\033[45m" ),
		BG_CYAN( "\033[46m" ),
		BG_WHITE( "\033[47m" ),

	    // Bright background colors
		BG_BRIGHT_BLACK( "\033[100m" ),
		BG_BRIGHT_RED( "\033[101m" ),
		BG_BRIGHT_GREEN( "\033[102m" ),
		BG_BRIGHT_YELLOW( "\033[103m" ),
		BG_BRIGHT_BLUE( "\033[104m" ),
		BG_BRIGHT_MAGENTA( "\033[105m" ),
		BG_BRIGHT_CYAN( "\033[106m" ),
		BG_BRIGHT_WHITE( "\033[107m" );

		private final String code;

		CODES( String code ) {
			this.code = code;
		}

		public String code() {
			return this.code;
		}

		/**
		 * Find a code by its name (case-insensitive)
		 *
		 * @param name The name of the code (e.g., "red", "bg_bright_white", "reset")
		 *
		 * @return The ANSI code string, or null if not found
		 */
		public static String get( String name ) {
			Objects.requireNonNull( name, "Code name is required" );
			String upperName = name.toUpperCase();

			for ( CODES code : values() ) {
				if ( code.name().equals( upperName ) ) {
					return code.code;
				}
			}

			// Throw a BoxRuntimeException
			throw new BoxRuntimeException( "Invalid ANSI code name: " + name );
		}

		@Override
		public String toString() {
			return this.code;
		}
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Properties
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Current prompt string
	 */
	private String						prompt					= DEFAULT_PROMPT;

	/**
	 * Command history storage (using ArrayList for performance)
	 */
	private final List<String>			history					= new ArrayList<>();

	/**
	 * Current position in command history (-1 means not navigating history)
	 */
	private int							historyIndex			= -1;

	/**
	 * Maximum number of history entries to retain
	 */
	private int							maxHistorySize			= 1000;

	/**
	 * Reusable StringBuilder for input buffering (performance optimization)
	 */
	private final StringBuilder			inputBuffer				= new StringBuilder( 256 );

	/**
	 * Optional syntax highlighter for real-time input coloring
	 */
	private ISyntaxHighlighter			syntaxHighlighter		= null;

	/**
	 * List of registered tab completion providers
	 */
	private final List<ITabProvider>	tabProviders			= new ArrayList<>();

	/**
	 * Current completion state for cycling through suggestions
	 */
	private TabCompletionState			completionState			= null;

	/**
	 * Number of lines used by the current completion display
	 */
	private int							completionDisplayLines	= 0;

	/**
	 * ----------------------------------------------------------------------------
	 * Constructors
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Constructor with default settings
	 */
	public MiniConsole() {
		// Uses default prompt
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
	 * Constructor with custom prompt and syntax highlighter
	 *
	 * @param prompt      The prompt string to display
	 * @param highlighter The syntax highlighter to use, or null to disable
	 */
	public MiniConsole( String prompt, ISyntaxHighlighter highlighter ) {
		this( prompt );
		this.syntaxHighlighter = highlighter;
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Prompt Management
	 * ----------------------------------------------------------------------------
	 */

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
	 * ----------------------------------------------------------------------------
	 * Syntax Highlighting
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Set a syntax highlighter for real-time input coloring
	 *
	 * @param highlighter The syntax highlighter to use, or null to disable
	 *
	 * @return this console instance for method chaining
	 */
	public MiniConsole setSyntaxHighlighter( ISyntaxHighlighter highlighter ) {
		this.syntaxHighlighter = highlighter;
		return this;
	}

	/**
	 * Get the current syntax highlighter
	 *
	 * @return The current syntax highlighter, or null if none set
	 */
	public ISyntaxHighlighter getSyntaxHighlighter() {
		return syntaxHighlighter;
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Tab Completion and Management
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Register a tab completion provider
	 *
	 * @param provider The tab completion provider to register
	 *
	 * @return This MiniConsole instance for method chaining
	 */
	public MiniConsole registerTabProvider( ITabProvider provider ) {
		tabProviders.add( provider );
		// Sort by priority (higher priority first)
		tabProviders.sort( ( a, b ) -> Integer.compare( b.getPriority(), a.getPriority() ) );
		return this;
	}

	/**
	 * ----------------------------------------------------------------------------
	 * History Management
	 * ----------------------------------------------------------------------------
	 */

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
	 * Get the previous command (second-to-last) from history
	 *
	 * @return The previous command, or null if not enough history
	 */
	public String getPreviousCommand() {
		if ( history.size() < 2 ) {
			return null;
		}
		return history.get( history.size() - 2 );
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
	 * ----------------------------------------------------------------------------
	 * Utilities
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Create a 256-color foreground ANSI sequence
	 *
	 * @param colorIndex Color index (0-255)
	 *
	 * @return ANSI escape sequence for foreground color
	 */
	public static String color( int colorIndex ) {
		return CODES.FOREGROUND.code() + colorIndex + "m";
	}

	/**
	 * Create a 256-color background ANSI sequence
	 *
	 * @param colorIndex Color index (0-255)
	 *
	 * @return ANSI escape sequence for background color
	 */
	public static String background( int colorIndex ) {
		return CODES.BACKGROUND.code() + colorIndex + "m";
	}

	/**
	 * ANSI reset sequence to clear all formatting
	 *
	 * @return ANSI reset sequence
	 */
	public static String reset() {
		return CODES.RESET.code();
	}

	/**
	 * Convenient static alias to print an error message in red bold text
	 *
	 * @param text The message text
	 */
	@SuppressWarnings( "static-access" )
	public static void printError( String text ) {
		ColorPrint.bold().red().println( "🔴  " + text );
	}

	/**
	 * Convenient static alias to print a success message in green bold text
	 *
	 * @param text The message text
	 */
	@SuppressWarnings( "static-access" )
	public static void printSuccess( String text ) {
		ColorPrint.green().bold().println( "✅  " + text );
	}

	/**
	 * Convenient static alias to print a warning message in yellow bold text
	 *
	 * @param text The message text
	 */
	@SuppressWarnings( "static-access" )
	public static void printWarning( String text ) {
		ColorPrint.yellow().bold().println( "⚠️  " + text );
	}

	/**
	 * Convenient static alias to print an info message in blue bold text
	 *
	 * @param text The message text
	 */
	@SuppressWarnings( "static-access" )
	public static void printInfo( String text ) {
		ColorPrint.blue().bold().println( "ℹ️  " + text );
	}

	/**
	 * Convenient static alias to print a debug message in magenta bold text
	 *
	 * @param text The message text
	 */
	@SuppressWarnings( "static-access" )
	public static void printDebug( String text ) {
		ColorPrint.magenta().bold().println( "🔍  " + text );
	}

	/**
	 * This function clears the entire console screen and resets the cursor to the top-left corner.
	 */
	public void clear() {
		// ANSI escape sequence to clear the screen and move cursor to top-left
		// Use the CODES enum
		System.out.print( CODES.CLEAR_SCREEN.code() + CODES.CURSOR_HOME.code() );
		System.out.flush();
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Input Reading and Line Editing
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Read a line of input with full editing capabilities
	 *
	 * @return The input line, or null on EOF/exit
	 *
	 * @throws IOException If an I/O error occurs
	 */
	public String readLine() throws IOException {
		try ( BoxInputStreamReader reader = new BoxInputStreamReader( System.in ) ) {
			System.out.print( prompt );
			System.out.flush();

			// Reuse buffer for performance
			inputBuffer.setLength( 0 );

			for ( ;; ) {
				int b = reader.readByte();
				if ( b == -1 ) {
					return null; // EOF
				}

				// ENTER (CR or LF)
				if ( b == '\r' || b == '\n' ) {
					// If we're in completion mode, accept the selected completion and continue editing
					if ( completionState != null ) {
						TabCompletion selected = completionState.getCurrentCompletion();
						if ( selected != null ) {
							// Restore original input first
							inputBuffer.setLength( 0 );
							inputBuffer.append( completionState.getOriginalInput() );
							// Apply the completion
							applyCompletion( prompt, inputBuffer, selected );
						}
						completionState = null;
						// Clear completion list and continue editing - don't execute the line
						clearCompletionDisplay();
						redraw( prompt, inputBuffer );
						continue;
					}
					// Normal ENTER - execute the line
					// Clear the current line to remove any terminal echo before moving to next line
					System.out.print( CODES.CLEAR_LINE.code() );
					System.out.flush();
					historyIndex = -1;
					String result = inputBuffer.toString();
					addToHistory( result );
					return result;
				}

				// Backspace (DEL or BS)
				if ( b == 127 || b == 8 ) {
					if ( completionState != null ) {
						clearCompletionDisplay();
						completionState			= null; // Clear completion state
						completionDisplayLines	= 0;
					}
					if ( inputBuffer.length() > 0 ) {
						inputBuffer.deleteCharAt( inputBuffer.length() - 1 );
						redraw( prompt, inputBuffer );
					}
					continue;
				}

				// Escape sequences (arrows, etc.)
				if ( b == 27 ) { // ESC
					int next = reader.readByte();

					if ( next == '[' ) {
						// CSI sequence: ESC [ ...
						handleCSISequence( reader, prompt, inputBuffer );
					} else if ( next == 'O' ) {
						// SS3 sequence: ESC O ...
						handleSS3Sequence( reader, prompt, inputBuffer );
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

				// Tab completion
				if ( b == 9 ) {
					handleTabCompletion( prompt, inputBuffer );
					continue;
				}

				// Control+C
				if ( b == 3 ) {
					System.out.print( "^C\r\n" );
					System.out.flush();
					historyIndex = -1;
					return null; // Exit signal
				}

				// Control + L (clear screen)
				if ( b == 12 ) {
					clear();
					redraw( prompt, inputBuffer );
					continue;
				}

				// Regular printable character
				if ( b >= 32 && b < 127 ) {
					if ( completionState != null ) {
						clearCompletionDisplay();
						completionState			= null; // Clear completion state
						completionDisplayLines	= 0;
					}
					inputBuffer.append( ( char ) b );
					redraw( prompt, inputBuffer );
				}
			}
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
	 * Close the console and release any resources
	 */
	@Override
	public void close() {
		// Nothing to clean up at this level - handled by inner classes
	}

	/**
	 * Handle CSI escape sequences (ESC[...)
	 */
	private void handleCSISequence( BoxInputStreamReader reader, String prompt, StringBuilder buffer ) throws IOException {
		StringBuilder	sequence	= new StringBuilder();
		int				c;

		while ( ( c = reader.readByte() ) != -1 ) {
			sequence.append( ( char ) c );

			// Final character of CSI sequence (A-Z, a-z)
			if ( ( c >= 'A' && c <= 'Z' ) || ( c >= 'a' && c <= 'z' ) ) {
				break;
			}
		}

		String seq = sequence.toString();

		// Handle SHIFT+TAB (backtab)
		if ( seq.equals( "Z" ) ) {
			handleShiftTabCompletion( prompt, buffer );
			return;
		}

		// Handle arrow keys - ignore any parameters (like 1;5A for Ctrl+Up)
		if ( seq.endsWith( "A" ) ) { // Up arrow (any variant)
			if ( completionState != null ) {
				clearCompletionDisplay();
				completionState			= null; // Clear completion state
				completionDisplayLines	= 0;
			}
			String prev = navigateHistoryPrevious();
			if ( prev != null ) {
				buffer.setLength( 0 );
				buffer.append( prev );
				redraw( prompt, buffer );
			}
		} else if ( seq.endsWith( "B" ) ) { // Down arrow (any variant)
			if ( completionState != null ) {
				clearCompletionDisplay();
				completionState			= null; // Clear completion state
				completionDisplayLines	= 0;
			}
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
	private void handleSS3Sequence( BoxInputStreamReader reader, String prompt, StringBuilder buffer ) throws IOException {
		int c = reader.readByte();

		switch ( c ) {
			case 'A' -> { // Up arrow
				if ( completionState != null ) {
					clearCompletionDisplay();
					completionState			= null;
					completionDisplayLines	= 0;
				}
				String prev = navigateHistoryPrevious();
				if ( prev != null ) {
					buffer.setLength( 0 );
					buffer.append( prev );
					redraw( prompt, buffer );
				}
			}
			case 'B' -> { // Down arrow
				if ( completionState != null ) {
					clearCompletionDisplay();
					completionState			= null;
					completionDisplayLines	= 0;
				}
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
	 * ----------------------------------------------------------------------------
	 * Tab Completion Algorithm
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Handles tab completion by finding matching providers and cycling through suggestions.
	 */
	private void handleTabCompletion( String prompt, StringBuilder buffer ) {
		String	currentInput	= buffer.toString();
		int		cursorPosition	= buffer.length(); // Cursor is always at end in our simple implementation

		// If we're already showing completions and user presses tab again, cycle through them
		if ( completionState != null ) {
			completionState.nextCompletion();
			showCompletionList( prompt, buffer, completionState );
			return;
		}

		// Find completions from registered providers
		List<TabCompletion> allCompletions = new ArrayList<>();

		for ( ITabProvider provider : tabProviders ) {
			if ( provider.canProvideCompletions( currentInput, cursorPosition ) ) {
				// Use first provider that can handle this input (highest priority)
				allCompletions.addAll( provider.getCompletions( currentInput, cursorPosition ) );
				break;
			}
		}

		// Handle completions
		if ( allCompletions.isEmpty() ) {
			// No completions found - clear any existing completion state
			completionState = null;
			return;
		}

		if ( allCompletions.size() == 1 ) {
			// Single completion - apply it directly
			applyCompletion( prompt, buffer, allCompletions.get( 0 ) );
			completionState = null;
		} else {
			// Multiple completions - determine replace bounds and start cycling state
			TabCompletion	firstCompletion	= allCompletions.get( 0 );
			int				replaceEnd		= buffer.length();
			int				replaceStart	= firstCompletion.hasCustomRange() ? firstCompletion.getReplaceStart() : findWordStart( currentInput, replaceEnd );

			completionState = new TabCompletionState( currentInput, allCompletions, cursorPosition, replaceStart, replaceEnd );
			showCompletionList( prompt, buffer, completionState );
		}
	}

	/**
	 * Handles shift+tab completion by going backwards through suggestions.
	 */
	private void handleShiftTabCompletion( String prompt, StringBuilder buffer ) {
		// If we're already showing completions and user presses shift+tab, cycle backwards through them
		if ( completionState != null ) {
			completionState.previousCompletion();
			showCompletionList( prompt, buffer, completionState );
			return;
		}

		// If no completion state exists, start a new completion cycle and immediately go to the last item
		String				currentInput	= buffer.toString();
		int					cursorPosition	= buffer.length(); // Cursor is always at end in our simple implementation

		// Find completions from registered providers
		List<TabCompletion>	allCompletions	= new ArrayList<>();

		for ( ITabProvider provider : tabProviders ) {
			try {
				if ( provider.canProvideCompletions( currentInput, cursorPosition ) ) {
					List<TabCompletion> providerCompletions = provider.getCompletions( currentInput, cursorPosition );
					if ( providerCompletions != null ) {
						allCompletions.addAll( providerCompletions );
					}
					// Use first provider that can handle this input (highest priority)
					break;
				}
			} catch ( Exception e ) {
				// Continue to next provider if this one fails
				System.err.println( "Tab completion error in " + provider.getProviderName() + ": " + e.getMessage() );
			}
		}

		// Handle completions
		if ( allCompletions.isEmpty() ) {
			// No completions found - do nothing
			return;
		}

		if ( allCompletions.size() == 1 ) {
			// Single completion - apply it directly
			applyCompletion( prompt, buffer, allCompletions.get( 0 ) );
		} else {
			// Multiple completions - start at the last item (for backwards navigation feel)
			TabCompletion	firstCompletion	= allCompletions.get( 0 );
			int				replaceEnd		= buffer.length();
			int				replaceStart	= firstCompletion.hasCustomRange() ? firstCompletion.getReplaceStart() : findWordStart( currentInput, replaceEnd );

			completionState = new TabCompletionState( currentInput, allCompletions, cursorPosition, replaceStart, replaceEnd );

			// Move to the last completion to give a "backwards" feel
			for ( int i = 0; i < allCompletions.size() - 1; i++ ) {
				completionState.nextCompletion();
			}

			showCompletionList( prompt, buffer, completionState );
		}
	}

	/**
	 * Applies a completion to the current input buffer.
	 */
	private void applyCompletion( String prompt, StringBuilder buffer, TabCompletion completion ) {
		if ( completion == null ) {
			return;
		}

		// For now, simple implementation: replace from start of current word to end
		String	currentInput	= buffer.toString();
		int		replaceStart	= completion.getReplaceStart();
		int		replaceEnd		= completion.getReplaceEnd();

		if ( !completion.hasCustomRange() ) {
			// Auto-detect word boundaries
			replaceEnd		= buffer.length();
			replaceStart	= findWordStart( currentInput, replaceEnd );
		}

		// Replace the text
		if ( replaceStart >= 0 && replaceEnd >= replaceStart && replaceEnd <= buffer.length() ) {
			buffer.delete( replaceStart, replaceEnd );
			buffer.insert( replaceStart, completion.getText() );
		}

		redraw( prompt, buffer );
	}

	/**
	 * Displays a dropdown-style list of completions below the current cursor position.
	 * Shows a preview of the selected completion in the input line but doesn't apply it to the buffer.
	 *
	 * @param prompt The current prompt
	 * @param buffer The current input buffer
	 * @param state  The completion state containing all completions and selection
	 */
	private void showCompletionList( String prompt, StringBuilder buffer, TabCompletionState state ) {
		List<TabCompletion>	completions		= state.getAllCompletions();
		int					selectedIndex	= state.getCurrentIndex();

		// Clear any previous completion display
		clearCompletionDisplay();

		// Show the completion list below the current line
		System.out.print( "\r\n" );
		int	linesUsed	= 1; // Count the newline we just printed

		// Display up to 10 completions to avoid overwhelming the user
		int	maxDisplay	= Math.min( 10, completions.size() );
		int	startIndex	= Math.max( 0, selectedIndex - 5 ); // Center around selected item
		int	endIndex	= Math.min( completions.size(), startIndex + maxDisplay );

		// Adjust start if we're near the end
		if ( endIndex - startIndex < maxDisplay && startIndex > 0 ) {
			startIndex = Math.max( 0, endIndex - maxDisplay );
		}

		for ( int i = startIndex; i < endIndex; i++ ) {
			TabCompletion	completion	= completions.get( i );
			boolean			isSelected	= ( i == selectedIndex );

			// Highlight the selected item
			if ( isSelected ) {
				System.out.print( color( 44 ) ); // Blue background
				System.out.print( color( 15 ) ); // White text
				System.out.print( "► " );
			} else {
				System.out.print( "  " );
			}

			// Show the completion text with its original formatting
			System.out.print( completion.getDisplayText() );

			// Show description if available
			if ( completion.getDescription() != null && !completion.getDescription().isEmpty() ) {
				if ( isSelected ) {
					// Keep selected item colors for description
					System.out.print( " - " + completion.getDescription() );
				} else {
					System.out.print( color( 8 ) ); // Dim color for description
					System.out.print( " - " + completion.getDescription() );
					System.out.print( reset() ); // Reset after description
				}
			}

			// Always reset at the end of each line to prevent color bleeding
			System.out.print( reset() );
			System.out.print( "\r\n" );
			linesUsed++; // Count each completion line
		}

		// Show pagination info if there are more items
		if ( completions.size() > maxDisplay ) {
			System.out.print( color( 8 ) ); // Dim color
			System.out.print( "  ... (" + ( selectedIndex + 1 ) + "/" + completions.size() + " - TAB/SHIFT+TAB to navigate, ENTER to accept)" );
			System.out.print( reset() );
			System.out.print( "\r\n" );
			linesUsed++; // Count pagination line
		}

		// Store the number of lines used for later clearing
		completionDisplayLines = linesUsed;

		// Show preview of selected completion in the input line
		showCompletionPreview( prompt, buffer, state );
	}

	/**
	 * Shows a preview of the selected completion in the input line without modifying the buffer.
	 */
	private void showCompletionPreview( String prompt, StringBuilder buffer, TabCompletionState state ) {
		TabCompletion selectedCompletion = state.getCurrentCompletion();
		if ( selectedCompletion == null ) {
			redraw( prompt, buffer );
			return;
		}

		// Create a preview by building the line with the completion applied
		String			originalInput	= state.getOriginalInput();
		int				replaceStart	= state.getReplaceStart();
		int				replaceEnd		= state.getReplaceEnd();

		StringBuilder	preview			= new StringBuilder( originalInput );
		if ( replaceStart >= 0 && replaceEnd >= replaceStart && replaceEnd <= preview.length() ) {
			preview.delete( replaceStart, replaceEnd );
			preview.insert( replaceStart, selectedCompletion.getText() );
		}

		// Show the preview
		System.out.print( "\r" );
		System.out.print( CODES.CLEAR_LINE.code() );
		System.out.print( prompt );

		// Apply syntax highlighting if available
		if ( syntaxHighlighter != null ) {
			String highlighted = syntaxHighlighter.highlight( preview.toString() );
			System.out.print( highlighted );
		} else {
			System.out.print( preview );
		}

		System.out.flush();
	}

	/**
	 * Clears the completion display by moving up and clearing the lines used.
	 */
	private void clearCompletionDisplay() {
		if ( completionDisplayLines > 0 ) {
			// Move cursor up by the number of lines used by completion display
			System.out.print( "\033[" + completionDisplayLines + "A" );
			// Clear from cursor to end of screen
			System.out.print( "\033[0J" );
			System.out.flush();
			completionDisplayLines = 0;
		}
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Charavcter and Word Utilities
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Finds the start of the current word for tab completion.
	 */
	private int findWordStart( String input, int fromPosition ) {
		if ( input.isEmpty() || fromPosition <= 0 ) {
			return 0;
		}

		int pos = Math.min( fromPosition - 1, input.length() - 1 );

		// Move backwards while we see word characters
		while ( pos > 0 && isWordCharacter( input.charAt( pos ) ) ) {
			pos--;
		}

		// If we stopped on a non-word character, move forward one
		if ( pos > 0 && !isWordCharacter( input.charAt( pos ) ) ) {
			pos++;
		}

		return pos;
	}

	/**
	 * Checks if a character is part of a word for completion purposes.
	 */
	private boolean isWordCharacter( char c ) {
		return Character.isLetterOrDigit( c ) || c == '_' || c == ':' || c == '.';
	}

	/**
	 * Redraw the current line with prompt and buffer content
	 */
	private void redraw( String promptStr, StringBuilder buffer ) {
		System.out.print( "\r" );
		System.out.print( CODES.CLEAR_LINE.code() );
		System.out.print( promptStr );

		// Apply syntax highlighting if available
		if ( syntaxHighlighter != null ) {
			String highlighted = syntaxHighlighter.highlight( buffer.toString() );
			System.out.print( highlighted );
		} else {
			System.out.print( buffer );
		}

		System.out.flush();
	}

	/**
	 * ----------------------------------------------------------------------------
	 * History Navigation
	 * ----------------------------------------------------------------------------
	 */

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
}