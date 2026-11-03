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
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * Tag-based mirror of {@link CodeProfilerTest}: same boxpiler code-profiling
 * assertions, but the source is written as Box template tags ({@code <bx:...>})
 * and run through {@code BoxRuntime.executeSource(source, context,
 * BoxSourceType.BOXTEMPLATE)}. Everything else works the same; only the
 * position-based span assertions differ because the tag text moves the code.
 */
// Profiling instrumentation is ASM-boxpiler-specific; skip under the Java boxpiler.
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class CodeProfilerTemplateTest {

	static BoxRuntime	runtime;
	CodeProfilerService	service;
	IBoxContext			context;
	boolean				prevProfiler;

	@BeforeAll
	static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void setup() {
		service											= runtime.getCodeProfilerService();
		context											= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		prevProfiler									= runtime.getConfiguration().codeProfilerEnabled;
		runtime.getConfiguration().codeProfilerEnabled	= true;
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		service.setActive( true );
		service.reset();
	}

	@AfterEach
	void teardown() {
		runtime.getConfiguration().codeProfilerEnabled = prevProfiler;
		service.setActive( false );
		service.reset();
	}

	@DisplayName( "It profiles the expression statement (tag)" )
	@Test
	void testExpressionStatement() {
		String source = """
		                <bx:set x = 2 + 2>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		assertThat( service.trackedBlueprints() ).containsKey( key );

		// Span 0 = the whole <bx:set x = 2 + 2> statement (cols 0-17). Both operands
		// of + are literals (cannot throw), so they stay in ONE span — matching the
		// script form "2 + 2;". The trailing newline's static text buffer output is
		// NOT tracked as a span. So exactly one span.
		var spanDefs = service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 1 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) );

		var spans = service.spansOnLine( key, 1 );
		assertThat( spans ).hasSize( 1 );
		assertThat( spans.get( 0 ).stats().count() ).isEqualTo( 1 );

		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles plain statements, one per line (tag)" )
	@Test
	void testPlainStatements() {
		String source = """
		                <bx:set x = 1>
		                <bx:set y = 2>
		                <bx:set z = x + y>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Three <bx:set> statements; the binary z = x + y splits left/right around
		// the RHS, and the tag's closing ">" is its own span grouped with the head
		// (always GREEN). Static newline buffer outputs are not tracked.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) );  // <bx:set x = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );  // <bx:set y = 2>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) );  // <bx:set z = x + >
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 17, true ) ); // y (RHS)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 18, true ) ); // > (tag close)

		assertThat( service.spansOnLine( key, 1 ) ).hasSize( 1 );
		assertThat( service.spansOnLine( key, 1 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 2 ) ).hasSize( 1 );
		assertThat( service.spansOnLine( key, 2 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 3 ) ).hasSize( 3 );
		assertThat( service.spansOnLine( key, 3 ).get( 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 3 ).get( 1 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spansOnLine( key, 3 ).get( 2 ).stats().count() ).isEqualTo( 1 );

		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 3 <bx:set> assignments (one span each) + the ternary statement
		// splits into 4 spans: "<bx:set x = bar ? " | "baz : " | "bum" | ">".
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 19, true ) );  // <bx:set bar = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 16, true ) );  // <bx:set baz = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) );  // <bx:set bum = 2>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 18, true ) );  // <bx:set x = bar ? >
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 18, 4, 24, true ) ); // baz : (whenTrue)
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 24, 4, 27, true ) ); // bum (whenFalse)
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 4, 27, 4, 28, true ) ); // > (tag close)

		// Pass B: bar=true so the whenTrue branch (baz) ran once; whenFalse (bum)
		// is MISSED (count 0). The tag's closing ">" ran with the tag (GREEN).
		assertThat( service.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 4, 1 ).stats().count() ).isEqualTo( 1 );   // x = bar ? cond
		assertThat( service.spanAt( key, 4, 19 ).stats().count() ).isEqualTo( 1 );  // baz ran
		assertThat( service.spanAt( key, 4, 25 ).stats().count() ).isEqualTo( 0 );  // bum missed
		assertThat( service.spanAt( key, 4, 27 ).stats().count() ).isEqualTo( 1 );  // > ran

		// Line-based: lines 1-4 covered once.
		for ( int line : new int[] { 1, 2, 3, 4 } ) {
			assertThat( service.lineAt( key, line ).covered() ).isTrue();
			assertThat( service.lineAt( key, line ).count() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "It records inclusive time for a tag that runs a body (loop)" )
	@Test
	void testTagInclusiveTiming() {
		// A tag invokes its body synchronously, so the tag's span should report the
		// whole time the tag was on the stack — including the body's work. A
		// <bx:loop> that sleeps 100ms per iteration (3 iterations) => ~300ms.
		String source = """
		                <bx:loop from="1" to="3" index="i">
		                <bx:set dummy = sleep( 100 )>
		                </bx:loop>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// The <bx:loop> tag header span (line 1, col 1). Its inclusive time must be
		// at least the 3 x 100ms the body slept.
		long	inclusive	= service.spanAt( key, 1, 1 ).stats().inclusiveNanos();
		assertThat( inclusive ).isAtLeast( 300L * 1_000_000L );
		assertThat( inclusive ).isAtMost( 900L * 1_000_000L );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// The <bx:output> body produces five spans:
		// 0: (1,0)-(1,11) the <bx:output> open tag
		// 1: (1,11)-(3,0) the "\nfoo\n" text buffer (foo rendered output)
		// 2: (3,0)-(3,7) the #now()# interpolation (real expression)
		// 3: (3,7)-(5,0) the "\nbar\n" text buffer (bar rendered output)
		// 4: (5,0)-(5,12) the </bx:output> close tag
		// The blank "\n" buffer after </bx:output> is NOT tracked.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 11, true ) ); // <bx:output>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 11, 3, 0, true ) ); // "\nfoo\n"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 7, true ) );    // #now()#
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 7, 5, 0, true ) );   // "\nbar\n"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 12, true ) );   // </bx:output>

		// Pass B: foo text, the interpolation, and bar text all executed once; the
		// <bx:output> open/close tags each ran once.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );   // <bx:output>
		assertThat( service.spanAt( key, 1, 11 ).stats().count() ).isEqualTo( 1 );  // foo
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );   // #now()#
		assertThat( service.spanAt( key, 3, 7 ).stats().count() ).isEqualTo( 1 );   // bar
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // </bx:output>

		// Line-based: the interpolation line 3 is covered once. The text buffers' spans
		// are attributed to their source-line coordinates (line 1 for "\nfoo\n", line
		// 3 for "\nbar\n"), so they do not create separate coverage entries on the
		// visual foo/bar lines.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
			runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );
		} catch ( RuntimeException e ) {
			// expected — the template throws mid-way
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 10 executable spans — 4 <bx:set> before the throw (lines 1-4), the
		// throw itself (line 5), and 5 after (lines 6-10).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
			assertThat( service.lineAt( key, line ).covered() ).isTrue();
			assertThat( service.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 1 );
		}
		for ( int line = 6; line <= 10; line++ ) {
			assertThat( service.lineAt( key, line ).covered() ).isFalse();
			assertThat( service.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 0 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 4 <bx:set> assignments (one span each) + the chain statement splits
		// into 4 operands: "result = foo & " | "bar & " | "bum & " | "quz>".
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // result = foo
		assertThat( service.spanAt( key, 5, 23 ).stats().count() ).isEqualTo( 1 );  // bar
		assertThat( service.spanAt( key, 5, 29 ).stats().count() ).isEqualTo( 1 );  // bum
		assertThat( service.spanAt( key, 5, 35 ).stats().count() ).isEqualTo( 1 );  // quz

		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
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
			runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );
		} catch ( RuntimeException e ) {
			// expected — the IIFE throws mid-chain
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: 2 <bx:set> assignments + the chain splits into foo / the IIFE
		// (whose body throw is its own span) / bar. 8 spans in source order.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) ); // <bx:set foo = "a">
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 18, true ) ); // <bx:set bar = "b">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 23, true ) ); // head + foo + " & ("
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 23, 3, 36, true ) ); // closure shell
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 38, 3, 53, true ) ); // throw body
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 36, 3, 37, true ) ); // ")"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 3, 55, 3, 56, true ) ); // "&"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 3, 63, 3, 67, true ) ); // bar>

		// Pass B: foo ran, the IIFE threw (so it counts as started), bar never ran.
		assertThat( service.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 1 );  // "foo" ran
		assertThat( service.spanAt( key, 3, 25 ).stats().count() ).isEqualTo( 1 );  // IIFE started
		assertThat( service.spanAt( key, 3, 63 ).stats().count() ).isEqualTo( 0 );  // "bar" never ran

		// Line-based: line 3 covered (foo + the started IIFE).
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a multi-line if into condition, then spans (tag)" )
	@Test
	void testIfBlock() {
		String source = """
		                <bx:if true>
		                <bx:set a = 1>
		                </bx:if>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: the <bx:if> head, the then-body <bx:set a = 1>, the </bx:if> close
		// tag, plus the \r newline buffer spans after each line.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 12, true ) ); // <bx:if true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 12, 1, 13, true ) ); // \r
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set a = 1>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 14, 2, 15, true ) ); // \r
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 8, true ) );  // </bx:if>

		// Pass B: condition true, so the then-body ran once; the </bx:if> close
		// tag also ran.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 ); // if head
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 ); // a = 1
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 ); // </bx:if>

		// Line-based: lines 1-3 covered once.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: i=0, the while HEADER (up to the condition), the CONDITION (its
		// own span so it can be marked per-iteration), the body i++, the </bx:while>
		// close tag, plus the \r newline buffer spans after each tag.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set i = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 20, true ) ); // <bx:while condition= (header)
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 20, 2, 28, true ) );// "i < 3"> (condition)
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 28, 2, 29, true ) );// \r
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 12, true ) ); // <bx:set i++>
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 12, 3, 13, true ) );// \r
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 11, true ) ); // </bx:while>

		// Pass B: the while header ran once (open tag entry); the CONDITION ran 4
		// times (i:0->1->2 true, then i=3 false to exit); body i++ ran 3 times.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 ); // i = 0
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 ); // while header
		assertThat( service.spanAt( key, 2, 20 ).stats().count() ).isEqualTo( 4 ); // condition per iteration
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 ); // i++ ran 3x
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 ); // </bx:while> close

		// Line-based: lines 1-4 covered; line 1 header 1, condition line 2, body 3.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 ); // header is FIRST span on line 2
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: i=0, the while header, the condition, sleep(100), i++, the
		// </bx:while> close tag, plus \r newline buffer spans after each tag.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 9 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set i = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 20, true ) ); // <bx:while condition= (header)
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 20, 2, 28, true ) );// "i < 3"> (condition)
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 28, 2, 29, true ) );// \r
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 21, true ) ); // <bx:set sleep( 100 )>
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 21, 3, 22, true ) );// \r
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 12, true ) ); // <bx:set i++>
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 4, 12, 4, 13, true ) );// \r
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 11, true ) ); // </bx:while>

		// Pass B: body ran 3x — sleep and i++ each 3 times.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 3 );

		// Timing: 3 x 100ms sleeps accumulated into the sleep span (~300ms).
		long sleepNanos = service.spanAt( key, 3, 0 ).stats().totalNanos();
		assertThat( sleepNanos ).isAtLeast( 300L * 1_000_000L );
		assertThat( sleepNanos ).isAtMost( 900L * 1_000_000L );

		// Line-based: lines 1-4 covered; line 5 (close tag) covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 4 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 4 ).count() ).isEqualTo( 3 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: x=2, the switch head (which runs into the first case label), each
		// case/default label, each body span, the </bx:case>/</bx:defaultcase>
		// close tags, the </bx:switch> close, plus the \r newline buffer spans.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 18 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) );   // <bx:set x = 2>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 3, 15, true ) );   // switch head + case 1 label start
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 19, true ) );   // <bx:case value="1">
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 19, 3, 20, true ) );  // \r
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 15, true ) );   // a = 10 (case 1 body)
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 15, 4, 16, true ) );  // \r
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 10, true ) );   // </bx:case> (case 1 close)
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 19, true ) );   // <bx:case value="2">
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 6, 19, 6, 20, true ) );  // \r
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 15, true ) );   // a = 20 (case 2 body)
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 7, 15, 7, 16, true ) ); // \r
		assertThat( spanDefs.get( 11 ) ).isEqualTo( new Blueprint.SpanDef( 8, 0, 8, 10, true ) );  // </bx:case> (case 2 close)
		assertThat( spanDefs.get( 12 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 16, true ) );  // <bx:defaultcase>
		assertThat( spanDefs.get( 13 ) ).isEqualTo( new Blueprint.SpanDef( 9, 16, 9, 17, true ) ); // \r
		assertThat( spanDefs.get( 14 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 15, true ) );// a = 30 (default body)
		assertThat( spanDefs.get( 15 ) ).isEqualTo( new Blueprint.SpanDef( 10, 15, 10, 16, true ) );// \r
		assertThat( spanDefs.get( 16 ) ).isEqualTo( new Blueprint.SpanDef( 11, 0, 11, 17, true ) );// </bx:defaultcase>
		assertThat( spanDefs.get( 17 ) ).isEqualTo( new Blueprint.SpanDef( 12, 0, 12, 12, true ) );// </bx:switch>

		// Pass B: x=2 matches case 2, so case 2 ran (a=20); case 1 and default did
		// not. a=10 (case 1) and a=30 (default) stayed missed.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );    // x = 2
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );    // switch head (reached)
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 0 );    // a = 10 missed (case 1)
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );    // a = 20 ran (case 2)
		assertThat( service.spanAt( key, 10, 0 ).stats().count() ).isEqualTo( 0 );   // a = 30 missed (default)

		// Case labels are entered (probe-charged) as the switch scans for a match,
		// but only the matched case's BODY runs. Case 1 body and default body missed.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );    // case 1 label (probed, body missed)
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 1 );    // case 2 label (matched)
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );    // </bx:case> (case 1 close, entered via probe)
		assertThat( service.spanAt( key, 8, 0 ).stats().count() ).isEqualTo( 1 );    // </bx:case> (case 2 close)
		assertThat( service.spanAt( key, 11, 0 ).stats().count() ).isEqualTo( 0 );   // </bx:defaultcase> (auto-broke before it)
		assertThat( service.spanAt( key, 12, 0 ).stats().count() ).isEqualTo( 1 );   // </bx:switch>

		// Line-based: x=2 (line 1) and switch head (line 2) covered once; the
		// matched case 2 body (line 7) covered once. Case 1 body (line 4), default
		// body (line 10) are not covered.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 4 ).covered() ).isFalse();  // case 1 body (a=10) missed
		assertThat( service.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 7 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 10 ).covered() ).isFalse(); // default body (a=30) missed
		assertThat( service.lineAt( key, 12 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 12 ).count() ).isEqualTo( 1 ); // </bx:switch> reached
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: the try tag, the try body's three statements, the catch tag+body,
		// the finally tag+body, the </bx:catch>/</bx:finally>/</bx:try> close tags,
		// plus the \r newline buffer spans after each tag/statement.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 21 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 8, true ) );    // <bx:try>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 8, 1, 9, true ) );    // \r
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );   // a = 1
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 14, 2, 15, true ) );  // \r
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 21, true ) );   // sleep( 100 )
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 21, 3, 22, true ) );  // \r
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 14, true ) );   // b = 2
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 4, 14, 4, 15, true ) );  // \r
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 7, 11, 7, 12, true ) );  // \r after </bx:catch>
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 10, 13, 10, 14, true ) );// \r after </bx:finally>
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 11, 0, 11, 9, true ) ); // </bx:try>
		assertThat( spanDefs.get( 11 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 10, true ) );  // <bx:catch>
		assertThat( spanDefs.get( 12 ) ).isEqualTo( new Blueprint.SpanDef( 5, 10, 5, 11, true ) ); // \r
		assertThat( spanDefs.get( 13 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 14, true ) );  // c = 3 catch
		assertThat( spanDefs.get( 14 ) ).isEqualTo( new Blueprint.SpanDef( 6, 14, 6, 15, true ) ); // \r
		assertThat( spanDefs.get( 15 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 11, true ) );  // </bx:catch>
		assertThat( spanDefs.get( 16 ) ).isEqualTo( new Blueprint.SpanDef( 8, 0, 8, 12, true ) );  // <bx:finally>
		assertThat( spanDefs.get( 17 ) ).isEqualTo( new Blueprint.SpanDef( 8, 12, 8, 13, true ) ); // \r
		assertThat( spanDefs.get( 18 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 14, true ) );  // d = 4 finally
		assertThat( spanDefs.get( 19 ) ).isEqualTo( new Blueprint.SpanDef( 9, 14, 9, 15, true ) ); // \r
		assertThat( spanDefs.get( 20 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 13, true ) );// </bx:finally>

		// Pass B: try body ran (a=1, sleep, b=2); catch did NOT (no throw); finally
		// ALWAYS ran (d=4). The sleep span was charged ~100ms.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // a = 1
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 );  // b = 2
		assertThat( service.spanAt( key, 6, 0 ).stats().count() ).isEqualTo( 0 );  // c = 3 missed
		assertThat( service.spanAt( key, 9, 0 ).stats().count() ).isEqualTo( 1 );  // d = 4 ran

		// Timing: the sleep(100) span was charged at least the 100ms.
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( service.spanAt( key, 3, 0 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );

		// Line-based: try + finally covered; catch not covered.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 6 ).covered() ).isFalse();
		assertThat( service.lineAt( key, 9 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 9 ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: the array splits per element (like struct values) inside the
		// <bx:set>: "<bx:set arr = [ " | 10 | ", " | 20 | ", " | 30 | " ]" | ">"
		// (8 spans on line 1), then the <bx:loop> header + \r, the body y = x + \r,
		// and the </bx:loop> close tag.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 13 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 16, true ) ); // "<bx:set arr = [ "
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 16, 1, 18, true ) ); // "10"
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 18, 1, 20, true ) ); // ", "
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 1, 20, 1, 22, true ) ); // "20"
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 1, 22, 1, 24, true ) ); // ", "
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 1, 24, 1, 26, true ) ); // "30"
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 1, 26, 1, 28, true ) ); // " ]"
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 1, 28, 1, 29, true ) ); // ">"
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 32, true ) ); // <bx:loop array="#arr#" item="x">
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 2, 32, 2, 33, true ) ); // \r
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 14, true ) ); // <bx:set y = x>
		assertThat( spanDefs.get( 11 ) ).isEqualTo( new Blueprint.SpanDef( 3, 14, 3, 15, true ) ); // \r
		assertThat( spanDefs.get( 12 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 10, true ) ); // </bx:loop>

		// Pass B: header ran once (arr set); body y = x ran 3 times (10, 20, 30).
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // arr = [...]
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );  // y = x ran 3x

		// Line-based: line 1 covered once; line 3 covered 3 times.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: j=0, the <bx:loop> header, then the body j = j + i splits into
		// left+right spans (the + right operand i may throw) plus the tag's ">"
		// (grouped with the head), the </bx:loop> close, and the \r buffer spans.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set j = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 35, true ) ); // <bx:loop from="0" to="2" index="i">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 35, 2, 36, true ) ); // \r
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) ); // j = j +
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 17, true ) ); // i (right operand)
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 18, true ) ); // > (tag close)
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 3, 18, 3, 19, true ) ); // \r
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 10, true ) );  // </bx:loop>

		// Pass B: header ran once; body ran 3 times (i: 0,1,2) — both parts of j+j+i.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // j = 0
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );  // j = j + ran 3x
		assertThat( service.spanAt( key, 3, 16 ).stats().count() ).isEqualTo( 3 ); // i ran 3x
		assertThat( service.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 3 ); // > ran 3x

		// Line-based: line 1 covered once; line 3 covered 3 times.
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 3 );
	}

	@DisplayName( "It short-circuits && so the right span is missed (tag)" )
	@Test
	void testAndShortCircuit() {
		String source = """
		                <bx:set a = false>
		                <bx:set b = 1>
		                <bx:set x = a && b>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: a=false, b=1, and the x = a && b statement splits into left
		// ("x = a && "), right ("b"), and the tag's closing ">" (grouped with the
		// always-run head).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) ); // <bx:set a = false>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) ); // x = a &&
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 18, true ) ); // b (right operand)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 18, 3, 19, true ) ); // > (tag close)

		// Pass B: a is false so b never ran (MISSED); the left side and the tag's
		// ">" ran once (GREEN).
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( service.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 0 ); // b never ran
		assertThat( service.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 1 ); // > ran

		// Line-based: lines 1-3 covered (line 3 via the left span).
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It marks both && operands when both run (tag)" )
	@Test
	void testAndBothRun() {
		String source = """
		                <bx:set a = true>
		                <bx:set b = 1>
		                <bx:set x = a && b>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: same span layout as testAndShortCircuit; a=true so both run.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // <bx:set a = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) ); // x = a &&
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 18, true ) ); // b (right operand)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 18, 3, 19, true ) ); // > (tag close)

		// Pass B: a=true so BOTH operands ran, plus the ">" (GREEN).
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( service.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 1 ); // b ran
		assertThat( service.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 1 ); // > ran

		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It short-circuits || so the right span is missed (tag)" )
	@Test
	void testOrShortCircuit() {
		String source = """
		                <bx:set a = true>
		                <bx:set b = 1>
		                <bx:set x = a || b>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: same span layout; a=true so b short-circuits.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // <bx:set a = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) ); // <bx:set b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) ); // x = a ||
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 3, 18, true ) ); // b (right operand)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 18, 3, 19, true ) ); // > (tag close)

		// Pass B: a=true (truthy) so b never ran (MISSED); the ">" ran (GREEN).
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( service.spanAt( key, 3, 17 ).stats().count() ).isEqualTo( 0 ); // b never ran
		assertThat( service.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 1 ); // > ran

		assertThat( service.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles a string interpolation as one span (tag)" )
	@Test
	void testStringInterpolation() {
		String source = """
		                <bx:set a = 1>
		                <bx:set x = "val #a# now">
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: a=1 and the interpolated string are each one span.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 14, true ) ); // <bx:set a = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 26, true ) ); // <bx:set x = "val #a# now">

		// Pass B: both ran once.
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );

		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Pass A: each assignment is a single span, EXCEPT the paren binary
		// ( c + d ) which splits at the + right operand d (could throw).
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
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
			assertThat( service.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 1 );
		}
		assertThat( service.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.spanAt( key, 7, 18 ).stats().count() ).isEqualTo( 1 );

		// Line-based: all covered once.
		for ( int line = 1; line <= 7; line++ ) {
			assertThat( service.lineAt( key, line ).covered() ).isTrue();
			assertThat( service.lineAt( key, line ).count() ).isEqualTo( 1 );
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
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// tag lengths:
		// L1 <bx:function name="foo"> 24
		// L2 <bx:set x = 1> 14
		// L3 <bx:return x> 13
		// L4 </bx:function> 14
		// L5 <bx:set result = foo()> 23

		// Pass A: the function shell (line 1, ending at its ">" — it must NOT
		// swallow the whitespace onto line 2's body), the body x = 1 (line 2), the
		// return (line 3), the </bx:function> close tag (line 4), the foo() call
		// site (line 5), plus the \r newline buffer spans.
		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 24, true ) );  // function shell (open tag)
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 14, true ) );  // x = 1 body
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 14, 2, 15, true ) ); // \r
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) );  // return x
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 13, 3, 14, true ) ); // \r
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 14, true ) );  // </bx:function>
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 23, true ) ); // foo() call

		// Pass B: the shell ran at declaration; the body x=1 ran on invocation; the
		// return ran; the call site ran.
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // shell
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );  // x = 1 body
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // return
		assertThat( service.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );  // foo() call site

		// Line-based: line 2 (body) covered once, line 5 (call site) covered.
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles the <bx:param> tag (BOXTEMPLATE)" )
	@Test
	void testParamTag() {
		// Variable does NOT exist — the param runs and sets the default.
		// The default is a COMPLEX expression (#now()#), so it breaks into its own
		// span (deferred closure, evaluated only when the variable is missing).
		String source = "<bx:param name=\"foo\" default=#now()#>";
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );
		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// DEBUG
		// System.out.println( "=== testParamTag dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 29, true ) );  // tag head incl. "default="
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 1, 29, 1, 36, true ) ); // #now()# default (pounds included)
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 1, 36, 1, 37, true ) ); // > tag close
		assertThat( service.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );
		// foo is missing, so the deferred default ran.
		assertThat( service.spanAt( key, 1, 29 ).stats().count() ).isEqualTo( 1 );
		assertThat( service.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 1 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles the <bx:param> tag when the variable already exists (BOXTEMPLATE)" )
	@Test
	void testParamTagExists() {
		// The variable EXISTS, so the param statement still executes (skips the
		// default) — the DEFAULT EXPRESSION is NOT evaluated (count 0, RED).
		String source = "<bx:set foo = 1>\n<bx:param name=\"foo\" default=#now()#>";
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );
		String	key			= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// DEBUG
		// System.out.println( "=== testParamTagExists dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		var		spanDefs	= service.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 16, true ) );  // <bx:set foo = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 29, true ) );  // param tag head
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 29, 2, 36, true ) ); // #now()# default (pounds included)
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 2, 36, 2, 37, true ) ); // > tag close
		assertThat( service.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );
		// foo exists — the default expression was NEVER evaluated (count 0, RED).
		assertThat( service.spanAt( key, 2, 29 ).stats().count() ).isEqualTo( 0 );
		assertThat( service.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( service.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It locates a close tag written in different case (tag)" )
	@Test
	void testUppercaseWhileCloseTag() {
		// BoxLang tags are case-insensitive. The close-tag locator scans the source
		// for a lowercase "</bx:name"/"</cfname"; an uppercase "</BX:WHILE>" must
		// still be found, or the close-tag span is never registered (reported missed
		// even though it ran).
		String source = """
		                <bx:set i = 0>
		                <bx:while condition="i < 3">
		                <bx:set i++>
		                </BX:WHILE>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// DEBUG
		// System.out.println( "=== testUppercaseWhileCloseTag dump" );
		// System.out.print( service.dumpSpans( key, source ) );

		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 ); // </BX:WHILE>
	}

	@DisplayName( "It marks a reached <BX:ELSEIF> tag written in different case (tag)" )
	@Test
	void testUppercaseElseIfElseTags() {
		// BoxLang tags are case-insensitive: an uppercase "<BX:ELSEIF>" tag that is
		// REACHED must be registered and marked (count 1). (Earlier the branch-tag
		// locator used a case-sensitive scan; this locks in case-insensitive support.)
		String source = """
		                <bx:if false>
		                <bx:set a = 1>
		                <BX:ELSEIF true>
		                <bx:set b = 2>
		                </bx:if>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 ); // <BX:ELSEIF> reached
	}

	@DisplayName( "It marks a reached <bx:elseif> tag" )
	@Test
	void testReachedElseIfTagIsMarked() {
		// A REACHED elseif tag must be marked (count 1). NOTE: the tag is marked by
		// the generic mark hook (AsmTranspiler.transform), NOT by emitElseIfTagMark —
		// verified by disabling emitElseIfTagMark: this assertion still passes. This
		// test guards that a reached elseif tag is covered (and that emitElseIfTagMark
		// never *unmarks* it via a stale claim).
		String source = """
		                <bx:if false>
		                <bx:set a = 1>
		                <bx:elseif true>
		                <bx:set b = 2>
		                </bx:if>
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// if false → elseif true → the elseif tag ran and its body ran.
		assertThat( service.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 ); // <bx:elseif true>
		assertThat( service.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 1 ); // b = 2
	}

	@DisplayName( "It profiles a SINGLE-LINE if/elseif/else chain (no crash, correct spans)" )
	@Test
	void testSingleLineElseIfChain() {
		// Regression: a nested template <bx:elseif>'s OWN source text stopped at its
		// then body while its attached <bx:else> branch started BEYOND that text. The
		// else-keyword scan (which walks the node's own text) then ran off the end and
		// threw StringIndexOutOfBoundsException during profiled compilation. The parser
		// now extends each elseif's extent over the else chain it owns.
		String source = "<bx:if false><bx:set a=1><bx:elseif false><bx:set a=2><bx:else><bx:set a=3></bx:if>";
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// if false → elseif false → else runs. The keyword tags and bodies each get
		// their own span; only the reached ones are marked.
		assertThat( service.spanAt( key, 1, 13 ).stats().count() ).isEqualTo( 0 ); // <bx:set a=1> skipped
		assertThat( service.spanAt( key, 1, 25 ).stats().count() ).isEqualTo( 0 ); // <bx:elseif false> skipped
		assertThat( service.spanAt( key, 1, 42 ).stats().count() ).isEqualTo( 0 ); // <bx:set a=2> skipped
		assertThat( service.spanAt( key, 1, 54 ).stats().count() ).isEqualTo( 1 ); // <bx:else> reached
		assertThat( service.spanAt( key, 1, 63 ).stats().count() ).isEqualTo( 1 ); // <bx:set a=3> ran
	}

	@DisplayName( "It profiles a SINGLE-LINE if/elseif/elseif/else chain (no crash)" )
	@Test
	void testSingleLineMultipleElseIfChain() {
		// Same regression with MORE than one elseif on one line: each nested elseif's
		// extent must reach the <bx:else> so every keyword scan stays in bounds.
		String source = "<bx:if false><bx:set a=1><bx:elseif true><bx:set a=2><bx:elseif false><bx:set a=3><bx:else><bx:set a=4></bx:if>";
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// if false → first elseif true → its body runs; the rest are skipped.
		assertThat( service.spanAt( key, 1, 25 ).stats().count() ).isEqualTo( 1 ); // <bx:elseif true> reached
		assertThat( service.spanAt( key, 1, 41 ).stats().count() ).isEqualTo( 1 ); // <bx:set a=2> ran
		assertThat( service.spanAt( key, 1, 53 ).stats().count() ).isEqualTo( 0 ); // <bx:elseif false> skipped
		assertThat( service.spanAt( key, 1, 82 ).stats().count() ).isEqualTo( 0 ); // <bx:else> skipped
		assertThat( service.spanAt( key, 1, 91 ).stats().count() ).isEqualTo( 0 ); // <bx:set a=4> skipped
	}

	@DisplayName( "It splits a tag's RUNTIME attribute values into their own spans" )
	@Test
	void testComponentRuntimeAttributeValuesSplit() {
		// A tag's runtime-expression attribute values (e.g. var="#sleep( 5 )#") must
		// break out into their OWN spans — exactly like function-call arguments —
		// because evaluating them is separate work from evaluating the tag. Literal
		// values stay fused into the tag shell. This must be IDENTICAL for the
		// single-line and multi-line forms: line breaks and inter-attribute whitespace
		// never change the span model.
		String source = """
		                <bx:dump var="#sleep( 5 )#" label="#sleep( 7 )#">

		                <bx:dump
		                    var="#sleep( 5 )#"
		                    label="#sleep( 7 )#">
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );
		String					key		= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		java.util.List<String>	single	= new java.util.ArrayList<>();
		java.util.List<String>	multi	= new java.util.ArrayList<>();
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			String text = sourceSlice( source, s );
			if ( s.startLine() == 1 ) {
				single.add( text );
			} else if ( s.startLine() >= 3 ) {
				multi.add( text );
			}
		}

		// The EXACT token sequence — one-line and multi-line must be IDENTICAL.
		// (This is the invariant that failed before: the shell pieces were cut
		// differently by whitespace, and `#">` was missing on the one-liner.)
		assertThat( single ).isEqualTo( multi );
		assertThat( single ).containsExactly(
		    "<bx:dump", "var=\"#", "sleep( 5 )", "#\"", "label=\"#", "sleep( 7 )", "#\">" ).inOrder();

		// NO span may contain whitespace (the inter-attribute gaps must be unspanned),
		// and no span may SPAN A LINE BREAK (which would prove line breaks matter).
		// EVERY span must have run EXACTLY ONCE (no shell token double-counted, no
		// shell token left unmarked) — uniform across BOTH tag forms.
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			String raw = sourceSliceRaw( source, s );
			assertThat( raw ).doesNotContain( "\n" );
			assertThat( raw ).isEqualTo( raw.strip() );
			assertThat( s.stats().count() ).isEqualTo( 1 );
		}
	}

	@DisplayName( "It charges a tag's attribute evaluation time to the tag, and the closing > carries it" )
	@Test
	void testTagAttributeTimeIsInclusive() {
		// The tag's on-stack (inclusive) time must INCLUDE evaluating its attributes:
		// the two sleeps total ~12ms, so the tag SHELL tokens (including the closing
		// `>`) must each report >= ~10ms — NOT a separate ~0 probe time. And each
		// runtime value span reports its own ~5ms / ~7ms.
		String source = """
		                <bx:dump var="#sleep( 5 )#" label="#sleep( 7 )#">
		                """;
		runtime.executeSource( source, context, BoxSourceType.BOXTEMPLATE );
		String						key		= IBoxpiler.MD5( BoxSourceType.BOXTEMPLATE.toString() + source );

		// Find the shell's closing `>` span and the head span by their source text.
		CodeProfilerService.Span	tail	= null;
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( "#\">".equals( sourceSliceRaw( source, s ) ) ) {
				tail = s;
			}
		}
		assertThat( tail ).isNotNull();
		// The closing `>` ran once and carries the tag's inclusive time (~12ms of
		// sleeps + tag overhead), NOT a separate ~0 probe time.
		assertThat( tail.stats().count() ).isEqualTo( 1 );
		assertThat( tail.stats().onStackNanos() ).isAtLeast( 10L * 1_000_000L );
	}

	/** The exact source characters of a span (whitespace preserved). */
	private static String sourceSliceRaw( String source, CodeProfilerService.Span s ) {
		String[] lines = source.split( "\n", -1 );
		if ( s.startLine() == s.endLine() ) {
			String line = lines[ s.startLine() - 1 ];
			return line.substring( s.startCol(), Math.min( s.endCol(), line.length() ) );
		}
		StringBuilder sb = new StringBuilder();
		sb.append( lines[ s.startLine() - 1 ].substring( s.startCol() ) );
		for ( int ln = s.startLine() + 1; ln < s.endLine(); ln++ ) {
			sb.append( '\n' ).append( lines[ ln - 1 ] );
		}
		sb.append( '\n' ).append( lines[ s.endLine() - 1 ], 0, s.endCol() );
		return sb.toString();
	}

	/** The span text with whitespace runs collapsed to single spaces and trimmed. */
	private static String sourceSlice( String source, CodeProfilerService.Span s ) {
		return sourceSliceRaw( source, s ).replaceAll( "\\s+", " " ).trim();
	}
}