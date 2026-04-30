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
 * Configuration for leading comma style in multiline collections. When enabled,
 * commas are placed at the beginning of each continuation line instead of at the
 * end of the previous line.
 *
 * <pre>
 * // enabled: true, padding: true (default padding)
 * [
 *     1
 *   , 2
 *   , 3
 * ]
 *
 * // enabled: false (default)
 * [
 *     1,
 *     2,
 *     3
 * ]
 * </pre>
 */
public class LeadingComma {

	/**
	 * Enable leading comma style. When true, commas are placed at the start of
	 * continuation lines rather than at the end of the previous line.
	 */
	private boolean	enabled	= false;

	/**
	 * Add a space after the leading comma.
	 */
	private boolean	padding	= true;

	/** Default constructor. */
	public LeadingComma() {
	}

	/**
	 * Construct a LeadingComma with the specified enabled state.
	 *
	 * @param enabled true to enable leading commas
	 */
	public LeadingComma( boolean enabled ) {
		this.enabled = enabled;
	}

	/**
	 * Create a LeadingComma from a map, typically from JSON deserialization.
	 *
	 * @param map a map with {@code "enabled"} and/or {@code "padding"} keys
	 *
	 * @return a new LeadingComma instance
	 */
	public static LeadingComma fromMap( Map<String, Object> map ) {
		LeadingComma lc = new LeadingComma();
		if ( map.containsKey( "enabled" ) && map.get( "enabled" ) instanceof Boolean b ) {
			lc.setEnabled( b );
		}
		if ( map.containsKey( "padding" ) && map.get( "padding" ) instanceof Boolean b ) {
			lc.setPadding( b );
		}
		return lc;
	}

	/**
	 * Get whether leading comma style is enabled.
	 *
	 * @return true if leading commas are enabled
	 */
	public boolean getEnabled() {
		return enabled;
	}

	/**
	 * Set whether leading comma style is enabled.
	 *
	 * @param enabled true to enable leading commas
	 *
	 * @return this config for chaining
	 */
	public LeadingComma setEnabled( boolean enabled ) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Get whether a space is added after the leading comma.
	 *
	 * @return true if padding is enabled
	 */
	public boolean getPadding() {
		return padding;
	}

	/**
	 * Set whether a space is added after the leading comma.
	 *
	 * @param padding true to enable padding
	 *
	 * @return this config for chaining
	 */
	public LeadingComma setPadding( boolean padding ) {
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
		map.put( "enabled", enabled );
		map.put( "padding", padding );
		return map;
	}
}