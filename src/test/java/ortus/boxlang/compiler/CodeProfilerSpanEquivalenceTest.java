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

import java.util.ArrayList;
import java.util.List;

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
 * THE span-model invariant: <b>whitespace and line breaks do NOT change the span
 * model.</b> The SAME code MUST produce the SAME spans regardless of how it is
 * formatted. A single-line {@code foo( a(), b(), c() )} and a multi-line
 * {@code foo(\n a(),\n b(),\n c()\n)} are the same program and must yield the same
 * span sequence (modulo position/whitespace).
 */
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class CodeProfilerSpanEquivalenceTest {

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
	 * The sequence of span source texts for adhoc BOXSCRIPT source, each with all
	 * whitespace collapsed to a single space and trimmed — so two runs that differ
	 * only by formatting compare equal.
	 *
	 * @param source the BOXSCRIPT source
	 *
	 * @return the normalized span-text sequence in blueprint (source) order
	 */
	private List<String> spanTexts( String source ) {
		runtime.executeSource( source );
		String							key		= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );
		List<CodeProfilerService.Span>	spans	= service.fileSpans( key );
		List<String>					texts	= new ArrayList<>();
		for ( CodeProfilerService.Span s : spans ) {
			texts.add( service.spanSourceText( key, source, s ).replaceAll( "\\s+", " " ).trim() );
		}
		// Reset between the two runs so the second isn't polluted / deduped by the first.
		service.reset();
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		service.setActive( true );
		return texts;
	}

	@DisplayName( "A single-line call and its multi-line equivalent produce identical spans" )
	@Test
	void testSingleLineVsMultiLineCallSpanEquivalence() {
		String			singleLine	= """
		                              getData1 = ()->sleep(100);
		                              getData2 = ()->sleep(200);
		                              getData3 = ()->sleep(300);
		                              processData = (data1, data2, data3)->sleep(100)
		                              processData( getData1(), getData2(), getData3() );
		                              """;
		String			multiLine	= """
		                              getData1 = ()->sleep(100);
		                              getData2 = ()->sleep(200);
		                              getData3 = ()->sleep(300);
		                              processData = (data1, data2, data3)->sleep(100)
		                              processData(
		                                  getData1(),
		                                  getData2(),
		                                  getData3()
		                              );
		                              """;

		List<String>	single		= spanTexts( singleLine );
		List<String>	multi		= spanTexts( multiLine );

		assertThat( single ).isEqualTo( multi );
	}

	@DisplayName( "A multi-line, multi-arg call splits each argument into its own span" )
	@Test
	void testMultiArgCallSplitsArguments() {
		String			source	= """
		                          getData1 = ()->sleep(100);
		                          getData2 = ()->sleep(200);
		                          processData = (data1, data2)->sleep(100)
		                          processData( getData1(), getData2() );
		                          """;
		List<String>	texts	= spanTexts( source );

		// Write the span texts out for inspection / pinning.
		try {
			java.nio.file.Files.writeString( java.nio.file.Paths.get( "build", "span-equivalence.txt" ),
			    String.join( "\n", texts ) );
		} catch ( java.io.IOException e ) {
			// ignore
		}

		assertThat( texts ).isNotEmpty();
	}

	@DisplayName( "A call's head and tail are the bare shell, not surrounding whitespace" )
	@Test
	void testCallShellHasNoSurroundingWhitespace() {
		String source = """
		                getData1 = ()->sleep(100);
		                getData2 = ()->sleep(200);
		                processData = (a, b)->sleep(100)
		                processData( getData1(), getData2() );
		                processData(
		                    getData1(),
		                    getData2()
		                );
		                """;
		runtime.executeSource( source );
		String							key		= IBoxpiler.MD5( BoxSourceType.BOXSCRIPT.toString() + source );
		List<CodeProfilerService.Span>	spans	= service.fileSpans( key );

		StringBuilder					raw		= new StringBuilder();
		List<String>					texts	= new ArrayList<>();
		for ( CodeProfilerService.Span s : spans ) {
			// Show the raw span text with whitespace VISIBLE (· space, ⏎ newline).
			String text = service.spanSourceText( key, source, s );
			texts.add( text );
			raw.append( "(" ).append( s.startLine() ).append( "," ).append( s.startCol() )
			    .append( ")-(" ).append( s.endLine() ).append( "," ).append( s.endCol() ).append( ") [ " )
			    .append( text.replace( " ", "·" ).replace( "\n", "⏎" ).replace( "\t", "→" ) ).append( " ]\n" );
		}
		try {
			java.nio.file.Files.writeString( java.nio.file.Paths.get( "build", "span-raw.txt" ), raw.toString() );
		} catch ( java.io.IOException e ) {
			// ignore
		}

		// INVARIANT: no span carries surrounding whitespace. A span starts on a
		// token and ends on the last token char. Interior whitespace is fine
		// (seperator spans collapse to ", "); leading/trailing is not.
		for ( String text : texts ) {
			assertThat( text ).isEqualTo( text.strip() );
		}
		// Both call forms produce the bare shell head + tail.
		assertThat( texts ).containsAtLeast( "processData(", "getData1()", "getData2()", ")" ).inOrder();
	}

	/**
	 * The {@code #...#} OUT-string wrapper is source METADATA only: it must not change
	 * the span model. {@code visitExprOutString} widens the inner node's source text to
	 * cover the full {@code #el2#}, so it must ALSO widen the node's POSITION — otherwise
	 * the text and the position describe different slices, and
	 * {@code SpanCollectorVisitor.offsetOf()}/{@code pointAt()} (which treat the node's
	 * start as the text's origin) shift every derived point by one. That made a boxed
	 * call's shell absorb the {@code #} plus trailing whitespace and spill a stray
	 * {@code #} span.
	 */
	@DisplayName( "A boxed call's spans match the bare call's spans (BOXSCRIPT + CFSCRIPT)" )
	@Test
	void testBoxedCallSpansMatchBareCallSpans() {
		String	def			= "x = 1\ny = 2\nf = (a,b) -> a\n";
		String	bareTail	= "result = f( x, y );";
		String	boxedTail	= "result = #f( x, y )#;";

		for ( BoxSourceType type : List.of( BoxSourceType.BOXSCRIPT, BoxSourceType.CFSCRIPT ) ) {
			List<String>	bare		= spanTextsForType( def + bareTail, type );
			List<String>	boxed		= spanTextsForType( def + boxedTail, type );

			// The `#` wrapper is metadata: the boxed spans must be the bare spans with the
			// `#` on each END of the whole expression (the head and the tail) and NOTHING
			// else — no stray `#` span, no absorbed whitespace, and the inner argument spans
			// (`x`, `y`) must be untouched.
			List<String>	expected	= new ArrayList<>( bare );
			expected.set( 0, "result = #f(" );
			expected.set( expected.size() - 1, ")#" );

			assertThat( boxed ).isEqualTo( expected );
			// And no span may carry surrounding whitespace, wrapper or not.
			for ( String text : boxed ) {
				assertThat( text ).isEqualTo( text.strip() );
			}
		}
	}

	/**
	 * Span texts (normalized, {@code #} kept) for a given source and syntax.
	 */
	private List<String> spanTextsForType( String source, BoxSourceType type ) {
		runtime.executeSource( source, new ortus.boxlang.runtime.context.ScriptingRequestBoxContext( runtime.getRuntimeContext() ), type );
		String							key		= IBoxpiler.MD5( type.toString() + source );
		List<CodeProfilerService.Span>	spans	= service.fileSpans( key );
		List<String>					texts	= new ArrayList<>();
		for ( CodeProfilerService.Span s : spans ) {
			// Only the statement line(s) that matter (line 4).
			if ( s.startLine() >= 4 ) {
				texts.add( service.spanSourceText( key, source, s ).replaceAll( "\\s+", " " ).trim() );
			}
		}
		service.reset();
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		service.setActive( true );
		return texts;
	}
}
