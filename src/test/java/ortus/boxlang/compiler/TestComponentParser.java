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

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.CFTemplateParser;
import ortus.boxlang.compiler.parser.ParsingResult;

public class TestComponentParser extends TestBase {

	public ParsingResult parseStatement( String statement ) throws IOException {
		CFTemplateParser	parser	= new CFTemplateParser();
		ParsingResult		result	= parser.parse( statement );
		if ( !result.isCorrect() ) {
			System.out.println( result.getIssues() );
		}
		assertTrue( result.isCorrect() );
		return result;
	}

	@Test
	public void invokeMethod() throws IOException {
		String			statement	= """
		                              	<cfoutput query="myQry">
		                              	foo #bar# baz
		                              </cfoutput>
		                                                                                                                                                   """;

		ParsingResult	result		= parseStatement( statement );
	}

}
