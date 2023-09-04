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
import org.junit.Test;
import ourtus.boxlang.parser.BoxLangParser;
import ourtus.boxlang.parser.ParsingResult;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestOperators {

	@Test
	public void testConcat() throws IOException {
		String expression = """
   			"Hello " & "world";
			""";

		BoxLangParser parser = new BoxLangParser();
		BoxLangTranspiler transpiler = new BoxLangTranspiler();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Concat.invoke(context, \"Hello \", \"world\")",
			javaAST.toString()
		);

	}

	@Test
	public void testPlus() throws IOException {
		String expression = """
   			1 + 2
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Plus.invoke(context, 1, 2)",
			javaAST.toString()
		);

	}

	@Test
	public void testMinus() throws IOException {
		String expression = """
   			1 - 2
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Minus.invoke(context, 1, 2)",
			javaAST.toString()
		);

	}

	@Test
	public void testStar() throws IOException {
		String expression = """
   			1 * 2
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Multiply.invoke(context, 1, 2)",
			javaAST.toString()
		);

	}

	@Test
	public void testSlash() throws IOException {
		String expression = """
   			1 / 2
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Divide.invoke(context, 1, 2)",
			javaAST.toString()
		);

	}
	@Test
	public void testContains() throws IOException {
		String expression = """
   			"Brad Wood" contains "Wood"
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Contains.contains(context, \"Brad Wood\", \"Wood\")",
			javaAST.toString()
		);

	}
	@Test
	public void testNegate() throws IOException {
		String expression = """
   			!True
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Negate.invoke(context, \"True\")",
			javaAST.toString()
		);

	}
	@Test
	public void testNegateNegate() throws IOException {
		String expression = """
   			!!False
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Negate.invoke(context, Negate.invoke(context, \"False\"))",
			javaAST.toString()
		);

	}
	@Test
	public void testTernary() throws IOException {
		String expression = """
   			isGood ? "eat" : "toss"
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseExpression( expression);

		Node javaAST = BoxLangTranspiler.transform(result.getRoot());

		assertEquals(
			"Ternary.invoke(context, Key.of(\"isGood\"), \"eat\", \"toss\")",
			javaAST.toString()
		);

	}

}
