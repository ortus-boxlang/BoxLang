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

import java.util.function.Predicate;

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

public class ArrayFindTest {

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

	@DisplayName( "It should match numbers" )
	@Test
	public void testMatchNumber() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];
		        result = nums.find( 3 );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 3 );
	}

	@DisplayName( "It should match doubles" )
	@Test
	public void testMatchDoubles() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];
		        result = nums.find( 3.0 );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 3 );
	}

	@DisplayName( "It should match numbers and strings" )
	@Test
	public void testMatchNumberAndString() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 4, 5, "3" ];
		        result = nums.find( 3 );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 5 );
	}

	@DisplayName( "It should find strings in a case sensitive manner" )
	@Test
	public void testMatchStringCaseSensitive() {
		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.find( "bluE" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 0 );
	}

	@DisplayName( "It should find strings in a case insensitive manner when using nocase" )
	@Test
	public void testMatchStringCaseInSensitive() {
		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.findNoCase( "bluE" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );

		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.findNoCase( "tests" );
		    """,
		    context );
		found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 0 );
	}

	@DisplayName( "It should find strings using a closure to match" )
	@Test
	public void testMatchStringClosure() {
		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.find( item => item == "blue" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );
	}

	@DisplayName( "It should find strings using a lambda to match" )
	@Test
	public void testMatchStringLambda() {
		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.find( item -> item == "blue" );
		    """,
		    context );
		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );
	}

	@DisplayName( "It should find using a closure returning a boolean" )
	@Test
	public void testMatchClosure() {
		instance.executeSource(
		    """
		    result = ["a","b","c"].find( (v) => v == "b" ? 1 : 0 );
		    """,
		    context );

		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );
	}

	@DisplayName( "Function can be Java functional interface" )
	@Test
	@Disabled( "See comments on https://ortussolutions.atlassian.net/browse/BL-617" )
	public void testJavaFunctionalInterface() {
		Predicate<String> javaPredicate = ( s ) -> s.equals( "b" );
		variables.put( "javaPredicate", javaPredicate );
		instance.executeSource(
		    """
		    import java.util.function.Predicate;
		       result = ["a","b","c"].find( javaPredicate );
		       """,
		    context );

		int found = ( int ) variables.get( result );
		assertThat( found ).isEqualTo( 2 );
	}
}
