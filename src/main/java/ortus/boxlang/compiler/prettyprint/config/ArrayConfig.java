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
 * Configuration options for array literal formatting.
 */
public class ArrayConfig {

	/**
	 * Add spaces inside square brackets of array literals that contain elements.
	 * When flat, adds a space; when broken across lines, adds a newline.
	 *
	 * <pre>
	 * // padding: true
	 * arr = [ 1, 2, 3 ];
	 *
	 * // padding: false (default)
	 * arr = [1, 2, 3];
	 * </pre>
	 */
	private boolean			padding			= true;

	/**
	 * Add a space inside empty array literals.
	 *
	 * <pre>
	 * // emptyPadding: true
	 * arr = [ ];
	 *
	 * // emptyPadding: false (default)
	 * arr = [];
	 * </pre>
	 */
	@JsonProperty( "empty_padding" )
	private boolean			emptyPadding	= false;

	/**
	 * Multiline formatting thresholds for array literals. Controls when arrays
	 * switch from single-line to multi-line formatting based on element count
	 * or total length.
	 *
	 * @see MultilineConfig
	 */
	private MultilineConfig	multiline		= new MultilineConfig();

	/** Default constructor. */
	public ArrayConfig() {
		this.multiline.setElementCount( 2 );
		this.multiline.setMinLength( 50 );
	}

	/**
	 * Get the multiline formatting configuration for array literals.
	 *
	 * @return the multiline configuration
	 */
	public MultilineConfig getMultiline() {
		return multiline;
	}

	/**
	 * Set the multiline formatting configuration for array literals.
	 *
	 * @param multiline the multiline configuration to set
	 *
	 * @return this config for chaining
	 */
	public ArrayConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	/**
	 * Get whether spaces are added inside array brackets.
	 *
	 * @return true if padding is enabled
	 */
	public boolean getPadding() {
		return padding;
	}

	/**
	 * Set whether spaces are added inside array brackets.
	 *
	 * @param padding true to enable padding
	 *
	 * @return this config for chaining
	 */
	public ArrayConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	/**
	 * Get whether spaces are added inside empty array brackets.
	 *
	 * @return true if empty padding is enabled
	 */
	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	/**
	 * Set whether spaces are added inside empty array brackets.
	 *
	 * @param emptyPadding true to enable empty padding
	 *
	 * @return this config for chaining
	 */
	public ArrayConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
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
		map.put( "empty_padding", emptyPadding );
		map.put( "multiline", multiline.toMap() );
		return map;
	}

}
