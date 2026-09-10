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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.parser.CFParser;
import ortus.boxlang.compiler.parser.ParsingResult;

public class TestComponentParser extends TestBase {

	public ParsingResult parseStatement( String statement ) throws IOException {
		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( statement, false );
		if ( !result.isCorrect() ) {
			System.out.println( result.getIssues() );
		}
		assertTrue( result.isCorrect() );
		return result;
	}

	@Test
	public void invokeMethod() throws IOException {
		String statement = """
		                   	<cfoutput query="myQry">
		                   	foo #bar# baz
		                   </cfoutput>
		                                                                                                                                        """;

		parseStatement( statement );
	}

	@Test
	public void preservesLabeledSwitchControlInTemplateCases() throws IOException {
		String			statement				= """
		                                          <cfloop from="1" to="2" index="outer" label="outerLoop">
		                                              <cfswitch expression="go">
		                                                  <cfcase value="go">
		                                                      <cfbreak outerLoop>
		                                                      <cfcontinue outerLoop>
		                                                  </cfcase>
		                                              </cfswitch>
		                                          </cfloop>
		                                          """;

		ParsingResult	result					= parseStatement( statement );

		BoxSwitch		boxSwitch				= result.getRoot().getDescendantsOfType( BoxSwitch.class ).getFirst();
		BoxBreak		boxBreak				= result.getRoot().getDescendantsOfType( BoxBreak.class ).getFirst();
		BoxContinue		boxContinue				= result.getRoot().getDescendantsOfType( BoxContinue.class ).getFirst();
		BoxComponent	breakLoopAncestor		= boxBreak.getFirstAncestorOfType( BoxComponent.class );
		BoxComponent	continueLoopAncestor	= boxContinue.getFirstAncestorOfType( BoxComponent.class );

		assertTrue( boxSwitch.hasBreakingCases() );
		assertEquals( "outerLoop", boxBreak.getLabel() );
		assertEquals( "outerLoop", boxContinue.getLabel() );
		assertEquals( "loop", breakLoopAncestor.getName().toLowerCase() );
		assertEquals( "loop", continueLoopAncestor.getName().toLowerCase() );
		assertTrue( boxBreak.getFirstAncestorOfType( BoxForIndex.class ) == null );
		assertTrue( boxContinue.getFirstAncestorOfType( BoxForIndex.class ) == null );
	}

}
