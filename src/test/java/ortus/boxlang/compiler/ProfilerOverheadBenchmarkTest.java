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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * Measures the RUNTIME overhead of code-profiler instrumentation at scale.
 * <p>
 * This is a developer-facing benchmark, not an assertion test. It runs the same
 * script many times with capture OFF and then with capture ON (marks emitted +
 * recorded) and prints the wall-clock difference. The fixed per-run cost shared by
 * both paths (context creation, class lookup, dispatch through
 * {@code executeSource}) cancels in the DIFFERENCE, so the reported delta is the
 * instrumentation cost itself.
 * <p>
 * Tagged {@code benchmark} so it is skipped in normal runs; run explicitly to
 * regenerate the overhead report.
 */
@Tag( "benchmark" )
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
public class ProfilerOverheadBenchmarkTest {

	private BoxRuntime			runtime;
	private CodeProfilerService	service;
	private boolean				prevProfiler;

	@BeforeEach
	void setup() {
		this.runtime		= BoxRuntime.getInstance();
		this.service		= this.runtime.getCodeProfilerService();
		this.prevProfiler	= this.runtime.getConfiguration().codeProfilerEnabled;
	}

	@AfterEach
	void teardown() {
		this.runtime.getConfiguration().codeProfilerEnabled = this.prevProfiler;
		this.service.setActive( false );
		this.service.reset();
	}

	private void clearPools() {
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
	}

	/** Force a fresh compile of {@code source} under the current profiler flag and cache it. */
	private void warm( String source ) {
		clearPools();
		this.runtime.executeSource( source );
	}

	/** One measured run: elapsed nanos + the number of span marks recorded during it. */
	private record Rep( long nanos, long marks ) {
	}

	private Rep runRep( String source, int iters ) {
		long	m0		= totalMarked();
		long	start	= System.nanoTime();
		for ( int i = 0; i < iters; i++ ) {
			this.runtime.executeSource( source );
		}
		long nanos = System.nanoTime() - start;
		return new Rep( nanos, totalMarked() - m0 );
	}

	/** Best-of-3 rep (the least GC/JIT-perturbed), for stable timing. */
	private Rep bestRep( String source, int iters ) {
		Rep best = null;
		for ( int i = 0; i < 3; i++ ) {
			Rep r = runRep( source, iters );
			if ( best == null || r.nanos() < best.nanos() ) {
				best = r;
			}
		}
		return best;
	}

	/**
	 * A/B the BASE instrumentation (span marks only) against the NEW instrumentation
	 * (span marks + the additive inclusive call-site brackets). BOTH are profiler-on
	 * and both record into the service, so the only difference is the brackets — the
	 * delta is the MARGINAL cost of the inclusive-timing feature.
	 */
	private void report( String label, String source, int warmup, int iters ) {
		// ---- BASE: profiler on, inclusive brackets DISABLED ----
		ortus.boxlang.compiler.asmboxpiler.Transpiler.setInclusiveTimingEnabled( false );
		this.runtime.getConfiguration().codeProfilerEnabled = true;
		this.service.setActive( true );
		this.service.reset();
		warm( source );
		runRep( source, warmup );
		Rep		base	= bestRep( source, iters );
		double	baseMs	= base.nanos() / 1_000_000.0;

		// ---- NEW: profiler on, inclusive brackets ENABLED ----
		ortus.boxlang.compiler.asmboxpiler.Transpiler.setInclusiveTimingEnabled( true );
		this.service.reset();
		warm( source );
		runRep( source, warmup );
		Rep		incl	= bestRep( source, iters );
		double	inclMs	= incl.nanos() / 1_000_000.0;

		long	marks	= incl.marks();
		long	spans	= totalSpans();
		this.service.reset();
		ortus.boxlang.compiler.asmboxpiler.Transpiler.setInclusiveTimingEnabled( true );

		double	perRunBaseUs	= base.nanos() / 1_000.0 / iters;
		double	perRunInclUs	= incl.nanos() / 1_000.0 / iters;
		double	pct				= base.nanos() == 0 ? 0 : ( ( double ) incl.nanos() / base.nanos() - 1.0 ) * 100.0;
		double	nsPerMark		= marks == 0 ? 0 : ( incl.nanos() - base.nanos() ) / ( double ) marks;

		String	block			= String.format(
		    "%n=== %s (%d iters) ===%n" +
		        "  base instr (marks only)      : %10.1f ms total  (%8.2f us/run)%n" +
		        "  new instr (marks + inclusive): %10.1f ms total  (%8.2f us/run)%n" +
		        "  MARGINAL FEATURE COST        : %+.1f%%  (%+.2f us/run)%n" +
		        "  spans=%-6d marks=%-8d  incl-cost/mark=%.1f ns%n",
		    label, iters, baseMs, perRunBaseUs, inclMs, perRunInclUs, pct, perRunInclUs - perRunBaseUs,
		    spans, marks, nsPerMark
		);
		System.out.print( block );
		appendReport( block );
	}

