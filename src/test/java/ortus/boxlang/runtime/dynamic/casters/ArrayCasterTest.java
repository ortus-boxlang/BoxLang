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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ArrayCasterTest {

	@DisplayName( "It can cast a BL Array to a array" )
	@Test
	void testItCanCastArray() {
		assertThat( ArrayCaster.cast( new Array() ) instanceof Array ).isTrue();
	}

	@DisplayName( "It can cast arguments scope to a array" )
	@Test
	void testItCanCastArgumentsScope() {
		assertThat( ArrayCaster.cast( new ArgumentsScope() ) instanceof Array ).isTrue();
	}

	@DisplayName( "It can cast a Java List to a array" )
	@Test
	void testItCanCastList() {
		assertThat( ArrayCaster.cast( new ArrayList<Object>() ) instanceof Array ).isTrue();
		// immutable
		assertThat( ArrayCaster.cast( List.of() ) instanceof Array ).isTrue();
		assertThat( ArrayCaster.cast( List.copyOf( List.of() ) ) instanceof Array ).isTrue();
	}

	@DisplayName( "It can cast a Java Array to a array" )
	@Test
	void testItCanCastJavaArray() {
		assertThat( ArrayCaster.cast( "Brad".getBytes() ) instanceof Array ).isTrue();
		Array result = ArrayCaster.cast( "Brad".getBytes() );
		assertThat( result.get( 0 ) ).isEqualTo( ( int ) 'B' );
		assertThat( result.get( 1 ) ).isEqualTo( ( int ) 'r' );
		assertThat( result.get( 2 ) ).isEqualTo( ( int ) 'a' );
		assertThat( result.get( 3 ) ).isEqualTo( ( int ) 'd' );
	}

	@DisplayName( "It should no cast Integer to Java Array" )
	@Test
	void testThrows() {
		assertThrows( BoxRuntimeException.class, () -> {
			ArrayCaster.cast( Integer.parseInt("123"), true);
		} );
	}

}
