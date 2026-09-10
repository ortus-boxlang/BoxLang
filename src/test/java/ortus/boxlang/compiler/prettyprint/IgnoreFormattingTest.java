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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.config.Config;

@DisplayName( "Formatter Ignore Region Tests" )
public class IgnoreFormattingTest extends PrettyPrintTest {

	@Test
	@DisplayName( "cfformat-ignore-start / cfformat-ignore-end markers preserve unformatted region" )
	public void testCfformatMarkers() throws IOException {
		printTestWithDefaultConfig( "ignore", "cfformat_markers" );
	}

	@Test
	@DisplayName( "@formatter:off / @formatter:on markers preserve unformatted region" )
	public void testFormatterOffOn() throws IOException {
		printTestWithDefaultConfig( "ignore", "formatter_off" );
	}

	@Test
	@DisplayName( "bxformat-ignore-start / bxformat-ignore-end markers preserve unformatted region" )
	public void testBxformatMarkers() throws IOException {
		printTestWithDefaultConfig( "ignore", "bxformat_markers" );
	}

	@Test
	@DisplayName( "Multiple ignore regions in the same file are each preserved independently" )
	public void testMultipleRegions() throws IOException {
		printTestWithDefaultConfig( "ignore", "multiple_regions" );
	}

	@Test
	@DisplayName( "Start marker without a matching end marker ignores to end of file" )
	public void testNoEndMarker() throws IOException {
		printTestWithDefaultConfig( "ignore", "no_end_marker" );
	}

	@Test
	@DisplayName( "Transpiler-injected AST nodes with null source text inside ignore region are still output" )
	public void testNullSourceTextFromTranspiler() throws IOException {
		// This CFML code triggers the CFTranspilerVisitor to wrap arrayDelete in an IIFE
		// because the return value is used. The injected nodes have null position/sourceText.
		String			cfmlCode	= """
		                              x = 1+2;
		                              // cfformat-ignore-start
		                              result = arrayDelete(arr, "item");
		                              // cfformat-ignore-end
		                              a = 7+8;
		                              """;

		// Parse with transpilation enabled so the CFTranspilerVisitor injects synthetic nodes
		ParsingResult	result		= parser.parse( cfmlCode, BoxSourceType.CFSCRIPT, false, true );

		// Format should not throw and should contain the transpiled arrayDelete code
		String			output		= assertDoesNotThrow( () -> PrettyPrint.prettyPrint( result.getRoot(), new Config() ) );

		// The transpiled output should contain arrayDelete (inside the IIFE wrapper)
		assertTrue( output.contains( "arrayDelete" ), "Output should contain arrayDelete but was:\n" + output );
		// The ignore markers should still be present
		assertTrue( output.contains( "cfformat-ignore-start" ), "Output should contain ignore-start marker" );
		assertTrue( output.contains( "cfformat-ignore-end" ), "Output should contain ignore-end marker" );
		// The normal formatted code outside the region should still be formatted
		assertTrue( output.contains( "x = 1 + 2" ), "Code before ignore region should be formatted" );
		assertTrue( output.contains( "a = 7 + 8" ), "Code after ignore region should be formatted" );
	}

}