	/** Sum of every span's execution count across all tracked blueprints (marks recorded). */
	private long totalMarked() {
		long total = 0;
		for ( String key : this.service.trackedBlueprints().keySet() ) {
			for ( CodeProfilerService.Span s : this.service.fileSpans( key ) ) {
				total += s.stats().count();
			}
		}
		return total;
	}

	/** Total number of executable spans across all tracked blueprints. */
	private long totalSpans() {
		long total = 0;
		for ( String key : this.service.trackedBlueprints().keySet() ) {
			total += this.service.fileSpans( key ).size();
		}
		return total;
	}

	/** Append a benchmark block to the shared report file so results survive the test run. */
	private static synchronized void appendReport( String block ) {
		try {
			java.nio.file.Path out = java.nio.file.Paths.get( "build", "profiler-overhead-benchmark.txt" );
			java.nio.file.Files.createDirectories( out.getParent() );
			java.nio.file.Files.writeString( out, block, java.nio.charset.StandardCharsets.UTF_8,
			    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND );
		} catch ( java.io.IOException e ) {
			System.out.println( "Failed to write benchmark report: " + e.getMessage() );
		}
	}

	@DisplayName( "Benchmark: instrumentation overhead (tight arithmetic loop)" )
	@Test
	void benchmarkTightLoop() {
		String source = """
		                total = 0;
		                for( i = 1; i <= 50; i++ ) {
		                    total = total + i;
		                }
		                """;
		report( "tight loop (50 iters, no UDF)", source, 20_000, 60_000 );
	}

	@DisplayName( "Benchmark: instrumentation overhead (UDF call in a loop)" )
	@Test
	void benchmarkUDFLoop() {
		String source = """
		                function add( a, b ) {
		                    return a + b;
		                }
		                total = 0;
		                for( i = 1; i <= 50; i++ ) {
		                    total = add( total, i );
		                }
		                """;
		report( "UDF call loop (50 iters)", source, 10_000, 30_000 );
	}

	@DisplayName( "Benchmark: instrumentation overhead (call-heavy)" )
	@Test
	void benchmarkCallHeavy() {
		String source = """
		                function tick( n ) {
		                    return n + 1;
		                }
		                x = 0;
		                x = tick( x );
		                x = tick( x );
		                x = tick( x );
		                x = tick( x );
		                x = tick( x );
		                """;
		report( "call-heavy (5 UDF calls)", source, 20_000, 60_000 );
	}

	// -------------------------------------------------------------------------
	// NO-OP overhead: profiling ENABLED (marks compiled in) but NEVER ACTIVATED
	// -------------------------------------------------------------------------

