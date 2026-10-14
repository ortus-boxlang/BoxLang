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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * DEBUG TOOLING — renders a profiled source file as a SPAN ONLY heat map: each
 * executable span is shaded a hotter (darker) red the more "extreme" its chosen
 * metric is. No line-level aggregation — this is purely about which SPANS dominate.
 * <p>
 * Three metrics are supported (see {@link Metric}):
 * <ul>
 * <li>{@code TIME_TOTAL} — total self time spent in the span (summed over runs).</li>
 * <li>{@code TIME_PER_RUN} — self time divided by the span's execution count, so a
 * span that ran once for 900ms outranks one that ran 1000× for 1ms each (both equal
 * in total, but very different per invocation).</li>
 * <li>{@code COUNT} — raw execution count.</li>
 * </ul>
 * <p>
 * Shading scale:
 * <ul>
 * <li><b>Relative (default)</b> — normalized against the maximum metric value among
 * the spans on THIS page. Best for "what stands out in this file".</li>
 * <li><b>Fixed</b> — normalized against a caller-supplied maximum, so multiple files
 * can be compared on one absolute scale.</li>
 * </ul>
 * Spans with a zero metric are shown in a neutral gray so their boundaries are still
 * visible.
 *
 * <pre>
 * 
 * // hottest by total time, relative scale
 * String html = SpanHeatmapHTMLRenderer.renderHTML( service, fileKey, null, Metric.TIME_TOTAL );
 * // hottest by per-run cost, fixed 50ms scale
 * String html = SpanHeatmapHTMLRenderer.renderHTML( service, fileKey, null, Metric.TIME_PER_RUN, 50_000_000L );
 * </pre>
 */
public final class SpanHeatmapHTMLRenderer {

	/**
	 * No construction - static utility.
	 */
	private SpanHeatmapHTMLRenderer() {
	}

	/**
	 * Absolute shading floor for TIME metrics: a span under this (50ms) is treated as
	 * "basically nothing" and stays barely tinted no matter how slow the page's worst
	 * span is.
	 */
	private static final long	TIME_FLOOR_NANOS	= 50L * 1_000_000L;

	/**
	 * Absolute shading floor for the COUNT metric: a span that ran this many times
	 * (5) or fewer is treated as "basically nothing".
	 */
	private static final long	COUNT_FLOOR			= 5L;

	/** Bare-tint opacity for spans at or below the floor (and for uniform pages). */
	private static final double	BASE_ALPHA			= 0.12;

	/** Deepest-red opacity for the hottest span on the page. */
	private static final double	HOT_ALPHA			= 0.95;

	/**
	 * The metric a heat map shades by.
	 */
	public enum Metric {

		/** Total self time summed over all runs. */
		TIME_TOTAL( "Total time" ),
		/** Self time divided by execution count (cost per run). */
		TIME_PER_RUN( "Time per run" ),
		/** Raw execution count. */
		COUNT( "Execution count" );

		private final String label;

		Metric( String label ) {
			this.label = label;
		}

		/**
		 * Human-readable label for the metric.
		 *
		 * @return the label
		 */
		public String label() {
			return this.label;
		}
	}

	/**
	 * Render a heat map with a RELATIVE scale (normalized to the page's max metric).
	 *
	 * @param service the profiler service to read spans from
	 * @param fileKey the normalized blueprint key
	 * @param title   optional title; defaults to the file key
	 * @param metric  the metric to shade by
	 *
	 * @return the full HTML document string
	 */
	public static String renderHTML( CodeProfilerService service, String fileKey, String title, Metric metric ) {
		return renderHTML( service, fileKey, title, metric, 0L );
	}

