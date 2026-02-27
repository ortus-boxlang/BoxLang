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
 * Configuration options for function formatting.
 */
public class FunctionConfig {

	private String				style		= "preserve";
	private ParametersConfig	parameters	= new ParametersConfig();
	private ArrowConfig			arrow		= new ArrowConfig();

	public FunctionConfig() {
	}

	public String getStyle() {
		return style;
	}

	public FunctionConfig setStyle( String style ) {
		this.style = style;
		return this;
	}

	public ParametersConfig getParameters() {
		return parameters;
	}

	public FunctionConfig setParameters( ParametersConfig parameters ) {
		this.parameters = parameters;
		return this;
	}

	public ArrowConfig getArrow() {
		return arrow;
	}

	public FunctionConfig setArrow( ArrowConfig arrow ) {
		this.arrow = arrow;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "style", style );
		map.put( "parameters", parameters.toMap() );
		map.put( "arrow", arrow.toMap() );
		return map;
	}

	/**
	 * Configuration for function parameter lists.
	 */
	public static class ParametersConfig {

		@JsonProperty( "comma_dangle" )
		private boolean	commaDangle		= false;

		@JsonProperty( "multiline_count" )
		private int		multilineCount	= 4;

		@JsonProperty( "multiline_length" )
		private int		multilineLength	= 60;

		public ParametersConfig() {
		}

		public boolean getCommaDangle() {
			return commaDangle;
		}

		public ParametersConfig setCommaDangle( boolean commaDangle ) {
			this.commaDangle = commaDangle;
			return this;
		}

		public int getMultilineCount() {
			return multilineCount;
		}

		public ParametersConfig setMultilineCount( int multilineCount ) {
			this.multilineCount = multilineCount;
			return this;
		}

		public int getMultilineLength() {
			return multilineLength;
		}

		public ParametersConfig setMultilineLength( int multilineLength ) {
			this.multilineLength = multilineLength;
			return this;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
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

		private String parens = "always";

		public ArrowConfig() {
		}

		public String getParens() {
			return parens;
		}

		public ArrowConfig setParens( String parens ) {
			this.parens = parens;
			return this;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "parens", parens );
			return map;
		}
	}
}
