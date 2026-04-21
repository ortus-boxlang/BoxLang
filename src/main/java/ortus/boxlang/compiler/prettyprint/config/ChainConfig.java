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
 * Configuration options for method chain formatting.
 */
public class ChainConfig {

	@JsonProperty( "break_count" )
	private int	breakCount	= 3;

	@JsonProperty( "break_length" )
	private int	breakLength	= 60;

	public ChainConfig() {
	}

	public int getBreakCount() {
		return breakCount;
	}

	public ChainConfig setBreakCount( int breakCount ) {
		this.breakCount = breakCount;
		return this;
	}

	public int getBreakLength() {
		return breakLength;
	}

	public ChainConfig setBreakLength( int breakLength ) {
		this.breakLength = breakLength;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "break_count", breakCount );
		map.put( "break_length", breakLength );
		return map;
	}
}
