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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * DEBUG TOOLING — renders a profiled source file to an HTML file where every
 * executable span is wrapped in a background-highlighted {@code <span>}: covered
 * spans (count &gt; 0) green, missed spans (count == 0) red, so you can eyeball
 * span boundaries against the source.
 * <p>
 * Standalone and intentionally NOT part of the profiler service API. The caller is
 * responsible for having already run the file under the profiler
 * ({@link CodeProfilerService}), so the spans are tracked; this class only reads the
 * tracked spans and the file's source and writes the HTML.
 *
 * <pre>
 * String html = SpanHTMLRenderer.renderHTML( "/abs/path/to/file.bx" );
 * SpanHTMLRenderer.renderToFile( "/abs/path/to/file.bx", "build/out.html" );
 * </pre>
 */
public final class SpanHTMLRenderer {

	/**
	 * No construction - static utility.
	 */
	private SpanHTMLRenderer() {
	}

	/**
	 * Render one tracked source file to HTML with span annotations, keyed by the
	 * given (already-normalized) blueprint key.
	 *
	 * @param fileKey the normalized blueprint key registered with
	 *                {@link CodeProfilerService} (lowercased absolute path)
	 *
	 * @return the full HTML document string
	 */
	public static String renderHTML( String fileKey ) {
		return renderHTML( fileKey, null );
	}

