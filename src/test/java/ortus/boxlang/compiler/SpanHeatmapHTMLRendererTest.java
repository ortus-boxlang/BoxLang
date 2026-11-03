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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.CodeProfilerService;
import ortus.boxlang.runtime.services.coverage.SpanHeatmapHTMLRenderer;
import ortus.boxlang.runtime.services.coverage.SpanHeatmapHTMLRenderer.Metric;

/**
 * Tests for {@link SpanHeatmapHTMLRenderer}: the span-only heat maps by total time,
 * per-run cost, and execution count, plus the relative vs fixed scale options.
 */
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class SpanHeatmapHTMLRendererTest {

	private BoxRuntime			runtime;
	private CodeProfilerService	service;
	private boolean				prevProfiler;

	@BeforeEach
	void setup() {
		this.runtime										= BoxRuntime.getInstance();
		this.service										= this.runtime.getCodeProfilerService();
		this.prevProfiler									= this.runtime.getConfiguration().codeProfilerEnabled;
		this.runtime.getConfiguration().codeProfilerEnabled	= true;
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

	/**
	 * Write a source file to disk, execute it through the profiler, and return the
	 * normalized absolute path (the blueprint key for a FILE).
	 */
	private String runDisk( String fileName, String source ) {
		try {
			Path file = Paths.get( "build", "heatmap-test", fileName ).toAbsolutePath().normalize();
			Files.createDirectories( file.getParent() );
			Files.writeString( file, source );
			this.runtime.executeTemplate( file.toString() );
			return file.toString();
		} catch ( java.io.IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * One span that runs MANY times cheaply and one that runs ONCE expensively —
	 * so total, per-run, and count each rank them differently.
	 */
	private static final String HOT_AND_COLD = """
	                                           function cheap() {
	                                           }
	                                           function expensive() {
	                                               sleep( 250 );
	                                           }
	                                           for( i = 1; i <= 50; i++ ) {
	                                               cheap();
	                                           }
	                                           expensive();
	                                           """;

	@DisplayName( "It renders a code-only heat map with mouse-over stats and no tables" )
	@Test
	void testTotalTimeHeatMap() {
		String	key		= runDisk( "HeatTotal.bxs", HOT_AND_COLD );
		String	html	= SpanHeatmapHTMLRenderer.renderHTML( service, key, "heat", Metric.TIME_TOTAL );

		assertThat( html ).contains( "Span heat map by Total time" );
		// ONLY the code table — the outlier/span table is gone.
		assertThat( html ).contains( "table class=\"code\"" );
		assertThat( html ).doesNotContain( "class=\"spans\"" );
		assertThat( html ).doesNotContain( "Outliers" );
		// The real source is shown, not the placeholder.
		assertThat( html ).contains( "sleep( 250 )" );
		assertThat( html ).doesNotContain( "unable to read source" );
		// Mouse-over tooltips carry count + time for the span.
		assertThat( html ).contains( "title=\"count=" );
		assertThat( html ).contains( "per-run=" );
	}

	@DisplayName( "It keeps sub-50ms spans at the pale floor colour regardless of the page max" )
	@Test
	void testSubFloorSpansBarelyTinted() {
		// HOT_AND_COLD has a 250ms span and many ~0ms spans. The sub-50ms spans stay at
		// the pale floor colour (rgb(255,243,176)) even though the page max is 250ms;
		// the 250ms span reaches the deep-red end (rgb(163,0,0)).
		String	key		= runDisk( "HeatFloor.bxs", HOT_AND_COLD );
		String	html	= SpanHeatmapHTMLRenderer.renderHTML( service, key, null, Metric.TIME_TOTAL );

		assertThat( html ).contains( "rgb(255,243,176)" );   // sub-floor pale yellow
		assertThat( html ).contains( "rgb(163,0,0)" );       // hottest span deep red
	}

	@DisplayName( "It keeps spans running 5 or fewer times at the pale floor colour" )
	@Test
	void testCountFloor() {
		// The count metric floors at 5: spans running <= 5 times stay pale even when
		// another span ran 50 times.
		String	key		= runDisk( "HeatCountFloor.bxs", HOT_AND_COLD );
		String	html	= SpanHeatmapHTMLRenderer.renderHTML( service, key, null, Metric.COUNT );

		assertThat( html ).contains( "rgb(255,243,176)" );   // <=5 runs pale floor
		assertThat( html ).contains( "count=50" );          // the 50x span
	}

	@DisplayName( "It distinguishes total time from per-run cost" )
	@Test
	void testPerRunDiffersFromTotal() {
		// `expensive()` runs once for ~250ms; `cheap()` runs 50x but is ~free. Under
		// TOTAL time the expensive span dominates. Under PER-RUN the cheap span's
		// per-run is near-zero while the expensive span keeps ~250ms.
		String	key		= runDisk( "HeatPerRun.bxs", HOT_AND_COLD );
		String	total	= SpanHeatmapHTMLRenderer.renderHTML( service, key, null, Metric.TIME_TOTAL );
		String	perRun	= SpanHeatmapHTMLRenderer.renderHTML( service, key, null, Metric.TIME_PER_RUN );

		assertThat( total ).contains( "Total time" );
		assertThat( perRun ).contains( "Time per run" );
		assertThat( total ).isNotEqualTo( perRun );
		assertThat( perRun ).contains( "sleep( 250 )" );
	}

	@DisplayName( "It honors a fixed scale so multiple files share one absolute scale" )
	@Test
	void testFixedScale() {
		String	key			= runDisk( "HeatFixed.bxs", HOT_AND_COLD );
		String	relative	= SpanHeatmapHTMLRenderer.renderHTML( service, key, null, Metric.TIME_TOTAL, 0L );
		String	fixed		= SpanHeatmapHTMLRenderer.renderHTML( service, key, null, Metric.TIME_TOTAL, 1_000_000_000L );

		assertThat( relative ).contains( "relative (normalized" );
		assertThat( fixed ).contains( "fixed (normalized to a caller-supplied maximum" );
		assertThat( fixed ).contains( "1sec" );
		assertThat( fixed ).isNotEqualTo( relative );
	}

	@DisplayName( "It writes the heat map to a file" )
	@Test
	void testRenderToFile() {
		String				key	= runDisk( "HeatFile.bxs", HOT_AND_COLD );
		java.nio.file.Path	out	= SpanHeatmapHTMLRenderer.renderToFile(
		    service, key, "build/heatmap-test/out-heat.html", "heat", Metric.COUNT, 0L );
		assertThat( java.nio.file.Files.exists( out ) ).isTrue();
	}
}
