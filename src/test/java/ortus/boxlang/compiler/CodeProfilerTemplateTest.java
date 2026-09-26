/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:// //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * Tag-based mirror of {@link CodeProfilerTest}: same boxpiler code-profiling
 * assertions, but the source is written as Box template tags ({@code <bx:...>})
 * and run through {@code BoxRuntime.executeSource(source, context,
 * BoxSourceType.BOXTEMPLATE)}. Everything else works the same; only the
 * position-based span assertions differ because the tag text moves the code.
 */
class CodeProfilerTemplateTest {

	private BoxRuntime	runtime;
	private boolean		prevProfiler;

	@BeforeEach
	void setup() {
		this.runtime										= BoxRuntime.getInstance();
		this.prevProfiler									= this.runtime.getConfiguration().codeProfilerEnabled;
		this.runtime.getConfiguration().codeProfilerEnabled	= true;
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
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

	@DisplayName( "It profiles the expression statement (tag)" )
	@Test
	void testExpressionStatement() {
		String source = """
		                <bx:set x = 2 + 2>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( key );

		// Span 0 = the whole <bx:set x = 2 + 2> statement (cols 0-17). Both operands
		// of + are literals (cannot throw), so they stay in ONE span — matching the
		// script form "2 + 2;". The trailing newline's static text buffer output is
		// NOT tracked as a span. So exactly one span.
		var spanDefs = CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 1 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) );

		var spans = CodeProfilerService.spansOnLine( key, 1 );
		assertThat( spans ).hasSize( 1 );
		assertThat( spans.get( 0 ).stats().count() ).isEqualTo( 1 );

		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles plain statements, one per line (tag)" )
	@Test
	void testPlainStatements() {
		String source = """
		                <bx:set x = 1>
		                <bx:set y = 2>
		                <bx:set z = x + y>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Three <bx:set> statements; the binary z = x + y splits left/right around
		// the RHS. Static newline buffer outputs are not tracked, so no noise spans.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) );  // <bx:set x = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );  // <bx:set y = 2>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) );  // <bx:set z = x + >
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 18, true ) ); // y>

