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

	@DisplayName( "Can snake_case a string" )
	@Test
	void testSnakeCase() {
		// camelCase → snake_case
		assertThat( StringUtil.snakeCase( "myVariable" ) ).isEqualTo( "my_variable" );
		assertThat( StringUtil.snakeCase( "thisIsATest" ) ).isEqualTo( "this_is_a_test" );
		assertThat( StringUtil.snakeCase( "parseXML" ) ).isEqualTo( "parse_xml" );

		// PascalCase → snake_case
		assertThat( StringUtil.snakeCase( "MyClass" ) ).isEqualTo( "my_class" );
		assertThat( StringUtil.snakeCase( "HelloWorld" ) ).isEqualTo( "hello_world" );
		assertThat( StringUtil.snakeCase( "XMLParser" ) ).isEqualTo( "xml_parser" );

		// kebab-case → snake_case
		assertThat( StringUtil.snakeCase( "my-variable" ) ).isEqualTo( "my_variable" );
		assertThat( StringUtil.snakeCase( "hello-world-test" ) ).isEqualTo( "hello_world_test" );

		// Spaces → snake_case
		assertThat( StringUtil.snakeCase( "my variable" ) ).isEqualTo( "my_variable" );
		assertThat( StringUtil.snakeCase( "hello world" ) ).isEqualTo( "hello_world" );
		assertThat( StringUtil.snakeCase( "this is a   test" ) ).isEqualTo( "this_is_a_test" );

		// Already snake_case (idempotent)
		assertThat( StringUtil.snakeCase( "my_variable" ) ).isEqualTo( "my_variable" );
		assertThat( StringUtil.snakeCase( "already_snake_case" ) ).isEqualTo( "already_snake_case" );

		// ALLCAPS → single word lowercased
		assertThat( StringUtil.snakeCase( "ALLCAPS" ) ).isEqualTo( "allcaps" );
		assertThat( StringUtil.snakeCase( "XML" ) ).isEqualTo( "xml" );

		// Complex acronym boundaries
		assertThat( StringUtil.snakeCase( "parseXMLHTTPRequest" ) ).isEqualTo( "parse_xmlhttp_request" );
		assertThat( StringUtil.snakeCase( "XMLHTTPRequest" ) ).isEqualTo( "xmlhttp_request" );

		// Numbers
		assertThat( StringUtil.snakeCase( "myVar2" ) ).isEqualTo( "my_var2" );
		assertThat( StringUtil.snakeCase( "test2Var" ) ).isEqualTo( "test2_var" );
		assertThat( StringUtil.snakeCase( "JSON2XML" ) ).isEqualTo( "json2_xml" );

		// Mixed separators
		assertThat( StringUtil.snakeCase( "my-variable_name" ) ).isEqualTo( "my_variable_name" );
		assertThat( StringUtil.snakeCase( "hello-world test" ) ).isEqualTo( "hello_world_test" );

		// Special characters → underscores
		assertThat( StringUtil.snakeCase( "hello@world!" ) ).isEqualTo( "hello_world" );
		assertThat( StringUtil.snakeCase( "price$amount" ) ).isEqualTo( "price_amount" );

		// Leading/trailing whitespace and special chars
		assertThat( StringUtil.snakeCase( " hello " ) ).isEqualTo( "hello" );
		assertThat( StringUtil.snakeCase( "  hello  world  " ) ).isEqualTo( "hello_world" );

		// Empty and edge cases
		assertThat( StringUtil.snakeCase( "" ) ).isEqualTo( "" );
		assertThat( StringUtil.snakeCase( "_" ) ).isEqualTo( "" );
		assertThat( StringUtil.snakeCase( "___" ) ).isEqualTo( "" );
	}

	@DisplayName( "Can PascalCase a string" )
	@Test
	void testPascalCase() {
		// camelCase → PascalCase
		assertThat( StringUtil.pascalCase( "myVariable" ) ).isEqualTo( "MyVariable" );
		assertThat( StringUtil.pascalCase( "thisIsATest" ) ).isEqualTo( "ThisIsATest" );
		assertThat( StringUtil.pascalCase( "parseXML" ) ).isEqualTo( "ParseXml" );

		// snake_case → PascalCase
		assertThat( StringUtil.pascalCase( "my_variable" ) ).isEqualTo( "MyVariable" );
		assertThat( StringUtil.pascalCase( "hello_world" ) ).isEqualTo( "HelloWorld" );
		assertThat( StringUtil.pascalCase( "xml_parser" ) ).isEqualTo( "XmlParser" );

		// kebab-case → PascalCase
		assertThat( StringUtil.pascalCase( "my-variable" ) ).isEqualTo( "MyVariable" );
		assertThat( StringUtil.pascalCase( "hello-world-test" ) ).isEqualTo( "HelloWorldTest" );

		// Spaces → PascalCase
		assertThat( StringUtil.pascalCase( "my variable" ) ).isEqualTo( "MyVariable" );
		assertThat( StringUtil.pascalCase( "hello world" ) ).isEqualTo( "HelloWorld" );
		assertThat( StringUtil.pascalCase( "this is a   test" ) ).isEqualTo( "ThisIsATest" );

		// Already PascalCase (idempotent)
		assertThat( StringUtil.pascalCase( "MyClass" ) ).isEqualTo( "MyClass" );
		assertThat( StringUtil.pascalCase( "HelloWorld" ) ).isEqualTo( "HelloWorld" );
		assertThat( StringUtil.pascalCase( "MyVariable" ) ).isEqualTo( "MyVariable" );

		// ALLCAPS → title-cased single word
		assertThat( StringUtil.pascalCase( "ALLCAPS" ) ).isEqualTo( "Allcaps" );
		assertThat( StringUtil.pascalCase( "XML" ) ).isEqualTo( "Xml" );

		// Complex acronym boundaries
		assertThat( StringUtil.pascalCase( "parseXMLHTTPRequest" ) ).isEqualTo( "ParseXmlhttpRequest" );
		assertThat( StringUtil.pascalCase( "XMLHTTPRequest" ) ).isEqualTo( "XmlhttpRequest" );
		assertThat( StringUtil.pascalCase( "XMLParser" ) ).isEqualTo( "XmlParser" );

		// Numbers
		assertThat( StringUtil.pascalCase( "myVar2" ) ).isEqualTo( "MyVar2" );
		assertThat( StringUtil.pascalCase( "test2Var" ) ).isEqualTo( "Test2Var" );
		assertThat( StringUtil.pascalCase( "JSON2XML" ) ).isEqualTo( "Json2Xml" );

		// Mixed separators
		assertThat( StringUtil.pascalCase( "my-variable_name" ) ).isEqualTo( "MyVariableName" );
		assertThat( StringUtil.pascalCase( "hello-world test" ) ).isEqualTo( "HelloWorldTest" );

		// Special characters → word separators
		assertThat( StringUtil.pascalCase( "hello@world!" ) ).isEqualTo( "HelloWorld" );
		assertThat( StringUtil.pascalCase( "price$amount" ) ).isEqualTo( "PriceAmount" );

		// Leading/trailing whitespace and special chars
		assertThat( StringUtil.pascalCase( " hello " ) ).isEqualTo( "Hello" );
		assertThat( StringUtil.pascalCase( "  hello  world  " ) ).isEqualTo( "HelloWorld" );

		// Empty and edge cases
		assertThat( StringUtil.pascalCase( "" ) ).isEqualTo( "" );
		assertThat( StringUtil.pascalCase( "_" ) ).isEqualTo( "" );
		assertThat( StringUtil.pascalCase( "___" ) ).isEqualTo( "" );
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
