
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

package ortus.boxlang.runtime.bifs.global.struct;

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
import ortus.boxlang.runtime.types.IStruct;

public class StructEachTest {

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

	@DisplayName( "It tests the BIF StructEach" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		               values = [];
		               ref = [
		         	"foo" : "bar",
		         	"bar" : 1,
		         	"blah" : "blerg",
		         	"complex" : {
		         		"foo" : "bar"
		    }
		         ];

		               function eachFn( key, value, struct ){
		         	values.append( value );
		               };

		               StructEach( ref, eachFn );
		           """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 4 );
		assertThat( values.get( 0 ) ).isEqualTo( "bar" );
		assertThat( values.get( 1 ) ).isEqualTo( 1 );
		assertThat( values.get( 2 ) ).isEqualTo( "blerg" );
		assertTrue( values.get( 3 ) instanceof IStruct );

	}

	@DisplayName( "It tests the member function Struct.each()" )
	@Test
	public void testMember() {
		instance.executeSource(
		    """
		               values = [];
		               ref = [
		         	"foo" : "bar",
		         	"bar" : 1,
		         	"blah" : "blerg",
		         	"complex" : {
		         		"foo" : "bar"
		    }
		         ];

		    function eachFn( key, value, struct ){
		         	values.append( value );
		    };

		    ref.each( eachFn );
		           """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 4 );
		assertThat( values.get( 0 ) ).isEqualTo( "bar" );
		assertThat( values.get( 1 ) ).isEqualTo( 1 );
		assertThat( values.get( 2 ) ).isEqualTo( "blerg" );
		assertTrue( values.get( 3 ) instanceof IStruct );
	}

	@DisplayName( "Tests using in parallel with no max threads and no ordering" )
	@Test
	public void testBifWithParallel() {
		// @formatter:off
		instance.executeSource(
		    """
		    	values = [];
		    	ref = [
						"foo" : "bar",
						"bar" : 1,
						"blah" : "blerg",
						"complex" : {
							"foo" : "bar"
						}
		       ];

				function eachFn( key, value, struct ){
					values.append( value );
				};

		    	StructEach( ref, eachFn, true );
		       """,
		    context );
		// @formatter:on

		// Note: order is not guaranteed with parallel execution
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 4 );
		// Note: order is not guaranteed with parallel execution
		assertThat( values.contains( "bar" ) ).isTrue();
		assertThat( values.contains( 1 ) ).isTrue();
		assertThat( values.contains( "blerg" ) ).isTrue();
	}

	@DisplayName( "Tests using in parallel with no max threads with ordering" )
	@Test
	public void testBifWithParallelWithOrdering() {
		// @formatter:off
		instance.executeSource(
		    """
		    	values = [];
		    	ref = [
						"foo" : "bar",
						"bar" : 1,
						"blah" : "blerg",
						"complex" : {
							"foo" : "bar"
						}
		       ];

				function eachFn( key, value, struct ){
					values.append( value );
				};

		    	StructEach( ref, eachFn, true, 0, true );
		       """,
		    context );
		// @formatter:on

		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 4 );
		assertThat( values.get( 0 ) ).isEqualTo( "bar" );
		assertThat( values.get( 1 ) ).isEqualTo( 1 );
		assertThat( values.get( 2 ) ).isEqualTo( "blerg" );
		assertTrue( values.get( 3 ) instanceof IStruct );
	}

	@DisplayName( "Tests using in parallel with max threads" )
	@Test
	public void testBifWithParallelWithMaxThreads() {
		// @formatter:off
		instance.executeSource(
		    """
		    	values = [];
		    	ref = [
						"foo" : "bar",
						"bar" : 1,
						"blah" : "blerg",
						"complex" : {
							"foo" : "bar"
						}
		       ];

				function eachFn( key, value, struct ){
					values.append( value );
				};

		    	StructEach( ref, eachFn, true, 2 );
		       """,
		    context );
		// @formatter:on

		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 4 );
		// Note: order is not guaranteed with parallel execution
		assertThat( values.contains( "bar" ) ).isTrue();
		assertThat( values.contains( 1 ) ).isTrue();
		assertThat( values.contains( "blerg" ) ).isTrue();
	}
}
