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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxMatchCase;
import ortus.boxlang.compiler.ast.expression.BoxMatchExpression;
import ortus.boxlang.compiler.ast.expression.BoxMatchPattern;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class MatchConstructorPatternsTest {

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
	public void testMatchBuildsConstructorPatternAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match( value ) {
		    	Some( x ) => x
		    	_ => null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		BoxMatchExpression	matchExpression	= assertInstanceOf( BoxMatchExpression.class, result.getRoot() );
		BoxMatchCase		firstCase		= matchExpression.getCases().get( 0 );
		BoxMatchPattern		firstPattern	= firstCase.getPattern();
		assertEquals( "BoxMatchConstructorPattern", firstPattern.getClass().getSimpleName() );
		assertThat( firstPattern.toMap().get( "label" ) ).isNotNull();
		assertThat( firstPattern.toMap().get( "patterns" ) ).isNotNull();
	}

	@Test
	public void testMatchPrettyPrintsConstructorPatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match( value ) {
		    	Some( x ) => x
		    	_ => null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		String prettyPrinted = PrettyPrint.prettyPrint( result.getRoot() )
		    .replace( "\r\n", "\n" )
		    .replace( "\t", "    " )
		    .stripTrailing();
		assertEquals(
		    "match( value ) {\n    Some( x ) => x\n    _ => null\n}",
		    prettyPrinted
		);
	}

	@Test
	public void testMatchConstructorPatternBindsAnnotatedProperties() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchSome( value = \"Ada\" )", this.context )
		);

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Some( x ) => x
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada" );
	}

	@Test
	public void testMatchConstructorPatternFallsThroughWhenTagDoesNotMatch() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchSome( value = \"Ada\" )", this.context )
		);

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Success( x ) => x
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "fallback" );
	}

	@Test
	public void testMatchConstructorPatternSupportsNestedPayloadDestructuring() {
		IStruct	meta	= Struct.of( "cached", true );
		IStruct	payload	= Struct.of( "data", "Ada", "meta", meta );

		this.variables.put( Key.of( "payload" ), payload );
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchSuccess( payload = payload )", this.context )
		);

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Success( { data, meta } ) if meta.cached => data
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada" );
	}

	@Test
	public void testMatchConstructorPatternRejectsInvalidMetadata() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchInvalidTag( value = \"Ada\" )", this.context )
		);

		BoxRuntimeException exception = assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeStatement(
		        """
		        match( value ) {
		        	Broken( x ) => x
		        	_ => "fallback"
		        }
		        """,
		        this.context
		    )
		);

		assertThat( exception ).hasMessageThat().contains( "@patternMatch references unknown property [missingValue]" );
	}

	@Test
	public void testMatchConstructorPatternSupportsMultipleTagsPerClass() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchTaggedValue( value = \"Ada\" )", this.context )
		);

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Just( x ) => x
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada" );
	}

	@Test
	public void testMatchConstructorPatternUsesFirstMatchingBranch() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchTaggedValue( value = \"Ada\" )", this.context )
		);

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Just( x ) => x & "-first"
		    	Just( x ) => x & "-second"
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada-first" );
	}

	@Test
	public void testMatchConstructorPatternSupportsMultipleTagsWithNestedPayloads() {
		IStruct	meta	= Struct.of( "cached", true );
		IStruct	payload	= Struct.of( "data", "Ada", "meta", meta );

		this.variables.put( Key.of( "payload" ), payload );
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchTaggedValue( payload = payload )", this.context )
		);

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Success( { data, meta } ) if meta.cached => data
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada" );
	}

	@Test
	public void testMatchConstructorPatternSupportsAttemptOkAlias() {
		this.variables.put( Key.of( "value" ), Attempt.of( "Ada" ) );

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Ok( x ) => x
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "Ada" );
	}

	@Test
	public void testMatchConstructorPatternSupportsAttemptErrAlias() {
		this.variables.put( Key.of( "value" ), Attempt.fail( "boom" ) );

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Err( problem ) => problem
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "boom" );
	}

	@Test
	public void testMatchConstructorPatternSupportsEmptyAttemptFailureArity() {
		this.variables.put( Key.of( "value" ), Attempt.empty() );

		Object result = instance.executeStatement(
		    """
		    match( value ) {
		    	Failure() => "empty"
		    	_ => "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "empty" );
	}
}