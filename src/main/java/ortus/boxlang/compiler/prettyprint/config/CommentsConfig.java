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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration options for comment formatting.
 */
public class CommentsConfig {

	/**
	 * Preserve blank lines between comments or between a comment and adjacent code.
	 * When true, existing empty lines around comments are retained.
	 */
	@JsonProperty( "preserve_blank_lines" )
	private boolean	preserveBlankLines	= true;

	/**
	 * Automatically wrap long comment lines based on the global {@code maxLineLength} setting.
	 * When true, comment text that exceeds the max line length is reflowed.
	 */
	private boolean	wrap				= false;

	/** Default constructor. */
	public CommentsConfig() {
	}

	/**
	 * Get whether blank lines around comments are preserved.
	 *
	 * @return true if blank lines are preserved
	 */
	public boolean getPreserveBlankLines() {
		return preserveBlankLines;
	}

	/**
	 * Set whether blank lines around comments are preserved.
	 *
	 * @param preserveBlankLines true to preserve blank lines
	 *
	 * @return this config for chaining
	 */
	public CommentsConfig setPreserveBlankLines( boolean preserveBlankLines ) {
		this.preserveBlankLines = preserveBlankLines;
		return this;
	}

	/**
	 * Get whether long comment lines are automatically wrapped.
	 *
	 * @return true if wrapping is enabled
	 */
	public boolean getWrap() {
		return wrap;
	}

	/**
	 * Set whether long comment lines are automatically wrapped.
	 *
	 * @param wrap true to enable wrapping
	 *
	 * @return this config for chaining
	 */
	public CommentsConfig setWrap( boolean wrap ) {
		this.wrap = wrap;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "preserve_blank_lines", preserveBlankLines );
		map.put( "wrap", wrap );
		return map;
	}
}
