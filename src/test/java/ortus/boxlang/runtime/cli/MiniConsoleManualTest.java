/**
 * Manual test script for MiniConsole with CustomInputStreamReader
 * 
 * This is a simple demonstration to verify:
 * - Basic input reading works
 * - Arrow keys for history navigation work
 * - Tab completion works
 * - Ctrl+C and Ctrl+D work
 * - Multi-byte characters (UTF-8) work
 * 
 * To run this test:
 * 1. Compile the project: ./gradlew compileJava
 * 2. Run this script directly (not automated, requires manual interaction)
 */
package ortus.boxlang.runtime.cli;

import java.io.IOException;

public class MiniConsoleManualTest {

	public static void main( String[] args ) {
		System.out.println( "==================================================" );
		System.out.println( "MiniConsole Manual Test with CustomInputStreamReader" );
		System.out.println( "==================================================" );
		System.out.println();
		System.out.println( "Test the following features:" );
		System.out.println( "  1. Type some text and press ENTER" );
		System.out.println( "  2. Use UP/DOWN arrows to navigate history" );
		System.out.println( "  3. Type UTF-8 characters like: 世界 or 😀" );
		System.out.println( "  4. Press Ctrl+D on empty line to exit" );
		System.out.println( "  5. Press Ctrl+C to cancel current input" );
		System.out.println();
		System.out.println( "Type 'exit' or press Ctrl+D to quit." );
		System.out.println( "==================================================" );
		System.out.println();

		try ( MiniConsole console = new MiniConsole() ) {
			console.setPrompt( MiniConsole.color( 33 ) + "test> " + MiniConsole.reset() );

			String input;
			while ( ( input = console.readLine() ) != null ) {
				if ( "exit".equalsIgnoreCase( input.trim() ) ) {
					System.out.println( "Goodbye!" );
					break;
				}

				if ( input.trim().isEmpty() ) {
					continue;
				}

				// Echo back what was entered
				System.out.println( MiniConsole.color( 40 ) + "You entered: " + MiniConsole.reset() + input );

				// Test UTF-8 by checking if input contains multi-byte characters
				if ( input.codePoints().anyMatch( cp -> cp > 127 ) ) {
					System.out.println( MiniConsole.color( 34 ) + "✓ UTF-8 characters detected!" + MiniConsole.reset() );
				}

				// Show history
				if ( "history".equalsIgnoreCase( input.trim() ) ) {
					console.showHistory();
				}
			}
		} catch ( IOException e ) {
			System.err.println( "Error: " + e.getMessage() );
			e.printStackTrace();
		}
	}
}
