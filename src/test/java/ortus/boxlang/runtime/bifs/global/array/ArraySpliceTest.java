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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class ArraySpliceTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IStruct		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= ( IStruct ) context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It should remove the specified elements" )
	@Test
	public void testRemove() {
		instance.executeSource(
		    """
		          colors = [ "red", "green", "blue" ];
		    ArraySplice( colors, 2, 1 );
		    result = colors;
		      """,
		    context );
		Array res = variables.getAsArray( result );
		assertThat( res.size() ).isEqualTo( 2 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "blue" );
	}

	@DisplayName( "It should handle negative indexes" )
	@Test
	public void testNegative() {
		instance.executeSource(
		    """
		          colors = [ "red", "green", "blue" ];
		    ArraySplice( colors, -1, 1 );
		    result = colors;
		      """,
		    context );
		Array res = variables.getAsArray( result );
		assertThat( res.size() ).isEqualTo( 2 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "green" );
	}

	@DisplayName( "It should handle negative indexes greater than the size" )
	@Test
	public void testNegativeGreaterThanSize() {
		instance.executeSource(
		    """
		          colors = [ "red", "green", "blue" ];
		    ArraySplice( colors, -4, 1 );
		    result = colors;
		      """,
		    context );
		Array res = variables.getAsArray( result );
		assertThat( res.size() ).isEqualTo( 2 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "green" );
	}

	@DisplayName( "It should insert the specified elements" )
	@Test
	public void testInsert() {
		instance.executeSource(
		    """
		          colors = [ "red", "green", "blue" ];
		    ArraySplice( colors, 2, 1, [ "orange" ] );
		    result = colors;
		      """,
		    context );
		Array res = variables.getAsArray( result );
		assertThat( res.size() ).isEqualTo( 3 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "orange" );
		assertThat( res.get( 2 ) ).isEqualTo( "blue" );
	}

	@DisplayName( "It should allow member invocation" )
	@Test
	public void testMemberInvocation() {
		instance.executeSource(
		    """
		          colors = [ "red", "green", "blue" ];
		    colors.splice( 2, 1, [ "orange" ] );
		    result = colors;
		      """,
		    context );
		Array res = variables.getAsArray( result );
		assertThat( res.size() ).isEqualTo( 3 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
		assertThat( res.get( 1 ) ).isEqualTo( "orange" );
		assertThat( res.get( 2 ) ).isEqualTo( "blue" );
	}

}
