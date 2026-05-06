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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringPattern;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxMatchArrayPattern;
import ortus.boxlang.compiler.ast.expression.BoxMatchCase;
import ortus.boxlang.compiler.ast.expression.BoxMatchExpression;
import ortus.boxlang.compiler.ast.expression.BoxMatchObjectPattern;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringPattern;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

public class MatchStructuralPatternsTest {

	private static final Key	RESULT		= Key.of( "result" );
	private static final Key	GUARD_RUNS	= Key.of( "guardRuns" );

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
	public void testMatchBuildsNestedObjectPatternAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	{ user: { name }, ...rest } -> name
		    	_ -> null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		BoxMatchExpression				matchExpression	= assertInstanceOf( BoxMatchExpression.class, result.getRoot() );
		BoxMatchCase					firstCase		= matchExpression.getCases().get( 0 );
		BoxMatchObjectPattern			objectPattern	= assertInstanceOf( BoxMatchObjectPattern.class, firstCase.getPattern() );
		BoxObjectDestructuringPattern	pattern			= objectPattern.getPattern();
		assertEquals( 2, pattern.getBindings().size() );

		BoxObjectDestructuringBinding	userBinding		= pattern.getBindings().get( 0 );
		BoxObjectDestructuringPattern	nestedPattern	= assertInstanceOf( BoxObjectDestructuringPattern.class, userBinding.getPattern() );
		assertEquals( 1, nestedPattern.getBindings().size() );
		assertThat( nestedPattern.getBindings().get( 0 ).getTarget() ).isInstanceOf( BoxIdentifier.class );
		assertEquals( "name", ( ( BoxIdentifier ) nestedPattern.getBindings().get( 0 ).getTarget() ).getName() );

		BoxObjectDestructuringBinding restBinding = pattern.getBindings().get( 1 );
		assertTrue( restBinding.isRest() );
		assertThat( restBinding.getTarget() ).isInstanceOf( BoxIdentifier.class );
		assertEquals( "rest", ( ( BoxIdentifier ) restBinding.getTarget() ).getName() );
	}

	@Test
	public void testMatchBuildsArrayPatternAstWithGuard() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	[ head, ...tail ] if head > 0 -> tail
		    	_ -> []
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		BoxMatchExpression	matchExpression	= assertInstanceOf( BoxMatchExpression.class, result.getRoot() );
		BoxMatchCase		firstCase		= matchExpression.getCases().get( 0 );
		assertThat( firstCase.getGuard() ).isNotNull();

		BoxMatchArrayPattern			arrayPattern	= assertInstanceOf( BoxMatchArrayPattern.class, firstCase.getPattern() );
		BoxArrayDestructuringPattern	pattern			= arrayPattern.getPattern();
		assertEquals( 2, pattern.getBindings().size() );

		BoxArrayDestructuringBinding firstBinding = pattern.getBindings().get( 0 );
		assertThat( firstBinding.getTarget() ).isInstanceOf( BoxIdentifier.class );
		assertEquals( "head", ( ( BoxIdentifier ) firstBinding.getTarget() ).getName() );

		BoxArrayDestructuringBinding restBinding = pattern.getBindings().get( 1 );
		assertTrue( restBinding.isRest() );
		assertThat( restBinding.getTarget() ).isInstanceOf( BoxIdentifier.class );
		assertEquals( "tail", ( ( BoxIdentifier ) restBinding.getTarget() ).getName() );
	}

	@Test
	public void testMatchPrettyPrintsStructuralPatternsAndGuards() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	{ user: { name }, ...rest } if name != "" -> rest
		    	[ head, ...tail ] -> tail
		    	_ -> null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		String prettyPrinted = PrettyPrint.prettyPrint( result.getRoot() )
		    .replace( "\r\n", "\n" )
		    .replace( "\t", "    " )
		    .stripTrailing();
		assertEquals(
		    "match value {\n    { user: { name }, ...rest } if name != \"\" -> rest\n    [ head, ...tail ] -> tail\n    _ -> null\n}",
		    prettyPrinted
		);
	}

	@Test
	public void testMatchObjectPatternBindsValuesBeforeGuardAndBody() {
		Struct user = new Struct();
		user.put( Key.of( "name" ), "Ada" );

		Struct data = new Struct();
		data.put( Key.of( "user" ), user );
		data.put( Key.of( "role" ), "admin" );
		data.put( Key.of( "active" ), true );
		this.variables.put( Key.of( "data" ), data );

		Object result = instance.executeStatement(
		    """
		    match data {
		    	{ user: { name }, role, ...rest } if role == "admin" -> name & ":" & rest.active
		    	_ -> "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada:true" );
	}

	@Test
	public void testMatchArrayPatternSupportsRestBindingsInGuards() {
		Array data = new Array();
		data.add( 1 );
		data.add( 2 );
		data.add( 3 );
		data.add( 4 );
		this.variables.put( Key.of( "data" ), data );

		Object result = instance.executeStatement(
		    """
		    match data {
		    	[ head, ...tail ] if tail.len() == 3 -> head + tail[ 1 ]
		    	_ -> 0
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( 3 );
	}

	@Test
	public void testMatchDoesNotRunGuardWhenStructuralPatternFails() {
		this.variables.put( GUARD_RUNS, 0 );

		Object result = instance.executeStatement(
		    """
		    match {} {
		    	{ user: { name } } if ( guardRuns = guardRuns + 1 ) > 0 -> name
		    	_ -> "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "fallback" );
		assertThat( this.variables.get( GUARD_RUNS ) ).isEqualTo( 0 );
	}

	@Test
	public void testMatchParsesGuardedStructuralMissWithFallbackCase() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    "match {} { { user: { name } } if ( guardRuns = guardRuns + 1 ) > 0 -> name _ -> \"fallback\" }"
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		BoxMatchExpression matchExpression = assertInstanceOf( BoxMatchExpression.class, result.getRoot() );
		assertEquals( 2, matchExpression.getCases().size() );
		assertThat( matchExpression.getCases().get( 0 ).getGuard() ).isNotNull();
	}

	@Test
	public void testMatchWildcardCaseReturnsBody() {
		Object result = instance.executeStatement(
		    "match {} { _ -> \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "fallback" );
	}

	@Test
	public void testMatchStructuralMissFallsThroughWithoutGuard() {
		Object result = instance.executeStatement(
		    "match {} { { user: { name } } -> \"bad\" _ -> \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "fallback" );
	}
}