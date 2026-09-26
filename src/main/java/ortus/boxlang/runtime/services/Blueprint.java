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

import java.util.List;

/**
 * The compile-time blueprint of a source unit: the list of EXECUTABLE spans
 * produced by the boxpiler (Pass A) and registered with {@link CodeProfilerService}.
 * <p>
 * Only executable spans are listed — the atomic units that can run and get a span
 * id. Non-executable regions (whitespace, punctuation, braces, comments, trailing
 * lines) are absent; the runtime treats any point not inside an executable span
 * as NOT_EXECUTABLE ({@code spanAt} returns null for it).
 * <p>
 * {@code kind} marks how the blueprint's key is interpreted — a real file path
 * (FILE) or an adhoc source hash (SOURCE) — so generic reporting can treat each
 * registered blueprint self-describing.
 */
public record Blueprint(
    int totalLines,
    List<SpanDef> spans,
    Kind kind ) {

	/**
	 * What the registered key represents.
	 */
	public enum Kind {
		/**
		 * The key is a normalized filesystem path.
		 */
		FILE,

		/**
		 * The key is a hash of adhoc source (REPL, unsafe eval, test suites).
		 */
		SOURCE
	}

	/**
	 * Convenience constructor defaulting the kind to {@link Kind#FILE}.
	 *
	 * @param totalLines total source line count
	 * @param spans      every span of the source
	 */
	public Blueprint( int totalLines, List<SpanDef> spans ) {
		this( totalLines, spans, Kind.FILE );
	}

	/**
	 * A single span of source text.
	 *
	 * @param startLine  1-based
	 * @param startCol   0-based
	 * @param endLine    1-based
	 * @param endCol     0-based
	 * @param executable whether this span can run / emits bytecode
	 */
	public record SpanDef(
	    int startLine,
	    int startCol,
	    int endLine,
	    int endCol,
	    boolean executable ) {
	}
}