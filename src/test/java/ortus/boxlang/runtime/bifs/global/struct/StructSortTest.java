
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
import ortus.boxlang.runtime.types.Array;

public class StructSortTest {

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

	@DisplayName( "It tests the BIF StructSort using the textual sort directives" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = structSort( myStruct );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 0 ), "bar" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "foo" );
		assertEquals( variables.getAsArray( result ).get( 2 ), "zena" );

		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = structSort( myStruct, "textNoCase" );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 0 ), "bar" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "foo" );
		assertEquals( variables.getAsArray( result ).get( 2 ), "zena" );

		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = structSort( myStruct, "text", "desc" );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 2 ), "bar" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "foo" );
		assertEquals( variables.getAsArray( result ).get( 0 ), "zena" );

		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = structSort( myStruct, "textNoCase", "desc" );

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 2 ), "bar" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "foo" );
		assertEquals( variables.getAsArray( result ).get( 0 ), "zena" );

	}

	@DisplayName( "It tests the BIF With numeric sort and path directives" )
	@Test
	public void testBifPaths() {
		instance.executeSource(
		    """
		      	myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };

		      	result = structSort( myStruct, "numeric", "asc", "total" );

		      """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 0 ), "cat" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "pig" );
		assertEquals( variables.getAsArray( result ).get( 2 ), "cow" );

		instance.executeSource(
		    """
		      	myStruct = {
		    	cow: {
		    		total: 12
		    	},
		    	pig: {
		    		total: 5
		    	},
		    	cat: {
		    		total: 3
		    	}
		    };

		      	result = structSort( myStruct, "numeric", "desc", "total" );

		      """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 2 ), "cat" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "pig" );
		assertEquals( variables.getAsArray( result ).get( 0 ), "cow" );

	}

	@DisplayName( "It tests the BIF StructSort using a callback sort" )
	@Test
	public void testBifCallback() {

		instance.executeSource(
		    """
		        	myStruct = {
		      	cow: {
		      		total: 12
		      	},
		      	pig: {
		      		total: 5
		      	},
		      	cat: {
		      		total: 3
		      	}
		      };

		        	result = structSort( myStruct, ( a , b ) => {
		    	return compare( myStruct[ b ].total, myStruct[ a ].total )
		    } );

		        """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 2 ), "cat" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "pig" );
		assertEquals( variables.getAsArray( result ).get( 0 ), "cow" );

		instance.executeSource(
		    """
		         	myStruct = {
		       	cow: {
		       		total: 12
		       	},
		       	pig: {
		       		total: 5
		       	},
		       	cat: {
		       		total: 3
		       	}
		       };
		    system = createObject( "java", "java.lang.System" );

		         	result = structSort( myStruct, ( a , b ) => compare( myStruct[ a ].total, myStruct[ b ].total )  );

		         """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 0 ), "cat" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "pig" );
		assertEquals( variables.getAsArray( result ).get( 2 ), "cow" );
	}

	@DisplayName( "Can execute sort operations on a case-senstive struct" )
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

		         	result = structSort( myStruct, "textNoCase" );

		         """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsInteger( Key.of( "count" ) ), 4 );
		assertEquals( variables.getAsArray( result ).get( 0 ), "bAR" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "bar" );
		assertEquals( variables.getAsArray( result ).get( 2 ), "fOO" );
		assertEquals( variables.getAsArray( result ).get( 3 ), "foo" );

		instance.executeSource(
		    """
		    myStruct = structNew( "casesensitive" );
		          myStruct[ "coW" ] = {
		    	total: 12
		    };
		    myStruct[ "Pig" ] = {
		    	total: 12
		    };
		    myStruct[ "cAt" ] = {
		    	total: 3
		    };

		               	result = myStruct.sort( ( a , b ) => {
		          		return compare( myStruct[ b ].total, myStruct[ a ].total )
		          	} );

		               """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 2 ), "cAt" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "Pig" );
		assertEquals( variables.getAsArray( result ).get( 0 ), "coW" );

	}

	@DisplayName( "It tests the member function for StructSort" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    	myStruct = {
		    		"foo" : "bar",
		    		"bar" : "foo",
		    		"zena" : "princess warrior"
		    	};

		    	result = myStruct.sort();

		    """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );
		assertEquals( variables.getAsArray( result ).get( 0 ), "bar" );
		assertEquals( variables.getAsArray( result ).get( 1 ), "foo" );
		assertEquals( variables.getAsArray( result ).get( 2 ), "zena" );
	}

}
