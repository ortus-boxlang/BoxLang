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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class IsNumericTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It detects numeric values" )
	@Test
	public void testNumerics() {
		instance.executeSource(
		    """
		    int         = isnumeric( 123 );
		    stringInt   = isnumeric( "123" );
		    stringFloat = isnumeric( "123.4" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringInt" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringFloat" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-numeric values, including non-string values like a struct" )
	@Test
	public void testSimpleNumerics() {
		instance.executeSource(
		    """
		    int         = isnumeric( "abc83" );
		    hexadecimal = isnumeric( "3FA5" );
		    badFloat    = isnumeric( "123.x" );
		    struct      = isnumeric( {} );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "hexadecimal" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "badFloat" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "struct" ) ) ).isFalse();
	}
}
