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
import ortus.boxlang.runtime.types.Array;

public class ToBXArrayTest {

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

	@DisplayName( "It can collect a stream into an array" )
	@Test
	public void testCanCollect() {
		instance.executeSource(
		    """
		       foods = [ 'apples', 'bananas', 'pizza', 'tacos' ];
		    result = foods.stream().toBXArray();
		            """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertThat( ( variables.getAsArray( result ) ).size() ).isEqualTo( 4 );
		assertThat( ( variables.getAsArray( result ) ).get( 0 ) ).isEqualTo( "apples" );
		assertThat( ( variables.getAsArray( result ) ).get( 1 ) ).isEqualTo( "bananas" );
		assertThat( ( variables.getAsArray( result ) ).get( 2 ) ).isEqualTo( "pizza" );
		assertThat( ( variables.getAsArray( result ) ).get( 3 ) ).isEqualTo( "tacos" );
	}

	@DisplayName( "It can collect a parallel stream into an array" )
	@Test
	public void testCanCollectParallel() {
		instance.executeSource(
		    """
		       foods = [ 'apples', 'bananas', 'pizza', 'tacos' ];
		    result = foods.stream().parallel().toBXArray();
		            """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertThat( ( variables.getAsArray( result ) ).size() ).isEqualTo( 4 );
		assertThat( ( variables.getAsArray( result ) ).get( 0 ) ).isEqualTo( "apples" );
		assertThat( ( variables.getAsArray( result ) ).get( 1 ) ).isEqualTo( "bananas" );
		assertThat( ( variables.getAsArray( result ) ).get( 2 ) ).isEqualTo( "pizza" );
		assertThat( ( variables.getAsArray( result ) ).get( 3 ) ).isEqualTo( "tacos" );
	}

	@DisplayName( "It can collect an int stream into an array" )
	@Test
	public void testCanCollectInt() {
		instance.executeSource(
		    """
		      import java.util.stream.IntStream;
		    result = IntStream.range(1, 6).toBXArray();
		                """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertThat( ( variables.getAsArray( result ) ).size() ).isEqualTo( 5 );
		assertThat( ( variables.getAsArray( result ) ).get( 0 ) ).isEqualTo( 1 );
		assertThat( ( variables.getAsArray( result ) ).get( 1 ) ).isEqualTo( 2 );
		assertThat( ( variables.getAsArray( result ) ).get( 2 ) ).isEqualTo( 3 );
		assertThat( ( variables.getAsArray( result ) ).get( 3 ) ).isEqualTo( 4 );
		assertThat( ( variables.getAsArray( result ) ).get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It can collect an long stream into an array" )
	@Test
	public void testCanCollectLong() {
		instance.executeSource(
		    """
		      import java.util.stream.LongStream;
		    result = LongStream.range(1, 6).toBXArray();
		                """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertThat( ( variables.getAsArray( result ) ).size() ).isEqualTo( 5 );
		assertThat( ( variables.getAsArray( result ) ).get( 0 ) ).isEqualTo( 1 );
		assertThat( ( variables.getAsArray( result ) ).get( 1 ) ).isEqualTo( 2 );
		assertThat( ( variables.getAsArray( result ) ).get( 2 ) ).isEqualTo( 3 );
		assertThat( ( variables.getAsArray( result ) ).get( 3 ) ).isEqualTo( 4 );
		assertThat( ( variables.getAsArray( result ) ).get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It can collect an double stream into an array" )
	@Test
	public void testCanCollectDouble() {
		instance.executeSource(
		    """
		      import java.util.stream.DoubleStream;
		    result = DoubleStream.of(100).toBXArray();
		                """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertThat( ( variables.getAsArray( result ) ).size() ).isEqualTo( 1 );
		assertThat( ( variables.getAsArray( result ) ).get( 0 ) ).isEqualTo( 100 );
	}

}
