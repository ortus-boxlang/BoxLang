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

	private StringUtil stringUtil;

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
		String plural = StringUtil.pluralize( "test" );
		assertThat( plural ).isEqualTo( "tests" );
	}

}
