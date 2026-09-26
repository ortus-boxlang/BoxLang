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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	 *       "&lt;lineNum&gt;"      : { "covered": bool, "count": n, "totalNanos": n }
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
	public static Struct buildLineCoverage( Blueprint.Kind... kinds ) {
		Set<Blueprint.Kind>	allowed	= resolveKinds( kinds );
		Struct				root	= new Struct( IStruct.TYPES.LINKED );
		for ( Map.Entry<String, Blueprint> entry : CodeProfilerService.trackedBlueprints().entrySet() ) {
			Blueprint bp = entry.getValue();
			if ( !allowed.contains( bp.kind() ) ) {
				continue;
			}
			Struct fileObj = new Struct( IStruct.TYPES.LINKED );
			fileObj.put( "filePath", entry.getKey() );
			fileObj.put( "kind", bp.kind().name() );

			Map<Integer, CodeProfilerService.LineCoverage>	lines		= CodeProfilerService.fileLines( entry.getKey() );
			Struct											lineObj		= new Struct( IStruct.TYPES.LINKED );
			long											execLines	= 0, coveredLines = 0;
			// Sort for stable, readable output.
			List<Integer>									lineNums	= new ArrayList<>( lines.keySet() );
			lineNums.sort( Comparator.naturalOrder() );
			for ( Integer line : lineNums ) {
				CodeProfilerService.LineCoverage	lc	= lines.get( line );
				Struct								lv	= new Struct( IStruct.TYPES.LINKED );
				lv.put( "covered", lc.covered() );
				lv.put( "count", lc.count() );
				lv.put( "totalNanos", lc.totalNanos() );
				lineObj.put( Key.of( line ), lv );
				execLines++;
				if ( lc.covered() ) {
					coveredLines++;
				}
			}
			fileObj.put( "numExecutableLines", execLines );
			fileObj.put( "numCoveredLines", coveredLines );
			fileObj.put( "percCoverage", execLines == 0 ? 1.0 : ( double ) coveredLines / execLines );
			fileObj.put( "lines", lineObj );
			root.put( entry.getKey(), fileObj );
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
	 *         "count": n, "totalNanos": n }
	 *   ]
	 * }
	 * </pre>
	 *
	 * @param kinds the blueprint kinds to include, default {@link Blueprint.Kind#FILE}
	 *
	 * @return a {@link Struct} keyed by blueprint key, ordered by registration
	 */
	public static Struct buildSpanCoverage( Blueprint.Kind... kinds ) {
		Set<Blueprint.Kind>	allowed	= resolveKinds( kinds );
		Struct				root	= new Struct( IStruct.TYPES.LINKED );
		for ( Map.Entry<String, Blueprint> entry : CodeProfilerService.trackedBlueprints().entrySet() ) {
			Blueprint bp = entry.getValue();
			if ( !allowed.contains( bp.kind() ) ) {
				continue;
			}
			Struct fileObj = new Struct( IStruct.TYPES.LINKED );
			fileObj.put( "filePath", entry.getKey() );
			fileObj.put( "kind", bp.kind().name() );
			Array spanArr = new Array();
			for ( CodeProfilerService.Span s : CodeProfilerService.fileSpans( entry.getKey() ) ) {
				Struct sv = new Struct( IStruct.TYPES.LINKED );
				sv.put( "id", s.id() );
				sv.put( "startLine", s.startLine() );
				sv.put( "startCol", s.startCol() );
				sv.put( "endLine", s.endLine() );
				sv.put( "endCol", s.endCol() );
				sv.put( "count", s.stats().count() );
				sv.put( "totalNanos", s.stats().totalNanos() );
				spanArr.append( sv );
			}
			fileObj.put( "spans", spanArr );
			root.put( entry.getKey(), fileObj );
		}
		return root;
	}
}