	/**
	 * Measure the cost of leaving profiling ENABLED while the capture service is
	 * NEVER activated — i.e. the class files are instrumented (every `mark` /
	 * `markEnter` / `markExit` call is baked into the bytecode) but every one of
	 * those calls short-circuits to a no-op at runtime.
	 * <p>
	 * A/B: BOTH arms run with the profiler capture INACTIVE, so neither records;
	 * the ONLY difference is whether the class files carry the mark bytecode:
	 * <ul>
	 * <li><b>ARM A</b> ({@code codeProfilerEnabled=false}) — compiled WITHOUT any
	 * mark calls. Pure, uninstrumented execution.</li>
	 * <li><b>ARM B</b> ({@code codeProfilerEnabled=true}) — compiled WITH mark
	 * calls (flat marks + inclusive brackets), but {@code service.active=false}, so
	 * every mark is a no-op.</li>
	 * </ul>
	 * The delta is the total cost of the no-op mark calls: one static invoke plus a
	 * volatile field read and an early {@code return} per mark site. The number of
	 * marks that DID NOTHING is measured separately (one short capture-on run) so the
	 * report can give a per-mark nanosecond cost.
	 */
	private void reportNoOpOverhead( String label, String source, int warmup, int iters ) {
		// ---- ARM A: UNINSTRUMENTED (no mark bytecode at all) ----
		this.runtime.getConfiguration().codeProfilerEnabled = false;
		this.service.setActive( false );
		ortus.boxlang.compiler.asmboxpiler.Transpiler.setInclusiveTimingEnabled( true );
		this.service.reset();
		warm( source );
		runRep( source, warmup );
		Rep		baseline	= bestRep( source, iters );
		double	baseMs		= baseline.nanos() / 1_000_000.0;

		// ---- ARM B: INSTRUMENTED but INACTIVE (marks present, all no-ops) ----
		this.runtime.getConfiguration().codeProfilerEnabled = true;
		this.service.setActive( false ); // NEVER activated — marks must no-op
		this.service.reset();
		warm( source );
		runRep( source, warmup );
		Rep		noop	= bestRep( source, iters );
		double	noopMs	= noop.nanos() / 1_000_000.0;

		// Count the marks that did nothing: one short ACTIVE run over the SAME
		// instrumented class (already compiled with marks), summed across spans.
		this.service.reset();
		this.service.setActive( true );
		long marksPerRun = runRep( source, iters ).marks() / iters;
		this.service.setActive( false );

		double	perRunBaseUs	= baseline.nanos() / 1_000.0 / iters;
		double	perRunNoopUs	= noop.nanos() / 1_000.0 / iters;
		double	pct				= baseline.nanos() == 0 ? 0 : ( ( double ) noop.nanos() / baseline.nanos() - 1.0 ) * 100.0;
		double	nsPerMark		= marksPerRun == 0 ? 0 : ( noop.nanos() - baseline.nanos() ) / ( double ) ( marksPerRun * iters );

		String	block			= String.format(
		    "%n=== %s (%d iters) [NO-OP: enabled but never activated] ===%n" +
		        "  ARM A  uninstrumented (no marks) : %10.1f ms total  (%8.2f us/run)%n" +
		        "  ARM B  instrumented, INACTIVE   : %10.1f ms total  (%8.2f us/run)%n" +
		        "  NO-OP OVERHEAD                  : %+.1f%%  (%+.3f us/run)%n" +
		        "  marks/run=%-8d  cost per no-op mark=%.2f ns%n",
		    label, iters, baseMs, perRunBaseUs, noopMs, perRunNoopUs, pct, perRunNoopUs - perRunBaseUs, marksPerRun, nsPerMark
		);
		System.out.print( block );
		appendReport( block );

		// Leave the service inactive and the config off, as found.
		this.service.setActive( false );
		this.runtime.getConfiguration().codeProfilerEnabled = false;
	}

	@DisplayName( "Benchmark: no-op overhead (tight arithmetic loop)" )
	@Test
	void benchmarkNoOpTightLoop() {
		String source = """
		                total = 0;
		                for( i = 1; i <= 50; i++ ) {
		                    total = total + i;
		                }
		                """;
		reportNoOpOverhead( "no-op tight loop (50 iters, no UDF)", source, 20_000, 60_000 );
	}

	@DisplayName( "Benchmark: no-op overhead (UDF call in a loop)" )
	@Test
	void benchmarkNoOpUDFLoop() {
		String source = """
		                function add( a, b ) {
		                    return a + b;
		                }
		                total = 0;
		                for( i = 1; i <= 50; i++ ) {
		                    total = add( total, i );
		                }
		                """;
		reportNoOpOverhead( "no-op UDF call loop (50 iters)", source, 10_000, 30_000 );
	}

	@DisplayName( "Benchmark: no-op overhead (call-heavy)" )
	@Test
	void benchmarkNoOpCallHeavy() {
		String source = """
		                function tick( n ) {
		                    return n + 1;
		                }
		                x = 0;
		                x = tick( x );
		                x = tick( x );
		                x = tick( x );
		                x = tick( x );
		                x = tick( x );
		                """;
		reportNoOpOverhead( "no-op call-heavy (5 UDF calls)", source, 20_000, 60_000 );
	}

	@DisplayName( "Benchmark: no-op overhead (branch-heavy, many spans)" )
	@Test
	void benchmarkNoOpBranchHeavy() {
		String source = """
		                total = 0;
		                for( i = 1; i <= 50; i++ ) {
		                    if( i % 2 == 0 ) {
		                        total = total + i;
		                    } else {
		                        total = total - 1;
		                    }
		                    if( i > 25 ) {
		                        total = total * 2;
		                    }
		                }
		                """;
		reportNoOpOverhead( "no-op branch-heavy (many spans)", source, 20_000, 60_000 );
	}
}
