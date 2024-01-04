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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class PlusTest {

	private IBoxContext context = new ScriptingBoxContext();

	@DisplayName( "It can add numbers" )
	@Test
	void testItCanAddNumbers() {
		assertThat( Plus.invoke( 3, 2 ) ).isEqualTo( 5 );
		assertThat( Plus.invoke( 3.5, 2.5 ) ).isEqualTo( 6 );
	}

	@DisplayName( "It can add strings" )
	@Test
	void testItCanAddStrings() {
		assertThat( Plus.invoke( "3", "2" ) ).isEqualTo( 5 );
		assertThat( Plus.invoke( "3.5", "2.5" ) ).isEqualTo( 6 );
	}

	@DisplayName( "It can compound add" )
	@Test
	void testItCanCompountAdd() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), 4 );
		assertThat( Plus.invoke( context, scope, Key.of( "i" ), 2 ) ).isEqualTo( 6 );
		assertThat( scope.get( Key.of( "i" ) ) ).isEqualTo( 6 );
	}

}
