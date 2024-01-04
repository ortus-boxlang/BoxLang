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

public class CompareNoCaseTest {

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

	@DisplayName( "It returns 0 if the two strings are equal" )
	@Test
	public void testCompareEqual() {
		instance.executeSource(
		    """
		    result = compareNoCase( 'a', 'a' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "It performs a case insensitive compare" )
	@Test
	public void testCompareCaseSensitive() {
		instance.executeSource(
		    """
		    result = compareNoCase( 'a', 'A' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "It returns -1 if the first string precedes the second string lexicographically" )
	@Test
	public void testCompareFirst() {
		instance.executeSource(
		    """
		    result = compareNoCase( 'a', 'C' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( -1 );
	}

	@DisplayName( "It returns 1 if the second string precedes the first string lexicographically" )
	@Test
	public void testCompareSecond() {
		instance.executeSource(
		    """
		    result = compareNoCase( 'B', 'a' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It comparse as a member function" )
	@Test
	public void testCompareMember() {
		instance.executeSource(
		    """
		    value = 'a';
		    result = value.compareNoCase( "b" );
		    result2 = "b".compareNoCase( "a" );
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( -1 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 1 );
	}

}
