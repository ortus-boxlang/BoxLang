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

package ortus.boxlang.runtime.bifs.global.math;

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

public class RandRangeTest {

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

	@DisplayName( "It returns a random number in range" )
	@Test
	public void testItReturnsARandomNumberInRange() {
		instance.executeSource(
		    """
		    bx:loop times=1000 {
		       	result = randRange( 0, 12 );
		    	assert result >= 0;
		    	assert result <= 12;
		    }
		       """, context );

		instance.executeSource(
		    """
		    bx:loop times=1000 {
		       	result = randRange( -12, 0 );
		       	assert result >= -12;
		       	assert result <= 0;
		       }
		       		 """, context );

		instance.executeSource(
		    """
		    bx:loop times=1000 {
		     result = randRange( 3.5, 4.9 );
		     assert result >= 3;
		     assert result <= 4;

		    }
		       """, context );

		instance.executeSource(
		    """
		    bx:loop times=1000 {
		        result = randRange( 100000000000000000000000, 100000000000000000001000 );
		        assert result >= 100000000000000000000000;
		        assert result <= 100000000000000000001000;

		       }
		          """, context );
	}

	@DisplayName( "It returns a random number in range using an algorithm" )
	@Test
	public void testItReturnsARandomNumberInRangeWithAlgorithm() {
		instance.executeSource(
		    """
		    bx:loop times=1000 {
		          	result = randRange( 0, 12, "SHA1PRNG" );
		       	assert result >= 0;
		       	assert result <= 12;
		       }
		          """, context );
	}

	@DisplayName( "It includes upper and lower bound" )
	@Test
	public void testItIncludesUpperAndLowerBound() {
		instance.executeSource(
		    """
		      result = []
		    bx:loop times=1000 {
		            	result.append( randRange( 1, 3 ) );
		      		 //result.append( rand() );
		         }
		            """, context );
		assertThat( variables.getAsArray( result ) ).contains( 1L );
		assertThat( variables.getAsArray( result ) ).contains( 2L );
		assertThat( variables.getAsArray( result ) ).contains( 3L );
	}

}
