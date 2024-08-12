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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class ArrayClearTest {

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
	}

	@DisplayName( "It can clear the array" )
	@Test
	public void testCanClear() {
		instance.executeSource(
		    """
		              nums = [ 1, 2, 3, 4, 5 ];

		              result = arrayClear( nums );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( ( ( Array ) variables.get( Key.of( "nums" ) ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "It can clear the array member" )
	@Test
	public void testCanClearMember() {
		instance.executeSource(
		    """
		              nums = [ 1, 2, 3, 4, 5 ];

		              result = nums.clear();
		    """,
		    context );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 0 );
		assertThat( ( variables.getAsArray( Key.of( "nums" ) ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "It can clear native array" )
	@Test
	public void testCanClearNative() {
		assertThrows( BoxCastException.class, () -> instance.executeSource(
		    """
		              nums = "brad".getBytes();

		              result = arrayClear( nums );
		    """,
		    context ) );
	}

}
