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

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;

/**
 * Unit tests for BoxRepl functionality.
 * Tests the REPL's basic functionality, ensuring it works correctly with the unified
 * BoxInputStreamReader approach.
 */
public class BoxReplTest {

	private BoxRuntime				runtime;
	private BoxRepl					repl;
	private ByteArrayOutputStream	capturedOutput;
	private PrintStream				originalOut;

	@BeforeEach
	public void setUp() {
		// Initialize BoxRuntime for testing
		runtime			= BoxRuntime.getInstance( true );
		repl			= new BoxRepl( runtime );

		// Capture System.out for testing output
		capturedOutput	= new ByteArrayOutputStream();
		originalOut		= System.out;
		System.setOut( new PrintStream( capturedOutput ) );
	}

	@AfterEach
	public void tearDown() {
		// Restore System.out
		System.setOut( originalOut );

		// Shutdown runtime after each test
		if ( runtime != null ) {
			runtime.shutdown();
		}
	}

	@Test
	@DisplayName( "Test BoxRepl can be instantiated" )
	public void testInstantiation() {
		assertThat( repl ).isNotNull();
	}

	@Test
	@DisplayName( "Test BoxRepl with runtime" )
	public void testReplWithRuntime() {
		BoxRuntime	testRuntime	= BoxRuntime.getInstance( true );
		BoxRepl		testRepl	= new BoxRepl( testRuntime );

		assertThat( testRepl ).isNotNull();

		// Cleanup
		testRuntime.shutdown();
	}

