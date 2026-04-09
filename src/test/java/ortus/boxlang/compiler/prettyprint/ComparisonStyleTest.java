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

@DisplayName( "Comparison Style Tests" )
public class ComparisonStyleTest extends PrettyPrintTest {

	@Test
	@DisplayName( "symbols style normalizes keyword operators to symbols" )
	public void testSymbolsStyle() throws IOException {
		printTestWithConfigFile( "operators", "comparison_style_symbols" );
	}

	@Test
	@DisplayName( "keywords style normalizes symbol operators to keywords" )
	public void testKeywordsStyle() throws IOException {
		printTestWithConfigFile( "operators", "comparison_style_keywords" );
	}

	@Test
	@DisplayName( "preserve style keeps each operator in its original form" )
	public void testPreserveStyle() throws IOException {
		printTestWithConfigFile( "operators", "comparison_style_preserve" );
	}

	@Test
	@DisplayName( "strict equality operators are unaffected by keywords style" )
	public void testStrictOperatorsUnaffected() throws IOException {
		printTestWithConfigFile( "operators", "comparison_style_strict" );
	}

}
