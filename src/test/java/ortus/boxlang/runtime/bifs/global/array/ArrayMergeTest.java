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

public class ArrayMergeTest {

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

	@DisplayName( "It can merge two arrays" )
	@Test
	public void testMerge() {

		instance.executeSource(
		    """
		    a = [ "red", "yellow", "orange" ];
		    b = [ "green", "blue", "purple" ];
		    result = arrayMerge( a, b );
		    """,
		    context );
		Array res = ( Array ) variables.get( result );
		assertThat( res.size() ).isEqualTo( 6 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "yellow" );
		assertThat( res.get( 2 ) ).isEqualTo( "orange" );
		assertThat( res.get( 3 ) ).isEqualTo( "green" );
		assertThat( res.get( 4 ) ).isEqualTo( "blue" );
		assertThat( res.get( 5 ) ).isEqualTo( "purple" );
	}

	@DisplayName( "It should not mutate the inputs" )
	@Test
	public void testDoesNotMutate() {

		instance.executeSource(
		    """
		    a = [ "red", "yellow", "orange" ];
		    b = [ "green", "blue", "purple" ];
		    result = arrayMerge( a, b );
		    """,
		    context );
		Array	res	= ( Array ) variables.get( result );
		Array	a	= ( Array ) variables.get( "a" );
		Array	b	= ( Array ) variables.get( "b" );
		assertThat( res.size() ).isEqualTo( 6 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "yellow" );
		assertThat( res.get( 2 ) ).isEqualTo( "orange" );
		assertThat( res.get( 3 ) ).isEqualTo( "green" );
		assertThat( res.get( 4 ) ).isEqualTo( "blue" );
		assertThat( res.get( 5 ) ).isEqualTo( "purple" );
		assertThat( a.size() ).isEqualTo( 3 );
		assertThat( a.get( 0 ) ).isEqualTo( "red" );
		assertThat( a.get( 1 ) ).isEqualTo( "yellow" );
		assertThat( a.get( 2 ) ).isEqualTo( "orange" );
		assertThat( b.size() ).isEqualTo( 3 );
		assertThat( b.get( 0 ) ).isEqualTo( "green" );
		assertThat( b.get( 1 ) ).isEqualTo( "blue" );
		assertThat( b.get( 2 ) ).isEqualTo( "purple" );
	}

	@DisplayName( "It should preserve indexes when the leaveIndex flag is set" )
	@Test
	public void testLeaveIndex() {

		instance.executeSource(
		    """
		    a = [ "red", "yellow", "orange" ];
		    b = [ "green", "blue", "purple" ];
		    result = arrayMerge( a, b, true );
		    """,
		    context );
		Array res = ( Array ) variables.get( result );
		assertThat( res.size() ).isEqualTo( 3 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "yellow" );
		assertThat( res.get( 2 ) ).isEqualTo( "orange" );
	}

	@DisplayName( "It should preserve indexes when the leaveIndex flag is set and the second array is longer" )
	@Test
	public void testLongArray() {

		instance.executeSource(
		    """
		    a = [ "red", "yellow", "orange" ];
		    b = [ "green", "blue", "purple", "fuschia" ];
		    result = arrayMerge( a, b, true );
		    """,
		    context );
		Array res = ( Array ) variables.get( result );
		assertThat( res.size() ).isEqualTo( 4 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "yellow" );
		assertThat( res.get( 2 ) ).isEqualTo( "orange" );
		assertThat( res.get( 3 ) ).isEqualTo( "fuschia" );
	}

	@DisplayName( "It should allow member invocation" )
	@Test
	public void testMemberInvocation() {

		instance.executeSource(
		    """
		    a = [ "red", "yellow", "orange" ];
		    b = [ "green", "blue", "purple" ];
		    result = a.merge( b );
		    """,
		    context );
		Array res = ( Array ) variables.get( result );
		assertThat( res.size() ).isEqualTo( 6 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "yellow" );
		assertThat( res.get( 2 ) ).isEqualTo( "orange" );
		assertThat( res.get( 3 ) ).isEqualTo( "green" );
		assertThat( res.get( 4 ) ).isEqualTo( "blue" );
		assertThat( res.get( 5 ) ).isEqualTo( "purple" );
	}
}
