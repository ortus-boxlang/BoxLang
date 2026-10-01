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
package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Unit tests for {@link CodeProfilerService} using the SPAN + BLUEPRINT model:
 * we register a file blueprint with executable spans, then call
 * {@link CodeProfilerService#mark(int,int)} with (fileId, spanId) as instrumented
 * bytecode would, sleeping to simulate CPU self-time, and assert the span/line
 * query methods.
 */
class CodeProfilerServiceTest {

	private static final String	FILE	= "src/test/resources/code-profiler/sample.bx";

	private BoxRuntime			runtime;

	@BeforeEach
	void setup() {
		this.runtime = BoxRuntime.getInstance();
		this.runtime.getCodeProfilerService();
		CodeProfilerService.setActive( true );
		CodeProfilerService.reset();
	}

	@AfterEach
	void teardown() {
		CodeProfilerService.setActive( false );
		CodeProfilerService.reset();
	}

	// \tx = 1; (tab is leading whitespace, span starts at col 1)
	// \ty = 2;
	// \tz = x + y;
	private static final Blueprint	PLAIN_BLUEPRINT		= new Blueprint( 3,
	    List.of(
	        new Blueprint.SpanDef( 1, 1, 1, 8, true ),   // span 0: "x = 1 "
	        new Blueprint.SpanDef( 2, 1, 2, 8, true ),   // span 1: "y = 2 "
	        new Blueprint.SpanDef( 3, 1, 3, 12, true )   // span 2: "z = x + y "
	    ) );

	// \tx = bar ? baz : bum;
	// tab(col0) non-exec gap; "x = " exec; " ? " gap; "bar" exec; " : " gap; "baz" exec;
	// " " gap; "bum" exec. The trailing "; " is whitespace/gap, NOT its own span.
	private static final Blueprint	TERNARY_BLUEPRINT	= new Blueprint( 1,
	    List.of(
	        new Blueprint.SpanDef( 1, 0, 1, 0, false ),   // tab (non-exec gap)
	        new Blueprint.SpanDef( 1, 1, 1, 4, true ),    // span 0: "x = "
	        new Blueprint.SpanDef( 1, 5, 1, 5, false ),   // "?" (non-exec gap)
	        new Blueprint.SpanDef( 1, 7, 1, 10, true ),   // span 1: "bar"
	        new Blueprint.SpanDef( 1, 11, 1, 11, false ), // ":" (non-exec gap)
	        new Blueprint.SpanDef( 1, 13, 1, 16, true ),  // span 2: "baz"
	        new Blueprint.SpanDef( 1, 18, 1, 21, true )   // span 3: "bum"
	    ) );

	// -------------------------------------------------------------------------
	// Plain statements, one per line
	// -------------------------------------------------------------------------

	@DisplayName( "It tracks plain spans, one per line" )
	@Test
	void testPlainStatements() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, PLAIN_BLUEPRINT );

		CodeProfilerService.mark( fileId, 0 );
		sleep( 20 );
		CodeProfilerService.mark( fileId, 1 );
		sleep( 10 );
		CodeProfilerService.mark( fileId, 2 );
		sleep( 5 );

		// Each line is covered with one span -> count == 1
		for ( int line = 1; line <= 3; line++ ) {
			CodeProfilerService.LineCoverage lc = CodeProfilerService.lineAt( FILE, line );
			assertThat( lc ).isNotNull();
			assertThat( lc.covered() ).isTrue();
			assertThat( lc.count() ).isEqualTo( 1 );
		}

		// spanAt resolves a point to the executable span
		CodeProfilerService.Span span2 = CodeProfilerService.spanAt( FILE, 2, 1 );
		assertThat( span2 ).isNotNull();
		assertThat( span2.id() ).isEqualTo( 1 );
		assertThat( span2.stats().count() ).isEqualTo( 1 );
		assertThat( span2.stats().totalNanos() ).isGreaterThan( 0L );

		// Longer sleep on span 0 -> more time than span 2
		CodeProfilerService.Span	s0	= CodeProfilerService.spanAt( FILE, 1, 1 );
		CodeProfilerService.Span	s2	= CodeProfilerService.spanAt( FILE, 3, 1 );
		assertThat( s0.stats().totalNanos() ).isGreaterThan( s2.stats().totalNanos() );

		// fileLines has all 3 lines
		assertThat( CodeProfilerService.fileLines( FILE ).keySet() ).containsExactly( 1, 2, 3 );
	}

	// -------------------------------------------------------------------------
	// Ternary: not all spans on the line run
	// -------------------------------------------------------------------------

	@DisplayName( "It marks only the executable spans that actually ran on one line" )
	@Test
	void testTernary() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, TERNARY_BLUEPRINT );

		// x = bar ? baz : bum; -> bar true, so baz runs, bum does NOT.
		CodeProfilerService.mark( fileId, 0 ); // "x = "
		CodeProfilerService.mark( fileId, 1 ); // "bar"
		CodeProfilerService.mark( fileId, 2 ); // "baz"
		// "bum" (span 3) never marked

		// Covered spans
		assertThat( CodeProfilerService.spanAt( FILE, 1, 0 ) ).isNull();              // tab NOT_EXECUTABLE
		assertThat( CodeProfilerService.spanAt( FILE, 1, 1 ).stats().count() ).isEqualTo( 1 );  // x =
		assertThat( CodeProfilerService.spanAt( FILE, 1, 5 ) ).isNull();                          // "?" NOT_EXECUTABLE
		assertThat( CodeProfilerService.spanAt( FILE, 1, 7 ).stats().count() ).isEqualTo( 1 );  // bar
		assertThat( CodeProfilerService.spanAt( FILE, 1, 13 ).stats().count() ).isEqualTo( 1 ); // baz

		// "bum" (span 3) is MISSED: executable span registered but count == 0
		CodeProfilerService.Span bum = CodeProfilerService.spanAt( FILE, 1, 18 );
		assertThat( bum ).isNotNull();
		assertThat( bum.stats().count() ).isEqualTo( 0 );

		// trailing ";" is whitespace/gap -> non-executable
		assertThat( CodeProfilerService.spanAt( FILE, 1, 22 ) ).isNull();

		// Line is covered (x=, bar, baz all ran); count = first/outermost span "x = "
		CodeProfilerService.LineCoverage line = CodeProfilerService.lineAt( FILE, 1 );
		assertThat( line ).isNotNull();
		assertThat( line.covered() ).isTrue();
		assertThat( line.count() ).isEqualTo( 1 );
	}

	// -------------------------------------------------------------------------
	// Function definition (definition-time vs invocation-time spans)
	// -------------------------------------------------------------------------

	// \t1 public static string function myFunc( required string arg="default value", arg2=somevar.foobar ){
	// \t2 return 42;
	// \t3 }
	//
	// Definition-time executable spans:
	// "public static string function myFunc" (registers the UDF)
	// "required string arg" (arg declaration)
	// "arg2" (arg declaration)
	// Non-executable: "=","(",")",",","{","}"," " gaps, and "=\"default value\"" (a stored
	// literal default -> no bytecode, NOT_EXECUTABLE), and "return 42" (body, invocation-only).
	// Invocation-time-only executable spans (MISSED during definition until the arg is
	// omitted / the body is called): "=somevar.foobar" default expr (span 3), "return 42" (span 4).
	private static final Blueprint FUNC_BLUEPRINT = new Blueprint( 3,
	    List.of(
	        new Blueprint.SpanDef( 1, 1, 1, 33, true ),  // span 0: "public static string function myFunc"
	        new Blueprint.SpanDef( 1, 35, 1, 54, true ), // span 1: "required string arg"
	        new Blueprint.SpanDef( 1, 55, 1, 57, false ),// "=" (gap)
	        new Blueprint.SpanDef( 1, 58, 1, 72, false ),// "\"default value\"" stored literal -> NOT_EXECUTABLE
	        new Blueprint.SpanDef( 1, 76, 1, 80, true ), // span 2: "arg2" (arg declaration)
	        new Blueprint.SpanDef( 1, 81, 1, 81, false ),// "=" (gap)
	        new Blueprint.SpanDef( 1, 82, 1, 96, true ), // span 3: "somevar.foobar" (invocation-only default expr)
	        new Blueprint.SpanDef( 2, 5, 2, 14, true ),  // span 4: "return 42" (invocation-only body)
	        new Blueprint.SpanDef( 3, 1, 3, 1, false )   // "}" (gap)
	    ) );

	@DisplayName( "It separates definition-time from invocation-time spans on a function" )
	@Test
	void testFunctionDefinition() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, FUNC_BLUEPRINT );

		// DEFINITION runs: registers the UDF + processes args. Arg defaults NOT evaluated.
		CodeProfilerService.mark( fileId, 0 ); // function declaration/registration
		CodeProfilerService.mark( fileId, 1 ); // required string arg
		CodeProfilerService.mark( fileId, 2 ); // arg2
		// "somevar.foobar" (span 3) and "return 42" (span 4) are NOT marked at definition.

		// Definition-time spans are covered
		assertThat( CodeProfilerService.spanAt( FILE, 1, 1 ).stats().count() ).isEqualTo( 1 );  // myFunc decl
		assertThat( CodeProfilerService.spanAt( FILE, 1, 35 ).stats().count() ).isEqualTo( 1 ); // required string arg
		assertThat( CodeProfilerService.spanAt( FILE, 1, 76 ).stats().count() ).isEqualTo( 1 ); // arg2

		// Stored literal default is NOT_EXECUTABLE
		assertThat( CodeProfilerService.spanAt( FILE, 1, 58 ) ).isNull();

		// Invocation-only spans are MISSED (registered executable, count == 0)
		CodeProfilerService.Span defaultExpr = CodeProfilerService.spanAt( FILE, 1, 82 );
		assertThat( defaultExpr ).isNotNull();
		assertThat( defaultExpr.stats().count() ).isEqualTo( 0 );
		CodeProfilerService.Span body = CodeProfilerService.spanAt( FILE, 2, 5 );
		assertThat( body ).isNotNull();
		assertThat( body.stats().count() ).isEqualTo( 0 );

		// Closing brace is NOT_EXECUTABLE
		assertThat( CodeProfilerService.spanAt( FILE, 3, 1 ) ).isNull();

		// NOW an invocation omits arg2 -> its default evaluates, and the body runs.
		CodeProfilerService.mark( fileId, 3 ); // somevar.foobar default evaluates
		CodeProfilerService.mark( fileId, 4 ); // return 42 body

		assertThat( CodeProfilerService.spanAt( FILE, 1, 82 ).stats().count() ).isEqualTo( 1 ); // default now ran
		assertThat( CodeProfilerService.spanAt( FILE, 2, 5 ).stats().count() ).isEqualTo( 1 );  // body now ran

		// Line 1 is covered; count = first/outermost span "public static string function myFunc"
		CodeProfilerService.LineCoverage line1 = CodeProfilerService.lineAt( FILE, 1 );
		assertThat( line1 ).isNotNull();
		assertThat( line1.covered() ).isTrue();
		assertThat( line1.count() ).isEqualTo( 1 );
	}

	// -------------------------------------------------------------------------
	// Adhoc source (like REPL / unsafe eval) keyed by source hash
	// -------------------------------------------------------------------------

	@DisplayName( "It registers adhoc source under its hash with SOURCE kind" )
	@Test
	void testAdhocSource() {
		String		source			= "x = foo() + 1;";
		// hashCode here stands in for the compiler's MD5(source); in a real flow the
		// boxpiler computes and passes the same hash.
		String		sourceHash		= Integer.toString( source.hashCode() );

		Blueprint	sourceBlueprint	= new Blueprint( 1,
		    List.of(
		        new Blueprint.SpanDef( 1, 0, 1, 4, true ),   // span 0: "x = "
		        new Blueprint.SpanDef( 1, 5, 1, 9, true ),   // span 1: "foo()"
		        new Blueprint.SpanDef( 1, 10, 1, 12, true )  // span 2: "+ 1"
		    ),
		    Blueprint.Kind.SOURCE );

		String		fileId			= CodeProfilerService.registerBlueprintForSource( sourceHash, sourceBlueprint );

		CodeProfilerService.mark( fileId, 0 );
		CodeProfilerService.mark( fileId, 1 );

		// Query by the hash key
		assertThat( CodeProfilerService.spanAt( sourceHash, 1, 0 ).stats().count() ).isEqualTo( 1 );
		assertThat( CodeProfilerService.spanAt( sourceHash, 1, 10 ).stats().count() ).isEqualTo( 0 );

		// trackedBlueprints includes the adhoc entry and it is self-describing as SOURCE
		assertThat( CodeProfilerService.trackedBlueprints() ).containsKey( sourceHash );
		assertThat( CodeProfilerService.trackedBlueprints().get( sourceHash ).kind() ).isEqualTo( Blueprint.Kind.SOURCE );
	}

	// -------------------------------------------------------------------------
	// JSON export
	// -------------------------------------------------------------------------

	@DisplayName( "It builds native per-line coverage data (FILE default)" )
	@Test
	void testBuildLineCoverage() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, PLAIN_BLUEPRINT );

		CodeProfilerService.mark( fileId, 0 );
		sleep( 5 );
		CodeProfilerService.mark( fileId, 1 );
		// span 2 (line 3) never marked -> missed

		IStruct	data	= CodeProfilerService.buildLineCoverage();

		// FILE is included by default; key is the normalized file path
		String	key		= normalizePath( FILE );
		assertThat( data.containsKey( key ) ).isTrue();
		IStruct fileData = ( IStruct ) data.get( key );
		assertThat( fileData.get( "filePath" ).toString() ).isEqualTo( key );
		assertThat( fileData.get( "kind" ).toString() ).isEqualTo( "FILE" );
		assertThat( fileData.get( "numExecutableLines" ) ).isEqualTo( 3L );
		assertThat( fileData.get( "numCoveredLines" ) ).isEqualTo( 2L );
		assertThat( ( ( Number ) fileData.get( "percCoverage" ) ).doubleValue() ).isEqualTo( 2.0 / 3.0 );

		// Line 1 and 2 covered (count 1); line 3 missed (count 0).
		IStruct lines = ( IStruct ) fileData.get( "lines" );
		assertThat( lines.containsKey( Key.of( 1 ) ) ).isTrue();
		assertThat( lines.containsKey( Key.of( 3 ) ) ).isTrue();
		assertThat( ( ( IStruct ) lines.get( Key.of( 1 ) ) ).get( "covered" ) ).isEqualTo( true );
		assertThat( ( ( IStruct ) lines.get( Key.of( 3 ) ) ).get( "covered" ) ).isEqualTo( false );
		assertThat( ( ( IStruct ) lines.get( Key.of( 3 ) ) ).get( "count" ) ).isEqualTo( 0L );
	}

	@DisplayName( "It builds native per-span coverage data" )
	@Test
	void testBuildSpanCoverage() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, PLAIN_BLUEPRINT );

		CodeProfilerService.mark( fileId, 0 );
		sleep( 5 );
		CodeProfilerService.mark( fileId, 1 );
		// span 2 missed

		IStruct	data		= CodeProfilerService.buildSpanCoverage();
		String	key			= normalizePath( FILE );
		IStruct	fileData	= ( IStruct ) data.get( key );
		assertThat( fileData.get( "filePath" ).toString() ).isEqualTo( key );
		assertThat( fileData.get( "kind" ).toString() ).isEqualTo( "FILE" );
		Array spans = ( Array ) fileData.get( "spans" );
		assertThat( spans.size() ).isEqualTo( 3 );
		IStruct first = ( IStruct ) spans.get( 0 );
		assertThat( first.get( "id" ) ).isEqualTo( 0 );
		assertThat( first.get( "startLine" ) ).isEqualTo( 1 );
		assertThat( first.get( "count" ) ).isEqualTo( 1L );
		assertThat( ( ( Number ) first.get( "totalNanos" ) ).longValue() ).isGreaterThan( 0L );
		assertThat( ( ( IStruct ) spans.get( 1 ) ).get( "count" ) ).isEqualTo( 1L );
		assertThat( ( ( IStruct ) spans.get( 2 ) ).get( "count" ) ).isEqualTo( 0L );
	}

	@DisplayName( "It writes a per-line coverage JSON report" )
	@Test
	void testWriteLineCoverageJSON() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, PLAIN_BLUEPRINT );

		CodeProfilerService.mark( fileId, 0 );
		sleep( 5 );
		CodeProfilerService.mark( fileId, 1 );
		// span 2 (line 3) never marked -> missed

		String json = CodeProfilerService.buildLineCoverageJSON();
		assertThat( json ).contains( "\"filePath\"" );
		assertThat( json ).contains( "\"kind\"" );
		assertThat( json ).contains( "\"numExecutableLines\"" );
		assertThat( json ).contains( "\"numCoveredLines\"" );
		assertThat( json ).contains( "\"percCoverage\"" );
		assertThat( json ).contains( "\"lines\"" );
		assertThat( json ).contains( "\"totalNanos\"" );
		// Line 1 and 2 covered (count 1); line 3 missed (count 0).
		assertThat( json ).contains( "\"1\"" );
		assertThat( json ).contains( "\"3\"" );
		assertThat( json ).contains( "\"count\" : 0" );
		assertThat( json ).contains( "\"covered\" : false" );
		assertThat( json ).contains( "\"covered\" : true" );
	}

	@DisplayName( "It writes a per-span coverage JSON report" )
	@Test
	void testWriteSpanCoverageJSON() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, PLAIN_BLUEPRINT );

		CodeProfilerService.mark( fileId, 0 );
		sleep( 5 );
		// span 1 missed

		String json = CodeProfilerService.buildSpanCoverageJSON();
		assertThat( json ).contains( "\"spans\"" );
		assertThat( json ).contains( "\"id\" : 0" );
		assertThat( json ).contains( "\"startLine\"" );
		assertThat( json ).contains( "\"startCol\"" );
		assertThat( json ).contains( "\"endLine\"" );
		assertThat( json ).contains( "\"endCol\"" );
		assertThat( json ).contains( "\"totalNanos\"" );
		assertThat( json ).contains( "\"count\" : 1" );
		assertThat( json ).contains( "\"count\" : 0" );
	}

	@DisplayName( "It exports SonarQube generic coverage XML" )
	@Test
	void testSonarQubeXML() {
		String fileId = CodeProfilerService.registerBlueprintForFile( FILE, PLAIN_BLUEPRINT );

		CodeProfilerService.mark( fileId, 0 );
		sleep( 5 );
		CodeProfilerService.mark( fileId, 1 );
		// span 2 (line 3) never marked -> missed

		String xml = CodeProfilerService.buildSonarQubeXML();
		assertThat( xml ).contains( "<coverage version=\"1\">" );
		assertThat( xml ).contains( "<file path=\"" );
		assertThat( xml ).contains( "lineToCover" );
		assertThat( xml ).contains( "lineNumber=\"1\"" );
		assertThat( xml ).contains( "lineNumber=\"3\"" );
		assertThat( xml ).contains( "covered=\"true\"" );
		assertThat( xml ).contains( "covered=\"false\"" );
	}

	@DisplayName( "It filters by kind when exporting" )
	@Test
	void testKindFilter() {
		String		sourceHash		= Integer.toString( "json-filter".hashCode() );
		Blueprint	sourceBlueprint	= new Blueprint( 1,
		    List.of(
		        new Blueprint.SpanDef( 1, 0, 1, 4, true )
		    ),
		    Blueprint.Kind.SOURCE );
		String		fileId			= CodeProfilerService.registerBlueprintForSource( sourceHash, sourceBlueprint );
		CodeProfilerService.mark( fileId, 0 );

		// Default (FILE) excludes SOURCE
		IStruct defaultData = CodeProfilerService.buildLineCoverage();
		assertThat( defaultData.containsKey( sourceHash ) ).isFalse();

		// Explicit SOURCE includes it
		IStruct sourceData = CodeProfilerService.buildLineCoverage( Blueprint.Kind.SOURCE );
		assertThat( sourceData.containsKey( sourceHash ) ).isTrue();

		// Both
		IStruct both = CodeProfilerService.buildLineCoverage( Blueprint.Kind.FILE, Blueprint.Kind.SOURCE );
		assertThat( both.containsKey( sourceHash ) ).isTrue();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static String normalizePath( String path ) {
		return java.nio.file.Paths.get( path ).toAbsolutePath().normalize().toString().toLowerCase( java.util.Locale.ROOT );
	}

	private static void sleep( long ms ) {
		try {
			Thread.sleep( ms );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}
}