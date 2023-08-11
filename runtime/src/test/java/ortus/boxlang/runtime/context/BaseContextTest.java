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

package ortus.boxlang.runtime.context;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;

public class BaseContextTest {

	private BaseContext context;

	@BeforeEach
	void setUp() {
		context = new BaseContext();
	}

	@Test
	void testGetName() {
		assertThat( context.getName() ).isEqualTo( "base" );
	}

	@Test
	void testGetVariablesScope() {
		assertNotNull( context.getVariablesScope() );
		assertThat( context.getVariablesScope().getName() ).isEqualTo( "variables" );
	}

	@Test
	void testScopeFindKeyFound() {
		Key		key		= Key.of( "testKey" );
		Object	value	= "testValue";
		context.getVariablesScope().put( key, value );
		assertEquals( value, context.scopeFind( key ) ); // Key should be found in variables scope
	}

	@Test
	void testScopeFindKeyNotFound() {
		assertThrows( KeyNotFoundException.class, () -> context.scopeFind( new Key( "nonExistentKey" ) ) );
	}

}
