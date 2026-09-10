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
package ortus.boxlang.compiler.prettyprint.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;

@DisplayName( "Config Tests" )
public class ConfigTest {

	@Test
	@DisplayName( "Config has correct defaults" )
	public void testDefaults() {
		Config config = new Config();

		assertEquals( 4, config.getIndentSize() );
		assertTrue( config.getTabIndent() );
		assertEquals( 115, config.getMaxLineLength() );
		assertEquals( "\n", config.getNewLine() );
		assertFalse( config.getSingleQuote() );
		assertTrue( config.getBracketPadding() );
		assertTrue( config.getParensPadding() );
		assertTrue( config.getBinaryOperatorsPadding() );
		assertTrue( config.getSemicolons() );
	}

	@Test
	@DisplayName( "Semicolons option can be set and retrieved" )
	public void testSemicolonsOption() {
		Config config = new Config();

		assertTrue( config.getSemicolons() );

		config.setSemicolons( false );
		assertFalse( config.getSemicolons() );

		config.setSemicolons( true );
		assertTrue( config.getSemicolons() );
	}

	@Test
	@DisplayName( "Semicolons option is included in toMap" )
	public void testSemicolonsInToMap() {
		Config config = new Config();
		config.setSemicolons( false );

		Map<String, Object> map = config.toMap();

		assertNotNull( map.get( "semicolons" ) );
		assertEquals( false, map.get( "semicolons" ) );
	}

	@Test
	@DisplayName( "Semicolons option can be loaded from map" )
	public void testSemicolonsFromMap() {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "semicolons", false );

		Config config = new Config().loadFromConfig( configMap );

