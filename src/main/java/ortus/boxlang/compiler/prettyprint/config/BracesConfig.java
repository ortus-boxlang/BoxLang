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
 * Configuration options for brace style formatting.
 */
public class BracesConfig {

	private String		style						= "same-line";

	@JsonProperty( "require_for_single_statement" )
	private boolean		requireForSingleStatement	= true;

	@JsonProperty( "else" )
	private ElseConfig	elseConfig					= new ElseConfig();

	public BracesConfig() {
	}

	public String getStyle() {
		return style;
	}

	public BracesConfig setStyle( String style ) {
		this.style = style;
		return this;
	}

	public boolean getRequireForSingleStatement() {
		return requireForSingleStatement;
	}

	public BracesConfig setRequireForSingleStatement( boolean requireForSingleStatement ) {
		this.requireForSingleStatement = requireForSingleStatement;
		return this;
	}

	public ElseConfig getElseConfig() {
		return elseConfig;
	}

	public BracesConfig setElseConfig( ElseConfig elseConfig ) {
		this.elseConfig = elseConfig;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "style", style );
		map.put( "require_for_single_statement", requireForSingleStatement );
		map.put( "else", elseConfig.toMap() );
		return map;
	}

	/**
	 * Configuration for else clause formatting.
	 */
	public static class ElseConfig {

		private String style = "same-line";

		public ElseConfig() {
		}

		public String getStyle() {
			return style;
		}

		public ElseConfig setStyle( String style ) {
			this.style = style;
			return this;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "style", style );
			return map;
		}
	}
}
