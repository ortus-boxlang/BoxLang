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
package ortus.boxlang.runtime.config.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.util.FileSystemUtil;

public class PlaceholderHelperTest {

	@DisplayName( "PlaceholderHelper.resolve() should resolve placeholders in the input string with no case sensitivity" )
	@Test
	public void testResolveWithValidPlaceholders() {
		String	input		= "User home directory: ${boxlang-HOME}, Temp directory: ${java-temp}";
		String	expected	= "User home directory: " + System.getProperty( "user.home" ) + FileSystemUtil.LINE_SEPARATOR + ".boxlang, Temp directory: "
		    + System.getProperty( "java.io.tmpdir" );

		String	resolved	= PlaceholderHelper.resolve( input );

		assertThat( resolved ).isEqualTo( expected );
	}

	@DisplayName( "PlaceholderHelper.resolve() should not resolve unknown placeholders" )
	@Test
	public void testResolveWithUnknownPlaceholder() {
		String	input		= "Unknown placeholder: ${unknown-placeholder}";
		String	expected	= "Unknown placeholder: ${unknown-placeholder}";

		String	resolved	= PlaceholderHelper.resolve( input );

		assertThat( resolved ).isEqualTo( expected );
	}

	@DisplayName( "PlaceholderHelper.resolve() should not resolve placeholders with invalid syntax" )
	@Test
	public void testResolveWithMixedPlaceholders() {
		String	input		= "User home directory: ${boxlang-home}, Unknown placeholder: ${unknown-placeholder}";
		String	expected	= "User home directory: " + System.getProperty( "user.home" ) + FileSystemUtil.LINE_SEPARATOR + ".boxlang"
		    + ", Unknown placeholder: ${unknown-placeholder}";

		String	resolved	= PlaceholderHelper.resolve( input );
		assertThat( resolved ).isEqualTo( expected );
	}

	@DisplayName( "PlaceholderHelper.resolve() should not resolve placeholders with invalid syntax" )
	@Test
	public void testResolveWithEmptyInput() {
		String	input		= "";
		String	expected	= "";

		String	resolved	= PlaceholderHelper.resolve( input );

		assertThat( resolved ).isEqualTo( expected );
	}

	@DisplayName( "Placeholder can replace with a custom map" )
	@Test
	public void testResolveWithCustomMap() {
		String				input		= "User home directory: ${test1}, Temp directory: ${test2} and ${test3}";
		String				expected	= "User home directory: test, Temp directory: love and ${test3}";
		Map<String, String>	map			= Map.of( "test1", "test", "test2", "love" );

		String				resolved	= PlaceholderHelper.resolve( input, map );

		assertThat( resolved ).isEqualTo( expected );
	}

	@DisplayName( "Placeholder can replace with a custom map and default values" )
	@Test
	public void testResolveWithCustomMapAndDefaultValues() {
		String				input		= "User home directory: ${test1}, Temp directory: ${test2} and ${test3:awesome}";
		String				expected	= "User home directory: test, Temp directory: love and awesome";
		Map<String, String>	map			= Map.of( "test1", "test", "test2", "love" );

		String				resolved	= PlaceholderHelper.resolve( input, map );

		assertThat( resolved ).isEqualTo( expected );
	}

	@DisplayName( "Placeholder can replace more than one placeholder of the same name" )
	@Test
	public void testResolveMultiple() {
		String				input		= "foo ${brad} bar ${brad} baz ${brad} bum";
		String				expected	= "foo wood bar wood baz wood bum";
		Map<String, String>	map			= Map.of( "brad", "wood" );

		String				resolved	= PlaceholderHelper.resolve( input, map );

		assertThat( resolved ).isEqualTo( expected );
	}

}
