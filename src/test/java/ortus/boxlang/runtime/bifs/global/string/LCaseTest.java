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

package ortus.boxlang.runtime.bifs.global.string;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class LCaseTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It uppercases as BIF" )
	@Test
	public void testItUppercasesBIF() {
		instance.executeSource(
		    """
		    result = lcase( 'BRAD' );
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "brad" );
	}

	@DisplayName( "It uppercases as BIF and casting" )
	@Test
	public void testItUppercasesBIFCasting() {
		instance.executeSource(
		    """
		    result = lcase( 5 );
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "5" );
	}

	@DisplayName( "It uppercases as member" )
	@Test
	public void testItUppercasesMember() {
		instance.executeSource(
		    """
		    value = 'BRAD';
		       result = value.lcase();
		    result2 = "LUIS".lcase();
		       """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "brad" );
		assertThat( variables.dereference( Key.of( "result2" ), false ) ).isEqualTo( "luis" );
	}

	@DisplayName( "It uppercases as member and casting" )
	@Test
	public void testItUppercasesMemberCasting() {
		instance.executeSource(
		    """
		    value = 5;
		       result = value.lcase();
		    result2 = (6).lcase();
		       """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "5" );
		assertThat( variables.dereference( Key.of( "result2" ), false ) ).isEqualTo( "6" );
	}

}
