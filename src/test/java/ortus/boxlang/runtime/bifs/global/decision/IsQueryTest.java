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

public class IsQueryTest {

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

	@DisplayName( "It detects query values" )
	@Test
	public void testOnCreateObject() {
		assertThat( ( Boolean ) instance.executeStatement( "isQuery( new java:ortus.boxlang.runtime.types.Query() )" ) ).isTrue();
	}

	@DisplayName( "It detects query values" )
	@Test
	public void testOnQueryNew() {
		assertThat( ( Boolean ) instance.executeStatement( "isQuery( queryNew( 'id,name', 'numeric,varchar' ) )" ) ).isTrue();
	}

	@DisplayName( "It returns false for non-query values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    aBool = isQuery( true );
		    aStruct = isQuery( { columns: ["id", "name" ]} );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aBool" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aStruct" ) ) ).isFalse();
	}

}
