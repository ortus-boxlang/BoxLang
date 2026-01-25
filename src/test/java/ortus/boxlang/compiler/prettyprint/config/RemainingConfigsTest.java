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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName( "Remaining Config Classes Tests" )
public class RemainingConfigsTest {

	@Nested
	@DisplayName( "ImportConfig Tests" )
	class ImportConfigTests {

		@Test
		@DisplayName( "ImportConfig has correct defaults" )
		public void testDefaults() {
			ImportConfig config = new ImportConfig();

			assertFalse( config.getSort() );
			assertFalse( config.getGroup() );
		}

		@Test
		@DisplayName( "ImportConfig setters work correctly" )
		public void testSetters() {
			ImportConfig config = new ImportConfig();

			config.setSort( true );
			assertTrue( config.getSort() );

			config.setGroup( true );
			assertTrue( config.getGroup() );
		}

		@Test
		@DisplayName( "ImportConfig toMap produces correct structure" )
		public void testToMap() {
			ImportConfig config = new ImportConfig();
			config.setSort( true );
			config.setGroup( true );

			Map<String, Object> map = config.toMap();

			assertEquals( true, map.get( "sort" ) );
			assertEquals( true, map.get( "group" ) );
		}

		@Test
		@DisplayName( "Config loads ImportConfig from map" )
		public void testConfigLoadFromMap() {
			Map<String, Object> importMap = new HashMap<>();
			importMap.put( "sort", true );
			importMap.put( "group", true );

			Map<String, Object> configMap = new HashMap<>();
			configMap.put( "import", importMap );

			Config config = new Config().loadFromConfig( configMap );

			assertTrue( config.getImportConfig().getSort() );
			assertTrue( config.getImportConfig().getGroup() );
		}
	}

	@Nested
	@DisplayName( "CommentsConfig Tests" )
	class CommentsConfigTests {

		@Test
		@DisplayName( "CommentsConfig has correct defaults" )
		public void testDefaults() {
			CommentsConfig config = new CommentsConfig();

			assertTrue( config.getPreserveBlankLines() );
			assertFalse( config.getWrap() );
		}

		@Test
		@DisplayName( "CommentsConfig setters work correctly" )
		public void testSetters() {
			CommentsConfig config = new CommentsConfig();

			config.setPreserveBlankLines( false );
			assertFalse( config.getPreserveBlankLines() );

			config.setWrap( true );
			assertTrue( config.getWrap() );
		}

		@Test
		@DisplayName( "CommentsConfig toMap produces correct structure" )
		public void testToMap() {
			CommentsConfig config = new CommentsConfig();
			config.setPreserveBlankLines( false );
			config.setWrap( true );

			Map<String, Object> map = config.toMap();

			assertEquals( false, map.get( "preserve_blank_lines" ) );
			assertEquals( true, map.get( "wrap" ) );
		}

		@Test
		@DisplayName( "Config loads CommentsConfig from map" )
		public void testConfigLoadFromMap() {
			Map<String, Object> commentsMap = new HashMap<>();
			commentsMap.put( "preserve_blank_lines", false );
			commentsMap.put( "wrap", true );

			Map<String, Object> configMap = new HashMap<>();
			configMap.put( "comments", commentsMap );

			Config config = new Config().loadFromConfig( configMap );

			assertFalse( config.getComments().getPreserveBlankLines() );
			assertTrue( config.getComments().getWrap() );
		}
	}

	@Nested
	@DisplayName( "ClassConfig Tests" )
	class ClassConfigTests {

		@Test
		@DisplayName( "ClassConfig has correct defaults" )
		public void testDefaults() {
			ClassConfig config = new ClassConfig();

			assertEquals( "preserve", config.getMemberOrder() );
			assertEquals( 1, config.getMemberSpacing() );
		}

		@Test
		@DisplayName( "ClassConfig setters work correctly" )
		public void testSetters() {
			ClassConfig config = new ClassConfig();

			config.setMemberOrder( "properties-first" );
			assertEquals( "properties-first", config.getMemberOrder() );

			config.setMemberSpacing( 2 );
			assertEquals( 2, config.getMemberSpacing() );
		}

		@Test
		@DisplayName( "ClassConfig toMap produces correct structure" )
		public void testToMap() {
			ClassConfig config = new ClassConfig();
			config.setMemberOrder( "methods-first" );
			config.setMemberSpacing( 2 );

			Map<String, Object> map = config.toMap();

			assertEquals( "methods-first", map.get( "member_order" ) );
			assertEquals( 2, map.get( "member_spacing" ) );
		}

		@Test
		@DisplayName( "Config loads ClassConfig from map" )
		public void testConfigLoadFromMap() {
			Map<String, Object> classMap = new HashMap<>();
			classMap.put( "member_order", "properties-first" );
			classMap.put( "member_spacing", 2 );

			Map<String, Object> configMap = new HashMap<>();
			configMap.put( "class", classMap );

			Config config = new Config().loadFromConfig( configMap );

			assertEquals( "properties-first", config.getClassConfig().getMemberOrder() );
			assertEquals( 2, config.getClassConfig().getMemberSpacing() );
		}
	}

	@Nested
	@DisplayName( "SqlConfig Tests" )
	class SqlConfigTests {

		@Test
		@DisplayName( "SqlConfig has correct defaults" )
		public void testDefaults() {
			SqlConfig config = new SqlConfig();

			assertTrue( config.getUppercaseKeywords() );
			assertTrue( config.getIndentClauses() );
		}

		@Test
		@DisplayName( "SqlConfig setters work correctly" )
		public void testSetters() {
			SqlConfig config = new SqlConfig();

			config.setUppercaseKeywords( false );
			assertFalse( config.getUppercaseKeywords() );

			config.setIndentClauses( false );
			assertFalse( config.getIndentClauses() );
		}

		@Test
		@DisplayName( "SqlConfig toMap produces correct structure" )
		public void testToMap() {
			SqlConfig config = new SqlConfig();
			config.setUppercaseKeywords( false );
			config.setIndentClauses( false );

			Map<String, Object> map = config.toMap();

			assertEquals( false, map.get( "uppercase_keywords" ) );
			assertEquals( false, map.get( "indent_clauses" ) );
		}

		@Test
		@DisplayName( "Config loads SqlConfig from map" )
		public void testConfigLoadFromMap() {
			Map<String, Object> sqlMap = new HashMap<>();
			sqlMap.put( "uppercase_keywords", false );
			sqlMap.put( "indent_clauses", false );

			Map<String, Object> configMap = new HashMap<>();
			configMap.put( "sql", sqlMap );

			Config config = new Config().loadFromConfig( configMap );

			assertFalse( config.getSql().getUppercaseKeywords() );
			assertFalse( config.getSql().getIndentClauses() );
		}
	}

	@Test
	@DisplayName( "Config toMap includes all new config objects" )
	public void testConfigToMapIncludesAll() {
		Config config = new Config();
		Map<String, Object> map = config.toMap();

		assertNotNull( map.get( "arguments" ) );
		assertNotNull( map.get( "braces" ) );
		assertNotNull( map.get( "operators" ) );
		assertNotNull( map.get( "chain" ) );
		assertNotNull( map.get( "template" ) );
		assertNotNull( map.get( "import" ) );
		assertNotNull( map.get( "comments" ) );
		assertNotNull( map.get( "class" ) );
		assertNotNull( map.get( "sql" ) );
	}
}
