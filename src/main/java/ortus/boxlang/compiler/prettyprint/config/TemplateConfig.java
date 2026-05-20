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
 * Configuration options for BoxTemplate/CFTemplate formatting.
 */
public class TemplateConfig {

	/**
	 * Enable template (BXM/CFM) formatting. When true (the default), template sources
	 * are formatted. When false, template sources are returned unchanged.
	 */
	@JsonProperty( "enabled" )
	private boolean	enabled					= true;

	/**
	 * The prefix used for template component tags. Common values are {@code "bx"}
	 * for BoxLang and {@code "cf"} for ColdFusion.
	 *
	 * <pre>
	 * // componentPrefix: "bx" (default)
	 * &lt;bx:if condition="true"&gt;
	 *
	 * // componentPrefix: "cf"
	 * &lt;cfif condition="true"&gt;
	 * </pre>
	 */
	@JsonProperty( "component_prefix" )
	private String	componentPrefix			= "bx";

	/**
	 * Indent the body content of template tags. When true, content inside tags
	 * like {@code <bx:if>} is indented one level and whitespace is cleaned up.
	 *
	 * <pre>
	 * // indentContent: true (default)
	 * &lt;bx:if condition="true"&gt;
	 *     &lt;p&gt;Hello&lt;/p&gt;
	 * &lt;/bx:if&gt;
	 *
	 * // indentContent: false
	 * &lt;bx:if condition="true"&gt;
	 * &lt;p&gt;Hello&lt;/p&gt;
	 * &lt;/bx:if&gt;
	 * </pre>
	 */
	@JsonProperty( "indent_content" )
	private boolean	indentContent			= true;

	/**
	 * Force each attribute in a template tag onto its own line.
	 *
	 * <pre>
	 * // singleAttributePerLine: true
	 * &lt;bx:mail
	 *     to="user@example.com"
	 *     from="admin@example.com"
	 *     subject="Hello"
	 * &gt;
	 *
	 * // singleAttributePerLine: false (default)
	 * &lt;bx:mail to="user@example.com" from="admin@example.com" subject="Hello"&gt;
	 * </pre>
	 */
	@JsonProperty( "single_attribute_per_line" )
	private boolean	singleAttributePerLine	= false;

	/**
	 * Use self-closing syntax for tags without a body.
	 *
	 * <pre>
	 * // selfClosing: true (default)
	 * &lt;bx:set name="foo" /&gt;
	 *
	 * // selfClosing: false
	 * &lt;bx:set name="foo"&gt;&lt;/bx:set&gt;
	 * </pre>
	 */
	@JsonProperty( "self_closing" )
	private boolean	selfClosing				= true;

	/** Default constructor. */
	public TemplateConfig() {
	}

	/**
	 * Get whether template formatting is enabled.
	 *
	 * @return true when template formatting is enabled
	 */
	public boolean getEnabled() {
		return enabled;
	}

	/**
	 * Set whether template formatting is enabled.
	 *
	 * @param enabled true to enable template formatting
	 *
	 * @return this config for chaining
	 */
	public TemplateConfig setEnabled( boolean enabled ) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Get the component tag prefix.
	 *
	 * @return the component prefix
	 */
	public String getComponentPrefix() {
		return componentPrefix;
	}

	/**
	 * Set the component tag prefix.
	 *
	 * @param componentPrefix the component prefix (e.g. {@code "bx"} or {@code "cf"})
	 *
	 * @return this config for chaining
	 */
	public TemplateConfig setComponentPrefix( String componentPrefix ) {
		this.componentPrefix = componentPrefix;
		return this;
	}

	/**
	 * Get whether template tag body content is indented.
	 *
	 * @return true if content indentation is enabled
	 */
	public boolean getIndentContent() {
		return indentContent;
	}

	/**
	 * Set whether template tag body content is indented.
	 *
	 * @param indentContent true to indent content
	 *
	 * @return this config for chaining
	 */
	public TemplateConfig setIndentContent( boolean indentContent ) {
		this.indentContent = indentContent;
		return this;
	}

	/**
	 * Get whether attributes are placed one per line.
	 *
	 * @return true if single attribute per line is enabled
	 */
	public boolean getSingleAttributePerLine() {
		return singleAttributePerLine;
	}

	/**
	 * Set whether attributes are placed one per line.
	 *
	 * @param singleAttributePerLine true to place each attribute on its own line
	 *
	 * @return this config for chaining
	 */
	public TemplateConfig setSingleAttributePerLine( boolean singleAttributePerLine ) {
		this.singleAttributePerLine = singleAttributePerLine;
		return this;
	}

	/**
	 * Get whether self-closing tag syntax is used.
	 *
	 * @return true if self-closing is enabled
	 */
	public boolean getSelfClosing() {
		return selfClosing;
	}

	/**
	 * Set whether self-closing tag syntax is used.
	 *
	 * @param selfClosing true to use self-closing tags
	 *
	 * @return this config for chaining
	 */
	public TemplateConfig setSelfClosing( boolean selfClosing ) {
		this.selfClosing = selfClosing;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "enabled", enabled );
		map.put( "component_prefix", componentPrefix );
		map.put( "indent_content", indentContent );
		map.put( "single_attribute_per_line", singleAttributePerLine );
		map.put( "self_closing", selfClosing );
		return map;
	}

	/**
	 * Create a deep copy of this configuration.
	 *
	 * @return a new TemplateConfig with the same settings
	 */
	public TemplateConfig clone() {
		TemplateConfig clone = new TemplateConfig();
		clone.enabled					= this.enabled;
		clone.componentPrefix			= this.componentPrefix;
		clone.indentContent				= this.indentContent;
		clone.singleAttributePerLine	= this.singleAttributePerLine;
		clone.selfClosing				= this.selfClosing;
		return clone;
	}
}
