
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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;

public class StructFilterTest {

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

	@DisplayName( "It tests the BIF StructFilter" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function filterFn( key, value, struct ){
		    		return value == "blerg";
		       };

		      result = StructFilter( ref, filterFn );
		      """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 1 );
		instance.executeSource(
		    """
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function filterFn( key, value, struct ){
		    		return value == "bloop";
		       };

		      result = StructFilter( ref, filterFn );
		      """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 0 );

	}

	@DisplayName( "It tests the member function Struct.some()" )
	@Test
	public void testMember() {
		instance.executeSource(
		    """
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function filterFn( key, value, struct ){
		    		return value == "blerg";
		       };

		       result = ref.filter( filterFn );
		              """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 1 );
		instance.executeSource(
		    """
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function filterFn( key, value, struct ){
		    		return value == "bloop";
		       };

		       result = ref.filter( filterFn );
		              """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 0 );
	}

	@DisplayName( "It should execute in parallel with no maxThreads arg" )
	@Test
	public void testParallelDefault() {
		instance.executeSource(
		    """
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function filterFn( key, value, struct ){
		    		return value == "blerg";
		       };

		       result = ref.filter( filterFn, true );
		              """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 1 );
	}

	@DisplayName( "It should execute in parallel with a max threads arg" )
	@Test
	public void testParallelMaxThreads() {
		instance.executeSource(
		    """
		    ref = {
		    	"foo" : "bar",
		    	"bar" : 1,
		    	"blah" : "blerg"
		    };

		       function filterFn( key, value, struct ){
		    		return value == "blerg";
		       };

		       result = ref.filter( filterFn, true, 3 );
		              """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 1 );
	}

}
