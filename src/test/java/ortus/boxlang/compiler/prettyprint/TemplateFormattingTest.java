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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.config.Config;

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

	@Test
	@DisplayName( "Space between adjacent template expressions is preserved" )
	public void testAdjacentExpressions() throws IOException {
		Config config = new Config();
		config.getTemplate().setEnabled( true );
		_printTestWithConfig( "template", "adjacent_expressions", config );
	}

	@Test
	@DisplayName( "Template formatting is disabled by default" )
	public void testTemplateFormattingDisabledByDefault() throws IOException {
		File			inputFile	= new File( TEST_RESOURCES_PATH + "template/self_closing_true_input.bxm" );
		ParsingResult	result		= parser.parse( inputFile, false );
		String			actual		= PrettyPrint.prettyPrint( result.getRoot(), new Config() );
		String			expected	= readFile( TEST_RESOURCES_PATH + "template/self_closing_true_input.bxm" );
		assertEqualsIgnoringLineEndings( expected, actual );
	}

	@Test
	@DisplayName( "Space between adjacent expressions is preserved when transpiling CFM to BXM" )
	public void testAdjacentExpressionsTranspile() throws IOException {
		File			inputFile	= new File( TEST_RESOURCES_PATH + "template/adjacent_expressions_input.cfm" );
		ParsingResult	result		= parser.parse( inputFile, false );
		Config			config		= new Config().setSourceType( BoxSourceType.BOXTEMPLATE );
		config.getTemplate().setEnabled( true );
		String			actual		= PrettyPrint.prettyPrint( result.getRoot(), config );
		String			expected	= readFile( TEST_RESOURCES_PATH + "template/adjacent_expressions_output.bxm" );
		assertEqualsIgnoringLineEndings( expected, actual );
	}

	@Test
	@DisplayName( "Space between adjacent expressions is preserved in nested HTML" )
	public void testAdjacentExpressionsNested() throws IOException {
		File			inputFile	= new File( TEST_RESOURCES_PATH + "template/adjacent_expressions_nested_input.cfm" );
		ParsingResult	result		= parser.parse( inputFile );
		Config			config		= new Config().setSourceType( BoxSourceType.BOXTEMPLATE );
		config.getTemplate().setEnabled( true );
		String			actual		= PrettyPrint.prettyPrint( result.getRoot(), config );
		assertTrue( actual.contains( ")# #" ), "Space between adjacent expressions should be preserved. Actual:\n" + actual );
	}

	@Test
	@DisplayName( "Script island content is indented inside bx:script tags" )
	public void testScriptIslandIndent() throws IOException {
		File			inputFile	= new File( TEST_RESOURCES_PATH + "template/script_island_indent_input.cfm" );
		ParsingResult	result		= parser.parse( inputFile );
		Config			config		= new Config().setSourceType( BoxSourceType.BOXTEMPLATE );
		config.getTemplate().setEnabled( true );
		String			actual		= PrettyPrint.prettyPrint( result.getRoot(), config );
		String			expected	= readFile( TEST_RESOURCES_PATH + "template/script_island_indent_output.bxm" );
		assertEqualsIgnoringLineEndings( expected, actual );
	}

	@Test
	@DisplayName( "Closure in template attribute uses script mode for body" )
	public void testClosureInAttribute() throws IOException {
		File			inputFile	= new File( TEST_RESOURCES_PATH + "template/closure_in_attribute_input.cfm" );
		ParsingResult	result		= parser.parse( inputFile );
		Config			config		= new Config().setSourceType( BoxSourceType.BOXTEMPLATE );
		config.getTemplate().setEnabled( true );
		String			actual		= PrettyPrint.prettyPrint( result.getRoot(), config );
		// The closure body should use script-mode return, not <bx:return>
		assertTrue( !actual.contains( "<bx:return" ), "Closure body should use script mode, not template tags. Actual:\n" + actual );
		assertTrue( actual.contains( "return true" ), "Closure body should contain 'return true'. Actual:\n" + actual );
	}

	@Test
	@DisplayName( "cfloop condition closure is unwrapped to plain expression text" )
	public void testCfloopConditionUnwrap() throws IOException {
		File			inputFile	= new File( TEST_RESOURCES_PATH + "template/cfloop_condition_input.cfm" );
		ParsingResult	result		= parser.parse( inputFile );
		Config			config		= new Config().setSourceType( BoxSourceType.BOXTEMPLATE );
		config.getTemplate().setEnabled( true );
		String			actual		= PrettyPrint.prettyPrint( result.getRoot(), config );
		// The condition should be a plain expression, not a closure
		assertTrue( !actual.contains( "function()" ), "Condition should not contain closure syntax. Actual:\n" + actual );
		assertTrue( actual.contains( "condition=\"i lt 10\"" ) || actual.contains( "condition=\"i < 10\"" ),
		    "Condition should be a plain expression. Actual:\n" + actual );
	}
}
