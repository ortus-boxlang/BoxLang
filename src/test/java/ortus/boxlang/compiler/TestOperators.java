package ortus.boxlang.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
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

import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.JavaTranspiler;

@Disabled
public class TestOperators extends TestBase {

	@Test
	public void concat() throws IOException {
		String			expression	= """
		                              			"Hello " & "world";
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Concat.invoke(\"Hello \", \"world\")", javaAST.toString() );
	}

	@Test
	public void plus() throws IOException {
		String			expression	= """
		                              			1 + 2
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Plus.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void minus() throws IOException {
		String			expression	= """
		                              			1 - 2
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Minus.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void star() throws IOException {
		String			expression	= """
		                              			1 * 2
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Multiply.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void power() throws IOException {
		String			expression	= """
		                              			1 ^ 2
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Power.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void slash() throws IOException {
		String			expression	= """
		                              			1 / 2
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Divide.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void backslash() throws IOException {
		String			expression	= """
		                              			1 \\ 2
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "IntegerDivide.invoke(1, 2)", javaAST.toString() );

	}

	@Test
	public void conatins() throws IOException {
		String			expression	= """
		                              			"Brad Wood" contains "Wood"
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Contains.invoke(\"Brad Wood\", \"Wood\")", javaAST.toString() );

	}

	@Test
	public void doesNotContains() throws IOException {
		String			expression	= """
		                              			"Brad Wood" does not contain "Luis"
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "!Contains.invoke(\"Brad Wood\", \"Luis\")", javaAST.toString() );

	}

	@Test
	public void negate() throws IOException {
		String			expression	= """
		                              			-5
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Negate.invoke(5)", javaAST.toString() );

	}

	@Test
	public void not() throws IOException {
		String			expression	= """
		                              			!True
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Not.invoke(true)", javaAST.toString() );

	}

	@Test
	public void notNot() throws IOException {
		String			expression	= """
		                              			!!False
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Not.invoke(Not.invoke(false))", javaAST.toString() );

	}

	@Test
	public void ternary() throws IOException {
		String			expression	= """
		                              			isGood ? "eat" : "toss"
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// isGood is not on the left hand side of an elvis or save navigation operator, so it should not be providing a default scope. We want it to error if
		// not found.
		assertEquals( "Ternary.invoke(context.scopeFindNearby(Key.of(\"isGood\"), null	).value(), \"eat\", \"toss\")",
		    javaAST.toString() );

	}

	@Test
	public void referenceToVariablesScope() throws IOException {
		String			expression	= """
		                              			variables['system']
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// Explicit use of variables scope should be direcfty referencing the variables scope. No need to search for it.
		assertEquals( "variablesScope.dereference(Key.of(\"system\"), false)", javaAST.toString() );

	}

	@Test
	public void referenceToIdentifier() throws IOException {
		String			expression	= """
		                              			foo
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// TODO: foo is getting returned direclty instead of searching the scopes for it
		assertEqualsNoWhiteSpaces( "context.scopeFindNearby(Key.of(\"foo\"),null).value()", javaAST.toString() );

	}

	@Test
	public void elvis() throws IOException {
		String			expression	= """
		                              			variables.maybeNull ?: "use if null"
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		// TODO: Since we're dereferencing on the left hand side of the elvis operator, we must pass "true" to the safe param of the dereference method
		// I am using objectAccess.isSafe(), but it's returning false instaed of true!
		assertEqualsNoWhiteSpaces(
		    "Elvis.invoke(variablesScope.dereference(Key.of(\"maybeNull\"),true),\"useifnull\")", javaAST.toString() );

	}

	@Test
	public void testElvisLeftDereferencing() throws IOException {
		String			expression	= """
		                              			variables.foo.bar ?: "brad"
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// TODO: Explicit variables scope access should not be using scopeFindNearby().
		// Also, All dereferencing on the left hand side of an elvis operator must be done safely
		assertEqualsNoWhiteSpaces(
		    """
		    Elvis.invoke(Referencer.get(variablesScope.dereference(Key.of("foo"),true),Key.of("bar"),true),"brad")
		                              """,
		    javaAST.toString()
		);

	}

	@Test
	public void xor() throws IOException {
		String			expression	= """
		                              			2 XOR 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "XOR.invoke(2, 3)", javaAST.toString() );

	}

	@Test
	public void mod() throws IOException {
		String			expression	= """
		                              			2 MOD 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Modulus.invoke(2, 3)", javaAST.toString() );

	}

	@Test
	public void instanceOf() throws IOException {
		String			expression	= """
		                              foo instanceOf "String"
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// We pass null as the default scope since we are not on the left hand side of an elvis or safe navigation operator. We also use .value() directly.
		assertEqualsNoWhiteSpaces(
		    """
		          InstanceOf.invoke(context, context.scopeFindNearby(Key.of("foo"), null).value(), "String")
		    """,
		    javaAST.toString() );

	}

	@Test
	public void parenthesis() throws IOException {
		String			expression	= """
		                              (1+ 2) * 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Multiply.invoke((Plus.invoke(1, 2)), 3)", javaAST.toString() );

	}

	@Test
	public void equalEqual() throws IOException {
		String			expression	= """
		                              1 == 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "EqualsEquals.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 EQ 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "EqualsEquals.invoke(1, 3)", javaAST.toString() );
		expression	= """
		              1 IS 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "EqualsEquals.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void equalEqualEqual() throws IOException {
		String			expression	= """
		                              true === "true"
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "EqualsEqualsEquals.invoke(true, \"true\")", javaAST.toString() );
	}

	@Test
	public void notEqual() throws IOException {
		String			expression	= """
		                              1 != 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "!EqualsEquals.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 NEQ 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "!EqualsEquals.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void greaterThan() throws IOException {
		String			expression	= """
		                              1 > 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "GreaterThan.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 GT 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "GreaterThan.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void greaterThanEqual() throws IOException {
		String			expression	= """
		                              1 >= 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "GreaterThanEqual.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 GTE 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "GreaterThanEqual.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void lessThan() throws IOException {
		String			expression	= """
		                              1 < 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "LessThan.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 LT 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "LessThan.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void lessThanEqual() throws IOException {
		String			expression	= """
		                              1 <= 3
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "LessThanEqual.invoke(1, 3)", javaAST.toString() );

		expression	= """
		              1 LTE 3
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "LessThanEqual.invoke(1, 3)", javaAST.toString() );

	}

	@Test
	public void and() throws IOException {
		String			expression	= """
		                              true && "true"
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "And.invoke(true, \"true\")", javaAST.toString() );

		expression	= """
		              true AND "true"
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "And.invoke(true, \"true\")", javaAST.toString() );

	}

	@Test
	public void or() throws IOException {
		String			expression	= """
		                              true || "true"
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		assertEquals( "Or.invoke(true, \"true\")", javaAST.toString() );

		expression	= """
		              true OR "true"
		              """;
		result		= parseExpression( expression );

		javaAST		= new JavaTranspiler().transform( result.getRoot() );
		assertEquals( "Or.invoke(true, \"true\")", javaAST.toString() );

	}

	@Test
	public void postIncrement() throws IOException {
		String			expression	= """
		                              variables['a']++
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Explicit reference to variables scope should directly reference it
		assertEqualsNoWhiteSpaces(
		    """
		    	Increment.invokePost(variablesScope,Key.of("a"))
		    """, javaAST.toString() );
	}

	@Test
	public void preIncrement() throws IOException {
		String			expression	= """
		                              ++variables['a']
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Explicit reference to variables scope should directly reference it
		assertEqualsNoWhiteSpaces(
		    """
		    	Increment.invokePre(variablesScope,Key.of("a"))
		    """, javaAST.toString() );
	}

	@Test
	public void postDecrement() throws IOException {
		String			expression	= """
		                              variables['a']--
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Explicit reference to variables scope should directly reference it
		assertEqualsNoWhiteSpaces(
		    """
		    	Decrement.invokePost(variablesScope,Key.of("a"))
		    """, javaAST.toString() );
	}

	@Test
	public void preDecrement() throws IOException {
		String			expression	= """
		                              --variables['a']
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Explicit reference to variables scope should directly reference it
		assertEqualsNoWhiteSpaces(
		    """
		    	Decrement.invokePre(variablesScope,Key.of("a"))
		    """, javaAST.toString() );
	}

	@Test
	public void interpolation() throws IOException {
		String			expression	= """
		                              "a is #variables.a# and b is #variables.b#"
		                              """;

		ParsingResult	result		= parseExpression( expression );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		       Concat.invoke(
		    	"ais",
		    	Concat.invoke(
		    		variablesScope.dereference(Key.of("a"),false),
		    		Concat.invoke(
		    			"andbis",
		    			variablesScope.dereference(Key.of("b"),false)
		    		)
		    	)
		    )
		          """,
		    javaAST.toString() );
	}

