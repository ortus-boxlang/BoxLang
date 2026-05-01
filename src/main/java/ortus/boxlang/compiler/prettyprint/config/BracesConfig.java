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
 * Configuration options for brace style and placement formatting.
 */
public class BracesConfig {

	/**
	 * Placement style for the opening brace. {@code "same-line"} places the brace on the
	 * same line as the statement; {@code "new-line"} places it on its own line;
	 * {@code "preserve"} keeps the original source placement.
	 *
	 * <pre>
	 * // style: "same-line" (default)
	 * if ( true ) {
	 *     foo();
	 * }
	 *
	 * // style: "new-line"
	 * if ( true ) {
	 *     foo();
	 * }
	 * </pre>
	 */
	private String		style						= "same-line";

	/**
	 * Require braces around single-statement bodies for control structures such as
	 * {@code if}, {@code for}, and {@code while}.
	 *
	 * <pre>
	 * // requireForSingleStatement: true (default)
	 * if ( true ) {
	 *     foo();
	 * }
	 *
	 * // requireForSingleStatement: false
	 * if ( true )
	 *     foo();
	 * </pre>
	 */
	@JsonProperty( "require_for_single_statement" )
	private boolean		requireForSingleStatement	= true;

	/**
	 * Configuration for {@code else} clause placement.
	 *
	 * @see ElseConfig
	 */
	@JsonProperty( "else" )
	private ElseConfig	elseConfig					= new ElseConfig();

	/** Default constructor. */
	public BracesConfig() {
	}

	/**
	 * Get the opening brace placement style.
	 *
	 * @return the brace style
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * Set the opening brace placement style.
	 *
	 * @param style the brace style ({@code "same-line"}, {@code "new-line"}, or {@code "preserve"})
	 *
	 * @return this config for chaining
	 */
	public BracesConfig setStyle( String style ) {
		this.style = style;
		return this;
	}

	/**
	 * Get whether braces are required for single-statement bodies.
	 *
	 * @return true if braces are required
	 */
	public boolean getRequireForSingleStatement() {
		return requireForSingleStatement;
	}

	/**
	 * Set whether braces are required for single-statement bodies.
	 *
	 * @param requireForSingleStatement true to require braces
	 *
	 * @return this config for chaining
	 */
	public BracesConfig setRequireForSingleStatement( boolean requireForSingleStatement ) {
		this.requireForSingleStatement = requireForSingleStatement;
		return this;
	}

	/**
	 * Get the else clause configuration.
	 *
	 * @return the else configuration
	 */
	public ElseConfig getElseConfig() {
		return elseConfig;
	}

	/**
	 * Set the else clause configuration.
	 *
	 * @param elseConfig the else configuration to set
	 *
	 * @return this config for chaining
	 */
	public BracesConfig setElseConfig( ElseConfig elseConfig ) {
		this.elseConfig = elseConfig;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "style", style );
		map.put( "require_for_single_statement", requireForSingleStatement );
		map.put( "else", elseConfig.toMap() );
		return map;
	}

	/**
	 * Configuration for {@code else} clause placement relative to the closing brace.
	 */
	public static class ElseConfig {

		/**
		 * Placement style for the {@code else} keyword. {@code "same-line"} places
		 * it on the same line as the closing brace; {@code "new-line"} places it
		 * on its own line.
		 *
		 * <pre>
		 * // style: "same-line" (default)
		 * } else {
		 *
		 * // style: "new-line"
		 * }
		 * else {
		 * </pre>
		 */
		private String style = "same-line";

		/** Default constructor. */
		public ElseConfig() {
		}

		/**
		 * Get the else placement style.
		 *
		 * @return the else style
		 */
		public String getStyle() {
			return style;
		}

		/**
		 * Set the else placement style.
		 *
		 * @param style the else style ({@code "same-line"} or {@code "new-line"})
		 *
		 * @return this config for chaining
		 */
		public ElseConfig setStyle( String style ) {
			this.style = style;
			return this;
		}

		/**
		 * Convert this configuration to a map for JSON serialization.
		 *
		 * @return a map representation of this configuration
		 */
		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "style", style );
			return map;
		}
	}
}
