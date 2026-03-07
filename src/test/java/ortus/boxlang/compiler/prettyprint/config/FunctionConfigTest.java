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

@DisplayName( "FunctionConfig Tests" )
public class FunctionConfigTest {

	@Test
	@DisplayName( "FunctionConfig has correct defaults" )
	public void testDefaults() {
		FunctionConfig config = new FunctionConfig();

		assertEquals( "preserve", config.getStyle() );
		assertNotNull( config.getParameters() );
		assertNotNull( config.getArrow() );
	}

	@Test
	@DisplayName( "ParametersConfig has correct defaults" )
	public void testParametersDefaults() {
		FunctionConfig.ParametersConfig params = new FunctionConfig.ParametersConfig();

		assertFalse( params.getCommaDangle() );
		assertEquals( 4, params.getMultilineCount() );
		assertEquals( 60, params.getMultilineLength() );
	}

	@Test
	@DisplayName( "ArrowConfig has correct defaults" )
	public void testArrowDefaults() {
		FunctionConfig.ArrowConfig arrow = new FunctionConfig.ArrowConfig();

		assertEquals( "always", arrow.getParens() );
	}

	@Test
	@DisplayName( "FunctionConfig setters work correctly" )
	public void testSetters() {
		FunctionConfig config = new FunctionConfig();

		config.setStyle( "declaration" );
		assertEquals( "declaration", config.getStyle() );

		config.getParameters().setCommaDangle( true );
		assertEquals( true, config.getParameters().getCommaDangle() );

		config.getParameters().setMultilineCount( 3 );
		assertEquals( 3, config.getParameters().getMultilineCount() );

		config.getParameters().setMultilineLength( 80 );
		assertEquals( 80, config.getParameters().getMultilineLength() );

		config.getArrow().setParens( "avoid" );
		assertEquals( "avoid", config.getArrow().getParens() );
	}

	@Test
	@DisplayName( "FunctionConfig toMap produces correct structure" )
	@SuppressWarnings( "unchecked" )
	public void testToMap() {
		FunctionConfig config = new FunctionConfig();
		config.setStyle( "expression" );
		config.getParameters().setCommaDangle( true );
		config.getParameters().setMultilineCount( 5 );
		config.getParameters().setMultilineLength( 100 );
		config.getArrow().setParens( "avoid" );

		Map<String, Object> map = config.toMap();

		assertEquals( "expression", map.get( "style" ) );

		Map<String, Object> paramsMap = ( Map<String, Object> ) map.get( "parameters" );
		assertNotNull( paramsMap );
		assertEquals( true, paramsMap.get( "comma_dangle" ) );
		assertEquals( 5, paramsMap.get( "multiline_count" ) );
		assertEquals( 100, paramsMap.get( "multiline_length" ) );

		Map<String, Object> arrowMap = ( Map<String, Object> ) map.get( "arrow" );
		assertNotNull( arrowMap );
		assertEquals( "avoid", arrowMap.get( "parens" ) );
	}

	@Test
	@DisplayName( "Config loads FunctionConfig from map" )
	@SuppressWarnings( "unchecked" )
	public void testConfigLoadFromMap() {
		Map<String, Object> functionMap = new HashMap<>();
		functionMap.put( "style", "declaration" );

		Map<String, Object> paramsMap = new HashMap<>();
		paramsMap.put( "comma_dangle", true );
		paramsMap.put( "multiline_count", 6 );
		paramsMap.put( "multiline_length", 120 );
		functionMap.put( "parameters", paramsMap );

		Map<String, Object> arrowMap = new HashMap<>();
		arrowMap.put( "parens", "avoid" );
		functionMap.put( "arrow", arrowMap );

		Map<String, Object> configMap = new HashMap<>();
		configMap.put( "function", functionMap );

		Config config = new Config().loadFromConfig( configMap );

		assertEquals( "declaration", config.getFunction().getStyle() );
		assertEquals( true, config.getFunction().getParameters().getCommaDangle() );
		assertEquals( 6, config.getFunction().getParameters().getMultilineCount() );
		assertEquals( 120, config.getFunction().getParameters().getMultilineLength() );
		assertEquals( "avoid", config.getFunction().getArrow().getParens() );
	}

	@Test
	@DisplayName( "Config includes FunctionConfig in toMap" )
	@SuppressWarnings( "unchecked" )
	public void testConfigToMapIncludesFunction() {
		Config config = new Config();
		config.getFunction().setStyle( "expression" );
		config.getFunction().getArrow().setParens( "avoid" );

		Map<String, Object> map = config.toMap();

		assertNotNull( map.get( "function" ) );
		Map<String, Object> functionMap = ( Map<String, Object> ) map.get( "function" );
		assertEquals( "expression", functionMap.get( "style" ) );

		Map<String, Object> arrowMap = ( Map<String, Object> ) functionMap.get( "arrow" );
		assertEquals( "avoid", arrowMap.get( "parens" ) );
	}

	@Test
	@DisplayName( "FunctionConfig is accessible from main Config" )
	public void testFunctionConfigAccessFromConfig() {
		Config config = new Config();

		assertNotNull( config.getFunction() );
		assertEquals( "preserve", config.getFunction().getStyle() );

		config.getFunction().setStyle( "declaration" );
		assertEquals( "declaration", config.getFunction().getStyle() );
	}
}
