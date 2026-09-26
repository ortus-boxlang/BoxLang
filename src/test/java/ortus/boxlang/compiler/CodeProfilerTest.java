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

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * Tests for the ASM boxpiler's code-profiling instrumentation: run a block with
 * profiling enabled and verify the boxpiler registered a blueprint (Pass A) and the
 * executed expression statement's span got covered via the emitted
 * {@code CodeProfilerService.mark} call (Pass B).
 * <p>
 * This differs from the service-only {@code CodeProfilerServiceTest}: here we go
 * source -> boxpiler spans -> mark emission -> recorded coverage through the runtime.
 */
class CodeProfilerTest {

	private BoxRuntime	runtime;
	private boolean		prevProfiler;

	@BeforeEach
	void setup() {
		this.runtime										= BoxRuntime.getInstance();
		// Enable compile-time instrumentation for this test
		this.prevProfiler									= this.runtime.getConfiguration().codeProfilerEnabled;
		this.runtime.getConfiguration().codeProfilerEnabled	= true;
		// Source is compiled once and cached in the boxpiler class pool (memory + disk),
		// so clear BOTH to force a recompile now that instrumentation is enabled.
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		// REMEMBER: this clean may trash other test caches in local runs — CI starts
		// with an empty classes dir, so it's only actually needed locally.
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		CodeProfilerService.setActive( true );
		CodeProfilerService.reset();
	}

	@AfterEach
	void teardown() {
		this.runtime.getConfiguration().codeProfilerEnabled = this.prevProfiler;
		CodeProfilerService.setActive( false );
		CodeProfilerService.reset();
	}

	@DisplayName( "It registers a blueprint and profiles the expression statement" )
	@Test
	void testExpressionStatement() {
		String source = "2 + 2;";
		runtime.executeSource( source );

		// Source is adhoc, so the blueprint is keyed by the same MD5 the boxpiler used.
		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Pass A: the boxpiler registered a SOURCE blueprint.
		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( key );

		// Span-definition sanity: both operands of 2 + 2 are literals, which cannot
		// throw when evaluated, so the whole statement is one span.
		var spanDefs = CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 1 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );

		// Pass B: the emitted mark() ran the statement span exactly once.
		var spans = CodeProfilerService.spansOnLine( key, 1 );
		assertThat( spans ).hasSize( 1 );
		assertThat( spans.get( 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: line 1 is covered with count 1.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles plain statements, one per line" )
	@Test
	void testPlainStatements() {
		String source = """
		                x = 1;
		                y = 2;
		                z = x + y;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Pass A: three statements -> x=1 and y=2 are single spans; z = x + y splits
		// into two (left + operator, then the right operand which may throw).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );  // "x = 1 "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );  // "y = 2 "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );  // "z = x + "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 8, 3, 9, true ) );  // "y"

		// Pass B: each statement's span profiled exactly once.
		assertThat( CodeProfilerService.spansOnLine( key, 1 ) ).hasSize( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 1 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 2 ) ).hasSize( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 2 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 3 ) ).hasSize( 2 );
		assertThat( CodeProfilerService.spansOnLine( key, 3 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 3 ).get( 1 ).stats().count() ).isEqualTo( 1 );

		// Line-based: all three lines covered with count 1.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles each ternary operand as its own span" )
	@Test
	void testTernary() {
		String source = """
		                bar = true;
		                baz = 1;
		                bum = 2;
		                x = bar ? baz : bum;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testTernary dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the ternary statement splits into 3 executable spans:
		// "x = bar ? " (line 4, cols 0-10) — condition + punctuation
		// "baz : " (line 4, cols 10-16) — whenTrue branch
		// "bum" (line 4, cols 16-19) — whenFalse branch
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 ); // 3 assignments + 3 ternary spans
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 10, true ) );  // bar = true
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 7, true ) );   // baz = 1
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 7, true ) );   // bum = 2
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 10, true ) );  // "x = bar ? "
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 10, 4, 16, true ) ); // "baz : "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 16, 4, 19, true ) ); // "bum"

		// Pass B: bar=true so the true branch (baz) ran exactly once, the false branch
		// (bum) is MISSED (count == 0).
		var	xBar	= CodeProfilerService.spanAt( key, 4, 1 );
		var	baz		= CodeProfilerService.spanAt( key, 4, 11 );
		var	bum		= CodeProfilerService.spanAt( key, 4, 17 );
		assertThat( xBar.stats().count() ).isEqualTo( 1 );
		assertThat( baz.stats().count() ).isEqualTo( 1 );
		assertThat( bum.stats().count() ).isEqualTo( 0 );

		// Line-based: lines 1-3 assignments cover once; line 4 the ternary covers once.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a branching condition out of the leading span" )
	@Test
	void testTernaryWithBranchingCondition() {
		String source = "x = ( true ? false : true ) ? 1 : 2;";
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testTernaryWithBranchingCondition dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The condition is always evaluated, so it merges into the running span;
		// each branch breaks into its own span. Punctuation is included per the
		// design. Expect 5 executable spans:
		// "x = ( true ? " | "false : " | "true ) ? " | "1 : " | "2"
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) );  // "x = ( true ? "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 13, 1, 21, true ) ); // "false : "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 21, 1, 30, true ) ); // "true ) ? "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 30, 1, 34, true ) ); // "1 : "
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 34, 1, 35, true ) );  // "2"

		assertThat( CodeProfilerService.spansOnLine( key, 1 ).get( 0 ).stats().count() ).isEqualTo( 1 );   // first span on line 1
		assertThat( CodeProfilerService.spanAt( key, 1, 34 ).stats().count() ).isEqualTo( 1 );  // "2"

		// Line-based: line 1 is covered, count 1 from the leading span.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It only marks statements that ran before a throw" )
	@Test
	void testThrow() {
		String source = """
		                i = 5;
		                i++;
		                i++;
		                i++;
		                throw( "boom" );
		                i++;
		                i++;
		                i++;
		                i++;
		                i++;
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the script throws mid-way
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 10 executable spans — 4 before the throw (lines 1-4), the throw
		// itself (line 5), and 5 after (lines 6-10).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 10 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "i = 5"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 15, true ) );  // "throw( \"boom\" )"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 8, 0, 8, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 3, true ) ); // "i++"

		// Lines 1-5 ran (i=5 + three i++ + the throw); lines 6-10 never ran.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 6 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 8 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 9 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 10 ).covered() ).isFalse();

		// Exact counts: lines 1-5 each ran exactly once; spans on lines 6-10 are 0.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // i = 5
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // i++
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // i++
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );  // i++
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );  // throw
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( CodeProfilerService.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( CodeProfilerService.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( CodeProfilerService.spanAt( key, 10, 0 ).stats().count() ).isEqualTo( 0 ); // i++ not run
	}

	@DisplayName( "It splits each binary operand into its own span" )
	@Test
	void testBinaryChain() {
		String source = """
		                foo = "a";
		                bar = "b";
		                bum = "c";
		                quz = "d";
		                result = foo & bar & bum & quz;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testBinaryChain dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 4 assignments (1 span each) + the chain statement splits into 4
		// operands (result = foo / bar / bum / quz each its own span).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 15, true ) );  // "result = foo "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 15, 5, 21, true ) ); // "& bar"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 5, 21, 5, 27, true ) ); // "& bum"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 5, 27, 5, 30, true ) ); // "& quz"

		// Pass B: the whole chain ran, every operand exactly once.
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // result = foo
		assertThat( CodeProfilerService.spanAt( key, 5, 15 ).stats().count() ).isEqualTo( 1 );  // & bar
		assertThat( CodeProfilerService.spanAt( key, 5, 21 ).stats().count() ).isEqualTo( 1 );  // & bum
		assertThat( CodeProfilerService.spanAt( key, 5, 27 ).stats().count() ).isEqualTo( 1 );  // & quz

		// Line-based: line 5 is covered with count 1 (first/outermost span on the line).
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It stops covering spans after a mid-statement throw" )
	@Test
	void testBinaryChainWithThrow() {
		String source = """
		                foo = "a";
		                bar = "b";
		                result = foo & ( function() { throw( "boom" ); } )() & bar;
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the IIFE throws mid-chain
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testBinaryChainWithThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 2 assignments + the chain splits into operands: foo / the IIFE
		// (whose body throw is its own span, and whose braces get their own spans) /
		// bar.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );

		// Pass B: foo ran, the IIFE threw (so it counts as started), bar never ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 10 ).stats().count() ).isEqualTo( 1 );  // "foo" ran
		assertThat( CodeProfilerService.spanAt( key, 3, 15 ).stats().count() ).isEqualTo( 1 );  // "( function() " shell
		assertThat( CodeProfilerService.spanAt( key, 3, 30 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( CodeProfilerService.spanAt( key, 3, 28 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 3, 47 ).stats().count() ).isEqualTo( 1 );  // } ran
		assertThat( CodeProfilerService.spanAt( key, 3, 55 ).stats().count() ).isEqualTo( 0 );  // "bar" never ran

		// Line-based: line 3 is covered (foo + the started IIFE), count from the leading span.
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits an if on one line into condition and branch spans" )
	@Test
	void testIfOneLiner() {
		String source = "if( true ) a = 1;";
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testIfOneLiner dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The if's condition runs, then the single no-brace statement body (a = 1)
		// breaks into its own executable span — it may not run, so it's tracked
		// separately (same as a block body's first statement).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 11, true ) );  // "if( true ) "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 11, 1, 16, true ) ); // "a = 1"

		// Runtime coverage: the condition and the (taken) then-body ran exactly once.
		assertThat( CodeProfilerService.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );  // condition
		assertThat( CodeProfilerService.spanAt( key, 1, 11 ).stats().count() ).isEqualTo( 1 ); // a = 1

		// Line-based: line 1 is covered with count 1.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a multi-line if into condition, then, and else spans" )
	@Test
	void testIfBlock() {
		String source = """
		                if( true ) {
		                a = 1;
		                } else {
		                b = 2;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testIfBlock dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: condition (line 1), then-open brace, then-body a = 1 (line 2),
		// then-close brace, else keyword, else-open brace, else-body b = 2 (line 4),
		// else-close brace — each its own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 11, true ) );  // "if( true ) "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // "a = 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 11, 1, 12, true ) ); // "{" then open
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}" then close
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 2, 3, 6, true ) );   // "else"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 5, true ) );   // "b = 2"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 3, 7, 3, 8, true ) );   // "{" else open
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 1, true ) );   // "}" else close

		// Runtime coverage: condition is true, so then (line 2) ran once, else (line 4) never ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );  // condition
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );  // a = 1 ran
		assertThat( CodeProfilerService.spanAt( key, 4, 1 ).stats().count() ).isEqualTo( 0 );  // b = 2 missed

		// Line-based: line 1 and line 2 covered; line 4 (else) not covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isFalse();
	}

	@DisplayName( "It charges each span's self-time from the probe interval" )
	@Test
	void testTiming() {
		// Each sleep is its own statement/span. Probe-charging: when span N+1's mark
		// fires, the elapsed time since span N's mark is charged to span N.
		String source = """
		                sleep( 500 );
		                sleep( 1000 );
		                sleep( 2000 );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Pass A: three expression statements -> three spans.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 12, true ) );
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) );
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) );

		// Full captured snapshot for debugging.
		System.out.println( "TIMING SNAPSHOT: " + CodeProfilerService.fileSpans( key ) );

		// Each of the three statements ran exactly once.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: each line ran once and is covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );

		// Span 1 was charged while sleep(500) ran; span 2 while sleep(1000) ran.
		// Both >= their sleep, within a generous band for scheduling slop.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().totalNanos() ).isAtLeast( 500L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().totalNanos() ).isAtMost( 750L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 1_000L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 1_500L * 1_000_000L );
	}

	@DisplayName( "It splits a while loop into condition and body spans" )
	@Test
	void testWhile() {
		String source = """
		                i = 0;
		                while( i < 3 ) {
		                i++;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testWhile dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: i=0, the while condition, the body i++, and the two loop braces
		// ({ and }) are each their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "i = 0"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 15, true ) );  // "while( i < 3 ) "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 15, 2, 16, true ) ); // "{" open brace
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}" close brace

		// Pass B: the loop condition ran once (header), body ran 3 times.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // while condition
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // i++ ran 3x
		assertThat( CodeProfilerService.spanAt( key, 2, 15 ).stats().count() ).isEqualTo( 3 );  // open brace ran 3x
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // close brace ran 3x

		// Line-based: lines 1-4 covered (line 4 is the closing brace, ran 3x).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It accumulates per-iteration timing inside a loop body" )
	@Test
	void testWhileTiming() {
		String source = """
		                i = 0;
		                while( i < 3 ) {
		                sleep( 100 );
		                i++;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testWhileTiming dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: i=0, while cond, sleep(100), i++, { and } braces.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );

		// Pass B: body ran 3x — sleep and i++ each counted 3 times.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // sleep( 100 ) ran 3x
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // i++ ran 3x

		// Timing: 3 x 100ms sleeps accumulated into the sleep span (~300ms).
		long sleepNanos = CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos();
		assertThat( sleepNanos ).isAtLeast( 300L * 1_000_000L );
		assertThat( sleepNanos ).isAtMost( 900L * 1_000_000L );

		// Line-based: lines 1-5 covered (line 5 is the closing brace, ran 3x).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
	}

	@DisplayName( "It splits a switch into condition, case, and body spans" )
	@Test
	void testSwitch() {
		String source = """
		                x = 2;
		                switch( x ) {
		                case 1: a = 10; break;
		                case 2: a = 20; break;
		                default: a = 30;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testSwitch dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: x=2, the switch header (with case spillover), each case label +
		// its body/break as separate spans, the default label + body, and the
		// closing brace.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 11 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "x = 2"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 3, 5, true ) );   // switch header + case 1 label spillover
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );   // "case 1: "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 8, 3, 14, true ) );  // "a = 10"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 21, true ) ); // "break"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 8, true ) );   // "case 2: "
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 4, 8, 4, 14, true ) );  // "a = 20"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 4, 16, 4, 21, true ) ); // "break"
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 9, true ) );   // "default: "
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 5, 9, 5, 15, true ) );  // "a = 30"
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 1, true ) );  // "}"

		// Pass B: x=2 matches case 2, so case 2 ran (a=20 + break); case 1 and
		// default did not.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 2
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // switch cond
		assertThat( CodeProfilerService.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // case 1 a=10 missed
		assertThat( CodeProfilerService.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 1 );   // case 2 a=20 ran
		assertThat( CodeProfilerService.spanAt( key, 5, 9 ).stats().count() ).isEqualTo( 0 );   // default a=30 missed

		// Line-based: lines 1-4 covered; line 5 (default, missed) not covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isFalse();
	}

	@DisplayName( "It profiles try/catch/finally block start and end statements" )
	@Test
	void testTry() {
		String source = """
		                try {
		                a = 1;
		                sleep( 100 );
		                b = 2;
		                } catch( any e ) {
		                c = 3;
		                } finally {
		                d = 4;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testTry dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: try keyword, the try body's three statements, the catch keyword,
		// the catch body, the finally keyword, the finally body, and their braces.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 11 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 2, 0, true ) );  // "try {\n"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // "a = 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 12, true ) );  // "sleep( 100 )"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 5, true ) );   // "b = 2"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 1, true ) );   // "}"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 2, 6, 0, true ) );   // "catch( any e ) {\n"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 5, true ) );   // "c = 3"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 1, true ) );   // "}"
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 7, 2, 8, 0, true ) );   // "finally {\n"
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 8, 0, 8, 5, true ) );   // "d = 4"
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 1, true ) );  // "}"

		// Pass B: try body ran (a=1, sleep, b=2); catch did NOT (no throw); finally
		// ALWAYS ran (d=4). The sleep span was charged ~100ms.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // sleep( 100 )
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // b = 2
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 0 );   // c = 3 missed
		assertThat( CodeProfilerService.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 ran

		// Timing: the sleep(100) span was charged at least the 100ms sleep.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );

		// Line-based: try + finally covered; catch not.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 6 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 8 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 8 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles an assert statement and its message" )
	@Test
	void testAssert() {
		String source = """
		                assert( true, "all good" );
		                assert( false, "boom" );
		                x = 1;
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — second assert fails
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testAssert dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: each assert and the trailing x=1 are their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 26, true ) );  // assert( true, "all good" )
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 23, true ) );  // assert( false, "boom" )
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 5, true ) );   // x = 1

		// Pass B: the first assert passed (count 1); the second assert threw before
		// its mark was emitted, and x = 1 never ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );

		// Line-based: line 1 covered; lines 2-3 not.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It splits a for-in loop into collection and body spans" )
	@Test
	void testForIn() {
		String source = """
		                arr = [ 10, 20, 30 ];
		                for( item in arr ) {
		                x = item;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testForIn dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: arr=[...], the for-in header, the body x = item, and the loop
		// braces { and } are each their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 20, true ) );  // "arr = [ 10, 20, 30 ]"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 19, true ) );  // "for( item in arr ) "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );   // "x = item"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 19, 2, 20, true ) ); // "{" open brace
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}" close brace

		// Pass B: header ran once; body ran 3 times (10, 20, 30).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // arr = [...]
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // for-in header
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // x = item ran 3x
		assertThat( CodeProfilerService.spanAt( key, 2, 19 ).stats().count() ).isEqualTo( 3 );  // { ran 3x
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // } ran 3x

		// Line-based: lines 1-4 covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It splits a do-while loop into body and condition spans" )
	@Test
	void testDo() {
		String source = """
		                i = 0;
		                do {
		                i++;
		                } while( i < 2 );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testDo dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: i=0, do, the body i++, the two braces { and }, while(, the
		// condition, and the closing ) are each their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "i = 0"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 3, true ) );   // "do "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 3, 2, 4, true ) );   // "{"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 2, 4, 9, true ) );   // "while( "
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 4, 9, 4, 14, true ) );  // "i < 2"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 4, 14, 4, 16, true ) ); // " )"

		// Pass B: body ran twice (i: 0->1, 1->2); the condition was checked twice
		// (after each body run, the second one failing); do/while(/) ran once each.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // do
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // i++ ran 2x
		assertThat( CodeProfilerService.spanAt( key, 2, 3 ).stats().count() ).isEqualTo( 2 );   // { ran 2x
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 2 );   // } ran 2x
		assertThat( CodeProfilerService.spanAt( key, 4, 2 ).stats().count() ).isEqualTo( 1 );   // while(
		assertThat( CodeProfilerService.spanAt( key, 4, 9 ).stats().count() ).isEqualTo( 2 );   // condition checked 2x
		assertThat( CodeProfilerService.spanAt( key, 4, 14 ).stats().count() ).isEqualTo( 1 );  // )

		// Line-based: lines 1-4 covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 2 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It splits a C-style for loop into header and body spans" )
	@Test
	void testForIndex() {
		String source = """
		                j = 0;
		                for( i = 0; i < 3; i++ ) {
		                j = j + i;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testForIndex dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: j=0, the for header, the body j = j + i (2 spans: LHS+RHS), and the
		// loop braces { and } are each their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "j = 0"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 25, true ) );  // "for( i = 0; i < 3; i++ ) "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );   // "j = j + "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 8, 3, 9, true ) );   // "i"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 2, 25, 2, 26, true ) ); // "{" open brace
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}" close brace

		// Pass B: header ran once; body ran 3 times (i: 0,1,2).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // j = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // for header
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // j = j + ran 3x
		assertThat( CodeProfilerService.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 3 );   // i ran 3x
		assertThat( CodeProfilerService.spanAt( key, 2, 25 ).stats().count() ).isEqualTo( 3 );  // { ran 3x
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // } ran 3x

		// Line-based: lines 1-4 covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It charges block start and end statements inside a nested block" )
	@Test
	void testBlockTiming() {
		String source = """
		                a = 1;
		                if( true ) {
		                b = 2;
		                sleep( 150 );
		                c = 3;
		                }
		                d = 4;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testBlockTiming dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: a=1, if cond, b=2, sleep(150), c=3, { and } braces, d=4.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "a = 1"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 11, true ) );  // "if( true ) "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 5, true ) );   // "b = 2"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 12, true ) );  // "sleep( 150 )"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 5, true ) );   // "c = 3"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 2, 11, 2, 12, true ) ); // "{" open brace
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 1, true ) );   // "}" close brace
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 5, true ) );   // "d = 4"

		// Pass B: all statements in the taken if ran once; the sleep was charged.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // if cond
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // b = 2
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // sleep( 150 )
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // c = 3
		assertThat( CodeProfilerService.spanAt( key, 2, 11 ).stats().count() ).isEqualTo( 1 );  // {
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // }
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4

		// Timing: the sleep(150) span's self-time >= 150ms (charged from the probe
		// interval when the NEXT span's mark fires).
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtLeast( 150L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtMost( 450L * 1_000_000L );

		// Line-based: lines 1-7 covered once (line 6 is the braces, ran once).
		for ( int line : new int[] { 1, 2, 3, 4, 5, 6, 7 } ) {
			assertThat( CodeProfilerService.lineAt( key, line ).covered() ).isTrue();
			assertThat( CodeProfilerService.lineAt( key, line ).count() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "It charges the final span via the closing markEnd" )
	@Test
	void testMarkEndChargesFinalSpan() {
		String source = """
		                sleep( 100 );
		                sleep( 200 );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testMarkEndChargesFinalSpan dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Two spans; the final span's interval is closed by markEnd, so it must
		// carry the sleep(200) time.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 12, true ) );

		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );

		// The final span (line 2) was charged the full 200ms by markEnd.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
	}

	@DisplayName( "It charges a sleep before a loop from the loop's entry mark" )
	@Test
	void testSleepBeforeLoopChargedByEntryMark() {
		String source = """
		                i = 0;
		                sleep( 100 );
		                while( i < 1 ) {
		                i++;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testSleepBeforeLoopChargedByEntryMark dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The sleep(100) span (line 2) is charged when the while's entry mark fires.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// The loop itself ran once.
		assertThat( CodeProfilerService.spanAt( key, 3, 1 ).stats().count() ).isEqualTo( 1 );   // while cond
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // i++
	}

	@DisplayName( "It multiplies counts across nested while loops" )
	@Test
	void testNestedWhile() {
		String source = """
		                i = 0;
		                while( i < 3 ) {
		                j = 0;
		                while( j < 2 ) {
		                j++;
		                }
		                i++;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testNestedWhile dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 10 spans (including the brace spans and the nested-loop headers).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 10 );

		// Outer while ran 3x: j=0 (line 3) and inner header (line 4) each 3x;
		// inner body j++ (line 5) ran 3x2=6; outer body i++ (line 7) ran 3x.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // i = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );  // outer cond
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );  // j = 0
		assertThat( CodeProfilerService.spanAt( key, 4, 1 ).stats().count() ).isEqualTo( 3 );  // inner cond
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 6 );  // j++ ran 6x
		assertThat( CodeProfilerService.spanAt( key, 4, 15 ).stats().count() ).isEqualTo( 6 ); // inner { ran 6x
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 6 );  // inner } ran 6x
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 3 );  // i++ ran 3x
		assertThat( CodeProfilerService.spanAt( key, 2, 15 ).stats().count() ).isEqualTo( 3 ); // outer { ran 3x
		assertThat( CodeProfilerService.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 3 );  // outer } ran 3x

		// Line-based.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 6 );
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 7 ).count() ).isEqualTo( 3 );
	}

	@DisplayName( "It marks the break statement and stops the loop" )
	@Test
	void testBreak() {
		String source = """
		                i = 0;
		                while( true ) {
		                i++;
		                break;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testBreak dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// i++ and break each ran once (loop broke after one iteration).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // while cond
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // i++
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // break
		assertThat( CodeProfilerService.spanAt( key, 2, 14 ).stats().count() ).isEqualTo( 1 );  // {
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // }

		// Line-based: lines 1-5 covered (line 5 is the closing brace, ran once).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
	}

	@DisplayName( "It never runs statements after continue" )
	@Test
	void testContinue() {
		String source = """
		                i = 0;
		                while( i < 3 ) {
		                i++;
		                continue;
		                i = 99;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testContinue dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// i++ and continue each ran 3x; i=99 never ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // i++
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // continue
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 0 );   // i = 99

		// Line-based: line 5 not covered.
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isFalse();
	}

	@DisplayName( "It does not run a zero-iteration while body" )
	@Test
	void testWhileZeroIterations() {
		String source = """
		                i = 0;
		                while( false ) {
		                i++;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testWhileZeroIterations dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Condition ran once; body never ran.
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // cond
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );   // i++

		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It does not run a zero-iteration for body" )
	@Test
	void testForIndexZeroIterations() {
		String source = """
		                j = 0;
		                for( i = 0; i < 0; i++ ) {
		                j++;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testForIndexZeroIterations dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Header ran once; body never ran.
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // header
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );   // j++

		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It runs the catch body when the try body throws" )
	@Test
	void testTryCatchOnThrow() {
		String source = """
		                try {
		                a = 1;
		                throw( "boom" );
		                b = 2;
		                } catch( any e ) {
		                c = 3;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testTryCatchOnThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// a=1 ran, throw ran, b=2 (after throw) never ran, c=3 (catch) ran.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // throw
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 0 );   // b = 2
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // c = 3

		// Line-based.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 6 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 6 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It runs the finally body even when the exception propagates" )
	@Test
	void testFinallyOnPropagatingThrow() {
		String source = """
		                try {
		                a = 1;
		                throw( "boom" );
		                } finally {
		                d = 4;
		                }
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the throw propagates out of the script after finally ran
		}

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyOnPropagatingThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The finally body IS executed by Java semantics when the try throws. The
		// BoxTryTransformer compiles the finally body in three bytecode copies
		// (inline-after-try, inline-after-catch, exceptional handler); the mark
		// emission must exist in ALL of them so whichever executes fires the count.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // throw
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 in finally

		// Line-based: line 5 (finally) covered.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It runs the finally body once when the exception is caught" )
	@Test
	void testFinallyOnCaughtThrow() {
		String source = """
		                try {
		                a = 1;
		                throw( "boom" );
		                } catch( any e ) {
		                c = 3;
		                } finally {
		                d = 4;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyOnCaughtThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// a=1 ran, the throw ran, the catch c=3 ran, and the finally d=4 ran exactly
		// once (via the catch's inlined-finally copy — the try's inline copy never
		// ran because the throw skipped it).
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // throw
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // c = 3 catch
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 finally

		// Line-based: catch and finally both covered once.
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 7 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It runs the switch default when no case matches" )
	@Test
	void testSwitchDefault() {
		String source = """
		                x = 99;
		                switch( x ) {
		                case 1: a = 10; break;
		                default: a = 30;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testSwitchDefault dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// case 1 missed, default body a=30 ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 99
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // switch cond
		assertThat( CodeProfilerService.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // case 1 missed
		assertThat( CodeProfilerService.spanAt( key, 4, 9 ).stats().count() ).isEqualTo( 1 );   // default a = 30

		// Line-based: line 4 (default) covered.
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It charges a sleep inside a matching switch case" )
	@Test
	void testSwitchCaseTiming() {
		String source = """
		                x = 2;
		                switch( x ) {
		                case 1: sleep( 100 ); break;
		                case 2: sleep( 150 ); break;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// case 2 matched: its sleep(150) span ran once and was charged ~150ms.
		assertThat( CodeProfilerService.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 1 );   // sleep( 150 )
		assertThat( CodeProfilerService.spanAt( key, 4, 8 ).stats().totalNanos() ).isAtLeast( 150L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 4, 8 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
		// case 1 missed.
		assertThat( CodeProfilerService.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // sleep( 100 ) in case 1
	}

	@DisplayName( "It accumulates sleep timing across do-while iterations" )
	@Test
	void testDoTiming() {
		String source = """
		                i = 0;
		                do {
		                sleep( 100 );
		                i++;
		                } while( i < 2 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testDoTiming dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Body ran 2x; the sleep(100) span accumulated ~200ms.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // sleep( 100 )
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 600L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 2 );   // i++
		assertThat( CodeProfilerService.spanAt( key, 5, 9 ).stats().count() ).isEqualTo( 2 );   // cond checked 2x

		// Line-based.
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 2 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
	}

	@DisplayName( "It accumulates sleep timing across for-index iterations" )
	@Test
	void testForIndexTiming() {
		String source = """
		                j = 0;
		                for( i = 0; i < 2; i++ ) {
		                sleep( 100 );
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testForIndexTiming dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Body ran 2x; the sleep(100) span accumulated ~200ms.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // sleep( 100 )
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 600L * 1_000_000L );

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 2 );
	}

	@DisplayName( "It accumulates sleep timing across for-in iterations" )
	@Test
	void testForInTiming() {
		String source = """
		                arr = [ 10, 20 ];
		                for( item in arr ) {
		                sleep( 100 );
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Body ran 2x; the sleep(100) span accumulated ~200ms.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // sleep( 100 )
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 600L * 1_000_000L );

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 2 );
	}

	@DisplayName( "It splits a comparison into left and right spans" )
	@Test
	void testComparison() {
		String source = """
		                a = 1;
		                b = 2;
		                x = a == b;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testComparison dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: a=1, b=2, and the comparison splits into left ("x = a == ") and
		// right ("b") spans because the right operand could throw.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 9, true ) );   // "x = a == "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 9, 3, 10, true ) );  // "b"

		// Pass B: both ran once.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 1 );

		// Line-based.
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It leaves the comparison right span missed when it throws" )
	@Test
	void testComparisonRightThrows() {
		String source = """
		                a = 1;
		                try {
		                x = a == ( function() { throw( "boom" ); } )();
		                } catch( any e ) {
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testComparisonRightThrows dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The comparison's left span ran (a == evaluated); the right operand (IIFE)
		// threw before its mark, so its span and everything after stayed missed.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = a ==
		assertThat( CodeProfilerService.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 1 );   // "( function() " shell
		assertThat( CodeProfilerService.spanAt( key, 3, 24 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( CodeProfilerService.spanAt( key, 3, 22 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 3, 41 ).stats().count() ).isEqualTo( 1 );  // } ran

		// Line-based: line 3 covered (left ran).
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
	}

	@DisplayName( "It marks the elvis right span when the left is null" )
	@Test
	void testElvisRightRuns() {
		String source = """
		                foo = null;
		                bar = 2;
		                x = foo ?: ( bar = 99 );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testElvisRightRuns dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: foo=null, bar=2, and the elvis splits: "x = foo ?: " then
		// "( bar = 99 )" (paren unwrapped to the inner assignment).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) );  // "x = foo ?: "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 13, 3, 23, true ) ); // "bar = 99"

		// Pass B: foo is null so the right RAN and its mark fired.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 13 ).stats().count() ).isEqualTo( 1 );
	}

	@DisplayName( "It short-circuits the elvis right span when the left is set" )
	@Test
	void testElvisRightShortCircuits() {
		String source = """
		                foo = 1;
		                bar = 2;
		                x = foo ?: ( bar = 99 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// foo is set so the right never ran — its span is MISSED.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 13 ).stats().count() ).isEqualTo( 0 );
	}

	@DisplayName( "It short-circuits && so the right span is missed" )
	@Test
	void testAndShortCircuit() {
		String source = """
		                a = false;
		                b = 1;
		                x = a && b;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testAndShortCircuit dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// a false -> b never ran (MISSED).
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 0 );
	}

	@DisplayName( "It marks both && operands when both run" )
	@Test
	void testAndBothRun() {
		String source = """
		                a = true;
		                b = 1;
		                x = a && b;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testAndBothRun dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 1 );
	}

	@DisplayName( "It short-circuits || so the right span is missed" )
	@Test
	void testOrShortCircuit() {
		String source = """
		                a = true;
		                b = 1;
		                x = a || b;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testOrShortCircuit dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// a true -> b never ran (MISSED).
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 0 );
	}

	@DisplayName( "It marks a compound array literal as one span when all elements run" )
	@Test
	void testArrayLiteralAllRun() {
		String source = """
		                a = 1;
		                b = 2;
		                x = [ a, b, 3 ];
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testArrayLiteralAllRun dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The literal is one span (all identifiers/literals, no throw) and ran once.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It leaves trailing array elements missed when a middle element throws" )
	@Test
	void testArrayLiteralMidThrow() {
		String source = """
		                a = 1;
		                try {
		                x = [ a, ( function() { throw( "boom" ); } )(), 3 ];
		                } catch( any e ) {
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testArrayLiteralMidThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// a ran, the IIFE started (threw), the trailing 3 never ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = [ a, (
		assertThat( CodeProfilerService.spanAt( key, 3, 24 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( CodeProfilerService.spanAt( key, 3, 22 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 3, 41 ).stats().count() ).isEqualTo( 1 );  // } ran

		// Line-based: line 3 covered (a ran before the throw).
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
	}

	@DisplayName( "It marks a struct literal as one span when all values run" )
	@Test
	void testStructLiteralAllRun() {
		String source = """
		                a = 1;
		                b = 2;
		                x = { one: a, two: b };
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testStructLiteralAllRun dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It leaves trailing struct values missed when a middle value throws" )
	@Test
	void testStructLiteralMidThrow() {
		String source = """
		                a = 1;
		                try {
		                x = { one: a, two: ( function() { throw( "boom" ); } )() };
		                } catch( any e ) {
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testStructLiteralMidThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// a ran, the IIFE started (threw).
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = { one: a, two: (
		assertThat( CodeProfilerService.spanAt( key, 3, 34 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( CodeProfilerService.spanAt( key, 3, 32 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 3, 51 ).stats().count() ).isEqualTo( 1 );  // } ran

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
	}

	@DisplayName( "It marks a spread array literal as one span when all run" )
	@Test
	void testSpreadAllRun() {
		String source = """
		                a = [ 1, 2 ];
		                b = 3;
		                x = [ ...a, b ];
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testSpreadAllRun dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It leaves trailing spread elements missed when a middle element throws" )
	@Test
	void testSpreadMidThrow() {
		String source = """
		                a = [ 1, 2 ];
		                b = 3;
		                try {
		                x = [ ...a, ( function() { throw( "boom" ); } )(), b ];
		                } catch( any e ) {
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testSpreadMidThrow dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// the spread a ran, the IIFE started (threw), trailing b never ran.
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // x = [ ...a, (
		assertThat( CodeProfilerService.spanAt( key, 4, 27 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( CodeProfilerService.spanAt( key, 4, 25 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 4, 44 ).stats().count() ).isEqualTo( 1 );  // } ran

		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It profiles a string interpolation as one span" )
	@Test
	void testStringInterpolation() {
		String source = """
		                a = 1;
		                x = "val #a# now";
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testStringInterpolation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a ternary inside a binary RHS and misses the untaken branch" )
	@Test
	void testTernaryInBinaryRHS() {
		String source = """
		                a = 1;
		                b = true;
		                c = 2;
		                x = a + ( b ? c : 0 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testTernaryInBinaryRHS dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// x = a + ( (line 4 cols 0-10), b ? (10-14), c : (14-18), 0 (18-21, the
		// untaken false branch).
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // x = a + (
		assertThat( CodeProfilerService.spanAt( key, 4, 10 ).stats().count() ).isEqualTo( 1 );  // b ?
		assertThat( CodeProfilerService.spanAt( key, 4, 14 ).stats().count() ).isEqualTo( 1 );  // c : (taken)
		assertThat( CodeProfilerService.spanAt( key, 4, 18 ).stats().count() ).isEqualTo( 0 );  // 0 (untaken)
	}

	@DisplayName( "It splits a ternary inside a comparison RHS and misses the untaken branch" )
	@Test
	void testTernaryInComparisonRHS() {
		String source = """
		                a = 1;
		                b = true;
		                c = 2;
		                x = a == ( b ? c : 0 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testTernaryInComparisonRHS dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // x = a == (
		assertThat( CodeProfilerService.spanAt( key, 4, 11 ).stats().count() ).isEqualTo( 1 );  // b ?
		assertThat( CodeProfilerService.spanAt( key, 4, 15 ).stats().count() ).isEqualTo( 1 );  // c : (taken)
		assertThat( CodeProfilerService.spanAt( key, 4, 19 ).stats().count() ).isEqualTo( 0 );  // 0 (untaken)
	}

	@DisplayName( "It profiles unary not, negation, and parens as single spans" )
	@Test
	void testUnaryNegateParen() {
		String source = """
		                a = false;
		                b = 5;
		                c = 1;
		                d = 2;
		                x = !a;
		                y = -b;
		                z = ( c + d );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testUnaryNegateParen dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Each expression statement is a single span (unary/negate/paren have one
		// sub-expression), all ran once.
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // !a
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // -b
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // ( c + d )

		for ( int line = 1; line <= 7; line++ ) {
			assertThat( CodeProfilerService.lineAt( key, line ).covered() ).isTrue();
			assertThat( CodeProfilerService.lineAt( key, line ).count() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "It profiles null-safe dot access as a single span" )
	@Test
	void testNullSafeDotAccess() {
		String source = """
		                s = {};
		                x = s?.foo;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testNullSafeDotAccess dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The null-safe access on a missing key ran (returned null) — count 1.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It leaves the array index span missed when the index throws" )
	@Test
	void testArrayAccessIndexThrows() {
		String source = """
		                arr = [ 1, 2, 3 ];
		                try {
		                x = arr[ ( function() { throw( "boom" ); } )() ];
		                } catch( any e ) {
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testArrayAccessIndexThrows dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The array access started, the index IIFE threw before its mark, and the
		// closing access never completed.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = arr[ (
		assertThat( CodeProfilerService.spanAt( key, 3, 24 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( CodeProfilerService.spanAt( key, 3, 22 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 3, 41 ).stats().count() ).isEqualTo( 1 );  // } ran

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
	}

	@DisplayName( "It profiles an empty function declaration as one span" )
	@Test
	void testFunctionEmptyDeclaration() {
		String source = """
		                function foo() {}
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionEmptyDeclaration dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// An empty function declaration body has no executable spans — the shell has
		// no statements and the body is empty, so there is nothing to profile. As a
		// result, no line coverage is recorded for line 1.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).isEmpty();
		assertThat( CodeProfilerService.fileLines( key ) ).isEmpty();
	}

	@DisplayName( "It splits a function declaration shell from its body span" )
	@Test
	void testFunctionShellAndBody() {
		String source = """
		                function foo() {
		                x = 1;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionShellAndBody dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the declaration shell (before the brace), the body statement, and the
		// two braces are separate spans; the shell runs at declaration, the body +
		// braces only when the function is invoked.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );  // "function foo() "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // "x = 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 16, true ) ); // "{"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}"

		// Pass B: shell ran; body + braces did NOT (never invoked).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );  // body missed
		assertThat( CodeProfilerService.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 0 ); // { missed
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );  // } missed

		// Line-based: line 1 covered (shell). Lines 2-3 only touched by missed body
		// and brace spans, so they are not covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It keeps plain args inside the declaration shell span" )
	@Test
	void testFunctionPlainArgs() {
		String source = """
		                function foo( a, b ) {
		                x = a;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionPlainArgs dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Plain args (no defaults) stay in the shell — no extra arg spans. The brace
		// spans and the body statement are their own spans.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 21, true ) );  // shell incl. args
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // body x = a
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 21, 1, 22, true ) ); // "{"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}"

		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: line 1 covered (shell, once). Lines 2-3 are only touched by
		// missed body and brace spans, so they are NOT covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It keeps literal defaults inside the declaration shell span" )
	@Test
	void testFunctionLiteralDefaults() {
		String source = """
		                function foo( a = 1, b = 2 ) {
		                x = a + b;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionLiteralDefaults dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Literal defaults evaluate inline at declaration (cannot throw), so they
		// stay inside the shell span. The body splits per its own expressions, and
		// the braces get their own spans.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 29, true ) );  // shell + literal defaults
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 8, true ) );   // "x = a + "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 8, 2, 9, true ) );   // "b"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 29, 1, 30, true ) ); // "{"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}"

		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: line 1 covered (shell, once). Lines 2-3 are only touched by
		// missed body and brace spans, so they are NOT covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It profiles the function body only when the function is invoked" )
	@Test
	void testFunctionBodyRunsOnInvocation() {
		String source = """
		                function foo() {
		                x = 1;
		                }
		                foo();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionBodyRunsOnInvocation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Body span ran exactly once (one invocation).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // body
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // foo() call

		// Line-based: line 2 covered (body ran).
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It leaves the body span missed when the function is never invoked" )
	@Test
	void testFunctionBodyMissedWhenNeverInvoked() {
		String source = """
		                function foo() {
		                x = 1;
		                }
		                y = 2;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionBodyMissedWhenNeverInvoked dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );  // body missed
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2

		// Line 2 is only touched by the missed body span — NOT covered.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isFalse();
	}

	@DisplayName( "It increments the body span count per invocation" )
	@Test
	void testFunctionBodyCountPerInvocation() {
		String source = """
		                function foo() {
		                x = 1;
		                }
		                foo();
		                foo();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionBodyCountPerInvocation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Body ran twice (two invocations); each call site ran once.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 2 );  // body
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // foo() 1
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // foo() 2

		// Line 2's aggregate is the body span's count (2) — the shell only covers
		// line 1.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 2 );
	}

	@DisplayName( "It charges a single UDF body statement's self-time" )
	@Test
	void testFunctionBodyTiming() {
		String source = """
		                function foo() {
		                sleep( 150 );
		                }
		                foo();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The body's sleep(150) span was charged ~150ms (its interval closed by the
		// UDF body's markEnd).
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 150L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 450L * 1_000_000L );

		// The CALL SITE (line 4) only carries the dispatch overhead — the sleep is
		// charged to the body span, so the call span must be far below the sleep.
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// Line-based: line 1 covered (shell); line 2 covered (body ran); line 4
		// covered (call site). Line 2's aggregate is the shell tail's count.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It charges first and last UDF body statements including markEnd" )
	@Test
	void testFunctionBodyTimingFirstAndLast() {
		String source = """
		                function foo() {
		                sleep( 100 );
		                sleep( 200 );
		                }
		                foo();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// First body statement (line 2): charged while sleep(100) ran.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// Last body statement (line 3): charged while sleep(200) ran, closed by the
		// UDF body's markEnd.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );

		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );

		// The CALL SITE (line 5) only carries dispatch overhead, not the sleeps.
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// Line-based: line 1 covered (shell); lines 2-3 covered (body ran); line 5
		// covered (call site).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a non-literal default into its own span" )
	@Test
	void testFunctionDynamicDefaultSplitsShell() {
		String source = """
		                function foo( a = bar ) {
		                x = a;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionDynamicDefaultSplitsShell dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the shell splits around the non-literal default, isolating it:
		// "function foo( a = " | "bar" | ") {" | the body. The shell head and the
		// tail ") {" run atomically at declaration (batched into one varargs mark);
		// the default `bar` may not run at declaration (it's a lazy defaultExpr).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) );   // shell head "function foo( a = "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 18, 1, 21, true ) );  // default "bar"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 21, 1, 24, true ) );  // shell tail " ) "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );    // body
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 24, 1, 25, true ) );  // "{"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );    // "}"

		// Pass B: the shell head and tail ran; the default and body did NOT (never invoked).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell head ran
		assertThat( CodeProfilerService.spanAt( key, 1, 18 ).stats().count() ).isEqualTo( 0 );  // default missed
		assertThat( CodeProfilerService.spanAt( key, 1, 21 ).stats().count() ).isEqualTo( 1 );  // shell tail ran
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );   // body missed

		// Line-based: line 1 covered (shell head ran). Lines 2-3 only touched by
		// missed body/brace spans — NOT covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It evaluates and marks a dynamic default only when the arg is omitted" )
	@Test
	void testFunctionDynamicDefaultRunsOnOmit() {
		String source = """
		                function foo( a = now() ) {
		                x = a;
		                }
		                foo();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// foo() omits the arg, so the default's defaultExpr_N method ran — the
		// default span (line 1 col 18) counts 1.
		assertThat( CodeProfilerService.spanAt( key, 1, 18 ).stats().count() ).isEqualTo( 1 );  // default ran
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // body ran

		// Line-based: line 1 covered (shell + default ran); line 2 covered (body
		// ran); line 4 covered (call site). Line 2's aggregate is the shell tail.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It skips a dynamic default when the arg is passed" )
	@Test
	void testFunctionDynamicDefaultSkippedWhenPassed() {
		String source = """
		                function foo( a = now() ) {
		                x = a;
		                }
		                foo( 1 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// foo( 1 ) passes the arg, so the default never ran — default span count 0.
		assertThat( CodeProfilerService.spanAt( key, 1, 18 ).stats().count() ).isEqualTo( 0 );  // default skipped
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // body ran
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // foo( 1 )

		// Line-based: line 1 covered (shell ran; default missed but lineAt reports
		// covered via the shell span); line 2 covered (body ran); line 4 covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It isolates many complex defaults with interleaved shell fragments" )
	@Test
	void testFunctionManyComplexDefaults() {
		String source = """
		                function foo( a, b = 1, c = now(), d = 2, e = new src.test.resources.profiler.ProfilerComplex(), f = 3, g = hash( "x" ), h = 4, i = [ 1, 2, 3 ], j = 5 ) {
		                x = a;
		                }
		                foo();
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testFunctionManyComplexDefaults dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the single-line declaration shell splits at EACH non-literal
		// default, isolating each complex default (now(), new ...ProfilerComplex(),
		// hash("x")) into its OWN span — they are compiled to lazy defaultExpr_N
		// methods that may not run at declaration (only when the arg is omitted at
		// call time). Literal defaults (b=1, d=2, f=3, h=4, j=5) and the all-literal
		// array (i=[1,2,3]) stay inline. The HEAD + INTERSTITIAL + TAIL shell
		// fragments run atomically at declaration; they are batched into ONE varargs
		// mark(fileId, 0, 2, 4, 6).
		//
		// span 0: (1,0)-(1,28) function foo( a, b = 1, c = shell head
		// span 1: (1,28)-(1,33) now() lazy default (defaultExpr)
		// span 2: (1,33)-(1,46) , d = 2, e = shell interstitial
		// span 3: (1,46)-(1,95) new ...ProfilerComplex() lazy default (defaultExpr)
		// span 4: (1,95)-(1,108) , f = 3, g = shell interstitial
		// span 5: (1,108)-(1,119) hash( "x" ) lazy default (defaultExpr)
		// span 6: (1,119)-(1,153) , h = 4, i = [ 1, 2, 3 ], j = 5 ) shell tail
		// span 7: (2,0)-(2,5) x = a; body (runs at invocation)
		// span 8: (1,153)-(1,154) { function body open brace
		// span 9: (3,0)-(3,1) } function body close brace
		// span 10: (4,0)-(4,5) foo() call site
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 11 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 28, true ) );    // shell head
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 28, 1, 33, true ) );   // now() default
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 33, 1, 46, true ) );   // shell interstitial
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 46, 1, 95, true ) );   // new ...() default
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 95, 1, 108, true ) );  // shell interstitial
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 1, 108, 1, 119, true ) ); // hash( "x" ) default
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 1, 119, 1, 153, true ) );  // shell tail
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );      // body x = a
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 1, 153, 1, 154, true ) ); // "{"
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );      // "}"
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 5, true ) );     // foo() call

		// Pass B: foo() omits ALL args, so every lazy default ran. The shell head,
		// interstitials, and tail were batched into one varargs mark (each count 1).
		// foo() omits all args, so every lazy default ran — each count 1.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );    // shell head
		assertThat( CodeProfilerService.spanAt( key, 1, 28 ).stats().count() ).isEqualTo( 1 );   // now() default ran
		assertThat( CodeProfilerService.spanAt( key, 1, 33 ).stats().count() ).isEqualTo( 1 );   // shell interstitial
		assertThat( CodeProfilerService.spanAt( key, 1, 46 ).stats().count() ).isEqualTo( 1 );   // new ...() default ran
		assertThat( CodeProfilerService.spanAt( key, 1, 95 ).stats().count() ).isEqualTo( 1 );   // shell interstitial
		assertThat( CodeProfilerService.spanAt( key, 1, 108 ).stats().count() ).isEqualTo( 1 );  // hash( "x" ) default ran
		assertThat( CodeProfilerService.spanAt( key, 1, 119 ).stats().count() ).isEqualTo( 1 );  // shell tail
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );    // body x = a
		assertThat( CodeProfilerService.spanAt( key, 1, 153 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );    // } ran
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );    // foo() call

		// Line-based: line 1 covered (declaration + all defaults ran); line 2 covered
		// (body); line 3 covered (closing brace); line 4 covered (call site).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It splits a closure shell from its body span" )
	@Test
	void testClosureShellAndBody() {
		String source = """
		                foo = ( x ) => x + 1;
		                y = 2;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testClosureShellAndBody dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the closure creation shell "foo = ( x ) => " (line 1 cols 0-15)
		// and the expression body "x + 1" (line 1 cols 15-20) are separate spans.
		// The body only runs at invocation.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );   // "foo = ( x ) => "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 20, true ) );  // "x + 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // "y = 2"

		// Pass B: the closure creation ran (shell count 1); the body did NOT (never
		// invoked) — count 0.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( CodeProfilerService.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 0 );  // body missed
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2

		// Line-based: line 1 covered (shell ran); line 2 covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks the closure body span when the closure is invoked" )
	@Test
	void testClosureBodyRunsOnInvocation() {
		String source = """
		                foo = ( x ) => x + 1;
		                result = foo( 2 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testClosureBodyRunsOnInvocation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The closure body ran once (one invocation); the call site ran once.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( CodeProfilerService.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 1 );  // body ran
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // call site

		// Line-based: both lines covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a closure block body into statement spans" )
	@Test
	void testClosureBlockBody() {
		String source = """
		                foo = ( x ) => {
		                return x + 1;
		                };
		                result = foo( 2 );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testClosureBlockBody dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: shell (line 1), body return (line 2), { and } braces, call (line 4).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );  // shell
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 12, true ) ); // "return x + 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 16, true ) ); // "{"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );  // "}"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 17, true ) ); // call

		// Pass B: body return ran once; braces ran once; call site ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: lines 1-4 covered (line 3 is the closing brace, ran once).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It keeps a closure literal default in the shell and runs the body" )
	@Test
	void testClosureLiteralDefault() {
		String source = """
		                foo = ( x = 1 ) => x;
		                result = foo();
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testClosureLiteralDefault dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Literal default stays in the shell: "foo = ( x = 1 ) => " (0-19) and body
		// "x" (19-20). foo() omits the arg; the literal default evaluates inline.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 19, true ) );   // shell + literal default
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 19, 1, 20, true ) );  // body "x"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );   // foo()

		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 1, 19 ).stats().count() ).isEqualTo( 1 );   // body ran
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // foo()
	}

	@DisplayName( "It splits a lambda shell from its body span" )
	@Test
	void testLambdaShellAndBody() {
		String source = """
		                foo = ( x ) -> x + 1;
		                y = 2;
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLambdaShellAndBody dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Same span structure as closures: shell (0-15) + body (15-20) + y=2.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );   // shell
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 20, true ) );  // body "x + 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // y = 2

		// Never invoked -> body missed.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 0 );

		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks the lambda body span when the lambda is invoked" )
	@Test
	void testLambdaBodyRunsOnInvocation() {
		String source = """
		                foo = ( x ) -> x + 1;
		                result = foo( 2 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLambdaBodyRunsOnInvocation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( CodeProfilerService.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 1 );  // body ran
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // call site

		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It charges a lambda body sleep and keeps the call site light" )
	@Test
	void testLambdaBodyTiming() {
		String source = """
		                foo = ( x ) -> {
		                sleep( 100 );
		                return x;
		                };
		                result = foo( 2 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Body sleep(100) span charged ~100ms (closed by the lambda markEnd).
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// The return statement ran once.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );

		// The CALL SITE (line 5) only carries dispatch overhead — far below sleep.
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// Line-based: lines 1-3 and 5 covered; line 4 (spillover) not.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It registers a local class blueprint and runs static code at class load" )
	@Test
	void testLocalClassStaticRunsOnLoad() {
		String source = """
		                class brad {
		                static {
		                x = 1;
		                }
		                }
		                b = new brad();
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLocalClassStaticRunsOnLoad dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// The class's spans live under the OUTER SCRIPT's blueprint (a local class
		// is part of its container from the user's perspective).
		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( scriptKey );

		// The static block's statement ran at class load (triggered by new brad()).
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = 1
		assertThat( CodeProfilerService.spanAt( scriptKey, 6, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
	}

	@DisplayName( "It runs the class body only on instantiation" )
	@Test
	void testLocalClassBodyRunsOnInstantiation() {
		String source = """
		                class brad {
		                y = 2;
		                }
		                b = new brad();
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLocalClassBodyRunsOnInstantiation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( scriptKey );

		// y = 2 (class body) ran once at instantiation.
		assertThat( CodeProfilerService.spanAt( scriptKey, 2, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2
		assertThat( CodeProfilerService.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
	}

	@DisplayName( "It runs the member method body only on invocation" )
	@Test
	void testLocalClassMemberRunsOnInvocation() {
		String source = """
		                class brad {
		                function member() {
		                z = 3;
		                }
		                }
		                b = new brad();
		                b.member();
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLocalClassMemberRunsOnInvocation dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( scriptKey );

		// Member body ran once (one invocation); new brad() and b.member() call
		// sites ran once each.
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 1 );   // z = 3
		assertThat( CodeProfilerService.spanAt( scriptKey, 6, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
		assertThat( CodeProfilerService.spanAt( scriptKey, 7, 0 ).stats().count() ).isEqualTo( 1 );  // b.member()
	}

	@DisplayName( "It leaves class body and member spans missed when never instantiated" )
	@Test
	void testLocalClassNeverInstantiated() {
		String source = """
		                class brad {
		                static {
		                x = 1;
		                }
		                y = 2;
		                function member() {
		                z = 3;
		                }
		                }
		                w = 9;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLocalClassNeverInstantiated dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( scriptKey );

		// Never instantiated or invoked: body + member missed. Static block only
		// runs if the class LOADS (it doesn't here — nothing references brad).
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 0 );   // x = 1 static
		assertThat( CodeProfilerService.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 0 );   // y = 2 body
		assertThat( CodeProfilerService.spanAt( scriptKey, 7, 0 ).stats().count() ).isEqualTo( 0 );   // z = 3 member
		assertThat( CodeProfilerService.spanAt( scriptKey, 10, 0 ).stats().count() ).isEqualTo( 1 );  // w = 9
	}

	@DisplayName( "It charges class static, body, and member timing separately" )
	@Test
	void testLocalClassTiming() {
		String source = """
		                class brad {
		                static {
		                sleep( 50 );
		                }
		                y = 2;
		                function member() {
		                sleep( 100 );
		                }
		                }
		                b = new brad();
		                b.member();
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testLocalClassTiming dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// Static sleep(50) charged ~50ms at class load.
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 0 ).stats().totalNanos() ).isAtLeast( 50L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 0 ).stats().totalNanos() ).isAtMost( 250L * 1_000_000L );

		// Body statement ran once at instantiation.
		assertThat( CodeProfilerService.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2

		// Member sleep(100) charged ~100ms at invocation.
		assertThat( CodeProfilerService.spanAt( scriptKey, 7, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( scriptKey, 7, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( scriptKey, 7, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// Call sites ran once.
		assertThat( CodeProfilerService.spanAt( scriptKey, 10, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
		assertThat( CodeProfilerService.spanAt( scriptKey, 11, 0 ).stats().count() ).isEqualTo( 1 );  // b.member()
	}

	@DisplayName( "It tracks a literal property default at class load" )
	@Test
	void testPropertyLiteralDefault() {
		String source = """
		                class brad {
		                property name="x" default=42;
		                }
		                b = new brad();
		                v = b.x;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testPropertyLiteralDefault dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// The property default `42` is its own span in the script blueprint; it ran
		// at class load (count 1).
		assertThat( CodeProfilerService.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 1 );   // default=42
		assertThat( CodeProfilerService.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
		assertThat( CodeProfilerService.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 1 );   // v = b.x
	}

	@DisplayName( "It tracks a complex array-literal property default at class load" )
	@Test
	void testPropertyArrayLiteralDefault() {
		String source = """
		                class brad {
		                property name="x" default=[ 1, 2, 3 ];
		                }
		                b = new brad();
		                v = b.x;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testPropertyArrayLiteralDefault dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// The array-literal default ran at class load.
		assertThat( CodeProfilerService.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 1 );   // default=[ 1, 2, 3 ]
		assertThat( CodeProfilerService.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
	}

	@DisplayName( "It tracks a complex struct-literal property default at class load" )
	@Test
	void testPropertyStructLiteralDefault() {
		String source = """
		                class brad {
		                property name="x" default={ one: 1 };
		                }
		                b = new brad();
		                v = b.x;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testPropertyStructLiteralDefault dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// The struct-literal default ran at class load.
		assertThat( CodeProfilerService.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 1 );   // default={ one: 1 }
		assertThat( CodeProfilerService.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
	}

	@DisplayName( "It tracks a non-literal identifier property default" )
	@Test
	void testPropertyIdentifierDefault() {
		String source = """
		                seed = 42;
		                class brad {
		                property name="x" default=seed type="string";
		                }
		                b = new brad();
		                v = b.x;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testPropertyIdentifierDefault dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// seed=42 (line 1) and the identifier default (line 3 col 26) both ran.
		assertThat( CodeProfilerService.spanAt( scriptKey, 1, 0 ).stats().count() ).isEqualTo( 1 );   // seed = 42
		assertThat( CodeProfilerService.spanAt( scriptKey, 3, 26 ).stats().count() ).isEqualTo( 1 );  // default=seed
		assertThat( CodeProfilerService.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
	}

	@DisplayName( "It leaves the property default missed when the class never loads" )
	@Test
	void testPropertyDefaultNeverLoads() {
		String source = """
		                class brad {
		                property name="x" default=42;
		                }
		                w = 9;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testPropertyDefaultNeverLoads dump" );
		// System.out.print( CodeProfilerService.dumpSpans( scriptKey, source ) );

		// The class never loads (nothing references brad), so its property default
		// never ran — but w = 9 did.
		assertThat( CodeProfilerService.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 0 );   // default=42 missed
		assertThat( CodeProfilerService.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // w = 9
	}

	@DisplayName( "It leaves statements after a return missed inside a function body" )
	@Test
	void testReturnShortCircuitsBody() {
		String source = """
		                function foo( flag ) {
		                if( flag ) {
		                return 1;
		                }
		                return 2;
		                }
		                x = foo( true );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testReturnShortCircuitsBody dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// foo( true ) takes the if-branch, so `return 1` (line 3) runs and the
		// function returns immediately; `return 2` (line 5) is never reached.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // function shell
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // return 1 ran
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 0 );   // return 2 missed
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // x = foo( true )

		// Line-based: return 1 line covered; return 2 line missed; call covered.
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isTrue();
	}

	@DisplayName( "It runs every case body when a case falls through without break" )
	@Test
	void testSwitchFallthrough() {
		String source = """
		                x = 2;
		                switch( x ) {
		                case 1: a = 10; break;
		                case 2: a = 20;
		                case 3: a = 30; break;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testSwitchFallthrough dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// x=2 matches case 2, which has no break — execution falls through into
		// case 3, so BOTH a=20 and a=30 ran. Case 1 (a=10) stayed missed.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 2
		assertThat( CodeProfilerService.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // case 1 a=10 missed
		assertThat( CodeProfilerService.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 1 );   // case 2 a=20 ran
		assertThat( CodeProfilerService.spanAt( key, 5, 8 ).stats().count() ).isEqualTo( 1 );   // case 3 a=30 ran

		// Line-based: case 2 and case 3 lines covered. Case 1's line is touched by
		// the multi-line switch-header span, so lineAt reports it covered — the
		// authoritative "case 1 missed" signal is its BODY SPAN count (above).
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
	}

	@DisplayName( "It profiles an else-if chain and leaves untaken branches missed" )
	@Test
	void testElseIfChain() {
		String source = """
		                x = 2;
		                if( x == 1 ) {
		                a = 10;
		                } else if( x == 2 ) {
		                a = 20;
		                } else if( x == 3 ) {
		                a = 30;
		                } else {
		                a = 99;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testElseIfChain dump" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// x=2: first cond false (a=10 missed), second else-if cond true (a=20 ran),
		// third else-if and final else both missed.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 2
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );   // a = 10 missed
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // a = 20 ran
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 0 );   // a = 30 missed
		assertThat( CodeProfilerService.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 0 );   // a = 99 missed

		// Line-based: taken branch (line 5) covered; untaken branches missed.
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 9 ).covered() ).isFalse();
	}

	@DisplayName( "It profiles a real file on disk keyed by its file path" )
	@Test
	void testDiskFile() {
		String relativePath = "src/test/resources/profiler/ProfilerSample.bxs";
		runtime.executeTemplate( relativePath );

		// The blueprint is keyed by the NORMALIZED absolute file path (what
		// registerBlueprintForFile stores), not a source hash.
		Path	absolute	= Paths.get( relativePath ).toAbsolutePath().normalize();
		String	fileKey		= absolute.toString().toLowerCase( Locale.ROOT );

		// DEBUG (reads file from disk, no source arg)
		// System.out.println( "=== testDiskFile dump" );
		// System.out.print( CodeProfilerService.dumpSpans( fileKey ) );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( fileKey );

		var blueprint = CodeProfilerService.trackedBlueprints().get( fileKey );
		// The file has several executable spans (assignments, loop body, function
		// body, if branches, ternary, closure) — a general high-level check.
		assertThat( blueprint.spans() ).isNotEmpty();

		// High-level line coverage: the file's executable lines ran. Line counts
		// reflect how many times the line's leading span executed.
		// 6 : i = 0 (ran once)
		// 7 : j = 0 (ran once)
		// 13 : i++ (ran 3x, inside while( i < 3 ))
		// 37 : j = 1 (if taken, doubled > 40) (ran once)
		// 39 : j = 2 (else, not taken) (MISSED)
		// 82 : added = adder( doubled ) (ran once)
		assertThat( CodeProfilerService.lineAt( fileKey, 6 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 6 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 7 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 7 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 13 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 13 ).count() ).isEqualTo( 3 );   // i++ ran 3x
		assertThat( CodeProfilerService.lineAt( fileKey, 37 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 37 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 39 ).covered() ).isFalse();      // else branch missed
		assertThat( CodeProfilerService.lineAt( fileKey, 82 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 82 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles a class file on disk keyed by its file path" )
	@Test
	void testDiskClassFile() {
		String		relativePath	= "src/test/resources/profiler/ProfilerComplex.bx";

		// One shared request context so the SAME variables scope (and therefore the
		// SAME class instance) persists across every incremental executeSource call.
		IBoxContext	context			= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Instantiate the on-disk class via a script. Compiling registers the
		// blueprint (Pass A); instantiation runs the static block + pseudo-constructor.
		runtime.executeSource( "pc = new src.test.resources.profiler.ProfilerComplex();", context );

		// Same file-path keying: normalized absolute path.
		Path	absolute	= Paths.get( relativePath ).toAbsolutePath().normalize();
		String	fileKey		= absolute.toString().toLowerCase( Locale.ROOT );

		// DEBUG (reads file from disk, no source arg)
		// System.out.println( "=== testDiskClassFile dump" );
		// System.out.print( CodeProfilerService.dumpSpans( fileKey ) );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( fileKey );

		var blueprint = CodeProfilerService.trackedBlueprints().get( fileKey );
		assertThat( blueprint.spans() ).isNotEmpty();

		// High-level line coverage of the class file (comment lines 1-2 shift lines):
		// 5 : default=40 (property default ran at class load)
		// 8 : staticInitRan = true (static block ran at class load)
		// 11 : instanceInit = 0 (pseudo-constructor ran at instantiation)
		assertThat( CodeProfilerService.lineAt( fileKey, 8 ).covered() ).isTrue();   // static block ran
		assertThat( CodeProfilerService.lineAt( fileKey, 8 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 11 ).covered() ).isTrue();  // pseudo-constructor body
		assertThat( CodeProfilerService.lineAt( fileKey, 11 ).count() ).isEqualTo( 1 );
		// The property default (line 5, col 35) ran at class load, count 1.
		assertThat( CodeProfilerService.spanAt( fileKey, 5, 35 ).stats().count() ).isEqualTo( 1 );  // property default ran

		// No method has been invoked yet, so the member function body statement
		// (line 18: "sleep( 100 )", at col 2 after the tab) has NOT run — its count
		// is 0 and its line is not covered. (Line 17 is the member function shell.)
		assertThat( CodeProfilerService.spanAt( fileKey, 18, 2 ).stats().count() ).isEqualTo( 0 );  // member body missed
		assertThat( CodeProfilerService.lineAt( fileKey, 18 ).covered() ).isFalse();

		// Now invoke the member method on the SAME instance in a second script (the
		// shared context keeps pc in the variables scope); its body should run.
		runtime.executeSource( "result = pc.member( 5 );", context );

		// The member body slept ~100ms, so its span is covered with the sleep charged.
		assertThat( CodeProfilerService.spanAt( fileKey, 18, 2 ).stats().count() ).isEqualTo( 1 );  // sleep ran
		assertThat( CodeProfilerService.spanAt( fileKey, 18, 2 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( fileKey, 18, 2 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
		assertThat( CodeProfilerService.lineAt( fileKey, 18 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 18 ).count() ).isEqualTo( 1 );
	}
}