	@Test
	public void new_() throws IOException {
		String			statement	= """
		                              new java:java.lang.RuntimeException( "My Message" );
		                              """;

		ParsingResult	result		= parseExpression( statement );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Need to pass the full class name and resolver prefix
		assertEqualsNoWhiteSpaces(
		    """
		    classLocator.load(context,(String)"java:java.lang.RuntimeException",imports).invokeConstructor(newObject[]{"MyMessage"})
		      	""", javaAST.toString() );
	}

	@Test
	public void castAS() throws IOException {
		String			expression	= """
		                              			5 castAs sdf;
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// We're not assigning sdf, so we don't need to pass a default scope. Also, we can use value() directly
		assertEquals( "CastAs.invoke(5, context.scopeFindNearby(Key.of(\"sdf\"), null).value())",
		    javaAST.toString() );
	}

	@Test
	public void hash() throws IOException {
		String			expression	= """
		                              			a & #aaa#
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// We're not assigning a or aaa, so we don't need to pass a default scope. Also, we can use value() directly
		assertEqualsNoWhiteSpaces(
		    "Concat.invoke(context.scopeFindNearby(Key.of(\"a\"), null).value(), context.scopeFindNearby(Key.of(\"aaa\"), null).value())",
		    javaAST.toString() );
	}

	@Test
	public void assigmentExpression() throws IOException {
		String			expression	= """
		                              			 5 * ( myNum = 5 )
		                              """;

		ParsingResult	result		= parseExpression( expression );
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

	}
}
