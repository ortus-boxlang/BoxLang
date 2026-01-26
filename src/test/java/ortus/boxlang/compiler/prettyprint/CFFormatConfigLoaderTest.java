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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.prettyprint.config.CFFormatConfigLoader;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.compiler.prettyprint.config.Separator;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Tests for CFFormatConfigLoader - loading and converting CFFormat configuration files.
 */
public class CFFormatConfigLoaderTest {

	private static final String TEST_RESOURCES_PATH = "src/test/resources/prettyprint/cli/";

	@BeforeAll
	public static void setUp() {
		BoxRuntime.getInstance( true );
	}

	@Test
	public void testLoadCFFormatConfig() throws IOException {
		Config config = CFFormatConfigLoader.loadCFFormatConfig( TEST_RESOURCES_PATH + "test.cfformat.json" );

		assertNotNull( config, "Config should not be null" );

		// Check top-level settings
		assertEquals( 4, config.getIndentSize(), "indentSize should be 4" );
		assertTrue( config.getTabIndent(), "tabIndent should be true" );
		assertEquals( 120, config.getMaxLineLength(), "maxLineLength should be 120" );
		assertFalse( config.getSingleQuote(), "singleQuote should be false (strings.quote: double)" );

		// Check padding settings
		assertTrue( config.getBracketPadding(), "bracketPadding should be true" );
		assertTrue( config.getParensPadding(), "parensPadding should be true" );
		assertTrue( config.getBinaryOperatorsPadding(), "binaryOperatorsPadding should be true" );
		assertTrue( config.getForLoopSemicolons().getPadding(), "forLoopSemicolons.padding should be true" );
	}

	@Test
	public void testLoadCFFormatConfigArraySettings() throws IOException {
		Config config = CFFormatConfigLoader.loadCFFormatConfig( TEST_RESOURCES_PATH + "test.cfformat.json" );

		// Check array settings
		assertTrue( config.getArray().getPadding(), "array.padding should be true" );
		assertFalse( config.getArray().getEmptyPadding(), "array.emptyPadding should be false" );
		assertEquals( 2, config.getArray().getMultiline().getElementCount(), "array.multiline.elementCount should be 2" );
		assertEquals( 40, config.getArray().getMultiline().getMinLength(), "array.multiline.minLength should be 40" );
		assertFalse( config.getArray().getMultiline().getLeadingComma().getEnabled(), "array.multiline.leadingComma should be false" );
		assertTrue( config.getArray().getMultiline().getLeadingComma().getPadding(), "array.multiline.leadingComma.padding should be true" );
	}

	@Test
	public void testLoadCFFormatConfigStructSettings() throws IOException {
		Config config = CFFormatConfigLoader.loadCFFormatConfig( TEST_RESOURCES_PATH + "test.cfformat.json" );

		// Check struct settings
		assertTrue( config.getStruct().getPadding(), "struct.padding should be true" );
		assertFalse( config.getStruct().getEmptyPadding(), "struct.emptyPadding should be false" );
		assertEquals( Separator.COLON_SPACE, config.getStruct().getSeparator(), "struct.separator should be COLON_SPACE" );
		assertEquals( 2, config.getStruct().getMultiline().getElementCount(), "struct.multiline.elementCount should be 2" );
		assertEquals( 40, config.getStruct().getMultiline().getMinLength(), "struct.multiline.minLength should be 40" );
		assertFalse( config.getStruct().getMultiline().getLeadingComma().getEnabled(), "struct.multiline.leadingComma should be false" );
		assertTrue( config.getStruct().getMultiline().getLeadingComma().getPadding(), "struct.multiline.leadingComma.padding should be true" );
	}

