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
 * Configuration options for operator formatting.
 */
public class OperatorsConfig {

	private String			position	= "end";
	private TernaryConfig	ternary		= new TernaryConfig();

	public OperatorsConfig() {
	}

	public String getPosition() {
		return position;
	}

	public OperatorsConfig setPosition( String position ) {
		this.position = position;
		return this;
	}

	public TernaryConfig getTernary() {
		return ternary;
	}

	public OperatorsConfig setTernary( TernaryConfig ternary ) {
		this.ternary = ternary;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "position", position );
		map.put( "ternary", ternary.toMap() );
		return map;
	}

	/**
	 * Configuration for ternary operator formatting.
	 */
	public static class TernaryConfig {

		private String	style				= "flat";

		@JsonProperty( "question_position" )
		private String	questionPosition	= "start";

		public TernaryConfig() {
		}

		public String getStyle() {
			return style;
		}

		public TernaryConfig setStyle( String style ) {
			this.style = style;
			return this;
		}

		public String getQuestionPosition() {
			return questionPosition;
		}

		public TernaryConfig setQuestionPosition( String questionPosition ) {
			this.questionPosition = questionPosition;
			return this;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "style", style );
			map.put( "question_position", questionPosition );
			return map;
		}
	}
}
