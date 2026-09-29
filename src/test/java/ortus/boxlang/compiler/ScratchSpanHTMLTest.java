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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.CodeProfilerService;
import ortus.boxlang.runtime.services.coverage.LineCoverageHTMLRenderer;
import ortus.boxlang.runtime.services.coverage.SpanHTMLRenderer;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * TEMPORARY driver: runs a disk file through the profiler and hands off to the
 * standalone {@link SpanHTMLRenderer} to write an annotated HTML view for
 * eyeballing span definitions. NOT KEPT AROUND.
 */
class ScratchSpanHTMLTest {

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

	@Test
	void renderSpansToHTML() {
		// Output filenames map back to the SOURCE file they came from:
		// `<source>.html` (e.g. ProfilerSample.bxs → build/coverage-html/ProfilerSample.bxs.html).
		// The span and line renderers write separate views per source so neither
		// overwrites the other: `<source>.html` (span view) and
		// `<source>-line.html` (line view).
		String outDir = "build/coverage-html";

		// The four sample files DO the class instantiation / static references
		// themselves. Running them loads the disk classes into the profiler, so
		// their blueprints get real coverage counts.
		runSample( "src/test/resources/profiler/ProfilerSample.bxs" );
		runSample( "src/test/resources/profiler/ProfilerSample.cfs" );
		runSample( "src/test/resources/profiler/ProfilerSample.bxm" );
		runSample( "src/test/resources/profiler/ProfilerSample.cfm" );

		// The GHOST disk classes are NEVER referenced by the samples (that is the
		// whole point — their blueprints must show ALL RED). They only need to be
		// COMPILED (Pass A registers the blueprint); nothing is instantiated.
		compileGhost( "src/test/resources/profiler/ProfilerGhost.bx" );
		compileGhost( "src/test/resources/profiler/ProfilerGhostCF.cfc" );
		compileGhost( "src/test/resources/profiler/ProfilerGhostTag.cfc" );

		// Render HTML for EVERY tracked blueprint under the profiler sample dir:
		// the samples plus any disk class they referenced (ProfilerComplex*,
		// ProfilerStaticOnly*, ProfilerSuper, ProfilerGhost*). Skip anything the
		// test harness loaded outside that dir (e.g. an Application.bx).
		for ( String fileKey : CodeProfilerService.trackedBlueprints().keySet() ) {
			if ( fileKey.contains( "resources\\profiler" ) || fileKey.contains( "resources/profiler" ) ) {
				renderTracked( fileKey, outDir );
			}
		}
	}

	/**
	 * Run a sample template so it executes and registers its blueprint (and any
	 * disk classes it references).
	 *
	 * @param relativePath the sample file (relative to the repo root)
	 */
	private void runSample( String relativePath ) {
		IBoxContext context = new ScriptingRequestBoxContext( this.runtime.getRuntimeContext() );
		this.runtime.executeTemplate( relativePath );
	}

	/**
	 * Compile a ghost disk class (register its blueprint) WITHOUT running it, so
	 * every span stays RED in the HTML. This is not instantiation — the class is
	 * never referenced, which is the ghost scenario.
	 *
	 * @param relativePath the on-disk class file
	 */
	private void compileGhost( String relativePath ) {
		Path absolute = Paths.get( relativePath ).toAbsolutePath().normalize();
		RunnableLoader.getInstance().getBoxpiler().compileClass( ResolvedFilePath.of( absolute ) );
	}

	/**
	 * Render both the span and line HTML views for a tracked blueprint key.
	 *
	 * @param fileKey the normalized blueprint key (an absolute lowercase path)
	 * @param outDir  the output directory
	 */
	private void renderTracked( String fileKey, String outDir ) {
		// Derive the output filename from the blueprint's source file path so it
		// maps back to the source (<source>.html / <source>-line.html).
		String	fileName	= fileKey;
		int		slash		= Math.max( fileKey.lastIndexOf( '/' ), fileKey.lastIndexOf( '\\' ) );
		if ( slash >= 0 ) {
			fileName = fileKey.substring( slash + 1 );
		}
		Path spanOut = SpanHTMLRenderer.renderToFile( fileKey, outDir + "/" + fileName + ".html", fileName );
		System.out.println( "WROTE: " + spanOut );
		Path lineOut = LineCoverageHTMLRenderer.renderToFile( fileKey, outDir + "/" + fileName + "-line.html", fileName );
		System.out.println( "WROTE: " + lineOut );

		String source = null;
		try {
			source = java.nio.file.Files.readString( Paths.get( fileKey ) );
		} catch ( java.io.IOException e ) {
			// ignore
		}
		System.out.println( "SPANS(" + fileName + "): " + CodeProfilerService.fileSpans( fileKey ).size() );
		for ( CodeProfilerService.Span s : CodeProfilerService.fileSpans( fileKey ) ) {
			System.out.println(
			    "  id=" + s.id() + " (" + s.startLine() + "," + s.startCol() + ")-(" + s.endLine() + "," + s.endCol() + ") count=" + s.stats().count()
			        + " [" + CodeProfilerService.spanSourceText( fileKey, source, s ).replace( "\n", "\\n" ) + "]" );
		}
	}
}