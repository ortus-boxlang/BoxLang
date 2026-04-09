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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "TemplateConfig Tests" )
public class TemplateConfigTest {

	@Test
	@DisplayName( "TemplateConfig has correct defaults" )
	public void testDefaults() {
		TemplateConfig config = new TemplateConfig();

		assertEquals( "bx", config.getComponentPrefix() );
		assertTrue( config.getIndentContent() );
		assertFalse( config.getSingleAttributePerLine() );
		assertTrue( config.getSelfClosing() );
	}

	@Test
	@DisplayName( "TemplateConfig setters work correctly" )
	public void testSetters() {
		TemplateConfig config = new TemplateConfig();

		config.setComponentPrefix( "cf" );
		assertEquals( "cf", config.getComponentPrefix() );

		config.setIndentContent( false );
		assertFalse( config.getIndentContent() );

		config.setSingleAttributePerLine( true );
		assertTrue( config.getSingleAttributePerLine() );

		config.setSelfClosing( false );
		assertFalse( config.getSelfClosing() );
	}

	@Test
	@DisplayName( "TemplateConfig toMap produces correct structure" )
	public void testToMap() {
		TemplateConfig config = new TemplateConfig();
		config.setComponentPrefix( "cf" );
		config.setIndentContent( false );
		config.setSingleAttributePerLine( true );
		config.setSelfClosing( false );

		Map<String, Object> map = config.toMap();

		assertEquals( "cf", map.get( "component_prefix" ) );
		assertEquals( false, map.get( "indent_content" ) );
		assertEquals( true, map.get( "single_attribute_per_line" ) );
		assertEquals( false, map.get( "self_closing" ) );
	}

	@Test
	@DisplayName( "Config loads TemplateConfig from map" )
	public void testConfigLoadFromMap() {
		Map<String, Object> templateMap = new HashMap<>();
		templateMap.put( "component_prefix", "cf" );
		templateMap.put( "indent_content", false );
		templateMap.put( "single_attribute_per_line", true );
		templateMap.put( "self_closing", false );

		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "template", templateMap );

		Config config = new Config().loadFromConfig( configMap );

		assertEquals( "cf", config.getTemplate().getComponentPrefix() );
		assertFalse( config.getTemplate().getIndentContent() );
		assertTrue( config.getTemplate().getSingleAttributePerLine() );
		assertFalse( config.getTemplate().getSelfClosing() );
	}

	@Test
	@DisplayName( "Config includes TemplateConfig in toMap" )
	public void testConfigToMapIncludesTemplate() {
		Config config = new Config();
		config.getTemplate().setComponentPrefix( "cf" );

		Map<String, Object> map = config.toMap();

		assertNotNull( map.get( "template" ) );
	}
}
