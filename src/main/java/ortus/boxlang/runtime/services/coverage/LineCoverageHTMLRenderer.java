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
import java.util.Map;

import ortus.boxlang.runtime.services.CodeProfilerService;

/**
 * DEBUG TOOLING — renders a profiled source file to an HTML file at the LINE
 * level: each line is colored green (covered, count &gt; 0), red (missed, has
 * executable spans but none ran), or un-colored (no executable spans on that
 * line). Unlike {@link SpanHTMLRenderer} (which highlights individual spans),
 * this visualizes whole-line coverage.
 * <p>
 * Standalone and intentionally NOT part of the profiler service API. The caller
 * is responsible for having already run the file under the profiler
 * ({@link CodeProfilerService}).
 *
 * <pre>
 * String html = LineCoverageHTMLRenderer.renderHTML( "/abs/path/to/file.bx" );
 * LineCoverageHTMLRenderer.renderToFile( "/abs/path/to/file.bx", "build/out.html" );
 * </pre>
 */
public final class LineCoverageHTMLRenderer {

	/**
	 * No construction - static utility.
	 */
	private LineCoverageHTMLRenderer() {
	}

	/**
	 * Render one tracked source file to line-level coverage HTML.
	 *
	 * @param fileKey the normalized blueprint key registered with
	 *                {@link CodeProfilerService}
	 * @param title   an optional HTML title; defaults to the file key
	 *
	 * @return the full HTML document string
	 */
	public static String renderHTML( String fileKey, String title ) {
		String											source		= readSource( fileKey );
		// line -> LineCoverage (only lines with executable spans present).
		Map<Integer, CodeProfilerService.LineCoverage>	lines		= CodeProfilerService.fileLines( fileKey );
		String[]										srcLines	= splitLines( source );

		StringBuilder									html		= new StringBuilder();
		html.append( "<html><head><meta charset=\"utf-8\"><title>" ).append( escape( title != null ? title : fileKey ) ).append( "</title><style>" )
		    .append( "body{font-family:'SF Mono',Consolas,monospace;margin:24px;background:#fff;color:#111;} " )
		    .append( "h3{padding-left:8px;} table.code{border-collapse:collapse;font-size:14px;line-height:1.5;} " )
		    .append( "td.ln{text-align:right;padding:0 10px 0 0;color:#999;border-right:1px solid #ddd;user-select:none;min-width:3ch;} " )
		    .append( "td.c{white-space:pre;padding:0 0 0 10px;} " )
		    .append( "tr.odd td.ln{background:#fafafa;} tr.odd td.c{background:#fafafa;} " )
		    .append( "tr.cov td.c{background:rgba(10,220,90,.28);} " )
		    .append( "tr.miss td.c{background:rgba(230,60,60,.25);} " )
		    .append( "span.cov{background:rgba(10,220,90,.28);} " )
		    .append( "span.miss{background:rgba(230,60,60,.25);} " )
		    .append( "</style>" ).append( '\n' ).append( "</head>" ).append( '\n' ).append( "<body>" ).append( '\n' )
		    .append( "<h3>" ).append( escape( fileKey ) ).append( "</h3>" ).append( '\n' )
		    .append(
		        "<p style=\"padding-left:8px\"><span class=\"cov\">covered</span>&nbsp;&nbsp;<span class=\"miss\">missed</span>&nbsp;(uncolored = no executable spans)</p>" )
		    .append( '\n' ).append( "<table class=\"code\">" ).append( '\n' );

		for ( int i = 0; i < srcLines.length; i++ ) {
			int									lineNum	= i + 1;
			String								rowCls	= ( lineNum % 2 == 0 ) ? "" : "odd";
			CodeProfilerService.LineCoverage	lc		= lines.get( lineNum );
			String								state	= "";
			if ( lc != null ) {
				rowCls	= ( rowCls.isEmpty() ? "" : rowCls + " " ) + ( lc.covered() ? "cov" : "miss" );
				state	= " title=\"" + ( lc.covered() ? "covered" : "missed" ) + ", count=" + lc.count() + ", time="
				    + SpanHTMLRenderer.formatDuration( lc.totalNanos() ) + "\"";
			}
			html.append( "  <tr class=\"" ).append( rowCls ).append( "\">" )
			    .append( "<td class=\"ln\">" ).append( lineNum ).append( "</td>" )
			    .append( "<td class=\"c\"" ).append( state ).append( ">" ).append( escape( srcLines[ i ] ) ).append( "</td></tr>" )
			    .append( '\n' );
		}
		html.append( "</table>" ).append( '\n' ).append( "</body>" ).append( '\n' ).append( "</html>" ).append( '\n' );
		return html.toString();
	}

	/**
	 * Render a tracked source file and write the line-level HTML to
	 * {@code outputPath}, creating parent directories as needed.
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
			throw new RuntimeException( "Failed to write line-coverage HTML to [" + out + "]", e );
		}
		return out;
	}

	/**
	 * Read the source for a FILE blueprint from disk, normalizing line endings.
	 */
	private static String readSource( String fileKey ) {
		try {
			return Files.readString( Paths.get( fileKey ) ).replace( "\r\n", "\n" ).replace( "\r", "\n" );
		} catch ( java.io.IOException e ) {
			return "<unable to read source for " + fileKey + ": " + e.getMessage() + ">";
		}
	}

	/**
	 * Split the source into lines (LF-based; drop a trailing empty element from a
	 * terminal newline).
	 */
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