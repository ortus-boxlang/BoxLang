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
 * Configuration options for operator formatting and style.
 */
public class OperatorsConfig {

	/**
	 * Position of binary operators when expressions wrap across multiple lines.
	 * {@code "end"} places the operator at the end of the previous line;
	 * {@code "start"} places it at the start of the next line.
	 *
	 * <pre>
	 * // position: "end" (default)
	 * result = longExpression +
	 *     anotherExpression;
	 *
	 * // position: "start"
	 * result = longExpression
	 *     + anotherExpression;
	 * </pre>
	 */
	private String			position		= "end";

	/**
	 * Style for comparison operators. {@code "symbols"} uses symbolic operators
	 * ({@code ==}, {@code >}, {@code <}); {@code "keywords"} uses word operators
	 * ({@code is}, {@code gt}, {@code lt}); {@code "preserve"} keeps the original style.
	 *
	 * <pre>
	 * // comparisonStyle: "symbols" (default)
	 * if ( a == b &amp;&amp; c &gt; 0 ) {}
	 *
	 * // comparisonStyle: "keywords"
	 * if ( a is b and c gt 0 ) {}
	 * </pre>
	 */
	@JsonProperty( "comparison_style" )
	private String			comparisonStyle	= "symbols";

	/**
	 * Configuration for ternary operator formatting.
	 *
	 * @see TernaryConfig
	 */
	private TernaryConfig	ternary			= new TernaryConfig();

	/** Default constructor. */
	public OperatorsConfig() {
	}

	/**
	 * Get the multiline operator position.
	 *
	 * @return the operator position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * Set the multiline operator position.
	 *
	 * @param position the operator position ({@code "end"} or {@code "start"})
	 *
	 * @return this config for chaining
	 */
	public OperatorsConfig setPosition( String position ) {
		this.position = position;
		return this;
	}

	/**
	 * Get the comparison operator style.
	 *
	 * @return the comparison style
	 */
	public String getComparisonStyle() {
		return comparisonStyle;
	}

	/**
	 * Set the comparison operator style.
	 *
	 * @param comparisonStyle the comparison style ({@code "symbols"}, {@code "keywords"}, or {@code "preserve"})
	 *
	 * @return this config for chaining
	 */
	public OperatorsConfig setComparisonStyle( String comparisonStyle ) {
		this.comparisonStyle = comparisonStyle;
		return this;
	}

	/**
	 * Get the ternary operator configuration.
	 *
	 * @return the ternary configuration
	 */
	public TernaryConfig getTernary() {
		return ternary;
	}

	/**
	 * Set the ternary operator configuration.
	 *
	 * @param ternary the ternary configuration to set
	 *
	 * @return this config for chaining
	 */
	public OperatorsConfig setTernary( TernaryConfig ternary ) {
		this.ternary = ternary;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "position", position );
		map.put( "comparison_style", comparisonStyle );
		map.put( "ternary", ternary.toMap() );
		return map;
	}

	/**
	 * Create a deep copy of this configuration.
	 *
	 * @return a new OperatorsConfig with the same settings
	 */
	public OperatorsConfig clone() {
		OperatorsConfig clone = new OperatorsConfig();
		clone.position			= this.position;
		clone.comparisonStyle	= this.comparisonStyle;
		clone.ternary			= this.ternary.clone();
		return clone;
	}

	/**
	 * Configuration for ternary ({@code ? :}) operator formatting.
	 */
	public static class TernaryConfig {

		/**
		 * Ternary formatting style. {@code "flat"} keeps the expression on one line when possible;
		 * {@code "always-multiline"} always breaks across lines; {@code "preserve"} keeps the
		 * original source formatting.
		 */
		private String	style				= "flat";

		/**
		 * Position of the {@code ?} and {@code :} operators in multiline ternary expressions.
		 * {@code "start"} places them at the start of continuation lines;
		 * {@code "end"} places them at the end of the previous line.
		 *
		 * <pre>
		 * // questionPosition: "start" (default)
		 * result = condition
		 *     ? trueValue
		 *     : falseValue;
		 *
		 * // questionPosition: "end"
		 * result = condition ? trueValue : falseValue;
		 * </pre>
		 */
		@JsonProperty( "question_position" )
		private String	questionPosition	= "start";

		/** Default constructor. */
		public TernaryConfig() {
		}

		/**
		 * Get the ternary formatting style.
		 *
		 * @return the ternary style
		 */
		public String getStyle() {
			return style;
		}

		/**
		 * Set the ternary formatting style.
		 *
		 * @param style the ternary style ({@code "flat"}, {@code "always-multiline"}, or {@code "preserve"})
		 *
		 * @return this config for chaining
		 */
		public TernaryConfig setStyle( String style ) {
			this.style = style;
			return this;
		}

		/**
		 * Get the question/colon operator position in multiline ternaries.
		 *
		 * @return the question position
		 */
		public String getQuestionPosition() {
			return questionPosition;
		}

		/**
		 * Set the question/colon operator position in multiline ternaries.
		 *
		 * @param questionPosition the question position ({@code "start"} or {@code "end"})
		 *
		 * @return this config for chaining
		 */
		public TernaryConfig setQuestionPosition( String questionPosition ) {
			this.questionPosition = questionPosition;
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
			map.put( "question_position", questionPosition );
			return map;
		}

		/**
		 * Create a deep copy of this configuration.
		 *
		 * @return a new TernaryConfig with the same settings
		 */
		public TernaryConfig clone() {
			TernaryConfig clone = new TernaryConfig();
			clone.style				= this.style;
			clone.questionPosition	= this.questionPosition;
			return clone;
		}
	}
}
