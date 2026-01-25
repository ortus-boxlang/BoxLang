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

@DisplayName( "Class Formatting Tests" )
public class ClassFormattingTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Member spacing 1 (default) - one blank line between members" )
	public void testMemberSpacing1() throws IOException {
		printTestWithConfigFile( "class", "member_spacing_1" );
	}

	@Test
	@DisplayName( "Member spacing 0 - no blank lines between members" )
	public void testMemberSpacing0() throws IOException {
		printTestWithConfigFile( "class", "member_spacing_0" );
	}

	@Test
	@DisplayName( "Member spacing 2 - two blank lines between members" )
	public void testMemberSpacing2() throws IOException {
		printTestWithConfigFile( "class", "member_spacing_2" );
	}
}
