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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class ScopeWrapperTest {

	@Test
	void testWrapperScope() {
		Key		inVar			= Key.of( "InVar" );
		Key		spoofed			= Key.of( "spoofed" );
		IScope	variablesScope	= new VariablesScope();
		variablesScope.put( inVar, "value" );
		IScope scopeWrapper = new ScopeWrapper( variablesScope, Map.of( spoofed, "foo" ) );

		// The var from variables is found
		assertThat( scopeWrapper.containsKey( inVar ) ).isTrue();
		assertThat( scopeWrapper.get( inVar ) ).isEqualTo( "value" );

		// The spoofed var is found
		assertThat( scopeWrapper.containsKey( spoofed ) ).isTrue();
		assertThat( scopeWrapper.get( spoofed ) ).isEqualTo( "foo" );

		// But the spoofed var doesn't actually exist in the variables scope
		assertThat( variablesScope.containsKey( spoofed ) ).isFalse();
		assertThrows( KeyNotFoundException.class, () -> variablesScope.dereference( spoofed, false ) );
	}

}
