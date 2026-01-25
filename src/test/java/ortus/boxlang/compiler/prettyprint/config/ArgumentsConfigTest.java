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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "ArgumentsConfig Tests" )
public class ArgumentsConfigTest {

	@Test
	@DisplayName( "ArgumentsConfig has correct defaults" )
	public void testDefaults() {
		ArgumentsConfig config = new ArgumentsConfig();

		assertFalse( config.getCommaDangle() );
		assertEquals( 4, config.getMultilineCount() );
		assertEquals( 60, config.getMultilineLength() );
	}

	@Test
	@DisplayName( "ArgumentsConfig setters work correctly" )
	public void testSetters() {
		ArgumentsConfig config = new ArgumentsConfig();

		config.setCommaDangle( true );
		assertEquals( true, config.getCommaDangle() );

		config.setMultilineCount( 3 );
		assertEquals( 3, config.getMultilineCount() );

		config.setMultilineLength( 80 );
		assertEquals( 80, config.getMultilineLength() );
	}

	@Test
	@DisplayName( "ArgumentsConfig toMap produces correct structure" )
	public void testToMap() {
		ArgumentsConfig config = new ArgumentsConfig();
		config.setCommaDangle( true );
		config.setMultilineCount( 5 );
		config.setMultilineLength( 100 );

		Map<String, Object> map = config.toMap();

		assertEquals( true, map.get( "comma_dangle" ) );
		assertEquals( 5, map.get( "multiline_count" ) );
		assertEquals( 100, map.get( "multiline_length" ) );
	}

	@Test
	@DisplayName( "Config loads ArgumentsConfig from map" )
	public void testConfigLoadFromMap() {
		Map<String, Object> argumentsMap = new HashMap<>();
		argumentsMap.put( "comma_dangle", true );
		argumentsMap.put( "multiline_count", 6 );
		argumentsMap.put( "multiline_length", 120 );

		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "arguments", argumentsMap );

		Config config = new Config().loadFromConfig( configMap );

		assertEquals( true, config.getArguments().getCommaDangle() );
		assertEquals( 6, config.getArguments().getMultilineCount() );
		assertEquals( 120, config.getArguments().getMultilineLength() );
	}

	@Test
	@DisplayName( "Config includes ArgumentsConfig in toMap" )
	public void testConfigToMapIncludesArguments() {
		Config config = new Config();
		config.getArguments().setCommaDangle( true );

		Map<String, Object> map = config.toMap();

		assertNotNull( map.get( "arguments" ) );
	}
}
