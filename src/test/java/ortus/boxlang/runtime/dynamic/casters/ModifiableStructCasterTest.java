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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class ModifiableStructCasterTest {

	@DisplayName( "It can cast a BL Struct to a Struct" )
	@Test
	void testItCanCastStruct() {
		assertThat( ModifiableStructCaster.cast( new Struct() ) instanceof IStruct ).isTrue();
	}

	@DisplayName( "It can cast arguments scope to a Struct" )
	@Test
	void testItCanCastArgumentsScope() {
		assertThat( ModifiableStructCaster.cast( new ArgumentsScope() ) instanceof IStruct ).isTrue();
	}

	@DisplayName( "It can cast a Java Map to a Struct" )
	@Test
	void testItCanCastList() {
		assertThat( ModifiableStructCaster.cast( new HashMap<Object, Object>() ) instanceof IStruct ).isTrue();

		// immutable

		assertThrows( BoxCastException.class, () -> ModifiableStructCaster.cast( Map.of() ) );
		assertThrows( BoxCastException.class, () -> ModifiableStructCaster.cast( Map.copyOf( Map.of() ) ) );

		// unmodifable Collection
		Map<String, String> modifiableMap = new HashMap<>();
		modifiableMap.put( "key1", "value1" );
		modifiableMap.put( "key2", "value2" );

		Map<String, String> unmodifiableMap = Collections.unmodifiableMap( modifiableMap );

		assertThrows( BoxCastException.class, () -> ModifiableStructCaster.cast( unmodifiableMap ) );

	}

}
