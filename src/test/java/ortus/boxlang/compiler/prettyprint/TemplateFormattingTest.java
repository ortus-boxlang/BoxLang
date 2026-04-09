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

@DisplayName( "Template Formatting Tests" )
public class TemplateFormattingTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Single attribute per line enabled formats each attribute on its own line" )
	public void testSingleAttributePerLineTrue() throws IOException {
		printTestWithConfigFile( "template", "single_attr_per_line_true" );
	}

	@Test
	@DisplayName( "Single attribute per line disabled keeps attributes on same line" )
	public void testSingleAttributePerLineFalse() throws IOException {
		printTestWithConfigFile( "template", "single_attr_per_line_false" );
	}

	@Test
	@DisplayName( "Self closing enabled outputs components without body as self-closing tags" )
	public void testSelfClosingTrue() throws IOException {
		printTestWithConfigFile( "template", "self_closing_true" );
	}

	@Test
	@DisplayName( "Self closing disabled outputs components without body as open tags" )
	public void testSelfClosingFalse() throws IOException {
		printTestWithConfigFile( "template", "self_closing_false" );
	}
}
