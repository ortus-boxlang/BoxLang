
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

public class StringEveryTest {

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

	@DisplayName( "It should run the UDF over every element as long as they return true" )
	@Test
	public void testUseProvidedUDF() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "12345";

		        function eachFn( value, i ){
		            indexes[ i ] = value;
		            return true;
		        };

		        result = StringEvery( nums, eachFn );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "3" );
		assertThat( indexes.get( 3 ) ).isEqualTo( "4" );
		assertThat( indexes.get( 4 ) ).isEqualTo( "5" );
	}

	@DisplayName( "It should return early when it his a false condition" )
	@Test
	public void testReturnEarlyOnFalse() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "12345";

		        function eachFn( value, i ){
		            if( value <= 3){
		                indexes[ i ] = value;
		                return true;
		            }

		            return false;
		        };

		        result = StringEvery( nums, eachFn );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( false );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 3 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "3" );
	}

}
