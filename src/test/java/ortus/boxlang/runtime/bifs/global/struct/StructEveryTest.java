
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

public class StructEveryTest {

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
		       ref = {
		       	"foo" : "bar",
		       	"bar" : 1,
		       	"blah" : "blerg"
		       };

		       function eachFn( key, value, struct ){
		       		 values.append( value );
		       		return true;
		       };

		    result = StructEvery( ref, eachFn );
		    """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 3 );
		assertTrue( variables.getAsBoolean( result ) );

	}

	@DisplayName( "It tests the member function Struct.each()" )
	@Test
	public void testMember() {
		instance.executeSource(
		    """
		    values = [];
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function eachFn( key, value, struct ){
		            	values.append( value );
		    		return true;
		       };

		       result = ref.every( eachFn );
		              """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 3 );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It can run in parallel" )
	@Test
	public void testRunInParallel() {
		instance.executeSource(
		    """
		       values = [];
		       ref = {
		       	"foo" : "bar",
		       	"bar" : 1,
		       	"blah" : "blerg"
		       };

		       function eachFn( key, value, struct ){
		       		 values.append( value );
		       		return true;
		       };

		    result = StructEvery( ref, eachFn, true );
		    """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 3 );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It can run in parallel with max threads" )
	@Test
	public void testRunInParallelWithMaxThreads() {
		instance.executeSource(
		    """
		       values = [];
		       ref = {
		       	"foo" : "bar",
		       	"bar" : 1,
		       	"blah" : "blerg"
		       };

		       function eachFn( key, value, struct ){
		       		 values.append( value );
		       		return true;
		       };

		    result = StructEvery( ref, eachFn, true, 2 );
		    """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 3 );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It can run in parallel with virtual threads" )
	@Test
	public void testRunInParallelWithVirtualThreads() {
		instance.executeSource(
		    """
		       values = [];
		       ref = {
		       	"foo" : "bar",
		       	"bar" : 1,
		       	"blah" : "blerg"
		       };

		       function eachFn( key, value, struct ){
		       		 values.append( value );
		       		return true;
		       };

		    result = StructEvery( ref, eachFn, true, true );
		    """,
		    context );
		Array values = ( Array ) variables.get( Key.of( "values" ) );
		assertThat( values.size() ).isEqualTo( 3 );
		assertTrue( variables.getAsBoolean( result ) );
	}

}
