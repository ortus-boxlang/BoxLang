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
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Configuration options that control when collections (arrays, structs, argument lists, etc.)
 * switch from single-line to multi-line formatting.
 */
public class MultilineConfig {

	/**
	 * Number of elements that triggers multiline formatting. If the collection
	 * has at least this many elements, it is formatted with one element per line.
	 */
	@JsonProperty( "element_count" )
	private int				elementCount	= 4;

	/**
	 * Add a trailing comma after the last element when formatted across multiple lines.
	 *
	 * <pre>
	 * // commaDangle: true
	 * [
	 *     1,
	 *     2,
	 *     3,
	 * ]
	 *
	 * // commaDangle: false (default)
	 * [
	 *     1,
	 *     2,
	 *     3
	 * ]
	 * </pre>
	 */
	@JsonProperty( "comma_dangle" )
	private boolean			commaDangle		= false;

	/**
	 * Configuration for leading comma style in multiline collections.
	 * When enabled, commas are placed at the start of each line rather than
	 * at the end of the previous line.
	 *
	 * @see LeadingComma
	 */
	@JsonProperty( "leading_comma" )
	private LeadingComma	leadingComma	= new LeadingComma();

	/**
	 * Minimum total character length of the flat-printed collection that triggers
	 * multiline formatting. If the single-line representation exceeds this length,
	 * it switches to multiline.
	 */
	@JsonProperty( "min_length" )
	private int				minLength		= 40;

	/** Default constructor. */
	public MultilineConfig() {
	}

	/**
	 * Get the leading comma configuration.
	 *
	 * @return the leading comma configuration
	 */
	public LeadingComma getLeadingComma() {
		return leadingComma;
	}

	/**
	 * Set the leading comma configuration. Accepts either a {@link Boolean} (shorthand for
	 * enabling/disabling) or a {@link Map} with {@code enabled} and {@code padding} keys.
	 *
	 * @param leadingComma the leading comma config (Boolean or Map)
	 *
	 * @return this config for chaining
	 */
	@JsonSetter( "leading_comma" )
	public MultilineConfig setLeadingComma( Object leadingComma ) {
		if ( leadingComma instanceof Boolean b ) {
			this.leadingComma = new LeadingComma( b );
		} else if ( leadingComma instanceof Map lcMap ) {
			this.leadingComma = LeadingComma.fromMap( lcMap );
		}

		return this;
	}

	/**
	 * Get the minimum length threshold for multiline formatting.
	 *
	 * @return the minimum length
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * Set the minimum length threshold for multiline formatting.
	 *
	 * @param minLength the minimum length
	 *
	 * @return this config for chaining
	 */
	public MultilineConfig setMinLength( int minLength ) {
		this.minLength = minLength;
		return this;
	}

	/**
	 * Get whether a trailing comma is added in multiline mode.
	 *
	 * @return true if dangling comma is enabled
	 */
	public boolean getCommaDangle() {
		return commaDangle;
	}

	/**
	 * Set whether a trailing comma is added in multiline mode.
	 *
	 * @param commaDangle true to enable dangling comma
	 *
	 * @return this config for chaining
	 */
	public MultilineConfig setCommaDangle( boolean commaDangle ) {
		this.commaDangle = commaDangle;
		return this;
	}

	/**
	 * Get the element count threshold for multiline formatting.
	 *
	 * @return the element count threshold
	 */
	public int getElementCount() {
		return elementCount;
	}

	/**
	 * Set the element count threshold for multiline formatting.
	 *
	 * @param elementCount the element count threshold
	 *
	 * @return this config for chaining
	 */
	@JsonSetter( "element_count" )
	public MultilineConfig setElementCount( int elementCount ) {
		this.elementCount = elementCount;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "element_count", elementCount );
		map.put( "comma_dangle", commaDangle );
		map.put( "leading_comma", leadingComma.toMap() );
		map.put( "min_length", minLength );
		return map;
	}
}