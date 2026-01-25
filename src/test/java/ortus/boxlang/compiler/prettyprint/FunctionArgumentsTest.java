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

@DisplayName( "Function Arguments Formatting Tests" )
public class FunctionArgumentsTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Arguments go multiline when count >= multiline_count (custom: 2)" )
	public void testMultilineCount2() throws IOException {
		printTestWithConfigFile( "function_arguments", "multiline_count_2" );
	}

	@Test
	@DisplayName( "Arguments go multiline when count >= multiline_count (default: 4)" )
	public void testMultilineCountDefault() throws IOException {
		printTestWithDefaultConfig( "function_arguments", "multiline_count_default" );
	}

	@Test
	@DisplayName( "Arguments go multiline when length >= multiline_length (custom: 30)" )
	public void testMultilineLength30() throws IOException {
		printTestWithConfigFile( "function_arguments", "multiline_length_30" );
	}
}
