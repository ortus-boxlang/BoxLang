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
package ortus.boxlang.compiler.prettyprint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Tests for PrettyPrint CLI test files and configuration.
 *
 * Note: Direct testing of PrettyPrint.main() is not feasible because it calls System.exit()
 * in several code paths (--help, --check, --initConfig, error handling), which would terminate
 * the test JVM. These tests verify that the test files are valid and can be formatted correctly.
 *
 * For manual CLI testing, use the test files in src/test/resources/prettyprint/cli/:
 *
 * Test single file formatting:
 * ./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -i src/test/resources/prettyprint/cli/input/test1.bxs -o
 * /tmp/test1-out.bxs"
 *
 * Test directory formatting:
 * ./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -i src/test/resources/prettyprint/cli/input -o /tmp/output"
 *
 * Test custom config:
 * ./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -c src/test/resources/prettyprint/cli/custom-config.json -i
 * src/test/resources/prettyprint/cli/input/test1.bxs -o /tmp/test1-custom.bxs"
 *
 * Test check mode (with unformatted file):
 * ./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint --check -i src/test/resources/prettyprint/cli/input/test1.bxs"
 *
 * Test in-place formatting (make a copy first):
 * cp src/test/resources/prettyprint/cli/input/test1.bxs /tmp/test1-copy.bxs
 * ./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -i /tmp/test1-copy.bxs"
 */
public class PrettyPrintMainTest {

	private static final String	TEST_RESOURCES_PATH	= "src/test/resources/prettyprint/cli/";

	protected static BoxRuntime	instance;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@Test
	public void testInputFilesExist() {
		assertTrue( new File( TEST_RESOURCES_PATH + "input/test1.bxs" ).exists(), "test1.bxs should exist" );
		assertTrue( new File( TEST_RESOURCES_PATH + "input/test2.cfm" ).exists(), "test2.cfm should exist" );
		assertTrue( new File( TEST_RESOURCES_PATH + "input/test3.bx" ).exists(), "test3.bx should exist" );
	}

	@Test
	public void testCustomConfigExists() {
		File configFile = new File( TEST_RESOURCES_PATH + "custom-config.json" );
		assertTrue( configFile.exists(), "custom-config.json should exist" );
	}

	@Test
	public void testCustomConfigCanBeLoaded() throws Exception {
		Config config = Config.loadConfig( TEST_RESOURCES_PATH + "custom-config.json" );
		assertNotNull( config, "Config should be loaded" );
		// Custom config has indentSize: 2
		assertTrue( config.getIndentSize() == 2, "Config should have indentSize of 2" );
	}

	@Test
	public void testInputFilesCanBeParsed() throws IOException {
		Parser parser = new Parser();

		// Test test1.bxs
		ParsingResult result1 = parser.parse( new File( TEST_RESOURCES_PATH + "input/test1.bxs" ), false );
		assertTrue( result1.isCorrect(), "test1.bxs should parse correctly" );

		// Test test2.cfm
		ParsingResult result2 = parser.parse( new File( TEST_RESOURCES_PATH + "input/test2.cfm" ), false );
		assertTrue( result2.isCorrect(), "test2.cfm should parse correctly" );

		// Test test3.bx
		ParsingResult result3 = parser.parse( new File( TEST_RESOURCES_PATH + "input/test3.bx" ), false );
		assertTrue( result3.isCorrect(), "test3.bx should parse correctly" );
	}

	@Test
	public void testInputFilesCanBeFormattedWithDefaultConfig() throws IOException {
		Parser	parser	= new Parser();
		Config	config	= new Config();

		// Test formatting test1.bxs
		File			file1	= new File( TEST_RESOURCES_PATH + "input/test1.bxs" );
		ParsingResult	result1	= parser.parse( file1, false );
		String			output1	= PrettyPrint.prettyPrint( result1.getRoot(), config );

		assertNotNull( output1, "Formatted output should not be null" );
		assertTrue( output1.length() > 0, "Formatted output should have content" );
		assertTrue( output1.contains( "function foo" ), "Formatted output should contain function" );
		// Check that it has proper spacing (not double spaces in parameters)
		assertFalse( output1.contains( "(  a,  b  )" ), "Formatted output should not have double spaces" );
		assertFalse( output1.contains( "a+b" ), "Formatted output should have spaces around operators" );
	}

	@Test
	public void testInputFilesCanBeFormattedWithCustomConfig() throws Exception {
		Parser	parser	= new Parser();
		Config	config	= Config.loadConfig( TEST_RESOURCES_PATH + "custom-config.json" );

		// Test formatting with custom config (indentSize: 2)
		File			file1	= new File( TEST_RESOURCES_PATH + "input/test1.bxs" );
		ParsingResult	result1	= parser.parse( file1, false );
		String			output1	= PrettyPrint.prettyPrint( result1.getRoot(), config );

		assertNotNull( output1, "Formatted output should not be null" );
		assertTrue( output1.length() > 0, "Formatted output should have content" );
		// Verify config was actually loaded (indentSize: 2, maxLineLength: 120)
		assertTrue( config.getIndentSize() == 2, "Custom config should have indentSize of 2" );
		assertTrue( config.getMaxLineLength() == 120, "Custom config should have maxLineLength of 120" );
	}

	@Test
	public void testAllTestFilesAreUnformatted() throws IOException {
		// Verify that our test files actually need formatting (they should be intentionally unformatted)
		Parser	parser	= new Parser();
		Config	config	= new Config();

		File	file1		= new File( TEST_RESOURCES_PATH + "input/test1.bxs" );
		String	original1	= Files.readString( file1.toPath() );

		ParsingResult	result1		= parser.parse( file1, false );
		String			formatted1	= PrettyPrint.prettyPrint( result1.getRoot(), config );

		// The original should be different from formatted (proving it needs formatting)
		assertFalse( original1.equals( formatted1 ), "test1.bxs should be unformatted (different from formatted version)" );
	}

	@Test
	public void testFormattingIsIdempotent() throws IOException {
		// Formatting an already-formatted file should produce the same output
		Parser	parser	= new Parser();
		Config	config	= new Config();

		File			file1			= new File( TEST_RESOURCES_PATH + "input/test1.bxs" );
		ParsingResult	result1			= parser.parse( file1, false );
		String			formatted1		= PrettyPrint.prettyPrint( result1.getRoot(), config );

		// Parse the formatted output and format it again
		Path			tempFile		= Files.createTempFile( "test-idempotent", ".bxs" );
		Files.writeString( tempFile, formatted1 );

		ParsingResult	result2			= parser.parse( tempFile.toFile(), false );
		String			formatted2		= PrettyPrint.prettyPrint( result2.getRoot(), config );

		// Should be identical
		assertTrue( formatted1.equals( formatted2 ), "Formatting should be idempotent" );

		Files.delete( tempFile );
	}
}
