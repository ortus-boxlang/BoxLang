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
 * Configuration options for semicolon formatting within {@code for(;;)} loop headers.
 */
public class ForLoopSemicolons {

	/**
	 * Add a space after each semicolon in a for-loop header.
	 *
	 * <pre>
	 * // padding: true (default)
	 * for ( i = 0; i &lt; 10; i++ ) {
	 * }
	 *
	 * // padding: false
	 * for ( i = 0; i &lt; 10; i++ ) {
	 * }
	 * </pre>
	 */
	private boolean padding = true;

	/** Default constructor. */
	public ForLoopSemicolons() {
	}

	/**
	 * Get whether spaces are added after for-loop semicolons.
	 *
	 * @return true if padding is enabled
	 */
	public boolean getPadding() {
		return padding;
	}

	/**
	 * Set whether spaces are added after for-loop semicolons.
	 *
	 * @param padding true to enable padding
	 *
	 * @return this config for chaining
	 */
	public ForLoopSemicolons setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "padding", padding );
		return map;
	}
}
