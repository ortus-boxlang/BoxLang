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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.services.CodeProfilerService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Builds coverage data from {@link CodeProfilerService} into NATIVE BoxLang data
 * structures ({@link Struct} and {@link Array}), so BoxLang tooling can consume
 * and transform it without parsing JSON or touching the low-level query API.
 * <p>
 * The data is keyed by the registered blueprint key (a normalized file path for
 * {@link Blueprint.Kind#FILE}, a source hash for {@link Blueprint.Kind#SOURCE})
 * and only includes the {@link Blueprint.Kind}s passed in — defaulting to
 * {@link Blueprint.Kind#FILE} when none are specified, since adhoc SOURCE
 * blueprints are rarely useful in exported reports.
 */
public final class CoverageDataBuilder {

	/**
	 * No construction - static utility.
	 */
	private CoverageDataBuilder() {
	}

	/**
	 * Resolve the kinds to include: the given varargs, or {@code FILE} when empty.
	 *
	 * @param kinds the requested blueprint kinds (may be empty/null)
	 *
	 * @return the set of kinds to include
	 */
	static Set<Blueprint.Kind> resolveKinds( Blueprint.Kind... kinds ) {
		if ( kinds == null || kinds.length == 0 ) {
			return EnumSet.of( Blueprint.Kind.FILE );
		}
		Set<Blueprint.Kind> result = new HashSet<>();
		Collections.addAll( result, kinds );
		return result;
	}

	/**
	 * Build PER-LINE coverage for every registered blueprint of the requested
	 * kinds. Each file value:
	 * 
	 * <pre>
	 * {
	 *   "filePath"          : &lt;normalized path or source hash&gt;,
	 *   "kind"              : "FILE" | "SOURCE",
	 *   "numExecutableLines": &lt;lines with an executable span&gt;,
	 *   "numCoveredLines"   : &lt;lines with count &gt; 0&gt;,
	 *   "percCoverage"      : &lt;covered / executable, or 1 if none&gt;,
	 *   "lines"             : {
	 *       "&lt;lineNum&gt;"      : { "covered": bool, "count": n, "time": n }
	 *   }
	 * }
	 * </pre>
	 *
	 * Only lines touched by an executable span are present in {@code lines}.
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key, ordered by registration
	 */
	public static Struct buildLineCoverage( CodeProfilerService service, Blueprint.Kind... kinds ) {
		Set<Blueprint.Kind>		allowed	= resolveKinds( kinds );
		Map<String, Blueprint>	tracked	= service.trackedBlueprints();
		Struct					root	= new Struct( IStruct.TYPES.LINKED, false, Math.max( 16, tracked.size() ) );
		for ( Map.Entry<String, Blueprint> entry : tracked.entrySet() ) {
			Blueprint bp = entry.getValue();
			if ( !allowed.contains( bp.kind() ) ) {
				continue;
			}
			Struct fileObj = new Struct( IStruct.TYPES.LINKED, false, 7 );
			fileObj.put( Key.filePath, entry.getKey() );
			fileObj.put( Key.kind, bp.kind().name() );

			Map<Integer, CodeProfilerService.LineCoverage>	lines		= service.fileLines( entry.getKey() );
			Struct											lineObj		= new Struct( IStruct.TYPES.LINKED, false, Math.max( 16, lines.size() ) );
			long											execLines	= 0, coveredLines = 0;
			// fileLines returns a TreeMap, so its keySet() is already in ascending line order.
			for ( Integer line : lines.keySet() ) {
				CodeProfilerService.LineCoverage	lc	= lines.get( line );
				Struct								lv	= new Struct( IStruct.TYPES.LINKED, false, 3 );
				lv.put( Key.covered, lc.covered() );
				lv.put( Key.count, lc.count() );
				// The single time number: ON-THE-STACK time (inclusive for call spans,
				// self for leaf spans). Self/inclusive are NOT exported.
				lv.put( Key.time, lc.nanos() );
				lineObj.put( Key.cachedInt( line ), lv );
				execLines++;
				if ( lc.covered() ) {
					coveredLines++;
				}
			}
			fileObj.put( Key.numExecutableLines, execLines );
			fileObj.put( Key.numCoveredLines, coveredLines );
			fileObj.put( Key.percCoverage, execLines == 0 ? 1.0 : ( double ) coveredLines / execLines );
			fileObj.put( Key.lines, lineObj );
			root.put( Key.of( entry.getKey() ), fileObj );
		}
		return root;
	}

	/**
	 * Build EVERY SPAN (in blueprint order) for every registered blueprint of the
	 * requested kinds. Each file value:
	 * 
	 * <pre>
	 * {
	 *   "filePath": &lt;normalized path or source hash&gt;,
	 *   "kind"    : "FILE" | "SOURCE",
	 *   "spans"   : [
	 *       { "id": 0, "startLine": 1, "startCol": 0, "endLine": 1, "endCol": 5,
	 *         "count": n, "time": n }
	 *   ]
	 * }
	 * </pre>
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key, ordered by registration
	 */
	public static Struct buildSpanCoverage( CodeProfilerService service, Blueprint.Kind... kinds ) {
		Set<Blueprint.Kind>		allowed	= resolveKinds( kinds );
		Map<String, Blueprint>	tracked	= service.trackedBlueprints();
		Struct					root	= new Struct( IStruct.TYPES.LINKED, false, Math.max( 16, tracked.size() ) );
		for ( Map.Entry<String, Blueprint> entry : tracked.entrySet() ) {
			Blueprint bp = entry.getValue();
			if ( !allowed.contains( bp.kind() ) ) {
				continue;
			}
			Struct fileObj = new Struct( IStruct.TYPES.LINKED, false, 3 );
			fileObj.put( Key.filePath, entry.getKey() );
			fileObj.put( Key.kind, bp.kind().name() );
			List<CodeProfilerService.Span>	fileSpans	= service.fileSpans( entry.getKey() );
			Array							spanArr		= new Array( fileSpans.size(), false );
			for ( CodeProfilerService.Span s : fileSpans ) {
				Struct sv = new Struct( IStruct.TYPES.LINKED, false, 7 );
				sv.put( Key.id, s.id() );
				sv.put( Key.startLine, s.startLine() );
				sv.put( Key.startCol, s.startCol() );
				sv.put( Key.endLine, s.endLine() );
				sv.put( Key.endCol, s.endCol() );
				sv.put( Key.count, s.stats().count() );
				// The single time number: ON-THE-STACK time (inclusive for call spans,
				// self for leaf spans). Self/inclusive are NOT exported.
				sv.put( Key.time, s.stats().onStackNanos() );
				spanArr.append( sv );
			}
			fileObj.put( Key.spans, spanArr );
			root.put( Key.of( entry.getKey() ), fileObj );
		}
		return root;
	}
}
