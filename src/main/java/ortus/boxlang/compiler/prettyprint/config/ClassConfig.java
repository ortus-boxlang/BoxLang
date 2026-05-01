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
 * Configuration options for class and interface body formatting.
 */
public class ClassConfig {

	/**
	 * Strategy for ordering class members (properties, methods, etc.).
	 * {@code "preserve"} keeps the original source order.
	 */
	@JsonProperty( "member_order" )
	private String	memberOrder		= "preserve";

	/**
	 * Number of blank lines printed between class members (methods, properties, etc.).
	 *
	 * <pre>
	 * 
	 * // memberSpacing: 1 (default)
	 * function foo() {
	 * }
	 *
	 * function bar() {
	 * }
	 *
	 * // memberSpacing: 2
	 * function foo() {
	 * }
	 *
	 * function bar() {
	 * }
	 * </pre>
	 */
	@JsonProperty( "member_spacing" )
	private int		memberSpacing	= 1;

	/**
	 * Strategy for ordering property declarations within a class.
	 * Options: {@code "preserve"} (default), {@code "alphabetical"}, {@code "length"}, {@code "type"}.
	 */
	@JsonProperty( "property_order" )
	private String	propertyOrder	= "preserve";

	/**
	 * Strategy for ordering method declarations within a class.
	 * Options: {@code "preserve"} (default), {@code "alphabetical"}.
	 */
	@JsonProperty( "method_order" )
	private String	methodOrder		= "preserve";

	/**
	 * Group methods by access modifier or type when sorting.
	 * When true, methods are first grouped (e.g. public before private),
	 * then sorted within each group.
	 */
	@JsonProperty( "method_grouping" )
	private boolean	methodGrouping	= false;

	/** Default constructor. */
	public ClassConfig() {
	}

	/**
	 * Get the member ordering strategy.
	 *
	 * @return the member order strategy
	 */
	public String getMemberOrder() {
		return memberOrder;
	}

	/**
	 * Set the member ordering strategy.
	 *
	 * @param memberOrder the member order strategy
	 *
	 * @return this config for chaining
	 */
	public ClassConfig setMemberOrder( String memberOrder ) {
		this.memberOrder = memberOrder;
		return this;
	}

	/**
	 * Get the number of blank lines between class members.
	 *
	 * @return the member spacing
	 */
	public int getMemberSpacing() {
		return memberSpacing;
	}

	/**
	 * Set the number of blank lines between class members.
	 *
	 * @param memberSpacing the member spacing
	 *
	 * @return this config for chaining
	 */
	public ClassConfig setMemberSpacing( int memberSpacing ) {
		this.memberSpacing = memberSpacing;
		return this;
	}

	/**
	 * Get the property ordering strategy.
	 *
	 * @return the property order strategy
	 */
	public String getPropertyOrder() {
		return propertyOrder;
	}

	/**
	 * Set the property ordering strategy.
	 *
	 * @param propertyOrder the property order strategy
	 *
	 * @return this config for chaining
	 */
	public ClassConfig setPropertyOrder( String propertyOrder ) {
		this.propertyOrder = propertyOrder;
		return this;
	}

	/**
	 * Get the method ordering strategy.
	 *
	 * @return the method order strategy
	 */
	public String getMethodOrder() {
		return methodOrder;
	}

	/**
	 * Set the method ordering strategy.
	 *
	 * @param methodOrder the method order strategy
	 *
	 * @return this config for chaining
	 */
	public ClassConfig setMethodOrder( String methodOrder ) {
		this.methodOrder = methodOrder;
		return this;
	}

	/**
	 * Get whether methods are grouped by access modifier.
	 *
	 * @return true if method grouping is enabled
	 */
	public boolean getMethodGrouping() {
		return methodGrouping;
	}

	/**
	 * Set whether methods are grouped by access modifier.
	 *
	 * @param methodGrouping true to enable method grouping
	 *
	 * @return this config for chaining
	 */
	public ClassConfig setMethodGrouping( boolean methodGrouping ) {
		this.methodGrouping = methodGrouping;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
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