	/**
	 * Render one tracked source file to HTML with span annotations.
	 *
	 * @param fileKey the normalized blueprint key registered with
	 *                {@link CodeProfilerService}
	 * @param title   an optional HTML title; defaults to the file key
	 *
	 * @return the full HTML document string
	 */
	public static String renderHTML( String fileKey, String title ) {
		List<CodeProfilerService.Span>	allSpans	= CodeProfilerService.fileSpans( fileKey );
		String							source		= readSource( fileKey );

		// [startChar, endChar, span]
		List<int[]>						bounds		= lineBounds( source );
		List<Object[]>					spans		= new ArrayList<>();
		for ( CodeProfilerService.Span s : allSpans ) {
			int	start	= offset( bounds, s.startLine(), s.startCol() );
			int	end		= offset( bounds, s.endLine(), s.endCol() );
			spans.add( new Object[] { start, end, s } );
		}
		spans.sort( Comparator.comparingInt( a -> ( int ) a[ 0 ] ) );

		// Per-line mouse-over metrics (tooltip): covered flag, the FIRST/OUTERMOST span's
		// count (same semantics as CodeProfilerService.lineAt, so hovering a line
		// shows the line's coverage count, not a summed total across all its spans),
		// and the total time summed across touching spans. Iterate the SORTED spans
		// so "first" = earliest/outermost in source.
		Map<Integer, long[]>	lineMetrics	= new LinkedHashMap<>(); // line -> [covered01, count, 0]
		Map<Integer, Long>		lineNanos	= new LinkedHashMap<>();
		for ( Object[] sp : spans ) {
			CodeProfilerService.Span s = ( CodeProfilerService.Span ) sp[ 2 ];
			for ( int ln = s.startLine(); ln <= s.endLine(); ln++ ) {
				long[] m = lineMetrics.computeIfAbsent( ln, k -> new long[] { 0, Long.MIN_VALUE, 0 } );
				m[ 0 ] = m[ 0 ] == 1 || s.stats().count() > 0 ? 1 : 0;
				// Keep the FIRST span's count (smallest start) to match lineAt.
				if ( m[ 1 ] == Long.MIN_VALUE ) {
					m[ 1 ] = s.stats().count();
				}
				long nanos = lineNanos.getOrDefault( ln, 0L ) + s.stats().totalNanos();
				lineNanos.put( ln, nanos );
			}
		}

		// Build per-line span segments so the color lives on <span> tags, but extends
		// to the START and END of each line that has any span (colour only within
		// that line — never bleeding across newlines, and never onto lines with no
		// spans, so comments stay uncolored).
		// For each line we compute a list of colored segments: untracked leading
		// code gets the first span's colour, untracked code between spans gets the
		// previous span's colour, untracked trailing code gets the last span's
		// colour.
		// The code cell is a flex container; a final GROWING pad span stretches to
		// fill whatever horizontal space remains in the row, so the colour ALWAYS
		// reaches the right edge of the rendered line — no brittle nbsp counting
		// against a guessed max width (which miscounts tabs, since white-space:pre
		// renders a tab wider than one monospace column).
		Map<Integer, List<Object[]>> lineSpans = new LinkedHashMap<>(); // line -> [ [startChar,endChar,covered01,count,spanNanos], ... ]
		for ( Object[] sp : spans ) {
			int							start	= ( int ) sp[ 0 ];
			int							end		= ( int ) sp[ 1 ];
			CodeProfilerService.Span	span	= ( CodeProfilerService.Span ) sp[ 2 ];
			int							cov		= span.stats().count() > 0 ? 1 : 0;
			for ( int ln = span.startLine(); ln <= span.endLine(); ln++ ) {
				lineSpans.computeIfAbsent( ln, k -> new ArrayList<>() )
				    .add( new Object[] { start, end, cov, span.stats().count(), span.stats().totalNanos() } );
			}
		}
		lineSpans.values().forEach( l -> l.sort( Comparator.comparingInt( a -> ( int ) a[ 0 ] ) ) );

		String[]		srcLines	= splitLines( source );
		// Each line with an executable span renders as inline spans: REAL spans
		// coloured exactly over their source text (boundaries precise), plus PAD
		// spans that carry the fill over untracked leading/between/trailing code and
		// right-edge nbsp so the fill reaches the edges. Hiding ".pad" (fill toggle)
		// leaves only the exact spans.
		// Build the full code-cell HTML per source line.
		List<String>	lineCells	= new ArrayList<>();
		for ( int i = 0; i < srcLines.length; i++ ) {
			int				lineNum		= i + 1;
			int				lineStart	= offset( bounds, lineNum, 0 );
			List<Object[]>	ls			= lineSpans.get( lineNum );
			String			escaped		= escape( srcLines[ i ] );
			if ( ls == null || ls.isEmpty() || srcLines[ i ].isEmpty() ) {
				// No executable spans on this line (or it's empty) — plain and
				// uncolored. A transparent fill span keeps the flex row's code
				// columns aligned with the spanned lines' real spans (both start
				// at the same left edge) and pushes nothing extra.
				lineCells.add( "<td class=\"c\">" + escaped + "<span class=\"fill\"></span></td>" );
				continue;
			}
			// Mouse-over tooltip: covered, total count, total time for this line.
			String	tooltip	= "";
			long[]	lm		= lineMetrics.get( lineNum );
			long	nanos	= lineNanos.getOrDefault( lineNum, 0L );
			if ( lm != null ) {
				tooltip = " title=\"" + ( lm[ 0 ] == 1 ? "covered" : "missed" ) + ", count=" + lm[ 1 ] + ", time="
				    + formatDuration( nanos ) + "\"";
			}
			int		lineEnd		= srcLines[ i ].length();
			// Colour per character index: 1 = covered, 0 = missed. Untracked
			// leading adopts the first span's colour, between/trailing the nearest
			// preceding colour, so the whole line resolves to one of the two.
			int[]	charColor	= new int[ lineEnd ];
			java.util.Arrays.fill( charColor, -1 );
			for ( Object[] spg : ls ) {
				int	sLocal	= Math.max( 0, ( int ) spg[ 0 ] - lineStart );
				int	eLocal	= Math.min( lineEnd, ( int ) spg[ 1 ] - lineStart );
				for ( int c = sLocal; c < eLocal; c++ ) {
					charColor[ c ] = ( int ) spg[ 2 ];
				}
			}
			int prevCov = ( int ) ls.get( 0 )[ 2 ];
			for ( int c = 0; c < lineEnd; c++ ) {
				if ( charColor[ c ] == -1 ) {
					charColor[ c ] = prevCov;
				} else {
					prevCov = charColor[ c ];
				}
			}
			// Distinguish REAL span characters (inside an actual executable span,
			// so their boundaries are exact) from PAD characters (untracked leading
			// / between / trailing code that merely carries the fill to the edges).
			boolean[] isReal = new boolean[ lineEnd ];
			for ( Object[] spg : ls ) {
				int	sLocal	= Math.max( 0, ( int ) spg[ 0 ] - lineStart );
				int	eLocal	= Math.min( lineEnd, ( int ) spg[ 1 ] - lineStart );
				for ( int c = sLocal; c < eLocal; c++ ) {
					isReal[ c ] = true;
				}
			}

			// Build the code cell as PLAIN SOLID inline spans (no gradients):
			// real spans coloured exactly over their text, and pad spans (untracked
			// leading/between/trailing code + trailing nbsp) extending the fill to
			// the right edge. The "pad" class lets the toggle hide just the pads so
			// you can see the exact real-span boundaries. Every colour is solid.
			// Each REAL span carries a per-span tooltip (its own count + time) so
			// hovering "i < 3" shows the condition ran 4x even though the line's
			// cell tooltip shows the FIRST/outermost span's count (lineAt semantics).
			// charOwner[c] = index into ls of the span that owns this real char (-1 = pad).
			int[] charOwner = new int[ lineEnd ];
			java.util.Arrays.fill( charOwner, -1 );
			for ( int si = 0; si < ls.size(); si++ ) {
				Object[]	spg		= ls.get( si );
				int			sLocal	= Math.max( 0, ( int ) spg[ 0 ] - lineStart );
				int			eLocal	= Math.min( lineEnd, ( int ) spg[ 1 ] - lineStart );
				for ( int c = sLocal; c < eLocal; c++ ) {
					charOwner[ c ] = si;
				}
			}
			StringBuilder	sb		= new StringBuilder();
			int				cursor	= 0;
			while ( cursor < lineEnd ) {
				int		color	= charColor[ cursor ];
				boolean	real	= isReal[ cursor ];
				int		owner	= charOwner[ cursor ];
				int		runEnd	= cursor + 1;
				// Break runs on BOTH color/pad changes AND span-owner changes so
				// adjacent real spans (e.g. a while header and its condition) keep
				// their own elements and per-span tooltips.
				while ( runEnd < lineEnd && charColor[ runEnd ] == color && isReal[ runEnd ] == real
				    && ( !real || charOwner[ runEnd ] == owner ) ) {
					runEnd++;
				}
				String	cls		= color == 1 ? "cov" : "miss";
				String	extra	= real ? "" : " pad";
				String	tt		= "";
				if ( real && charOwner[ cursor ] >= 0 ) {
					Object[]	spg	= ls.get( charOwner[ cursor ] );
					long		cnt	= ( long ) spg[ 3 ];
					long		ns	= ( long ) spg[ 4 ];
					tt = " title=\"count=" + cnt + ", time=" + formatDuration( ns ) + "\"";
				}
				sb.append( "<span class=\"" ).append( cls ).append( extra ).append( "\"" ).append( tt ).append( ">" )
				    .append( escape( srcLines[ i ].substring( cursor, runEnd ) ) ).append( "</span>" );
				cursor = runEnd;
			}
			// Trailing pad: a growth span that the flex CSS stretches to fill the
			// rest of the row, in the line's last colour. flex-grow guarantees the
			// colour reaches the right edge regardless of tabs or browser width.
			{
				String tailCls = prevCov == 1 ? "cov" : "miss";
				sb.append( "<span class=\"" ).append( tailCls ).append( " pad fill\"></span>" );
			}
			lineCells.add( "<td class=\"c\"" + tooltip + ">" + sb + "</td>" );
		}

		StringBuilder html = new StringBuilder();
		html.append( "<html><head><meta charset=\"utf-8\"><title>" ).append( escape( title != null ? title : fileKey ) ).append( "</title><style>" )
		    .append( "body{font-family:'SF Mono',Consolas,monospace;margin:24px;background:#fff;color:#111;} " )
		    .append( "h3{padding-left:8px;} table.code{border-collapse:collapse;font-size:14px;line-height:1.5;} " )
		    .append( "td.ln{text-align:right;padding:0 10px 0 0;color:#999;border-right:1px solid #ddd;user-select:none;min-width:3ch;} " )
		    .append( "td.c{white-space:pre;padding:0 0 0 10px;} " )
		    .append( "tr.odd td.ln{background:#fafafa;} tr.odd td.c{background:#fafafa;} " )
		    .append( "tr.cov td.c{background:rgba(10,220,90,.28);} " )
		    .append( "tr.miss td.c{background:rgba(230,60,60,.25);} " )
		    // Two-tone: REAL spans get a saturated color; PAD spans (the untracked
		    // fill to the line edges) get a LIGHTER shade of the same hue so you can
		    // tell exact span boundaries from the fill. All solid colors.
		    // The code cell is a FLEX ROW; the trailing `fill` span flex-grows to fill
		    // the remaining row width, so the colour ALWAYS reaches the right edge
		    // (tabs / browser width no longer matter). `align-items:stretch` makes the
		    // fill span's background fill the full row HEIGHT (fixing the earlier
		    // height:0px — children use fit-content height, not 100% of an auto-height
		    // flex parent).
		    .append( "td.c{display:flex;align-items:stretch;} " )
		    .append( "td.c span.cov,td.c span.miss,td.c span.pad{display:inline-block;} " )
		    .append( "td.c span.fill{flex:1 1 auto;} " )
		    .append( "span.cov{background:rgba(10,220,90,.32);} " )
		    .append( "span.miss{background:rgba(230,60,60,.30);} " )
		    .append( "span.pad.cov{background:rgba(10,220,90,.18);} " )
		    .append( "span.pad.miss{background:rgba(230,60,60,.12);} " )
		    // Checkbox OFF: only the PAD BACKGROUNDS become transparent — the text
		    // (including the actual code in a pad span) stays fully visible, just
		    // uncolored, so you can inspect exact real-span boundaries.
		    .append( "body.nofill span.pad{background:transparent!important;} " )
		    .append( "</style>" ).append( '\n' ).append( "</head>" ).append( '\n' ).append( "<body>" ).append( '\n' )
		    // Fill toggle. Default ON (two-tone fill to the edges). Uncheck to clear
		    // only the pad fills and view the exact real-span boundaries.
		    .append( "<p style=\"padding-left:8px;margin:0 0 8px 0\">" )
		    .append(
		        "<label><input type=\"checkbox\" id=\"fillToggle\" checked onchange=\"document.body.classList.toggle('nofill',!this.checked)\"> Show fill colors</label>" )
		    .append( "</p>" ).append( '\n' )
		    .append( "<h3>" ).append( escape( fileKey ) ).append( "</h3>" ).append( '\n' )
		    .append(
		        "<p style=\"padding-left:8px\"><span class=\"cov\">covered</span>&nbsp;&nbsp;<span class=\"miss\">missed</span>&nbsp;(uncolored = no executable spans)</p>" )
		    .append( '\n' ).append( "<table class=\"code\">" ).append( '\n' );

		for ( int i = 0; i < lineCells.size(); i++ ) {
			String rowCls = ( i + 1 ) % 2 == 0 ? "" : "odd";
			html.append( "  <tr class=\"" ).append( rowCls ).append( "\">" )
			    .append( "<td class=\"ln\">" ).append( i + 1 ).append( "</td>" )
			    .append( lineCells.get( i ) ).append( "</tr>" ).append( '\n' );
		}
		html.append( "</table>" ).append( '\n' ).append( "</body>" ).append( '\n' ).append( "</html>" ).append( '\n' );
		return html.toString();
	}

