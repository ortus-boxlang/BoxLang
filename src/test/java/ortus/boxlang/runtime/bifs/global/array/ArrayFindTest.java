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

public class ArrayFindTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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
	}
}
