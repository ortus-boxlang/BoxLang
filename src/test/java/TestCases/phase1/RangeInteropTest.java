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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

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

public class RangeInteropTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= Key.of( "result" );

	@BeforeAll
	public static void setupRuntime() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "finite ranges participate in for-in iteration" )
	@Test
	void testRangeForInIteration() {
		instance.executeSource(
		    """
		    result = [];
		    for( item in 2..4 ) {
		        result.append( item );
		    }
		    """,
		    this.context
		);

		assertThat( this.variables.get( result ) ).isEqualTo( Array.of( 2, 3, 4 ) );
	}

	@SuppressWarnings( "unchecked" )
	@DisplayName( "finite ranges can initiate streams" )
	@Test
	void testRangeStreamMemberInvocation() {
		instance.executeSource(
		    """
		    result = ( 1..5 )
		        .stream()
		        .filter( value -> value >= 3 )
		        .toList();
		    """,
		    this.context
		);

		assertThat( ( List<Integer> ) this.variables.get( result ) ).containsExactly( 3, 4, 5 ).inOrder();
	}

	@SuppressWarnings( "unchecked" )
	@DisplayName( "finite ranges can initiate parallel streams" )
	@Test
	void testRangeParallelStreamMemberInvocation() {
		instance.executeSource(
		    """
		    result = ( 1..5 )
		        .parallelStream()
		        .filter( value -> value >= 3 )
		        .toList();
		    """,
		    this.context
		);

		assertThat( ( List<Integer> ) this.variables.get( result ) ).containsExactly( 3, 4, 5 );
	}

	@DisplayName( "finite ranges can materialize explicitly to a BoxLang array" )
	@Test
	void testRangeToBoxArray() {
		instance.executeSource(
		    """
		    result = ( 2..4 ).toBoxArray();
		    """,
		    this.context
		);

		assertThat( this.variables.get( result ) ).isEqualTo( Array.of( 2, 3, 4 ) );
	}
}