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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.coverage.CoverageDataBuilder;
import ortus.boxlang.runtime.services.coverage.CoverageJSONExporter;
import ortus.boxlang.runtime.services.coverage.CoverageSonarQubeExporter;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The runtime {@link IService} responsible for collecting per-source-span
 * execution data produced by instrumented bytecode.
 * <p>
 * Design (see workbench/internal-docs/COVERAGE_DESIGN.md): the boxpiler bakes a
 * per-file BLUEPRINT of every executable SPAN into the generated class. When the
 * class is first loaded, it registers the blueprint
 * ({@link #registerBlueprint}), getting a compact {@code fileId}, and the service
 * pre-allocates a counter for every executable span. Instrumented bytecode then
 * calls {@link #mark(int,int)} with a file id + span id — a direct array-index
 * increment, no positions.
 * <p>
 * A span is the atomic executable unit. Non-executable spans (whitespace, `?`,
 * `:`, `;`, braces, `else`, comments) have no id and are never registered. Spans
 * support 3 states: COVERED (ran), MISSED (registered but never ran), and
 * NOT_EXECUTABLE (not in the blueprint).
 * <p>
 * Only {@link #mark} is on the hot path; everything else is lifecycle/querying.
 */
public class CodeProfilerService extends BaseService {

	/**
	 * The singleton instance. Used by the static {@link #mark} to delegate.
	 */
	private static CodeProfilerService						instance;

	/**
	 * Whether live capture is active. Off => {@link #mark} returns immediately.
	 */
	private volatile boolean								active		= false;

	/**
	 * Assigns compact file ids to blueprints.
	 */
	private final AtomicInteger								fileIdSeq	= new AtomicInteger( 0 );

	/**
	 * Registered blueprints by fileId (the id bytecode uses in mark()).
	 */
	private final ConcurrentHashMap<Integer, FileBlueprint>	byFileId	= new ConcurrentHashMap<>();

	/**
	 * Registered blueprints by NORMALIZED file path, for query-by-path.
	 */
	private final ConcurrentHashMap<String, FileBlueprint>	byPath		= new ConcurrentHashMap<>();

	/**
	 * Thread-local timestamp of the last span fired on this thread (probe-charging).
	 */
	private final ThreadLocal<Long>							lastNano	= new ThreadLocal<>();

	/**
	 * Thread-local FileBlueprint that opened the current interval.
	 */
	private final ThreadLocal<FileBlueprint>				lastFile	= new ThreadLocal<>();

	/**
	 * Thread-local span id that opened the current interval.
	 */
	private final ThreadLocal<Integer>						lastSpanId	= new ThreadLocal<>();

	/**
	 * Construct the CodeProfilerService. Called by {@link BoxRuntime} during startup.
	 *
	 * @param runtime the BoxRuntime singleton
	 */
	public CodeProfilerService( BoxRuntime runtime ) {
		super( runtime, Key.codeProfilerService );
		instance = this;
	}

	// -------------------------------------------------------------------------
	// IService lifecycle
	// -------------------------------------------------------------------------

	@Override
	public void onConfigurationLoad() {
		// No config wiring yet.
	}

	@Override
	public void onStartup() {
		// No-op for now.
	}

	@Override
	public void onShutdown( Boolean force ) {
		instance = null;
	}

	// -------------------------------------------------------------------------
	// Hot-path (instrumentation-facing) API
	// -------------------------------------------------------------------------

	/**
	 * Record that one span executed, charging elapsed time since the previous
	 * span on this thread to the span that was current (probe-charging).
	 * <p>
	 * The ONLY method instrumented bytecode calls for a single span. Direct array
	 * increment, no positions, no map lookup, no allocation. Returns immediately
	 * when inactive.
	 *
	 * @param fileId the file id returned by {@link #registerBlueprintForFile} or
	 *               {@link #registerBlueprintForSource} (0-based sequence)
	 * @param spanId the id of the executable span within that file's blueprint
	 *               (0-based; indexes the blueprint's executable span array)
	 */
	public static void mark( int fileId, int spanId ) {
		mark( fileId, new int[] { spanId } );
	}

	/**
	 * Record that SEVERAL spans executed atomically — used for an all-or-nothing
	 * unit (e.g. a function/closure/lambda declaration shell). The whole unit
	 * either runs or does not, so it must open exactly ONE probe-charging interval
	 * and increment all its spans' counts together, with the LAST span owning the
	 * next interval (so a following mark charges the tail to the shell's end).
	 * <p>
	 * This is several {@code mark( fileId, spanId )} calls collapsed into one
	 * bytecode invocation — same semantics, one call instead of N.
	 *
	 * @param fileId  the file id
	 * @param spanIds the ids of all spans covered by this atomic unit, in source
	 *                order; the last becomes the current span for the next interval
	 */
	public static void mark( int fileId, int... spanIds ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active || spanIds.length == 0 ) {
			return;
		}

		long	now			= System.nanoTime();
		Long	prevNano	= service.lastNano.get();

		// Charge the interval since the previous timestamp to the span that opened it.
		if ( prevNano != null && prevNano > 0 && now >= prevNano ) {
			FileBlueprint	prevFile	= service.lastFile.get();
			Integer			prevSpan	= service.lastSpanId.get();
			if ( prevFile != null && prevSpan != null ) {
				SpanStats prev = prevFile.spanStats( prevSpan );
				if ( prev != null ) {
					prev.addNanos( now - prevNano );
				}
			}
		}

		service.lastNano.set( now );

		// The current (last) span now owns the next interval.
		FileBlueprint fb = service.byFileId.get( fileId );
		if ( fb == null ) {
			return;
		}
		service.lastFile.set( fb );

		int	lastId	= spanIds[ spanIds.length - 1 ];
		int	count	= spanIds.length;
		for ( int i = 0; i < count; i++ ) {
			SpanStats stats = fb.spanStats( spanIds[ i ] );
			if ( stats != null ) {
				stats.incrementCount();
			}
		}
		service.lastSpanId.set( lastId );
	}

	/**
	 * Close the probe-charging interval for the CURRENT span (charge the elapsed time
	 * since its mark to it) WITHOUT opening a new interval or incrementing any count.
	 * Emitted at the end of a body so the last span's self-time is not lost.
	 *
	 * @param fileId the file id whose current span interval to close
	 */
	public static void markEnd( int fileId ) {
		CodeProfilerService service = instance;
		if ( service == null || !service.active ) {
			return;
		}

		long	now			= System.nanoTime();
		Long	prevNano	= service.lastNano.get();
		if ( prevNano != null && prevNano > 0 && now >= prevNano ) {
			FileBlueprint	prevFile	= service.lastFile.get();
			Integer			prevSpan	= service.lastSpanId.get();
			if ( prevFile != null && prevSpan != null && prevFile == service.byFileId.get( fileId ) ) {
				SpanStats prev = prevFile.spanStats( prevSpan );
				if ( prev != null ) {
					prev.addNanos( now - prevNano );
				}
			}
		}

		// Close the interval: no new span owns the next interval.
		service.lastNano.remove();
		service.lastFile.remove();
		service.lastSpanId.remove();
	}

	/**
	 * Reset the probe-charging clock for the current thread.
	 */
	public static void resetThreadClock() {
		CodeProfilerService inst = instance;
		if ( inst != null ) {
			inst.lastNano.remove();
			inst.lastFile.remove();
			inst.lastSpanId.remove();
		}
	}

	// -------------------------------------------------------------------------
	// Lifecycle API (non-hot)
	// -------------------------------------------------------------------------

	/**
	 * Turn live capture on or off.
	 *
	 * @param active whether to capture
	 */
	public static void setActive( boolean active ) {
		if ( instance != null ) {
			instance.active = active;
		}
	}

	/**
	 * Whether live capture is currently active.
	 *
	 * @return {@code true} if capturing
	 */
	public static boolean isActive() {
		return instance != null && instance.active;
	}

	/**
	 * Clear all collected data and registered blueprints.
	 */
	public static void reset() {
		if ( instance != null ) {
			instance.byFileId.clear();
			instance.byPath.clear();
			instance.lastNano.remove();
			instance.lastFile.remove();
			instance.lastSpanId.remove();
		}
	}

	/**
	 * Register (or replace) the blueprint for a real FILE. The key is the source
	 * path, normalized internally. Pre-allocates a counter for every executable span.
	 *
	 * @param filePath  the source file path (normalized internally)
	 * @param blueprint the file's blueprint
	 *
	 * @return the compact fileId bytecode uses in {@link #mark}; -1 if no instance
	 */
	public static int registerBlueprintForFile( String filePath, Blueprint blueprint ) {
		return registerBlueprint( normalize( filePath ), blueprint, Blueprint.Kind.FILE );
	}

	/**
	 * Register (or replace) the blueprint for ADHOC SOURCE. The key is the source
	 * hash, passed through untouched (not a filesystem path). Pre-allocates a
	 * counter for every executable span.
	 *
	 * @param sourceHash the hash of the adhoc source
	 * @param blueprint  the source's blueprint
	 *
	 * @return the compact fileId bytecode uses in {@link #mark}; -1 if no instance
	 */
	public static int registerBlueprintForSource( String sourceHash, Blueprint blueprint ) {
		return registerBlueprint( sourceHash, blueprint, Blueprint.Kind.SOURCE );
	}

	/**
	 * Core registration: build the FileBlueprint (keyed by the given opaque key
	 * string, which is already canonical) and pre-allocate per-span counters.
	 */
	private static int registerBlueprint( String key, Blueprint blueprint, Blueprint.Kind kind ) {
		CodeProfilerService service = instance;
		if ( service == null ) {
			return -1;
		}
		FileBlueprint	fb		= new FileBlueprint( key, blueprint );
		int				fileId	= service.fileIdSeq.getAndIncrement();
		service.byFileId.put( fileId, fb );
		service.byPath.put( key, fb );
		return fileId;
	}

	/**
	 * All registered blueprints (by their key) for generic reporting. Each returned
	 * blueprint is self-describing via its {@link Blueprint.Kind}.
	 *
	 * @return map of registered key -> blueprint
	 */
	public static Map<String, Blueprint> trackedBlueprints() {
		Map<String, Blueprint> result = new ConcurrentHashMap<>();
		if ( instance != null ) {
			instance.byPath.forEach( ( key, fb ) -> result.put( key, fb.blueprint() ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Query API (cold path)
	// -------------------------------------------------------------------------

	/**
	 * The executable span containing the given point, or null if the point is in no
	 * executable span (NOT_EXECUTABLE).
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param line     the line to look up (1-based, like all line indexes in this class)
	 * @param col      the column to look up (0-based, matching {@link Blueprint.SpanDef#startCol()})
	 *
	 * @return the executable {@link Span} containing the point, or null if the point
	 *         falls in a gap/non-executable region or the file isn't registered
	 */
	public static Span spanAt( String filePath, int line, int col ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? null : fb.spanAt( line, col );
	}

	/**
	 * The executable span by its id.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param spanId   the span id (0-based; indexes the file's executable span array)
	 *
	 * @return the {@link Span} with that id, or null if the id is out of range or
	 *         the file isn't registered
	 */
	public static Span span( String filePath, int spanId ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? null : fb.span( spanId );
	}

	/**
	 * All executable spans touching the given line.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param line     the line to look up (1-based)
	 *
	 * @return the executable spans on that line, in source order; empty list if none
	 *         or the file isn't registered
	 */
	public static List<Span> spansOnLine( String filePath, int line ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? List.of() : fb.spansOnLine( line );
	}

	/**
	 * All executable spans for a file, in blueprint (source) order.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 *
	 * @return the file's executable spans; empty list if not registered
	 */
	public static List<Span> fileSpans( String filePath ) {
		FileBlueprint fb = fileBlueprint( filePath );
		return fb == null ? List.of() : fb.fileSpans();
	}

	/**
	 * Print EVERY span's source characters to stdout for a blueprint, reading the
	 * source straight from disk: {@code System.out.println( CodeProfilerService.dumpSpans( filePath ) ) }.
	 *
	 * @param filePath the blueprint key (normalized file path or adhoc source hash)
	 *
	 * @return a multi-line string, one {@code spanN (line,col)-(line,col) count=.. [chars] } per span
	 */
	public static String dumpSpans( String filePath ) {
		return dumpSpans( filePath, null );
	}

	/**
	 * Print every span's source characters to stdout, for eyeballing what each span
	 * maps to in a test: {@code System.out.println( CodeProfilerService.dumpSpans( key, source ) ) }.
	 * <p>
	 * If {@code source} is {@code null} and the registered key is a real {@link Blueprint.Kind#FILE}
	 * (a normalized path), the source is read straight from disk — no need to pass it.
	 *
	 * @param filePath the blueprint key (adhoc source hash or normalized file path)
	 * @param source   the source text to slice spans from, or {@code null} to read from disk
	 *
	 * @return a multi-line string, one {@code spanN (line,col)-(line,col) count=.. [chars] } per span
	 */
	public static String dumpSpans( String filePath, String source ) {
		FileBlueprint fb = fileBlueprint( filePath );
		if ( fb == null ) {
			return "<no blueprint for " + filePath + ">";
		}
		String resolvedSource = source;
		if ( resolvedSource == null && fb.kind() == Blueprint.Kind.FILE ) {
			try {
				resolvedSource = java.nio.file.Files.readString( Paths.get( filePath ) );
			} catch ( java.io.IOException e ) {
				return "<cannot read " + filePath + ": " + e.getMessage() + ">";
			}
		}
		StringBuilder	sb		= new StringBuilder();
		List<Span>		spans	= fb.fileSpans();
		int				i		= 0;
		for ( Span s : spans ) {
			String text = resolvedSource == null ? "<no source>"
			    : sliceSource( resolvedSource, s.startLine(), s.startCol(), s.endLine(), s.endCol() );
			sb.append( "span" ).append( i )
			    .append( " (" ).append( s.startLine() ).append( "," ).append( s.startCol() )
			    .append( ")-(" ).append( s.endLine() ).append( "," ).append( s.endCol() )
			    .append( ") count=" ).append( s.stats().count() )
			    .append( " [" ).append( text.replace( "\n", "\\n" ).replace( "\r", "\\r" ) ).append( "]" )
			    .append( '\n' );
			i++;
		}
		return sb.toString();
	}

	/**
	 * The RAW SOURCE CHARACTERS a single executable span covers, for eyeballing what
	 * a span maps to. Span coordinates are 1-based lines and 0-based columns, so this
	 * slices {@code source} (split on {@code \n}) between the span's start and end.
	 * <p>
	 * Add {@code System.out.println( CodeProfilerService.spanSourceText( filePath, source, span ) ) }
	 * to a test to confirm the exact text of a single span.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param source   the original source text (may have trailing newline; handled)
	 * @param span     the span whose characters to extract
	 *
	 * @return the characters the span covers, {@code <no source>} if the source is
	 *         absent, or {@code null} if the span is null
	 */
	public static String spanSourceText( String filePath, String source, Span span ) {
		if ( span == null ) {
			return null;
		}
		if ( source == null ) {
			return "<no source>";
		}
		return sliceSource( source, span.startLine(), span.startCol(), span.endLine(), span.endCol() );
	}

	/**
	 * Split the source into lines and return the substring a (1-based line, 0-based
	 * column) span covers. Multi-line spans join the intervening lines with {@code \n}.
	 */
	private static String sliceSource( String source, int startLine, int startCol, int endLine, int endCol ) {
		String[] lines = source.split( "\n", -1 );
		// Drop a trailing empty element left by a terminal newline in the text block.
		if ( lines.length > 0 && lines[ lines.length - 1 ].isEmpty() ) {
			String[] trimmed = new String[ lines.length - 1 ];
			System.arraycopy( lines, 0, trimmed, 0, trimmed.length );
			lines = trimmed;
		}
		if ( startLine > lines.length || startLine < 1 ) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if ( startLine == endLine ) {
			String line = lines[ startLine - 1 ];
			// Guard against inverted spans (startCol > endCol) or out-of-range endCol
			// so the dump NEVER crashes — it must always reveal the raw spans.
			if ( startCol < line.length() ) {
				if ( endCol >= startCol ) {
					return line.substring( startCol, Math.min( endCol, line.length() ) );
				}
				// Inverted: return from startCol to end of line, flagged.
				return "<inverted " + line.substring( startCol ) + ">";
			}
			return "";
		}
		String first = lines[ startLine - 1 ];
		sb.append( startCol < first.length() ? first.substring( startCol ) : "" );
		for ( int ln = startLine; ln < endLine - 1 && ln < lines.length; ln++ ) {
			sb.append( '\n' ).append( lines[ ln ] );
		}
		if ( endLine <= lines.length && endCol < lines[ endLine - 1 ].length() ) {
			sb.append( '\n' ).append( lines[ endLine - 1 ], 0, endCol );
		}
		return sb.toString();
	}

	/**
	 * Aggregated data for a single line.
	 * <ul>
	 * <li>{@code covered} = any executable span touching the line has count &gt; 0</li>
	 * <li>{@code count} = count of the FIRST/OUTERMOST span touching the line</li>
	 * <li>{@code totalNanos} = SUM of totalNanos over all spans touching the line</li>
	 * </ul>
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 * @param line     the line to aggregate (1-based)
	 *
	 * @return per-line aggregate, or null if no executable span touches the line
	 *         or the file isn't registered
	 */
	public static LineCoverage lineAt( String filePath, int line ) {
		FileBlueprint fb = fileBlueprint( filePath );
		if ( fb == null ) {
			return null;
		}
		List<Span> spans = fb.spansOnLine( line );
		if ( spans.isEmpty() ) {
			return null;
		}
		boolean	covered		= spans.stream().anyMatch( s -> s.stats().count() > 0 );
		long	lineCount	= spans.get( 0 ).stats().count(); // first/outermost span
		long	nanos		= spans.stream().mapToLong( s -> s.stats().totalNanos() ).sum();
		return new LineCoverage( line, covered, nanos, lineCount );
	}

	/**
	 * Per-line aggregates for a file, keyed by line.
	 *
	 * @param filePath the file path or adhoc source-hash key the blueprint was registered under
	 *
	 * @return map of 1-based line number to its {@link LineCoverage}; empty map if
	 *         the file isn't registered
	 */
	public static Map<Integer, LineCoverage> fileLines( String filePath ) {
		FileBlueprint fb = fileBlueprint( filePath );
		if ( fb == null ) {
			return Map.of();
		}
		Map<Integer, LineCoverage> result = new ConcurrentHashMap<>();
		// A multi-line span covers every line in [startLine, endLine] — register a
		// per-line aggregate for EACH of them (not just the start line), so the
		// line-level renderers color all the lines an executable span touches.
		fb.spans().forEach( s -> {
			for ( int line = s.startLine(); line <= s.endLine(); line++ ) {
				result.computeIfAbsent( line, l -> lineAt( fb.filePath, l ) );
			}
		} );
		return result;
	}

	// -------------------------------------------------------------------------
	// Coverage data + export
	// -------------------------------------------------------------------------

	/**
	 * Build PER-LINE coverage for every registered blueprint of the requested
	 * kinds, as NATIVE BoxLang structures ({@link Struct}/{@link Array}) — directly
	 * consumable in BoxLang, and serializable via
	 * {@code CoverageJSONExporter.toJSON(...)}. Delegates to
	 * {@link CoverageDataBuilder#buildLineCoverage(Blueprint.Kind...)}.
	 * <p>
	 * Each file value:
	 * 
	 * <pre>
	 * {
	 *   "filePath"          : &lt;normalized path or source hash&gt;,
	 *   "kind"              : "FILE" | "SOURCE",
	 *   "numExecutableLines": &lt;lines with an executable span&gt;,
	 *   "numCoveredLines"   : &lt;lines with count &gt; 0&gt;,
	 *   "percCoverage"      : &lt;covered / executable, or 1 if none&gt;,
	 *   "lines"             : {
	 *       "&lt;lineNum&gt;"      : { "covered": bool, "count": n, "totalNanos": n }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key
	 */
	public static IStruct buildLineCoverage( Blueprint.Kind... kinds ) {
		return CoverageDataBuilder.buildLineCoverage( kinds );
	}

	/**
	 * Build EVERY SPAN (in blueprint order) for every registered blueprint of the
	 * requested kinds, as NATIVE BoxLang structures. Delegates to
	 * {@link CoverageDataBuilder#buildSpanCoverage(Blueprint.Kind...)}.
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key
	 */
	public static IStruct buildSpanCoverage( Blueprint.Kind... kinds ) {
		return CoverageDataBuilder.buildSpanCoverage( kinds );
	}

	/**
	 * Build the per-line coverage JSON string for every registered blueprint of
	 * the requested kinds (default {@link Blueprint.Kind#FILE}).
	 *
	 * @param kinds the blueprint kinds to include
	 *
	 * @return the JSON string
	 */
	public static String buildLineCoverageJSON( Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.toJSON( buildLineCoverage( kinds ) );
	}

	/**
	 * Build the per-span coverage JSON string for every registered blueprint of
	 * the requested kinds (default {@link Blueprint.Kind#FILE}).
	 *
	 * @param kinds the blueprint kinds to include
	 *
	 * @return the JSON string
	 */
	public static String buildSpanCoverageJSON( Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.toJSON( buildSpanCoverage( kinds ) );
	}

	/**
	 * Write a JSON report of PER-LINE coverage to {@code path}. The shape is
	 * inspired by TestBox's {@code coverageReport.json} but uses objects instead of
	 * a query, and adds timing. Delegates to {@link CoverageJSONExporter}.
	 *
	 * @param path   the absolute or relative output file path
	 * @param pretty whether to pretty-print the JSON
	 * @param kinds  the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written (also available for embedding)
	 */
	public static String writeLineCoverageJSON( String path, boolean pretty, Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.write( path, buildLineCoverage( kinds ), pretty );
	}

	/**
	 * {@link #writeLineCoverageJSON(String, boolean, Blueprint.Kind...)} with
	 * pretty printing on.
	 *
	 * @param path  the output file path
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written
	 */
	public static String writeLineCoverageJSON( String path, Blueprint.Kind... kinds ) {
		return writeLineCoverageJSON( path, true, kinds );
	}

	/**
	 * Write a JSON report of EVERY SPAN in blueprint order to {@code path}.
	 * Delegates to {@link CoverageJSONExporter}.
	 *
	 * @param path   the absolute or relative output file path
	 * @param pretty whether to pretty-print the JSON
	 * @param kinds  the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written
	 */
	public static String writeSpanCoverageJSON( String path, boolean pretty, Blueprint.Kind... kinds ) {
		return CoverageJSONExporter.write( path, buildSpanCoverage( kinds ), pretty );
	}

	/**
	 * {@link #writeSpanCoverageJSON(String, boolean, Blueprint.Kind...)} with
	 * pretty printing on.
	 *
	 * @param path  the output file path
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the JSON string written
	 */
	public static String writeSpanCoverageJSON( String path, Blueprint.Kind... kinds ) {
		return writeSpanCoverageJSON( path, true, kinds );
	}

	/**
	 * Write SonarQube Generic Coverage XML for per-line coverage to {@code path}.
	 * Delegates to {@link CoverageSonarQubeExporter}.
	 *
	 * @param path  the output file path
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the XML string written
	 */
	public static String writeSonarQubeXML( String path, Blueprint.Kind... kinds ) {
		return CoverageSonarQubeExporter.write( buildLineCoverage( kinds ), path );
	}

	/**
	 * Generate SonarQube Generic Coverage XML for per-line coverage.
	 * Delegates to {@link CoverageSonarQubeExporter}.
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return the XML string
	 */
	public static String buildSonarQubeXML( Blueprint.Kind... kinds ) {
		return CoverageSonarQubeExporter.generateXML( buildLineCoverage( kinds ) );
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private static String normalize( String filePath ) {
		Path path = Paths.get( filePath ).toAbsolutePath().normalize();
		return path.toString().toLowerCase( Locale.ROOT );
	}

	private static FileBlueprint fileBlueprint( String key ) {
		if ( instance == null ) {
			return null;
		}
		FileBlueprint fb = instance.byPath.get( key );
		if ( fb == null ) {
			// Not an exact key match; maybe a real path needing normalization.
			fb = instance.byPath.get( normalize( key ) );
		}
		return fb;
	}

	/**
	 * Immutable descriptor + mutable stats of an executable span.
	 */
	public record Span(
	    int id,
	    int startLine,
	    int startCol,
	    int endLine,
	    int endCol,
	    SpanStats stats ) {
	}

	/**
	 * Mutable per-span runtime counters, mutated by {@link #mark} and read by tools.
	 */
	public static final class SpanStats {

		private final AtomicLong	count		= new AtomicLong();
		private final AtomicLong	totalNanos	= new AtomicLong();

		public long count() {
			return count.get();
		}

		public long totalNanos() {
			return totalNanos.get();
		}

		void incrementCount() {
			count.incrementAndGet();
		}

		void addNanos( long delta ) {
			totalNanos.addAndGet( delta );
		}

		@Override
		public String toString() {
			return "count=" + count.get() + ", nanos=" + totalNanos.get();
		}
	}

	/**
	 * Immutable per-line aggregate.
	 */
	public record LineCoverage(
	    int line,
	    boolean covered,
	    long totalNanos,
	    long count ) {
	}

	/**
	 * A registered file's blueprint + its pre-allocated span/counter storage.
	 */
	static final class FileBlueprint {

		final String							filePath;
		private final Blueprint					blueprint;
		private final Span[]					spans;	// executable spans by id
		private final Map<Integer, List<Span>>	byLine	= new ConcurrentHashMap<>();

		Blueprint.Kind kind() {
			return blueprint.kind();
		}

		FileBlueprint( String filePath, Blueprint blueprint ) {
			this.filePath	= filePath;
			this.blueprint	= blueprint;
			// Allocate counters for executable spans (span id = its position in the array).
			int executableCount = ( int ) blueprint.spans().stream().filter( Blueprint.SpanDef::executable ).count();
			this.spans = new Span[ executableCount ];
			int[] idx = { 0 };
			blueprint.spans().forEach( b -> {
				if ( b.executable() ) {
					Span sp = new Span( idx[ 0 ], b.startLine(), b.startCol(), b.endLine(), b.endCol(), new SpanStats() );
					this.spans[ idx[ 0 ]++ ] = sp;
					for ( int line = b.startLine(); line <= b.endLine(); line++ ) {
						this.byLine.computeIfAbsent( line, k -> new ArrayList<>() ).add( sp );
					}
				}
			} );
			this.byLine.values().forEach( l -> l.sort( ( a, c ) -> Integer.compare( a.startCol(), c.startCol() ) ) );
		}

		Span span( int spanId ) {
			return spanId >= 0 && spanId < spans.length ? spans[ spanId ] : null;
		}

		SpanStats spanStats( int spanId ) {
			Span s = span( spanId );
			return s == null ? null : s.stats();
		}

		Span spanAt( int line, int col ) {
			List<Span> lineSpans = byLine.get( line );
			if ( lineSpans == null ) {
				return null;
			}
			// A column that begins an inner span owns the point at that boundary
			// (e.g. col 34 starts the "2" branch of "1 : 2" even though it is also
			// the inclusive end of the "1 : " span). A span only "starts" the
			// boundary if its START LINE is the queried line — a multi-line span
			// (e.g. a function shell that ends on the body's line) must not claim
			// a column on a line it merely passes through.
			Span boundary = null;
			for ( Span s : lineSpans ) {
				if ( s.startLine() == line && col == s.startCol() ) {
					boundary = s;
					break;
				}
			}
			if ( boundary != null ) {
				return boundary;
			}
			for ( Span s : lineSpans ) {
				if ( col >= s.startCol() && col <= s.endCol() ) {
					return s;
				}
			}
			return null;
		}

		List<Span> spansOnLine( int line ) {
			return byLine.getOrDefault( line, List.of() );
		}

		List<Span> fileSpans() {
			return java.util.Arrays.asList( spans );
		}

		List<Span> spans() {
			return java.util.Arrays.asList( spans );
		}

		Blueprint blueprint() {
			return blueprint;
		}
	}
}