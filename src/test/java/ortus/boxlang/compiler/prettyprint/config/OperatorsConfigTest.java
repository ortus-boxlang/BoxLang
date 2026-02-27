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

@DisplayName( "OperatorsConfig Tests" )
public class OperatorsConfigTest {

	@Test
	@DisplayName( "OperatorsConfig has correct defaults" )
	public void testDefaults() {
		OperatorsConfig config = new OperatorsConfig();

		assertEquals( "end", config.getPosition() );
		assertNotNull( config.getTernary() );
	}

	@Test
	@DisplayName( "TernaryConfig has correct defaults" )
	public void testTernaryDefaults() {
		OperatorsConfig.TernaryConfig ternary = new OperatorsConfig.TernaryConfig();

		assertEquals( "flat", ternary.getStyle() );
		assertEquals( "start", ternary.getQuestionPosition() );
	}

	@Test
	@DisplayName( "OperatorsConfig setters work correctly" )
	public void testSetters() {
		OperatorsConfig config = new OperatorsConfig();

		config.setPosition( "start" );
		assertEquals( "start", config.getPosition() );

		config.getTernary().setStyle( "always-multiline" );
		assertEquals( "always-multiline", config.getTernary().getStyle() );

		config.getTernary().setQuestionPosition( "end" );
		assertEquals( "end", config.getTernary().getQuestionPosition() );
	}

	@Test
	@DisplayName( "OperatorsConfig toMap produces correct structure" )
	@SuppressWarnings( "unchecked" )
	public void testToMap() {
		OperatorsConfig config = new OperatorsConfig();
		config.setPosition( "start" );
		config.getTernary().setStyle( "always-multiline" );
		config.getTernary().setQuestionPosition( "end" );

		Map<String, Object> map = config.toMap();

		assertEquals( "start", map.get( "position" ) );

		Map<String, Object> ternaryMap = ( Map<String, Object> ) map.get( "ternary" );
		assertNotNull( ternaryMap );
		assertEquals( "always-multiline", ternaryMap.get( "style" ) );
		assertEquals( "end", ternaryMap.get( "question_position" ) );
	}

	@Test
	@DisplayName( "Config loads OperatorsConfig from map" )
	public void testConfigLoadFromMap() {
		Map<String, Object> ternaryMap = new HashMap<>();
		ternaryMap.put( "style", "always-multiline" );
		ternaryMap.put( "question_position", "end" );

		Map<String, Object> operatorsMap = new HashMap<>();
		operatorsMap.put( "position", "start" );
		operatorsMap.put( "ternary", ternaryMap );

		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "operators", operatorsMap );

		Config config = new Config().loadFromConfig( configMap );

		assertEquals( "start", config.getOperators().getPosition() );
		assertEquals( "always-multiline", config.getOperators().getTernary().getStyle() );
		assertEquals( "end", config.getOperators().getTernary().getQuestionPosition() );
	}
}
