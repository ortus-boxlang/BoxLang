
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

package ortus.boxlang.runtime.bifs.global.list;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ListFindTest {

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

	@DisplayName( "Can find a number in a string list" )
	@Test
	public void testMatchNumber() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = listFind( nums, 3 );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 3 );
	}

	@DisplayName( "It can find a string" )
	@Test
	public void testMatch() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = listFind( nums, "3" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 3 );
	}

	@DisplayName( "Tests the member String.listFind" )
	@Test
	public void testMember() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = nums.listFind( "3" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 3 );
	}

	@DisplayName( "Tests when not found" )
	@Test
	public void testNotFound() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = listFind( nums, "foo" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 0 );
	}

	@DisplayName( "It should find strings in a case sensitive manner" )
	@Test
	public void testMatchStringCaseSensitive() {
		instance.executeSource(
		    """
		        nums = "red,blue,orange";
		        result = listFindNoCase( nums, "bluE" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );
	}

	@DisplayName( "Test String.listFindNoCase" )
	@Test
	public void testMemberMatchStringCaseSensitive() {
		instance.executeSource(
		    """
		        nums = "red,blue,orange";
		        result = nums.listFind( "bluE" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 0 );
	}

	@DisplayName( "Test listContains" )
	@Test
	public void testMatchSubStringCaseSensitive() {
		instance.executeSource(
		    """
		    	result = listContains("sheep,goat,foo,bar,goo", "oo");
		    """,
		    context );
		Boolean found = ( Boolean ) variables.get( result );
		assertTrue( found );
	}

	@DisplayName( "Test listFindNoCase" )
	@Test
	public void testMatchSubStringCaseInSensitive() {
		instance.executeSource(
		    """
		    	result = listContainsNoCase("sheep,goat,foo,bar,goo", "oO");
		    """,
		    context );
		Boolean found = ( Boolean ) variables.get( result );
		assertTrue( found );
	}

	@DisplayName( "It should find strings in a case insensitive manner when using nocase" )
	@Test
	public void testMatchStringCaseInSensitive() {
		instance.executeSource(
		    """
		        nums = "red,blue,orange";
		        result = nums.listFindNoCase( "bluE" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );
	}

	@Disabled( "Performance benchmark test" )
	@Test
	public void benchmark() {
		instance.executeSource(
		    """
		                nums = "red,blue,orange";
		          system = createObject( "java", "java.lang.System" );
		       start = system.currentTimeMillis();
		       for( i=1; i <= 100000; i++ ){
		       	result = nums.listFindNoCase( "bluE" );
		       }
		       end = system.currentTimeMillis();
		    totalTime = ( end - start );
		            """,
		    context );
	}

	@DisplayName( "It should not find partial" )
	@Test
	public void testPartial() {
		instance.executeSource(
		    """
		        nums = "red,blue,orange";
		        result = nums.listFindNoCase( "b" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 0 );
	}

}
