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

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

}
