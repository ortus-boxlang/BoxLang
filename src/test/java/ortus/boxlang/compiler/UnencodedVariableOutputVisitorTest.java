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
package ortus.boxlang.compiler;

import java.io.File;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.visitor.UnencodedVariableOutputVisitor;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.ParseException;

public class UnencodedVariableOutputVisitorTest {

	@Test
	public void testUnencodedVisitor() {
		BoxRuntime.getInstance( true );
		ParsingResult result = new Parser().parse( new File( "src/test/java/ortus/boxlang/compiler/UnencodedTest.cfm" ) );
		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), "" );
		}
		UnencodedVariableOutputVisitor visitor = new UnencodedVariableOutputVisitor();
		result.getRoot().accept( visitor );

		var issues = visitor.getIssues();
		System.out.println( "found " + issues.size() + " issues" );
		for ( var issue : issues ) {
			System.out.println( issue.message() );
		}

	}

}
