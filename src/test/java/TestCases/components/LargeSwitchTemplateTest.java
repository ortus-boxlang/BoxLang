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
package TestCases.components;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class LargeSwitchTemplateTest {

	private static final int	LARGE_SWITCH_CASE_COUNT			= 1200;
	private static final int	LARGE_SWITCH_SEGMENT_COUNT		= 2;
	private static final int	OVERSIZED_CASE_SEGMENT_COUNT	= 400;
	private static final Key	RESULT_KEY						= Key.of( "result" );
	private static final Key	AFTER_LOOP_KEY					= Key.of( "afterLoop" );

	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large tag switch does not fall through after a match" )
	@Test
	public void testLargeTagSwitchDoesNotFallThroughAfterMatch() {
		executeTemplateFresh( buildLargeTagSwitchNoFallThroughTemplate() );

		assertThat( this.variables.get( RESULT_KEY ) ).isEqualTo( "case0-segment1" );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large tag switch break in a case exits the enclosing loop" )
	@Test
	public void testLargeTagSwitchBreakInCaseExitsEnclosingLoop() {
		executeTemplateFresh( buildLargeTagSwitchBreakPropagationTemplate() );

		assertThat( this.variables.get( RESULT_KEY ) ).isEqualTo( 3 );
		assertThat( this.variables.get( AFTER_LOOP_KEY ) ).isEqualTo( "reached" );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large tag switch continue in a case skips the loop iteration" )
	@Test
	public void testLargeTagSwitchContinueInCaseSkipsLoopIteration() {
		executeTemplateFresh( buildLargeTagSwitchContinuePropagationTemplate() );

		assertThat( this.variables.get( RESULT_KEY ) ).isEqualTo( "1245" );
		assertThat( this.variables.get( AFTER_LOOP_KEY ) ).isEqualTo( "reached" );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large tag switch labeled break in a case exits the targeted outer loop" )
	@Test
	public void testLargeTagSwitchLabeledBreakInCaseExitsTargetedOuterLoop() {
		executeTemplateFresh( buildLargeTagSwitchLabeledBreakPropagationTemplate() );

		assertThat( this.variables.get( RESULT_KEY ) ).isEqualTo( 1 );
		assertThat( this.variables.get( AFTER_LOOP_KEY ) ).isEqualTo( "reached" );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large tag switch labeled continue in a case skips to the targeted outer loop iteration" )
	@Test
	public void testLargeTagSwitchLabeledContinueInCaseSkipsTargetedOuterLoopIteration() {
		executeTemplateFresh( buildLargeTagSwitchLabeledContinuePropagationTemplate() );

		assertThat( this.variables.get( RESULT_KEY ) ).isEqualTo( "1234" );
		assertThat( this.variables.get( AFTER_LOOP_KEY ) ).isEqualTo( "reached" );
	}

	private void executeTemplateFresh( String source ) {
		instance.executeSource(
		    source + "\n<!--- nonce: " + System.nanoTime() + " --->",
		    this.context,
		    BoxSourceType.CFTEMPLATE
		);
	}

	private String buildLargeTagSwitchNoFallThroughTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );

		for ( int caseIndex = 0; caseIndex < LARGE_SWITCH_CASE_COUNT; caseIndex++ ) {
			source.append( "<cfcase value=\"case" ).append( caseIndex ).append( "\">\n" );

			for ( int segmentIndex = 0; segmentIndex < LARGE_SWITCH_SEGMENT_COUNT; segmentIndex++ ) {
				source.append( "<cfset result = 'case" )
				    .append( caseIndex )
				    .append( "-segment" )
				    .append( segmentIndex )
				    .append( "'>\n" );
			}

			source.append( "<cfbreak>\n" );
			source.append( "</cfcase>\n" );
		}

		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );

		return source.toString();
	}

	private String buildLargeTagSwitchBreakPropagationTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = 0>\n" );
		source.append( "<cfloop from=\"1\" to=\"10\" index=\"i\">\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );
		source.append( "<cfcase value=\"case0\">\n" );
		appendOversizedCaseNoise( source, "break-noise" );
		source.append( "<cfset result = result + 1>\n" );
		source.append( "<cfif result EQ 3>\n" );
		source.append( "<cfbreak>\n" );
		source.append( "</cfif>\n" );
		source.append( "</cfcase>\n" );
		source.append( "<cfcase value=\"case1\">\n" );
		source.append( "<cfset result = -999>\n" );
		source.append( "</cfcase>\n" );
		appendTrailingSimpleCases( source, 2 );
		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = -1000>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );
		source.append( "</cfloop>\n" );
		source.append( "<cfset afterLoop = 'reached'>\n" );

		return source.toString();
	}

	private String buildLargeTagSwitchContinuePropagationTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfloop from=\"1\" to=\"5\" index=\"i\">\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );
		source.append( "<cfcase value=\"case0\">\n" );
		appendOversizedCaseNoise( source, "continue-noise" );
		source.append( "<cfif i EQ 3>\n" );
		source.append( "<cfcontinue>\n" );
		source.append( "</cfif>\n" );
		source.append( "</cfcase>\n" );
		source.append( "<cfcase value=\"case1\">\n" );
		source.append( "<cfset result = 'wrong-branch'>\n" );
		source.append( "</cfcase>\n" );
		appendTrailingSimpleCases( source, 2 );
		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'wrong-default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );
		source.append( "<cfset result = result & i>\n" );
		source.append( "</cfloop>\n" );
		source.append( "<cfset afterLoop = 'reached'>\n" );

		return source.toString();
	}

	private String buildLargeTagSwitchLabeledBreakPropagationTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = 0>\n" );
		source.append( "<cfloop from=\"1\" to=\"5\" index=\"outer\" label=\"outerLoop\">\n" );
		source.append( "<cfloop from=\"1\" to=\"5\" index=\"inner\">\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );
		source.append( "<cfcase value=\"case0\">\n" );
		appendOversizedCaseNoise( source, "labeled-break-noise" );
		source.append( "<cfset result = result + 1>\n" );
		source.append( "<cfbreak outerLoop>\n" );
		source.append( "</cfcase>\n" );
		source.append( "<cfcase value=\"case1\">\n" );
		source.append( "<cfset result = -999>\n" );
		source.append( "</cfcase>\n" );
		appendTrailingSimpleCases( source, 2 );
		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = -1000>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );
		source.append( "<cfset result = -998>\n" );
		source.append( "</cfloop>\n" );
		source.append( "<cfset result = -997>\n" );
		source.append( "</cfloop>\n" );
		source.append( "<cfset afterLoop = 'reached'>\n" );

		return source.toString();
	}

	private String buildLargeTagSwitchLabeledContinuePropagationTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfloop from=\"1\" to=\"4\" index=\"outer\" label=\"outerLoop\">\n" );
		source.append( "<cfloop from=\"1\" to=\"2\" index=\"inner\">\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );
		source.append( "<cfcase value=\"case0\">\n" );
		appendOversizedCaseNoise( source, "labeled-continue-noise" );
		source.append( "<cfif inner EQ 1>\n" );
		source.append( "<cfset result = result & outer>\n" );
		source.append( "<cfcontinue outerLoop>\n" );
		source.append( "</cfif>\n" );
		source.append( "</cfcase>\n" );
		source.append( "<cfcase value=\"case1\">\n" );
		source.append( "<cfset result = 'wrong-branch'>\n" );
		source.append( "</cfcase>\n" );
		appendTrailingSimpleCases( source, 2 );
		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'wrong-default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );
		source.append( "<cfset result = result & 'x'>\n" );
		source.append( "</cfloop>\n" );
		source.append( "<cfset result = result & 'y'>\n" );
		source.append( "</cfloop>\n" );
		source.append( "<cfset afterLoop = 'reached'>\n" );

		return source.toString();
	}

	private void appendOversizedCaseNoise( StringBuilder source, String prefix ) {
		for ( int segmentIndex = 0; segmentIndex < OVERSIZED_CASE_SEGMENT_COUNT; segmentIndex++ ) {
			source.append( "<cfset noise = '" )
			    .append( prefix )
			    .append( "-segment" )
			    .append( segmentIndex )
			    .append( "'>\n" );
		}
	}

	private void appendTrailingSimpleCases( StringBuilder source, int startIndex ) {
		for ( int caseIndex = startIndex; caseIndex < LARGE_SWITCH_CASE_COUNT; caseIndex++ ) {
			source.append( "<cfcase value=\"case" ).append( caseIndex ).append( "\">\n" );
			source.append( "<cfset noise = 'case" ).append( caseIndex ).append( "'>\n" );
			source.append( "</cfcase>\n" );
		}
	}
}