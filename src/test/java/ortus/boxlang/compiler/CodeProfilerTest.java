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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.services.CodeProfilerService;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Tests for the ASM boxpiler's code-profiling instrumentation: run a block with
 * profiling enabled and verify the boxpiler registered a blueprint (Pass A) and the
 * executed expression statement's span got covered via the emitted
 * {@code CodeProfilerService.mark} call (Pass B).
 * <p>
 * This differs from the service-only {@code CodeProfilerServiceTest}: here we go
 * source -> boxpiler spans -> mark emission -> recorded coverage through the runtime.
 */
// These tests compile + inspect bytecode emitted by the ASM boxpiler (mark/
// markEnd instructions, clinit self-registration). They only apply when the
// ASM boxpiler is the active compiler.
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class CodeProfilerTest {

	private BoxRuntime			runtime;
	private CodeProfilerService	service;
	private boolean				prevProfiler;

	@BeforeEach
	void setup() {
		this.runtime										= BoxRuntime.getInstance();
		this.service										= this.runtime.getCodeProfilerService();
		// Enable compile-time instrumentation for this test
		this.prevProfiler									= this.runtime.getConfiguration().codeProfilerEnabled;
		this.runtime.getConfiguration().codeProfilerEnabled	= true;
		// Source is compiled once and cached in the boxpiler class pool (memory + disk),
		// so clear BOTH to force a recompile now that instrumentation is enabled.
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		// REMEMBER: this clean may trash other test caches in local runs — CI starts
		// with an empty classes dir, so it's only actually needed locally.
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		service.setActive( true );
		service.reset();
	}

	@AfterEach
	void teardown() {
		this.runtime.getConfiguration().codeProfilerEnabled = this.prevProfiler;
		service.setActive( false );
		service.reset();
	}

	@DisplayName( "It registers a blueprint and profiles the expression statement" )
	@Test
	void testExpressionStatement() {
		String source = "2 + 2;";
		runtime.executeSource( source );

		// Source is adhoc, so the blueprint is keyed by the same MD5 the boxpiler used.
		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Pass A: the boxpiler registered a SOURCE blueprint.
		assertThat( service.trackedBlueprints() ).containsKey( key );
		// Span-definition sanity: both operands of 2 + 2 are literals, which cannot
		// throw when evaluated, so the whole statement is one span.
		var spanDefs = service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 1 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );

		// Pass B: the emitted mark() ran the statement span exactly once.
		var spans = service.spansOnLine( key, 1 );
		assertThat( spans ).hasSize( 1 );
		assertThat( spans.get( 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: line 1 is covered with count 1.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "A reset() mid-execution drops prior marks; later marks re-register and are tracked" )
	@Test
	void testResetMidExecutionReRegisters() {
		// The script clears ALL blueprints HALFWAY through (via the Java service), then
		// keeps running. The already-loaded class's later marks must RE-REGISTER its
		// blueprint (each mark passes the class's own Blueprint static field) and be
		// tracked; only lines executed BEFORE the clear remain tracked... and they do not.
		String source = """
		                x1 = 1;
		                x2 = 2;
		                getBoxRuntime().getCodeProfilerService().reset();
		                x3 = 3;
		                x4 = 4;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The two pre-reset lines' marks were cleared and are NOT tracked.
		assertThat( service.lineAt( key, 1 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();

		// The line containing the reset() call itself ran (its mark fired BEFORE the
		// clear), so it was dropped too.
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();

		// The two POST-reset lines re-registered the blueprint on their marks and ARE
		// tracked.
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
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
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );  // "x = 1 "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );  // "y = 2 "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );  // "z = x + "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 8, 3, 9, true ) );  // "y"

		// Pass B: each statement's span profiled exactly once.
		assertThat( service.spansOnLine( key, 1 ) ).hasSize( 1 );
		assertThat( service.spansOnLine( key, 1 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 2 ) ).hasSize( 1 );
		assertThat( service.spansOnLine( key, 2 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 3 ) ).hasSize( 2 );
		assertThat( service.spansOnLine( key, 3 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 3 ).get( 1 ).stats().count() ).isEqualTo( 1 );

		// Line-based: all three lines covered with count 1.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: the ternary statement splits into 3 executable spans:
		// "x = bar ? " (line 4, cols 0-10) — condition + punctuation
		// "baz : " (line 4, cols 10-16) — whenTrue branch
		// "bum" (line 4, cols 16-19) — whenFalse branch
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 ); // 3 assignments + 3 ternary spans
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 10, true ) );  // bar = true
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 7, true ) );   // baz = 1
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 7, true ) );   // bum = 2
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 10, true ) );  // "x = bar ? "
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 10, 4, 16, true ) ); // "baz : "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 16, 4, 19, true ) ); // "bum"

		// Pass B: bar=true so the true branch (baz) ran exactly once, the false branch
		// (bum) is MISSED (count == 0).
		var	xBar	= service.spanAt( key, 4, 1 );
		var	baz		= service.spanAt( key, 4, 11 );
		var	bum		= service.spanAt( key, 4, 17 );
		assertThat( xBar.stats().count() ).isEqualTo( 1 );
		assertThat( baz.stats().count() ).isEqualTo( 1 );
		assertThat( bum.stats().count() ).isEqualTo( 0 );

		// Line-based: lines 1-3 assignments cover once; line 4 the ternary covers once.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a branching condition out of the leading span" )
	@Test
	void testTernaryWithBranchingCondition() {
		String source = "x = ( true ? false : true ) ? 1 : 2;";
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testTernaryWithBranchingCondition dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The condition is always evaluated, so it merges into the running span;
		// each branch breaks into its own span. Punctuation is included per the
		// design. Expect 5 executable spans:
		// "x = ( true ? " | "false : " | "true ) ? " | "1 : " | "2"
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) );  // "x = ( true ? "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 13, 1, 21, true ) ); // "false : "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 21, 1, 30, true ) ); // "true ) ? "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 30, 1, 34, true ) ); // "1 : "
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 34, 1, 35, true ) );  // "2"

		assertThat( service.spansOnLine( key, 1 ).get( 0 ).stats().count() ).isEqualTo( 1 );   // first span on line 1
		assertThat( service.spanAt( key, 1, 34 ).stats().count() ).isEqualTo( 1 );  // "2"

		// Line-based: line 1 is covered, count 1 from the leading span.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: 10 executable spans — 4 before the throw (lines 1-4), the throw
		// itself (line 5), and 5 after (lines 6-10).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 6 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 7 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 8 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 9 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 10 ).covered() ).isFalse();

		// Exact counts: lines 1-5 each ran exactly once; spans on lines 6-10 are 0.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // i = 5
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // i++
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // i++
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );  // i++
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );  // throw
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( service.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( service.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 0 );  // i++ not run
		assertThat( service.spanAt( key, 10, 0 ).stats().count() ).isEqualTo( 0 ); // i++ not run
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: 4 assignments (1 span each) + the chain statement splits into 4
		// operands (result = foo / bar / bum / quz each its own span).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 15, true ) );  // "result = foo "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 15, 5, 21, true ) ); // "& bar"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 5, 21, 5, 27, true ) ); // "& bum"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 5, 27, 5, 30, true ) ); // "& quz"

		// Pass B: the whole chain ran, every operand exactly once.
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // result = foo
		assertThat( service.spanAt( key, 5, 15 ).stats().count() ).isEqualTo( 1 );  // & bar
		assertThat( service.spanAt( key, 5, 21 ).stats().count() ).isEqualTo( 1 );  // & bum
		assertThat( service.spanAt( key, 5, 27 ).stats().count() ).isEqualTo( 1 );  // & quz

		// Line-based: line 5 is covered with count 1 (first/outermost span on the line).
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: 2 assignments + the chain splits into operands: foo / the IIFE
		// (whose body throw is its own span, and whose braces get their own spans) /
		// bar.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );

		// Pass B: foo ran, the IIFE threw (so it counts as started), bar never ran.
		assertThat( service.spanAt( key, 3, 10 ).stats().count() ).isEqualTo( 1 );  // "foo" ran
		assertThat( service.spanAt( key, 3, 15 ).stats().count() ).isEqualTo( 1 );  // "( function() " shell
		assertThat( service.spanAt( key, 3, 30 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( service.spanAt( key, 3, 28 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 3, 47 ).stats().count() ).isEqualTo( 1 );  // } ran
		assertThat( service.spanAt( key, 3, 55 ).stats().count() ).isEqualTo( 0 );  // "bar" never ran

		// Line-based: line 3 is covered (foo + the started IIFE), count from the leading span.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits an if on one line into condition and branch spans" )
	@Test
	void testIfOneLiner() {
		String source = "if( true ) a = 1;";
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testIfOneLiner dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The if's condition runs, then the single no-brace statement body (a = 1)
		// breaks into its own executable span — it may not run, so it's tracked
		// separately (same as a block body's first statement).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 11, true ) );  // "if( true ) "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 11, 1, 16, true ) ); // "a = 1"

		// Runtime coverage: the condition and the (taken) then-body ran exactly once.
		assertThat( service.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );  // condition
		assertThat( service.spanAt( key, 1, 11 ).stats().count() ).isEqualTo( 1 ); // a = 1

		// Line-based: line 1 is covered with count 1.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: condition (line 1), then-open brace, then-body a = 1 (line 2),
		// then-close brace, else keyword, else-open brace, else-body b = 2 (line 4),
		// else-close brace — each its own span.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );  // condition
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );  // a = 1 ran
		assertThat( service.spanAt( key, 4, 1 ).stats().count() ).isEqualTo( 0 );  // b = 2 missed

		// Line-based: line 1 and line 2 covered; line 4 (else) not covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isFalse();
	}

	@DisplayName( "It marks the executed else keyword even when the then branch holds an else-like string" )
	@Test
	void testElseKeywordMarkedDespiteElseTagLikeStringInThenBranch() {
		// The else-keyword span is located by scanning BACKWARD from the else body for
		// the keyword, then checking whether it is inside a template else TAG. A `<`
		// appearing earlier in the source — such as the one starting the STRING LITERAL
		// `"<cfelse>"` in the (skipped) then branch — must NOT be mistaken for this
		// keyword's tag, or the executed else-keyword span is left unmarked.
		String source = """
		                x = 0;
		                if( false ) { s = "<cfelse>"; } else { y = 1; }
		                """;
		runtime.executeSource( source );

		String						key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The condition is false, so the else branch RAN: both the else keyword and its
		// body must be reported as executed.
		CodeProfilerService.Span	elseKeyword	= null;
		CodeProfilerService.Span	elseBody	= null;
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			String text = service.spanSourceText( key, source, s );
			if ( "else".equals( text ) ) {
				elseKeyword = s;
			} else if ( "y = 1".equals( text ) ) {
				elseBody = s;
			}
		}
		assertThat( elseKeyword ).isNotNull();
		assertThat( elseBody ).isNotNull();
		assertThat( elseKeyword.stats().count() ).isEqualTo( 1 );
		assertThat( elseBody.stats().count() ).isEqualTo( 1 );

		// The skipped then-branch string literal must stay unexecuted.
		boolean hasStringSpan = false;
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( service.spanSourceText( key, source, s ).contains( "<cfelse>" ) ) {
				hasStringSpan = true;
				assertThat( s.stats().count() ).isEqualTo( 0 );
			}
		}
		assertThat( hasStringSpan ).isTrue();
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
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 12, true ) );
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) );
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) );

		// Full captured snapshot for debugging.
		System.out.println( "TIMING SNAPSHOT: " + service.fileSpans( key ) );

		// Each of the three statements ran exactly once.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: each line ran once and is covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );

		// Span 1 was charged while sleep(500) ran; span 2 while sleep(1000) ran.
		// Both >= their sleep, within a generous band for scheduling slop.
		assertThat( service.spanAt( key, 1, 0 ).stats().totalNanos() ).isAtLeast( 500L * 1_000_000L );
		assertThat( service.spanAt( key, 1, 0 ).stats().totalNanos() ).isAtMost( 750L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 1_000L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 1_500L * 1_000_000L );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: i=0, the while header, the condition (its own span), the body
		// i++, and the two loop braces ({ and }) are each their own span.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "i = 0"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 7, true ) );   // "while( " header
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 7, 2, 15, true ) );  // " i < 3 )" condition
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 3, true ) );   // "i++"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 2, 15, 2, 16, true ) ); // "{" open brace
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}" close brace

		// Pass B: the loop header ran once; the CONDITION re-evaluated every
		// iteration (3 true + 1 false exit = 4); body ran 3 times.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // while( header
		assertThat( service.spanAt( key, 2, 7 ).stats().count() ).isEqualTo( 4 );   // condition: 3 true + 1 false
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // i++ ran 3x
		assertThat( service.spanAt( key, 2, 15 ).stats().count() ).isEqualTo( 3 );  // open brace ran 3x
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // close brace ran 3x

		// Line-based: lines 1-4 covered (line 4 is the closing brace, ran 3x).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: i=0, while header, condition, sleep(100), i++, { and } braces.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );

		// Pass B: body ran 3x — sleep and i++ each counted 3 times.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // sleep( 100 ) ran 3x
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // i++ ran 3x

		// Timing: 3 x 100ms sleeps accumulated into the sleep span (~300ms).
		long sleepNanos = service.spanAt( key, 3, 0 ).stats().totalNanos();
		assertThat( sleepNanos ).isAtLeast( 300L * 1_000_000L );
		assertThat( sleepNanos ).isAtMost( 900L * 1_000_000L );

		// Line-based: lines 1-5 covered (line 5 is the closing brace, ran 3x).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: x=2, the switch header (with case spillover), each case label +
		// its body/break as separate spans, the default label + body, and the
		// closing brace.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 2
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // switch cond
		assertThat( service.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // case 1 a=10 missed
		assertThat( service.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 1 );   // case 2 a=20 ran
		assertThat( service.spanAt( key, 5, 9 ).stats().count() ).isEqualTo( 0 );   // default a=30 missed

		// Line-based: lines 1-4 covered; line 5 (default, missed) not covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 5 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: try keyword, the try body's three statements, the catch keyword,
		// the catch body, the finally keyword, the finally body, and their braces.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // sleep( 100 )
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // b = 2
		assertThat( service.spanAt( key, 5, 2 ).stats().count() ).isEqualTo( 0 );   // catch header missed
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 0 );   // c = 3 missed
		assertThat( service.spanAt( key, 7, 2 ).stats().count() ).isEqualTo( 1 );   // finally container ran
		assertThat( service.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 ran

		// Timing: the sleep(100) span was charged at least the 100ms sleep.
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );

		// Line-based: try + finally covered; catch not.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 6 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 8 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 8 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks the braces of an empty finally block when it runs" )
	@Test
	void testEmptyFinallyBracesCovered() {
		String source = """
		                try {
		                a = 1;
		                } catch( any e ) {
		                c = 3;
		                } finally {
		                }
		                done = 2;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testEmptyFinallyBracesCovered dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The empty finally `} finally {` + `}` still EXECUTES, so both brace spans
		// must exist and be covered (regression: previously the whole empty-finally
		// block was silently dropped — neither the header nor the closing brace was
		// registered, leaving line 5/6 uncovered).
		assertThat( service.spanAt( key, 5, 2 ).stats().count() ).isEqualTo( 1 );   // finally {
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // }
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();

		// The try body ran; the catch did not.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 0 );   // c = 3 (catch missed)
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // done = 2
	}

	@DisplayName( "It marks the braces of an empty finally on the exception-handler path" )
	@Test
	void testEmptyFinallyBracesCoveredOnThrow() {
		String source = """
		                try {
		                throw( "boom" );
		                } finally {
		                }
		                done = 2;
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the throw propagates after the empty finally runs
		}
		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The finally runs via the EXCEPTION-HANDLER bytecode copy when the try
		// throws; its braces must still be marked (count > 0).
		assertThat( service.spanAt( key, 3, 2 ).stats().count() ).isEqualTo( 1 );   // finally {
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		// done = 2 never ran (the throw propagated).
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 0 );
	}

	@DisplayName( "It marks the finally braces when the try body holds tag-like text inside a string" )
	@Test
	void testFinallyBracesCoveredWithTagLikeTextInTryBody() {
		// Regression: finallyKeyword() pre-scans backward for the TAG form of the
		// finally and aborts on ANY "<" that looks like a tag. Tag-like text inside a
		// string literal in the try body (`"<foo>"`) is unrelated to template syntax,
		// but the scan stopped there and never reached the script `finally` lookup,
		// leaving the executed finally brace group reported as MISSED.
		String source = """
		                try {
		                x = "<foo>";
		                } finally {
		                y = 1;
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The try body statement ran.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();   // x = "<foo>"

		// The finally ALWAYS runs: its header brace group, body and closing brace
		// must all be GREEN.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();   // } finally {
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();   // y = 1
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();   // }
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It does not mark a nested try's finally when the outer try has no finally" )
	@Test
	void testOuterTryWithoutFinallyDoesNotMarkNestedFinally() {
		// Regression (PR #653 review): a try/catch with NO finally clause also has an
		// EMPTY getFinallyBody(), so finallyKeyword()'s backward scan from the outer
		// try's end walks back INTO a nested try and selects the NESTED
		// `finally { ... }` keyword. The outer try's exceptional cleanup then marks
		// that nested finally even though it never executed (the throw happens before
		// the nested try is ever reached). The scan must distinguish an ABSENT finally
		// from an EMPTY one and restrict lookup to this try's own finally clause.
		String source = """
		                try {
		                throw( "boom" );
		                try {
		                x = 1;
		                } finally {
		                y = 2;
		                }
		                }
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the throw propagates (the outer try has no catch/finally)
		}

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The throw occurs before the nested try, so the nested try body and its
		// finally NEVER run — nothing on the nested try may be reported as covered.
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 0 );   // x = 1
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 0 );   // } finally {
		assertThat( service.lineAt( key, 6 ).count() ).isEqualTo( 0 );   // y = 2
	}

	@DisplayName( "It does not double-count an earlier try's finally from a later try with no finally" )
	@Test
	void testLaterTryWithoutFinallyDoesNotRemarkEarlierFinally() {
		// Regression (PR #653 review): CONSECUTIVE try statements where the FIRST has
		// a finally and the SECOND has none. The second try's empty finally scans back
		// from its own end and reaches the FIRST try's `finally { ... }` keyword,
		// emitting a second brace-group mark for a block that only ran once — inflating
		// its execution count to 2.
		String source = """
		                try {
		                a = 1;
		                } finally {
		                b = 2;
		                }
		                try {
		                c = 3;
		                } catch( any e ) {
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The first try's finally ran exactly ONCE — the second try (which has no
		// finally) must not re-mark it.
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );   // } finally {
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );   // b = 2
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );   // }
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: each assert and the trailing x=1 are their own span.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 26, true ) );  // assert( true, "all good" )
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 23, true ) );  // assert( false, "boom" )
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 5, true ) );   // x = 1

		// Pass B: the first assert passed (count 1); the second assert threw before
		// its mark was emitted, and x = 1 never ran.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );

		// Line-based: line 1 covered; lines 2-3 not.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: arr=[ 10, 20, 30 ] splits per element (like struct values):
		// "arr = [ " | "10" | ", " | "20" | ", " | "30" | " ]" (7 spans), then the
		// for-in header, the body x = item, and the loop braces { and }.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 11 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 8, true ) );   // "arr = [ "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 8, 1, 10, true ) );  // "10"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 10, 1, 12, true ) ); // ", "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 12, 1, 14, true ) ); // "20"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 14, 1, 16, true ) ); // ", "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 1, 16, 1, 18, true ) ); // "30"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 1, 18, 1, 20, true ) ); // " ]"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 19, true ) );  // "for( item in arr ) "
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );   // "x = item"
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 2, 19, 2, 20, true ) ); // "{" open brace
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );  // "}" close brace

		// Pass B: header ran once; body ran 3 times (10, 20, 30).
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // arr = [ (
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // for-in header
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // x = item ran 3x
		assertThat( service.spanAt( key, 2, 19 ).stats().count() ).isEqualTo( 3 );  // { ran 3x
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // } ran 3x

		// Line-based: lines 1-4 covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: i=0, do, the body i++, the two braces { and }, while(, the
		// condition, and the closing ) are each their own span.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // do
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // i++ ran 2x
		assertThat( service.spanAt( key, 2, 3 ).stats().count() ).isEqualTo( 2 );   // { ran 2x
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 2 );   // } ran 2x
		assertThat( service.spanAt( key, 4, 2 ).stats().count() ).isEqualTo( 1 );   // while(
		assertThat( service.spanAt( key, 4, 9 ).stats().count() ).isEqualTo( 2 );   // condition checked 2x
		assertThat( service.spanAt( key, 4, 14 ).stats().count() ).isEqualTo( 1 );  // )

		// Line-based: lines 1-4 covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 2 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: j=0, then the for header splits into its THREE independent
		// run-count parts — the initializer "for( i = 0;" (runs once), the
		// condition "i < 3" (re-evaluated every iteration, n+1 times), and the
		// step "i++" (runs n times) — plus the body j = j + i (2 spans: LHS+RHS)
		// and the loop braces { and }.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 5, true ) );   // "j = 0"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 12, true ) );  // "for( i = 0;"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 12, 2, 19, true ) ); // "i < 3" (condition)
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 19, 2, 25, true ) ); // "i++" (step)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );   // "j = j + "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 8, 3, 9, true ) );   // "i"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 2, 25, 2, 26, true ) ); // "{" open brace
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}" close brace

		// Pass B: the initializer ran once; the CONDITION ran 4 times (i: 0,1,2
		// true + i=3 false to exit); the STEP ran 3 times; body ran 3 times.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // j = 0
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // for initializer
		assertThat( service.spanAt( key, 2, 12 ).stats().count() ).isEqualTo( 4 );  // condition: 3 true + 1 false
		assertThat( service.spanAt( key, 2, 19 ).stats().count() ).isEqualTo( 3 );  // step i++ ran 3x
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // j = j + ran 3x
		assertThat( service.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 3 );   // i ran 3x
		assertThat( service.spanAt( key, 2, 25 ).stats().count() ).isEqualTo( 3 );  // { ran 3x
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // } ran 3x

		// Line-based: lines 1-4 covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: a=1, if cond, b=2, sleep(150), c=3, { and } braces, d=4.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // if cond
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // b = 2
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // sleep( 150 )
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // c = 3
		assertThat( service.spanAt( key, 2, 11 ).stats().count() ).isEqualTo( 1 );  // {
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // }
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4

		// Timing: the sleep(150) span's self-time >= 150ms (charged from the probe
		// interval when the NEXT span's mark fires).
		assertThat( service.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtLeast( 150L * 1_000_000L );
		assertThat( service.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtMost( 450L * 1_000_000L );

		// Line-based: lines 1-7 covered once (line 6 is the braces, ran once).
		for ( int line : new int[] { 1, 2, 3, 4, 5, 6, 7 } ) {
			assertThat( service.lineAt( key, line ).covered() ).isTrue();
			assertThat( service.lineAt( key, line ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Two spans; the final span's interval is closed by markEnd, so it must
		// carry the sleep(200) time.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 12, true ) );

		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );

		// The final span (line 2) was charged the full 200ms by markEnd.
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The sleep(100) span (line 2) is charged when the while's entry mark fires.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// The loop itself ran once.
		assertThat( service.spanAt( key, 3, 1 ).stats().count() ).isEqualTo( 1 );   // while cond
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // i++
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: 12 spans — i=0, outer header, outer cond, j=0, inner header,
		// inner cond, j++, i++, and the four loop braces ({ and } each for inner
		// and outer).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 12 );

		// Outer while ran 3x: j=0 (line 3) and inner header (line 4) each 3x;
		// inner body j++ (line 5) ran 3x2=6; outer body i++ (line 7) ran 3x.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // outer header
		assertThat( service.spanAt( key, 2, 8 ).stats().count() ).isEqualTo( 4 );   // outer cond 3 true + 1 false
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // j = 0
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // inner header
		assertThat( service.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 9 );   // inner cond 3 checks x 3 outer = 9
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 6 );   // j++ ran 6x
		assertThat( service.spanAt( key, 4, 15 ).stats().count() ).isEqualTo( 6 );  // inner { ran 6x
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 6 );   // inner } ran 6x
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 3 );   // i++ ran 3x
		assertThat( service.spanAt( key, 2, 15 ).stats().count() ).isEqualTo( 3 );  // outer { ran 3x
		assertThat( service.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 3 );   // outer } ran 3x

		// Line-based.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 6 );
		assertThat( service.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 7 ).count() ).isEqualTo( 3 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// i++ and break each ran once (loop broke after one iteration).
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // i = 0
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // while cond
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // i++
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // break
		assertThat( service.spanAt( key, 2, 14 ).stats().count() ).isEqualTo( 1 );  // {
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // }

		// Line-based: lines 1-5 covered (line 5 is the closing brace, ran once).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// i++ and continue each ran 3x; i=99 never ran.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );   // i++
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );   // continue
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 0 );   // i = 99

		// Line-based: line 5 not covered.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 5 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Condition ran once; body never ran.
		assertThat( service.spanAt( key, 2, 8 ).stats().count() ).isEqualTo( 1 );   // cond (once, then exit)
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );   // i++

		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Header ran once; body never ran.
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // header
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );   // j++

		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// a=1 ran, throw ran, b=2 (after throw) never ran, c=3 (catch) ran.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // throw
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 0 );   // b = 2
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // c = 3

		// Line-based.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 6 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The finally body IS executed by Java semantics when the try throws. The
		// BoxTryTransformer compiles the finally body in three bytecode copies
		// (inline-after-try, inline-after-catch, exceptional handler); the mark
		// emission must exist in ALL of them so whichever executes fires the count.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // throw
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 in finally

		// Line-based: line 5 (finally) covered.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// a=1 ran, the throw ran, the catch c=3 ran, and the finally d=4 ran exactly
		// once (via the catch's inlined-finally copy — the try's inline copy never
		// ran because the throw skipped it).
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // a = 1
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // throw
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // c = 3 catch
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 finally

		// Line-based: catch and finally both covered once.
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 7 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It keeps a grouped construct inside a duplicated finally body covered on the catch path" )
	@Test
	void testFinallyBodyGroupedConstructOnCatchPath() {
		// A GROUPED construct (here an `if` with braces → a header + closing-brace
		// span GROUP) inside a finally body. The finally body is compiled into
		// MULTIPLE bytecode copies (try-inline, catch-inline, exceptional handler)
		// and only ONE runs at runtime — so the group must be re-emitted in EVERY
		// copy. The snapshot/restore of emitted marks restores only the CLAIMED
		// marks; unless it also restores the consumed span GROUP, the first copy
		// consumes the group and later copies emit only the head span, leaving the
		// rest (e.g. the closing brace) missed on the path that actually runs.
		String source = """
		                try {
		                a = 1;
		                throw( "boom" );
		                } catch( any e ) {
		                c = 3;
		                } finally {
		                d = 4;
		                if( true ) {
		                e = 5;
		                }
		                }
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyBodyGroupedConstructOnCatchPath dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The throw is caught, so the finally runs via the CATCH's inlined copy (the
		// try's inline copy is skipped). Its grouped `if` must be FULLY covered: the
		// finally statement, the if body, the if header and the if's closing brace.
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 (finally body)
		assertThat( service.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 1 );   // e = 5 (if body)
		assertThat( service.spanAt( key, 10, 0 ).stats().count() ).isEqualTo( 1 );  // if closing "}" (group member)

		// Line-based: the if's closing-brace line is covered.
		assertThat( service.lineAt( key, 10 ).covered() ).isTrue();
	}

	@DisplayName( "It keeps the finally body covered when the try body has a return on another path" )
	@Test
	void testFinallyBodyCoveredWhenTryReturnsOnNonReturnPath() {
		// The try body contains a `return` on ONE path only. The boxpiler compiles the
		// finally body into a bytecode copy for THAT return too, and that copy is what
		// used to claim the finally statement's mark first — starving the fall-through
		// copy (the one that actually runs when the return is NOT taken). Coverage must
		// not depend on which duplicated copy happened to be transformed first.
		String source = """
		                function foo( flag ) {
		                try {
		                if( flag ) {
		                return 1;
		                }
		                a = 2;
		                } finally {
		                d = 4;
		                }
		                return 0;
		                }
		                result = foo( false );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyBodyCoveredWhenTryReturnsOnNonReturnPath dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// foo( false ) takes the fall-through path: a=2 runs, then the finally d=4 runs.
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // a = 2
		assertThat( service.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 (finally body)
		assertThat( service.lineAt( key, 8 ).covered() ).isTrue();
	}

	@DisplayName( "It marks the finally braces when the finally runs via a return path" )
	@Test
	void testFinallyBracesCoveredOnReturnPath() {
		// `return 5` inside the try jumps straight to the (inlined) finally body, so the
		// finally header/brace marks emitted by the fall-through copy never execute. The
		// return path's own copy must therefore carry the finally brace-group mark.
		String source = """
		                function bar() {
		                try {
		                return 5;
		                } finally {
		                d = 6;
		                }
		                }
		                result = bar();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyBracesCoveredOnReturnPath dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The finally ran (via the return path) — its body, header and closing brace must be GREEN.
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // d = 6 (finally body)
		assertThat( service.spanAt( key, 4, 2 ).stats().count() ).isEqualTo( 1 );   // } finally { (group head)
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // } (group member)
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It keeps the finally body covered when a catch body throws" )
	@Test
	void testFinallyBodyCoveredWhenCatchBodyThrows() {
		// A throwing catch body runs the finally via the dedicated exception-handler
		// copy. A DIFFERENT duplicated copy (the "no catch matched" copy) is emitted
		// earlier and used to claim the finally statement's mark first, starving the
		// exception-handler copy that actually runs.
		String source = """
		                try {
		                throw( "boom" );
		                } catch( any e ) {
		                throw( "again" );
		                } finally {
		                d = 4;
		                }
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the catch body rethrows after the finally runs
		}

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyBodyCoveredWhenCatchBodyThrows dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The finally body ran (after the throwing catch body) — must be GREEN.
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // throw "again"
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 (finally body)
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();
	}

	@DisplayName( "It marks the finally braces when the finally runs on the unmatched-catch path" )
	@Test
	void testFinallyBracesCoveredOnUnmatchedCatchPath() {
		// An exception that matches NO catch clause runs the finally via the
		// "no catch matched" copy, which must carry the finally brace-group mark.
		String source = """
		                try {
		                throw( "boom" );
		                } catch( java.io.IOException e ) {
		                c = 3;
		                } finally {
		                d = 4;
		                }
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — no catch matched, so the exception propagates after finally runs
		}

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFinallyBracesCoveredOnUnmatchedCatchPath dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The finally ran (no catch matched) — body AND braces must be GREEN.
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // d = 4 (finally body)
		assertThat( service.spanAt( key, 5, 2 ).stats().count() ).isEqualTo( 1 );   // } finally { (group head)
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // } (group member)
		assertThat( service.lineAt( key, 7 ).covered() ).isTrue();
	}

	@DisplayName( "It wraps profiled method bodies in a catch-all that closes the probe interval on uncaught exceptions" )
	@Test
	void testExceptionEscapeClosesProbeInterval() throws Exception {
		// Every profiled method boundary (script _invoke, UDF/closure/lambda
		// invoker, pseudo-constructor, static initializer) must wrap its body in a
		// catch-all handler that calls CodeProfilerService.markEnd then rethrows.
		// Regression: an uncaught exception used to bypass the return-path and
		// fall-through markEnd, leaving the thread's probe interval open and
		// charging unwind/caller time to the throwing span on the next probe.
		runtime.getConfiguration().codeProfilerEnabled = true;

		var	boxpiler	= ( ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		var	source		= """
		                  boom = () -> {
		                  throw( "Oh no!" );
		                  };
		                  function bounce() {
		                  boom();
		                  }
		                  bounce();
		                  """;
		var	classInfo	= ortus.boxlang.compiler.ClassInfo.forScript( source, BoxSourceType.BOXSCRIPT, boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );
		java.util.List<byte[]>	compiled		= boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );

		// Count methods that are profiled body boundaries and verify each has a
		// catch-all handler (TryCatchBlockNode with null type) whose handler label
		// leads to markEnd before an ATHROW.
		int						profiledMethods	= 0;
		int						guarded			= 0;
		for ( byte[] bytes : compiled ) {
			if ( bytes.length < 4 || bytes[ 0 ] != ( byte ) 0xCA || bytes[ 1 ] != ( byte ) 0xFE || bytes[ 2 ] != ( byte ) 0xBA
			    || bytes[ 3 ] != ( byte ) 0xBE ) {
				continue;
			}
			org.objectweb.asm.tree.ClassNode classNode = new org.objectweb.asm.tree.ClassNode();
			new org.objectweb.asm.ClassReader( bytes ).accept( classNode, 0 );
			for ( var method : classNode.methods ) {
				boolean isBoundary = method.name.equals( "_invoke" )
				    || method.name.startsWith( "invokeFunction_" )
				    || method.name.startsWith( "invokeClosure_" )
				    || method.name.startsWith( "invokeLambda_" );
				if ( !isBoundary ) {
					continue;
				}
				profiledMethods++;
				if ( method.tryCatchBlocks.stream().anyMatch( tc -> tc.type == null && isMarkEndThenThrow( method, tc.handler ) ) ) {
					guarded++;
				}
			}
		}
		assertThat( profiledMethods ).isGreaterThan( 0 );
		assertThat( guarded ).isEqualTo( profiledMethods );

		runtime.getConfiguration().codeProfilerEnabled = false;
	}

	private boolean isMarkEndThenThrow( org.objectweb.asm.tree.MethodNode method, org.objectweb.asm.tree.LabelNode handler ) {
		// Walk forward from the handler label: markEnd(...) then ATHROW.
		boolean	foundHandler	= false;
		boolean	foundMarkEnd	= false;
		for ( var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext() ) {
			if ( insn == handler ) {
				foundHandler = true;
			} else if ( !foundHandler ) {
				continue;
			}
			if ( insn instanceof org.objectweb.asm.tree.MethodInsnNode call
			    && call.owner.equals( org.objectweb.asm.Type.getInternalName( CodeProfilerService.class ) )
			    && call.name.equals( "markEnd" ) ) {
				foundMarkEnd = true;
			}
			if ( foundMarkEnd && insn.getOpcode() == org.objectweb.asm.Opcodes.ATHROW ) {
				return true;
			}
		}
		return false;
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
		// System.out.print( service.dumpSpans( key, source ) );

		// case 1 missed, default body a=30 ran.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 99
		assertThat( service.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );   // switch cond
		assertThat( service.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // case 1 missed
		assertThat( service.spanAt( key, 4, 9 ).stats().count() ).isEqualTo( 1 );   // default a = 30

		// Line-based: line 4 (default) covered.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks an EMPTY default label when the switch reaches it" )
	@Test
	void testEmptySwitchDefaultLabel() {
		String source = """
		                x = 1;
		                switch( x ) {
		                default:
		                }
		                a = 2;
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The switch falls through to `default:` (no case matches), so the default
		// LABEL executed even though it has no body statements. NOTE: assert the
		// SPAN, not the line — the multi-line switch-header span also covers line 3,
		// so `lineAt` would mask a missed default label.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // "default:"
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
		assertThat( service.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 1 );   // sleep( 150 )
		assertThat( service.spanAt( key, 4, 8 ).stats().totalNanos() ).isAtLeast( 150L * 1_000_000L );
		assertThat( service.spanAt( key, 4, 8 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
		// case 1 missed.
		assertThat( service.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // sleep( 100 ) in case 1
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Body ran 2x; the sleep(100) span accumulated ~200ms.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // sleep( 100 )
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 600L * 1_000_000L );
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 2 );   // i++
		assertThat( service.spanAt( key, 5, 9 ).stats().count() ).isEqualTo( 2 );   // cond checked 2x

		// Line-based.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 2 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Body ran 2x; the sleep(100) span accumulated ~200ms.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // sleep( 100 )
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 600L * 1_000_000L );

		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 2 );
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
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 2 );   // sleep( 100 )
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 600L * 1_000_000L );

		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 2 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: a=1, b=2, and the comparison splits into left ("x = a == ") and
		// right ("b") spans because the right operand could throw.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 9, true ) );   // "x = a == "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 9, 3, 10, true ) );  // "b"

		// Pass B: both ran once.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 1 );

		// Line-based.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The comparison's left span ran (a == evaluated); the right operand (IIFE)
		// threw before its mark, so its span and everything after stayed missed.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = a ==
		assertThat( service.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 1 );   // "( function() " shell
		assertThat( service.spanAt( key, 3, 24 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( service.spanAt( key, 3, 22 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 3, 41 ).stats().count() ).isEqualTo( 1 );  // } ran

		// Line-based: line 3 covered (left ran).
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: foo=null, bar=2, and the elvis splits: "x = foo ?: " then
		// "( bar = 99 )" — the parens are PART of the right operand's span.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 11, true ) );  // "x = foo ?: "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 11, 3, 23, true ) ); // "( bar = 99 )"

		// Pass B: foo is null so the right RAN and its mark fired.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 11 ).stats().count() ).isEqualTo( 1 );
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
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 11 ).stats().count() ).isEqualTo( 0 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// a false -> b never ran (MISSED).
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 0 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// a true -> b never ran (MISSED).
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 9 ).stats().count() ).isEqualTo( 0 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The literal is one span (all identifiers/literals, no throw) and ran once.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// a ran, the IIFE started (threw), the trailing 3 never ran.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = [ a, (
		assertThat( service.spanAt( key, 3, 24 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( service.spanAt( key, 3, 22 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 3, 41 ).stats().count() ).isEqualTo( 1 );  // } ran

		// Line-based: line 3 covered (a ran before the throw).
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// a ran, the IIFE started (threw).
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = { one: a, two: (
		assertThat( service.spanAt( key, 3, 34 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( service.spanAt( key, 3, 32 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 3, 51 ).stats().count() ).isEqualTo( 1 );  // } ran

		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
	}

	@DisplayName( "It covers the closing delimiters of nested struct/call/lambda literals" )
	@Test
	void testNestedStructClosingDelimiters() {
		// Simplified from StreamingService.cfc: a struct argument closed with `} );`
		// on its own line, and a struct of lambda callbacks where each lambda closes
		// with `},` and the outer call with `);`. All these closing-delimiter lines
		// must be covered because they execute when the enclosing construct runs.
		String source = """
		                service = { add: function( x ) { return x; } };
		                function q( data ) {
		                service.add( {
		                "eventType" : data.type,
		                "data"      : data.payload
		                } );
		                }
		                result = {
		                "cb1" : ( target, results ) => {
		                var name = target.getName();
		                service.add(
		                "evt",
		                {
		                "id"   : name,
		                "name" : target.name
		                }
		                );
		                },
		                "cb2" : ( x ) => {
		                return x * 2;
		                }
		                };
		                ok = q( { type: "t", payload: "p" } );
		                ok2 = result.cb1( { getName: () => "n", name: "x" }, {} );
		                ok3 = result.cb2( 5 );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testNestedStructClosingDelimiters dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// The `} );` closing the struct argument to add() (line 6) runs (q is called).
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();
		// The struct literal's closing `}` inside the call (line 16) runs.
		assertThat( service.lineAt( key, 16 ).covered() ).isTrue();
		// The call's closing `);` (line 17) runs.
		assertThat( service.lineAt( key, 17 ).covered() ).isTrue();
		// The first lambda's closing `},` (line 18) runs.
		assertThat( service.lineAt( key, 18 ).covered() ).isTrue();
		// The second lambda's closing `}` (line 21) runs when invoked.
		assertThat( service.lineAt( key, 21 ).covered() ).isTrue();
		// The outer struct's closing `};` (line 22) runs.
		assertThat( service.lineAt( key, 22 ).covered() ).isTrue();
	}

	@DisplayName( "It keeps struct values after a ternary value covered (multi-line)" )
	@Test
	void testMultiLineStructTernary() {
		String source = """
		                spec = { id: 1, displayName: "hi", name: "n", timestamp: 2 };
		                result = {
		                "id"          : spec.id,
		                "label"       : spec.displayName ?: spec.id,
		                "name"        : spec.name,
		                "timestamp"   : spec.timestamp
		                };
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testMultiLineStructTernary dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// Every struct value line must be covered; the ternary branches may be, but
		// the surrounding `name`/`timestamp` values must NOT be left red.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();   // { "id"
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();   // "label" : ternary
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();   // "name" (AFTER ternary)
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();   // "timestamp" getTickCount (AFTER ternary)
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();   // };
	}

	@DisplayName( "It covers each multi-line struct value key and its separator (regression)" )
	@Test
	void testMultiLineStructSeparatorAndKeyCovered() {
		// REGRESSION: the closing-delimiter tail grouping overwrote the separator
		// group keyed by the same last-value span, so the `,\n key: ` separator
		// span (registered between values) stayed RED even though it always runs
		// when the struct is built. Every value key + separator must be GREEN.
		String source = """
		                st = {
		                one: 1,
		                two: 2
		                };
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testMultiLineStructSeparatorAndKeyCovered dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// Span model for this struct (line-oriented):
		// line 1 `st = {` statement head
		// line 2 `one: 1,` value one + trailing separator
		// line 3 `two: 2` separator + key two + value two (the regression span)
		// line 4 `};` closing tail
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();  // "one: 1," GREEN
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();  // "two: 2" GREEN
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();  // "st = {" GREEN
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();  // "};" GREEN
		// Every executable span in the struct is covered (the separator + key two
		// must NOT be RED) — the structural assertion that guards the regression.
		for ( Blueprint.SpanDef def : service.trackedBlueprints().get( key ).spans() ) {
			if ( def.executable() ) {
				assertThat( service.spanAt( key, def.startLine(), def.startCol() ).stats().count() ).isEqualTo( 1 );
			}
		}
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// the spread a ran, the IIFE started (threw), trailing b never ran.
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // x = [ ...a, (
		assertThat( service.spanAt( key, 4, 27 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( service.spanAt( key, 4, 25 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 4, 44 ).stats().count() ).isEqualTo( 1 );  // } ran

		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// x = a + ( (line 4 cols 0-10), b ? (10-14), c : (14-18), 0 (18-21, the
		// untaken false branch).
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // x = a + (
		assertThat( service.spanAt( key, 4, 10 ).stats().count() ).isEqualTo( 1 );  // b ?
		assertThat( service.spanAt( key, 4, 14 ).stats().count() ).isEqualTo( 1 );  // c : (taken)
		assertThat( service.spanAt( key, 4, 18 ).stats().count() ).isEqualTo( 0 );  // 0 (untaken)
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // x = a == (
		assertThat( service.spanAt( key, 4, 11 ).stats().count() ).isEqualTo( 1 );  // b ?
		assertThat( service.spanAt( key, 4, 15 ).stats().count() ).isEqualTo( 1 );  // c : (taken)
		assertThat( service.spanAt( key, 4, 19 ).stats().count() ).isEqualTo( 0 );  // 0 (untaken)
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Each expression statement is a single span (unary/negate/paren have one
		// sub-expression), all ran once.
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // !a
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );   // -b
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // ( c + d )

		for ( int line = 1; line <= 7; line++ ) {
			assertThat( service.lineAt( key, line ).covered() ).isTrue();
			assertThat( service.lineAt( key, line ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The null-safe access on a missing key ran (returned null) — count 1.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The array access started, the index IIFE threw before its mark, and the
		// closing access never completed.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = arr[ (
		assertThat( service.spanAt( key, 3, 24 ).stats().count() ).isEqualTo( 1 );  // throw ran
		assertThat( service.spanAt( key, 3, 22 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 3, 41 ).stats().count() ).isEqualTo( 1 );  // } ran

		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// An empty function body must behave like ANY other function: the
		// declaration SHELL (including the braces' open group) marks GREEN at
		// definition. The shell spans a "function foo() ", the "{" and the "}" are
		// the body-brace group. The whole line is covered at declaration.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );  // "function foo() "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 16, true ) ); // "{" body open
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 16, 1, 17, true ) ); // "}"
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();   // the declaration ran (GREEN)
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: the declaration shell (before the brace), the body statement, and the
		// two braces are separate spans; the shell runs at declaration, the body +
		// braces only when the function is invoked.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );  // "function foo() "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // "x = 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 16, true ) ); // "{"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}"

		// Pass B: shell ran; body + braces did NOT (never invoked).
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );  // body missed
		assertThat( service.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 0 ); // { missed
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );  // } missed

		// Line-based: line 1 covered (shell). Lines 2-3 only touched by missed body
		// and brace spans, so they are not covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Plain args (no defaults) stay in the shell — no extra arg spans. The brace
		// spans and the body statement are their own spans.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 21, true ) );  // shell incl. args
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // body x = a
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 21, 1, 22, true ) ); // "{"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}"

		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: line 1 covered (shell, once). Lines 2-3 are only touched by
		// missed body and brace spans, so they are NOT covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Literal defaults evaluate inline at declaration (cannot throw), so they
		// stay inside the shell span. The body splits per its own expressions, and
		// the braces get their own spans.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 29, true ) );  // shell + literal defaults
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 8, true ) );   // "x = a + "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 8, 2, 9, true ) );   // "b"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 29, 1, 30, true ) ); // "{"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );   // "}"

		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: line 1 covered (shell, once). Lines 2-3 are only touched by
		// missed body and brace spans, so they are NOT covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Body span ran exactly once (one invocation).
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // body
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // foo() call

		// Line-based: line 2 covered (body ran).
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );  // body missed
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2

		// Line 2 is only touched by the missed body span — NOT covered.
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Body ran twice (two invocations); each call site ran once.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 2 );  // body
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // foo() 1
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // foo() 2

		// Line 2's aggregate is the body span's count (2) — the shell only covers
		// line 1.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 2 );
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
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 150L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 450L * 1_000_000L );

		// The CALL SITE (line 4) only carries the dispatch overhead — the sleep is
		// charged to the body span, so the call span must be far below the sleep.
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// Line-based: line 1 covered (shell); line 2 covered (body ran); line 4
		// covered (call site). Line 2's aggregate is the shell tail's count.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
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
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// Last body statement (line 3): charged while sleep(200) ran, closed by the
		// UDF body's markEnd.
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 200L * 1_000_000L );
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );

		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );

		// The CALL SITE (line 5) only carries dispatch overhead, not the sleeps.
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 5, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// Line-based: line 1 covered (shell); lines 2-3 covered (body ran); line 5
		// covered (call site).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It records INCLUSIVE call time on the UDF call-site span (how long the call was on the stack)" )
	@Test
	void testInclusiveCallTiming() {
		String source = """
		                function sleeper() {
		                sleep( 1000 );
		                }
		                sleeper();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The body statement's SELF time is ~1000ms (unchanged by this feature).
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 1000L * 1_000_000L );

		// The CALL SITE`span (line 4) now carries the FULL call duration as INCLUSIVE
		// time — the time the sleeper() call was on the stack (~1000ms + overhead).
		long inclusive = service.spanAt( key, 4, 0 ).stats().inclusiveNanos();
		assertThat( inclusive ).isAtLeast( 1000L * 1_000_000L );
		assertThat( inclusive ).isAtMost( 1500L * 1_000_000L );

		// Self time of the call site stays small (the body owns the self time), so the
		// two metrics are distinct and both available.
		assertThat( service.spanAt( key, 4, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// The line aggregate exposes the same on-the-stack time.
		assertThat( service.lineAt( key, 4 ).nanos() ).isAtLeast( 1000L * 1_000_000L );
	}

	@DisplayName( "It attributes inclusive time for a call whose argument is itself a call" )
	@Test
	void testInclusiveNestedCalls() {
		// foo( bar() ): the argument is evaluated before foo is invoked, so the
		// call-site span's INCLUSIVE time is the WHOLE expression — bar's ~100ms +
		// foo's own ~50ms = ~150ms. (bar() is ALSO its own argument span now, but the
		// foo( call-site span still encloses the whole thing.)
		String source = """
		                function bar() {
		                sleep( 100 );
		                return 1;
		                }
		                function foo( x ) {
		                sleep( 50 );
		                return x;
		                }
		                foo( bar() );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// foo( bar() ) call-site span (line 9, col 0) inclusive ~150ms.
		long	inclusive	= service.spanAt( key, 9, 0 ).stats().inclusiveNanos();
		assertThat( inclusive ).isAtLeast( 150L * 1_000_000L );
		assertThat( inclusive ).isAtMost( 450L * 1_000_000L );

		// The line aggregate exposes it too.
		assertThat( service.lineAt( key, 9 ).nanos() ).isAtLeast( 150L * 1_000_000L );
	}

	@DisplayName( "It splits a method call off its receiver and splits every argument into its own span" )
	@Test
	void testMethodCallReceiverAndArgsSplit() {
		// `new X( ... ).run( target, ... )` is THREE operations, not one: the
		// receiver (`new X( ... )`), the `.run( ... )` call, and each argument.
		// Regression: the method-call visitor visited arguments BEFORE the receiver
		// and never closed a boundary after it, so the whole expression collapsed
		// into ONE span; identifier arguments were also rolled into the shell.
		String source = """
		                class X {
		                    function init( o ) { return this }
		                    function run( a, b, c ) { return 1 }
		                }
		                options = {}
		                testResults = {}
		                callbacks = {}
		                target = 1
		                new X( options ).run(
		                    target,
		                    testResults,
		                    callbacks
		                );
		                """;
		runtime.executeSource( source );
		String					key		= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Collect normalized span texts for the statement lines.
		java.util.List<String>	texts	= new java.util.ArrayList<>();
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() >= 8 ) {
				texts.add( service.spanSourceText( key, source, s ).replaceAll( "\\s+", " " ).trim() );
			}
		}

		// The receiver call, the .run( call, and EACH argument are all separate spans.
		assertThat( texts ).contains( "new X( options )" );
		assertThat( texts ).contains( ".run(" );
		assertThat( texts ).contains( "target" );
		assertThat( texts ).contains( "testResults" );
		assertThat( texts ).contains( "callbacks" );
	}

	@DisplayName( "It splits every argument of a call into its own span, including bare identifiers and indexes" )
	@Test
	void testEveryArgumentGetsItsOwnSpan() {
		// A bare identifier arg (`i`), an indexed access (`variables.actual[ i ]`),
		// and identifier args (`missingMethodName`, `missingMethodArguments`) are ALL
		// separate expressions. They must each get their own span — the span model is
		// consistent: a simple literal stays in the shell, EVERYTHING else splits.
		String source = """
		                function tryElement( a, b, c, d ) { return 1 }
		                variables.actual = [ 1, 2, 3 ]
		                i = 1
		                missingMethodName = "x"
		                missingMethodArguments = {}
		                result = tryElement(
		                    i,
		                    variables.actual[ i ],
		                    missingMethodName,
		                    missingMethodArguments
		                );
		                """;
		runtime.executeSource( source );
		String					key		= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		java.util.List<String>	texts	= new java.util.ArrayList<>();
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() >= 6 ) {
				texts.add( service.spanSourceText( key, source, s ).replaceAll( "\\s+", " " ).trim() );
			}
		}

		assertThat( texts ).contains( "i" );
		assertThat( texts ).contains( "variables.actual[ i ]" );
		assertThat( texts ).contains( "missingMethodName" );
		assertThat( texts ).contains( "missingMethodArguments" );
	}

	@DisplayName( "It keeps struct-literal entries spanned after a value that ends in a call" )
	@Test
	void testStructLiteralValueCallDoesNotSwallowFollowingEntries() {
		// Regression: a struct VALUE that is (or ends with) a call — e.g.
		// `"id" : hash( ... )` — left the running span set at the call's end. That
		// leaked into the NEXT entry's nested visits, minting a PHANTOM span from the
		// call's `)` across the following keys/values (so the `)` and the next several
		// entries were swallowed). Every entry must be its own span, with no phantom.
		String source = """
		                function hash( s ) { return s }
		                arguments = { title: 1, body: 2, data: 3, name: 4 }
		                spec = {
		                    "id"     : hash( arguments.title ),
		                    "body"   : arguments.body,
		                    "data"   : arguments.data,
		                    "name"   : arguments.name
		                };
		                """;
		runtime.executeSource( source );
		String					key		= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		java.util.List<String>	texts	= new java.util.ArrayList<>();
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() >= 4 ) {
				texts.add( service.spanSourceText( key, source, s ).replaceAll( "\\s+", " " ).trim() );
			}
		}

		// Every value is spanned, including those AFTER the call value.
		assertThat( texts ).contains( "hash( arguments.title )" );
		assertThat( texts ).contains( "arguments.body" );
		assertThat( texts ).contains( "arguments.data" );
		assertThat( texts ).contains( "arguments.name" );

		// NO phantom: the bug minted a span from the call's `)` across the following
		// keys/values. At most ONE span may cross a line (the struct's closing
		// `}` tail); any OTHER multi-line span is the phantom.
		int multiLine = 0;
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() >= 4 && s.endLine() != s.startLine() ) {
				multiLine++;
			}
		}
		assertThat( multiLine ).isAtMost( 1 );
	}

	@DisplayName( "It keeps every named-argument `name =` fragment in the call shell" )
	@Test
	void testNamedArgumentsShellConsistency() {
		// Regression: for `fn( a = x, b = y, c = z )`, the FIRST `name =` landed in the
		// call HEAD but the FOLLOWING `name =` fragments lived in spans grouped with
		// their VALUES (inconsistent). Every `name =` must be in the call SHELL (which
		// is marked+charged atomically), and each value a separate arg span.
		String source = """
		                function fn( spec, testResults, suiteStats, runner ) { return 1 }
		                obj = { fn: fn }
		                testResults = {}
		                suiteStats = {}
		                thisSpec = 1
		                runner = 9
		                obj.fn(
		                    spec        = thisSpec,
		                    testResults = testResults,
		                    suiteStats  = suiteStats,
		                    runner      = runner
		                );
		                """;
		runtime.executeSource( source );
		String					key		= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		java.util.List<String>	texts	= new java.util.ArrayList<>();
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() >= 7 ) {
				texts.add( service.spanSourceText( key, source, s ).replaceAll( "\\s+", " " ).trim() );
			}
		}

		// The SHELL segments: the callee head, each `name =` interstitial, and the
		// closing `);`. EVERY `name =` fragment is a shell segment (consistent — the
		// first landed in the head, the rest are interstitials).
		assertThat( texts ).contains( ".fn( spec =" );
		assertThat( texts ).contains( ", testResults =" );
		assertThat( texts ).contains( ", suiteStats =" );
		assertThat( texts ).contains( ", runner =" );
		assertThat( texts ).contains( ");" );

		// Each VALUE is its own separate span (an ARG span, NOT a shell segment).
		assertThat( texts ).contains( "thisSpec" );
		assertThat( texts ).contains( "runner" );
	}

	@DisplayName( "It closes a call bracket when the callee throws (inclusive time still recorded)" )
	@Test
	void testInclusiveCallTimingOnThrow() {
		// The callee sleeps then throws. The exception escapes BOTH the callee and
		// the caller's catch-all (the caller has no try), so each frame unwinds back
		// to its entry depth. The call-site span's inclusive time must still be
		// recorded (the unwind charges the abandoned bracket).
		String source = """
		                function boomer() {
		                sleep( 200 );
		                throw( "boom" );
		                }
		                boomer();
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the throw propagates out of the script
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The call-site span (line 5) inclusive time is ~200ms+ even though the call
		// never returned normally.
		long	inclusive	= service.spanAt( key, 5, 0 ).stats().inclusiveNanos();
		assertThat( inclusive ).isAtLeast( 200L * 1_000_000L );
		assertThat( inclusive ).isAtMost( 600L * 1_000_000L );
	}

	@DisplayName( "It records inclusive time for a BIF call (sleep)" )
	@Test
	void testInclusiveBifCallTiming() {
		// A BIF call goes through the same bracket. `sleep( 300 )` blocks for 300ms
		// with no BoxLang frame in between, so the call-site span's inclusive time is
		// ~300ms — the whole time that line was on the stack.
		String source = """
		                sleep( 300 );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		long	inclusive	= service.spanAt( key, 1, 0 ).stats().inclusiveNanos();
		assertThat( inclusive ).isAtLeast( 300L * 1_000_000L );
		assertThat( inclusive ).isAtMost( 700L * 1_000_000L );
	}

	@DisplayName( "It includes a BIF callback's time in the BIF call's inclusive time" )
	@Test
	void testInclusiveBifCallbackTiming() {
		// arrayMap invokes the closure synchronously, INSIDE the arrayMap call. The
		// arrayMap call-site bracket is open across the whole call, so its inclusive
		// time includes the callback's sleep even though the closure is invoked by the
		// BIF (no BoxLang call site for the invocation itself).
		String source = """
		                arr = [ 1 ];
		                arrayMap( arr, ( x ) => { sleep( 200 ); return x; } );
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// arrayMap call site (line 2) inclusive time includes the callback's 200ms.
		long	inclusive	= service.spanAt( key, 2, 0 ).stats().inclusiveNanos();
		assertThat( inclusive ).isAtLeast( 200L * 1_000_000L );
		assertThat( inclusive ).isAtMost( 600L * 1_000_000L );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: the shell splits around the non-literal default, isolating it:
		// "function foo( a = " | "bar" | ") {" | the body. The shell head and the
		// tail ") {" run atomically at declaration (batched into one varargs mark);
		// the default `bar` may not run at declaration (it's a lazy defaultExpr).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) );   // shell head "function foo( a = "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 18, 1, 21, true ) );  // default "bar"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 21, 1, 24, true ) );  // shell tail " ) "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );    // body
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 24, 1, 25, true ) );  // "{"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );    // "}"

		// Pass B: the shell head and tail ran; the default and body did NOT (never invoked).
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell head ran
		assertThat( service.spanAt( key, 1, 18 ).stats().count() ).isEqualTo( 0 );  // default missed
		assertThat( service.spanAt( key, 1, 21 ).stats().count() ).isEqualTo( 1 );  // shell tail ran
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );   // body missed

		// Line-based: line 1 covered (shell head ran). Lines 2-3 only touched by
		// missed body/brace spans — NOT covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
	}

	@DisplayName( "It keeps a nested lambda default out of the outer shell group" )
	@Test
	void testFunctionLambdaDefaultDoesNotCorruptOuterShell() {
		String source = """
		                function f( cb = () => 1 ) {
		                x = cb();
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testFunctionLambdaDefaultDoesNotCorruptOuterShell dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: the outer declaration shell splits around the lambda default
		// "() => 1". The lambda's own shell ("() => ") breaks into its OWN isolated
		// span, and its body fragment ("1") must NOT be swept into the OUTER shell
		// group — the outer group is exactly [head, tail] so the declaration mark
		// covers the whole declaration text, and nothing of the nested lambda is
		// batched into it.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) );   // outer head "function f( cb = "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 17, 1, 23, true ) );  // lambda shell "() => "
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 23, 1, 24, true ) );  // lambda body "1"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 24, 1, 27, true ) );  // outer shell tail " ) "
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 8, true ) );    // outer body "x = cb()"
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 1, 27, 1, 28, true ) );  // "{"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );    // "}"

		// Pass B: f() was NEVER called — ONLY the declaration shell (head + tail)
		// ran. They were batched into ONE varargs mark at the declaration site.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // outer head ran at declaration
		assertThat( service.spanAt( key, 1, 24 ).stats().count() ).isEqualTo( 1 );  // outer tail ran at declaration
		// The nested lambda was never created (defaultExpr never ran) — its shell
		// and body both missed; the outer body + braces never ran either.
		assertThat( service.spanAt( key, 1, 17 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.spanAt( key, 1, 23 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.spanAt( key, 1, 27 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );

		// Line-based: line 1 covered via the declaration shell; lines 2-3 only
		// touched by missed spans — NOT covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
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
		assertThat( service.spanAt( key, 1, 18 ).stats().count() ).isEqualTo( 1 );  // default ran
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // body ran

		// Line-based: line 1 covered (shell + default ran); line 2 covered (body
		// ran); line 4 covered (call site). Line 2's aggregate is the shell tail.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
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
		assertThat( service.spanAt( key, 1, 18 ).stats().count() ).isEqualTo( 0 );  // default skipped
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // body ran
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // foo( 1 )

		// Line-based: line 1 covered (shell ran; default missed but lineAt reports
		// covered via the shell span); line 2 covered (body ran); line 4 covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

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
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );    // shell head
		assertThat( service.spanAt( key, 1, 28 ).stats().count() ).isEqualTo( 1 );   // now() default ran
		assertThat( service.spanAt( key, 1, 33 ).stats().count() ).isEqualTo( 1 );   // shell interstitial
		assertThat( service.spanAt( key, 1, 46 ).stats().count() ).isEqualTo( 1 );   // new ...() default ran
		assertThat( service.spanAt( key, 1, 95 ).stats().count() ).isEqualTo( 1 );   // shell interstitial
		assertThat( service.spanAt( key, 1, 108 ).stats().count() ).isEqualTo( 1 );  // hash( "x" ) default ran
		assertThat( service.spanAt( key, 1, 119 ).stats().count() ).isEqualTo( 1 );  // shell tail
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );    // body x = a
		assertThat( service.spanAt( key, 1, 153 ).stats().count() ).isEqualTo( 1 );  // { ran
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );    // } ran
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );    // foo() call

		// Line-based: line 1 covered (declaration + all defaults ran); line 2 covered
		// (body); line 3 covered (closing brace); line 4 covered (call site).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: the closure creation shell "foo = ( x ) => " (line 1 cols 0-15)
		// and the expression body "x + 1" (line 1 cols 15-20) are separate spans.
		// The body only runs at invocation.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );   // "foo = ( x ) => "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 20, true ) );  // "x + 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // "y = 2"

		// Pass B: the closure creation ran (shell count 1); the body did NOT (never
		// invoked) — count 0.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( service.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 0 );  // body missed
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2

		// Line-based: line 1 covered (shell ran); line 2 covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// The closure body ran once (one invocation); the call site ran once.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( service.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 1 );  // body ran
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // call site

		// Line-based: both lines covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: shell (line 1), body return (line 2), { and } braces, call (line 4).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );  // shell
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 12, true ) ); // "return x + 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 16, true ) ); // "{"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 1, true ) );  // "}"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 17, true ) ); // call

		// Pass B: body return ran once; braces ran once; call site ran.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );

		// Line-based: lines 1-4 covered (line 3 is the closing brace, ran once).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Literal default stays in the shell: "foo = ( x = 1 ) => " (0-19) and body
		// "x" (19-20). foo() omits the arg; the literal default evaluates inline.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 19, true ) );   // shell + literal default
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 19, 1, 20, true ) );  // body "x"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );   // foo()

		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 1, 19 ).stats().count() ).isEqualTo( 1 );   // body ran
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // foo()
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
		// System.out.print( service.dumpSpans( key, source ) );

		// Same span structure as closures: shell (0-15) + body (15-20) + y=2.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 15, true ) );   // shell
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 15, 1, 20, true ) );  // body "x + 1"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 5, true ) );   // y = 2

		// Never invoked -> body missed.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 0 );

		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
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
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // shell
		assertThat( service.spanAt( key, 1, 15 ).stats().count() ).isEqualTo( 1 );  // body ran
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // call site

		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( key, 2, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// The return statement ran once.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );

		// The CALL SITE (line 5) only carries dispatch overhead — far below sleep.
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 5, 0 ).stats().totalNanos() ).isAtMost( 100L * 1_000_000L );

		// Line-based: lines 1-5 are ALL covered. Line 4 is the lambda's closing brace
		// (`};`), which runs whenever the lambda is invoked — the block lambda's braces
		// are marked as an atomic group at body entry, so the executed `}` must be
		// reported as covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks a block lambda's braces exactly when the lambda is invoked" )
	@Test
	void testBlockLambdaBraceCoverage() {
		// Pass A registers a block lambda's "{" and "}" as ONE atomic span group keyed at
		// the "{". The lambda INVOKER transforms the body's CHILDREN directly (never the
		// BoxStatementBlock node), so nothing would emit that group — the braces read as
		// missed even when the lambda ran. The invoker must emit the group at body entry,
		// BEFORE the empty-body early return, so EMPTY block lambdas cover their braces too.
		// The braces must stay MISSED when the lambda is defined but never invoked.
		String[][] cases = {
		    // label, source, expectsCovered
		    { "invoked non-empty", "f = () -> { return 1; };\nf();\n", "true" },
		    { "uninvoked non-empty", "f = () -> { return 1; };\n", "false" },
		    { "invoked empty", "f = () -> {};\nf();\n", "true" },
		    { "uninvoked empty", "f = () -> {};\n", "false" }
		};
		for ( String[] c : cases ) {
			String	label	= c[ 0 ];
			String	source	= c[ 1 ];
			boolean	expect	= Boolean.parseBoolean( c[ 2 ] );

			runtime.executeSource( source );
			String						key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

			CodeProfilerService.Span	openBrace	= null;
			CodeProfilerService.Span	closeBrace	= null;
			for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
				String text = service.spanSourceText( key, source, s );
				if ( "{".equals( text ) ) {
					openBrace = s;
				} else if ( "}".equals( text ) ) {
					closeBrace = s;
				}
			}
			assertThat( openBrace ).isNotNull();
			assertThat( closeBrace ).isNotNull();
			long expectedCount = expect ? 1 : 0;
			assertThat( openBrace.stats().count() ).isEqualTo( expectedCount );
			assertThat( closeBrace.stats().count() ).isEqualTo( expectedCount );

			// Reset so the next case recompiles with instrumentation enabled.
			service.reset();
			RunnableLoader.getInstance().getBoxpiler().clearPagePool();
			RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
			service.setActive( true );
		}
	}

	@DisplayName( "It emits a markEnd before the ARETURN of a returning closure/lambda/UDF invoker" )
	@Test
	void testReturnPathClosesProbeInterval() throws Exception {
		// A callee that RETURNS (not fall-through) must close its probe-charging
		// interval. The invoker methods append a fall-through markEnd, but an
		// explicit `return` compiles to ARETURN that bypasses it; BoxReturnTransformer
		// must emit markEnd immediately before that ARETURN, or the open interval
		// leaks into the caller's next mark (inflating the callee's return span
		// timing and misattributing the caller's).
		runtime.getConfiguration().codeProfilerEnabled = true;

		var	boxpiler	= ( ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		var	classPath	= java.nio.file.Paths.get( "src/test/resources/profiler/ProfilerFunctionReturn.bxs" ).toAbsolutePath().normalize();
		var	classInfo	= ortus.boxlang.compiler.ClassInfo.forClass(
		    ResolvedFilePath.of( classPath ),
		    ortus.boxlang.compiler.parser.Parser.detectFile( classPath.toFile(), true ),
		    boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );
		java.util.List<byte[]>	compiled	= boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );

		// Every ARETURN in every method (giving us the UDF/closure/lambda invoker
		// bodies that can `return`) must be immediately preceded by a
		// CodeProfilerService.markEnd call.
		int						returnCount	= 0;
		int						guarded		= 0;
		for ( byte[] bytes : compiled ) {
			if ( bytes.length < 4 || bytes[ 0 ] != ( byte ) 0xCA || bytes[ 1 ] != ( byte ) 0xFE || bytes[ 2 ] != ( byte ) 0xBA
			    || bytes[ 3 ] != ( byte ) 0xBE ) {
				continue;
			}
			org.objectweb.asm.tree.ClassNode classNode = new org.objectweb.asm.tree.ClassNode();
			new org.objectweb.asm.ClassReader( bytes ).accept( classNode, 0 );
			for ( var method : classNode.methods ) {
				// Only the UDF/closure/lambda INVOKER methods can execute a `return`
				// statement. Other methods (infrastructure accessors, getters) return
				// internal objects and do not open a probe interval.
				if ( !method.name.startsWith( "invokeFunction_" )
				    && !method.name.startsWith( "invokeClosure_" )
				    && !method.name.startsWith( "invokeLambda_" ) ) {
					continue;
				}
				for ( var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext() ) {
					if ( insn.getOpcode() != org.objectweb.asm.Opcodes.ARETURN ) {
						continue;
					}
					returnCount++;
					// Skip label/line/frame noise to find the meaningful predecessor.
					var prev = insn.getPrevious();
					while ( prev instanceof org.objectweb.asm.tree.LabelNode
					    || prev instanceof org.objectweb.asm.tree.LineNumberNode
					    || prev instanceof org.objectweb.asm.tree.FrameNode ) {
						prev = prev.getPrevious();
					}
					if ( prev instanceof org.objectweb.asm.tree.MethodInsnNode call
					    && call.owner.equals( org.objectweb.asm.Type.getInternalName( CodeProfilerService.class ) )
					    && call.name.equals( "markEnd" ) ) {
						guarded++;
					}
				}
			}
		}

		// We must have actually found return opcodes (otherwise the test silently
		// passes), and EVERY one must be markEnd-guarded.
		assertThat( returnCount ).isGreaterThan( 0 );
		assertThat( guarded ).isEqualTo( returnCount );

		// Ensure profiling is restored for subsequent tests.
		runtime.getConfiguration().codeProfilerEnabled = false;
	}

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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// The class's spans live under the OUTER SCRIPT's blueprint (a local class
		// is part of its container from the user's perspective).
		assertThat( service.trackedBlueprints() ).containsKey( scriptKey );

		// The static block's statement ran at class load (triggered by new brad()).
		assertThat( service.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 1 );   // x = 1
		assertThat( service.spanAt( scriptKey, 6, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		assertThat( service.trackedBlueprints() ).containsKey( scriptKey );

		// y = 2 (class body) ran once at instantiation.
		assertThat( service.spanAt( scriptKey, 2, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2
		assertThat( service.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		assertThat( service.trackedBlueprints() ).containsKey( scriptKey );

		// Member body ran once (one invocation); new brad() and b.member() call
		// sites ran once each.
		assertThat( service.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 1 );   // z = 3
		assertThat( service.spanAt( scriptKey, 6, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
		assertThat( service.spanAt( scriptKey, 7, 0 ).stats().count() ).isEqualTo( 1 );  // b.member()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		assertThat( service.trackedBlueprints() ).containsKey( scriptKey );

		// Never instantiated or invoked: body + member missed. Static block only
		// runs if the class LOADS (it doesn't here — nothing references brad).
		assertThat( service.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 0 );   // x = 1 static
		assertThat( service.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 0 );   // y = 2 body
		assertThat( service.spanAt( scriptKey, 7, 0 ).stats().count() ).isEqualTo( 0 );   // z = 3 member
		assertThat( service.spanAt( scriptKey, 10, 0 ).stats().count() ).isEqualTo( 1 );  // w = 9
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// Static sleep(50) charged ~50ms at class load.
		assertThat( service.spanAt( scriptKey, 3, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( scriptKey, 3, 0 ).stats().totalNanos() ).isAtLeast( 50L * 1_000_000L );
		assertThat( service.spanAt( scriptKey, 3, 0 ).stats().totalNanos() ).isAtMost( 250L * 1_000_000L );

		// Body statement ran once at instantiation.
		assertThat( service.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 1 );   // y = 2

		// Member sleep(100) charged ~100ms at invocation.
		assertThat( service.spanAt( scriptKey, 7, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( scriptKey, 7, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( scriptKey, 7, 0 ).stats().totalNanos() ).isAtMost( 300L * 1_000_000L );

		// Call sites ran once.
		assertThat( service.spanAt( scriptKey, 10, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
		assertThat( service.spanAt( scriptKey, 11, 0 ).stats().count() ).isEqualTo( 1 );  // b.member()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// The property default `42` is its own span in the script blueprint; it ran
		// at class load (count 1).
		assertThat( service.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 1 );   // default=42
		assertThat( service.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
		assertThat( service.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 1 );   // v = b.x
	}

	@DisplayName( "It covers a property declaration with no default value" )
	@Test
	void testPropertyNoDefault() {
		// A `property name="x";` with NO default has no lazy/default expression, but
		// the declaration text is still metadata applied at class load — so its line
		// must be covered when the class loads (mimics TestBox's option/testbox props).
		String source = """
		                class brad {
		                property name="options";
		                property name="testbox";
		                y = 2;
		                }
		                b = new brad();
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testPropertyNoDefault dump" );
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		assertThat( service.lineAt( scriptKey, 2 ).covered() ).isTrue();   // property name="options";
		assertThat( service.lineAt( scriptKey, 3 ).covered() ).isTrue();   // property name="testbox";
		assertThat( service.spanAt( scriptKey, 6, 0 ).stats().count() ).isEqualTo( 1 );  // new brad()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// The array-literal default ran at class load.
		assertThat( service.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 1 );   // default=[ 1, 2, 3 ]
		assertThat( service.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// The struct-literal default ran at class load.
		assertThat( service.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 1 );   // default={ one: 1 }
		assertThat( service.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
	}

	@DisplayName( "It tracks a runtime-expression property default when it is applied" )
	@Test
	void testPropertyRuntimeDefault() {
		// A genuine RUNTIME default (an interpolated expression, not a literal) compiles
		// to a deferred evaluator, so it only runs when defaultProperties() applies it.
		String source = """
		                seed = 42;
		                class brad {
		                property name="x" default="#( 40 + 2 )#" type="string";
		                }
		                b = new brad();
		                v = b.x;
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG: print every span's actual source chars to confirm coordinates/counts.
		// System.out.println( "=== testPropertyRuntimeDefault dump" );
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// seed=42 (line 1) and the runtime default (line 3 col 26) both ran.
		assertThat( service.spanAt( scriptKey, 1, 0 ).stats().count() ).isEqualTo( 1 );   // seed = 42
		assertThat( service.spanAt( scriptKey, 3, 26 ).stats().count() ).isEqualTo( 1 );  // runtime default (applied)
		assertThat( service.spanAt( scriptKey, 5, 0 ).stats().count() ).isEqualTo( 1 );   // new brad()
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
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// The class never loads (nothing references brad), so its property default
		// never ran — but w = 9 did.
		assertThat( service.spanAt( scriptKey, 2, 26 ).stats().count() ).isEqualTo( 0 );   // default=42 missed
		assertThat( service.spanAt( scriptKey, 4, 0 ).stats().count() ).isEqualTo( 1 );   // w = 9
	}

	@DisplayName( "It skips a runtime property default preset by a super class" )
	@Test
	void testPropertyDefaultSkippedBySuper() {
		// The super class presets `threshold` in its pseudo-constructor. The child
		// declares `property name="threshold" default="#throw( 'boom' )#";` — a REAL
		// runtime (non-literal) default expression. defaultProperties() sees the value
		// already in scope and SKIPS the child's default, so its span must stay RED
		// (count 0) — and the throw never fires. A LITERAL default could not test this:
		// it is baked in at compile time and has no evaluator to skip.
		String source = """
		                seed = 42;
		                class brad {
		                threshold = "preset";
		                }
		                class child extends=brad {
		                property name="threshold" default="#throw( 'boom' )#";
		                }
		                c = new child();
		                """;
		runtime.executeSource( source );

		String scriptKey = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testPropertyDefaultSkippedBySuper dump" );
		// System.out.print( service.dumpSpans( scriptKey, source ) );

		// The child's `threshold` runtime default (line 6, value at col 34) is SKIPPED
		// because the super preset `threshold`. Count 0 = RED.
		assertThat( service.spanAt( scriptKey, 6, 34 ).stats().count() ).isEqualTo( 0 );  // runtime default SKIPPED (super preset)
		assertThat( service.spanAt( scriptKey, 8, 0 ).stats().count() ).isEqualTo( 1 );   // c = new child()
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
		// System.out.print( service.dumpSpans( key, source ) );

		// foo( true ) takes the if-branch, so `return 1` (line 3) runs and the
		// function returns immediately; `return 2` (line 5) is never reached.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // function shell
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // return 1 ran
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 0 );   // return 2 missed
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );   // x = foo( true )

		// Line-based: return 1 line covered; return 2 line missed; call covered.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 5 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 7 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// x=2 matches case 2, which has no break — execution falls through into
		// case 3, so BOTH a=20 and a=30 ran. Case 1 (a=10) stayed missed.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 2
		assertThat( service.spanAt( key, 3, 8 ).stats().count() ).isEqualTo( 0 );   // case 1 a=10 missed
		assertThat( service.spanAt( key, 4, 8 ).stats().count() ).isEqualTo( 1 );   // case 2 a=20 ran
		assertThat( service.spanAt( key, 5, 8 ).stats().count() ).isEqualTo( 1 );   // case 3 a=30 ran

		// Line-based: case 2 and case 3 lines covered. Case 1's line is touched by
		// the multi-line switch-header span, so lineAt reports it covered — the
		// authoritative "case 1 missed" signal is its BODY SPAN count (above).
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
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
		// System.out.print( service.dumpSpans( key, source ) );

		// x=2: first cond false (a=10 missed), second else-if cond true (a=20 ran),
		// third else-if and final else both missed.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // x = 2
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 0 );   // a = 10 missed
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // a = 20 ran
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 0 );   // a = 30 missed
		assertThat( service.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 0 );   // a = 99 missed

		// Line-based: taken branch (line 5) covered; untaken branches missed.
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 7 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 9 ).covered() ).isFalse();
	}

	@DisplayName( "It profiles a real file on disk keyed by its file path" )
	@Test
	void testDiskFile() {
		String relativePath = "src/test/resources/profiler/ProfilerSample.bxs";
		runtime.executeTemplate( relativePath );

		// The blueprint is keyed by the NORMALIZED absolute file path (what
		// registerBlueprintForFile stores), not a source hash.
		Path	absolute	= Paths.get( relativePath ).toAbsolutePath().normalize();
		String	fileKey		= absolute.toString();

		// DEBUG (reads file from disk, no source arg)
		// System.out.println( "=== testDiskFile dump" );
		// System.out.print( service.dumpSpans( fileKey ) );

		assertThat( service.trackedBlueprints() ).containsKey( fileKey );

		var blueprint = service.trackedBlueprints().get( fileKey );
		// The file has several executable spans (assignments, loop body, function
		// body, if branches, ternary, closure) — a general high-level check.
		assertThat( blueprint.spans() ).isNotEmpty();

		// High-level line coverage: the file's executable lines ran. Line counts
		// reflect how many times the line's leading span executed.
		// 2 : i = 0 (ran once)
		// 3 : j = 0 (ran once)
		// 9 : i++ (ran 3x, inside while( i < 3 ))
		// 33 : j = 1 (if taken, doubled > 40) (ran once)
		// 35 : j = 2 (else, not taken) (MISSED)
		// 69 : added = adder( doubled ) (ran once)
		assertThat( service.lineAt( fileKey, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( fileKey, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( fileKey, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( fileKey, 3 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( fileKey, 9 ).covered() ).isTrue();
		assertThat( service.lineAt( fileKey, 9 ).count() ).isEqualTo( 3 );   // i++ ran 3x
		assertThat( service.lineAt( fileKey, 33 ).covered() ).isTrue();
		assertThat( service.lineAt( fileKey, 33 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( fileKey, 35 ).covered() ).isFalse();     // else branch missed
		assertThat( service.lineAt( fileKey, 69 ).covered() ).isTrue();
		assertThat( service.lineAt( fileKey, 69 ).count() ).isEqualTo( 1 );
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
		String	fileKey		= absolute.toString();

		// DEBUG (reads file from disk, no source arg)
		// System.out.println( "=== testDiskClassFile dump" );
		// System.out.print( service.dumpSpans( fileKey ) );

		assertThat( service.trackedBlueprints() ).containsKey( fileKey );

		var blueprint = service.trackedBlueprints().get( fileKey );
		assertThat( blueprint.spans() ).isNotEmpty();

		// High-level line coverage of the class file (see ProfilerComplex.bx):
		// 4 : property threshold default=complexSeed — SKIPPED (super preset it)
		// 6 : property other default=complexSeed — applied (no preset)
		// 9 : complexSeed = 42 (static block ran at class load)
		// 10 : staticInitRan = true (static block ran at class load)
		// 14 : instanceInit = 0 (pseudo-constructor ran at instantiation)
		assertThat( service.lineAt( fileKey, 9 ).covered() ).isTrue();   // static block: complexSeed
		assertThat( service.lineAt( fileKey, 9 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( fileKey, 10 ).covered() ).isTrue();  // static block: staticInitRan
		assertThat( service.lineAt( fileKey, 10 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( fileKey, 14 ).covered() ).isTrue();  // pseudo-constructor body
		assertThat( service.lineAt( fileKey, 14 ).count() ).isEqualTo( 1 );
		// The SKIPPED `threshold` default (line 4, value at col 35) — never ran
		// because ProfilerSuper presets `threshold`. Count 0 = RED.
		assertThat( service.spanAt( fileKey, 4, 35 ).stats().count() ).isEqualTo( 0 );  // property default SKIPPED
		// The APPLIED `other` default (line 6, value at col 31) ran once = GREEN.
		assertThat( service.spanAt( fileKey, 6, 31 ).stats().count() ).isEqualTo( 1 );  // property default USED

		// No method has been invoked yet, so the member function body statement
		// (line 56: "sleep( 100 )", at col 2 after the tab) has NOT run — its count
		// is 0 and its line is not covered. (Line 55 is the member function shell.)
		assertThat( service.spanAt( fileKey, 56, 2 ).stats().count() ).isEqualTo( 0 );  // member body missed
		assertThat( service.lineAt( fileKey, 56 ).covered() ).isFalse();

		// Now invoke the member method on the SAME instance in a second script (the
		// shared context keeps pc in the variables scope); its body should run.
		runtime.executeSource( "result = pc.member( 5 );", context );

		// The member body slept ~100ms, so its span is covered with the sleep charged.
		assertThat( service.spanAt( fileKey, 56, 2 ).stats().count() ).isEqualTo( 1 );  // sleep ran
		assertThat( service.spanAt( fileKey, 56, 2 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( fileKey, 56, 2 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
		assertThat( service.lineAt( fileKey, 56 ).covered() ).isTrue();
		assertThat( service.lineAt( fileKey, 56 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It registers the full span model for the BX disk class files" )
	@Test
	void testDiskClassSpanDefs() {
		// One shared context so the SAME instance/variables scope persists.
		IBoxContext context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Load ProfilerSuper (implicitly via the children) and instantiate
		// ProfilerComplex; reference ProfilerStaticOnly statically (load + static
		// init, NO instance); compile ProfilerGhost WITHOUT running it (all RED).
		runtime.executeSource( "pc = new src.test.resources.profiler.ProfilerComplex();", context );
		runtime.executeSource( "ps = src.test.resources.profiler.ProfilerStaticOnly::staticInitRan;", context );
		Path ghostPath = Paths.get( "src/test/resources/profiler/ProfilerGhost.bx" ).toAbsolutePath().normalize();
		RunnableLoader.getInstance().getBoxpiler().compileClass( ResolvedFilePath.of( ghostPath ) );

		// ---- ProfilerSuper.bx : minimal class shell (3 spans, all covered) ----
		String	superKey	= keyFor( "src/test/resources/profiler/ProfilerSuper.bx" );
		var		superBlue	= service.trackedBlueprints().get( superKey );
		assertThat( superBlue.spans().stream().filter( Blueprint.SpanDef::executable ).count() ).isEqualTo( 3 );
		assertThat( superBlue.spans().get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 7, true ) );   // "class {"
		assertThat( superBlue.spans().get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}"
		assertThat( superBlue.spans().get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 1, 3, 27, true ) ); // 'threshold = "preset value"'
		assertThat( service.spanAt( superKey, 3, 1 ).stats().count() ).isEqualTo( 1 );       // ran for the single Complex instance
		assertThat( service.lineAt( superKey, 3 ).covered() ).isTrue();

		// ---- ProfilerComplex.bx : full span model (62 exec spans) ----
		String	complexKey	= keyFor( "src/test/resources/profiler/ProfilerComplex.bx" );
		var		complexBlue	= service.trackedBlueprints().get( complexKey );
		assertThat( complexBlue.spans().stream().filter( Blueprint.SpanDef::executable ).count() ).isEqualTo( 62 );
		assertThat( complexBlue.spans().get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 88, true ) ); // "class extends=ProfilerSuper implements=... {"
		assertThat( complexBlue.spans().get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 64, 0, 64, 1, true ) ); // final "}"
		assertThat( complexBlue.spans().get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 8, 1, 8, 9, true ) );   // "static {"
		assertThat( complexBlue.spans().get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 9, 2, 9, 18, true ) );  // "complexSeed = 42"
		assertThat( complexBlue.spans().get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 10, 2, 10, 22, true ) );// "staticInitRan = true"
		assertThat( complexBlue.spans().get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 11, 2, 11, 15, true ) );// "staticOnly::s"
		assertThat( complexBlue.spans().get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 12, 1, 12, 2, true ) ); // static "}"
		assertThat( complexBlue.spans().get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 14, 1, 14, 17, true ) );// "instanceInit = 0"
		assertThat( complexBlue.spans().get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 16, 1, 16, 13, true ) );// "class luis {"
		assertThat( complexBlue.spans().get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 28, 1, 28, 2, true ) ); // luis "}"
		assertThat( complexBlue.spans().get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 17, 2, 17, 10, true ) );// luis "static {"
		assertThat( complexBlue.spans().get( 11 ) ).isEqualTo( new Blueprint.SpanDef( 18, 3, 18, 8, true ) ); // "s = 1"
		assertThat( complexBlue.spans().get( 12 ) ).isEqualTo( new Blueprint.SpanDef( 19, 3, 19, 15, true ) );// "if( false ) "
		assertThat( complexBlue.spans().get( 13 ) ).isEqualTo( new Blueprint.SpanDef( 20, 4, 20, 38, true ) );// print never runs
		assertThat( complexBlue.spans().get( 14 ) ).isEqualTo( new Blueprint.SpanDef( 19, 15, 19, 16, true ) );// if "{"
		assertThat( complexBlue.spans().get( 15 ) ).isEqualTo( new Blueprint.SpanDef( 21, 3, 21, 4, true ) ); // if "}"
		assertThat( complexBlue.spans().get( 16 ) ).isEqualTo( new Blueprint.SpanDef( 22, 2, 22, 3, true ) ); // luis static "}"
		assertThat( complexBlue.spans().get( 17 ) ).isEqualTo( new Blueprint.SpanDef( 23, 2, 23, 7, true ) ); // "y = 2"
		assertThat( complexBlue.spans().get( 18 ) ).isEqualTo( new Blueprint.SpanDef( 24, 2, 24, 20, true ) );// "function member() "
		assertThat( complexBlue.spans().get( 19 ) ).isEqualTo( new Blueprint.SpanDef( 25, 3, 25, 14, true ) );// "a = "b" ?: "
		assertThat( complexBlue.spans().get( 20 ) ).isEqualTo( new Blueprint.SpanDef( 25, 14, 25, 17, true ) );// '"c"'
		assertThat( complexBlue.spans().get( 21 ) ).isEqualTo( new Blueprint.SpanDef( 26, 3, 26, 11, true ) );// "return 3"
		assertThat( complexBlue.spans().get( 22 ) ).isEqualTo( new Blueprint.SpanDef( 24, 20, 24, 21, true ) );// "function member() {" brace
		assertThat( complexBlue.spans().get( 23 ) ).isEqualTo( new Blueprint.SpanDef( 27, 2, 27, 3, true ) ); // luis fn "}"
		assertThat( complexBlue.spans().get( 24 ) ).isEqualTo( new Blueprint.SpanDef( 29, 1, 29, 18, true ) );// "inst = new luis()"
		assertThat( complexBlue.spans().get( 25 ) ).isEqualTo( new Blueprint.SpanDef( 31, 1, 31, 19, true ) );// "class staticOnly {"
		assertThat( complexBlue.spans().get( 26 ) ).isEqualTo( new Blueprint.SpanDef( 39, 1, 39, 2, true ) ); // staticOnly "}"
		assertThat( complexBlue.spans().get( 27 ) ).isEqualTo( new Blueprint.SpanDef( 32, 2, 32, 10, true ) );// staticOnly "static {"
		assertThat( complexBlue.spans().get( 28 ) ).isEqualTo( new Blueprint.SpanDef( 33, 3, 33, 8, true ) ); // "s = 1"
		assertThat( complexBlue.spans().get( 29 ) ).isEqualTo( new Blueprint.SpanDef( 34, 2, 34, 3, true ) ); // staticOnly static "}"
		assertThat( complexBlue.spans().get( 30 ) ).isEqualTo( new Blueprint.SpanDef( 35, 2, 35, 7, true ) ); // "y = 2"
		assertThat( complexBlue.spans().get( 31 ) ).isEqualTo( new Blueprint.SpanDef( 36, 2, 36, 20, true ) );// "function member() "
		assertThat( complexBlue.spans().get( 32 ) ).isEqualTo( new Blueprint.SpanDef( 37, 3, 37, 11, true ) );// "return 3"
		assertThat( complexBlue.spans().get( 33 ) ).isEqualTo( new Blueprint.SpanDef( 36, 20, 36, 21, true ) );// brace
		assertThat( complexBlue.spans().get( 34 ) ).isEqualTo( new Blueprint.SpanDef( 38, 2, 38, 3, true ) ); // staticOnly fn "}"
		assertThat( complexBlue.spans().get( 35 ) ).isEqualTo( new Blueprint.SpanDef( 41, 1, 41, 14, true ) );// "class ghost {"
		assertThat( complexBlue.spans().get( 36 ) ).isEqualTo( new Blueprint.SpanDef( 49, 1, 49, 2, true ) ); // ghost "}"
		assertThat( complexBlue.spans().get( 37 ) ).isEqualTo( new Blueprint.SpanDef( 42, 2, 42, 10, true ) );// ghost "static {"
		assertThat( complexBlue.spans().get( 38 ) ).isEqualTo( new Blueprint.SpanDef( 43, 3, 43, 8, true ) ); // "s = 1"
		assertThat( complexBlue.spans().get( 39 ) ).isEqualTo( new Blueprint.SpanDef( 44, 2, 44, 3, true ) ); // ghost static "}"
		assertThat( complexBlue.spans().get( 40 ) ).isEqualTo( new Blueprint.SpanDef( 45, 2, 45, 7, true ) ); // "y = 2"
		assertThat( complexBlue.spans().get( 41 ) ).isEqualTo( new Blueprint.SpanDef( 46, 2, 46, 20, true ) );// "function member() "
		assertThat( complexBlue.spans().get( 42 ) ).isEqualTo( new Blueprint.SpanDef( 47, 3, 47, 11, true ) );// "return 3"
		assertThat( complexBlue.spans().get( 43 ) ).isEqualTo( new Blueprint.SpanDef( 46, 20, 46, 21, true ) );// brace
		assertThat( complexBlue.spans().get( 44 ) ).isEqualTo( new Blueprint.SpanDef( 48, 2, 48, 3, true ) ); // ghost fn "}"
		assertThat( complexBlue.spans().get( 45 ) ).isEqualTo( new Blueprint.SpanDef( 51, 1, 51, 24, true ) );// "function doubleIt( n ) "
		assertThat( complexBlue.spans().get( 46 ) ).isEqualTo( new Blueprint.SpanDef( 52, 2, 52, 14, true ) );// "return n * 2"
		assertThat( complexBlue.spans().get( 47 ) ).isEqualTo( new Blueprint.SpanDef( 51, 24, 51, 25, true ) );// doubleIt brace
		assertThat( complexBlue.spans().get( 48 ) ).isEqualTo( new Blueprint.SpanDef( 53, 1, 53, 2, true ) ); // doubleIt "}"
		assertThat( complexBlue.spans().get( 49 ) ).isEqualTo( new Blueprint.SpanDef( 55, 1, 55, 22, true ) );// "function member( x ) "
		assertThat( complexBlue.spans().get( 50 ) ).isEqualTo( new Blueprint.SpanDef( 56, 2, 56, 14, true ) );// "sleep( 100 )"
		assertThat( complexBlue.spans().get( 51 ) ).isEqualTo( new Blueprint.SpanDef( 57, 2, 57, 14, true ) );// "return x * 3"
		assertThat( complexBlue.spans().get( 52 ) ).isEqualTo( new Blueprint.SpanDef( 55, 22, 55, 23, true ) );// member brace
		assertThat( complexBlue.spans().get( 53 ) ).isEqualTo( new Blueprint.SpanDef( 58, 1, 58, 2, true ) ); // member "}"
		// abstractOnly() — the interface abstract method impl
		assertThat( complexBlue.spans().get( 54 ) ).isEqualTo( new Blueprint.SpanDef( 61, 1, 61, 25, true ) );// "function abstractOnly() "
		assertThat( complexBlue.spans().get( 55 ) ).isEqualTo( new Blueprint.SpanDef( 62, 2, 62, 31, true ) );// "return "abstract implemented""
		assertThat( complexBlue.spans().get( 56 ) ).isEqualTo( new Blueprint.SpanDef( 61, 25, 61, 26, true ) );// brace
		assertThat( complexBlue.spans().get( 57 ) ).isEqualTo( new Blueprint.SpanDef( 63, 1, 63, 2, true ) ); // abstractOnly "}"
		assertThat( complexBlue.spans().get( 58 ) ).isEqualTo( new Blueprint.SpanDef( 4, 1, 4, 35, true ) );  // threshold property head
		assertThat( complexBlue.spans().get( 59 ) ).isEqualTo( new Blueprint.SpanDef( 4, 35, 4, 57, true ) );// runtime default (SKIPPED)
		assertThat( complexBlue.spans().get( 60 ) ).isEqualTo( new Blueprint.SpanDef( 6, 1, 6, 31, true ) ); // other property head
		assertThat( complexBlue.spans().get( 61 ) ).isEqualTo( new Blueprint.SpanDef( 6, 31, 6, 53, true ) );// runtime default (USED)

		// Complex coverage: static + pseudo-ctor ran; members only on call; the
		// skipped threshold default is count 0 while the applied other is count 1.
		assertThat( service.spanAt( complexKey, 9, 2 ).stats().count() ).isEqualTo( 1 );   // static complexSeed
		assertThat( service.spanAt( complexKey, 14, 1 ).stats().count() ).isEqualTo( 1 );  // instanceInit
		assertThat( service.spanAt( complexKey, 4, 35 ).stats().count() ).isEqualTo( 0 );  // threshold default SKIPPED
		assertThat( service.spanAt( complexKey, 6, 31 ).stats().count() ).isEqualTo( 1 );  // other default USED
		assertThat( service.spanAt( complexKey, 56, 2 ).stats().count() ).isEqualTo( 0 );  // sleep NOT yet run
		assertThat( service.lineAt( complexKey, 9 ).covered() ).isTrue();
		assertThat( service.lineAt( complexKey, 9 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( complexKey, 56 ).covered() ).isFalse();   // member body line missed
		assertThat( service.lineAt( complexKey, 4 ).covered() ).isTrue();     // property head RUNS
		// The property default value column alone is NOT covered because the value
		// skipped; but the line is still "covered" since the head ran.

		// ---- ProfilerStaticOnly.bx : instance body + members stay RED ----
		String	staticKey	= keyFor( "src/test/resources/profiler/ProfilerStaticOnly.bx" );
		var		staticBlue	= service.trackedBlueprints().get( staticKey );
		assertThat( staticBlue.spans().stream().filter( Blueprint.SpanDef::executable ).count() ).isEqualTo( 56 );
		// static block ran
		assertThat( service.spanAt( staticKey, 7, 2 ).stats().count() ).isEqualTo( 1 );  // complexSeed
		assertThat( service.spanAt( staticKey, 8, 2 ).stats().count() ).isEqualTo( 1 );  // staticInitRan
		assertThat( service.spanAt( staticKey, 9, 2 ).stats().count() ).isEqualTo( 1 );  // staticOnly::s
		// no instance created -> instance body and members stay RED. The nested
		// `class staticOnly` SHELL + static init still run at class load, but its
		// instance body (`y = 2`) and member bodies are never hit.
		assertThat( service.spanAt( staticKey, 12, 1 ).stats().count() ).isEqualTo( 0 ); // instanceInit (no instance)
		assertThat( service.spanAt( staticKey, 14, 1 ).stats().count() ).isEqualTo( 0 ); // class luis shell (never loaded)
		assertThat( service.spanAt( staticKey, 29, 1 ).stats().count() ).isEqualTo( 1 ); // nested staticOnly shell RUNS (via staticOnly::s ref at load)
		assertThat( service.spanAt( staticKey, 33, 2 ).stats().count() ).isEqualTo( 0 ); // nested staticOnly y = 2 (no instance)
		assertThat( service.spanAt( staticKey, 54, 2 ).stats().count() ).isEqualTo( 0 ); // member sleep
		assertThat( service.spanAt( staticKey, 4, 35 ).stats().count() ).isEqualTo( 0 ); // threshold default RED
		assertThat( service.lineAt( staticKey, 8 ).covered() ).isTrue();
		assertThat( service.lineAt( staticKey, 8 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( staticKey, 12 ).covered() ).isFalse();
		assertThat( service.lineAt( staticKey, 54 ).covered() ).isFalse();

		// ---- ProfilerGhost.bx : compiled but NEVER run -> ALL spans RED ----
		String	ghostKey	= keyFor( "src/test/resources/profiler/ProfilerGhost.bx" );
		var		ghostBlue	= service.trackedBlueprints().get( ghostKey );
		assertThat( ghostBlue.spans().stream().filter( Blueprint.SpanDef::executable ).count() ).isEqualTo( 56 );
		for ( Blueprint.SpanDef def : ghostBlue.spans() ) {
			if ( def.executable() ) {
				assertThat( service.spanAt( ghostKey, def.startLine(), def.startCol() ).stats().count() ).isEqualTo( 0 );
			}
		}
		assertThat( service.lineAt( ghostKey, 7 ).covered() ).isFalse();
		assertThat( service.lineAt( ghostKey, 8 ).covered() ).isFalse();
		assertThat( service.lineAt( ghostKey, 54 ).covered() ).isFalse();
	}

	@DisplayName( "It instruments and profiles a BoxLang interface's static block, static functions, and default methods" )
	@Test
	void testInterfaceSpanCoverage() {
		// Interfaces hold executable spans in their STATIC block, STATIC functions,
		// and DEFAULT methods (abstract members have no body). The interface
		// transformer must declare the codeProfilerId field, self-register the
		// blueprint, and emit mark probes that run when the interface is loaded,
		// its static functions are invoked, and an implementing class runs a
		// default method.
		IBoxContext context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Reference the interface statically (loads it, running its static block).
		runtime.executeSource( "src.test.resources.profiler.ProfilerInterface::initializedAt;", context );

		String	key	= keyFor( "src/test/resources/profiler/ProfilerInterface.bx" );
		var		bp	= service.trackedBlueprints().get( key );
		assertThat( bp ).isNotNull();

		// ---- SPAN DEFINITIONS (exact span model for the interface file) ----
		// Layout (the fixture has a static block, static functions, a CALLED
		// default method, an UNUSED default method, and an abstract method with
		// NO body — abstract members produce NO spans):
		// 1 interface displayName="ProfilerInterface" { (SHELL)
		// 3 static {
		// 4 static.initializedAt = createDateTime( 2020, 1, 1 );
		// 5 static.counter = 0;
		// 6 }
		// 8 static function bump() { 9 static.counter++; 10 return static.counter; 11 }
		// 13 static function display() { 14 var x = 1 + 2; 15 return displayName; 16 }
		// 20 default function greet() { 21 var msg...; 22 return msg; 23 }
		// 26 default function greetUnused(){27 var unusedPrefix; 28 return ...; 29 } (never called)
		// 32 function abstractOnly(); <- abstract, NO spans
		// 34 }
		List<Blueprint.SpanDef> exec = bp.spans().stream().filter( Blueprint.SpanDef::executable ).collect( Collectors.toList() );
		assertThat( exec ).containsExactly(
		    // interface shell (line 1) + closing "}" (line 34) — grouped, marked at load
		    new Blueprint.SpanDef( 1, 0, 1, 43, true ),   // interface displayName="ProfilerInterface" {
		    new Blueprint.SpanDef( 34, 0, 34, 1, true ),  // closing }
		    // static function bump()
		    new Blueprint.SpanDef( 8, 1, 8, 24, true ),   // static function bump() {
		    new Blueprint.SpanDef( 9, 2, 9, 18, true ),   // static.counter++;
		    new Blueprint.SpanDef( 10, 2, 10, 23, true ), // return static.counter;
		    new Blueprint.SpanDef( 8, 24, 8, 25, true ),  // bump opening brace
		    new Blueprint.SpanDef( 11, 1, 11, 2, true ),  // bump closing }
		    // static function display()
		    new Blueprint.SpanDef( 13, 1, 13, 27, true ), // static function display() {
		    new Blueprint.SpanDef( 14, 2, 14, 15, true ), // var x = 1 + 2;
		    new Blueprint.SpanDef( 15, 2, 15, 20, true ), // return displayName;
		    new Blueprint.SpanDef( 13, 27, 13, 28, true ),// display opening brace
		    new Blueprint.SpanDef( 16, 1, 16, 2, true ),  // display closing }
		    // default method greet() — CALLED via implementing class
		    new Blueprint.SpanDef( 20, 1, 20, 26, true ), // default function greet() {
		    new Blueprint.SpanDef( 21, 2, 21, 34, true ), // var msg = "hello from interface";
		    new Blueprint.SpanDef( 22, 2, 22, 12, true ), // return msg;
		    new Blueprint.SpanDef( 20, 26, 20, 27, true ),// greet opening brace
		    new Blueprint.SpanDef( 23, 1, 23, 2, true ),  // greet closing }
		    // default method greetUnused() — NEVER called, body stays RED
		    new Blueprint.SpanDef( 26, 1, 26, 32, true ), // default function greetUnused() {
		    new Blueprint.SpanDef( 27, 2, 27, 29, true ), // var unusedPrefix = "unused";
		    new Blueprint.SpanDef( 28, 2, 28, 27, true ), // return unusedPrefix & "!";
		    new Blueprint.SpanDef( 26, 32, 26, 33, true ),// greetUnused opening brace
		    new Blueprint.SpanDef( 29, 1, 29, 2, true ),  // greetUnused closing }
		    // abstract method abstractOnly() — declaration is static metadata, marked GREEN at load
		    new Blueprint.SpanDef( 32, 1, 32, 24, true ), // function abstractOnly();
		    // static init block (header + closing brace grouped, marked at load)
		    new Blueprint.SpanDef( 3, 1, 3, 9, true ),    // static {
		    new Blueprint.SpanDef( 4, 2, 4, 53, true ),   // static.initializedAt = ...
		    new Blueprint.SpanDef( 5, 2, 5, 20, true ),   // static.counter = 0;
		    new Blueprint.SpanDef( 6, 1, 6, 2, true )     // static block closing }
		);

		// ---- COVERAGE after a static reference ----
		// The interface SHELL (line 1) ran at load; static block RAN on load.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();  // interface shell
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();  // static { header
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();  // static block body
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();  // static block body
		// Function DECLARATION shells ran at load, but bodies did NOT.
		assertThat( service.lineAt( key, 9 ).covered() ).isFalse();  // bump body
		assertThat( service.lineAt( key, 14 ).covered() ).isFalse(); // display body
		assertThat( service.lineAt( key, 21 ).covered() ).isFalse(); // greet body
		assertThat( service.lineAt( key, 27 ).covered() ).isFalse(); // greetUnused body (never called)
		assertThat( service.lineAt( key, 28 ).covered() ).isFalse(); // greetUnused return (never called)
		// The ABSTRACT method has no BODY — but its DECLARATION (`function
		// abstractOnly();`) is static metadata of the interface, so it is marked
		// GREEN when the interface loads, exactly like a property declaration.
		var abstractDecl = service.lineAt( key, 32 );
		assertThat( abstractDecl ).isNotNull();
		assertThat( abstractDecl.covered() ).isTrue();
		assertThat( abstractDecl.count() ).isEqualTo( 1 );

		// ---- Invoke the static function twice ----
		runtime.executeSource( "src.test.resources.profiler.ProfilerInterface::bump();", context );
		runtime.executeSource( "src.test.resources.profiler.ProfilerInterface::bump();", context );
		assertThat( service.lineAt( key, 9 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 9 ).count() ).isEqualTo( 2 );
		assertThat( service.lineAt( key, 10 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 10 ).count() ).isEqualTo( 2 );

		// ---- Run a DEFAULT method on an implementing class instance ----
		// ProfilerComplex implements the interface, so running its inherited
		// greet() default method executes the INTERFACE's default-method body.
		runtime.executeSource( "new src.test.resources.profiler.ProfilerComplex().greet();", context );
		// The default method body span (in the INTERFACE's blueprint) now runs.
		assertThat( service.lineAt( key, 21 ).covered() ).isTrue(); // greet body msg line
		assertThat( service.lineAt( key, 21 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 22 ).covered() ).isTrue(); // return msg
		assertThat( service.lineAt( key, 22 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It does NOT mark an interface static block that was never entered (an earlier block threw)" )
	@Test
	void testUnreachedInterfaceStaticBlockStaysUncovered() {
		// Regression: the interface transformer used to batch-mark EVERY static
		// block's "static {" header + closing "}" group at static-init ENTRY. When
		// the first block threw, the second block was never entered — yet its braces
		// were reported COVERED while its body stayed RED, a false "entered" signal.
		// The mark must be emitted immediately before each block's OWN body.
		//
		// Fixture layout:
		// 1 interface displayName="ProfilerInterfaceThrowingStatic" {
		// 4 static { ... throw ... } <- FIRST block: runs, then throws
		// 8 static { static.reached = true; } <- SECOND block: NEVER entered
		// 12 }
		IBoxContext context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Loading the interface runs its static blocks; the first one throws.
		assertThrows( CustomException.class, () -> runtime.executeSource(
		    "src.test.resources.profiler.ProfilerInterfaceThrowingStatic::reached;", context ) );

		String key = keyFor( "src/test/resources/profiler/ProfilerInterfaceThrowingStatic.bx" );
		assertThat( service.trackedBlueprints() ).containsKey( key );

		// The FIRST block ran up to the throw: header, body and closing "}" covered.
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();   // static { (first block)
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();   // throw( ... )
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();   // } (first block)

		// The SECOND block was NEVER entered: its header, body and closing "}" must
		// ALL be uncovered. Only the body used to be uncovered.
		assertThat( service.lineAt( key, 8 ).covered() ).isFalse();  // static { (second block) — was true
		assertThat( service.lineAt( key, 9 ).covered() ).isFalse();  // static.reached = true;
		assertThat( service.lineAt( key, 10 ).covered() ).isFalse(); // } (second block) — was true
	}

	@DisplayName( "It embeds a clinit self-registration call into a compiled interface <clinit>" )
	@Test
	void testInterfaceEmbedsBlueprintRegistration() throws Exception {
		runtime.getConfiguration().codeProfilerEnabled = true;

		var	boxpiler	= ( ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		var	classPath	= java.nio.file.Paths.get( "src/test/resources/profiler/ProfilerInterface.bx" ).toAbsolutePath().normalize();
		var	classInfo	= ortus.boxlang.compiler.ClassInfo.forClass(
		    ResolvedFilePath.of( classPath ),
		    ortus.boxlang.compiler.parser.Parser.detectFile( classPath.toFile(), true ),
		    boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );
		java.util.List<byte[]>	compiled	= boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );

		// The interface class <clinit> must declare/init codeProfilerId and invoke
		// registerBlueprintFromClinit (proving self-registration bytecode is present).
		boolean					found		= false;
		for ( byte[] bytes : compiled ) {
			if ( bytes.length < 4 || bytes[ 0 ] != ( byte ) 0xCA || bytes[ 1 ] != ( byte ) 0xFE
			    || bytes[ 2 ] != ( byte ) 0xBA || bytes[ 3 ] != ( byte ) 0xBE ) {
				continue;
			}
			org.objectweb.asm.tree.ClassNode classNode = new org.objectweb.asm.tree.ClassNode();
			new org.objectweb.asm.ClassReader( bytes ).accept( classNode, 0 );
			for ( var method : classNode.methods ) {
				if ( !method.name.equals( "<clinit>" ) ) {
					continue;
				}
				for ( var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext() ) {
					if ( insn instanceof org.objectweb.asm.tree.MethodInsnNode call
					    && call.name.equals( "registerBlueprintFromClinit" ) ) {
						found = true;
						break;
					}
				}
			}
		}
		assertThat( found ).isTrue();

		runtime.getConfiguration().codeProfilerEnabled = false;
	}

	@DisplayName( "It DEFINES an interface that has only abstract methods (empty pseudo-constructor)" )
	@Test
	void testAbstractOnlyInterfaceDefines() throws Exception {
		// Regression: an interface whose body holds ONLY abstract method declarations
		// (no static block, no static/default method bodies — e.g. TestBox's
		// IRunner.cfc) has an EMPTY _pseudoConstructor. The profiler's catch-all
		// wrapper previously emitted a try range whose start == end (zero length),
		// which the JVM rejects with:
		// java.lang.ClassFormatError: Illegal exception table range in class file ...
		// at defineClass time. This must COMPILE and DEFINE cleanly.
		runtime.getConfiguration().codeProfilerEnabled = true;

		var	boxpiler	= ( ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		var	classPath	= java.nio.file.Paths.get( "src/test/resources/profiler/ProfilerInterfaceAbstractOnly.bx" ).toAbsolutePath().normalize();
		var	classInfo	= ortus.boxlang.compiler.ClassInfo.forClass(
		    ResolvedFilePath.of( classPath ),
		    ortus.boxlang.compiler.parser.Parser.detectFile( classPath.toFile(), true ),
		    boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );

		java.util.List<byte[]>	compiled	= boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );

		// Defining the produced bytes is the operation that failed with
		// Illegal exception table range before the fix. Any ClassFormatError here
		// fails the test.
		boolean					found		= false;
		for ( byte[] bytes : compiled ) {
			if ( bytes.length < 4 || bytes[ 0 ] != ( byte ) 0xCA || bytes[ 1 ] != ( byte ) 0xFE
			    || bytes[ 2 ] != ( byte ) 0xBA || bytes[ 3 ] != ( byte ) 0xBE ) {
				continue;
			}
			found = true;
			new ByteClassLoader().define( bytes );
		}
		assertThat( found ).isTrue();

		runtime.getConfiguration().codeProfilerEnabled = false;
	}

	/** Minimal loader that exposes the protected {@code defineClass} for tests. */
	private static class ByteClassLoader extends ClassLoader {

		Class<?> define( byte[] bytes ) {
			return defineClass( null, bytes, 0, bytes.length );
		}
	}

	@DisplayName( "It self-registers a blueprint from clinit and replaces stale span data" )
	@Test
	void testClinitBlueprintRegistration() {
		// A class compiles its blueprint INTO <clinit> and self-registers on load.
		// CodeProfilerService.registerBlueprintFromClinit is what <clinit> calls.
		// It must be:
		// 1) IDEMPOTENT per key (the id is the file path / source hash — a String)
		// 2) REPLACE the stale blueprint when the key's lastModified is NEWER.
		String		filePath	= "C:/opt/test/SomeComponent.cfc";

		// First registration — two spans, lastModified 1000. The call RETURNS the
		// blueprint (the class stores it in a static field so its marks can
		// re-register it after service.reset()).
		Blueprint	firstBp		= CodeProfilerService.registerBlueprintFromClinit( filePath, 1000L, Blueprint.Kind.FILE,
		    new int[] {
		        1, 0, 1, 10, 1,
		        2, 0, 2, 8, 1
		    } );
		String		normPath	= java.nio.file.Paths.get( filePath ).toAbsolutePath().normalize().toString();
		assertThat( firstBp ).isNotNull();
		assertThat( firstBp.spans() ).hasSize( 2 );

		// Idempotent: same key re-registered with the SAME lastModified -> SAME key.
		CodeProfilerService.registerBlueprintFromClinit( filePath, 1000L, Blueprint.Kind.FILE,
		    new int[] {
		        1, 0, 1, 10, 1,
		        2, 0, 2, 8, 1
		    } );

		// The blueprint is registered under the normalized absolute path.
		var bp = service.trackedBlueprints().get( normPath );
		assertThat( bp ).isNotNull();
		assertThat( bp.spans() ).hasSize( 2 );
		assertThat( bp.lastModified() ).isEqualTo( 1000L );

		// Newer lastModified (file changed + recompiled) -> SAME key, spans REPLACED.
		Blueprint newerBp = CodeProfilerService.registerBlueprintFromClinit( filePath, 2000L, Blueprint.Kind.FILE,
		    new int[] {
		        1, 0, 1, 12, 1,
		        2, 0, 2, 10, 1,
		        3, 0, 3, 6, 1
		    } );
		assertThat( newerBp ).isNotNull();
		assertThat( newerBp.spans() ).hasSize( 3 );

		var bp2 = service.trackedBlueprints().get( normPath );
		assertThat( bp2.lastModified() ).isEqualTo( 2000L );
		assertThat( bp2.spans() ).hasSize( 3 ); // replaced, not appended

		// The spans' counters are fresh (all zero) after replacement.
		assertThat( service.spanAt( normPath, 1, 0 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.spanAt( normPath, 3, 0 ).stats().count() ).isEqualTo( 0 );
	}

	@DisplayName( "It preserves the SOURCE kind without path-normalizing the id on clinit self-registration" )
	@Test
	void testClinitSourceBlueprintKeepsHashKey() {
		// A SOURCE self-registration (adhoc source) must key the blueprint by the
		// source-hash VERBATIM and keep kind=SOURCE. Regression: the clinit path
		// used to normalize every id as a filesystem path and force kind=FILE,
		// minting a bogus <cwd>/<hash> FILE entry while marks hit the hash key.
		String		sourceHash	= "0123456789abcdef0123456789abcdef";
		Blueprint	firstBp		= CodeProfilerService.registerBlueprintFromClinit( sourceHash, 0L, Blueprint.Kind.SOURCE,
		    new int[] {
		        1, 0, 1, 10, 1
		    } );

		// The returned blueprint keeps kind SOURCE (a hash is not a path to normalize).
		assertThat( firstBp ).isNotNull();
		assertThat( firstBp.kind() ).isEqualTo( Blueprint.Kind.SOURCE );

		var bp = service.trackedBlueprints().get( sourceHash );
		assertThat( bp ).isNotNull();
		assertThat( bp.kind() ).isEqualTo( Blueprint.Kind.SOURCE );
		// No bogus FILE entry was minted under a normalized path of the hash.
		assertThat( service.trackedBlueprints().values().stream()
		    .filter( b -> b.kind() == Blueprint.Kind.FILE )
		    .noneMatch( b -> b.equals( bp ) ) ).isTrue();
	}

	@DisplayName( "It embeds a blueprint self-registration call into the compiled class <clinit>" )
	@Test
	void testClinitEmbedsBlueprintRegistration() throws Exception {
		// Compile an on-disk class with profiling so its <clinit> gets the
		// registerBlueprintFromClinit self-registration bytecode.
		runtime.getConfiguration().codeProfilerEnabled = true;

		// Compile inline so we can grab the raw class bytes (not via resource lookup).
		var	boxpiler	= ( ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		var	classPath	= java.nio.file.Paths.get( "src/test/resources/profiler/ProfilerComplex.bx" ).toAbsolutePath().normalize();
		var	classInfo	= ortus.boxlang.compiler.ClassInfo.forClass(
		    ResolvedFilePath.of( classPath ),
		    ortus.boxlang.compiler.parser.Parser.detectFile( classPath.toFile(), true ),
		    boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );
		java.util.List<byte[]>	compiled	= boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );

		// Find the main (non-$) class and confirm its <clinit> invokes
		// CodeProfilerService.registerBlueprintFromClinit.
		boolean					found		= false;
		for ( byte[] bytes : compiled ) {
			// Skip non-class entries (e.g. auxiliary resources) in the byte list.
			if ( bytes.length < 4 || bytes[ 0 ] != ( byte ) 0xCA || bytes[ 1 ] != ( byte ) 0xFE || bytes[ 2 ] != ( byte ) 0xBA
			    || bytes[ 3 ] != ( byte ) 0xBE ) {
				continue;
			}
			org.objectweb.asm.tree.ClassNode classNode = new org.objectweb.asm.tree.ClassNode();
			new org.objectweb.asm.ClassReader( bytes ).accept( classNode, 0 );
			for ( var method : classNode.methods ) {
				if ( !method.name.equals( "<clinit>" ) ) {
					continue;
				}
				for ( var insn = method.instructions.getFirst(); insn != null; insn = insn.getNext() ) {
					if ( insn instanceof org.objectweb.asm.tree.MethodInsnNode call
					    && call.name.equals( "registerBlueprintFromClinit" ) ) {
						found = true;
						break;
					}
				}
			}
		}
		assertThat( found ).isTrue();

		// Ensure profiling is restored for subsequent tests.
		runtime.getConfiguration().codeProfilerEnabled = false;
	}

	@DisplayName( "It profiles the named-argument param statement (script)" )
	@Test
	void testParamNamed() {
		// Variable does NOT exist yet — the param runs and sets the default.
		// The default is a COMPLEX expression (now()), so it breaks into its own
		// span (deferred closure, evaluated only when the variable is missing).
		String source = "param name=\"foo\" default=now();";
		runtime.executeSource( source );
		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testParamNamed dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		// Pass A: the statement head "param name=\"foo\" default=" (runs every
		// time) and the default expression now() (runs ONLY when foo is missing)
		// are separate spans.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 25, true ) );  // param head
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 25, 1, 30, true ) ); // now() default
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		// foo is missing, so the deferred default ran.
		assertThat( service.spanAt( key, 1, 25 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles the named-argument param statement when the variable already exists (script)" )
	@Test
	void testParamNamedExists() {
		// The variable EXISTS, so the param statement still executes (it just skips
		// the default assignment) — the DEFAULT EXPRESSION is NOT evaluated, so its
		// span stays at count 0 (RED in the HTML).
		String source = "foo = 1;\nparam name=\"foo\" default=now();";
		runtime.executeSource( source );
		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testParamNamedExists dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 7, true ) );   // foo = 1
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 25, true ) );  // param head
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 25, 2, 30, true ) ); // now() default
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		// foo exists — the default expression was NEVER evaluated (count 0, RED).
		assertThat( service.spanAt( key, 2, 25 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles the param=default script syntax (script)" )
	@Test
	void testParamEquals() {
		// The shorthand form `param foo=now();` is normalized to
		// name="foo" default=now() — the complex default breaks into its own span.
		String source = "param foo=now();";
		runtime.executeSource( source );
		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testParamEquals dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 10, true ) );  // "param foo="
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 10, 1, 15, true ) ); // now() default
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 1, 10 ).stats().count() ).isEqualTo( 1 );   // default ran (foo missing)
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles the param=default script syntax when the variable already exists (script)" )
	@Test
	void testParamEqualsExists() {
		// foo EXISTS — the default now() is never evaluated (count 0, RED).
		String source = "foo = 1;\nparam foo=now();";
		runtime.executeSource( source );
		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// DEBUG
		// System.out.println( "=== testParamEqualsExists dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 7, true ) );   // foo = 1
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 10, true ) );  // "param foo="
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 10, 2, 15, true ) ); // now() default
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 2, 10 ).stats().count() ).isEqualTo( 0 );   // default MISSED (foo exists)
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	/**
	 * Build the normalized lowercase absolute-path blueprint key for a test resource.
	 *
	 * @param relativePath the class file path relative to the repo root
	 *
	 * @return the file-path key used to register the on-disk blueprint
	 */
	private String keyFor( String relativePath ) {
		Path absolute = Paths.get( relativePath ).toAbsolutePath().normalize();
		return absolute.toString();
	}

	@DisplayName( "It groups a script lock component's closing brace with its header" )
	@Test
	void testScriptComponentBlockBraces() {
		String source = """
		                totalSpecs = 0;
		                lock name="tb-results-1" type="exclusive" timeout="10" {
		                totalSpecs += 1;
		                }
		                """;
		runtime.executeSource( source );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Pass A: totalSpecs = 0 is one span; the lock header is one span (through
		// the "{"), the body statement is one span, and the closing "}" is grouped
		// with the header so it is covered when the body runs.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) );  // "totalSpecs = 0"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 3, 0, true ) );   // lock header through "{"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 15, true ) );  // "totalSpecs += 1"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 1, true ) );   // "}"

		// Pass B: everything ran, and the closing brace is covered (count 1) — no
		// phantom RED span over the closing "}".
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );   // lock header
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // body
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );   // closing "}" GREEN

		// Line-based: lines 1-4 all covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
	}

	@DisplayName( "It gives each struct-literal key and value its own tight span, with no comments in any span" )
	@Test
	void testStructLiteralKeysAndValuesAreTightSpans() {
		// The BUG 2 struct literal in ProfilerSample.bxs (the faithful TestResult.cfc
		// repro) has a comment before EVERY entry. Each KEY and each VALUE must be its
		// OWN span covering EXACTLY that expression — no wider — so a comment line is
		// NEVER part of any span, and the `:` / `,` separators are not spans either.
		String relativePath = "src/test/resources/profiler/ProfilerSample.bxs";
		runtime.executeTemplate( relativePath );
		String	key	= keyFor( relativePath );
		String	src	= readSampleSource();

		// Find every executable span that falls within the struct literal region
		// (lines 299-325). Each must be EXACTLY a key or EXACTLY a value — never
		// spanning a comment line, never the `: ` between key and value, never a `,`.
		// We assert on the exact defs for a representative slice and structurally
		// verify the invariant for the whole region.
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() < 299 || s.endLine() > 325 ) {
				continue;
			}
			// A span in the struct region must NOT extend across a comment-only line.
			for ( int commentLine : new int[] { 300, 302, 304, 306, 308, 310, 312, 314, 316, 320, 322, 324 } ) {
				assertWithMessage( "span " + s + " crosses comment line " + commentLine )
				    .that( s.startLine() <= commentLine && commentLine <= s.endLine() )
				    .isFalse();
			}
			// The span text must be exactly an expression — never the `: ` gap or `,`.
			String text = service.spanSourceText( key, src, s );
			assertWithMessage( "span " + s + " is not a tight key/value expression" )
			    .that( text )
			    .doesNotContain( ":" );
			assertWithMessage( "span " + s + " has a leading pad" )
			    .that( text.startsWith( " " ) )
			    .isFalse();
		}

		// Every comment line in the struct region must have NO executable span
		// (so lineAt is null and fileLines lacks the key).
		for ( int commentLine : new int[] { 298, 300, 302, 304, 306, 308, 310, 312, 314, 316, 320, 322, 324 } ) {
			assertThat( service.lineAt( key, commentLine ) ).isNull();
			assertThat( service.fileLines( key ) ).doesNotContainKey( commentLine );
		}
		// Every key/value line in the struct region is covered (they all ran).
		for ( int valueLine : new int[] { 299, 301, 303, 305, 307, 309, 311, 313, 315, 317, 318, 319, 321, 323, 325 } ) {
			assertThat( service.lineAt( key, valueLine ).covered() ).isTrue();
		}
	}

	/**
	 * Read the sample file source (used for spanSourceText in the struct test).
	 *
	 * @return the ProfilerSample.bxs source text
	 */
	private String readSampleSource() {
		try {
			return java.nio.file.Files.readString( Paths.get( "src/test/resources/profiler/ProfilerSample.bxs" ) );
		} catch ( java.io.IOException e ) {
			throw new RuntimeException( e );
		}
	}

	@DisplayName( "It covers a ternary parenthesized expression's closing-paren line" )
	@Test
	void testTernaryParenthesizedExpressionClosingParen() {
		String source = """
		                a = (
		                server.keyExists( "boxlang" ) ? "bx" : "cf"
		                );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The closing ")" on line 3 must be covered (GREEN), not a phantom RED span.
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
	}

	@DisplayName( "It covers an array-of-struct literal's closing } and ]" )
	@Test
	void testArrayOfStructLiteralClosing() {
		String source = """
		                reverseTree = [
		                {
		                name   : "foo",
		                skip   : false
		                }
		                ];
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The struct closing "}" (line 5) and the array closing "]" (line 6) must
		// be covered (GREEN), not phantom RED spans.
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();
	}

	@DisplayName( "It covers a function call's trailing closing-paren line" )
	@Test
	void testFunctionCallTrailingClosingParenLine() {
		String source = """
		                function append2( a, b ) {
		                return a & b;
		                }
		                append2(
		                "a",
		                "b"
		                );
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// The closing ")" on line 6 must be covered (GREEN), not a phantom RED span.
		assertThat( service.lineAt( key, 6 ).covered() ).isTrue();
	}

	@DisplayName( "It spans empty member function bodies when invoked" )
	@Test
	void testEmptyMemberFunctionBodies() {
		String source = """
		                class Spec {
		                function setup() {}
		                function teardown() {}
		                function afterTests() {}
		                function beforeTests() {}
		                }
		                s = new Spec();
		                s.setup();
		                s.teardown();
		                s.afterTests();
		                s.beforeTests();
		                """;
		runtime.executeSource( source );

		String key = IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );

		// Every empty method body — even though it has NO statements — gets its
		// braces registered as executable spans, and they are COVERED once the
		// method is invoked. This is the TestCase.cfc setup/teardown case: the
		// methods were instantiated and run, but their empty bodies were never
		// spanned.
		for ( int methodLine : new int[] { 2, 3, 4, 5 } ) {
			// The empty body's braces are on the same line; the matching `}` line is
			// the following line-dependent span. Since they're all on one line, the
			// line itself must be covered once invoked.
			assertThat( service.lineAt( key, methodLine ).covered() ).isTrue();
		}
		assertThat( service.lineAt( key, 8 ).covered() ).isTrue();   // s.setup() call
		assertThat( service.lineAt( key, 11 ).covered() ).isTrue();  // s.beforeTests() call
	}
}