
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
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.ListUtil;

public class StructReduceTest {

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

	@DisplayName( "Can reduce a struct to an array" )
	@Test
	public void testStructReduce() {
		instance.executeSource(
		    """
		              struct = [
		    "foo" : "bar",
		    "flea" : "flah"
		     ];

		              function reduction( acc, key, value ){
		    	acc.append( value );
		    	return acc;
		              };

		              result = structReduce( struct, reduction, [] );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 2 );
	}

	@DisplayName( "Can reduce a struct to a new struct" )
	@Test
	public void testStructReduceStruct() {
		instance.executeSource(
		    """
		              struct = [
		    "foo" : "bar",
		    "flea" : "flah"
		     ];

		              function reduction( acc, key, value ){
		    	acc.insert( value, key );
		    	return acc;
		              };

		              result = structReduce( struct, reduction, {} );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		assertEquals( variables.getAsStruct( result ).size(), 2 );
		assertTrue( variables.getAsStruct( result ).containsKey( "bar" ) );
		assertTrue( variables.getAsStruct( result ).containsKey( "flah" ) );
	}

	@DisplayName( "Can reduce a struct to a string" )
	@Test
	public void testStructReduceSimple() {
		instance.executeSource(
		    """
		              struct = [
		    "foo" : "bar",
		    "flea" : "flah"
		     ];

		              function reduction( acc, key, value ){
		    	return listAppend( acc, value );
		              };

		              result = structReduce( struct, reduction, "" );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( ListUtil.asList( variables.getAsString( result ), "," ).size(), 2 );
	}

	@DisplayName( "Can reduce a struct in to a nested object" )
	@Test
	public void testStructReduceComplex() {
		instance.executeSource(
		    """
		                struct = [
		      "foo" : "bar",
		      "flea" : "flah"
		       ];

		    accumulator = {
		    "items" = []
		    }

		                function reduction( acc, key, value ){
		    		acc.items.append( [ key, value ], true );
		    		return acc;
		                };

		                result = structReduce( struct, reduction, accumulator );
		      """,
		    context );
		assertTrue( variables.get( result ) instanceof Struct );
		IStruct accumulator = variables.getAsStruct( Key.of( "accumulator" ) );
		assertEquals( ArrayCaster.cast( accumulator.get( "items" ) ).size(), 4 );

	}

	@DisplayName( "Can reduce a using a member function" )
	@Test
	public void testStructReduceMember() {
		instance.executeSource(
		    """
		              struct = [
		    "foo" : "bar",
		    "flea" : "flah"
		     ];

		              function reduction( acc, key, value ){
		    	acc.append( value );
		    	return acc;
		              };

		              result = struct.reduce( reduction, [] );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 2 );
	}

}
