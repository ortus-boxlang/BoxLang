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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "BracesConfig Tests" )
public class BracesConfigTest {

	@Test
	@DisplayName( "BracesConfig has correct defaults" )
	public void testDefaults() {
		BracesConfig config = new BracesConfig();

		assertEquals( "same-line", config.getStyle() );
		assertTrue( config.getRequireForSingleStatement() );
		assertNotNull( config.getElseConfig() );
	}

	@Test
	@DisplayName( "ElseConfig has correct defaults" )
	public void testElseDefaults() {
		BracesConfig.ElseConfig elseConfig = new BracesConfig.ElseConfig();

		assertEquals( "same-line", elseConfig.getStyle() );
	}

	@Test
	@DisplayName( "BracesConfig setters work correctly" )
	public void testSetters() {
		BracesConfig config = new BracesConfig();

		config.setStyle( "new-line" );
		assertEquals( "new-line", config.getStyle() );

		config.setRequireForSingleStatement( false );
		assertEquals( false, config.getRequireForSingleStatement() );

		config.getElseConfig().setStyle( "new-line" );
		assertEquals( "new-line", config.getElseConfig().getStyle() );
	}

	@Test
	@DisplayName( "BracesConfig toMap produces correct structure" )
	@SuppressWarnings( "unchecked" )
	public void testToMap() {
		BracesConfig config = new BracesConfig();
		config.setStyle( "new-line" );
		config.setRequireForSingleStatement( false );
		config.getElseConfig().setStyle( "new-line" );

		Map<String, Object> map = config.toMap();

		assertEquals( "new-line", map.get( "style" ) );
		assertEquals( false, map.get( "require_for_single_statement" ) );

		Map<String, Object> elseMap = ( Map<String, Object> ) map.get( "else" );
		assertNotNull( elseMap );
		assertEquals( "new-line", elseMap.get( "style" ) );
	}

	@Test
	@DisplayName( "Config loads BracesConfig from map" )
	public void testConfigLoadFromMap() {
		Map<String, Object> elseMap = new HashMap<>();
		elseMap.put( "style", "new-line" );

		Map<String, Object> bracesMap = new HashMap<>();
		bracesMap.put( "style", "new-line" );
		bracesMap.put( "require_for_single_statement", false );
		bracesMap.put( "else", elseMap );

		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "braces", bracesMap );

		Config config = new Config().loadFromConfig( configMap );

		assertEquals( "new-line", config.getBraces().getStyle() );
		assertEquals( false, config.getBraces().getRequireForSingleStatement() );
		assertEquals( "new-line", config.getBraces().getElseConfig().getStyle() );
	}
}
