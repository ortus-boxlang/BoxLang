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
 * Tag-based CFML mirror of {@link CodeProfilerTemplateTest}: same boxpiler
 * code-profiling assertions, but the source is written as CFML template tags
 * ({@code <cfset>}, {@code <cfif>}, {@code <cfwhile>}, ...) and run through
 * {@code BoxRuntime.executeSource(source, context, BoxSourceType.CFTEMPLATE)}.
 * Everything else works the same; only the position-based span assertions
 * differ because the CF tag text moves the code relative to the Box tags.
 */
class CodeProfilerCFTemplateTest {

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
		                <cfset x = 2 + 2>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String key = IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for ExpressionStatement
		// System.out.println( "=== dump ExpressionStatement" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( key );

		// Span 0 = the whole <cfset x = 2 + 2> statement (cols 0-17). Both operands
		// of + are literals (cannot throw), so they stay in ONE span — matching the
		// script form "2 + 2;". The trailing newline's static text buffer output is
		// NOT tracked as a span. So exactly one span.
		var spanDefs = CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 1 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) );

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
		                <cfset x = 1>
		                <cfset y = 2>
		                <cfset z = x + y>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for PlainStatements
		// System.out.println( "=== dump PlainStatements" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Three <cfset> statements; the binary z = x + y splits left/right around
		// the RHS. Static newline buffer outputs are not tracked, so no noise spans.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) );  // <cfset x = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) );  // <cfset y = 2>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 15, true ) );  // <cfset z = x + >
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 15, 3, 17, true ) ); // y>

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
		                <cfset bar = true>
		                <cfset baz = 1>
		                <cfset bum = 2>
		                <cfset x = bar ? baz : bum>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for Ternary
		// System.out.println( "=== dump Ternary" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 3 <cfset> assignments (one span each) + the ternary statement
		// splits into 3 spans: "<cfset x = bar ? " | "baz : " | "bum".
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 18, true ) );  // <cfset bar = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 15, true ) );  // <cfset baz = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 15, true ) );  // <cfset bum = 2>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 17, true ) );  // <cfset x = bar ? >
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 17, 4, 23, true ) ); // baz : (whenTrue)
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 4, 23, 4, 27, true ) ); // bum (whenFalse)

		// Pass B: bar=true so the whenTrue branch (baz) ran once; whenFalse (bum)
		// is MISSED (count 0).
		assertThat( CodeProfilerService.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 4, 1 ).stats().count() ).isEqualTo( 1 );   // x = bar ? cond
		assertThat( CodeProfilerService.spanAt( key, 4, 18 ).stats().count() ).isEqualTo( 1 );  // baz ran
		assertThat( CodeProfilerService.spanAt( key, 4, 24 ).stats().count() ).isEqualTo( 0 );  // bum missed

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
		                <cfoutput>
		                foo
		                #now()#
		                bar
		                </cfoutput>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for BufferOutputTextAndInterpolation
		// System.out.println( "=== dump BufferOutputTextAndInterpolation" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// The <cfoutput> body produces four spans:
		// 0: (1,11)-(1,16) the "\nfoo\n" text buffer (foo rendered output)
		// 1: (3,0)-(3,7) the #now()# interpolation (real expression)
		// 2: (3,7)-(3,12) the "\nbar\n" text buffer (bar rendered output)
		// 3: (3,12)-(5,12) spillover to the </cfoutput> close
		// The blank "\n" buffer after </cfoutput> is NOT tracked.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 10, 1, 15, true ) ); // "\nfoo\n"
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 7, true ) );    // #now()#
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 7, 3, 12, true ) );   // "\nbar\n"
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 12, 5, 11, true ) );  // spillover

		// Pass B: foo text, the interpolation, and bar text all executed once; the
		// spillover and the blank trailing buffer never ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 10 ).stats().count() ).isEqualTo( 1 );  // foo
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
		                <cfset i = 5>
		                <cfset i++>
		                <cfset i++>
		                <cfset i++>
		                <cfthrow message="boom">
		                <cfset i++>
		                <cfset i++>
		                <cfset i++>
		                <cfset i++>
		                <cfset i++>
		                """;
		try {
			runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );
		} catch ( RuntimeException e ) {
			// expected — the template throws mid-way
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for Throw
		// System.out.println( "=== dump Throw" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 10 executable spans — 4 <cfset> before the throw (lines 1-4), the
		// throw itself (line 5), and 5 after (lines 6-10).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 10 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) );  // <cfset i = 5>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 24, true ) );  // <cfthrow message="boom">
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 8, 0, 8, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 11, true ) );  // <cfset i++>
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 11, true ) ); // <cfset i++>

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
		                <cfset foo = "a">
		                <cfset bar = "b">
		                <cfset bum = "c">
		                <cfset quz = "d">
		                <cfset result = foo & bar & bum & quz>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for BinaryChain
		// System.out.println( "=== dump BinaryChain" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 4 <cfset> assignments (one span each) + the chain statement splits
		// into 4 operands: "result = foo & " | "bar & " | "bum & " | "quz>".
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) );  // <cfset foo = "a">
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 17, true ) );  // <cfset bar = "b">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 17, true ) );  // <cfset bum = "c">
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 17, true ) );  // <cfset quz = "d">
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 22, true ) );  // result = foo &
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 22, 5, 28, true ) ); // bar &
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 5, 28, 5, 34, true ) ); // bum &
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 5, 34, 5, 38, true ) ); // quz>

		// Pass B: the whole chain ran, every operand exactly once.
		assertThat( CodeProfilerService.spanAt( key, 5, 0 ).stats().count() ).isEqualTo( 1 );   // result = foo
		assertThat( CodeProfilerService.spanAt( key, 5, 22 ).stats().count() ).isEqualTo( 1 );  // bar
		assertThat( CodeProfilerService.spanAt( key, 5, 28 ).stats().count() ).isEqualTo( 1 );  // bum
		assertThat( CodeProfilerService.spanAt( key, 5, 34 ).stats().count() ).isEqualTo( 1 );  // quz

		assertThat( CodeProfilerService.lineAt( key, 5 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 5 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It stops covering spans after a mid-statement throw (tag)" )
	@Test
	void testBinaryChainWithThrow() {
		String source = """
		                <cfset foo = "a">
		                <cfset bar = "b">
		                <cfset result = foo & ( function() { throw( "boom" ); } )() & bar>
		                """;
		try {
			runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );
		} catch ( RuntimeException e ) {
			// expected — the IIFE throws mid-chain
		}

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for BinaryChainWithThrow
		// System.out.println( "=== dump BinaryChainWithThrow" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: 2 <cfset> assignments + the chain splits into foo / the IIFE
		// (whose body throw is its own span) / bar. 7 spans in source order.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // <cfset foo = "a">
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 17, true ) ); // <cfset bar = "b">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 22, true ) ); // head + foo + " & ("
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 22, 3, 35, true ) ); // closure shell
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 37, 3, 52, true ) ); // throw body
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 3, 52, 3, 62, true ) ); // } )() &
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 3, 62, 3, 66, true ) ); // bar>

		// Pass B: foo ran, the IIFE threw (so it counts as started), bar never ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 1 );  // "foo" ran
		assertThat( CodeProfilerService.spanAt( key, 3, 23 ).stats().count() ).isEqualTo( 1 );  // IIFE started
		assertThat( CodeProfilerService.spanAt( key, 3, 63 ).stats().count() ).isEqualTo( 0 );  // "bar" never ran

		// Line-based: line 3 covered (foo + the started IIFE).
		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It splits a multi-line if into condition, then spans (tag)" )
	@Test
	void testIfBlock() {
		String source = """
		                <cfif true>
		                <cfset a = 1>
		                </cfif>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for IfBlock
		// System.out.println( "=== dump IfBlock" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the <cfif> head (line 1), the then-body <cfset a = 1> (line 2),
		// and the spillover to </cfif> (lines 2-3).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 11, true ) ); // <cfif true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) ); // <cfset a = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 13, 3, 7, true ) );  // spillover to </cfif>

		// Pass B: condition true, so the then-body ran once; the spillover (which
		// includes the executing </cfif> close) also ran.
		assertThat( CodeProfilerService.spanAt( key, 1, 1 ).stats().count() ).isEqualTo( 1 ); // if condition
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 ); // a = 1
		assertThat( CodeProfilerService.spanAt( key, 2, 13 ).stats().count() ).isEqualTo( 1 ); // spillover to </cfif>

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
		                <cfset i = 0>
		                <cfwhile condition="i < 3">
		                <cfset i++>
		                </cfwhile>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for While
		// System.out.println( "=== dump While" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: i=0, the while head, the body i++, and the spillover to </cfwhile>.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) ); // <cfset i = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 27, true ) ); // <cfwhile condition="i < 3">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 11, true ) ); // <cfset i++>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 11, 4, 10, true ) );// spillover to </cfwhile>

		// Pass B: the loop condition ran once (header); body i++ ran 3 times (i:0->1->2).
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 ); // i = 0
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 ); // while head
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 ); // i++ ran 3x
		assertThat( CodeProfilerService.spanAt( key, 3, 11 ).stats().count() ).isEqualTo( 3 ); // spillover executes per iteration

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
		                <cfset i = 0>
		                <cfwhile condition="i < 3">
		                <cfset sleep( 100 )>
		                <cfset i++>
		                </cfwhile>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for WhileTiming
		// System.out.println( "=== dump WhileTiming" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: i=0, the while head, sleep(100), i++, and the </cfwhile> spillover.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 5 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) ); // <cfset i = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 27, true ) ); // <cfwhile condition="i < 3">
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 20, true ) ); // <cfset sleep( 100 )>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 11, true ) ); // <cfset i++>
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 11, 5, 10, true ) );// spillover to </cfwhile>

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
		                <cfset x = 2>
		                <cfswitch expression="#x#">
		                <cfcase value="1">
		                <cfset a = 10>
		                </cfcase>
		                <cfcase value="2">
		                <cfset a = 20>
		                </cfcase>
		                <cfdefaultcase>
		                <cfset a = 30>
		                </cfdefaultcase>
		                </cfswitch>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for Switch
		// System.out.println( "=== dump Switch" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: x=2, then the switch head + each case/default label and body
		// span, ending with the </cfswitch> close. No <cfbreak> needed in tags.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 11 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) );   // <cfset x = 2>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 3, 18, true ) );   // switch head + case 1 label
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 18, 4, 0, true ) );   // case 1 label tail
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 14, true ) );   // a = 10 (case 1 body)
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 4, 14, 6, 18, true ) );  // case 1 close + case 2 label
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 18, 7, 0, true ) );   // case 2 label tail
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 14, true ) );   // a = 20 (case 2 body)
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 7, 14, 9, 15, true ) );  // case 2 close + default label
		assertThat( spanDefs.get( 8 ) ).isEqualTo( new Blueprint.SpanDef( 9, 15, 10, 0, true ) );  // default label tail
		assertThat( spanDefs.get( 9 ) ).isEqualTo( new Blueprint.SpanDef( 10, 0, 10, 14, true ) ); // a = 30 (default body)
		assertThat( spanDefs.get( 10 ) ).isEqualTo( new Blueprint.SpanDef( 10, 14, 12, 11, true ) );// default close + </cfswitch>

		// Pass B: x=2 matches case 2, so case 2 ran (a=20); case 1 and default did
		// not. a=10 (case 1) and a=30 (default) stayed missed.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );    // x = 2
		assertThat( CodeProfilerService.spanAt( key, 2, 1 ).stats().count() ).isEqualTo( 1 );    // switch cond (reached)
		assertThat( CodeProfilerService.spanAt( key, 4, 0 ).stats().count() ).isEqualTo( 0 );    // a = 10 missed (case 1)
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );    // a = 20 ran (case 2)
		assertThat( CodeProfilerService.spanAt( key, 10, 0 ).stats().count() ).isEqualTo( 0 );   // a = 30 missed (default)

		// The spillover/label spans that merely carry execution from the switch
		// head into the eventually-matched case 2 all ran exactly once.
		assertThat( CodeProfilerService.spanAt( key, 3, 18 ).stats().count() ).isEqualTo( 0 );   // case 1 label tail (never entered case 1)
		assertThat( CodeProfilerService.spanAt( key, 4, 14 ).stats().count() ).isEqualTo( 0 );   // case 1 close + case 2 label (not marked)
		assertThat( CodeProfilerService.spanAt( key, 6, 18 ).stats().count() ).isEqualTo( 1 );   // case 2 label tail (matched case 2 label)
		// After case 2's body, the auto-break fires as execution reaches the default
		// label, so the case-2-close + default-label span is entered (count 1) but
		// a=30 never runs.
		assertThat( CodeProfilerService.spanAt( key, 7, 14 ).stats().count() ).isEqualTo( 1 );   // case 2 close + default label (entered, then breaks)
		// After case 2 auto-breaks, execution jumps past the default and the
		// </cfswitch> close, so the default tail + switch-close spillover (line 10
		// col 14) never runs.
		assertThat( CodeProfilerService.spanAt( key, 10, 14 ).stats().count() ).isEqualTo( 0 );

		// Line-based: x=2 (line 1) and switch head (line 2) covered once; the
		// matched case 2 body (line 7) covered once. Case 1 body (line 4), default
		// body (line 10), and the </cfswitch> spillover (line 12) are not covered.
		assertThat( CodeProfilerService.lineAt( key, 1 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 1 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 4 ).covered() ).isFalse();  // case 1 body (a=10) missed
		assertThat( CodeProfilerService.lineAt( key, 7 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 7 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( key, 10 ).covered() ).isFalse(); // default body (a=30) missed
		assertThat( CodeProfilerService.lineAt( key, 12 ).covered() ).isFalse(); // </cfswitch> spillover not reached
	}

	@DisplayName( "It profiles try/catch/finally block statements (tag)" )
	@Test
	void testTry() {
		String source = """
		                <cftry>
		                <cfset a = 1>
		                <cfset sleep( 100 )>
		                <cfset b = 2>
		                <cfcatch>
		                <cfset c = 3>
		                </cfcatch>
		                <cffinally>
		                <cfset d = 4>
		                </cffinally>
		                </cftry>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for Try
		// System.out.println( "=== dump Try" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: the try body's three statements, the catch body, the finally
		// body, and their closing-tag spillovers are each their own span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 7 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) );   // a = 1
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 20, true ) );   // sleep( 100 )
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 13, true ) );   // b = 2
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 13, true ) );   // c = 3 catch
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 6, 13, 7, 10, true ) );  // catch spillover
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 9, 0, 9, 13, true ) );   // d = 4 finally
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 9, 13, 11, 8, true ) );  // finally spillover to </cftry>

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
		                <cfset arr = [ 10, 20, 30 ]>
		                <cfloop array="#arr#" item="x">
		                <cfset y = x>
		                </cfloop>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for ForIn
		// System.out.println( "=== dump ForIn" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: arr=[...] (line 1), the body y = x (line 3), and the spillover to
		// </cfloop> (lines 3-4). The <cfloop> header itself merges into the
		// running span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 3 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 28, true ) ); // <cfset arr = [ 10, 20, 30 ]>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) ); // <cfset y = x>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 13, 4, 9, true ) );// spillover to </cfloop>

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
		                <cfset j = 0>
		                <cfloop from="0" to="2" index="i">
		                <cfset j = j + i>
		                </cfloop>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for ForIndex
		// System.out.println( "=== dump ForIndex" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: j=0, then the body j = j + i splits into left+right spans (the +
		// right operand i may throw), and the spillover to </cfloop>. The <cfloop>
		// header merges into the running span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) ); // <cfset j = 0>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 15, true ) ); // j = j +
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 15, 3, 17, true ) ); // i>
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 17, 4, 9, true ) );// spillover to </cfloop>

		// Pass B: header ran once; body ran 3 times (i: 0,1,2) — both parts of j+j+i.
		assertThat( CodeProfilerService.spanAt( key, 1, 0 ).stats().count() ).isEqualTo( 1 );  // j = 0
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 3 );  // j = j + ran 3x
		assertThat( CodeProfilerService.spanAt( key, 3, 15 ).stats().count() ).isEqualTo( 3 ); // i ran 3x

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
		                <cfset a = false>
		                <cfset b = 1>
		                <cfset x = a && b>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for AndShortCircuit
		// System.out.println( "=== dump AndShortCircuit" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: a=false, b=1, and the x = a && b statement splits into left
		// ("x = a && ") and right ("b>") spans because the right operand may throw.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // <cfset a = false>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) ); // <cfset b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) ); // x = a &&
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 18, true ) ); // b> (right operand)

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
		                <cfset a = true>
		                <cfset b = 1>
		                <cfset x = a && b>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for AndBothRun
		// System.out.println( "=== dump AndBothRun" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: same span layout as testAndShortCircuit; a=true so both run.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 16, true ) ); // <cfset a = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) ); // <cfset b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) ); // x = a &&
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 18, true ) ); // b> (right operand)

		// Pass B: a=true so BOTH operands ran.
		assertThat( CodeProfilerService.spanAt( key, 3, 0 ).stats().count() ).isEqualTo( 1 );  // left ran
		assertThat( CodeProfilerService.spanAt( key, 3, 16 ).stats().count() ).isEqualTo( 1 ); // b ran

		assertThat( CodeProfilerService.lineAt( key, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 3 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It short-circuits || so the right span is missed (tag)" )
	@Test
	void testOrShortCircuit() {
		String source = """
		                <cfset a = true>
		                <cfset b = 1>
		                <cfset x = a || b>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for OrShortCircuit
		// System.out.println( "=== dump OrShortCircuit" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: same span layout; a=true so b short-circuits.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 4 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 16, true ) ); // <cfset a = true>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) ); // <cfset b = 1>
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 16, true ) ); // x = a ||
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 16, 3, 18, true ) ); // b> (right operand)

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
		                <cfset a = 1>
		                <cfset x = "val #a# now">
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for StringInterpolation
		// System.out.println( "=== dump StringInterpolation" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: a=1 and the interpolated string are each one span.
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 2 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 13, true ) ); // <cfset a = 1>
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 25, true ) ); // <cfset x = "val #a# now">

		// Pass B: both ran once.
		assertThat( CodeProfilerService.spanAt( key, 2, 0 ).stats().count() ).isEqualTo( 1 );

		assertThat( CodeProfilerService.lineAt( key, 2 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( key, 2 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles unary not, negation, and parens as single spans (tag)" )
	@Test
	void testUnaryNegateParen() {
		String source = """
		                <cfset a = false>
		                <cfset b = 5>
		                <cfset c = 1>
		                <cfset d = 2>
		                <cfset x = !a>
		                <cfset y = -b>
		                <cfset z = ( c + d )>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for UnaryNegateParen
		// System.out.println( "=== dump UnaryNegateParen" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// Pass A: each assignment is a single span, EXCEPT the paren binary
		// ( c + d ) which splits at the + right operand d (could throw).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 8 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 1, 17, true ) ); // a = false
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) ); // b = 5
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 13, true ) ); // c = 1
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 4, 0, 4, 13, true ) ); // d = 2
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 14, true ) ); // x = !a
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 6, 0, 6, 14, true ) ); // y = -b
		assertThat( spanDefs.get( 6 ) ).isEqualTo( new Blueprint.SpanDef( 7, 0, 7, 17, true ) ); // z = ( c +
		assertThat( spanDefs.get( 7 ) ).isEqualTo( new Blueprint.SpanDef( 7, 17, 7, 21, true ) );// d )>

		// Pass B: each ran once (both parts of the paren binary ran).
		for ( int line = 5; line <= 6; line++ ) {
			assertThat( CodeProfilerService.spanAt( key, line, 0 ).stats().count() ).isEqualTo( 1 );
		}
		assertThat( CodeProfilerService.spanAt( key, 7, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( key, 7, 17 ).stats().count() ).isEqualTo( 1 );

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
		                <cffunction name="foo">
		                <cfset x = 1>
		                <cfreturn x>
		                </cffunction>
		                <cfset result = foo()>
		                """;
		runtime.executeSource( source, new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), BoxSourceType.CFTEMPLATE );

		String	key			= IBoxpiler.MD5( BoxSourceType.CFTEMPLATE.toString() + source );
		// // DEBUG dump for FunctionShellAndBody
		// System.out.println( "=== dump FunctionShellAndBody" );
		// System.out.print( CodeProfilerService.dumpSpans( key, source ) );

		// tag lengths:
		// L1 <cffunction name="foo"> 23
		// L2 <cfset x = 1> 14
		// L3 <cfreturn x> 13
		// L4 </cffunction> 14
		// L5 <cfset result = foo()> 23

		// Pass A: the function shell (line 1), the body x = 1 (line 2), the return
		// (line 3), their tails, and the foo() call site (line 5).
		var		spanDefs	= CodeProfilerService.trackedBlueprints().get( key ).spans();
		assertThat( spanDefs ).hasSize( 6 );
		assertThat( spanDefs.get( 0 ) ).isEqualTo( new Blueprint.SpanDef( 1, 0, 2, 0, true ) );   // function shell
		assertThat( spanDefs.get( 1 ) ).isEqualTo( new Blueprint.SpanDef( 2, 0, 2, 13, true ) );  // x = 1 body
		assertThat( spanDefs.get( 2 ) ).isEqualTo( new Blueprint.SpanDef( 2, 13, 3, 0, true ) );  // x=1 tail
		assertThat( spanDefs.get( 3 ) ).isEqualTo( new Blueprint.SpanDef( 3, 0, 3, 12, true ) );  // return x
		assertThat( spanDefs.get( 4 ) ).isEqualTo( new Blueprint.SpanDef( 3, 12, 4, 13, true ) ); // return tail + </cffunction>
		assertThat( spanDefs.get( 5 ) ).isEqualTo( new Blueprint.SpanDef( 5, 0, 5, 22, true ) ); // foo() call

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

	@DisplayName( "It profiles a CFML tag file on disk keyed by its file path" )
	@Test
	void testDiskFile() {
		String relativePath = "src/test/resources/profiler/ProfilerSample.cfm";
		runtime.executeTemplate( relativePath );

		// The blueprint is keyed by the NORMALIZED absolute file path (what
		// registerBlueprintForFile stores), not a source hash.
		Path	absolute	= Paths.get( relativePath ).toAbsolutePath().normalize();
		String	fileKey		= absolute.toString().toLowerCase( Locale.ROOT );
		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( fileKey );

		var blueprint = CodeProfilerService.trackedBlueprints().get( fileKey );
		assertThat( blueprint.spans() ).isNotEmpty();

		// High-level line coverage of the CFML tag file (comment/blank lines shift
		// the executable lines; see ProfilerSample.cfm):
		// 3-4 : i = 0, j = 0 (ran once each)
		// 7 : <cfwhile condition="i < 3"> (header ran once)
		// 8 : i++ (ran 3x)
		// 16 : doubled = doubleIt( 21 ) (ran once)
		// 20 : j = 1 (if taken) (ran once)
		// 22 : j = 2 (else, not taken) (MISSED)
		// 26 : k = j EQ 1 ? (ternary cond ran once); "small" (untaken) missed
		assertThat( CodeProfilerService.lineAt( fileKey, 3 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 3 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 4 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 4 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 8 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 8 ).count() ).isEqualTo( 3 );   // i++ ran 3x
		assertThat( CodeProfilerService.lineAt( fileKey, 20 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 20 ).count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.lineAt( fileKey, 22 ).covered() ).isFalse();     // else branch missed
		assertThat( CodeProfilerService.lineAt( fileKey, 26 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 26 ).count() ).isEqualTo( 1 );
	}

	@DisplayName( "It profiles a CFML tag class file on disk keyed by its file path" )
	@Test
	void testDiskClassFile() {
		String		relativePath	= "src/test/resources/profiler/ProfilerComplexTag.cfc";

		// One shared request context so the SAME variables scope (and therefore the
		// SAME class instance) persists across every incremental executeSource call.
		IBoxContext	context			= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Instantiate the on-disk tag class via a CFSCRIPT new. Compiling registers
		// the blueprint (Pass A); instantiation runs the pseudo-constructor.
		runtime.executeSource( "pc = new src.test.resources.profiler.ProfilerComplexTag();", context, BoxSourceType.CFSCRIPT );

		// Same file-path keying: normalized absolute path.
		Path	absolute	= Paths.get( relativePath ).toAbsolutePath().normalize();
		String	fileKey		= absolute.toString().toLowerCase( Locale.ROOT );
		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( fileKey );

		var blueprint = CodeProfilerService.trackedBlueprints().get( fileKey );
		assertThat( blueprint.spans() ).isNotEmpty();

		// High-level line coverage of the tag class file (comment/blank lines shift
		// the executable lines; see ProfilerComplexTag.cfc):
		// 6 : <cfproperty name="threshold" default="40"> (property default ran at class load)
		// 8 : instanceInit = 0 (pseudo-constructor ran at instantiation)
		assertThat( CodeProfilerService.lineAt( fileKey, 8 ).covered() ).isTrue();   // pseudo-constructor body
		assertThat( CodeProfilerService.lineAt( fileKey, 8 ).count() ).isEqualTo( 1 );
		// The property default (line 6, col 38) ran at class load, count 1.
		assertThat( CodeProfilerService.spanAt( fileKey, 6, 38 ).stats().count() ).isEqualTo( 1 );  // property default ran

		// No method has been invoked yet, so the member function body statements
		// (lines 17-18: sleep/return) have NOT run — count 0.
		assertThat( CodeProfilerService.spanAt( fileKey, 17, 2 ).stats().count() ).isEqualTo( 0 );  // member body missed
		assertThat( CodeProfilerService.lineAt( fileKey, 17 ).covered() ).isFalse();

		// Now invoke the member method on the SAME instance in a second script (the
		// shared context keeps pc in the variables scope); its body should run.
		runtime.executeSource( "result = pc.member( 5 );", context, BoxSourceType.CFSCRIPT );

		// The member body slept ~100ms, so its span is covered with the sleep charged.
		assertThat( CodeProfilerService.spanAt( fileKey, 17, 2 ).stats().count() ).isEqualTo( 1 );  // sleep ran
		assertThat( CodeProfilerService.spanAt( fileKey, 17, 2 ).stats().totalNanos() ).isAtLeast( 100L * 1_000_000L );
		assertThat( CodeProfilerService.spanAt( fileKey, 17, 2 ).stats().totalNanos() ).isAtMost( 400L * 1_000_000L );
		assertThat( CodeProfilerService.lineAt( fileKey, 17 ).covered() ).isTrue();
		assertThat( CodeProfilerService.lineAt( fileKey, 17 ).count() ).isEqualTo( 1 );
	}
}
