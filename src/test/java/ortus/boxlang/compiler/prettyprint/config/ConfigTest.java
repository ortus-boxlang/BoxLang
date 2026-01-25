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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "Config Tests" )
public class ConfigTest {

	@Test
	@DisplayName( "Config has correct defaults" )
	public void testDefaults() {
		Config config = new Config();

		assertEquals( 4, config.getIndentSize() );
		assertTrue( config.getTabIndent() );
		assertEquals( 80, config.getMaxLineLength() );
		assertEquals( "os", config.getNewLine() );
		assertFalse( config.getSingleQuote() );
		assertFalse( config.getBracketPadding() );
		assertFalse( config.getParensPadding() );
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
		Config config = new Config();
		Map<String, Object> map = config.toMap();

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
		assertEquals( 80, config.getMaxLineLength() );
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

		// Default is "os"
		assertEquals( System.lineSeparator(), config.lineSeparator() );

		config.setNewLine( "\n" );
		assertEquals( "\n", config.lineSeparator() );

		config.setNewLine( "\r\n" );
		assertEquals( "\r\n", config.lineSeparator() );
	}
}
