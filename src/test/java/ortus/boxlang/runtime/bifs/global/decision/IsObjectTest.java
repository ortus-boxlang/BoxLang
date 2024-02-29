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
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class IsObjectTest {

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

	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    result1 = isObject( new src.test.java.TestCases.phase3.Chihuahua() );
		    result2 = isObject( new java.util.ArrayList() );
		      """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "result1" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "result2" ) ) ).isTrue();
	}

	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    result1 = isObject( "test" );
		    result2 = isObject( 53 );
		    result3 = isObject( {} );
		    result4 = isObject( [] );
		    result5 = isObject( queryNew( "" ) );
		    result6 = isObject( now() );
		    result7 = isObject( true );
		    result8 = isObject( 45.67 );
		        """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "result1" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result2" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result3" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result4" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result5" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result6" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result7" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result8" ) ) ).isFalse();
	}

}