		assertThat( CodeProfilerService.spansOnLine( key, 1 ) ).hasSize( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 1 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 2 ) ).hasSize( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 2 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 3 ) ).hasSize( 2 );
		assertThat( CodeProfilerService.spansOnLine( key, 3 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spansOnLine( key, 3 ).get( 1 ).stats().count() ).isEqualTo( 1 );

		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles each ternary operand as its own span (tag)" )
	@Test
	void testTernary() {
		String source = """
		                <bx:set bar = true>
		                <bx:set baz = 1>
		                <bx:set bum = 2>
		                <bx:set x = bar ? baz : bum>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 3 <bx:set> assignments (one span each) + the ternary statement
		// splits into 3 spans: "<bx:set x = bar ? " | "baz : " | "bum".
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 19, true ) );  // <bx:set bar = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 16, true ) );  // <bx:set baz = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) );  // <bx:set bum = 2>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 18, true ) );  // <bx:set x = bar ? >
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 18, 4, 24, true ) ); // baz : (whenTrue)
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 24, 4, 28, true ) ); // bum (whenFalse)

		// Pass B: bar=true so the whenTrue branch (baz) ran once; whenFalse (bum)
		// is MISSED (count 0).
		assertThat( CodeProfilerService.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 4, 1 ).stats().count() ).isEqualTo( 1 );   // x = bar ? cond
		assertThat( CodeProfilerService.spanAt( key, 4, 19 ).stats().count() ).isEqualTo( 1 );  // baz ran
		assertThat( CodeProfilerService.spanAt( key, 4, 25 ).stats().count() ).isEqualTo( 0 );  // bum missed

		// Line-based: lines 1-4 covered once.
		for ( int line : new int[] { 1, 2, 3, 4 } ) {
			assertThat( CodeProfilerService.lineAt( key, line ).covered() ).isTrue();
			assertThat( CodeProfilerService.lineAt( key, line ).count() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "It tracks rendered text and interpolation but skips blank buffer output (tag)" )
	@Test
	void testBufferOutputTextAndInterpolation() {
		String source = """
		                <bx:output>
		                foo
		                #now()#
		                bar
		                </bx:output>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// The <bx:output> body produces four spans:
		// 0: (1,11)-(1,16) the "\nfoo\n" text buffer (foo rendered output)
		// 1: (3,0)-(3,7) the #now()# interpolation (real expression)
		// 2: (3,7)-(3,12) the "\nbar\n" text buffer (bar rendered output)
		// 3: (3,12)-(5,12) spillover to the </bx:output> close
		// The blank "\n" buffer after </bx:output> is NOT tracked.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 11, 1, 16, true ) ); // "\nfoo\n"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 7, true ) );    // #now()#
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 7, 3, 12, true ) );   // "\nbar\n"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 12, 5, 12, true ) );  // spillover

		// Pass B: foo text, the interpolation, and bar text all executed once; the
		// spillover and the blank trailing buffer never ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 11 ).stats().count() ).isEqualTo( 1 );  // foo
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // #now()#
		assertThat( CodeProfilerService.spanAt( key, 3, 7 ).stats().count() ).isEqualTo( 1 );   // bar
		assertThat( CodeProfilerService.spanAt( key, 3, 12 ).stats().count() ).isEqualTo( 0 );  // spillover

		// Line-based: the interpolation line 3 is covered once. The text buffers' spans
		// are attributed to their source-line coordinates (line 1 for "\nfoo\n", line
		// 3 for "\nbar\n"), so they do not create separate coverage entries on the
		// visual foo/bar lines.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It only marks statements that ran before a throw (tag)" )
	@Test
	void testThrow() {
		String source = """
		                <bx:set i = 5>
		                <bx:set i++>
		                <bx:set i++>
		                <bx:set i++>
		                <bx:throw message="boom">
		                <bx:set i++>
		                <bx:set i++>
		                <bx:set i++>
		                <bx:set i++>
		                <bx:set i++>
		                """;
		try {
			runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );
		} catch ( RuntimeException e ) {
			// expected — the template throws mid-way
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 10 executable spans — 4 <bx:set> before the throw (lines 1-4), the
		// throw itself (line 5), and 5 after (lines 6-10).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 10 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) );  // <bx:set i = 5>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 25, true ) );  // <bx:throw message="boom">
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 8, 0, 8, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 12, true ) );  // <bx:set i++>
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 12, true ) ); // <bx:set i++>

		// Lines 1-5 ran (i=5 + three i++ + the throw); lines 6-10 never ran.
		for ( int line = 1; line <= 5; line++ ) {
			assertThat( CodeProfilerService.lineAt( key, line ).covered() ).isTrue();
			assertThat( CodeProfilerService.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 1 );
		}
		for ( int line = 6; line <= 10; line++ ) {
			assertThat( CodeProfilerService.lineAt( key, line ).covered() ).isFalse();
			assertThat( CodeProfilerService.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 0 );
		}
	}

	@DisplayName( "It splits each binary operand into its own span (tag)" )
	@Test
	void testBinaryChain() {
		String source = """
		                <bx:set foo = "a">
		                <bx:set bar = "b">
		                <bx:set bum = "c">
		                <bx:set quz = "d">
		                <bx:set result = foo & bar & bum & quz>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 4 <bx:set> assignments (one span each) + the chain statement splits
		// into 4 operands: "result = foo & " | "bar & " | "bum & " | "quz>".
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) );  // <bx:set foo = "a">
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 18, true ) );  // <bx:set bar = "b">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 18, true ) );  // <bx:set bum = "c">
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 18, true ) );  // <bx:set quz = "d">
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 23, true ) );  // result = foo &
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 23, 5, 29, true ) ); // bar &
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 5, 29, 5, 35, true ) ); // bum &
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 5, 35, 5, 39, true ) ); // quz>

		// Pass B: the whole chain ran, every operand exactly once.
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // result = foo
		assertThat( CodeProfilerService.spanAt( key, 5, 23 ).stats().count() ).isEqualTo( 1 );  // bar
		assertThat( CodeProfilerService.spanAt( key, 5, 29 ).stats().count() ).isEqualTo( 1 );  // bum
		assertThat( CodeProfilerService.spanAt( key, 5, 35 ).stats().count() ).isEqualTo( 1 );  // quz

		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It stops covering spans after a mid-statement throw (tag)" )
	@Test
	void testBinaryChainWithThrow() {
		String source = """
		                <bx:set foo = "a">
		                <bx:set bar = "b">
		                <bx:set result = foo & ( function() { throw( "boom" ); } )() & bar>
		                """;
		try {
			runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );
		} catch ( RuntimeException e ) {
			// expected — the IIFE throws mid-chain
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 2 <bx:set> assignments + the chain splits into foo / the IIFE
		// (whose body throw is its own span) / bar. 7 spans in source order.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) ); // <bx:set foo = "a">
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 18, true ) ); // <bx:set bar = "b">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 23, true ) ); // head + foo + " & ("
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 23, 3, 36, true ) ); // closure shell
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 38, 3, 53, true ) ); // throw body
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 53, 3, 63, true ) ); // } )() &
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 3, 63, 3, 67, true ) ); // bar>

		// Pass B: foo ran, the IIFE threw (so it counts as started), bar never ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 1 );  // "foo" ran
		assertThat( CodeProfilerService.spanAt( key, 3, 25 ).stats().count() ).isEqualTo( 1 );  // IIFE started
		assertThat( CodeProfilerService.spanAt( key, 3, 63 ).stats().count() ).isEqualTo( 0 );  // "bar" never ran

		// Line-based: line 3 covered (foo + the started IIFE).
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a multi-line if into condition, then spans (tag)" )
	@Test
	void testIfBlock() {
		String source = """
		                <bx:if true>
		                <bx:set a = 1>
		                </bx:if>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: the <bx:if> head (line 1), the then-body <bx:set a = 1> (line 2),
		// and the spillover to </bx:if> (lines 2-3).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 12, true ) ); // <bx:if true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set a = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 14, 3, 8, true ) );  // spillover to </bx:if>

		// Pass B: condition true, so the then-body ran once; the spillover (which
		// includes the executing </bx:if> close) also ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 ); // if condition
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 ); // a = 1
		assertThat( CodeProfilerService.spanAt( key, 2, 14 ).stats().count() ).isEqualTo( 1 ); // spillover to </bx:if>

		// Line-based: lines 1-2 covered once; line 3 covered via the spillover.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a while loop into condition and body spans (tag)" )
	@Test
	void testWhile() {
		String source = """
		                <bx:set i = 0>
		                <bx:while condition="i < 3">
		                <bx:set i++>
		                </bx:while>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: i=0, the while head, the body i++, and the spillover to </bx:while>.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set i = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 28, true ) ); // <bx:while condition="i < 3">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 12, true ) ); // <bx:set i++>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 12, 4, 11, true ) );// spillover to </bx:while>

		// Pass B: the loop condition ran once (header); body i++ ran 3 times (i:0->1->2).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 ); // i = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 ); // while head
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 ); // i++ ran 3x
		assertThat( CodeProfilerService.spanAt( key, 3, 12 ).stats().count() ).isEqualTo( 3 ); // spillover executes per iteration

		// Line-based: lines 1-4 covered; line 1 and 2 count 1, line 3 count 3.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 3 );
	}

	@DisplayName( "It accumulates per-iteration timing inside a loop body (tag)" )
	@Test
	void testWhileTiming() {
		String source = """
		                <bx:set i = 0>
		                <bx:while condition="i < 3">
		                <bx:set sleep( 100 )>
		                <bx:set i++>
		                </bx:while>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: i=0, the while head, sleep(100), i++, and the </bx:while> spillover.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set i = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 28, true ) ); // <bx:while condition="i < 3">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 21, true ) ); // <bx:set sleep( 100 )>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 12, true ) ); // <bx:set i++>
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 12, 5, 11, true ) );// spillover to </bx:while>

		// Pass B: body ran 3x — sleep and i++ each 3 times.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );

		// Timing: 3 x 100ms sleeps accumulated into the sleep span (~300ms).
		long sleepNanos = CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos();
		assertThat( sleepNanos ).isAtLeast( 300L * 1_000_000L );
		assertThat( sleepNanos ).isAtMost( 900L * 1_000_000L );

		// Line-based: lines 1-4 covered; line 5 (spillover) covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 4 ).count() ).isEqualTo( 3 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 3 );
	}

	@DisplayName( "It splits a switch into condition, case, and body spans (tag)" )
	@Test
	void testSwitch() {
		String source = """
		                <bx:set x = 2>
		                <bx:switch expression="#x#">
		                <bx:case value="1">
		                <bx:set a = 10>
		                </bx:case>
		                <bx:case value="2">
		                <bx:set a = 20>
		                </bx:case>
		                <bx:defaultcase>
		                <bx:set a = 30>
		                </bx:defaultcase>
		                </bx:switch>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: x=2, then the switch head + each case/default label and body
		// span, ending with the </bx:switch> close. No <bx:break> needed in tags.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 11 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) );   // <bx:set x = 2>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 3, 19, true ) );   // switch head + case 1 label
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 19, 4, 0, true ) );   // case 1 label tail
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 15, true ) );   // a = 10 (case 1 body)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 15, 6, 19, true ) );  // case 1 close + case 2 label
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 19, 7, 0, true ) );   // case 2 label tail
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 15, true ) );   // a = 20 (case 2 body)
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 7, 15, 9, 16, true ) );  // case 2 close + default label
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 9, 16, 10, 0, true ) );  // default label tail
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 15, true ) ); // a = 30 (default body)
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 10, 15, 12, 12, true ) );// default close + </bx:switch>

		// Pass B: x=2 matches case 2, so case 2 ran (a=20); case 1 and default did
		// not. a=10 (case 1) and a=30 (default) stayed missed.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );    // x = 2
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );    // switch cond (reached)
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 0 );    // a = 10 missed (case 1)
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );    // a = 20 ran (case 2)
		assertThat( CodeProfilerService.spanAt( key, 10, 0 ).stats().count() ).isEqualTo( 0 );   // a = 30 missed (default)

		// The spillover/label spans that merely carry execution from the switch
		// head into the eventually-matched case 2 all ran exactly once.
		assertThat( CodeProfilerService.spanAt( key, 3, 19 ).stats().count() ).isEqualTo( 0 );   // case 1 label tail (never entered case 1)
		assertThat( CodeProfilerService.spanAt( key, 4, 15 ).stats().count() ).isEqualTo( 0 );   // case 1 close + case 2 label (not marked)
		assertThat( CodeProfilerService.spanAt( key, 6, 19 ).stats().count() ).isEqualTo( 1 );   // case 2 label tail (matched case 2 label)
		// After case 2's body, the auto-break fires as execution reaches the default
		// label, so the case-2-close + default-label span is entered (count 1) but
		// a=30 never runs.
		assertThat( CodeProfilerService.spanAt( key, 7, 15 ).stats().count() ).isEqualTo( 1 );   // case 2 close + default label (entered, then breaks)
		// After case 2 auto-breaks, execution jumps past the default and the
		// </bx:switch> close, so the default tail + switch-close spillover (line 10
		// col 15) never runs.
		assertThat( CodeProfilerService.spanAt( key, 10, 15 ).stats().count() ).isEqualTo( 0 );

		// Line-based: x=2 (line 1) and switch head (line 2) covered once; the
		// matched case 2 body (line 7) covered once. Case 1 body (line 4), default
		// body (line 10), and the </bx:switch> spillover (line 12) are not covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isFalse();  // case 1 body (a=10) missed
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 7 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 10 ).covered() ).isFalse(); // default body (a=30) missed
		assertThat( CodeProfilerService.lineAt( key, 12 ).covered() ).isFalse(); // </bx:switch> spillover not reached
	}

	@DisplayName( "It profiles try/catch/finally block statements (tag)" )
	@Test
	void testTry() {
		String source = """
		                <bx:try>
		                <bx:set a = 1>
		                <bx:set sleep( 100 )>
		                <bx:set b = 2>
		                <bx:catch>
		                <bx:set c = 3>
		                </bx:catch>
		                <bx:finally>
		                <bx:set d = 4>
		                </bx:finally>
		                </bx:try>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: the try body's three statements, the catch body, the finally
		// body, and their closing-tag spillovers are each their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );   // a = 1
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 21, true ) );   // sleep( 100 )
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 14, true ) );   // b = 2
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 14, true ) );   // c = 3 catch
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 6, 14, 7, 11, true ) );  // catch spillover
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 14, true ) );   // d = 4 finally
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 9, 14, 11, 9, true ) );  // finally spillover to </bx:try>

		// Pass B: try body ran (a=1, sleep, b=2); catch did NOT (no throw); finally
		// ALWAYS ran (d=4). The sleep span was charged ~100ms.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // a = 1
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );  // b = 2
		assertThat( CodeProfilerService.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 0 );  // c = 3 missed
		assertThat( CodeProfilerService.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 1 );  // d = 4 ran

		// Timing: the sleep(100) span was charged at least the 100ms.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );

		// Line-based: try + finally covered; catch not covered.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 6 ).covered() ).isFalse();
		assertThat( CodeProfilerService.lineAt( key, 9 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 9 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a for-in loop into collection and body spans (tag)" )
	@Test
	void testForIn() {
		String source = """
		                <bx:set arr = [ 10, 20, 30 ]>
		                <bx:loop array="#arr#" item="x">
		                <bx:set y = x>
		                </bx:loop>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: arr=[...] (line 1), the body y = x (line 3), and the spillover to
		// </bx:loop> (lines 3-4). The <bx:loop> header itself merges into the
		// running span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 29, true ) ); // <bx:set arr = [ 10, 20, 30 ]>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 14, true ) ); // <bx:set y = x>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 14, 4, 10, true ) );// spillover to </bx:loop>

		// Pass B: header ran once (arr set); body y = x ran 3 times (10, 20, 30).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // arr = [...]
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );  // y = x ran 3x

		// Line-based: line 1 covered once; line 3 covered 3 times.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
	}

	@DisplayName( "It splits a numeric for loop into header and body spans (tag)" )
	@Test
	void testForIndex() {
		String source = """
		                <bx:set j = 0>
		                <bx:loop from="0" to="2" index="i">
		                <bx:set j = j + i>
		                </bx:loop>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: j=0, then the body j = j + i splits into left+right spans (the +
		// right operand i may throw), and the spillover to </bx:loop>. The <bx:loop>
		// header merges into the running span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set j = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) ); // j = j +
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 18, true ) ); // i>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 18, 4, 10, true ) );// spillover to </bx:loop>

		// Pass B: header ran once; body ran 3 times (i: 0,1,2) — both parts of j+j+i.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // j = 0
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );  // j = j + ran 3x
		assertThat( CodeProfilerService.spanAt( key, 3, 16 ).stats().count() ).isEqualTo( 3 ); // i ran 3x

		// Line-based: line 1 covered once; line 3 covered 3 times.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 3 );
	}

	@DisplayName( "It short-circuits && so the right span is missed (tag)" )
	@Test
	void testAndShortCircuit() {
		String source = """
		                <bx:set a = false>
		                <bx:set b = 1>
		                <bx:set x = a && b>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: a=false, b=1, and the x = a && b statement splits into left
		// ("x = a && ") and right ("b>") spans because the right operand may throw.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) ); // <bx:set a = false>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) ); // x = a &&
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 19, true ) ); // b> (right operand)

		// Pass B: a is false so b never ran (MISSED); the left side ran once.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( CodeProfilerService.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 0 ); // b never ran

		// Line-based: lines 1-3 covered (line 3 via the left span).
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks both && operands when both run (tag)" )
	@Test
	void testAndBothRun() {
		String source = """
		                <bx:set a = true>
		                <bx:set b = 1>
		                <bx:set x = a && b>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: same span layout as testAndShortCircuit; a=true so both run.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // <bx:set a = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) ); // x = a &&
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 19, true ) ); // b> (right operand)

		// Pass B: a=true so BOTH operands ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( CodeProfilerService.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 1 ); // b ran

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It short-circuits || so the right span is missed (tag)" )
	@Test
	void testOrShortCircuit() {
		String source = """
		                <bx:set a = true>
		                <bx:set b = 1>
		                <bx:set x = a || b>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: same span layout; a=true so b short-circuits.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // <bx:set a = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) ); // x = a ||
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 19, true ) ); // b> (right operand)

		// Pass B: a=true (truthy) so b never ran (MISSED).
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( CodeProfilerService.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 0 ); // b never ran

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles a string interpolation as one span (tag)" )
	@Test
	void testStringInterpolation() {
		String source = """
		                <bx:set a = 1>
		                <bx:set x = "val #a# now">
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: a=1 and the interpolated string are each one span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set a = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 26, true ) ); // <bx:set x = "val #a# now">

		// Pass B: both ran once.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );

		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles unary not, negation, and parens as single spans (tag)" )
	@Test
	void testUnaryNegateParen() {
		String source = """
		                <bx:set a = false>
		                <bx:set b = 5>
		                <bx:set c = 1>
		                <bx:set d = 2>
		                <bx:set x = !a>
		                <bx:set y = -b>
		                <bx:set z = ( c + d )>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: each assignment is a single span, EXCEPT the paren binary
		// ( c + d ) which splits at the + right operand d (could throw).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) ); // a = false
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // b = 5
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 14, true ) ); // c = 1
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 14, true ) ); // d = 2
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 15, true ) ); // x = !a
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 15, true ) ); // y = -b
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 18, true ) ); // z = ( c +
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 7, 18, 7, 22, true ) );// d )>

		// Pass B: each ran once (both parts of the paren binary ran).
		for ( int line = 5; line <= 6; line++ ) {
			assertThat( CodeProfilerService.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 1 );
		}
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 7, 18 ).stats().count() ).isEqualTo( 1 );

		// Line-based: all covered once.
		for ( int line = 1; line <= 7; line++ ) {
			assertThat( CodeProfilerService.lineAt( key, line ).covered() ).isTrue();
			assertThat( CodeProfilerService.lineAt( key, line ).count() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "It splits a function declaration shell from its body span (tag)" )
	@Test
	void testFunctionShellAndBody() {
		String source = """
		                <bx:function name="foo">
		                <bx:set x = 1>
		                <bx:return x>
		                </bx:function>
		                <bx:set result = foo()>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// tag lengths:
		// L1 <bx:function name="foo"> 23
		// L2 <bx:set x = 1> 14
		// L3 <bx:return x> 13
		// L4 </bx:function> 14
		// L5 <bx:set result = foo()> 23

		// Pass A: the function shell (line 1), the body x = 1 (line 2), the return
		// (line 3), their tails, and the foo() call site (line 5).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 2, 0, true ) );   // function shell
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );  // x = 1 body
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 14, 3, 0, true ) );  // x=1 tail
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) );  // return x
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 13, 4, 14, true ) ); // return tail + </bx:function>
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 23, true ) ); // foo() call

		// Pass B: the shell ran at declaration; the body x=1 ran on invocation; the
		// return ran; the call site ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // shell
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // x = 1 body
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // return
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );  // foo() call site

		// Line-based: line 2 (body) covered once, line 5 (call site) covered.
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}
}