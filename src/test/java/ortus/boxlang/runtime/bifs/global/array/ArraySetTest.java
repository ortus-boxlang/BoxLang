
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

public class ArraySetTest {

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

	@DisplayName( "It should set the values in the array" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		              arr = [ "red", "green", "blue" ];
		              result = ArraySet( arr, 3, 5, 10 );
		    """,
		    context );
		Array arr = variables.getAsArray( Key.of( "arr" ) );
		assertThat( arr.size() ).isEqualTo( 5 );
		assertThat( arr.get( 0 ) ).isEqualTo( "red" );
		assertThat( arr.get( 1 ) ).isEqualTo( "green" );
		assertThat( arr.get( 2 ) ).isEqualTo( 10 );
		assertThat( arr.get( 3 ) ).isEqualTo( 10 );
		assertThat( arr.get( 4 ) ).isEqualTo( 10 );
	}

	@DisplayName( "It should allow you to invoke it as a member function" )
	@Test
	public void testMemberInvocation() {
		instance.executeSource(
		    """
		              arr = [ "red", "green", "blue" ];
		              result = arr.set( 3, 5, 10 );
		    """,
		    context );
		Array arr = variables.getAsArray( Key.of( "arr" ) );
		assertThat( arr.size() ).isEqualTo( 5 );
		assertThat( arr.get( 0 ) ).isEqualTo( "red" );
		assertThat( arr.get( 1 ) ).isEqualTo( "green" );
		assertThat( arr.get( 2 ) ).isEqualTo( 10 );
		assertThat( arr.get( 3 ) ).isEqualTo( 10 );
		assertThat( arr.get( 4 ) ).isEqualTo( 10 );
	}

	@DisplayName( "It should work on an empty array" )
	@Test
	public void testEmptyArray() {
		instance.executeSource(
		    """
		              arr = [];
		              result = ArraySet( arr, 3, 5, 10 );
		    """,
		    context );
		Array arr = variables.getAsArray( Key.of( "arr" ) );
		assertThat( arr.size() ).isEqualTo( 5 );
		assertThat( arr.get( 0 ) ).isNull();
		assertThat( arr.get( 1 ) ).isNull();
		assertThat( arr.get( 2 ) ).isEqualTo( 10 );
		assertThat( arr.get( 3 ) ).isEqualTo( 10 );
		assertThat( arr.get( 4 ) ).isEqualTo( 10 );
	}

}
