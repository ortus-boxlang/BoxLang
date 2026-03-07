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
 * Configuration options for function call arguments formatting.
 */
public class ArgumentsConfig {

	@JsonProperty( "comma_dangle" )
	private boolean	commaDangle		= false;

	@JsonProperty( "multiline_count" )
	private int		multilineCount	= 4;

	@JsonProperty( "multiline_length" )
	private int		multilineLength	= 60;

	public ArgumentsConfig() {
	}

	public boolean getCommaDangle() {
		return commaDangle;
	}

	public ArgumentsConfig setCommaDangle( boolean commaDangle ) {
		this.commaDangle = commaDangle;
		return this;
	}

	public int getMultilineCount() {
		return multilineCount;
	}

	public ArgumentsConfig setMultilineCount( int multilineCount ) {
		this.multilineCount = multilineCount;
		return this;
	}

	public int getMultilineLength() {
		return multilineLength;
	}

	public ArgumentsConfig setMultilineLength( int multilineLength ) {
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
