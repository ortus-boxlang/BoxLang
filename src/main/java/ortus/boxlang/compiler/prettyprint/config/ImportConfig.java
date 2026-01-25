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

	private boolean	sort	= false;
	private boolean	group	= false;

	public ImportConfig() {
	}

	public boolean getSort() {
		return sort;
	}

	public ImportConfig setSort( boolean sort ) {
		this.sort = sort;
		return this;
	}

	public boolean getGroup() {
		return group;
	}

	public ImportConfig setGroup( boolean group ) {
		this.group = group;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "sort", sort );
		map.put( "group", group );
		return map;
	}
}
