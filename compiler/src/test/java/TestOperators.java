
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
import org.junit.Ignore;
import org.junit.Test;
import ortus.boxlang.parser.BoxLangParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.BoxLangTranspiler;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestOperators extends TestBase {

	@Test
	public void concat() throws IOException {
		String				expression	= """
		                                  			"Hello " & "world";
		                                  """;

		BoxLangParser		parser		= new BoxLangParser();
		BoxLangTranspiler	transpiler	= new BoxLangTranspiler();
		ParsingResult		result		= parser.parseExpression( expression );

		Node				javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Concat.invoke(\"Hello \", \"world\")", javaAST.toString() );

	}

	@Test
	public void plus() throws IOException {
		String			expression	= """
		                              			1 + 2
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Plus.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void minus() throws IOException {
		String			expression	= """
		                              			1 - 2
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Minus.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void star() throws IOException {
		String			expression	= """
		                              			1 * 2
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Multiply.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void power() throws IOException {
		String			expression	= """
		                              			1 ^ 2
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Power.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void slash() throws IOException {
		String			expression	= """
		                              			1 / 2
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Divide.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void backslash() throws IOException {
		String			expression	= """
		                              			1 \\ 2
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "IntegerDivide.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void conatins() throws IOException {
		String			expression	= """
		                              			"Brad Wood" contains "Wood"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Contains.contains(\"Brad Wood\", \"Wood\")", javaAST.toString() );

	}

	@Test
	public void doesNotContains() throws IOException {
		String			expression	= """
		                              			"Brad Wood" does not contain "Luis"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "!Contains.contains(\"Brad Wood\", \"Luis\")", javaAST.toString() );

	}

	@Test
	public void negate() throws IOException {
		String			expression	= """
		                              			!True
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Negate.invoke(\"True\")", javaAST.toString() );

	}

	@Test
	public void negateNegate() throws IOException {
		String			expression	= """
		                              			!!False
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Negate.invoke(Negate.invoke(\"False\"))", javaAST.toString() );

	}

	@Test
	public void ternary() throws IOException {
		String			expression	= """
		                              			isGood ? "eat" : "toss"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Ternary.invoke(isGood, \"eat\", \"toss\")", javaAST.toString() );

	}

	@Test
	public void referenceToVariablesScope() throws IOException {
		String			expression	= """
		                              			variables['system']
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "variablesScope.get(Key.of(\"system\"))", javaAST.toString() );

	}

	@Test
	@Ignore
	public void referenceToIdentifier() throws IOException {
		String			expression	= """
		                              			foo
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "context.scopeFindNearby(Key.of(\"foo\"))", javaAST.toString() );

	}

	@Test
	public void elvis() throws IOException {
		String			expression	= """
		                              			variables.maybeNull ?: "use if null"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Elvis.invoke(variablesScope.get(Key.of(\"maybeNull\")), \"use if null\")", javaAST.toString() );

	}

	@Test
	@Ignore
	public void testElvisLeftDereferencing() throws IOException {
		String			expression	= """
		                              			variables.foo.bar ?: "brad"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEqualsNoWhiteSpaces( """
		                           Elvis.invoke(
		                           	Referencer.get(
		                           		variablesScope
		                           		  .get(
		                           		    Key.of( "foo" ),
		                           		    true
		                           		  ),
		                           			Key.of( "bar" ),
		                           		  true
		                           	),
		                             "brad"
		                           )""".replaceAll( "[ \\r\\n\\t]", "" ), javaAST.toString().replaceAll( "[ \\t\\r\\n]", "" ) );

	}

	@Test
	public void xor() throws IOException {
		String			expression	= """
		                              			2 XOR 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Xor.invoke(2, 3)", javaAST.toString() );

	}

	@Test
	public void mod() throws IOException {
		String			expression	= """
		                              			2 MOD 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Mod.invoke(2, 3)", javaAST.toString() );

	}

	@Test
	public void instanceOf() throws IOException {
		String			expression	= """
		                              foo instanceOf "String"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "InstanceOf.invoke(context.scopeFindNearby(Key.of(\"foo\")), \"String\")", javaAST.toString() );

	}

	@Test
	public void parenthesis() throws IOException {
		String			expression	= """
		                              (1+ 2) * 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Multiply.invoke((Plus.invoke(1, 2)), 3)", javaAST.toString() );

	}

	@Test
	public void equalEqual() throws IOException {
		String			expression	= """
		                              1 == 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "EqualsEquals.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 EQ 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "EqualsEquals.invoke(1, 3)", javaAST.toString() );
		expression	= """
		              1 IS 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "EqualsEquals.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void equalEqualEqual() throws IOException {
		String			expression	= """
		                              true === "true"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "EqualsEqualsEquals.invoke(true, \"true\")", javaAST.toString() );
	}

	@Test
	public void notEqual() throws IOException {
		String			expression	= """
		                              1 != 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "!EqualsEquals.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 NEQ 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "!EqualsEquals.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void greaterThan() throws IOException {
		String			expression	= """
		                              1 > 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "GreaterThan.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 GT 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "GreaterThan.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void greaterThanEqual() throws IOException {
		String			expression	= """
		                              1 >= 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "GreaterThanEqual.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 GTE 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "GreaterThanEqual.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void lessThan() throws IOException {
		String			expression	= """
		                              1 < 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "LessThan.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 LT 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "LessThan.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void lessThanEqual() throws IOException {
		String			expression	= """
		                              1 <= 3
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "LessThanEqual.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 LTE 3
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "LessThanEqual.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void and() throws IOException {
		String			expression	= """
		                              true && "true"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "And.invoke(true, \"true\")", javaAST.toString() );

		expression	= """
		              true AND "true"
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "And.invoke(true, \"true\")", javaAST.toString() );

	}

	@Test
	public void or() throws IOException {
		String			expression	= """
		                              true || "true"
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "Or.invoke(true, \"true\")", javaAST.toString() );

		expression	= """
		              true OR "true"
		              """;
		result		= parser.parseExpression( expression );

		javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "Or.invoke(true, \"true\")", javaAST.toString() );

	}

	@Test
	public void postIncrement() throws IOException {
		String			expression	= """
		                              variables['a']++
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    	Increment.invokePost(variablesScope.get(Key.of("a")))
		    """, javaAST.toString() );
	}

	@Test
	public void preIncrement() throws IOException {
		String			expression	= """
		                              ++variables['a']
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    	Increment.invokePre(variablesScope.get(Key.of("a")))
		    """, javaAST.toString() );
	}

	@Test
	public void postDecrement() throws IOException {
		String			expression	= """
		                              variables['a']--
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    	Decrement.invokePost(variablesScope.get(Key.of("a")))
		    """, javaAST.toString() );
	}

	@Test
	public void preDecrement() throws IOException {
		String			expression	= """
		                              --variables['a']
		                              """;

		BoxLangParser	parser		= new BoxLangParser();
		ParsingResult	result		= parser.parseExpression( expression );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    	Decrement.invokePre(variablesScope.get(Key.of("a")))
		    """, javaAST.toString() );
	}
}