		assertFalse( config.getSemicolons() );
	}

	@Test
	@DisplayName( "Config nested objects have correct defaults" )
	public void testNestedDefaults() {
		Config config = new Config();

		assertNotNull( config.getStruct() );
		assertNotNull( config.getArray() );
		assertNotNull( config.getProperty() );
		assertNotNull( config.getForLoopSemicolons() );
		assertNotNull( config.getFunction() );
	}

	@Test
	@DisplayName( "Config toMap includes all nested objects" )
	public void testToMapIncludesAllNested() {
		Config				config	= new Config();
		Map<String, Object>	map		= config.toMap();

		assertNotNull( map.get( "struct" ) );
		assertNotNull( map.get( "array" ) );
		assertNotNull( map.get( "property" ) );
		assertNotNull( map.get( "for_loop_semicolons" ) );
		assertNotNull( map.get( "function" ) );
	}

	@Test
	@DisplayName( "Config loadFromConfig handles partial config" )
	public void testPartialConfigLoad() {
		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "indentSize", 2 );
		configMap.put( "tabIndent", false );

		Config config = new Config().loadFromConfig( configMap );

		// Changed values
		assertEquals( 2, config.getIndentSize() );
		assertFalse( config.getTabIndent() );

		// Unchanged defaults
		assertEquals( 115, config.getMaxLineLength() );
		assertTrue( config.getSemicolons() );
	}

	@Test
	@DisplayName( "Config fluent setters return this" )
	public void testFluentSetters() {
		Config config = new Config()
		    .setIndentSize( 2 )
		    .setTabIndent( false )
		    .setSemicolons( false )
		    .setMaxLineLength( 120 );

		assertEquals( 2, config.getIndentSize() );
		assertFalse( config.getTabIndent() );
		assertFalse( config.getSemicolons() );
		assertEquals( 120, config.getMaxLineLength() );
	}

	@Test
	@DisplayName( "indentToLevel produces correct indentation" )
	public void testIndentToLevel() {
		Config config = new Config().setIndentSize( 4 ).setTabIndent( false );

		assertEquals( "", config.indentToLevel( 0 ) );
		assertEquals( "    ", config.indentToLevel( 1 ) );
		assertEquals( "        ", config.indentToLevel( 2 ) );
	}

	@Test
	@DisplayName( "indentToLevel with tabs produces correct indentation" )
	public void testIndentToLevelWithTabs() {
		Config config = new Config().setIndentSize( 4 ).setTabIndent( true );

		assertEquals( "", config.indentToLevel( 0 ) );
		assertEquals( "\t", config.indentToLevel( 1 ) );
		assertEquals( "\t\t", config.indentToLevel( 2 ) );
	}

	@Test
	@DisplayName( "lineSeparator returns correct value" )
	public void testLineSeparator() {
		Config config = new Config();

		// Default is "\n"
		assertEquals( "\n", config.lineSeparator() );

		config.setNewLine( "os" );
		assertEquals( System.lineSeparator(), config.lineSeparator() );

		config.setNewLine( "\r\n" );
		assertEquals( "\r\n", config.lineSeparator() );
	}

	@Test
	@DisplayName( "clone produces a deep copy with identical values" )
	public void testCloneHasIdenticalValues() {
		Config original = new Config();
		original.setIndentSize( 2 );
		original.setTabIndent( false );
		original.setMaxLineLength( 80 );
		original.setNewLine( "\r\n" );
		original.setSingleQuote( true );
		original.setPreserveStringQuotes( true );
		original.setAlignConsecutiveAssignments( false );
		original.setAlignConsecutiveProperties( false );
		original.setBracketPadding( false );
		original.setParensPadding( false );
		original.setBinaryOperatorsPadding( false );
		original.setSemicolons( false );
		original.setCFFormatCompatibility( true );
		original.setSourceType( BoxSourceType.BOXSCRIPT );
		original.getArguments().setSeparator( Separator.EQUALS );

		Config clone = original.clone();

		assertEquals( original.getIndentSize(), clone.getIndentSize() );
		assertEquals( original.getTabIndent(), clone.getTabIndent() );
		assertEquals( original.getMaxLineLength(), clone.getMaxLineLength() );
		assertEquals( original.getNewLine(), clone.getNewLine() );
		assertEquals( original.getSingleQuote(), clone.getSingleQuote() );
		assertEquals( original.getPreserveStringQuotes(), clone.getPreserveStringQuotes() );
		assertEquals( original.getAlignConsecutiveAssignments(), clone.getAlignConsecutiveAssignments() );
		assertEquals( original.getAlignConsecutiveProperties(), clone.getAlignConsecutiveProperties() );
		assertEquals( original.getBracketPadding(), clone.getBracketPadding() );
		assertEquals( original.getParensPadding(), clone.getParensPadding() );
		assertEquals( original.getBinaryOperatorsPadding(), clone.getBinaryOperatorsPadding() );
		assertEquals( original.getSemicolons(), clone.getSemicolons() );
		assertEquals( original.getCFFormatCompatibility(), clone.getCFFormatCompatibility() );
		assertEquals( original.getSourceType(), clone.getSourceType() );
		assertEquals( original.getArguments().getSeparator(), clone.getArguments().getSeparator() );
	}

	@Test
	@DisplayName( "clone produces independent nested objects" )
	public void testCloneNestedObjectsAreIndependent() {
		Config original = new Config();
		original.getStruct().setPadding( false );
		original.getStruct().setQuoteKeys( true );
		original.getStruct().setSeparator( Separator.EQUALS_BOTH_SPACE );
		original.getStruct().getMultiline().setElementCount( 10 );
		original.getStruct().getMultiline().setMinLength( 200 );
		original.getStruct().getMultiline().getLeadingComma().setEnabled( true );

		Config clone = original.clone();

		// Verify values match
		assertFalse( clone.getStruct().getPadding() );
		assertTrue( clone.getStruct().getQuoteKeys() );
		assertEquals( Separator.EQUALS_BOTH_SPACE, clone.getStruct().getSeparator() );
		assertEquals( 10, clone.getStruct().getMultiline().getElementCount() );
		assertEquals( 200, clone.getStruct().getMultiline().getMinLength() );
		assertTrue( clone.getStruct().getMultiline().getLeadingComma().getEnabled() );

		// Verify objects are different instances
		assertNotSame( original.getStruct(), clone.getStruct() );
		assertNotSame( original.getStruct().getMultiline(), clone.getStruct().getMultiline() );
		assertNotSame( original.getStruct().getMultiline().getLeadingComma(), clone.getStruct().getMultiline().getLeadingComma() );

		// Modify clone and verify original is unaffected
		clone.getStruct().setPadding( true );
		clone.getStruct().getMultiline().setElementCount( 99 );
		clone.getStruct().getMultiline().getLeadingComma().setEnabled( false );

		assertFalse( original.getStruct().getPadding() );
		assertEquals( 10, original.getStruct().getMultiline().getElementCount() );
		assertTrue( original.getStruct().getMultiline().getLeadingComma().getEnabled() );
	}

	@Test
	@DisplayName( "clone produces independent function config" )
	public void testCloneFunctionConfigIsIndependent() {
		Config original = new Config();
		original.getFunction().setStyle( "arrow" );
		original.getFunction().getParameters().setPadding( false );
		original.getFunction().getParameters().setMultilineCount( 5 );
		original.getFunction().getArrow().setParens( "avoid" );

		Config clone = original.clone();

		// Verify values match
		assertEquals( "arrow", clone.getFunction().getStyle() );
		assertFalse( clone.getFunction().getParameters().getPadding() );
		assertEquals( 5, clone.getFunction().getParameters().getMultilineCount() );
		assertEquals( "avoid", clone.getFunction().getArrow().getParens() );

		// Verify independent instances
		assertNotSame( original.getFunction(), clone.getFunction() );
		assertNotSame( original.getFunction().getParameters(), clone.getFunction().getParameters() );
		assertNotSame( original.getFunction().getArrow(), clone.getFunction().getArrow() );

		// Modify clone and verify original is unaffected
		clone.getFunction().setStyle( "declaration" );
		clone.getFunction().getParameters().setMultilineCount( 1 );

		assertEquals( "arrow", original.getFunction().getStyle() );
		assertEquals( 5, original.getFunction().getParameters().getMultilineCount() );
	}

	@Test
	@DisplayName( "clone produces independent arguments config" )
	public void testCloneArgumentsConfigIsIndependent() {
		Config original = new Config();
		original.getArguments().setPadding( false );
		original.getArguments().setEmptyPadding( true );
		original.getArguments().setCommaDangle( true );
		original.getArguments().setMultilineCount( 7 );
		original.getArguments().setMultilineLength( 90 );
		original.getArguments().setSeparator( Separator.EQUALS );

		Config clone = original.clone();

		assertFalse( clone.getArguments().getPadding() );
		assertTrue( clone.getArguments().getEmptyPadding() );
		assertTrue( clone.getArguments().getCommaDangle() );
		assertEquals( 7, clone.getArguments().getMultilineCount() );
		assertEquals( 90, clone.getArguments().getMultilineLength() );
		assertEquals( Separator.EQUALS, clone.getArguments().getSeparator() );

		assertNotSame( original.getArguments(), clone.getArguments() );

		clone.getArguments().setSeparator( Separator.COLON );
		assertEquals( Separator.EQUALS, original.getArguments().getSeparator() );
	}

	@Test
	@DisplayName( "clone produces independent braces and operators config" )
	public void testCloneBracesAndOperatorsAreIndependent() {
		Config original = new Config();
		original.getBraces().setStyle( "new-line" );
		original.getBraces().setRequireForSingleStatement( false );
		original.getBraces().getElseConfig().setStyle( "new-line" );
		original.getOperators().setPosition( "start" );
		original.getOperators().setComparisonStyle( "keywords" );
		original.getOperators().getTernary().setStyle( "always-multiline" );
		original.getOperators().getTernary().setQuestionPosition( "end" );

		Config clone = original.clone();

		// Verify values
		assertEquals( "new-line", clone.getBraces().getStyle() );
		assertFalse( clone.getBraces().getRequireForSingleStatement() );
		assertEquals( "new-line", clone.getBraces().getElseConfig().getStyle() );
		assertEquals( "start", clone.getOperators().getPosition() );
		assertEquals( "keywords", clone.getOperators().getComparisonStyle() );
		assertEquals( "always-multiline", clone.getOperators().getTernary().getStyle() );
		assertEquals( "end", clone.getOperators().getTernary().getQuestionPosition() );

		// Verify independent instances
		assertNotSame( original.getBraces(), clone.getBraces() );
		assertNotSame( original.getBraces().getElseConfig(), clone.getBraces().getElseConfig() );
		assertNotSame( original.getOperators(), clone.getOperators() );
		assertNotSame( original.getOperators().getTernary(), clone.getOperators().getTernary() );

		// Modify clone and verify original is unaffected
		clone.getBraces().getElseConfig().setStyle( "same-line" );
		clone.getOperators().getTernary().setStyle( "flat" );

		assertEquals( "new-line", original.getBraces().getElseConfig().getStyle() );
		assertEquals( "always-multiline", original.getOperators().getTernary().getStyle() );
	}

	@Test
	@DisplayName( "clone produces independent simple config objects" )
	public void testCloneSimpleConfigsAreIndependent() {
		Config original = new Config();
		original.getArguments().setPadding( false );
		original.getArguments().setMultilineCount( 7 );
		original.getArray().setPadding( false );
		original.getArray().getMultiline().setElementCount( 8 );
		original.getProperty().getKeyValue().setPadding( true );
		original.getProperty().getMultiline().setMinLength( 100 );
		original.getForLoopSemicolons().setPadding( false );
		original.getChain().setBreakCount( 5 );
		original.getTemplate().setComponentPrefix( "cf" );
		original.getImportConfig().setSort( true );
		original.getComments().setWrap( true );
		original.getClassConfig().setMemberSpacing( 2 );
		original.getSql().setUppercaseKeywords( false );

		Config clone = original.clone();

		// Verify all independent
		assertNotSame( original.getArguments(), clone.getArguments() );
		assertNotSame( original.getArray(), clone.getArray() );
		assertNotSame( original.getArray().getMultiline(), clone.getArray().getMultiline() );
		assertNotSame( original.getProperty(), clone.getProperty() );
		assertNotSame( original.getProperty().getKeyValue(), clone.getProperty().getKeyValue() );
		assertNotSame( original.getProperty().getMultiline(), clone.getProperty().getMultiline() );
		assertNotSame( original.getForLoopSemicolons(), clone.getForLoopSemicolons() );
		assertNotSame( original.getChain(), clone.getChain() );
		assertNotSame( original.getTemplate(), clone.getTemplate() );
		assertNotSame( original.getImportConfig(), clone.getImportConfig() );
		assertNotSame( original.getComments(), clone.getComments() );
		assertNotSame( original.getClassConfig(), clone.getClassConfig() );
		assertNotSame( original.getSql(), clone.getSql() );

		// Modify clone and verify original is unaffected
		clone.getArguments().setMultilineCount( 1 );
		clone.getArray().getMultiline().setElementCount( 1 );
		clone.getProperty().getKeyValue().setPadding( false );
		clone.getTemplate().setComponentPrefix( "bx" );

		assertEquals( 7, original.getArguments().getMultilineCount() );
		assertEquals( 8, original.getArray().getMultiline().getElementCount() );
		assertTrue( original.getProperty().getKeyValue().getPadding() );
		assertEquals( "cf", original.getTemplate().getComponentPrefix() );
	}

	@Test
	@DisplayName( "clone toJSON matches original toJSON" )
	public void testCloneToJSONMatchesOriginal() {
		Config original = new Config();
		original.setIndentSize( 3 );
		original.getStruct().setQuoteKeys( true );
		original.getFunction().getParameters().setCommaDangle( true );
		original.getBraces().setStyle( "new-line" );

		Config clone = original.clone();

		assertEquals( original.toJSON(), clone.toJSON() );
	}
}
