
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

package ortus.boxlang.runtime.bifs.global.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class DuplicateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			resultKey	= new Key( "result" );
	static Key			refKey		= new Key( "ref" );

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

	@DisplayName( "It tests the BIF Duplicate can duplicate a struct" )
	@Test
	public void testDuplicateStruct() {
		instance.executeSource(
		    """
		         ref = {
		    timestamp: now(),
		      	foo : {
		      		bar : "baz"
		      	}
		      };
		         result = duplicate( ref );
		      result.foo.bar = "blah";
		         """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "baz" );

		instance.executeSource(
		    """
		           ref = {
		    	"a": 10,
		    	"b": [
		    		20,30
		    	],
		    	"c": [
		    		{ "d": 40 }
		    	]
		    };
		           result = duplicate( ref );
		     result.z = 50;
		     result.y = 50;
		    // mutate the original
		    ref.a = 100;
		    ref.b.append(400);
		    ref.c[ 1 ].d = 500;
		    ref.c[ 1 ].e = 600;
		           """,
		    context );
		ref		= StructCaster.cast( variables.get( refKey ) );
		result	= StructCaster.cast( variables.get( resultKey ) );
		assertEquals( ref.containsKey( "z" ), false );
		assertEquals( result.containsKey( "z" ), true );
		assertEquals( ref.containsKey( "y" ), false );
		assertEquals( result.containsKey( "y" ), true );
		assertEquals( ref.getAsInteger( Key.of( "a" ) ), 100 );
		assertEquals( result.getAsInteger( Key.of( "a" ) ), 10 );
		assertEquals( ref.getAsArray( Key.of( "b" ) ).size(), 3 );
		assertEquals( result.getAsArray( Key.of( "b" ) ).size(), 2 );
		assertEquals( StructCaster.cast( ref.getAsArray( Key.of( "c" ) ).get( 0 ) ).getAsInteger( Key.of( "d" ) ), 500 );
		assertEquals( StructCaster.cast( ref.getAsArray( Key.of( "c" ) ).get( 0 ) ).getAsInteger( Key.of( "e" ) ), 600 );
		assertEquals( StructCaster.cast( result.getAsArray( Key.of( "c" ) ).get( 0 ) ).get( Key.of( "d" ) ), 40 );
		assertEquals( StructCaster.cast( result.getAsArray( Key.of( "c" ) ).get( 0 ) ).containsKey( "e" ), false );
	}

	@DisplayName( "It tests the BIF Duplicate can duplicate a struct containing a closure" )
	@Test
	public void testDuplicateStructWithClosure() {
		instance.executeSource(
		    """
		           ref = {
		    timestamp: now(),
		        	foo : {
		        		bar : ()=>"baz"
		        	}
		        };
		           result = duplicate( ref );
		        result.foo.bar = "blah";
		           """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ) instanceof Function );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
	}

	@DisplayName( "It tests can duplicate a linked struct" )
	@Test
	public void testDuplicateStructLinked() {
		instance.executeSource(
		    """
		            ref = [
		    timestamp: now(),
		         	foo : [
		    	bar : "baz"
		    ]
		         ];
		            result = duplicate( ref );
		         result.foo.bar = "blah";
		            """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "baz" );
	}

	@DisplayName( "It tests can duplicate a sorted struct" )
	@Test
	public void testDuplicateStructSorted() {
		Comparator<Key>	comp	= ( k1, k2 ) -> Integer.compare( k2.getName().length(), k1.getName().length() );
		IStruct			sorted	= Struct.sortedOf(
		    comp,
		    "timestamp", new DateTime(),
		    "foo", new Struct( new HashMap<Key, Object>() {

			    {
				    put( Key.of( "bar" ), "baz" );
			    }
		    } )
		);
		variables.put( refKey, sorted );
		instance.executeSource(
		    """
		       result = duplicate( ref );
		    result.foo.bar = "blah";
		       """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "baz" );
	}

	@DisplayName( "It the BIF Duplicate can duplicate an array" )
	@Test
	public void testDuplicateArray() {

		instance.executeSource(
		    """
		          ref = [ "foo", "bar", "baz", { "foo" : "bar" }, now() ];
		          result = duplicate( ref );
		    result.deleteAt( 2 );
		       result[ 3 ].foo = "blah";
		          """,
		    context );

		Array	ref		= ArrayCaster.cast( variables.get( refKey ) );
		Array	result	= ArrayCaster.cast( variables.get( resultKey ) );
		assertEquals( ref.size(), 5 );
		assertEquals( result.size(), 4 );
		assertEquals( ref.getAt( 2 ), "bar" );
		assertEquals( result.getAt( 2 ), "baz" );
		assertEquals( StructCaster.cast( ref.getAt( 4 ) ).get( Key.of( "foo" ) ), "bar" );
		assertEquals( StructCaster.cast( result.getAt( 3 ) ).get( Key.of( "foo" ) ), "blah" );
		assertTrue( result.getAt( 4 ) instanceof DateTime );
	}

	@DisplayName( "It the BIF Duplicate can duplicate a DateTime Object" )
	@Test
	public void testDuplicateDateTime() {
		instance.executeSource(
		    """
		          ref = now();
		          result = duplicate( ref );
		    result.add( "d", 1 );
		          """,
		    context );

		DateTime	ref		= DateTimeCaster.cast( variables.get( refKey ) );
		DateTime	result	= DateTimeCaster.cast( variables.get( resultKey ) );

		assertEquals( ref.getWrapped().getYear(), result.getWrapped().getYear() );
		assertNotEquals( ref.getWrapped().getDayOfWeek(), result.getWrapped().getDayOfWeek() );
	}

	@DisplayName( "It can duplicate a variety of other types" )
	@Test
	public void testDuplicateOthers() {
		instance.executeSource(
		    """
		    ref = "foo";
		    result = duplicate( ref );
		    """,
		    context );
		assertEquals( variables.get( refKey ), variables.get( resultKey ) );
		instance.executeSource(
		    """
		    ref = 12345.4567;
		    result = duplicate( ref );
		    """,
		    context );
		assertEquals( variables.get( refKey ), variables.get( resultKey ) );

		instance.executeSource(
		    """
		    ref = 1 + 2;
		    result = duplicate( ref );
		    """,
		    context );
		assertEquals( variables.get( refKey ), variables.get( resultKey ) );
	}

	@Disabled( "Performance benchmark test on a struct" )
	@Test
	public void benchmarkStruct() {
		instance.executeSource(
		    """
		     ref = {
		       	foo : {
		       		bar : "baz",
		    		blah : {
		    			"blerg" : true
		    		}
		       	}
		    };
		                system = createObject( "java", "java.lang.System" );
		             start = system.currentTimeMillis();
		       duplicate( ref, true )
		             onceTime = ( system.currentTimeMillis() - start );
		       for( i=1; i <= 100000; i++ ){
		        duplicate( ref, true )
		             }
		             end = system.currentTimeMillis();
		          totalTime = ( end - start );
		                  """,
		    context );
	}

	@Disabled( "Performance benchmark test on an array" )
	@Test
	public void benchmarkArray() {
		instance.executeSource(
		    """
		     ref = [ "foo", "bar", "baz", { "foo" : "bar" } ];
		             system = createObject( "java", "java.lang.System" );
		          start = system.currentTimeMillis();
		    duplicate( ref, true )
		          onceTime = ( system.currentTimeMillis() - start );
		    for( i=1; i <= 100000; i++ ){
		     duplicate( ref, true )
		          }
		          end = system.currentTimeMillis();
		       totalTime = ( end - start );
		               """,
		    context );
	}

}
