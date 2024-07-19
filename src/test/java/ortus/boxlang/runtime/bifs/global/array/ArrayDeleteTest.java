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

public class ArrayDeleteTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			arr		= new Key( "arr" );

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

	@DisplayName( "It can delete" )
	@Test
	public void testCanDelete() {

		instance.executeSource(
		    """
		    arr = [ 'a', 'b', 'c' ];
		    result = arraydelete( arr, 'b' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Array.of( "a", "c" ) );
		assertThat( variables.getAsArray( arr ) ).hasSize( 2 );

		instance.executeSource(
		    """
		    arr = [ 'a', 'b', 'c' ];
		    result = arraydelete( arr, 'B' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Array.of( "a", "b", "c" ) );
		assertThat( variables.getAsArray( arr ) ).hasSize( 3 );
	}

	@DisplayName( "It can delete with scope arguments" )
	@Test
	public void testCanDeleteScope() {

		instance.executeSource(
		    """
		    arr = [ 'a', 'b', 'c', 'b' ];
		    result = arraydelete( arr, 'b', 'all' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Array.of( "a", "c" ) );
		assertThat( variables.getAsArray( arr ) ).hasSize( 2 );

		instance.executeSource(
		    """
		    arr = [ 'a', 'b', 'c', 'b' ];
		    result = arraydelete( arr, 'b', 'one' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Array.of( "a", "c", "b" ) );
		assertThat( variables.getAsArray( arr ) ).hasSize( 3 );
	}

	@DisplayName( "It can delete member" )
	@Test
	public void testCanDeleteMember() {

		instance.executeSource(
		    """
		    arr = [ 'a', 'b', 'c' ];
		    result = arr.Delete( 'b' );
		    """,
		    context );
		assertThat( variables.getAsArray( result ) ).hasSize( 2 );
		assertThat( variables.getAsArray( arr ) ).hasSize( 2 );

		instance.executeSource(
		    """
		    arr = [ 'a', 'b', 'c' ];
		    result = arr.Delete( 'B' );
		    """,
		    context );
		assertThat( variables.getAsArray( result ) ).hasSize( 3 );
		assertThat( variables.getAsArray( arr ) ).hasSize( 3 );
	}
}
