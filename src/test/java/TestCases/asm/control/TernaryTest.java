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
package TestCases.asm.control;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.*;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

@Disabled
public class TernaryTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

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
		instance.useASMBoxPiler();
	}

	@AfterEach
	public void teardownEach() {
		instance.useJavaBoxpiler();
	}

	@DisplayName( "Will return the true option of a ternary when condition is true" )
	@Test
	public void testTrueResult() {
		var result = instance.executeStatement(
		    """
		    true ? "red": "green";
		        """,
		    context );

		assertThat( result ).isEqualTo( "red" );
	}

	@DisplayName( "Will return the false option of a ternary when condition is false" )
	@Test
	public void testFalseResult() {
		var result = instance.executeStatement(
		    """
		    false ? "red": "green";
		        """,
		    context );

		assertThat( result ).isEqualTo( "green" );
	}

	@DisplayName( "Can assign from a ternary" )
	@Test
	public void testAssignFromTernary() {
		instance.executeStatement(
		    """
		    result = true ? 1 : 2;
		        """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}
}
