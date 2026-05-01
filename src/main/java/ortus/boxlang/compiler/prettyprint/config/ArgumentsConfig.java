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
 * Configuration options for function call argument list formatting.
 */
public class ArgumentsConfig {

	/**
	 * Add spaces inside the parentheses of function call argument lists.
	 *
	 * <pre>
	 * // padding: true
	 * foo( arg1, arg2 );
	 *
	 * // padding: false (default)
	 * foo( arg1, arg2 );
	 * </pre>
	 */
	private boolean	padding			= true;

	/**
	 * Add a space inside empty argument parentheses.
	 *
	 * <pre>
	 * 
	 * // emptyPadding: true
	 * foo();
	 * 
	 * // emptyPadding: false (default)
	 * foo();
	 * </pre>
	 */
	@JsonProperty( "empty_padding" )
	private boolean	emptyPadding	= false;

	/**
	 * Add a trailing comma after the last argument when formatted across multiple lines.
	 *
	 * <pre>
	 * // commaDangle: true
	 * foo(
	 *     arg1,
	 *     arg2,
	 * );
	 *
	 * // commaDangle: false (default)
	 * foo(
	 *     arg1,
	 *     arg2
	 * );
	 * </pre>
	 */
	@JsonProperty( "comma_dangle" )
	private boolean	commaDangle		= false;

	/**
	 * Number of arguments that triggers multiline formatting. If the argument
	 * count meets or exceeds this threshold, each argument is placed on its own line.
	 */
	@JsonProperty( "multiline_count" )
	private int		multilineCount	= 3;

	/**
	 * Total character length of the argument list that triggers multiline formatting.
	 * If the flat-printed argument list exceeds this length, it switches to multiline.
	 */
	@JsonProperty( "multiline_length" )
	private int		multilineLength	= 50;

	/** Default constructor. */
	public ArgumentsConfig() {
	}

	/**
	 * Get whether spaces are added inside argument parentheses.
	 *
	 * @return true if padding is enabled
	 */
	public boolean getPadding() {
		return padding;
	}

	/**
	 * Set whether spaces are added inside argument parentheses.
	 *
	 * @param padding true to enable padding
	 *
	 * @return this config for chaining
	 */
	public ArgumentsConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	/**
	 * Get whether spaces are added inside empty argument parentheses.
	 *
	 * @return true if empty padding is enabled
	 */
	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	/**
	 * Set whether spaces are added inside empty argument parentheses.
	 *
	 * @param emptyPadding true to enable empty padding
	 *
	 * @return this config for chaining
	 */
	public ArgumentsConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
		return this;
	}

	/**
	 * Get whether a trailing comma is added after the last argument in multiline mode.
	 *
	 * @return true if dangling comma is enabled
	 */
	public boolean getCommaDangle() {
		return commaDangle;
	}

	/**
	 * Set whether a trailing comma is added after the last argument in multiline mode.
	 *
	 * @param commaDangle true to enable dangling comma
	 *
	 * @return this config for chaining
	 */
	public ArgumentsConfig setCommaDangle( boolean commaDangle ) {
		this.commaDangle = commaDangle;
		return this;
	}

	/**
	 * Get the argument count threshold for multiline formatting.
	 *
	 * @return the multiline count threshold
	 */
	public int getMultilineCount() {
		return multilineCount;
	}

	/**
	 * Set the argument count threshold for multiline formatting.
	 *
	 * @param multilineCount the multiline count threshold
	 *
	 * @return this config for chaining
	 */
	public ArgumentsConfig setMultilineCount( int multilineCount ) {
		this.multilineCount = multilineCount;
		return this;
	}

	/**
	 * Get the total length threshold for multiline formatting.
	 *
	 * @return the multiline length threshold
	 */
	public int getMultilineLength() {
		return multilineLength;
	}

	/**
	 * Set the total length threshold for multiline formatting.
	 *
	 * @param multilineLength the multiline length threshold
	 *
	 * @return this config for chaining
	 */
	public ArgumentsConfig setMultilineLength( int multilineLength ) {
		this.multilineLength = multilineLength;
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
		map.put( "comma_dangle", commaDangle );
		map.put( "multiline_count", multilineCount );
		map.put( "multiline_length", multilineLength );
		return map;
	}
}
