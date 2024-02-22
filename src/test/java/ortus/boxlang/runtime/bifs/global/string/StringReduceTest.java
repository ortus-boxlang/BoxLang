
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

package ortus.boxlang.runtime.bifs.global.string;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class StringReduceTest {

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

	@DisplayName( "It tests the BIF StringReduce with defaults" )
	@Test
	public void testBIF() {
		instance.executeSource(
		    """
		              nums = "12345";

		              function sumReduce( acc, num ){
		                  return acc + num;
		              };

		              result = StringReduce( nums, sumReduce, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "It should use the provided arrow function over the array" )
	@Test
	public void testArrowFunction() {
		instance.executeSource(
		    """
		              nums = "12345";

		              result = StringReduce( nums, ( acc, num ) => acc + num, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "Can reduce in to a complex object" )
	@Test
	public void testComplexAccumulator() {
		instance.executeSource(
		    """
		              nums = "12345";
		              result = StringReduce( nums, function( acc, num ){ acc.indexes.append( num ); return acc; }, { "indexes" : [] } );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( ArrayCaster.cast( variables.getAsStruct( result ).get( Key.of( "indexes" ) ) ).size(), 5 );
	}

	@DisplayName( "Tests StringReduce with Lambda" )
	@Test
	public void testArrowLambda() {
		instance.executeSource(
		    """
		              nums = "12345";

		              result = StringReduce( nums, ( acc, num ) -> acc + num, 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

}