	/**
	 * Render a heat map.
	 *
	 * @param service       the profiler service to read spans from
	 * @param fileKey       the normalized blueprint key
	 * @param title         optional title; defaults to the file key
	 * @param metric        the metric to shade by
	 * @param fixedScaleMax when &gt; 0, normalize against this fixed maximum (comparable
	 *                      across files); when &le; 0, normalize against the page maximum
	 *
	 * @return the full HTML document string
	 */
	public static String renderHTML( CodeProfilerService service, String fileKey, String title, Metric metric, long fixedScaleMax ) {
		List<CodeProfilerService.Span>	allSpans	= service.fileSpans( fileKey );
		String							source		= readSource( fileKey );
		List<int[]>						bounds		= lineBounds( source );

		// Metric per span + the page maximum.
		long[]							metrics		= new long[ allSpans.size() ];
		long							max			= 0L;
		for ( int i = 0; i < allSpans.size(); i++ ) {
			metrics[ i ]	= metricValue( allSpans.get( i ), metric );
			max				= Math.max( max, metrics[ i ] );
		}

		boolean	relative	= fixedScaleMax <= 0L;
		long	scaleMax	= relative ? max : fixedScaleMax;
		if ( scaleMax <= 0L ) {
			scaleMax = 1L; // avoid divide-by-zero on an all-zero page
		}

		// ABSOLUTE floor: anything below this is "basically nothing" and stays a barely
		// tinted red REGARDLESS of how slow the slowest span on the page is. Times under
		// 50ms and counts of 5 or fewer are noise. Shading above the floor is scaled from
		// the floor to the page maximum, so when every span sits below the floor they are
		// ALL uniformly light (never all-dark).
		long					floor		= metric == Metric.COUNT ? COUNT_FLOOR : TIME_FLOOR_NANOS;

		String					scaleNote	= relative
		    ? "relative (normalized to the hottest span on this page)"
		    : "fixed (normalized to a caller-supplied maximum: " + formatMetric( scaleMax, metric ) + ")";

		// CODE VIEW: the full source, with each span heat-shaded IN PLACE.
		String[]				srcLines	= splitLines( source );
		List<List<Object[]>>	lineSpans	= new ArrayList<>();
		for ( int i = 0; i < srcLines.length; i++ ) {
			lineSpans.add( new ArrayList<>() );
		}
		for ( int si = 0; si < allSpans.size(); si++ ) {
			CodeProfilerService.Span s = allSpans.get( si );
			for ( int ln = s.startLine(); ln <= s.endLine() && ln <= srcLines.length; ln++ ) {
				if ( ln >= 1 ) {
					lineSpans.get( ln - 1 ).add( new Object[] { s, si } );
				}
			}
		}
		StringBuilder codeRows = new StringBuilder();
		for ( int i = 0; i < srcLines.length; i++ ) {
			String	line		= srcLines[ i ];
			int		lineNo		= i + 1;
			int[]	ownerIdx	= new int[ line.length() ];
			java.util.Arrays.fill( ownerIdx, -1 );
			int lineStart = offset( bounds, lineNo, 0 );
			for ( Object[] pair : lineSpans.get( i ) ) {
				CodeProfilerService.Span	s		= ( CodeProfilerService.Span ) pair[ 0 ];
				int							si		= ( int ) pair[ 1 ];
				int							sLocal	= Math.max( 0, offset( bounds, s.startLine(), s.startCol() ) - lineStart );
				int							eLocal	= Math.min( line.length(), offset( bounds, s.endLine(), s.endCol() ) - lineStart );
				for ( int c = sLocal; c < eLocal; c++ ) {
					ownerIdx[ c ] = si;
				}
			}
			StringBuilder	sb		= new StringBuilder();
			int				cursor	= 0;
			while ( cursor < line.length() ) {
				int	si		= ownerIdx[ cursor ];
				int	runEnd	= cursor + 1;
				while ( runEnd < line.length() && ownerIdx[ runEnd ] == si ) {
					runEnd++;
				}
				String text = escape( line.substring( cursor, runEnd ) );
				if ( si < 0 ) {
					sb.append( text );
				} else {
					CodeProfilerService.Span	s		= allSpans.get( si );
					String						style	= spanStyle( metrics[ si ], floor, scaleMax );
					if ( style.isEmpty() ) {
						sb.append( text );
					} else {
						sb.append( "<span style=\"" ).append( style ).append( "\"" ).append( tooltip( s ) ).append( ">" )
						    .append( text ).append( "</span>" );
					}
				}
				cursor = runEnd;
			}
			codeRows.append( "  <tr><td class=\"ln\">" ).append( lineNo ).append( "</td><td class=\"c\">" )
			    .append( sb.length() == 0 ? "&nbsp;" : sb ).append( "</td></tr>\n" );
		}

		StringBuilder html = new StringBuilder();
		html.append( "<html><head><meta charset=\"utf-8\"><title>" ).append( escape( title != null ? title : fileKey ) ).append( "</title><style>\n" )
		    .append( "body{font-family:'SF Mono',Consolas,monospace;margin:24px;background:#fff;color:#111;}\n" )
		    .append( "h3{padding-left:8px;}\n" )
		    .append( "table.code{border-collapse:collapse;font-size:14px;line-height:1.5;}\n" )
		    .append( "table.code td.ln{text-align:right;padding:0 10px 0 0;color:#999;border-right:1px solid #ddd;user-select:none;min-width:3ch;}\n" )
		    .append( "table.code td.c{white-space:pre;padding:0 0 0 10px;}\n" )
		    .append( "tr:nth-child(odd) td{background:#fafafa;}\n" )
		    .append( "span.sp{border-radius:2px;}\n" )
		    .append(
		        ".legend{display:inline-block;width:220px;height:14px;vertical-align:middle;border:1px solid #ccc;background:linear-gradient(90deg,rgba(211,47,47,.12),rgba(211,47,47,.95));}\n" )
		    .append( "</style>\n</head>\n<body>\n" )
		    .append( "<h3>" ).append( escape( fileKey ) ).append( "</h3>\n" )
		    .append( "<p style=\"padding-left:8px;margin:0 0 4px 0\"><b>Span heat map by " ).append( metric.label() )
		    .append( "</b> &mdash; " ).append( scaleNote ).append( ". Hover a span for its count and time.</p>\n" )
		    // Color scale legend: light (fast/few) -> dark (slow/many).
		    .append( "<p style=\"padding-left:8px;margin:0 0 12px 0\">light <span class=\"legend\"></span> dark &nbsp; (max on page: " )
		    .append( formatMetric( max, metric ) ).append( "; spans under " )
		    .append( metric == Metric.COUNT ? COUNT_FLOOR + " runs" : "50ms" ).append( " stay barely tinted)</p>\n" );

		html.append( "<table class=\"code\">\n" ).append( codeRows ).append( "</table>\n" )
		    .append( "</body>\n</html>\n" );
		return html.toString();
	}

