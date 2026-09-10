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

@DisplayName( "Arrow Function Formatting Tests" )
public class ArrowFunctionsTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Parentheses always added when parens is 'always'" )
	public void testParensAlways() throws IOException {
		printTestWithConfigFile( "arrow_functions", "parens_always" );
	}

	@Test
	@DisplayName( "Parentheses omitted for simple single param when parens is 'avoid'" )
	public void testParensAvoid() throws IOException {
		printTestWithConfigFile( "arrow_functions", "parens_avoid" );
	}
}
