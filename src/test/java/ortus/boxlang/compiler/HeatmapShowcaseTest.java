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
 * Runs the purpose-built {@code HeatmapShowcase.bxs} sample and renders all three
 * span heat maps (total time, per-run, count) so each metric's story can be seen
 * side by side. Also asserts the three metrics genuinely DIFFER on this sample.
 */
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class HeatmapShowcaseTest {

	private static final String	SHOWCASE	= "src/test/resources/profiler/HeatmapShowcase.bxs";

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

	/** The normalized absolute path key for the on-disk showcase file. */
	private String key() {
		return Paths.get( SHOWCASE ).toAbsolutePath().normalize().toString();
	}

	private String render( Metric metric ) {
		return SpanHeatmapHTMLRenderer.renderHTML( service, key(), "HeatmapShowcase.bxs", metric, 0L );
	}

	@DisplayName( "It renders all three heat-map exports for the showcase sample" )
	@Test
	void testRenderAllHeatMaps() {
		runtime.executeTemplate( SHOWCASE );

		String	outDir	= "build/heatmap-showcase";
		Path	total	= SpanHeatmapHTMLRenderer.renderToFile( service, key(), outDir + "/showcase-heat-total.html", "heat by total",
		    Metric.TIME_TOTAL, 0L );
		Path	perRun	= SpanHeatmapHTMLRenderer.renderToFile( service, key(), outDir + "/showcase-heat-perrun.html", "heat by per-run",
		    Metric.TIME_PER_RUN, 0L );
		Path	count	= SpanHeatmapHTMLRenderer.renderToFile( service, key(), outDir + "/showcase-heat-count.html", "heat by count",
		    Metric.COUNT, 0L );

		assertThat( java.nio.file.Files.exists( total ) ).isTrue();
		assertThat( java.nio.file.Files.exists( perRun ) ).isTrue();
		assertThat( java.nio.file.Files.exists( count ) ).isTrue();

		// Each map is a code-only view with mouse-overs (no tables).
		String totalHtml = render( Metric.TIME_TOTAL );
		assertThat( totalHtml ).contains( "table class=\"code\"" );
		assertThat( totalHtml ).doesNotContain( "class=\"spans\"" );
		assertThat( totalHtml ).contains( "title=\"count=" );

		// The three metrics each tell a different story.
		String	perRunHtml	= render( Metric.TIME_PER_RUN );
		String	countHtml	= render( Metric.COUNT );
		assertThat( totalHtml ).isNotEqualTo( perRunHtml );
		assertThat( perRunHtml ).isNotEqualTo( countHtml );
	}

	@DisplayName( "It exposes a top time outlier, a top count hotspot, and per-run differences" )
	@Test
	void testScenarioDifferentiation() {
		runtime.executeTemplate( SHOWCASE );
		String	fileKey		= key();

		// [TIME] the one-shot sleep(400) is the top TOTAL-TIME span but ran ONCE.
		// [COUNT] the 300-iteration loop's call spans ran 300x.
		// [BOTH] the 20x loop ran 20x.
		// [PERRUN] the 30ms span costs more per run than the 10ms span.
		boolean	has300		= false, has400 = false;
		long	maxCount	= 0;
		for ( CodeProfilerService.Span s : service.fileSpans( fileKey ) ) {
			maxCount = Math.max( maxCount, s.stats().count() );
			if ( s.stats().totalNanos() >= 350L * 1_000_000L && s.stats().count() == 1 ) {
				has400 = true;
			}
			if ( s.stats().count() >= 300 ) {
				has300 = true;
			}
		}
		// One-shot slow span exists (TIME outlier), and a 300x span exists (COUNT outlier).
		assertThat( has400 ).isTrue();
		assertThat( has300 ).isTrue();
		assertThat( maxCount ).isAtLeast( 300 );
	}
}
