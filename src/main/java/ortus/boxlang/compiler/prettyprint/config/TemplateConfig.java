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

	@JsonProperty( "component_prefix" )
	private String	componentPrefix			= "bx";

	@JsonProperty( "indent_content" )
	private boolean	indentContent			= true;

	@JsonProperty( "single_attribute_per_line" )
	private boolean	singleAttributePerLine	= false;

	@JsonProperty( "self_closing" )
	private boolean	selfClosing				= true;

	public TemplateConfig() {
	}

	public String getComponentPrefix() {
		return componentPrefix;
	}

	public TemplateConfig setComponentPrefix( String componentPrefix ) {
		this.componentPrefix = componentPrefix;
		return this;
	}

	public boolean getIndentContent() {
		return indentContent;
	}

	public TemplateConfig setIndentContent( boolean indentContent ) {
		this.indentContent = indentContent;
		return this;
	}

	public boolean getSingleAttributePerLine() {
		return singleAttributePerLine;
	}

	public TemplateConfig setSingleAttributePerLine( boolean singleAttributePerLine ) {
		this.singleAttributePerLine = singleAttributePerLine;
		return this;
	}

	public boolean getSelfClosing() {
		return selfClosing;
	}

	public TemplateConfig setSelfClosing( boolean selfClosing ) {
		this.selfClosing = selfClosing;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "component_prefix", componentPrefix );
		map.put( "indent_content", indentContent );
		map.put( "single_attribute_per_line", singleAttributePerLine );
		map.put( "self_closing", selfClosing );
		return map;
	}
}