	@Test
	public void testLoadCFFormatConfigFunctionSettings() throws IOException {
		Config config = CFFormatConfigLoader.loadCFFormatConfig( TEST_RESOURCES_PATH + "test.cfformat.json" );

		// Check function declaration settings (maps to function.parameters)
		assertEquals( 3, config.getFunction().getParameters().getMultilineCount(), "function.parameters.multilineCount should be 3" );
		assertEquals( 40, config.getFunction().getParameters().getMultilineLength(), "function.parameters.multilineLength should be 40" );

		// Check function call settings (maps to arguments)
		assertEquals( 3, config.getArguments().getMultilineCount(), "arguments.multilineCount should be 3" );
		assertEquals( 40, config.getArguments().getMultilineLength(), "arguments.multilineLength should be 40" );
	}

	@Test
	public void testConvertCFFormatToConfigMap() {
		Map<String, Object> cfConfig = new HashMap<>();
		cfConfig.put( "indent_size", 2 );
		cfConfig.put( "tab_indent", false );
		cfConfig.put( "max_columns", 100 );
		cfConfig.put( "strings.quote", "single" );
		cfConfig.put( "brackets.padding", false );
		cfConfig.put( "parentheses.padding", false );

		Config config = CFFormatConfigLoader.convertCFFormatToConfig( cfConfig );

		assertEquals( 2, config.getIndentSize() );
		assertFalse( config.getTabIndent() );
		assertEquals( 100, config.getMaxLineLength() );
		assertTrue( config.getSingleQuote(), "singleQuote should be true for strings.quote: single" );
		assertFalse( config.getBracketPadding() );
		assertFalse( config.getParensPadding() );
	}

	@Test
	public void testConvertToBoxFormatJSON() throws IOException {
		String bxFormatJSON = CFFormatConfigLoader.convertToBoxFormatJSON( TEST_RESOURCES_PATH + "test.cfformat.json" );

		assertNotNull( bxFormatJSON, "JSON output should not be null" );
		assertTrue( bxFormatJSON.contains( "\"indentSize\"" ), "JSON should contain indentSize" );
		assertTrue( bxFormatJSON.contains( "\"maxLineLength\"" ), "JSON should contain maxLineLength" );
		assertTrue( bxFormatJSON.contains( "\"struct\"" ), "JSON should contain struct" );
		assertTrue( bxFormatJSON.contains( "\"array\"" ), "JSON should contain array" );
	}

	@Test
	public void testConvertAndWriteBoxFormatFile() throws IOException {
		Path tempFile = Files.createTempFile( "bxformat-test", ".json" );

		try {
			CFFormatConfigLoader.convertAndWriteBoxFormatFile( TEST_RESOURCES_PATH + "test.cfformat.json", tempFile.toString() );

			assertTrue( Files.exists( tempFile ), "Output file should exist" );

			String content = Files.readString( tempFile );
			assertTrue( content.contains( "\"indentSize\"" ), "Output should contain indentSize" );
			assertTrue( content.contains( "\"tabIndent\"" ), "Output should contain tabIndent" );
		} finally {
			Files.deleteIfExists( tempFile );
		}
	}

	@Test
	public void testConfigLoadWithFallback() throws IOException {
		// Create a temp directory with only a .cfformat.json file
		Path tempDir = Files.createTempDirectory( "bxformat-fallback-test" );

		try {
			// Copy the test cfformat file to the temp directory
			Path cfFormatFile = tempDir.resolve( ".cfformat.json" );
			Files.copy( Path.of( TEST_RESOURCES_PATH + "test.cfformat.json" ), cfFormatFile );

			// Load config with fallback - should find .cfformat.json
			Config config = Config.loadConfigWithFallback( tempDir.toString() );

			assertNotNull( config, "Config should be loaded from fallback" );
			assertEquals( 4, config.getIndentSize(), "indentSize should match cfformat value" );
			assertEquals( 120, config.getMaxLineLength(), "maxLineLength should match cfformat value" );
		} finally {
			// Cleanup
			Files.walk( tempDir ).sorted( java.util.Comparator.reverseOrder() ).forEach( p -> {
				try {
					Files.delete( p );
				} catch ( IOException e ) {
					// ignore
				}
			} );
		}
	}