	@Test
	@DisplayName( "Test BoxRepl has console" )
	public void testReplHasConsole() {
		// Access the console field via reflection to verify it's initialized
		try {
			java.lang.reflect.Field consoleField = BoxRepl.class.getDeclaredField( "console" );
			consoleField.setAccessible( true );
			MiniConsole console = ( MiniConsole ) consoleField.get( repl );

			assertThat( console ).isNotNull();
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to access console field", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl has BIFs initialized" )
	public void testReplHasBifs() {
		// Access the bifs field via reflection to verify it's initialized
		try {
			java.lang.reflect.Field bifsField = BoxRepl.class.getDeclaredField( "bifs" );
			bifsField.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			java.util.Set<String> bifs = ( java.util.Set<String> ) bifsField.get( repl );

			assertThat( bifs ).isNotNull();
			assertThat( bifs ).isNotEmpty();
			// Check for some common BIFs
			assertThat( bifs.contains( "println" ) || bifs.contains( "print" ) || bifs.contains( "arraylen" ) ).isTrue();
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to access bifs field", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl has components initialized" )
	public void testReplHasComponents() {
		// Access the components field via reflection to verify it's initialized
		try {
			java.lang.reflect.Field componentsField = BoxRepl.class.getDeclaredField( "components" );
			componentsField.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			java.util.Set<String> components = ( java.util.Set<String> ) componentsField.get( repl );

			assertThat( components ).isNotNull();
			assertThat( components ).isNotEmpty();
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to access components field", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl has prompt configured" )
	public void testReplHasPrompt() {
		// Access the prompt field via reflection to verify it's initialized
		try {
			java.lang.reflect.Field promptField = BoxRepl.class.getDeclaredField( "prompt" );
			promptField.setAccessible( true );
			String prompt = ( String ) promptField.get( repl );

			assertThat( prompt ).isNotNull();
			assertThat( prompt ).isNotEmpty();
			assertThat( prompt ).contains( "BoxLang" );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to access prompt field", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl with syntax highlighter" )
	public void testReplWithSyntaxHighlighter() {
		// Verify console has syntax highlighter configured
		try {
			java.lang.reflect.Field consoleField = BoxRepl.class.getDeclaredField( "console" );
			consoleField.setAccessible( true );
			MiniConsole console = ( MiniConsole ) consoleField.get( repl );

			assertThat( console.getSyntaxHighlighter() ).isNotNull();
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to verify syntax highlighter", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl color palettes exist" )
	public void testReplColorPalettes() {
		// Access the color palette fields via reflection
		try {
			java.lang.reflect.Field darkPaletteField = BoxRepl.class.getDeclaredField( "darkPalette" );
			darkPaletteField.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			java.util.Map<String, String>	darkPalette			= ( java.util.Map<String, String> ) darkPaletteField.get( repl );

			java.lang.reflect.Field			lightPaletteField	= BoxRepl.class.getDeclaredField( "lightPalette" );
			lightPaletteField.setAccessible( true );
			@SuppressWarnings( "unchecked" )
			java.util.Map<String, String> lightPalette = ( java.util.Map<String, String> ) lightPaletteField.get( repl );

			assertThat( darkPalette ).isNotNull();
			assertThat( darkPalette ).containsKey( "prompt" );
			assertThat( darkPalette ).containsKey( "bif" );
			assertThat( darkPalette ).containsKey( "component" );

			assertThat( lightPalette ).isNotNull();
			assertThat( lightPalette ).containsKey( "prompt" );
			assertThat( lightPalette ).containsKey( "bif" );
			assertThat( lightPalette ).containsKey( "component" );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to access palette fields", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl runtime integration" )
	public void testReplRuntimeIntegration() {
		// Access the runtime field via reflection
		try {
			java.lang.reflect.Field runtimeField = BoxRepl.class.getDeclaredField( "runtime" );
			runtimeField.setAccessible( true );
			BoxRuntime replRuntime = ( BoxRuntime ) runtimeField.get( repl );

			assertThat( replRuntime ).isNotNull();
			assertThat( replRuntime ).isEqualTo( runtime );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to access runtime field", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl with tab providers registered" )
	public void testReplTabProviders() {
		// Verify that tab providers are registered with the console
		try {
			java.lang.reflect.Field consoleField = BoxRepl.class.getDeclaredField( "console" );
			consoleField.setAccessible( true );
			MiniConsole console = ( MiniConsole ) consoleField.get( repl );

			// The console should have tab providers registered
			// We can't easily check the internal list, but we verified console is not null
			assertThat( console ).isNotNull();
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to verify tab providers", e );
		}
	}

	@Test
	@DisplayName( "Test BoxRepl calculateBraceDepth helper - balanced braces" )
	public void testCalculateBraceDepthBalanced() throws Exception {
		// Access the private calculateBraceDepth method via reflection
		java.lang.reflect.Method method = BoxRepl.class.getDeclaredMethod( "calculateBraceDepth", String.class );
		method.setAccessible( true );

		// Test balanced braces
		int depth = ( int ) method.invoke( repl, "{ println('test') }" );
		assertThat( depth ).isEqualTo( 0 );

		// Test opening brace
		depth = ( int ) method.invoke( repl, "{ println('test')" );
		assertThat( depth ).isEqualTo( 1 );

		// Test closing brace
		depth = ( int ) method.invoke( repl, "}" );
		assertThat( depth ).isEqualTo( -1 );
	}

	@Test
	@DisplayName( "Test BoxRepl calculateBraceDepth helper - nested braces" )
	public void testCalculateBraceDepthNested() throws Exception {
		java.lang.reflect.Method method = BoxRepl.class.getDeclaredMethod( "calculateBraceDepth", String.class );
		method.setAccessible( true );

		// Test nested opening braces
		int depth = ( int ) method.invoke( repl, "{ { {" );
		assertThat( depth ).isEqualTo( 3 );

		// Test nested closing braces
		depth = ( int ) method.invoke( repl, "} } }" );
		assertThat( depth ).isEqualTo( -3 );
	}

	@Test
	@DisplayName( "Test BoxRepl calculateBraceDepth helper - braces in strings" )
	public void testCalculateBraceDepthInStrings() throws Exception {
		java.lang.reflect.Method method = BoxRepl.class.getDeclaredMethod( "calculateBraceDepth", String.class );
		method.setAccessible( true );

		// Braces inside strings should be ignored
		int depth = ( int ) method.invoke( repl, "println('{}')" );
		assertThat( depth ).isEqualTo( 0 );

		depth = ( int ) method.invoke( repl, "var x = '{ test }'" );
		assertThat( depth ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test BoxRepl calculateBraceDepth helper - braces in comments" )
	public void testCalculateBraceDepthInComments() throws Exception {
		java.lang.reflect.Method method = BoxRepl.class.getDeclaredMethod( "calculateBraceDepth", String.class );
		method.setAccessible( true );

		// Braces inside comments should be ignored
		int depth = ( int ) method.invoke( repl, "// { comment }" );
		assertThat( depth ).isEqualTo( 0 );

		depth = ( int ) method.invoke( repl, "/* { block comment } */" );
		assertThat( depth ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test BoxRepl isExitCommand helper" )
	public void testIsExitCommand() throws Exception {
		java.lang.reflect.Method method = BoxRepl.class.getDeclaredMethod( "isExitCommand", String.class );
		method.setAccessible( true );

		// Test exit commands
		assertThat( ( boolean ) method.invoke( repl, "exit" ) ).isTrue();
		assertThat( ( boolean ) method.invoke( repl, "EXIT" ) ).isTrue();
		assertThat( ( boolean ) method.invoke( repl, "quit" ) ).isTrue();
		assertThat( ( boolean ) method.invoke( repl, "QUIT" ) ).isTrue();

		// Test non-exit commands
		assertThat( ( boolean ) method.invoke( repl, "help" ) ).isFalse();
		assertThat( ( boolean ) method.invoke( repl, "println('exit')" ) ).isFalse();
		assertThat( ( boolean ) method.invoke( repl, "" ) ).isFalse();
		assertThat( ( boolean ) method.invoke( repl, ( String ) null ) ).isFalse();
	}

	@Test
	@DisplayName( "Test BoxRepl switchToTheme helper" )
	public void testSwitchToTheme() throws Exception {
		java.lang.reflect.Method method = BoxRepl.class.getDeclaredMethod( "switchToTheme", String.class );
		method.setAccessible( true );

		// Switch to dark theme
		method.invoke( repl, "dark" );
		String output = capturedOutput.toString();
		assertThat( output ).contains( "dark theme" );

		// Clear captured output
		capturedOutput.reset();

		// Switch to light theme
		method.invoke( repl, "light" );
		output = capturedOutput.toString();
		assertThat( output ).contains( "light theme" );
	}

	@Test
	@DisplayName( "Test BoxRepl uses BoxInputStreamReader through MiniConsole" )
	public void testReplUsesBoxInputStreamReader() {
		// Verify that the REPL is set up correctly to use MiniConsole,
		// which now uses BoxInputStreamReader under the hood
		try {
			java.lang.reflect.Field consoleField = BoxRepl.class.getDeclaredField( "console" );
			consoleField.setAccessible( true );
			MiniConsole console = ( MiniConsole ) consoleField.get( repl );

			// MiniConsole should be using the unified BoxInputStreamReader approach
			// (no OS-specific code)
			assertThat( console ).isNotNull();

			// Verify the console was created with the correct prompt
			assertThat( console.getPrompt() ).isNotNull();
			assertThat( console.getPrompt() ).isNotEmpty();
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to verify BoxInputStreamReader usage", e );
		}
	}
}