	/**
	 * Render a tracked source file and write the HTML to {@code outputPath},
	 * creating parent directories as needed.
	 *
	 * @param fileKey    the normalized blueprint key
	 * @param outputPath the output HTML file path
	 *
	 * @return the output path written
	 */
	public static Path renderToFile( String fileKey, String outputPath ) {
		return renderToFile( fileKey, outputPath, null );
	}

	/**
	 * Render a tracked source file and write the HTML to {@code outputPath}.
	 *
	 * @param fileKey    the normalized blueprint key
	 * @param outputPath the output HTML file path
	 * @param title      an optional HTML title
	 *
	 * @return the output path written
	 */
	public static Path renderToFile( String fileKey, String outputPath, String title ) {
		String	html	= renderHTML( fileKey, title );
		Path	out		= Paths.get( outputPath ).toAbsolutePath().normalize();
		try {
			Files.createDirectories( out.getParent() );
			Files.writeString( out, html );
		} catch ( java.io.IOException e ) {
			throw new RuntimeException( "Failed to write span HTML to [" + out + "]", e );
		}
		return out;
	}

	/**
	 * Format a duration in nanoseconds as a human-readable string, using the same
	 * display rules as TestBox's CFML {@code formatExecTime} helper but with
	 * milliseconds as the smallest unit:
	 * <ul>
	 * <li>days / hours / minutes / seconds roll up at 1000ms boundaries</li>
	 * <li>hours only shown if days exist; minutes only if hours/days exist</li>
	 * <li>seconds omitted when hours or days shown; milliseconds omitted when
	 * minutes or higher shown</li>
	 * <li>below 1 second, seconds shown with remainder milliseconds</li>
	 * <li>below 1 millisecond, the fractional milliseconds are shown with 3
	 * decimals (e.g. {@code 0.123ms})</li>
	 * </ul>
	 * Examples: {@code 750000ns -> "0.750ms"}, {@code 3456789ns -> "3sec 456ms"},
	 * {@code 90_000_000_000ns -> "1min 30sec"}.
	 *
	 * @param nanos the duration in nanoseconds
	 *
	 * @return a human-readable duration string
	 */
	public static String formatDuration( long nanos ) {
		if ( nanos < 0 ) {
			nanos = 0;
		}
		// Roll up from milliseconds to the largest whole unit, like the CFML loop.
		long	ms	= nanos / 1_000_000L;
		long	rem	= nanos % 1_000_000L;   // leftover nanoseconds under 1ms
		long	day	= 0, hr = 0, min = 0, sec = 0;
		while ( ms >= 1000 ) {
			ms = ms - 1000;
			sec++;
			if ( sec >= 60 ) {
				min++;
			}
			if ( sec == 60 ) {
				sec = 0;
			}
			if ( min >= 60 ) {
				hr++;
			}
			if ( min == 60 ) {
				min = 0;
			}
			if ( hr >= 24 ) {
				hr = hr - 24;
				day++;
			}
		}

		List<String> outputTime = new ArrayList<>();
		if ( day > 0 ) {
			outputTime.add( day + "d" );
		}
		if ( hr > 0 || day > 0 ) {
			outputTime.add( hr + "hr" );
		}
		if ( min > 0 || day > 0 || hr > 0 ) {
			outputTime.add( min + "min" );
		}
		if ( ( sec > 0 || min > 0 ) && hr == 0 && day == 0 ) {
			outputTime.add( sec + "sec" );
		}
		if ( ms > 0 && hr == 0 && day == 0 && min == 0 ) {
			outputTime.add( ms + "ms" );
		}
		// Sub-millisecond: show fractional milliseconds with 3 decimals so no
		// detail is lost (e.g. 230900ns -> 0.230ms).
		if ( outputTime.isEmpty() && rem > 0 ) {
			outputTime.add( String.format( "%.3fms", rem / 1_000_000.0 ) );
		}
		if ( outputTime.isEmpty() ) {
			outputTime.add( "0ms" );
		}
		return String.join( " ", outputTime );
	}

	/**
	 * Read the source for a FILE blueprint from disk. If the key is not a readable
	 * path (e.g. a SOURCE hash), returns a placeholder.
	 */
	private static String readSource( String fileKey ) {
		try {
			// Normalize CRLF/CR to LF so character offsets align with the span
			// coordinates (which are LF-based regardless of the file's line endings).
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
		int			offset	= 0;
		result.add( new int[] { 1, 0 } );
		for ( int i = 0; i < source.length(); i++ ) {
			if ( source.charAt( i ) == '\n' ) {
				offset = i + 1;
				result.add( new int[] { result.size() + 1, offset } );
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