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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.JsonNavigator.Navigator;

public class JsonNavigatorTest {

	private JsonNavigator jsonNavigator;

	@DisplayName( "Test an invalid path" )
	@Test
	void testInvalidPath() {
		assertThrows( BoxIOException.class, () -> {
			JsonNavigator.of( "invalidpath" );
		} );
	}

	@DisplayName( "Test a valid path" )
	@Test
	void testValidPath() {
		Navigator nav = JsonNavigator.of( "src/modules/test/box.json" );

		assertThat( nav.getAsString( "name" ) ).isEqualTo( "BoxLang Test Module" );
		assertThat( nav.getAsInteger( "count" ) ).isEqualTo( 1 );
		assertThat( nav.getAsBoolean( "isactive" ) ).isTrue();
		assertThat( nav.getAsBoolean( "isActiveTruthy" ) ).isTrue();
		assertThat( nav.getAsArray( "keywords" ) ).isNotEmpty();
		assertThat( nav.getAsStruct( "boxlang" ) ).isNotNull();

		assertThat( nav.from( "boxlang" ).get( "moduleName" ) ).isEqualTo( "test" );
	}

	@DisplayName( "Can navigate nested segments" )
	@Test
	void testNestedSegments() {
		Navigator	nav		= JsonNavigator.of( "src/modules/test/box.json" );

		String		name	= nav
		    .from( "boxlang", "settings" )
		    .getAsString( "hello" );

		assertThat( name ).isEqualTo( "luis" );
	}

	@DisplayName( "Cannot navigate non-existent segments" )
	@Test
	void testNonExistentSegments() {
		Navigator nav = JsonNavigator.of( "src/modules/test/box.json" );

		assertThat( nav.from( "boxlang", "settings", "nonexistent" ).get( "bogus", null ) ).isNull();
	}

	@DisplayName( "Can get nested segments" )
	@Test
	void testGetNestedSegments() {
		Navigator nav = JsonNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang", "settings", "hello" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "Test nested has" )
	@Test
	void testNestedHas() {
		Navigator nav = JsonNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "bogus" ) ).isFalse();
		assertThat( nav.has( "boxlang", "settings", "hello" ) ).isTrue();
		assertThat( nav.has( "boxlang", "settings", "nonexistent" ) ).isFalse();
	}

}