	@Test
	public void testConfigLoadPrefersBxFormat() throws IOException {
		// Create a temp directory with both config files
		Path tempDir = Files.createTempDirectory( "bxformat-prefer-test" );

		try {
			// Create .bxformat.json with different values
			Path bxFormatFile = tempDir.resolve( ".bxformat.json" );
			Files.writeString( bxFormatFile, "{\"indentSize\": 8, \"maxLineLength\": 200}" );

			// Copy .cfformat.json
			Path cfFormatFile = tempDir.resolve( ".cfformat.json" );
			Files.copy( Path.of( TEST_RESOURCES_PATH + "test.cfformat.json" ), cfFormatFile );

			// Load config - should prefer .bxformat.json
			Config config = Config.loadConfigWithFallback( tempDir.toString() );

			assertNotNull( config, "Config should be loaded" );
			assertEquals( 8, config.getIndentSize(), "Should use bxformat value, not cfformat" );
			assertEquals( 200, config.getMaxLineLength(), "Should use bxformat value, not cfformat" );
		} finally {
			// Cleanup
			Files.walk( tempDir ).sorted( java.util.Comparator.reverseOrder() ).forEach( p -> {
				try {
					Files.delete( p );
				} catch ( IOException e ) {
					// ignore
				}
			} );
		}
	}

	@Test
	public void testConfigAutoDetect() throws IOException {
		// Test loading a .cfformat.json file directly
		Config cfConfig = Config.loadConfigAutoDetect( TEST_RESOURCES_PATH + "test.cfformat.json" );
		assertNotNull( cfConfig, "Should load cfformat config" );
		assertEquals( 4, cfConfig.getIndentSize() );

		// Test loading a .bxformat.json file directly (using custom-config.json as example)
		Config bxConfig = Config.loadConfigAutoDetect( TEST_RESOURCES_PATH + "custom-config.json" );
		assertNotNull( bxConfig, "Should load bxformat config" );
		assertEquals( 2, bxConfig.getIndentSize() );
	}

	@Test
	public void testIsCFFormatConfig() {
		assertTrue( Config.isCFFormatConfig( ".cfformat.json" ) );
		assertTrue( Config.isCFFormatConfig( "/path/to/.cfformat.json" ) );
		assertTrue( Config.isCFFormatConfig( "test.cfformat.json" ) );
		assertFalse( Config.isCFFormatConfig( ".bxformat.json" ) );
		assertFalse( Config.isCFFormatConfig( "config.json" ) );
		assertFalse( Config.isCFFormatConfig( null ) );
	}

	@Test
	public void testSeparatorParsing() {
		// Test various separator formats
		Map<String, Object> cfConfig = new HashMap<>();

		// Test " : " separator
		cfConfig.put( "struct.separator", " : " );
		Config config1 = CFFormatConfigLoader.convertCFFormatToConfig( cfConfig );
		assertEquals( Separator.COLON_SPACE, config1.getStruct().getSeparator() );

		// Test ":" separator
		cfConfig.put( "struct.separator", ":" );
		Config config2 = CFFormatConfigLoader.convertCFFormatToConfig( cfConfig );
		assertEquals( Separator.COLON, config2.getStruct().getSeparator() );

		// Test "=" separator
		cfConfig.put( "struct.separator", "=" );
		Config config3 = CFFormatConfigLoader.convertCFFormatToConfig( cfConfig );
		assertEquals( Separator.EQUALS, config3.getStruct().getSeparator() );

		// Test " = " separator
		cfConfig.put( "struct.separator", " = " );
		Config config4 = CFFormatConfigLoader.convertCFFormatToConfig( cfConfig );
		assertEquals( Separator.EQUALS_SPACE, config4.getStruct().getSeparator() );
	}
}
