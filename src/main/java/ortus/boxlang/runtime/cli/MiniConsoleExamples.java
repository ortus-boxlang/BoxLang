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

/**
 * MiniConsole Usage Examples
 *
 * This class demonstrates various ways to use the MiniConsole class
 * for building interactive command-line applications.
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.6.0
 */
public class MiniConsoleExamples {

	/**
	 * Example 1: Basic console with default prompt
	 */
	public static void basicExample() throws IOException {
		try ( MiniConsole console = new MiniConsole() ) {
			String input;
			while ( ( input = console.readLine() ) != null ) {
				if ( "exit".equals( input ) )
					break;
				System.out.println( "You entered: " + input );
			}
		}
	}

	/**
	 * Example 2: Custom prompt with colors
	 */
	public static void coloredPromptExample() throws IOException {
		try ( MiniConsole console = new MiniConsole() ) {
			String prompt = MiniConsole.fg256( 196 ) + "MyApp" + MiniConsole.fg256( 39 ) + "> " + MiniConsole.reset();
			console.setPrompt( prompt );

			String input;
			while ( ( input = console.readLine() ) != null ) {
				if ( "quit".equals( input ) )
					break;

				// Process commands
				if ( input.startsWith( "echo " ) ) {
					System.out.println( input.substring( 5 ) );
				} else if ( "history".equals( input ) ) {
					console.showHistory();
				} else {
					System.out.println( "Unknown command: " + input );
				}
			}
		}
	}

	/**
	 * Example 3: Database CLI tool example
	 */
	public static void databaseCLIExample() throws IOException {
		try ( MiniConsole console = new MiniConsole() ) {
			console.setPrompt( "SQL> " );
			console.setMaxHistorySize( 100 );

			String query;
			while ( ( query = console.readLine() ) != null ) {
				if ( "exit".equals( query ) || "quit".equals( query ) )
					break;

				if ( query.trim().isEmpty() )
					continue;

				try {
					// Execute SQL query here
					System.out.println( "Executing: " + query );
					// ... database logic ...
				} catch ( Exception e ) {
					System.err.println( "Error: " + e.getMessage() );
				}
			}
		}
	}

	/**
	 * Example 4: Calculator with history
	 */
	public static void calculatorExample() throws IOException {
		try ( MiniConsole console = new MiniConsole( "calc> " ) ) {
			String expression;
			while ( ( expression = console.readLine() ) != null ) {
				if ( "exit".equals( expression ) )
					break;

				if ( "clear".equals( expression ) ) {
					console.clearHistory();
					System.out.println( "History cleared." );
					continue;
				}

				if ( "!!".equals( expression ) ) {
					String last = console.getLastCommand();
					if ( last != null ) {
						expression = last;
						System.out.println( "Repeating: " + expression );
					} else {
						System.out.println( "No previous command." );
						continue;
					}
				}

				try {
					// Evaluate mathematical expression
					// double result = evaluateExpression(expression);
					// System.out.println("= " + result);
					System.out.println( "Would evaluate: " + expression );
				} catch ( Exception e ) {
					System.err.println( "Invalid expression: " + e.getMessage() );
				}
			}
		}
	}

	/**
	 * Example 5: Multi-line input handling
	 */
	public static void multiLineExample() throws IOException {
		try ( MiniConsole console = new MiniConsole( "script> " ) ) {
			StringBuilder	multiLineInput	= new StringBuilder();
			boolean			inMultiLine		= false;

			String			line;
			while ( ( line = console.readLine() ) != null ) {
				if ( "exit".equals( line ) )
					break;

				if ( line.endsWith( "\\" ) ) {
					// Continue on next line
					multiLineInput.append( line.substring( 0, line.length() - 1 ) ).append( "\n" );
					inMultiLine = true;
					console.setPrompt( "     > " );
					continue;
				}

				if ( inMultiLine ) {
					multiLineInput.append( line );
					String fullInput = multiLineInput.toString();

					// Process the complete multi-line input
					System.out.println( "Complete input:\n" + fullInput );

					// Reset for next input
					multiLineInput.setLength( 0 );
					inMultiLine = false;
					console.setPrompt( "script> " );
				} else {
					// Single line input
					System.out.println( "Single line: " + line );
				}
			}
		}
	}

	/**
	 * Advanced CLI with subcommands and mode switching
	 */
	public static class AdvancedCLI {

		private MiniConsole	console;
		private String		currentMode	= "main";

		public void start() throws IOException {
			console = new MiniConsole();
			updatePrompt();

			String input;
			while ( ( input = console.readLine() ) != null ) {
				if ( "exit".equals( input ) || "quit".equals( input ) )
					break;

				handleCommand( input );
			}

			console.close();
		}

		private void handleCommand( String input ) {
			String[]	parts	= input.split( "\\s+" );
			String		command	= parts[ 0 ];

			switch ( currentMode ) {
				case "main" -> handleMainCommand( command, parts );
				case "database" -> handleDatabaseCommand( command, parts );
				case "file" -> handleFileCommand( command, parts );
			}
		}

		private void handleMainCommand( String command, String[] parts ) {
			switch ( command ) {
				case "db" -> {
					currentMode = "database";
					updatePrompt();
					System.out.println( "Entered database mode. Type 'back' to return." );
				}
				case "file" -> {
					currentMode = "file";
					updatePrompt();
					System.out.println( "Entered file mode. Type 'back' to return." );
				}
				case "history" -> console.showHistory();
				default -> System.out.println( "Unknown command: " + command );
			}
		}

		private void handleDatabaseCommand( String command, String[] parts ) {
			switch ( command ) {
				case "back" -> {
					currentMode = "main";
					updatePrompt();
					System.out.println( "Returned to main mode." );
				}
				case "tables" -> System.out.println( "Listing tables..." );
				case "query" -> System.out.println( "Execute query: " + String.join( " ", parts ) );
				default -> System.out.println( "Database command: " + command );
			}
		}

		private void handleFileCommand( String command, String[] parts ) {
			switch ( command ) {
				case "back" -> {
					currentMode = "main";
					updatePrompt();
					System.out.println( "Returned to main mode." );
				}
				case "ls" -> System.out.println( "Listing files..." );
				case "cat" -> System.out.println( "Reading file: " + ( parts.length > 1 ? parts[ 1 ] : "" ) );
				default -> System.out.println( "File command: " + command );
			}
		}

		private void updatePrompt() {
			String prompt = switch ( currentMode ) {
				case "database" -> MiniConsole.fg256( 34 ) + "DB" + MiniConsole.reset() + "> ";
				case "file" -> MiniConsole.fg256( 226 ) + "FILE" + MiniConsole.reset() + "> ";
				default -> MiniConsole.fg256( 39 ) + "CLI" + MiniConsole.reset() + "> ";
			};
			console.setPrompt( prompt );
		}
	}
}