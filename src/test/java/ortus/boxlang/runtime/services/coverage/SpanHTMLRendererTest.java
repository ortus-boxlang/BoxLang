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
package ortus.boxlang.runtime.services.coverage;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * Tests the overall coverage percentage reported at the top of the LINE
 * ({@link LineCoverageHTMLRenderer}) and SPAN ({@link SpanHTMLRenderer}) HTML
 * reports.
 * <p>
 * The LINE percentage is covered lines / executable lines. The SPAN percentage is
 * covered EXECUTABLE CHARACTERS / executable characters — whitespace is excluded
 * because it is not executable, so a large multi-line span is weighted by its real
 * code rather than counted as one.
 */
class SpanHTMLRendererTest {

	private static final Blueprint	BLUEPRINT	= new Blueprint(
	    List.of(
	        // "x = 1;" -> 4 non-whitespace chars (x,=,1,;)
	        new Blueprint.SpanDef( 1, 0, 1, 6, true ),
	        // "sleep( 100 )" -> 10 non-whitespace chars (s,l,e,e,p,(,1,0,0,))
	        new Blueprint.SpanDef( 2, 0, 2, 12, true )
	    ) );

	private BoxRuntime				runtime;
	private CodeProfilerService		service;

	@BeforeEach
	void setup() {
		this.runtime	= BoxRuntime.getInstance();
		this.service	= this.runtime.getCodeProfilerService();
		service.setActive( true );
		service.reset();
	}

	@AfterEach
	void teardown() {
		service.setActive( false );
		service.reset();
	}

	/**
	 * Write the two-line source to a temp file and register the blueprint so the
	 * renderers can read both the text and the tracked spans.
	 *
	 * @return the file's absolute path (the blueprint key)
	 */
	private String registerSource() {
		try {
			Path	file	= Files.createTempFile( "SpanHTMLRendererTest", ".bxs" );
			String	source	= "x = 1;\nsleep( 100 )";
			Files.writeString( file, source );
			String key = file.toAbsolutePath().normalize().toString();
			service.registerBlueprintForFile( key, BLUEPRINT );
			return key;
		} catch ( java.io.IOException e ) {
			throw new RuntimeException( e );
		}
	}

	@DisplayName( "It reports SPAN coverage as covered executable chars / executable chars" )
	@Test
	void testSpanCoveragePercent() {
		String key = registerSource();
		// Only span 0 ("x = 1;", 4 chars) runs; span 1 ("sleep( 100 )", 10 chars) is missed.
		CodeProfilerService.mark( key, 0 );

		String html = SpanHTMLRenderer.renderHTML( service, key, "test" );

		// 4 covered of 14 executable chars = 28.57% -> 29%.
		assertThat( html ).contains( "Span coverage: 29%" );
		assertThat( html ).contains( "(4 of 14 executable chars)" );
	}

	@DisplayName( "It reports LINE coverage as covered lines / executable lines" )
	@Test
	void testLineCoveragePercent() {
		String key = registerSource();
		// Line 1 runs, line 2 is missed -> 1 of 2 executable lines.
		CodeProfilerService.mark( key, 0 );

		String html = LineCoverageHTMLRenderer.renderHTML( service, key, "test" );

		assertThat( html ).contains( "Line coverage: 50%" );
		assertThat( html ).contains( "(1 of 2 executable lines)" );
	}

	@DisplayName( "It counts only non-whitespace characters as executable" )
	@Test
	void testCountNonWhitespace() {
		assertThat( SpanHTMLRenderer.countNonWhitespace( "x = 1;", 0, 6 ) ).isEqualTo( 4 );
		assertThat( SpanHTMLRenderer.countNonWhitespace( "sleep( 100 )", 0, 12 ) ).isEqualTo( 10 );
		// Range is clamped to the source bounds.
		assertThat( SpanHTMLRenderer.countNonWhitespace( "ab", 0, 100 ) ).isEqualTo( 2 );
	}

	@DisplayName( "It rounds percentages and reports 100% when nothing is executable" )
	@Test
	void testFormatPercent() {
		assertThat( SpanHTMLRenderer.formatPercent( 4, 14 ) ).isEqualTo( "29%" );
		assertThat( SpanHTMLRenderer.formatPercent( 1, 3 ) ).isEqualTo( "33%" );
		assertThat( SpanHTMLRenderer.formatPercent( 3, 3 ) ).isEqualTo( "100%" );
		// No executable code -> vacuously fully covered (matches the exporters).
		assertThat( SpanHTMLRenderer.formatPercent( 0, 0 ) ).isEqualTo( "100%" );
	}
}
