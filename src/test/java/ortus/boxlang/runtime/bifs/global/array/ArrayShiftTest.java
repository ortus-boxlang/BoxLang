
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

import org.junit.Ignore;
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

public class ArrayShiftTest {

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

	@DisplayName( "It will change the size of the array" )
	@Test
	public void testChangeSize() {

		instance.executeSource(
		    """
		          a = [ "a", "b", "c", "d" ];
		          ArrayShift( a );
		       	result = a;
		    """,
		    context );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 3 );
	}

	@DisplayName( "It will remove the first element" )
	@Test
	public void testRemoveFirst() {

		instance.executeSource(
		    """
		          a = [ "a", "b", "c", "d" ];
		          result = ArrayShift( a );
		    """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "a" );
	}

	@DisplayName( "It will shift the elements to the left" )
	@Test
	public void testShiftLeft() {

		instance.executeSource(
		    """
		    a = [ "a", "b", "c", "d" ];
		          result = ArrayShift( a );
		    result = a;
		       """,
		    context );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "b" );
	}

	@DisplayName( "It tests the member function for ArrayShift" )
	@Test
	@Ignore
	public void testItReturnsFloorMember() {
		instance.executeSource(
		    """
		    a = [ "a", "b", "c", "d" ];
		          result = a.shift();
		    result = a;
		       """,
		    context );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "b" );
	}

}
