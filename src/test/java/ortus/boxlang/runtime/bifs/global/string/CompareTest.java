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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

public class CompareTest {

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

	@DisplayName( "It returns 0 if the two strings are equal" )
	@Test
	public void testCompareEqual() {
		instance.executeSource(
		    """
		    result = compare( 'a', 'a' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "It performs a case sensitive compare" )
	@Test
	public void testCompareCaseSensitive() {
		instance.executeSource(
		    """
		    result = compare( 'a', 'A' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It returns -1 if the first string precedes the second string lexicographically" )
	@Test
	public void testCompareFirst() {
		instance.executeSource(
		    """
		    result = compare( 'a', 'c' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( -1 );
	}

	@DisplayName( "It returns 1 if the second string precedes the first string lexicographically" )
	@Test
	public void testCompareSecond() {
		instance.executeSource(
		    """
		    result = compare( 'b', 'a' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It compares as a member function" )
	@Test
	public void testCompareMember() {
		instance.executeSource(
		    """
		    value = 'a';
		    result = value.compare( "b" );
		    result2 = "b".compare( "a" );
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( -1 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "Tests that the compare function will sort an array of numbers - quoted or unquoted correctly" )
	@Test
	public void testCompareNumbers() {
		instance.executeSource(
		    """
		    result = ["10", "5", "20", "15", "30"];
		    arraySort(
		    	result,
		    	(x, y) => compare(x,y)
		    );
		         """,
		    context );

		assertThat( variables.get( result ) instanceof Array );
		System.out.println( variables.getAsArray( result ) );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "10" );
		assertThat( variables.getAsArray( result ).get( 1 ) ).isEqualTo( "15" );
		assertThat( variables.getAsArray( result ).get( 2 ) ).isEqualTo( "20" );
		assertThat( variables.getAsArray( result ).get( 3 ) ).isEqualTo( "30" );
		assertThat( variables.getAsArray( result ).get( 4 ) ).isEqualTo( "5" );

		instance.executeSource(
		    """
		    result = [10, 5, 20, 15, 30];
		    arraySort(
		    	result,
		    	(x, y) => compare(x,y)
		    );
		         """,
		    context );

		assertThat( variables.get( result ) instanceof Array );
		System.out.println( variables.getAsArray( result ) );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( 10 );
		assertThat( variables.getAsArray( result ).get( 1 ) ).isEqualTo( 15 );
		assertThat( variables.getAsArray( result ).get( 2 ) ).isEqualTo( 20 );
		assertThat( variables.getAsArray( result ).get( 3 ) ).isEqualTo( 30 );
		assertThat( variables.getAsArray( result ).get( 4 ) ).isEqualTo( 5 );
	}

}
