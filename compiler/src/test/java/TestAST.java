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
import org.junit.Test;
import ourtus.boxlang.parser.ParsingResult;
import ourtus.boxlang.parser.BoxLangParser;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class TestAST extends TestBase {

	@Test
	public void testParser() throws IOException {
		BoxLangParser parser = new BoxLangParser();
		//		List<Path> files = scanForFiles( testboxDirectory, Set.of( "cfc", "cfm", "cfml" ) );
		List<Path> files = scanForFiles( "/cf_to_java/HelloWorld", Set.of( "cfc", "cfm", "cfml" ) );
		for ( Path file : files ) {
			System.out.println( file );
			ParsingResult result = parser.parse( file.toFile() );
			if ( !result.isCorrect() ) {
				result.getIssues().forEach(System.out::println);
			}
		}
	}

	@Test
	public void testTranspiler() throws Exception {
		BoxLangParser parser = new BoxLangParser();
		BoxLangTranspiler transpiler = new BoxLangTranspiler();

		List<Path> files = scanForFiles( "/cf_to_java/HelloWorld", Set.of( "cfc" ) );
		for ( Path file : files ) {
			System.out.println( file );
			ParsingResult result = parser.parse( file.toFile() );
			CompilationUnit javaAST = transpiler.transpile(result.getRoot());
			System.out.println(javaAST);
		}

	}
}
