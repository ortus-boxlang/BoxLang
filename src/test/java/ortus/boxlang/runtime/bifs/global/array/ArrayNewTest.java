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
import ortus.boxlang.runtime.types.Struct;

public class ArrayNewTest {

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

	@DisplayName( "It can create new" )
	@Test
	public void testCanSearch() {

		instance.executeSource(
		    """
		    result = arrayNew();
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
	}

	@DisplayName( "It can create multi-dimensional arrays and access with negative indexes" )
	@Test
	public void testMultiDimensionalArray() {

		instance.executeSource(
		    """
		    arr = ArrayNew(2);
		    // Build first album array, Surfin' Safari
		    arr[1][1] = "Surfin' Safari";
		    arr[1][2] = "County Fair";
		    arr[1][3] = "Ten Little Indians";
		    // Build second album array, Surfin' USA
		    arr[2][1] = "Surfin' USA";
		    arr[2][2] = "Farmer's Daughter";
		    arr[2][3] = "Miserlou";
		    // Build third album array, Surfer Girl
		    arr[3][1] = "Surfer Girl";
		    arr[3][2] = "Catch a Wave";
		    arr[3][3] = "The Surfer Moon";

		    r1 = arr[-1][1];   // Surfer Girl
		    r2 = arr[-1][2];   // Catch a Wave
		    r3 = arr[-1][3];   // The Surfer Moon
		    r4 = arr[-1][-1];  // The Surfer Moon
		    r5 = arr[-1][-2];  // Catch a Wave
		    r6 = arr[-1][-3];  // Surfer Girl
		    r7 = arr[-3][1];   // Surfin' Safari
		    """,
		    context );
		assertThat( variables.get( Key.of( "r1" ) ) ).isEqualTo( "Surfer Girl" );
		assertThat( variables.get( Key.of( "r2" ) ) ).isEqualTo( "Catch a Wave" );
		assertThat( variables.get( Key.of( "r3" ) ) ).isEqualTo( "The Surfer Moon" );
		assertThat( variables.get( Key.of( "r4" ) ) ).isEqualTo( "The Surfer Moon" );
		assertThat( variables.get( Key.of( "r5" ) ) ).isEqualTo( "Catch a Wave" );
		assertThat( variables.get( Key.of( "r6" ) ) ).isEqualTo( "Surfer Girl" );
		assertThat( variables.get( Key.of( "r7" ) ) ).isEqualTo( "Surfin' Safari" );
	}

	@DisplayName( "It can create multi-dimensional arrays and access with positive indexes" )
	@Test
	public void testMultiDimensionalArrayPositiveIndexes() {

		instance.executeSource(
		    """
		    arr = ArrayNew(2);
		    // Build first album array, Surfin' Safari
		    arr[1][1] = "Surfin' Safari";
		    arr[1][2] = "County Fair";
		    arr[1][3] = "Ten Little Indians";
		    // Build second album array, Surfin' USA
		    arr[2][1] = "Surfin' USA";
		    arr[2][2] = "Farmer's Daughter";
		    arr[2][3] = "Miserlou";
		    // Build third album array, Surfer Girl
		    arr[3][1] = "Surfer Girl";
		    arr[3][2] = "Catch a Wave";
		    arr[3][3] = "The Surfer Moon";

		    r1 = arr[1][1];   // Surfin' Safari
		    r2 = arr[1][2];   // County Fair
		    r3 = arr[1][3];   // Ten Little Indians
		    r4 = arr[2][1];   // Surfin' USA
		    r5 = arr[2][2];   // Farmer's Daughter
		    r6 = arr[2][3];   // Miserlou
		    r7 = arr[3][1];   // Surfer Girl
		    r8 = arr[3][2];   // Catch a Wave
		    r9 = arr[3][3];   // The Surfer Moon
		    """,
		    context );
		assertThat( variables.get( Key.of( "r1" ) ) ).isEqualTo( "Surfin' Safari" );
		assertThat( variables.get( Key.of( "r2" ) ) ).isEqualTo( "County Fair" );
		assertThat( variables.get( Key.of( "r3" ) ) ).isEqualTo( "Ten Little Indians" );
		assertThat( variables.get( Key.of( "r4" ) ) ).isEqualTo( "Surfin' USA" );
		assertThat( variables.get( Key.of( "r5" ) ) ).isEqualTo( "Farmer's Daughter" );
		assertThat( variables.get( Key.of( "r6" ) ) ).isEqualTo( "Miserlou" );
		assertThat( variables.get( Key.of( "r7" ) ) ).isEqualTo( "Surfer Girl" );
		assertThat( variables.get( Key.of( "r8" ) ) ).isEqualTo( "Catch a Wave" );
		assertThat( variables.get( Key.of( "r9" ) ) ).isEqualTo( "The Surfer Moon" );
	}

	@DisplayName( "It can create three-dimensional arrays with positive indexes" )
	@Test
	public void testThreeDimensionalArray() {

		instance.executeSource(
		    """
		       arr = ArrayNew(3);
		       arr[1][1][1] = "a";
		       arr[1][1][2] = "b";
		       arr[1][2][1] = "c";
		       arr[2][1][1] = "d";
		       arr[2][1][2] = "e";
		    // Once I exceed the array dimensions, we fall back to normal struct-creation behavior
		       arr[2][2][1]["test"] = "f";

		       r1 = arr[1][1][1];
		       r2 = arr[1][1][2];
		       r3 = arr[1][2][1];
		       r4 = arr[2][1][1];
		       r5 = arr[2][1][2];
		       r6 = arr[2][2][1];
		     r7 = arr[2][2][1]["test"];
		       """,
		    context );
		assertThat( variables.get( Key.of( "r1" ) ) ).isEqualTo( "a" );
		assertThat( variables.get( Key.of( "r2" ) ) ).isEqualTo( "b" );
		assertThat( variables.get( Key.of( "r3" ) ) ).isEqualTo( "c" );
		assertThat( variables.get( Key.of( "r4" ) ) ).isEqualTo( "d" );
		assertThat( variables.get( Key.of( "r5" ) ) ).isEqualTo( "e" );
		assertThat( variables.get( Key.of( "r6" ) ) ).isInstanceOf( Struct.class );
		assertThat( variables.get( Key.of( "r7" ) ) ).isEqualTo( "f" );
	}

	@DisplayName( "Multi-dimensional arrays initialize sub arrays just by looking for the index" )
	@Test
	public void testMultiDimensionalArrayInitialization() {

		instance.executeSource(
		    """
		    arr = ArrayNew(2);
		    result = arr[1]
		    result2 = arr[2]
		    result3 = arr;
		         """,
		    context );
		assertThat( variables.get( Key.of( "result" ) ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( Key.of( "result" ) ) ).isEmpty();
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( Key.of( "result2" ) ) ).isEmpty();
		assertThat( variables.get( Key.of( "result3" ) ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( Key.of( "result3" ) ) ).hasSize( 2 );
	}

}
