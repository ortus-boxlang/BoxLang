package compiler;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.github.javaparser.ast.Node;

import ortus.boxlang.executor.JavaRunner;
import ortus.boxlang.parser.BoxFileType;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.BoxLangTranspiler;

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

public class TestExecution extends TestBase {

	@Test
	public void executeFreeStyle() throws IOException {
		BoxParser		parser	= new BoxParser();
		ParsingResult	result	= parser.parse( new File( "examples/cf_to_java/freestyle/freestyle.cfm" ) );
		result.getIssues().forEach( it -> System.out.println( it ) );
		assertTrue( result.isCorrect() );

		BoxLangTranspiler	transpiler	= new BoxLangTranspiler();
		Node				javaAST		= transpiler.transpile( result.getRoot() );
		new JavaRunner().run( transpiler.getStatements() );
	}

	@Test
	public void executeWhile() throws IOException {

		String			statement	= """
		                              variables['system'] = createObject('java','java.lang.System');

		                              a = 1;
		                              while(a < 10) {
		                                 switch(variables.a) {
		                                 case 0: {
		                                   variables.system.out.println("zero");
		                                   break;
		                                 }
		                                default: {
		                                   variables.system.out.println("non zero");
		                                   break;
		                                 }
		                              }
		                              if(!a % 2 == 0) {
		                                  variables.system.out.println("even and a=#variables.a#");
		                              }
		                              a +=1;

		                              }
		                              //assert(variables["a"] == 10);
		                              """;
		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parse( statement, BoxFileType.CFSCRIPT );
		assertTrue( result.isCorrect() );

		BoxLangTranspiler	transpiler	= new BoxLangTranspiler();
		Node				javaAST		= transpiler.transpile( result.getRoot() );
		new JavaRunner().run( transpiler.getStatements() );
	}

	@Test
	public void executeFor() throws IOException {

		String			statement	= """
		                                                                     variables['system'] = createObject('java','java.lang.System');
		                              variables.a = 0;
		                                                                     for(a = 0; a < 10; a++){
		                                                                     	variables.system.out.println(a);
		                                                                     }
		                                              assert(variables["a"] == 10);
		                                                                     """;
		BoxParser		parser		= new BoxParser();
		ParsingResult	result		= parser.parse( statement, BoxFileType.CFSCRIPT );
		assertTrue( result.isCorrect() );

		BoxLangTranspiler	transpiler	= new BoxLangTranspiler();
		Node				javaAST		= transpiler.transpile( result.getRoot() );
		new JavaRunner().run( transpiler.getStatements() );

	}

}
