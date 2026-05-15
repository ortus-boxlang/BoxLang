/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration options for import statement formatting.
 */
public class ImportConfig {

	/**
	 * Sort import statements alphabetically.
	 *
	 * <pre>
	 * 
	 * // sort: true
	 * import java.io.File;
	 * import java.util.List;
	 * import ortus.boxlang.runtime.BoxRuntime;
	 *
	 * // sort: false (default) - preserves original order
	 * </pre>
	 */
	private boolean	sort	= false;

	/**
	 * Group imports by top-level package with blank lines between groups.
	 *
	 * <pre>
	 * 
	 * // group: true
	 * import java.io.File;
	 * import java.util.List;
	 *
	 * import ortus.boxlang.runtime.BoxRuntime;
	 *
	 * // group: false (default) - no blank lines between imports
	 * </pre>
	 */
	private boolean	group	= false;

	/** Default constructor. */
	public ImportConfig() {
	}

	/**
	 * Get whether imports are sorted alphabetically.
	 *
	 * @return true if sorting is enabled
	 */
	public boolean getSort() {
		return sort;
	}

	/**
	 * Set whether imports are sorted alphabetically.
	 *
	 * @param sort true to enable sorting
	 *
	 * @return this config for chaining
	 */
	public ImportConfig setSort( boolean sort ) {
		this.sort = sort;
		return this;
	}

	/**
	 * Get whether imports are grouped by package.
	 *
	 * @return true if grouping is enabled
	 */
	public boolean getGroup() {
		return group;
	}

	/**
	 * Set whether imports are grouped by package.
	 *
	 * @param group true to enable grouping
	 *
	 * @return this config for chaining
	 */
	public ImportConfig setGroup( boolean group ) {
		this.group = group;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "sort", sort );
		map.put( "group", group );
		return map;
	}

	/**
	 * Create a deep copy of this configuration.
	 *
	 * @return a new ImportConfig with the same settings
	 */
	public ImportConfig clone() {
		ImportConfig clone = new ImportConfig();
		clone.sort	= this.sort;
		clone.group	= this.group;
		return clone;
	}
}
