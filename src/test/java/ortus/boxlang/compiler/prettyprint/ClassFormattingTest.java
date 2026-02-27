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

	@Test
	@DisplayName( "Property order preserve (default) - maintains original order" )
	public void testPropertyOrderPreserve() throws IOException {
		printTestWithConfigFile( "class", "property_order_preserve" );
	}

	@Test
	@DisplayName( "Property order alphabetical - sorts properties by name" )
	public void testPropertyOrderAlphabetical() throws IOException {
		printTestWithConfigFile( "class", "property_order_alphabetical" );
	}

	@Test
	@DisplayName( "Property order length - sorts properties by source text length" )
	public void testPropertyOrderLength() throws IOException {
		printTestWithConfigFile( "class", "property_order_length" );
	}

	@Test
	@DisplayName( "Property order type - sorts properties by type" )
	public void testPropertyOrderType() throws IOException {
		printTestWithConfigFile( "class", "property_order_type" );
	}

	@Test
	@DisplayName( "Property order alphabetical with shorthand syntax - sorts by name" )
	public void testPropertyOrderAlphabeticalShorthand() throws IOException {
		printTestWithConfigFile( "class", "property_order_alphabetical_shorthand" );
	}

	@Test
	@DisplayName( "Method order preserve (default) - maintains original order" )
	public void testMethodOrderPreserve() throws IOException {
		printTestWithConfigFile( "class", "method_order_preserve" );
	}

	@Test
	@DisplayName( "Method order alphabetical - sorts methods by name" )
	public void testMethodOrderAlphabetical() throws IOException {
		printTestWithConfigFile( "class", "method_order_alphabetical" );
	}

	@Test
	@DisplayName( "Method grouping disabled (default) - maintains original order" )
	public void testMethodGroupingDisabled() throws IOException {
		printTestWithConfigFile( "class", "method_grouping_disabled" );
	}

	@Test
	@DisplayName( "Method grouping enabled - groups public methods first" )
	public void testMethodGroupingEnabled() throws IOException {
		printTestWithConfigFile( "class", "method_grouping_enabled" );
	}

	@Test
	@DisplayName( "Method grouping with alphabetical - public first, then alphabetical within groups" )
	public void testMethodGroupingWithAlphabetical() throws IOException {
		printTestWithConfigFile( "class", "method_grouping_with_alphabetical" );
	}
}
