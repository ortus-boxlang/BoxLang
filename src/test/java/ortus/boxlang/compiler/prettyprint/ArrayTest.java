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

import org.junit.Test;

public class ArrayTest extends PrettyPrintTest {

	@Test
	public void testArrayEmptyPadding() throws IOException {
		printTestWithConfigFile( "array", "empty_padding_true" );
	}

	@Test
	public void testArrayEmptyPaddingDefault() throws IOException {
		printTestWithDefaultConfig( "array", "empty_padding_default" );
	}

	@Test
	public void testArrayMultiline() throws IOException {
		printTestWithConfigFile( "array", "multiline_element_count" );
	}
}
