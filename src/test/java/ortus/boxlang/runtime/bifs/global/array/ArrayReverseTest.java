
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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class ArrayReverseTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IStruct		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "It should reverse the array" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		              arr = [ "red", "green", "blue" ];
		              result = ArrayReverse( arr );
		    """,
		    context );
		Array reversed = ( Array ) variables.getAsArray( result );
		assertThat( reversed.size() ).isEqualTo( 3 );
		assertThat( reversed.get( 0 ) ).isEqualTo( "blue" );
		assertThat( reversed.get( 1 ) ).isEqualTo( "green" );
		assertThat( reversed.get( 2 ) ).isEqualTo( "red" );
	}

	@DisplayName( "It should not mutate the original array" )
	@Test
	public void testDoesntMutate() {
		instance.executeSource(
		    """
		              arr = [ "red", "green", "blue" ];
		              result = ArrayReverse( arr );
		    """,
		    context );
		Array arr = ( Array ) variables.getAsArray( Key.of( "arr" ) );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( "red" );
		assertThat( arr.get( 1 ) ).isEqualTo( "green" );
		assertThat( arr.get( 2 ) ).isEqualTo( "blue" );
	}

	@DisplayName( "It tests the member function for ArrayReverse" )
	@Test
	public void testItReturnsFloorMember() {
		instance.executeSource(
		    """
		              arr = [ "red", "green", "blue" ];
		              result = ArrayReverse( arr );
		    """,
		    context );
		Array reversed = ( Array ) variables.getAsArray( result );
		assertThat( reversed.size() ).isEqualTo( 3 );
		assertThat( reversed.get( 0 ) ).isEqualTo( "blue" );
		assertThat( reversed.get( 1 ) ).isEqualTo( "green" );
		assertThat( reversed.get( 2 ) ).isEqualTo( "red" );
	}

}
