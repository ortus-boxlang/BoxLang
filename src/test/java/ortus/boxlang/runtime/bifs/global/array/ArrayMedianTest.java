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

public class ArrayMedianTest {

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

	@DisplayName( "It can get the median" )
	@Test
	public void testMedian() {

		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = arrayMedian(arr);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "It should get the median of an odd length array" )
	@Test
	public void testOddLength() {

		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = arrayMedian(arr);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "It should get the median of an even length array" )
	@Test
	public void testEvenLength() {

		instance.executeSource(
		    """
		    arr = [ 1, 2, 3, 4 ];
		    result = arrayMedian(arr);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2.5 );
	}

	@DisplayName( "It should allow member invocation" )
	@Test
	public void testMemberInvocation() {

		instance.executeSource(
		    """
		    arr = [ 1, 2, 3, 4 ];
		    result = arr.median();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2.5 );
	}

	@DisplayName( "It should handle unsorted arrays" )
	@Test
	public void testUnsorted() {

		instance.executeSource(
		    """
		    arr = [ 1, 4, 3, 2 ];
		    result = arr.median();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2.5 );
	}

	@DisplayName( "It should throw an error for non-numeric values" )
	@Test
	public void testThrows() {

		assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    arr = [ 1, 2, 3, 4, "orange" ];
			    result = arrayMedian(arr);
			    """,
			    context );
		} );
	}

}
