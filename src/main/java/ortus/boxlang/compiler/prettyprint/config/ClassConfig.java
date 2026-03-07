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

	@JsonProperty( "property_order" )
	private String	propertyOrder	= "preserve";

	@JsonProperty( "method_order" )
	private String	methodOrder		= "preserve";

	@JsonProperty( "method_grouping" )
	private boolean	methodGrouping	= false;

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

	public String getPropertyOrder() {
		return propertyOrder;
	}

	public ClassConfig setPropertyOrder( String propertyOrder ) {
		this.propertyOrder = propertyOrder;
		return this;
	}

	public String getMethodOrder() {
		return methodOrder;
	}

	public ClassConfig setMethodOrder( String methodOrder ) {
		this.methodOrder = methodOrder;
		return this;
	}

	public boolean getMethodGrouping() {
		return methodGrouping;
	}

	public ClassConfig setMethodGrouping( boolean methodGrouping ) {
		this.methodGrouping = methodGrouping;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "member_order", memberOrder );
		map.put( "member_spacing", memberSpacing );
		map.put( "property_order", propertyOrder );
		map.put( "method_order", methodOrder );
		map.put( "method_grouping", methodGrouping );
		return map;
	}
}
