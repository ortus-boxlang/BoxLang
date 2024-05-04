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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class RightTest {

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

	@DisplayName( "It extracts rightmost characters from a string" )
	@Test
	public void testItExtractsRightmostCharacters() {
		instance.executeSource(
		    """
		    result = right("abcdef", 3);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "def" );
	}

	@DisplayName( "It extracts rightmost characters as member" )
	@Test
	public void testItExtractsRightmostCharactersMember() {
		instance.executeSource(
		    """
		    value = "abcdef";
		    result = value.right(3);
		    result2 = "xyz".right(2);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "def" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "yz" );
	}

	@DisplayName( "It throws exception for zero count" )
	@Test
	public void testItThrowsExceptionForZeroCount() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        value = "abcdef";
		              result = value.right(0);
		              """,
		        context )
		);
	}

	@DisplayName( "It allows negative count" )
	@Test
	public void testItAllowsNegativeCount() {
		instance.executeSource(
		    """
		    value = "abcdef";
		    result = value.right(-2);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "cdef" );
	}
}
