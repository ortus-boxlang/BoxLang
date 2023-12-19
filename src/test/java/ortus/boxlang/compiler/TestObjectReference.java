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
package ortus.boxlang.compiler;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import com.github.javaparser.ast.Node;

import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.JavaTranspiler;

@Disabled
public class TestObjectReference extends TestBase {

	@Test
	public void testDereferenceByKey() throws IOException {
		String			expression	= """
		                              			a
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    "context.scopeFindNearby( Key.of( \"a\" ), null ).value()",
		    javaAST.toString() );
	}

	@Test
	public void testDereferenceByKeyAsDictionary() throws IOException {
		String			expression	= """
		                              			foo["bar"]
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    "Referencer.get( context.scopeFindNearby( Key.of( \"foo\" ), null ).value(), Key.of( \"bar\" ), false )",
		    javaAST.toString() );
	}

	@Test
	public void testDereferenceByKeyAsDictionaryWithSingleQuotes() throws IOException {
		String			expression	= """
		                              			foo['bar']
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    "Referencer.get( context.scopeFindNearby( Key.of( \"foo\" ), null ).value(), Key.of( \"bar\" ), false )",
		    javaAST.toString() );
	}

	@Test
	public void testDereferenceByPeriod() throws IOException {
		String			expression	= """
		                              			foo.bar
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		/**
		 * Note, it is not necessary to use .scope().dereference(Key.of(\"foo\"),false) since the scope fine result already contains the value.
		 * Also, null should be passed as the default scope since we are not on the left hand side of an elvis operator or save navigation operator.
		 * This code should be the exact same as the two tests above it. There is no difference between the two except FOO should actually be upper case here
		 * but we can deal with that part later.
		 */
		assertEqualsNoWhiteSpaces(
		    "Referencer.get(context.scopeFindNearby(Key.of(\"foo\"),null).value(),Key.of(\"bar\"),false)",
		    javaAST.toString() );
	}

	@Test
	public void testSafeDereferenceByPeriod() throws IOException {
		String			expression	= """
		                              			foo?.bar
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    "Referencer.get(context.scopeFindNearby(Key.of(\"foo\"),context.getDefaultAssignmentScope()).value(),Key.of(\"bar\"),true)",
		    javaAST.toString() );
	}

	@Test
	public void testDereferenceByKeyFromKnownScope() throws IOException {
		String			expression	= """
		                              			variables.foo
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    "variablesScope.dereference(Key.of(\"foo\"),false)",
		    javaAST.toString() );
	}

	@Test
	public void testSafeDereferenceByKeyFromKnownScope() throws IOException {
		String			expression	= """
		                              			variables?.foo
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    "variablesScope.dereference(Key.of(\"foo\"),false)",
		    javaAST.toString() );
	}

	@Test
	public void knownScopeAssignment() throws IOException {
		String			expression	= """
		                              			variables.foo=bar
		                              """;

		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parseStatement( expression );
		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );

		// TODO: we're generating extra {} braces around the code. Not sure if that is correct.
		assertEqualsNoWhiteSpaces( """
		                           variablesScope.assign(Key.of("foo"),bar);
		                                                       """, javaAST.toString() );
	}

}
