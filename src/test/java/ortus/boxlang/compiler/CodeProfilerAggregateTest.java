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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * Aggregate / stress tests for the code-profiler SPAN + INCLUSIVE-timing model:
 * deep call chains, loops, recursion, and cross-class call graphs. Each level's
 * execution COUNT and inclusive TIME is asserted, so the per-call brackets are
 * proven to compose correctly under real nesting.
 * <p>
 * Inclusive time answers "how long was this call on the stack". Because an outer
 * call brackets an inner call, the inclusive ladder must be monotonic: an enclosing
 * call's inclusive time is at least the enclosed call's inclusive time plus its own
 * work.
 */
// Profiling instrumentation is ASM-boxpiler-specific; skip under the Java boxpiler.
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class CodeProfilerAggregateTest {

	private BoxRuntime			runtime;
	private CodeProfilerService	service;
	private boolean				prevProfiler;

	@BeforeEach
	void setup() {
		this.runtime										= BoxRuntime.getInstance();
		this.service										= this.runtime.getCodeProfilerService();
		this.prevProfiler									= this.runtime.getConfiguration().codeProfilerEnabled;
		this.runtime.getConfiguration().codeProfilerEnabled	= true;
		// Source is cached in the boxpiler pool; clear BOTH so it recompiles with
		// instrumentation enabled.
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
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

	/** Key for adhoc BOXSCRIPT source (matches what the boxpiler registered). */
	private String keyFor( String source ) {
		return IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );
	}

	/** Assert a line's inclusive call time falls within [min, max] nanoseconds. */
	private void assertLineInclusive( String key, int line, long minNanos, long maxNanos ) {
		CodeProfilerService.LineCoverage lc = service.lineAt( key, line );
		assertThat( lc ).isNotNull();
		assertThat( lc.nanos() ).isAtLeast( minNanos );
		assertThat( lc.nanos() ).isAtMost( maxNanos );
	}

	/** Assert a line's execution count (first/outermost span). */
	private void assertLineCount( String key, int line, long expected ) {
		CodeProfilerService.LineCoverage lc = service.lineAt( key, line );
		assertThat( lc ).isNotNull();
		assertThat( lc.count() ).isEqualTo( expected );
	}

	/** Assert the inclusive time of an OUTER line is at least that of an INNER line (jitter-tolerant). */
	private void assertInclusiveAtLeast( String key, int outerLine, int innerLine ) {
		long	outer	= service.lineAt( key, outerLine ).nanos();
		long	inner	= service.lineAt( key, innerLine ).nanos();
		assertThat( outer ).isAtLeast( inner - 10L * 1_000_000L );
	}

	// -------------------------------------------------------------------------
	// 1. Ten-deep call chain: inclusive time forms an increasing ladder
	// -------------------------------------------------------------------------

	@DisplayName( "It builds a monotonic inclusive ladder across a 10-deep call chain" )
	@Test
	void testTenDeepCallChainInclusiveLadder() {
		// Each function sleeps 20ms BEFORE calling the next; so the inclusive time of
		// the call to f_i = sum of sleeps from f_i down to f10 = (depth - i + 1) * 20ms.
		// Each function block is exactly 4 lines (decl, sleep, call-or-blank, close), so
		// the call to f_i sits on a computed line. This proves inclusive brackets
		// COMPOSE across ten nested frames.
		int				depth		= 10;
		int				perLevelMs	= 20;
		StringBuilder	sb			= new StringBuilder();
		for ( int i = depth; i >= 1; i-- ) {
			sb.append( "function f" ).append( i ).append( "() {\n" );
			sb.append( "    sleep( " ).append( perLevelMs ).append( " );\n" );
			if ( i < depth ) {
				sb.append( "    f" ).append( i + 1 ).append( "();\n" );
			} else {
				sb.append( "\n" );
			}
			sb.append( "}\n" );
		}
		sb.append( "f1();\n" );
		String source = sb.toString();
		runtime.executeSource( source );
		String key = keyFor( source );

		// Deepest sleep span (f10 body) ran once.
		assertLineCount( key, 2, 1 );

		// Walk the inclusive ladder from the deepest call (to f10) up to f2.
		long previousInclusive = 0L;
		// The call in block i invokes f_(i+1); it sits on line (depth - i) * 4 + 3 and
		// encloses the sleeps from f_(i+1) down to f10 => (depth - i) * perLevelMs.
		for ( int i = depth - 1; i >= 1; i-- ) {
			int		callLine	= ( depth - i ) * 4 + 3;
			long	expected	= ( long ) ( depth - i ) * perLevelMs * 1_000_000L;

			// Each call site executed exactly once.
			assertLineCount( key, callLine, 1 );

			// Inclusive time grows as we enclose more levels (generous band for jitter).
			long actual = service.lineAt( key, callLine ).nanos();
			assertThat( actual ).isAtLeast( ( long ) ( expected * 0.5 ) );
			assertThat( actual ).isAtMost( expected * 2 + 100L * 1_000_000L );

			// Monotonic: the enclosing call is at least as long as the enclosed one
			// (allow a small negative tolerance for scheduler jitter on tiny sleeps).
			assertThat( actual ).isAtLeast( previousInclusive - 20L * 1_000_000L );
			previousInclusive = actual;
		}

		// The TOP-LEVEL call `f1();` encloses all ten levels => ~200ms.
		assertLineCount( key, 41, 1 );
		assertLineInclusive( key, 41, 120L * 1_000_000L, 900L * 1_000_000L );
	}

	// -------------------------------------------------------------------------
	// 2. Loop calling a UDF: count + inclusive accumulate per iteration
	// -------------------------------------------------------------------------

	@DisplayName( "It accumulates a loop's per-iteration call count and inclusive time" )
	@Test
	void testLoopCallingUdfAccumulates() {
		String source = """
		                function work() {
		                    sleep( 20 );
		                }
		                total = 0;
		                for( i = 1; i <= 5; i++ ) {
		                    work();
		                }
		                """;
		runtime.executeSource( source );
		String key = keyFor( source );

		// The call site (line 6) ran once per iteration -> count 5; inclusive time is
		// the SUM of all five invocations (~100ms).
		assertLineCount( key, 6, 5 );
		assertLineInclusive( key, 6, 100L * 1_000_000L, 500L * 1_000_000L );

		// The callee body statement (line 2) ran 5x.
		assertLineCount( key, 2, 5 );
	}

	// -------------------------------------------------------------------------
	// 3. Nested loops calling a UDF: counts multiply, inclusive accumulates
	// -------------------------------------------------------------------------

	@DisplayName( "It multiplies call counts across nested loops and accumulates inclusive time" )
	@Test
	void testNestedLoopsCallingUdf() {
		String source = """
		                function work() {
		                    sleep( 10 );
		                }
		                for( i = 1; i <= 3; i++ ) {
		                    for( j = 1; j <= 2; j++ ) {
		                        work();
		                    }
		                }
		                """;
		runtime.executeSource( source );
		String key = keyFor( source );

		// 3 outer x 2 inner = 6 invocations of work().
		assertLineCount( key, 6, 6 );
		assertLineInclusive( key, 6, 60L * 1_000_000L, 400L * 1_000_000L );
		assertLineCount( key, 2, 6 );
	}

	// -------------------------------------------------------------------------
	// 4. Recursion: per-depth count + accumulated inclusive time
	// -------------------------------------------------------------------------

	@DisplayName( "It records recursive call counts and accumulated inclusive time" )
	@Test
	void testRecursionCountsAndInclusive() {
		String source = """
		                function rec( n ) {
		                    sleep( 20 );
		                    if( n <= 0 ) {
		                        return 0;
		                    }
		                    rec( n - 1 );
		                }
		                rec( 4 );
		                """;
		runtime.executeSource( source );
		String key = keyFor( source );

		// rec(4)->rec(3)->rec(2)->rec(1)->rec(0): the body ran 5x.
		assertLineCount( key, 2, 5 );

		// The recursive STATEMENT call site (line 6) executed 4x (n = 4,3,2,1) and its
		// inclusive time accumulates across those four invocations (~80ms).
		assertLineCount( key, 6, 4 );
		assertLineInclusive( key, 6, 80L * 1_000_000L, 600L * 1_000_000L );

		// The initial call (line 8) encloses the whole recursion (~100ms).
		assertLineCount( key, 8, 1 );
		assertLineInclusive( key, 8, 100L * 1_000_000L, 700L * 1_000_000L );
	}

	// -------------------------------------------------------------------------
	// 5. Cross-class call graph: inclusive time spans class boundaries
	// -------------------------------------------------------------------------

	@DisplayName( "It follows inclusive time across a cross-class call graph" )
	@Test
	void testCrossClassCallGraph() {
		String source = """
		                class Alpha {
		                    function outer( target ) {
		                        sleep( 10 );
		                        target.inner( this );
		                    }
		                    function callback() {
		                        sleep( 30 );
		                    }
		                }
		                class Beta {
		                    function inner( caller ) {
		                        caller.callback();
		                    }
		                }
		                objA = new Alpha();
		                objB = new Beta();
		                objA.outer( objB );
		                """;
		runtime.executeSource( source );
		String key = keyFor( source );

		// objA.outer(objB) -> target.inner(this) -> caller.callback() -> sleep(30).
		// The top call (line 17) encloses ~40ms (Alpha.outer's 10ms + nested 30ms).
		assertLineCount( key, 17, 1 );
		assertLineInclusive( key, 17, 40L * 1_000_000L, 400L * 1_000_000L );

		// target.inner(this) inside Alpha.outer (line 4) encloses callback()'s ~30ms.
		assertLineCount( key, 4, 1 );
		assertLineInclusive( key, 4, 30L * 1_000_000L, 300L * 1_000_000L );

		// caller.callback() inside Beta.inner (line 12) encloses the 30ms sleep.
		assertLineCount( key, 12, 1 );
		assertLineInclusive( key, 12, 30L * 1_000_000L, 300L * 1_000_000L );

		// The sleep span (line 7) ran once.
		assertLineCount( key, 7, 1 );

		// Cross-boundary inclusive must EXCEED the leaf call's inclusive (encloses it).
		assertInclusiveAtLeast( key, 17, 12 );
	}

	// -------------------------------------------------------------------------
	// 6. Loop of a deep chain: multiply counts, keep the inclusive ladder
	// -------------------------------------------------------------------------

	@DisplayName( "It keeps counts and inclusive ladder correct for a loop around a call chain" )
	@Test
	void testLoopAroundCallChain() {
		String source = """
		                function f3() {
		                    sleep( 20 );
		                }
		                function f2() {
		                    f3();
		                }
		                function f1() {
		                    f2();
		                }
		                for( i = 1; i <= 3; i++ ) {
		                    f1();
		                }
		                """;
		runtime.executeSource( source );
		String key = keyFor( source );

		// f1 is called 3x from the loop; each call cascades through f2 -> f3.
		assertLineCount( key, 11, 3 );  // f1() inside the loop
		assertLineInclusive( key, 11, 60L * 1_000_000L, 400L * 1_000_000L );

		// f2() (line 5) and f3() (line 8) each ran 3x too.
		assertLineCount( key, 5, 3 );
		assertLineCount( key, 8, 3 );

		// The deepest sleep (line 2) ran 3x (once per loop iteration).
		assertLineCount( key, 2, 3 );

		// Each level of the chain accumulated the same three 20ms sleeps (~60ms).
		assertLineInclusive( key, 11, 60L * 1_000_000L, 400L * 1_000_000L );
		assertLineInclusive( key, 5, 60L * 1_000_000L, 400L * 1_000_000L );
		assertLineInclusive( key, 8, 60L * 1_000_000L, 400L * 1_000_000L );
	}

	// -------------------------------------------------------------------------
	// 7. Many sibling calls on one line: count + inclusive sum
	// -------------------------------------------------------------------------

	@DisplayName( "It counts the calls on a single line" )
	@Test
	void testManySiblingCallsOneLine() {
		String source = """
		                function tick() {
		                    sleep( 10 );
		                }
		                a = tick() + tick() + tick() + tick();
		                """;
		runtime.executeSource( source );
		String key = keyFor( source );

		// Four calls on line 4; the callee body ran 4x.
		assertLineCount( key, 2, 4 );
		// The statement line ran once (count = first/outermost span on the line).
		assertLineCount( key, 4, 1 );

		// ALL FOUR calls must be bracketed for inclusive timing — including the FIRST
		// one, which sits inside the leading span `a = tick() + ` (starting at `a`, not
		// at the call). Before the containing-span fallback only 3 of the 4 calls were
		// bracketed, so the line reported ~3 sleeps. Require ~4 sleeps: a 3-call line
		// (~30ms) must fail.
		long lineNanos = service.lineAt( key, 4 ).nanos();
		assertThat( lineNanos ).isAtLeast( 35L * 1_000_000L );
		assertThat( lineNanos ).isAtMost( 80L * 1_000_000L );
	}

	// -------------------------------------------------------------------------
	// 8. Same-line nested call args must NOT double count the line's time
	// -------------------------------------------------------------------------

	@DisplayName( "It does not double count same-line nested call arguments" )
	@Test
	void testSameLineNestedArgsNotDoubleCounted() {
		// getData1()=100ms, getData2()=200ms, processData body=100ms. The argument calls
		// run INSIDE processData's bracket, on the SAME line, so the line's on-stack time
		// must be processData's inclusive (~400ms) — NOT the sum of the args PLUS the
		// call (~700ms). The arg spans' time is already inside the call's inclusive.
		String source = """
		                getData1 = ()->sleep(100);
		                getData2 = ()->sleep(200);
		                processData = (a, b)->sleep(100)
		                processData( getData1(), getData2() );
		                """;
		runtime.executeSource( source );
		String	key			= keyFor( source );

		long	lineNanos	= service.lineAt( key, 4 ).nanos();
		// ~400ms (the whole call), definitely not ~700ms (double-counted).
		assertThat( lineNanos ).isAtLeast( 400L * 1_000_000L );
		assertThat( lineNanos ).isAtMost( 600L * 1_000_000L );
	}

	// -------------------------------------------------------------------------
	// 9. Abort/throw through a deep chain keeps inclusive timing on every frame
	// -------------------------------------------------------------------------

	@DisplayName( "It records inclusive time on each frame when a deep chain throws" )
	@Test
	void testDeepChainThrowKeepsInclusive() {
		// f3 sleeps then throws; f2 calls f3; f1 calls f2; the top-level call is
		// uncaught. Each frame's unwind must still charge inclusive time to its
		// call-site span.
		String source = """
		                function f3() {
		                    sleep( 50 );
		                    throw( "boom" );
		                }
		                function f2() {
		                    f3();
		                }
		                function f1() {
		                    f2();
		                }
		                f1();
		                """;
		try {
			runtime.executeSource( source );
		} catch ( RuntimeException e ) {
			// expected — the throw propagates out of the script
		}
		String key = keyFor( source );

		// f3() call inside f2 (line 6) inclusive ~50ms despite the throw.
		assertLineInclusive( key, 6, 50L * 1_000_000L, 400L * 1_000_000L );
		// f2() call inside f1 (line 9) inclusive ~50ms.
		assertLineInclusive( key, 9, 50L * 1_000_000L, 400L * 1_000_000L );
		// Top-level f1() (line 11) inclusive ~50ms.
		assertLineInclusive( key, 11, 50L * 1_000_000L, 400L * 1_000_000L );

		// Monotonic across the unwound chain.
		assertInclusiveAtLeast( key, 11, 9 );
		assertInclusiveAtLeast( key, 9, 6 );
	}

	// -------------------------------------------------------------------------
	// 10. The call SHELL: `)` carries the SAME inclusive time as the head `callee(`
	// -------------------------------------------------------------------------

	@DisplayName( "It gives the closing ) the SAME on-stack time as the call head" )
	@Test
	void testCallCloseParenSharesHeadInclusiveTime() {
		// The shell around the args — `processData(` and `)` — is ONE atomic unit: it
		// runs exactly when the call runs. So the `)` span must report the SAME
		// on-stack (inclusive) time as the head `processData(` span — NOT a separate,
		// near-zero probe time. Between the head and the `)` the body sleeps ~100ms.
		String source = """
		                getData1 = ()->sleep(100);
		                getData2 = ()->sleep(200);
		                processData = (a, b)->sleep(100)
		                processData( getData1(), getData2() );
		                """;
		runtime.executeSource( source );
		String						key		= keyFor( source );

		// Find the head (`processData(`) and tail (`)`) spans ON LINE 4.
		CodeProfilerService.Span	head	= null;
		CodeProfilerService.Span	tail	= null;
		for ( CodeProfilerService.Span s : service.fileSpans( key ) ) {
			if ( s.startLine() != 4 ) {
				continue;
			}
			String text = service.spanSourceText( key, source, s );
			if ( "processData(".equals( text ) ) {
				head = s;
			} else if ( ")".equals( text ) ) {
				tail = s;
			}
		}
		assertThat( head ).isNotNull();
		assertThat( tail ).isNotNull();

		// The shell's inclusive time: the head is on the stack the whole call (~400ms).
		assertThat( head.stats().onStackNanos() ).isAtLeast( 400L * 1_000_000L );
		// THE INVARIANT: the `)` shares that time EXACTLY (same atomic shell).
		assertThat( tail.stats().onStackNanos() ).isEqualTo( head.stats().onStackNanos() );
	}

	// -------------------------------------------------------------------------
	// 11. Oversized switch: the chunked fallback still reports the executed case
	// -------------------------------------------------------------------------

	@DisplayName( "An OVERSIZED switch's chunked fallback still reports the executed case" )
	@Test
	void testOversizedSwitchReportsExecutedCase() {
		// A switch too large for one method falls back to per-chunk helper methods.
		// The inline pass runs FIRST and claims its marks, then its bytecode is
		// DISCARDED when the size check trips; the chunked copy re-transforms the same
		// nodes. Without restoring the mark state, the chunked copy emits no marks and
		// the executed case reports as MISSED. The chunked path must also emit the case
		// LABEL mark (the inline path does), or the label reads as missed too.
		int				caseCount		= 300;
		int				paddingPerCase	= 40;
		StringBuilder	sb				= new StringBuilder();
		sb.append( "result = 0;\n" );
		int case2LabelLine = -1, case2BodyLine = -1;
		sb.append( "switch( 2 ) {\n" );
		for ( int i = 0; i < caseCount; i++ ) {
			if ( i == 2 ) {
				case2LabelLine = sb.toString().split( "\n", -1 ).length;
			}
			sb.append( "case " ).append( i ).append( ":\n" );
			for ( int j = 0; j < paddingPerCase; j++ ) {
				sb.append( "  noise" ).append( j ).append( " = 'pad-" ).append( i ).append( "-" ).append( j ).append( "';\n" );
			}
			if ( i == 2 ) {
				case2BodyLine = sb.toString().split( "\n", -1 ).length;
			}
			sb.append( "  result = " ).append( i ).append( ";\n" );
			sb.append( "  break;\n" );
		}
		sb.append( "default:\n" );
		sb.append( "  result = -1;\n" );
		sb.append( "}\n" );

		String source = sb.toString();
		runtime.executeSource( source );
		String key = keyFor( source );

		// The chunked fallback was taken (the inline pass would have fit far fewer spans).
		assertThat( service.fileSpans( key ).size() ).isGreaterThan( 5000 );

		// The EXECUTED case (2) must be reported: its label line AND its body line.
		assertThat( service.lineAt( key, case2LabelLine ).covered() ).isTrue();
		assertThat( service.lineAt( key, case2BodyLine ).covered() ).isTrue();
		assertThat( service.lineAt( key, case2BodyLine ).count() ).isEqualTo( 1 );
	}
}
