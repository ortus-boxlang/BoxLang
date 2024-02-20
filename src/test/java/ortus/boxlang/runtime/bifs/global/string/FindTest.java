/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
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

public class FindTest {

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

	@DisplayName( "It finds the first occurrence of a substring in a string" )
	@Test
	public void testFindSubstring() {
		instance.executeSource(
		    """
		    result = find("Lang", "BoxLang is great");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // "Lang" starts at position 4
	}

	@DisplayName( "It returns zero for substring not found" )
	@Test
	public void testFindSubstringNotFound() {
		instance.executeSource(
		    """
		    result = find("ColdFusion", "BoxLang is great");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 ); // "ColdFusion" is not found
	}

	@DisplayName( "It finds the first occurrence from a specified start position" )
	@Test
	public void testFindSubstringWithStart() {
		instance.executeSource(
		    """
		    result = find("Lang", "BoxLang is great", 2 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // "Lang" starts at position 10 after the 6th position
	}

	@DisplayName( "It returns for negative start position" )
	@Test
	public void testFindNegativeStart() {
		instance.executeSource(
		    """
		    result = find("Lang", "BoxLang is great", -1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // Negative start position should be treated as 1
	}

	@DisplayName( "It returns for member function call" )
	@Test
	public void testFindMemberFunctionCall() {
		instance.executeSource(
		    """
		    result = "BoxLang is great".find("Lang");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // "Lang" starts at position 4
	}

	// Tests for findNoCase
	@DisplayName( "It finds the first occurrence of a substring in a string, ignoring case" )
	@Test
	public void testFindNoCaseSubstring() {
		instance.executeSource(
		    """
		    result = findNoCase("lang", "BoxLang is great");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // "Lang" starts at position 4
	}

	@DisplayName( "It returns zero for substring not found, ignoring case" )
	@Test
	public void testFindNoCaseSubstringNotFound() {
		instance.executeSource(
		    """
		    result = findNoCase("coldfusion", "BoxLang is great");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 ); // "ColdFusion" is not found
	}

	@DisplayName( "It finds the first occurrence from a specified start position, ignoring case" )
	@Test
	public void testFindNoCaseSubstringWithStart() {
		instance.executeSource(
		    """
		    result = findNoCase("lang", "BoxLang is great", 2 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // "Lang" starts at position 10 after the 6th position
	}

	@DisplayName( "It returns for negative start position, ignoring case" )
	@Test
	public void testFindNoCaseNegativeStart() {
		instance.executeSource(
		    """
		    result = findNoCase("lang", "BoxLang is great", -1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // Negative start position should be treated as 1
	}

	@DisplayName( "It returns for member function call, ignoring case" )
	@Test
	public void testFindNoCaseMemberFunctionCall() {
		instance.executeSource(
		    """
		    string = "BoxLang is great";
		      	result = string.findNoCase("lang");
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 ); // "Lang" starts at position 4
	}
}