	/**
	 * A {@code title} tooltip carrying the span's count + time stats for mouse-over.
	 */
	private static String tooltip( CodeProfilerService.Span s ) {
		long	count	= s.stats().count();
		long	total	= s.stats().totalNanos();
		long	perRun	= count == 0 ? 0L : total / count;
		String	incl	= s.stats().inclusiveNanos() > 0
		    ? ", inclusive=" + SpanHTMLRenderer.formatDuration( s.stats().inclusiveNanos() )
		    : "";
		return " title=\"count=" + count + ", total=" + SpanHTMLRenderer.formatDuration( total )
		    + ", per-run=" + SpanHTMLRenderer.formatDuration( perRun ) + incl + "\"";
	}

	/**
	 * The inline CSS for a span in the code view. Spans that never ran are uncolored.
	 * Spans at or below the absolute {@code floor} (time &lt; 50ms, or count &le; 5)
	 * are "basically nothing" and get only a bare tint, regardless of the page max.
	 * Above the floor the shade scales from the floor to {@code scaleMax}, so a page
	 * where EVERY span is below the floor stays uniformly light instead of all-dark.
	 *
	 * @param metric   the span's metric value
	 * @param floor    the absolute "basically nothing" threshold
	 * @param scaleMax the page maximum (or the caller's fixed maximum)
	 *
	 * @return the inline style, or empty for a span that never ran
	 */
	private static String spanStyle( long metric, long floor, long scaleMax ) {
		if ( metric <= 0L ) {
			return "";
		}
		// Below the floor: a bare tint; nothing to shout about.
		if ( metric <= floor || scaleMax <= floor ) {
			return String.format( "background:rgba(211,47,47,%.3f);color:#111;", BASE_ALPHA );
		}
		double	t		= clamp01( ( double ) ( metric - floor ) / ( double ) ( scaleMax - floor ) );
		double	alpha	= BASE_ALPHA + ( HOT_ALPHA - BASE_ALPHA ) * t;
		String	fg		= alpha > 0.55 ? "#fff" : "#111";
		return String.format( "background:rgba(211,47,47,%.3f);color:%s;", alpha, fg );
	}

