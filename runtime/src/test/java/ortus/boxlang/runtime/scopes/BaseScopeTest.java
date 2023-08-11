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
package ortus.boxlang.runtime.scopes;

import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.CastException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

import static com.google.common.truth.Truth.assertThat;

public class BaseScopeTest {

	private static BaseScope scope;

	@BeforeAll
	public static void setUp() {
		scope = new BaseScope( "test" );
	}

	@Test
	void testBasicGetAndSet() {
		// Test getValue() and setValue()
		assertThrows( KeyNotFoundException.class, () -> scope.get( Key.of( "InvalidKey" ) ) );

		Key		key		= Key.of( "testKey" );
		Object	value	= "testValue";
		scope.put( key, value );
		assertThat( scope.get( key ) ).isEqualTo( value );
		assertThat( scope.get( Key.of( "TestKey" ) ) ).isEqualTo( value );
	}

}
