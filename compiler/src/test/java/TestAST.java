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

import com.github.javaparser.ast.CompilationUnit;
import org.checkerframework.checker.units.qual.N;
import org.junit.Test;
import ourtus.boxlang.ast.Node;
import ourtus.boxlang.ast.expression.BoxBinaryOperation;
import ourtus.boxlang.parser.BoxLangParser;
import ourtus.boxlang.parser.ParsingResult;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
public class TestAST extends TestBase {
	protected BoxLangParser parser = new BoxLangParser();
	@Test
	public void testBinaryOperation() throws IOException {
		String[] epressions = new String[]  {
			"1 + 2",
			"1 + 2.0",
			"1 + \"a\"",
			"1 + (1 + a)",
			"1 + (-1)",
			"1 - 2",
			"1 * 2",
			"1 / 2",
			"1 + variables['system']",
			"1 + create('a')",
			"1 + a.create('a')",
			"1 + a.create(p1='a')",
			"1 + a.b",
			"true && false",
			"true || false",
//			"1 % 2",
			"""
   				"a" & b""
			""",

		};

		for (int i = 0; i < epressions.length; i++) {
			System.out.println(epressions[i]);
			ParsingResult result = parser.parseExpression(epressions[i]);
			assertTrue(result.isCorrect());
			assertTrue(result.getRoot() instanceof BoxBinaryOperation);

			BoxBinaryOperation operation = (BoxBinaryOperation)result.getRoot();
            assertEquals(2, operation.getChildren().size());
			operation.getChildren().forEach( it -> {
					assertEquals(it.getParent() , operation);
			});

		}
	}
	@Test
	public void testParser() throws IOException {
		//		List<Path> files = scanForFiles( testboxDirectory, Set.of( "cfc", "cfm", "cfml" ) );
		List<Path> files = scanForFiles( "../boxlang/examples/cf_to_java/HelloWorld",
			Set.of( "cfc", "cfm", "cfml" ) );
		for ( Path file : files ) {
			System.out.println( file );
			ParsingResult result = parser.parse( file.toFile() );
			if ( !result.isCorrect() ) {
				result.getIssues().forEach( error -> System.out.println( error ) );
			}
		}
	}

	@Test
	public void testTranspiler() throws Exception {

		BoxLangTranspiler transpiler = new BoxLangTranspiler();

		List<Path> files = scanForFiles( "../examples/cf_to_java/HelloWorld", Set.of( "cfc" ) );
		for ( Path file : files ) {
			System.out.println( file );
			ParsingResult result = parser.parse( file.toFile() );
			CompilationUnit javaAST = transpiler.transpile( result.getRoot() );
			System.out.println( javaAST );
		}

	}
}
