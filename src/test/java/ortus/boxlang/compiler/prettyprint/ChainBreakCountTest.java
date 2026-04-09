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

@DisplayName( "Chain Break Count Tests" )
public class ChainBreakCountTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Chain breaks at default break_count (3)" )
	public void testChainBreakCountDefault() throws IOException {
		printTestWithConfigFile( "chain", "chain_break_count_default" );
	}

	@Test
	@DisplayName( "Chain breaks at custom break_count (2)" )
	public void testChainBreakCount2() throws IOException {
		printTestWithConfigFile( "chain", "chain_break_count_2" );
	}

	@Test
	@DisplayName( "Dot access chains break based on break_count" )
	public void testChainDotAccess() throws IOException {
		printTestWithConfigFile( "chain", "chain_dot_access" );
	}

	@Test
	@DisplayName( "Chain breaks at default break_length (60)" )
	public void testChainBreakLengthDefault() throws IOException {
		printTestWithConfigFile( "chain", "chain_break_length_default" );
	}

	@Test
	@DisplayName( "Chain breaks at custom break_length (40)" )
	public void testChainBreakLength40() throws IOException {
		printTestWithConfigFile( "chain", "chain_break_length_40" );
	}

	@Test
	@DisplayName( "Long chain with high break_count but low break_length breaks by length" )
	public void testChainBreakLengthVsCount() throws IOException {
		printTestWithConfigFile( "chain", "chain_break_length_vs_count" );
	}
}
