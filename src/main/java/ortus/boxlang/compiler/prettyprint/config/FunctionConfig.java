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
 * Configuration options for function declaration formatting.
 */
public class FunctionConfig {

	/**
	 * Function declaration style. Controls the format of function declarations.
	 * Value {@code "preserve"} keeps the original source style.
	 */
	private String				style		= "preserve";

	/**
	 * Configuration for function parameter list formatting.
	 *
	 * @see ParametersConfig
	 */
	private ParametersConfig	parameters	= new ParametersConfig();

	/**
	 * Configuration for arrow/lambda function formatting.
	 *
	 * @see ArrowConfig
	 */
	private ArrowConfig			arrow		= new ArrowConfig();

	/** Default constructor. */
	public FunctionConfig() {
	}

	/**
	 * Get the function declaration style.
	 *
	 * @return the function style
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * Set the function declaration style.
	 *
	 * @param style the function style to set
	 *
	 * @return this config for chaining
	 */
	public FunctionConfig setStyle( String style ) {
		this.style = style;
		return this;
	}

	/**
	 * Get the parameter list configuration.
	 *
	 * @return the parameters configuration
	 */
	public ParametersConfig getParameters() {
		return parameters;
	}

	/**
	 * Set the parameter list configuration.
	 *
	 * @param parameters the parameters configuration to set
	 *
	 * @return this config for chaining
	 */
	public FunctionConfig setParameters( ParametersConfig parameters ) {
		this.parameters = parameters;
		return this;
	}

	/**
	 * Get the arrow function configuration.
	 *
	 * @return the arrow configuration
	 */
	public ArrowConfig getArrow() {
		return arrow;
	}

	/**
	 * Set the arrow function configuration.
	 *
	 * @param arrow the arrow configuration to set
	 *
	 * @return this config for chaining
	 */
	public FunctionConfig setArrow( ArrowConfig arrow ) {
		this.arrow = arrow;
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
		map.put( "parameters", parameters.toMap() );
		map.put( "arrow", arrow.toMap() );
		return map;
	}

	/**
	 * Configuration for function parameter list formatting.
	 */
	public static class ParametersConfig {

		/**
		 * Add spaces inside the parentheses of a function parameter list.
		 *
		 * <pre>
		 * // padding: true
		 * function foo( required string name, numeric age ) {}
		 *
		 * // padding: false (default)
		 * function foo(required string name, numeric age) {}
		 * </pre>
		 */
		private boolean	padding			= false;

		/**
		 * Add a space inside empty parameter parentheses.
		 *
		 * <pre>
		 * 
		 * // emptyPadding: true
		 * function foo() {
		 * }
		 *
		 * // emptyPadding: false (default)
		 * function foo() {
		 * }
		 * 
		 * </pre>
		 */
		@JsonProperty( "empty_padding" )
		private boolean	emptyPadding	= false;

		/**
		 * Add a trailing comma after the last parameter when formatted across multiple lines.
		 *
		 * <pre>
		 * // commaDangle: true
		 * function foo(
		 *     required string name,
		 *     numeric age,
		 * ) {}
		 *
		 * // commaDangle: false (default)
		 * function foo(
		 *     required string name,
		 *     numeric age
		 * ) {}
		 * </pre>
		 */
		@JsonProperty( "comma_dangle" )
		private boolean	commaDangle		= false;

		/**
		 * Number of parameters that triggers multiline formatting. If the parameter
		 * count meets or exceeds this threshold, each parameter is placed on its own line.
		 */
		@JsonProperty( "multiline_count" )
		private int		multilineCount	= 4;

		/**
		 * Total character length of the parameter list that triggers multiline formatting.
		 * If the flat-printed parameter list exceeds this length, it switches to multiline.
		 */
		@JsonProperty( "multiline_length" )
		private int		multilineLength	= 60;

		/** Default constructor. */
		public ParametersConfig() {
		}

		/**
		 * Get whether spaces are added inside parameter parentheses.
		 *
		 * @return true if padding is enabled
		 */
		public boolean getPadding() {
			return padding;
		}

		/**
		 * Set whether spaces are added inside parameter parentheses.
		 *
		 * @param padding true to enable padding
		 *
		 * @return this config for chaining
		 */
		public ParametersConfig setPadding( boolean padding ) {
			this.padding = padding;
			return this;
		}

		/**
		 * Get whether spaces are added inside empty parameter parentheses.
		 *
		 * @return true if empty padding is enabled
		 */
		public boolean getEmptyPadding() {
			return emptyPadding;
		}

		/**
		 * Set whether spaces are added inside empty parameter parentheses.
		 *
		 * @param emptyPadding true to enable empty padding
		 *
		 * @return this config for chaining
		 */
		public ParametersConfig setEmptyPadding( boolean emptyPadding ) {
			this.emptyPadding = emptyPadding;
			return this;
		}

		/**
		 * Get whether a trailing comma is added after the last parameter in multiline mode.
		 *
		 * @return true if dangling comma is enabled
		 */
		public boolean getCommaDangle() {
			return commaDangle;
		}

		/**
		 * Set whether a trailing comma is added after the last parameter in multiline mode.
		 *
		 * @param commaDangle true to enable dangling comma
		 *
		 * @return this config for chaining
		 */
		public ParametersConfig setCommaDangle( boolean commaDangle ) {
			this.commaDangle = commaDangle;
			return this;
		}

		/**
		 * Get the parameter count threshold for multiline formatting.
		 *
		 * @return the multiline count threshold
		 */
		public int getMultilineCount() {
			return multilineCount;
		}

		/**
		 * Set the parameter count threshold for multiline formatting.
		 *
		 * @param multilineCount the multiline count threshold
		 *
		 * @return this config for chaining
		 */
		public ParametersConfig setMultilineCount( int multilineCount ) {
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
		public ParametersConfig setMultilineLength( int multilineLength ) {
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

	/**
	 * Configuration for arrow/lambda function formatting.
	 */
	public static class ArrowConfig {

		/**
		 * Controls when parentheses are used around arrow function parameters.
		 * {@code "always"} always wraps parameters in parentheses; {@code "avoid"}
		 * omits parentheses for single-parameter lambdas with no type or default value.
		 *
		 * <pre>
		 * // parens: "always" (default)
		 * list.map( ( item ) -&gt; item.name );
		 *
		 * // parens: "avoid"
		 * list.map( item -&gt; item.name );
		 * </pre>
		 */
		private String parens = "always";

		/** Default constructor. */
		public ArrowConfig() {
		}

		/**
		 * Get the parentheses mode for arrow functions.
		 *
		 * @return the parentheses mode
		 */
		public String getParens() {
			return parens;
		}

		/**
		 * Set the parentheses mode for arrow functions.
		 *
		 * @param parens the parentheses mode ({@code "always"} or {@code "avoid"})
		 *
		 * @return this config for chaining
		 */
		public ArrowConfig setParens( String parens ) {
			this.parens = parens;
			return this;
		}

		/**
		 * Convert this configuration to a map for JSON serialization.
		 *
		 * @return a map representation of this configuration
		 */
		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "parens", parens );
			return map;
		}
	}
}
