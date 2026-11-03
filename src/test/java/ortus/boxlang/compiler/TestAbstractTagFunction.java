/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * A tag-based function marked `modifier="abstract"` must be represented in the
 * AST with a NULL body (like a script abstract function), so downstream abstract
 * discovery (which keys off `getBody() == null`) works. Whitespace-only bodies
 * are normalized to null; a body containing REAL statements is a semantic error.
 */
public class TestAbstractTagFunction {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	private BoxFunctionDeclaration firstFunction( ParsingResult result ) {
		return result.getRoot().getDescendantsOfType( BoxFunctionDeclaration.class ).getFirst();
	}

	@Test
	public void testCFAbstractFunctionWhitespaceBodyNormalized() throws IOException {
		String			code	= """
		                          <cfcomponent>
		                          <cffunction name="abs" modifier="abstract">
		                          </cffunction>
		                          </cfcomponent>
		                          """;
		ParsingResult	result	= new Parser().parse( code, BoxSourceType.CFTEMPLATE, true );
		assertTrue( result.isCorrect() );
		assertNull( firstFunction( result ).getBody() );
	}

	@Test
	public void testBoxAbstractFunctionWhitespaceBodyNormalized() throws IOException {
		String			code	= """
		                          <bx:function name="abs" modifier="abstract">
		                          </bx:function>
		                          """;
		ParsingResult	result	= new Parser().parse( code, BoxSourceType.BOXTEMPLATE, false );
		assertTrue( result.isCorrect() );
		assertNull( firstFunction( result ).getBody() );
	}

	@Test
	public void testCFAbstractFunctionWithWhitespaceText() throws IOException {
		// Whitespace-only buffer output is NOT real code, so it normalizes to null.
		String			code	= """
		                          <cfcomponent>
		                          <cffunction name="abs" modifier="abstract">
		                          	 \n
		                          </cffunction>
		                          </cfcomponent>
		                          """;
		ParsingResult	result	= new Parser().parse( code, BoxSourceType.CFTEMPLATE, true );
		assertTrue( result.isCorrect() );
		assertNull( firstFunction( result ).getBody() );
	}

	@Test
	public void testCFAbstractFunctionRejectsRealStatements() throws IOException {
		String			code	= """
		                          <cfcomponent>
		                          <cffunction name="abs" modifier="abstract">
		                          <cfset x = 1>
		                          </cffunction>
		                          </cfcomponent>
		                          """;
		ParsingResult	result	= new Parser().parse( code, BoxSourceType.CFTEMPLATE, true );
		assertTrue( !result.isCorrect() );
		assertTrue( result.getIssues().stream()
		    .anyMatch( issue -> issue.getMessage().contains( "Abstract function [abs] cannot have a body" ) ) );
	}

	@Test
	public void testBoxAbstractFunctionRejectsRealStatements() throws IOException {
		String			code	= """
		                          <bx:function name="abs" modifier="abstract">
		                          <bx:set x = 1>
		                          </bx:function>
		                          """;
		ParsingResult	result	= new Parser().parse( code, BoxSourceType.BOXTEMPLATE, false );
		assertTrue( !result.isCorrect() );
		assertTrue( result.getIssues().stream()
		    .anyMatch( issue -> issue.getMessage().contains( "Abstract function [abs] cannot have a body" ) ) );
	}

	@Test
	public void testCFInterfaceAbstractFunction() throws IOException {
		// A <cfinterface> containing an explicit `modifier="abstract"` function: the
		// function is normalized to a null body, and the interface conversion must
		// not NPE on that null (it iterates each function body to null out
		// whitespace-only bodies). Regression: the interface path lacked a null guard.
		String			code	= """
		                          <cfinterface>
		                          <cffunction name="abs" modifier="abstract">
		                          </cffunction>
		                          </cfinterface>
		                          """;
		ParsingResult	result	= new Parser().parse( code, BoxSourceType.CFTEMPLATE, true );
		assertTrue( result.isCorrect() );
		assertNull( firstFunction( result ).getBody() );
	}
}