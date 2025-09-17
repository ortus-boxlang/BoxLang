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

import ortus.boxlang.runtime.cli.providers.ITabProvider;

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
	private static final String			DEFAULT_PROMPT		= "> ";

	/**
	 * ANSI sequence to clear current line
	 */
	private static final String			CLR_LINE			= "\r\033[2K";

	/**
	 * Operating system detection
	 */
	private static final boolean		WINDOWS				= System.getProperty( "os.name" ).toLowerCase().contains( "win" );

	/**
	 * Current prompt string
	 */
	private String						prompt				= DEFAULT_PROMPT;

	/**
	 * Command history storage (using ArrayList for performance)
	 */
	private final List<String>			history				= new ArrayList<>();

	/**
	 * Current position in command history (-1 means not navigating history)
	 */
	private int							historyIndex		= -1;

	/**
	 * Maximum number of history entries to retain
	 */
	private int							maxHistorySize		= 1000;

	/**
	 * Reusable StringBuilder for input buffering (performance optimization)
	 */
	private final StringBuilder			inputBuffer			= new StringBuilder( 256 );

	/**
	 * Optional syntax highlighter for real-time input coloring
	 */
	private ISyntaxHighlighter			syntaxHighlighter	= null;

	/**
	 * List of registered tab completion providers
	 */
	private final List<ITabProvider>	tabProviders		= new ArrayList<>();

	/**
	 * Current completion state for cycling through suggestions
	 */
	private CompletionState				completionState		= null;

	/**
	 * Constructor with default settings
	 */
	public MiniConsole() {
		// Uses default prompt
	}

	/**
	 * Internal class to track completion state for cycling through suggestions
	 */
	private static class CompletionState {

		private final String				originalInput;
		private final List<TabCompletion>	completions;
		private final int					cursorPosition;
		private int							currentIndex;
		private final int					replaceStart;
		private final int					replaceEnd;

		CompletionState( String originalInput, List<TabCompletion> completions, int cursorPosition, int replaceStart, int replaceEnd ) {
			this.originalInput	= originalInput;
			this.completions	= new ArrayList<>( completions );
			this.cursorPosition	= cursorPosition;
			this.currentIndex	= 0;
			this.replaceStart	= replaceStart;
			this.replaceEnd		= replaceEnd;
		}

		TabCompletion getCurrentCompletion() {
			return this.completions.isEmpty() ? null : this.completions.get( currentIndex );
		}

		void nextCompletion() {
			if ( !this.completions.isEmpty() ) {
				currentIndex = ( currentIndex + 1 ) % this.completions.size();
			}
		}

		boolean hasCompletions() {
			return !this.completions.isEmpty();
		}

		int getCompletionCount() {
			return this.completions.size();
		}

		/**
		 * Get all completions in this state
		 */
		List<TabCompletion> getAllCompletions() {
			return this.completions;
		}

		/**
		 * Get the current completion index
		 */
		int getCurrentIndex() {
			return this.currentIndex;
		}

		/**
		 * Get the original input before any completions
		 */
		String getOriginalInput() {
			return this.originalInput;
		}

		/**
		 * Get the replace start position
		 */
		int getReplaceStart() {
			return this.replaceStart;
		}

		/**
		 * Get the replace end position
		 */
		int getReplaceEnd() {
			return this.replaceEnd;
		}
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
	 * Get the current prompt string
	 *
	 * @return The current prompt string
	 */
	public String getPrompt() {
		return prompt;
	}

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
	public static String color( int colorIndex ) {
		return "\033[38;5;" + colorIndex + "m";
	}

	/**
	 * Create a 256-color background ANSI sequence
	 *
	 * @param colorIndex Color index (0-255)
	 *
	 * @return ANSI escape sequence for background color
	 */
	public static String background( int colorIndex ) {
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

	// Convenient static aliases for common color printing
	public static void printError( String text ) {
		ColorPrint.redText().bold().println( text );
	}

	public static void printSuccess( String text ) {
		ColorPrint.greenText().bold().println( text );
	}

	public static void printWarning( String text ) {
		ColorPrint.yellowText().bold().println( text );
	}

	public static void printInfo( String text ) {
		ColorPrint.blueText().bold().println( text );
	}

	public static void printDebug( String text ) {
		ColorPrint.magentaText().bold().println( text );
	}

	@Override
	public void close() {
		// Nothing to clean up at this level - handled by inner classes
	}

	/**
	 * This function clears the entire console screen and resets the cursor to the top-left corner.
	 */
	public void clear() {
		// ANSI escape sequence to clear the screen and move cursor to top-left
		System.out.print( "\033[2J\033[H" );
		System.out.flush();
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
					// ENTER
					case "ENTER" -> {
						// If we're in completion mode, accept the selected completion
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
						}
						System.out.print( "\r\n" );
						historyIndex = -1;
						String result = inputBuffer.toString();
						addToHistory( result );
						return result;
					}
					// Backspace
					case "BACKSPACE" -> {
						completionState = null; // Clear completion state
						if ( inputBuffer.length() > 0 ) {
							inputBuffer.deleteCharAt( inputBuffer.length() - 1 );
							redraw( prompt, inputBuffer );
						}
					}
					// Up arrow = History navigation
					case "UP" -> {
						completionState = null; // Clear completion state
						String prev = navigateHistoryPrevious();
						if ( prev != null ) {
							inputBuffer.setLength( 0 );
							inputBuffer.append( prev );
							redraw( prompt, inputBuffer );
						}
					}
					// Down arrow = History navigation
					case "DOWN" -> {
						completionState = null; // Clear completion state
						String next = navigateHistoryNext();
						if ( next != null ) {
							inputBuffer.setLength( 0 );
							inputBuffer.append( next );
							redraw( prompt, inputBuffer );
						}
					}
					// Control+C (exit)
					case "CTRL_C" -> {
						System.out.println();
						return null;
					}
					// Control+D (EOF on empty line = exit)
					case "CTRL_D" -> {
						if ( inputBuffer.length() == 0 ) {
							System.out.println();
							return null;
						}
						inputBuffer.setLength( 0 );
						redraw( prompt, inputBuffer );
					}
					// Control + L (clear screen)
					case "CTRL_L" -> {
						clear();
						redraw( prompt, inputBuffer );
					}
					// Tab completion
					case "TAB" -> {
						handleTabCompletion( prompt, inputBuffer );
					}
					default -> {
						if ( token.startsWith( "CHAR:" ) ) {
							int code = Integer.parseInt( token.substring( 5 ) );
							if ( code >= 32 && code < 127 ) {
								completionState = null; // Clear completion state
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
				// If we're in completion mode, accept the selected completion
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
				}
				System.out.print( "\r\n" );
				System.out.flush();
				historyIndex = -1;
				String result = inputBuffer.toString();
				addToHistory( result );
				return result;
			}

			// Backspace (DEL or BS)
			if ( b == 127 || b == 8 ) {
				completionState = null; // Clear completion state
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
				completionState = null; // Clear completion state
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
			completionState = null; // Clear completion state
			String prev = navigateHistoryPrevious();
			if ( prev != null ) {
				buffer.setLength( 0 );
				buffer.append( prev );
				redraw( prompt, buffer );
			}
		} else if ( seq.endsWith( "B" ) ) { // Down arrow (any variant)
			completionState = null; // Clear completion state
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

			completionState = new CompletionState( currentInput, allCompletions, cursorPosition, replaceStart, replaceEnd );
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
	private void showCompletionList( String prompt, StringBuilder buffer, CompletionState state ) {
		List<TabCompletion>	completions		= state.getAllCompletions();
		int					selectedIndex	= state.getCurrentIndex();

		// Show the completion list below the current line
		System.out.print( "\r\n" );

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
				System.out.print( color( 8 ) ); // Dim color for description
				System.out.print( " - " + completion.getDescription() );
			}

			if ( isSelected ) {
				System.out.print( reset() );
			}

			System.out.print( "\r\n" );
		}

		// Show pagination info if there are more items
		if ( completions.size() > maxDisplay ) {
			System.out.print( color( 8 ) ); // Dim color
			System.out.print( "  ... (" + ( selectedIndex + 1 ) + "/" + completions.size() + " - TAB for next, ENTER to accept)" );
			System.out.print( reset() );
			System.out.print( "\r\n" );
		}

		// Show preview of selected completion in the input line
		showCompletionPreview( prompt, buffer, state );
	}

	/**
	 * Shows a preview of the selected completion in the input line without modifying the buffer.
	 */
	private void showCompletionPreview( String prompt, StringBuilder buffer, CompletionState state ) {
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
		System.out.print( CLR_LINE );
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
		System.out.print( CLR_LINE );
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
				    "  'Tab'       { [Console]::Out.WriteLine('TAB'); continue }",
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