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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class MatchTypePatternsTest {

	private static BoxRuntime	instance;

	private IBoxContext			context;
	private IScope				variables;

	@BeforeAll
	public static void setupRuntime() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testMatchParsesTypePatternWithBinding() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match( value ) {
		    	is numeric,string as n => n;
		    	_ => null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchPrettyPrintsTypePatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match( value ) {
		    	is numeric,string as n => n;
		    	_ => null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		String prettyPrinted = PrettyPrint.prettyPrint( result.getRoot() )
		    .replace( "\r\n", "\n" )
		    .replace( "\t", "    " )
		    .stripTrailing();
		assertEquals( "match( value ) {\n    is numeric, string as n => n;\n    _ => null\n}", prettyPrinted );
	}

	@Test
	public void testMatchTypePatternUsesLooseCasterForBinding() {
		this.variables.put( Key.of( "value" ), "42" );

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	is numeric as n => n + 1;
		    	_ => 0
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( 43 );
	}

	@Test
	public void testMatchTypePatternEvaluatesGuardAgainstCastedBinding() {
		this.variables.put( Key.of( "value" ), "42" );

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	is numeric as n if n > 40 => "big";
		    	_ => "small"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "big" );
	}

	@Test
	public void testMatchTypePatternTriesTypesLeftToRight() {
		this.variables.put( Key.of( "value" ), "42" );

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	is array,numeric as n => n;
		    	_ => 0
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( 42 );
	}
}