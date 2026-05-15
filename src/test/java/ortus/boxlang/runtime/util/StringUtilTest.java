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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.util.StringUtil;

public class StringUtilTest {

	@DisplayName( "Test create slug" )
	@Test
	void testSlug() {
		String slug = StringUtil.slugify( "This is a   test", 10, "" );
		assertThat( slug ).isEqualTo( "this-is-a-" );
	}

	@DisplayName( "Test create slug with special characters" )
	@Test
	void testSlugWithSpecialChars() {
		String slug = StringUtil.slugify( "This is ä ü test ß" );
		assertThat( slug ).isEqualTo( "this-is-a-u-test-ss" );
	}

	@DisplayName( "Can pretty print sql" )
	@Test
	void testPrettyPrintSql() {
		String	sql			= "SELECT (count(*) as size) FROM table WHERE id=1 and name='test' OR (id=2 and name='test2')";
		String	prettySql	= StringUtil.prettySql( sql );
		assertThat( prettySql ).isNotEmpty();
	}

	@DisplayName( "Can CamelCase a string" )
	@Test
	void testCamelCase() {
		String camelCase = StringUtil.camelCase( "this is a test" );
		assertThat( camelCase ).isEqualTo( "thisIsATest" );
	}

	@DisplayName( "Can singularize a string" )
	@Test
	void testSingularize() {
		String singular = StringUtil.singularize( "tests" );
		assertThat( singular ).isEqualTo( "test" );
	}

	@DisplayName( "Can pluralize a string" )
	@Test
	void testPluralize() {
		// Regular singular → plural
		assertThat( StringUtil.pluralize( "test" ) ).isEqualTo( "tests" );
		assertThat( StringUtil.pluralize( "minute" ) ).isEqualTo( "minutes" );
		assertThat( StringUtil.pluralize( "second" ) ).isEqualTo( "seconds" );
		assertThat( StringUtil.pluralize( "day" ) ).isEqualTo( "days" );
		assertThat( StringUtil.pluralize( "hour" ) ).isEqualTo( "hours" );

		// Already plural → unchanged (idempotent)
		assertThat( StringUtil.pluralize( "tests" ) ).isEqualTo( "tests" );
		assertThat( StringUtil.pluralize( "minutes" ) ).isEqualTo( "minutes" );
		assertThat( StringUtil.pluralize( "seconds" ) ).isEqualTo( "seconds" );
		assertThat( StringUtil.pluralize( "days" ) ).isEqualTo( "days" );
		assertThat( StringUtil.pluralize( "hours" ) ).isEqualTo( "hours" );

		// Words ending in "ss" → "sses"
		assertThat( StringUtil.pluralize( "class" ) ).isEqualTo( "classes" );
		// Already plural "classes" → unchanged (idempotent)
		assertThat( StringUtil.pluralize( "classes" ) ).isEqualTo( "classes" );

		// Words ending in "us" → "uses"
		assertThat( StringUtil.pluralize( "cactus" ) ).isEqualTo( "cactuses" );

		// Words ending in "y" → "ies" (consonant + y)
		assertThat( StringUtil.pluralize( "category" ) ).isEqualTo( "categories" );

		// Words ending in vowel + "y" → "ys"
		assertThat( StringUtil.pluralize( "day" ) ).isEqualTo( "days" );
		assertThat( StringUtil.pluralize( "key" ) ).isEqualTo( "keys" );

		// Words ending in "x", "z", "ch", "sh" → "es"
		assertThat( StringUtil.pluralize( "box" ) ).isEqualTo( "boxes" );
		assertThat( StringUtil.pluralize( "quiz" ) ).isEqualTo( "quizes" );
		assertThat( StringUtil.pluralize( "church" ) ).isEqualTo( "churches" );
		assertThat( StringUtil.pluralize( "dish" ) ).isEqualTo( "dishes" );
	}

}
