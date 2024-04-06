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

public class SpanIncludingTest {

	static BoxRuntime	runtime;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It extracts the set from the input string from left to right" )
	@Test
	public void testItExtractsSetFromLeftToRight() {
		runtime.executeSource(
		    """
		    value = "Highway Star";
		    result = spanIncluding( value, "High" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "High" );
	}

	@DisplayName( "It works as a member function" )
	@Test
	public void testItExtractsLeftmostCharactersMember() {
		runtime.executeSource(
		    """
		    value = "mystring";
		    result = value.spanIncluding( "mystery" );
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "mystr" );
	}

	@DisplayName( "It ignores the set if not found in the string" )
	@Test
	public void testItIgnoresSetIfNotFound() {
		runtime.executeSource(
		    """
		    value = "abcdef";
		    result = spanIncluding( value, "xyz" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "" );
	}

	@DisplayName( "It throws exception for empty string" )
	@Test
	public void testItThrowsExceptionForEmptyString() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource(
		        """
		            value = "";
		        	result = spanIncluding( value, "cde" );
		        """,
		        context )
		);
	}

}
