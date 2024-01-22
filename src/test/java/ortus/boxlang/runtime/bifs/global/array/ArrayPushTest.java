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

package ortus.boxlang.runtime.bifs.global.array;

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
import ortus.boxlang.runtime.types.Array;

public class ArrayPushTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "It can push a new element onto the end of the array" )
	@Test
	public void testCanSearch() {

		instance.executeSource(
		    """
		    a = [ "existing" ];
		    result = ArrayPush( a, 'test' );
		       """,
		    context );
		assertThat( ( ( Array ) variables.get( "a" ) ).size() ).isEqualTo( 2 );
		assertThat( ( ( Array ) variables.get( "a" ) ).get( 1 ) ).isEqualTo( "test" );
	}

	@DisplayName( "It returns the new size of the array" )
	@Test
	public void testReturnsSize() {

		instance.executeSource(
		    """
		    a = [ "existing" ];
		    result = ArrayPush( a, 'test' );
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "It can be invoked as a member function" )
	@Test
	public void testMemberInvocation() {

		instance.executeSource(
		    """
		    a = [ "existing" ];
		    result = a.push( 'test' );
		       """,
		    context );
		assertThat( ( ( Array ) variables.get( "a" ) ).size() ).isEqualTo( 2 );
		assertThat( ( ( Array ) variables.get( "a" ) ).get( 1 ) ).isEqualTo( "test" );
	}

}
