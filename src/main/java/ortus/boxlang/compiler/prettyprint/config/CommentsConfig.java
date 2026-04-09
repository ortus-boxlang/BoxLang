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

	@JsonProperty( "preserve_blank_lines" )
	private boolean	preserveBlankLines	= true;

	private boolean	wrap				= false;

	public CommentsConfig() {
	}

	public boolean getPreserveBlankLines() {
		return preserveBlankLines;
	}

	public CommentsConfig setPreserveBlankLines( boolean preserveBlankLines ) {
		this.preserveBlankLines = preserveBlankLines;
		return this;
	}

	public boolean getWrap() {
		return wrap;
	}

	public CommentsConfig setWrap( boolean wrap ) {
		this.wrap = wrap;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "preserve_blank_lines", preserveBlankLines );
		map.put( "wrap", wrap );
		return map;
	}
}
