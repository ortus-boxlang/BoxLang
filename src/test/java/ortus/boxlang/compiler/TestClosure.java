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

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.parser.CFScriptParser;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;

public class TestClosure extends TestBase {

	private Node transformClosure( String expression ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( expression );
		assertTrue( result.isCorrect() );

		JavaTranspiler transpiler = new JavaTranspiler();
		transpiler.setProperty( "packageName", "ortus.test" );
		transpiler.setProperty( "classname", "MyClosure" );
		transpiler.setProperty( "mappingName", "" );
		transpiler.setProperty( "mappingPath", "" );
		transpiler.setProperty( "relativePath", "" );
		transpiler.pushContextName( "context" );
		transpiler.transform( result.getRoot() );
		return transpiler.getCallables().get( 0 );
	}

	@Test
	public void testClosureParameters() throws IOException {
		String			code	= """
		                          ( required string param1='default' key='value' ) key='value' => { return param1 }
		                           """;
		CFScriptParser	parser	= new CFScriptParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
		BoxScript script = ( BoxScript ) result.getRoot();
		script.getStatements().forEach( stmt -> {
			stmt.getDescendants().forEach( it -> {
				if ( it instanceof BoxExpressionStatement exp && exp.getExpression() instanceof BoxClosure closure ) {
					Assertions.assertEquals( 1, closure.getArgs().size() );
					Assertions.assertEquals( 1, closure.getAnnotations().size() );

					BoxArgumentDeclaration arg;
					Assertions.assertEquals( 1, closure.getArgs().get( 0 ).getAnnotations().size() );

				}
			} );
		} );

		CompilationUnit javaAST = ( CompilationUnit ) transformClosure( code );
	}

	@Test
	public void testClosureAsAnonymous() throws IOException {
		String			code	= """
		                          	function( required string param1='default' key='value' ) key='value' { return param1; }
		                          """;
		CFScriptParser	parser	= new CFScriptParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
		BoxScript script = ( BoxScript ) result.getRoot();
		script.getStatements().forEach( stmt -> {
			stmt.getDescendants().forEach( it -> {
				if ( it instanceof BoxExpressionStatement exp && exp.getExpression() instanceof BoxClosure closure ) {
					Assertions.assertEquals( 1, closure.getArgs().size() );
					Assertions.assertEquals( 1, closure.getAnnotations().size() );

					BoxArgumentDeclaration arg;
					Assertions.assertEquals( 1, closure.getArgs().get( 0 ).getAnnotations().size() );

				}
			} );
		} );

		CompilationUnit javaAST = ( CompilationUnit ) transformClosure( code );
	}

	@Test
	public void testClosureReturn() throws IOException {
		String			code	= """
		                          	() => "my func";
		                          """;
		CFScriptParser	parser	= new CFScriptParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );

		CompilationUnit javaAST = ( CompilationUnit ) transformClosure( code );
	}

}
