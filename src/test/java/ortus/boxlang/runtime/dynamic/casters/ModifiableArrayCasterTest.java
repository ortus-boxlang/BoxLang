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
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class ModifiableArrayCasterTest {

	@DisplayName( "It can cast a BL Array to a array" )
	@Test
	void testItCanCastArray() {
		assertThat( ModifiableArrayCaster.cast( new Array() ) instanceof Array ).isTrue();
	}

	@DisplayName( "It can cast arguments scope to a array" )
	@Test
	void testItCanCastArgumentsScope() {
		assertThat( ModifiableArrayCaster.cast( new ArgumentsScope() ) instanceof Array ).isTrue();
	}

	@DisplayName( "It can cast a Java List to a array" )
	@Test
	void testItCanCastList() {
		assertThat( ModifiableArrayCaster.cast( new ArrayList<Object>() ) instanceof Array ).isTrue();

		// Unmodifiable
		assertThrows( BoxCastException.class, () -> ModifiableArrayCaster.cast( List.of() ) );
		assertThrows( BoxCastException.class, () -> ModifiableArrayCaster.cast( List.copyOf( List.of() ) ) );
	}

	@DisplayName( "It can cast a Java Array to a array" )
	@Test
	void testItCanCastJavaArray() {
		assertThrows( BoxCastException.class, () -> ModifiableArrayCaster.cast( "Brad".getBytes() ) );
	}

}
