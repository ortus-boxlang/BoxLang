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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class PropertyFileTest {

	static BoxRuntime	instance;
	static String		testFile	= "src/test/resources/test.properties";

	@TempDir
	Path				tempDir;

	PropertyFile		propertyFile;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void beforeEach() {
		propertyFile = new PropertyFile();
	}

	// =========================================================================
	// Loading Tests
	// =========================================================================

	@Test
	@DisplayName( "It should load a properties file and return the correct values" )
	public void testLoadPropertiesFile() {
		PropertyFile pf = new PropertyFile().load( testFile );
		System.out.println( pf.getAsStruct() );

		// Test basic key-value
		assertThat( pf.get( "simpleKey" ) ).isEqualTo( "simpleValue" );

		// Test different delimiters
		assertThat( pf.get( "keyWithSpaces" ) ).isEqualTo( "valueWithSpaces" );
		assertThat( pf.get( "colonKey" ) ).isEqualTo( "colonValue" );
		assertThat( pf.get( "spaceKey" ) ).isEqualTo( "valueWithSpaceDelimiter" );

		// Test escaped characters
		assertThat( pf.get( "escapedEquals" ) ).isEqualTo( "foo=bar" );
		assertThat( pf.get( "escapedColon" ) ).isEqualTo( "foo:bar" );
		assertThat( pf.get( "escapedSpace" ) ).isEqualTo( "foo bar" );
		assertThat( pf.get( "escapedBackslash" ) ).isEqualTo( "foo\\bar" );

		// Test unicode
		assertThat( pf.get( "unicodeKey" ) ).isEqualTo( "BoxLang" );

		// Test special characters
		assertThat( pf.get( "specialChars" ) ).isEqualTo( "tab\tnewline\ncarriage\rformfeed\fquote\"apostrophe'" );

		// Test whitespace handling
		assertThat( pf.get( "whitespaceKey" ) ).isEqualTo( "valueWithLeadingAndTrailingSpaces" );

		// Test empty values
		assertThat( pf.get( "emptyValue" ) ).isEqualTo( "" );

		// Test line continuation
		assertThat( pf.get( "longValue" ) ).isEqualTo( "This is a long value that continues on the next line and ends here." );
		assertThat( pf.get( "longValueWithSpaces" ) ).isEqualTo( "This value has spaces at the end   and at the start of the next line." );

		// Test delimiters in values
		assertThat( pf.get( "valueWithEquals" ) ).isEqualTo( "foo=bar" );
		assertThat( pf.get( "valueWithColon" ) ).isEqualTo( "foo:bar" );

		// Test duplicate keys (last wins)
		assertThat( pf.get( "duplicateKey" ) ).isEqualTo( "secondValue" );

		// Test escaped unicode
		assertThat( pf.get( "escapedUnicode" ) ).isEqualTo( "Box" );

		// Test multi-line escaped
		assertThat( pf.get( "multiLineEscaped" ) ).isEqualTo( "Line1\nLine2\tTabbed\nLine3" );

		// Test backslash values
		assertThat( pf.get( "backslashOnly" ) ).isEqualTo( "\\" );
		assertThat( pf.get( "backslashValue" ) ).isEqualTo( "\\" );

		// Test special single character values
		assertThat( pf.get( "colonOnly" ) ).isEqualTo( ":" );
		assertThat( pf.get( "equalsOnly" ) ).isEqualTo( "=" );
	}

	@Test
	@DisplayName( "It should handle all line continuation and backslash scenarios correctly" )
	public void testAllContinuationScenarios() throws Exception {
		Path	tempFile	= tempDir.resolve( "all_scenarios.properties" );
		String	content		= "# Various scenarios\n" +
		    "\n" +
		    "# Normal property\n" +
		    "normal=value\n" +
		    "\n" +
		    "# Property with single backslash value\n" +
		    "justBackslash=\\\n" +
		    "\n" +
		    "# Property with backslash and spaces\n" +
		    "backslashSpaces=  \\  \n" +
		    "\n" +
		    "# Legitimate line continuation\n" +
		    "continued=start \\\n" +
		    "middle \\\n" +
		    "end\n" +
		    "# Another continuation\n" +
		    "longValue=This is a long value that \\\n" +
		    "continues on the next line and \\\n" +
		    "ends here.\n" +
		    "\n" +
		    "# Double backslash (no continuation)\n" +
		    "doubleBackslash=value\\\\\n" +
		    "\n" +
		    "# Triple backslash (continuation)\n" +
		    "tripleBackslash=value\\\\\\\n" +
		    "# Colon with backslash value\n" +
		    "colonBackslash:\\\n" +
		    "\n" +
		    "# Final property\n" +
		    "final=value\n";

		Files.write( tempFile, content.getBytes() );

		PropertyFile pf = new PropertyFile().load( tempFile.toString() );

		// Debug output
		System.out.println( "=== Properties parsed ===" );
		System.out.println( pf.getLines() );
		System.out.println( "Total properties: " + pf.size() );

		// Normal property
		assertThat( pf.get( "normal" ) ).isEqualTo( "value" );

		// Single backslash properties
		assertThat( pf.exists( "justBackslash" ) ).isTrue();
		assertThat( pf.get( "justBackslash" ) ).isEqualTo( "\\" );

		assertThat( pf.exists( "backslashSpaces" ) ).isTrue();
		assertThat( pf.get( "backslashSpaces" ) ).isEqualTo( "\\" );

		// Line continuations
		assertThat( pf.get( "continued" ) ).isEqualTo( "start middle end" );
		assertThat( pf.get( "longValue" ) ).isEqualTo( "This is a long value that continues on the next line and ends here." );

		// Escaped backslashes
		assertThat( pf.get( "doubleBackslash" ) ).isEqualTo( "value\\" );
		assertThat( pf.get( "tripleBackslash" ) ).isEqualTo( "value\\\\" );

		// Different delimiters with backslash
		assertThat( pf.get( "colonBackslash" ) ).isEqualTo( "\\" );

		// Final property
		assertThat( pf.get( "final" ) ).isEqualTo( "value" );

		// Should not have any properties named after continuation lines
		assertThat( pf.exists( "middle" ) ).isFalse();
		assertThat( pf.exists( "end" ) ).isFalse();
		assertThat( pf.exists( "continues" ) ).isFalse();
		assertThat( pf.exists( "continued" ) ).isTrue(); // This is a real property name
	}

	@Test
	@DisplayName( "It should throw exception when file doesn't exist" )
	public void testLoadNonExistentFile() {
		assertThrows( BoxRuntimeException.class, () -> {
			new PropertyFile().load( "nonexistent.properties" );
		} );
	}

	@Test
	@DisplayName( "It should throw exception when path is null" )
	public void testLoadNullPath() {
		assertThrows( NullPointerException.class, () -> {
			new PropertyFile().load( null );
		} );
	}

	@Test
	@DisplayName( "It should throw exception when path is blank" )
	public void testLoadBlankPath() {
		assertThrows( BoxRuntimeException.class, () -> {
			new PropertyFile().load( "   " );
		} );
	}

	// =========================================================================
	// Getting Values Tests
	// =========================================================================

	@Test
	@DisplayName( "It should get values with defaults" )
	public void testGetWithDefault() {
		PropertyFile pf = new PropertyFile().load( testFile );

		assertThat( pf.get( "simpleKey", "default" ) ).isEqualTo( "simpleValue" );
		assertThat( pf.get( "nonExistentKey", "defaultValue" ) ).isEqualTo( "defaultValue" );
	}

	@Test
	@DisplayName( "It should throw exception when key doesn't exist" )
	public void testGetNonExistentKey() {
		PropertyFile pf = new PropertyFile().load( testFile );

		assertThrows( IllegalArgumentException.class, () -> {
			pf.get( "nonExistentKey" );
		} );
	}

	@Test
	@DisplayName( "It should check if key exists" )
	public void testExists() {
		PropertyFile pf = new PropertyFile().load( testFile );

		assertThat( pf.exists( "simpleKey" ) ).isTrue();
		assertThat( pf.exists( "nonExistentKey" ) ).isFalse();
	}

	// =========================================================================
	// Setting Values Tests
	// =========================================================================

	@Test
	@DisplayName( "It should set new properties" )
	public void testSetNewProperty() {
		PropertyFile pf = new PropertyFile();

		pf.set( "newKey", "newValue" );

		assertThat( pf.get( "newKey" ) ).isEqualTo( "newValue" );
		assertThat( pf.exists( "newKey" ) ).isTrue();
		assertThat( pf.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "It should update existing properties" )
	public void testSetExistingProperty() {
		PropertyFile	pf				= new PropertyFile().load( testFile );

		String			originalValue	= pf.get( "simpleKey" );
		pf.set( "simpleKey", "updatedValue" );

		assertThat( pf.get( "simpleKey" ) ).isEqualTo( "updatedValue" );
		assertThat( pf.get( "simpleKey" ) ).isNotEqualTo( originalValue );
	}

	@Test
	@DisplayName( "It should set multiple properties from Map" )
	public void testSetMultiFromMap() {
		PropertyFile		pf		= new PropertyFile();

		Map<String, String>	props	= new HashMap<>();
		props.put( "key1", "value1" );
		props.put( "key2", "value2" );
		props.put( "key3", "value3" );

		pf.setMulti( props );

		assertThat( pf.get( "key1" ) ).isEqualTo( "value1" );
		assertThat( pf.get( "key2" ) ).isEqualTo( "value2" );
		assertThat( pf.get( "key3" ) ).isEqualTo( "value3" );
		assertThat( pf.size() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "It should set multiple properties from IStruct" )
	public void testSetMultiFromIStruct() {
		PropertyFile	pf		= new PropertyFile();

		IStruct			props	= new Struct();
		props.put( "key1", "value1" );
		props.put( "key2", "value2" );
		props.put( "key3", "value3" );

		pf.setMulti( props );

		assertThat( pf.get( "key1" ) ).isEqualTo( "value1" );
		assertThat( pf.get( "key2" ) ).isEqualTo( "value2" );
		assertThat( pf.get( "key3" ) ).isEqualTo( "value3" );
		assertThat( pf.size() ).isEqualTo( 3 );
	}

	// =========================================================================
	// Removing Properties Tests
	// =========================================================================

	@Test
	@DisplayName( "It should remove existing properties" )
	public void testRemoveProperty() {
		PropertyFile	pf				= new PropertyFile().load( testFile );

		int				originalSize	= pf.size();
		assertThat( pf.exists( "simpleKey" ) ).isTrue();

		pf.remove( "simpleKey" );

		assertThat( pf.exists( "simpleKey" ) ).isFalse();
		assertThat( pf.size() ).isEqualTo( originalSize - 1 );
	}

	@Test
	@DisplayName( "It should handle removing non-existent properties" )
	public void testRemoveNonExistentProperty() {
		PropertyFile	pf				= new PropertyFile().load( testFile );

		int				originalSize	= pf.size();
		pf.remove( "nonExistentKey" );

		assertThat( pf.size() ).isEqualTo( originalSize );
	}

	@Test
	@DisplayName( "It should remove multiple properties" )
	public void testRemoveMultipleProperties() {
		PropertyFile	pf				= new PropertyFile().load( testFile );

		Array			keysToRemove	= new Array();
		keysToRemove.add( "simpleKey" );
		keysToRemove.add( "colonKey" );
		keysToRemove.add( "nonExistentKey" ); // Should handle gracefully

		int originalSize = pf.size();
		pf.removeMulti( keysToRemove );

		assertThat( pf.exists( "simpleKey" ) ).isFalse();
		assertThat( pf.exists( "colonKey" ) ).isFalse();
		assertThat( pf.size() ).isEqualTo( originalSize - 2 );
	}

	// =========================================================================
	// Static Factory Methods Tests
	// =========================================================================

	@Test
	@DisplayName( "It should create PropertyFile from Map" )
	public void testFromMap() {
		Map<String, String> props = new HashMap<>();
		props.put( "key1", "value1" );
		props.put( "key2", "value2" );

		PropertyFile pf = PropertyFile.fromMap( props );

		assertThat( pf.get( "key1" ) ).isEqualTo( "value1" );
		assertThat( pf.get( "key2" ) ).isEqualTo( "value2" );
		assertThat( pf.size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It should create PropertyFile from IStruct" )
	public void testFromIStruct() {
		IStruct props = new Struct();
		props.put( "key1", "value1" );
		props.put( "key2", "value2" );

		PropertyFile pf = PropertyFile.fromIStruct( props );

		assertThat( pf.get( "key1" ) ).isEqualTo( "value1" );
		assertThat( pf.get( "key2" ) ).isEqualTo( "value2" );
		assertThat( pf.size() ).isEqualTo( 2 );
	}

	// =========================================================================
	// Structure Methods Tests
	// =========================================================================

	@Test
	@DisplayName( "It should return properties as IStruct" )
	public void testGetAsStruct() {
		PropertyFile pf = new PropertyFile();
		pf.set( "key1", "value1" );
		pf.set( "key2", "value2" );

		IStruct result = pf.getAsStruct();

		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.getAsString( Key.of( "key1" ) ) ).isEqualTo( "value1" );
		assertThat( result.getAsString( Key.of( "key2" ) ) ).isEqualTo( "value2" );
	}

	@Test
	@DisplayName( "It should return properties as Map" )
	public void testGetAsMap() {
		PropertyFile pf = new PropertyFile();
		pf.set( "key1", "value1" );
		pf.set( "key2", "value2" );

		Map<String, String> result = pf.getAsMap();

		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.get( "key1" ) ).isEqualTo( "value1" );
		assertThat( result.get( "key2" ) ).isEqualTo( "value2" );
	}

	@Test
	@DisplayName( "It should return property names" )
	public void testGetPropertyNames() {
		PropertyFile pf = new PropertyFile();
		pf.set( "key1", "value1" );
		pf.set( "key2", "value2" );
		pf.set( "key3", "value3" );

		Array names = pf.getPropertyNames();

		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names.contains( "key1" ) ).isTrue();
		assertThat( names.contains( "key2" ) ).isTrue();
		assertThat( names.contains( "key3" ) ).isTrue();
	}

	// =========================================================================
	// Size and State Tests
	// =========================================================================

	@Test
	@DisplayName( "It should return correct size" )
	public void testSize() {
		PropertyFile	pf				= new PropertyFile().load( testFile );
		// Count properties in test file (excluding comments and whitespace)
		int				expectedSize	= 29; // Adjust based on actual count in test.properties
		assertThat( pf.size() ).isEqualTo( expectedSize );
	}

	@Test
	@DisplayName( "It should check if has properties" )
	public void testHasProperties() {
		PropertyFile	emptyPf		= new PropertyFile();
		PropertyFile	loadedPf	= new PropertyFile().load( testFile );

		assertThat( emptyPf.hasProperties() ).isFalse();
		assertThat( loadedPf.hasProperties() ).isTrue();
	}

	@Test
	@DisplayName( "It should clear properties while preserving structure" )
	public void testClearProperties() {
		PropertyFile pf = new PropertyFile().load( testFile );

		assertThat( pf.hasProperties() ).isTrue();
		assertThat( pf.size() ).isGreaterThan( 0 );

		pf.clearProperties();

		assertThat( pf.hasProperties() ).isFalse();
		assertThat( pf.size() ).isEqualTo( 0 );
		// Comments and whitespace should still be preserved in lines
		assertThat( pf.getLines().size() ).isGreaterThan( 0 );
	}

	@Test
	@DisplayName( "It should clear everything" )
	public void testClear() {
		PropertyFile pf = new PropertyFile().load( testFile );

		assertThat( pf.hasProperties() ).isTrue();
		assertThat( pf.getLines().size() ).isGreaterThan( 0 );

		pf.clear();

		assertThat( pf.hasProperties() ).isFalse();
		assertThat( pf.size() ).isEqualTo( 0 );
		assertThat( pf.getLines().size() ).isEqualTo( 0 );
	}

	// =========================================================================
	// Comments and Formatting Tests
	// =========================================================================

	@Test
	@DisplayName( "It should add comments" )
	public void testAddComment() {
		PropertyFile pf = new PropertyFile();

		pf.addComment( "This is a test comment" );
		pf.set( "testKey", "testValue" );

		assertThat( pf.getLines().size() ).isEqualTo( 2 );
		assertThat( pf.size() ).isEqualTo( 1 ); // Only counts properties
	}

	@Test
	@DisplayName( "It should add blank lines" )
	public void testAddBlankLine() {
		PropertyFile pf = new PropertyFile();

		pf.set( "key1", "value1" );
		pf.addBlankLine();
		pf.set( "key2", "value2" );

		assertThat( pf.getLines().size() ).isEqualTo( 3 );
		assertThat( pf.size() ).isEqualTo( 2 ); // Only counts properties
	}

	// =========================================================================
	// Merging Tests
	// =========================================================================

	@Test
	@DisplayName( "It should merge from IStruct" )
	public void testMergeStruct() {
		PropertyFile pf = new PropertyFile();
		pf.set( "existing", "original" );

		IStruct incoming = new Struct();
		incoming.put( "existing", "updated" );
		incoming.put( "new", "value" );

		pf.mergeStruct( incoming );

		assertThat( pf.get( "existing" ) ).isEqualTo( "updated" );
		assertThat( pf.get( "new" ) ).isEqualTo( "value" );
		assertThat( pf.size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It should merge from Map" )
	public void testMergeMap() {
		PropertyFile pf = new PropertyFile();
		pf.set( "existing", "original" );

		Map<String, String> incoming = new HashMap<>();
		incoming.put( "existing", "updated" );
		incoming.put( "new", "value" );

		pf.mergeMap( incoming );

		assertThat( pf.get( "existing" ) ).isEqualTo( "updated" );
		assertThat( pf.get( "new" ) ).isEqualTo( "value" );
		assertThat( pf.size() ).isEqualTo( 2 );
	}

	// =========================================================================
	// Storage Tests
	// =========================================================================

	@Test
	@DisplayName( "It should store to specified path" )
	public void testStoreToPath() throws Exception {
		PropertyFile pf = new PropertyFile();
		pf.set( "testKey", "testValue" );
		pf.addComment( "Test comment" );

		Path outputFile = tempDir.resolve( "output.properties" );
		pf.store( outputFile.toString() );

		assertThat( Files.exists( outputFile ) ).isTrue();

		// Load it back and verify
		PropertyFile loaded = new PropertyFile().load( outputFile.toString() );
		assertThat( loaded.get( "testKey" ) ).isEqualTo( "testValue" );
	}

	@Test
	@DisplayName( "It should store to loaded path" )
	public void testStoreToLoadedPath() throws Exception {
		// Create a temporary file first
		Path tempFile = tempDir.resolve( "temp.properties" );
		Files.write( tempFile, "originalKey=originalValue\n".getBytes() );

		PropertyFile pf = new PropertyFile().load( tempFile.toString() );
		pf.set( "newKey", "newValue" );

		pf.store(); // Should store to the loaded path

		// Load it back and verify
		PropertyFile reloaded = new PropertyFile().load( tempFile.toString() );
		assertThat( reloaded.get( "originalKey" ) ).isEqualTo( "originalValue" );
		assertThat( reloaded.get( "newKey" ) ).isEqualTo( "newValue" );
	}

	// =========================================================================
	// Getters and Setters Tests
	// =========================================================================

	@Test
	@DisplayName( "It should get and set path" )
	public void testPathGetterSetter() {
		PropertyFile pf = new PropertyFile();

		assertThat( pf.getPath() ).isNull();

		pf.setPath( "/test/path" );
		assertThat( pf.getPath() ).isEqualTo( "/test/path" );
	}

	@Test
	@DisplayName( "It should get and set max line width" )
	public void testMaxLineWidthGetterSetter() {
		PropertyFile pf = new PropertyFile();

		assertThat( pf.getMaxLineWidth() ).isEqualTo( 150 ); // Default value

		pf.setMaxLineWidth( 200 );
		assertThat( pf.getMaxLineWidth() ).isEqualTo( 200 );
	}

	@Test
	@DisplayName( "It should get and set lines" )
	public void testLinesGetterSetter() {
		PropertyFile pf = new PropertyFile();

		assertThat( pf.getLines() ).isNotNull();
		assertThat( pf.getLines().size() ).isEqualTo( 0 );

		pf.set( "testKey", "testValue" );
		assertThat( pf.getLines().size() ).isEqualTo( 1 );
	}

	// =========================================================================
	// Edge Cases and Error Handling Tests
	// =========================================================================

	@Test
	@DisplayName( "It should handle properties with special characters in names" )
	public void testSpecialCharacterNames() throws Exception {
		Path	tempFile	= tempDir.resolve( "special.properties" );
		String	content		= "key\\ with\\ spaces=value1\n" +
		    "key\\=with\\=equals=value2\n" +
		    "key\\:with\\:colons=value3\n";
		Files.write( tempFile, content.getBytes() );

		PropertyFile pf = new PropertyFile().load( tempFile.toString() );

		assertThat( pf.get( "key with spaces" ) ).isEqualTo( "value1" );
		assertThat( pf.get( "key=with=equals" ) ).isEqualTo( "value2" );
		assertThat( pf.get( "key:with:colons" ) ).isEqualTo( "value3" );
	}

	@Test
	@DisplayName( "It should handle incomplete unicode escape" )
	public void testIncompleteUnicodeEscape() throws Exception {
		Path	tempFile	= tempDir.resolve( "incomplete.properties" );
		String	content		= "key=value\\u123\n"; // Incomplete unicode escape
		Files.write( tempFile, content.getBytes() );

		assertThrows( IllegalArgumentException.class, () -> {
			new PropertyFile().load( tempFile.toString() );
		} );
	}

	@Test
	@DisplayName( "It should handle invalid unicode characters" )
	public void testInvalidUnicodeCharacters() throws Exception {
		Path	tempFile	= tempDir.resolve( "invalid_unicode.properties" );
		String	content		= "key=value\\u12GH\n"; // Invalid hex characters
		Files.write( tempFile, content.getBytes() );

		assertThrows( IllegalArgumentException.class, () -> {
			new PropertyFile().load( tempFile.toString() );
		} );
	}

	@Test
	@DisplayName( "It should preserve order of properties" )
	public void testPropertyOrder() {
		PropertyFile pf = new PropertyFile();
		pf.set( "first", "1" );
		pf.set( "second", "2" );
		pf.set( "third", "3" );

		Array names = pf.getPropertyNames();
		assertThat( names.get( 0 ) ).isEqualTo( "first" );
		assertThat( names.get( 1 ) ).isEqualTo( "second" );
		assertThat( names.get( 2 ) ).isEqualTo( "third" );
	}

	@Test
	@DisplayName( "It should handle fluent chaining" )
	public void testFluentChaining() {
		PropertyFile result = new PropertyFile()
		    .set( "key1", "value1" )
		    .set( "key2", "value2" )
		    .addComment( "Test comment" )
		    .addBlankLine()
		    .set( "key3", "value3" );

		assertThat( result.size() ).isEqualTo( 3 );
		assertThat( result.get( "key1" ) ).isEqualTo( "value1" );
		assertThat( result.get( "key2" ) ).isEqualTo( "value2" );
		assertThat( result.get( "key3" ) ).isEqualTo( "value3" );
	}
}