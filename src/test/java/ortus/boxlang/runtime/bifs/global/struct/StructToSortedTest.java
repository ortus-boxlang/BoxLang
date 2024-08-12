
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class StructToSortedTest {

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

	@DisplayName( "It tests the BIF StructToSorted using the textual sort directives" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = StructToSorted( myStruct );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		Array resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "bar" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "foo" );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "zena" );

		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = StructToSorted( myStruct, "textNoCase" );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "bar" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "foo" );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "zena" );

		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = StructToSorted( myStruct, "text", "desc" );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "bar" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "foo" );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "zena" );

		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = StructToSorted( myStruct, "textNoCase", "desc" );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "bar" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "foo" );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "zena" );

	}

	@DisplayName( "It tests the BIF StructToSorted using a callback sort" )
	@Test
	public void testBifCallback() {

		instance.executeSource(
		    """
		        	myStruct = {
		      	cow: {
		      		total: 3
		      	},
		      	pig: {
		      		total: 2
		      	},
		      	cat: {
		      		total: 1
		      	}
		      };

		        	result = StructToSorted( myStruct, ( a , b ) => {
		    	return compare( myStruct[ b ].total, myStruct[ a ].total )
		    } );

		        """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		Array resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "cat" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "pig" );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "cow" );

		instance.executeSource(
		    """
		      	myStruct = {
		    	cow: {
		    		total: 3
		    	},
		    	pig: {
		    		total: 2
		    	},
		    	cat: {
		    		total: 1
		    	}
		    };

		      	result = StructToSorted( myStruct, ( a , b ) => compare( myStruct[ a ].total, myStruct[ b ].total )  );

		      """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "cat" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "pig" );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "cow" );
	}

	@DisplayName( "Can perform a sorted conversion on a case-senstive struct" )
	@Test
	public void testMemberCaseSensitive() {
		instance.executeSource(
		    """
		       myStruct = structNew( "casesensitive" );
		         		myStruct[ "foo" ] = "bar";
		         		myStruct[ "fOO" ] = "bar";
		         		myStruct[ "bar" ] = "foo";
		         		myStruct[ "bAR" ] = "foo";
		    count = myStruct.keyArray().len()

		     result = StructToSorted( myStruct, "text" );

		         """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsInteger( Key.of( "count" ) ), 4 );
		Array resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "bAR" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "bar" );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "fOO" );
		assertEquals( KeyCaster.cast( resultKeys.get( 3 ) ).getName(), "foo" );

		instance.executeSource(
		    """
		       myStruct = structNew( "casesensitive" );
		             myStruct[ "coW" ] = {
		       	total: 3
		       };
		       myStruct[ "Pig" ] = {
		       	total: 2
		       };
		       myStruct[ "cAt" ] = {
		       	total: 1
		       };

		    result = myStruct.toSorted( ( a , b ) => {
		    	return compare( myStruct[ b ].total, myStruct[ a ].total )
		    } );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "cAt" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "Pig" );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "coW" );

		// Sorted structs created from a case sensitive struct should never be allowed to be sorted without case consideration, because entries will be removed
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		           myStruct = structNew( "casesensitive" );
		        			 myStruct[ "foo" ] = "bar";
		        			 myStruct[ "fOO" ] = "bar";
		        			 myStruct[ "bar" ] = "foo";
		        			 myStruct[ "bAR" ] = "foo";
		        count = myStruct.keyArray().len()

		         result = StructToSorted( myStruct, "textNoCase" );

		        	 """,
		        context )

		);

	}

	@DisplayName( "It tests the member function for Struct.ToSorted" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = myStruct.toSorted();

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 3 );
		Array resultKeys = ArrayCaster.cast( variables.getAsStruct( result ).keySet().toArray() );
		assertEquals( KeyCaster.cast( resultKeys.get( 0 ) ).getName(), "bar" );
		assertEquals( KeyCaster.cast( resultKeys.get( 1 ) ).getName(), "foo" );
		assertEquals( KeyCaster.cast( resultKeys.get( 2 ) ).getName(), "zena" );
	}

	@DisplayName( "It tests locale sensitivity for Struct.ToSorted" )
	@Test
	public void testsLocaleSensitiveStructToSorted() {
		// BL-227
		instance.executeSource(
		    """
		      	result = [
		    	"Zulu"  : 10,
		    	"Äpfel" : 20,
		    	"Bravo" : 30,
		    	"Alpha" : 40
		    ];

		      	resultAsc = result.toSorted( "text" );
		      	resultDesc = result.toSorted( "text", "desc" );
		      	resultLS = result.toSorted( "text", "desc", true );

		    testAsc = resultAsc == ["Alpha":40,"Bravo":30,"Zulu":10,"Äpfel":20];
		    testDesc = resultDesc == ["Äpfel":20,"Zulu":10,"Bravo":30,"Alpha":40];
		    testLS = resultLS == ["Zulu":10,"Bravo":30,"Äpfel":20,"Alpha":40];


		      """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).keySet().toArray().length, 4 );
		assertTrue( variables.getAsBoolean( Key.of( "testAsc" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "testDesc" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "testLS" ) ) );
	}

}
