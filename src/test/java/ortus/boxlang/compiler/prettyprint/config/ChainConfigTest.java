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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "ChainConfig Tests" )
public class ChainConfigTest {

	@Test
	@DisplayName( "ChainConfig has correct defaults" )
	public void testDefaults() {
		ChainConfig config = new ChainConfig();

		assertEquals( 3, config.getBreakCount() );
		assertEquals( 60, config.getBreakLength() );
	}

	@Test
	@DisplayName( "ChainConfig setters work correctly" )
	public void testSetters() {
		ChainConfig config = new ChainConfig();

		config.setBreakCount( 5 );
		assertEquals( 5, config.getBreakCount() );

		config.setBreakLength( 80 );
		assertEquals( 80, config.getBreakLength() );
	}

	@Test
	@DisplayName( "ChainConfig toMap produces correct structure" )
	public void testToMap() {
		ChainConfig config = new ChainConfig();
		config.setBreakCount( 5 );
		config.setBreakLength( 100 );

		Map<String, Object> map = config.toMap();

		assertEquals( 5, map.get( "break_count" ) );
		assertEquals( 100, map.get( "break_length" ) );
	}

	@Test
	@DisplayName( "Config loads ChainConfig from map" )
	public void testConfigLoadFromMap() {
		Map<String, Object> chainMap = new HashMap<>();
		chainMap.put( "break_count", 4 );
		chainMap.put( "break_length", 120 );

		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "chain", chainMap );

		Config config = new Config().loadFromConfig( configMap );

		assertEquals( 4, config.getChain().getBreakCount() );
		assertEquals( 120, config.getChain().getBreakLength() );
	}

	@Test
	@DisplayName( "Config includes ChainConfig in toMap" )
	public void testConfigToMapIncludesChain() {
		Config config = new Config();
		config.getChain().setBreakCount( 5 );

		Map<String, Object> map = config.toMap();

		assertNotNull( map.get( "chain" ) );
	}
}
