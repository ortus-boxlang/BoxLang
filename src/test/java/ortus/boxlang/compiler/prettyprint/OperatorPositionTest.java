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

@DisplayName( "Operator Position Tests" )
public class OperatorPositionTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Operators at end of line (default)" )
	public void testOperatorPositionEnd() throws IOException {
		printTestWithConfigFile( "operators", "position_end" );
	}

	@Test
	@DisplayName( "Operators at start of line" )
	public void testOperatorPositionStart() throws IOException {
		printTestWithConfigFile( "operators", "position_start" );
	}

	@Test
	@DisplayName( "Comparison operators at end of line" )
	public void testComparisonPositionEnd() throws IOException {
		printTestWithConfigFile( "operators", "comparison_position_end" );
	}

	@Test
	@DisplayName( "Comparison operators at start of line" )
	public void testComparisonPositionStart() throws IOException {
		printTestWithConfigFile( "operators", "comparison_position_start" );
	}
}
