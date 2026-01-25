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
 * Configuration options for class/interface formatting.
 */
public class ClassConfig {

	@JsonProperty( "member_order" )
	private String	memberOrder		= "preserve";

	@JsonProperty( "member_spacing" )
	private int		memberSpacing	= 1;

	public ClassConfig() {
	}

	public String getMemberOrder() {
		return memberOrder;
	}

	public ClassConfig setMemberOrder( String memberOrder ) {
		this.memberOrder = memberOrder;
		return this;
	}

	public int getMemberSpacing() {
		return memberSpacing;
	}

	public ClassConfig setMemberSpacing( int memberSpacing ) {
		this.memberSpacing = memberSpacing;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "member_order", memberOrder );
		map.put( "member_spacing", memberSpacing );
		return map;
	}
}
