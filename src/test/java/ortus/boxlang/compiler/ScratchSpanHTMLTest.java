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
import java.util.Locale;

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
		renderOne( "src/test/resources/profiler/ProfilerSample.bxs", "build/scratch-span-html.html", "build/scratch-line-html.html" );
		renderOne( "src/test/resources/profiler/ProfilerSample.cfs", "build/scratch-span-html-cfs.html", "build/scratch-line-html-cfs.html" );
		renderOne( "src/test/resources/profiler/ProfilerSample.bxm", "build/scratch-span-html-bxm.html", "build/scratch-line-html-bxm.html" );
		renderOne( "src/test/resources/profiler/ProfilerSample.cfm", "build/scratch-span-html-cfm.html", "build/scratch-line-html-cfm.html" );
	}

	private void renderOne( String relativePath, String spanOutPath, String lineOutPath ) {
		Path		absolute	= Paths.get( relativePath ).toAbsolutePath().normalize();
		String		fileKey		= absolute.toString().toLowerCase( Locale.ROOT );

		IBoxContext	context		= new ScriptingRequestBoxContext( this.runtime.getRuntimeContext() );
		this.runtime.executeTemplate( relativePath );

		// Span-level renderer.
		Path spanOut = SpanHTMLRenderer.renderToFile( fileKey, spanOutPath, absolute.getFileName().toString() );
		System.out.println( "WROTE: " + spanOut );

		// Line-level renderer.
		Path lineOut = LineCoverageHTMLRenderer.renderToFile( fileKey, lineOutPath, absolute.getFileName().toString() );
		System.out.println( "WROTE: " + lineOut );

		String source = null;
		try {
			source = java.nio.file.Files.readString( absolute );
		} catch ( java.io.IOException e ) {
			// ignore
		}
		System.out.println( "SPANS(" + relativePath + "): " + CodeProfilerService.fileSpans( fileKey ).size() );
		for ( CodeProfilerService.Span s : CodeProfilerService.fileSpans( fileKey ) ) {
			System.out.println(
			    "  id=" + s.id() + " (" + s.startLine() + "," + s.startCol() + ")-(" + s.endLine() + "," + s.endCol() + ") count=" + s.stats().count()
			        + " [" + CodeProfilerService.spanSourceText( fileKey, source, s ).replace( "\n", "\\n" ) + "]" );
		}
	}
}