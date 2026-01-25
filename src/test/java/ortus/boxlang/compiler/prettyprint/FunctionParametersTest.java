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

@DisplayName( "Function Parameters Formatting Tests" )
public class FunctionParametersTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Parameters go multiline when count >= multiline_count (custom: 2)" )
	public void testMultilineCount2() throws IOException {
		printTestWithConfigFile( "function_parameters", "multiline_count_2" );
	}

	@Test
	@DisplayName( "Parameters go multiline when count >= multiline_count (default: 4)" )
	public void testMultilineCountDefault() throws IOException {
		printTestWithDefaultConfig( "function_parameters", "multiline_count_default" );
	}

	@Test
	@DisplayName( "Parameters go multiline when length >= multiline_length (custom: 30)" )
	public void testMultilineLength30() throws IOException {
		printTestWithConfigFile( "function_parameters", "multiline_length_30" );
	}

	@Test
	@DisplayName( "Parameters go multiline when length >= multiline_length (60)" )
	public void testMultilineLength60() throws IOException {
		printTestWithConfigFile( "function_parameters", "multiline_length_60" );
	}

	@Test
	@DisplayName( "Trailing comma added when comma_dangle is true and multiline" )
	public void testCommaDangleTrue() throws IOException {
		printTestWithConfigFile( "function_parameters", "comma_dangle_true" );
	}

	@Test
	@DisplayName( "No trailing comma when comma_dangle is false" )
	public void testCommaDangleFalse() throws IOException {
		printTestWithConfigFile( "function_parameters", "comma_dangle_false" );
	}
}
