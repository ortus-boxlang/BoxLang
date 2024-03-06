
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
import ortus.boxlang.runtime.types.IStruct;

public class ListReduceRightTest {

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

	@DisplayName( "It tests the BIF ListReduceRight with defaults" )
	@Test
	public void testBIF() {
		instance.executeSource(
		    """
		              nums = "1,2,3,4,5";

		              function sumReduce( acc, num, idx ){
		      if( idx == 1 ){
		    	return acc += num;
		      }
		      return acc;
		              };

		              result = ListReduceRight( nums, sumReduce, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 5 );
	}

	@DisplayName( "It tests the BIF ListReduceRight with an alternate delimiter" )
	@Test
	public void testBIFDelimiter() {
		instance.executeSource(
		    """
		              nums = "1|2|3|4|5";

		              function sumReduce( acc, num ){
		                  return acc + num;
		              };

		              result = ListReduceRight( nums, sumReduce, 0, "|" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "It should use the provided arrow function over the array" )
	@Test
	public void testArrowFunction() {
		instance.executeSource(
		    """
		              nums = "1,2,3,4,5";

		              result = ListReduceRight( nums, ( acc, num ) => acc + num, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "Tests ListReduceRight with Lambda" )
	@Test
	public void testArrowLambda() {
		instance.executeSource(
		    """
		              nums = "1,2,3,4,5";

		              result = ListReduceRight( nums, ( acc, num ) -> acc + num, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "Tests the member function String.reduceRight" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		              nums = "1,2,3,4,5";

		              result = nums.reduceRight( ( acc, num ) -> acc + num, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "It should handle complex objects being used as the accumulator" )
	@Test
	public void testComplexAccumulator() {
		instance.executeSource(
		    """
		        people = "bob,alice,alice,bob,bob";

		        function nameReduce( acc, name, i ){
		            arrayAppend( acc.indexes, i );
		            if( ! acc.containsKey( name ) ){
		                acc[ name ] = 0;
		            }
		            acc[ name ]++;
		            return acc;
		        };

		        result = ListReduceRight( people, nameReduce, {indexes:[]} );
		    """,
		    context );
		assertThat( ( variables.getAsStruct( result ) ).get( "bob" ) ).isEqualTo( 3 );
		assertThat( ( variables.getAsStruct( result ) ).get( "alice" ) ).isEqualTo( 2 );
		Array indexes = ( ( Array ) ( ( IStruct ) variables.get( result ) ).get( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

}