	/**
	 * Render a heat map and write it to {@code outputPath}, creating parent
	 * directories as needed.
	 *
	 * @param service       the profiler service to read spans from
	 * @param fileKey       the normalized blueprint key
	 * @param outputPath    the output HTML file path
	 * @param title         an optional HTML title
	 * @param metric        the metric to shade by
	 * @param fixedScaleMax when &gt; 0, normalize against this fixed maximum; else relative
	 *
	 * @return the output path written
	 */
	public static Path renderToFile( CodeProfilerService service, String fileKey, String outputPath, String title, Metric metric, long fixedScaleMax ) {
		String	html	= renderHTML( service, fileKey, title, metric, fixedScaleMax );
		Path	out		= Paths.get( outputPath ).toAbsolutePath().normalize();
		try {
			Files.createDirectories( out.getParent() );
			Files.writeString( out, html );
		} catch ( java.io.IOException e ) {
			throw new RuntimeException( "Failed to write span heat map HTML to [" + out + "]", e );
		}
		return out;
	}

	/**
	 * The metric value for a span under the given metric.
	 */
	private static long metricValue( CodeProfilerService.Span s, Metric metric ) {
		return switch ( metric ) {
			case TIME_TOTAL -> s.stats().totalNanos();
			case TIME_PER_RUN -> s.stats().count() == 0 ? 0L : s.stats().totalNanos() / s.stats().count();
			case COUNT -> s.stats().count();
		};
	}

	/** Format a metric value for display. */
	private static String formatMetric( long value, Metric metric ) {
		return metric == Metric.COUNT ? String.valueOf( value ) : SpanHTMLRenderer.formatDuration( value );
	}

	private static double clamp01( double v ) {
		return v < 0 ? 0 : ( v > 1 ? 1 : v );
	}

	/** Read the source for a FILE blueprint from disk, normalizing line endings. */
	private static String readSource( String fileKey ) {
		try {
			return Files.readString( Paths.get( fileKey ) ).replace( "\r\n", "\n" ).replace( "\r", "\n" );
		} catch ( java.io.IOException e ) {
			return "<unable to read source for " + fileKey + ": " + e.getMessage() + ">";
		}
	}

	/** Char offset of (1-based line, 0-based col). */
	private static int offset( List<int[]> bounds, int line, int col ) {
		for ( int[] b : bounds ) {
			if ( b[ 0 ] == line ) {
				return b[ 1 ] + col;
			}
		}
		return col;
	}

	/** Start char offset of each (1-based) line. */
	private static List<int[]> lineBounds( String source ) {
		List<int[]>	result	= new ArrayList<>();
		int			off		= 0;
		result.add( new int[] { 1, 0 } );
		for ( int i = 0; i < source.length(); i++ ) {
			if ( source.charAt( i ) == '\n' ) {
				off = i + 1;
				result.add( new int[] { result.size() + 1, off } );
			}
		}
		return result;
	}

	/** Split the source into lines (LF-based; drop a trailing empty terminal line). */
	private static String[] splitLines( String source ) {
		String[] lines = source.split( "\n", -1 );
		if ( lines.length > 0 && lines[ lines.length - 1 ].isEmpty() ) {
			String[] trimmed = new String[ lines.length - 1 ];
			System.arraycopy( lines, 0, trimmed, 0, trimmed.length );
			lines = trimmed;
		}
		return lines;
	}

	private static String escape( String s ) {
		return s.replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" );
	}
}