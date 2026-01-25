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

@DisplayName( "Braces Formatting Tests" )
public class BracesTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Braces on same line when style is 'same-line'" )
	public void testStyleSameLine() throws IOException {
		printTestWithConfigFile( "braces", "style_same_line" );
	}

	@Test
	@DisplayName( "Braces on new line when style is 'new-line'" )
	public void testStyleNewLine() throws IOException {
		printTestWithConfigFile( "braces", "style_new_line" );
	}

	@Test
	@DisplayName( "Require braces for single statement when true" )
	public void testRequireForSingleStatementTrue() throws IOException {
		printTestWithConfigFile( "braces", "require_for_single_statement_true" );
	}

	@Test
	@DisplayName( "Do not require braces for single statement when false" )
	public void testRequireForSingleStatementFalse() throws IOException {
		printTestWithConfigFile( "braces", "require_for_single_statement_false" );
	}

	@Test
	@DisplayName( "Require braces for else-if chains" )
	public void testRequireForElseIfChains() throws IOException {
		printTestWithConfigFile( "braces", "require_elseif" );
	}

	@Test
	@DisplayName( "Require braces with new-line style" )
	public void testRequireWithNewLineStyle() throws IOException {
		printTestWithConfigFile( "braces", "require_newline" );
	}
}
