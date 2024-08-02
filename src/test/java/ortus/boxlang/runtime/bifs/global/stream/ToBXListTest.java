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

package ortus.boxlang.runtime.bifs.global.stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

public class ToBXListTest {

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

	@DisplayName( "It can collect a stream into a list" )
	@Test
	public void testCanCollect() {
		instance.executeSource(
		    """
		       foods = [ 'apples', 'bananas', 'pizza', true ];
		    result = foods.stream().toBXList();
		            """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertThat( variables.get( result ) ).isEqualTo( "apples,bananas,pizza,true" );
	}

	@DisplayName( "It can collect a stream into a list using delimiter" )
	@Test
	public void testCanCollectDelimiter() {
		instance.executeSource(
		    """
		                  foods = [ 'apples', 'bananas', 'pizza', true ];
		               result = foods.stream().toBXList( '|' );

		            result2 = [ "www", "google", "com" ].stream().toBXList( "." )

		         import java.util.stream.Stream;
		         evenNumbers = Stream.iterate(0, n -> n + 2);
		         result3 = evenNumbers.limit(10).toBXList()

		      // Fibonacci series
		      result4 = Stream.iterate([0, 1], f -> [f[2], f[1] + f[2] ])
		        .limit(20)
		        .map(f -> f[1])
		        .toBXList()


		     result5 = Stream.iterate([0, 1], f -> [f[2], f[1] + f[2] ])
		         .map(f -> f[1])
		         .dropWhile(n -> n < 1000)
		      	.findFirst()
		    .get()
		                       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertThat( variables.get( result ) ).isEqualTo( "apples|bananas|pizza|true" );

		assertTrue( variables.get( Key.of( "result2" ) ) instanceof String );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "www.google.com" );

		assertTrue( variables.get( Key.of( "result3" ) ) instanceof String );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "0,2,4,6,8,10,12,14,16,18" );

		assertTrue( variables.get( Key.of( "result4" ) ) instanceof String );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "0,1,1,2,3,5,8,13,21,34,55,89,144,233,377,610,987,1597,2584,4181" );

		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( 1597 );

	}

}
