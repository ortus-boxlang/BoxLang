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

@DisplayName( "Ternary Style Tests" )
public class TernaryStyleTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Ternary style flat (default)" )
	public void testTernaryStyleFlat() throws IOException {
		printTestWithConfigFile( "operators", "ternary_flat" );
	}

	@Test
	@DisplayName( "Ternary style always-multiline" )
	public void testTernaryStyleMultiline() throws IOException {
		printTestWithConfigFile( "operators", "ternary_multiline" );
	}

	@Test
	@DisplayName( "Ternary style preserve - keeps single line" )
	public void testTernaryStylePreserveSingleLine() throws IOException {
		printTestWithConfigFile( "operators", "ternary_preserve_single" );
	}

	@Test
	@DisplayName( "Ternary style preserve - keeps multiline" )
	public void testTernaryStylePreserveMultiline() throws IOException {
		printTestWithConfigFile( "operators", "ternary_preserve_multi" );
	}
}
