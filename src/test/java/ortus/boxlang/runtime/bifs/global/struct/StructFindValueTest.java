
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class StructFindValueTest {

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

	@DisplayName( "It tests the BIF StructFindValue" )
	@Test
	public void testBif() {
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
		       result = StructFindValue( myStruct, "12" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 1 );
		instance.executeSource(
		    """
		    myStruct = {
		    	cow: "mammal",
		    	pig: "mammal",
		    	cat: "mammal"
		    };
		       result = StructFindValue( myStruct, "mammal", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );

		instance.executeSource(
		    """
		    myStruct = {
		    	cow: "farm",
		    	pig: "farm",
		    	cat: "house"
		    };
		       result = StructFindValue( myStruct, "farm", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 2 );
		IStruct firstItem = StructCaster.cast( variables.getAsArray( result ).get( 0 ) );
		assertTrue( firstItem.containsKey( Key.owner ) );
		assertTrue( firstItem.containsKey( Key.key ) );
		assertTrue( firstItem.containsKey( Key.path ) );

	}

	@DisplayName( "It tests a comparison of value when arrays are within the struct" )
	@Test
	public void testBifWithArrays() {
		instance.executeSource(
		    """
		      myStruct = {
		      	cow: "farm",
		      	pig: "farm",
		      	cat: [
		    	"house",
		    	"barn"
		    ]
		      };
		         result = StructFindValue( myStruct, "farm", "all" );
		         """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 2 );
		IStruct firstItem = StructCaster.cast( variables.getAsArray( result ).get( 0 ) );
		assertTrue( firstItem.containsKey( Key.owner ) );
		assertTrue( firstItem.containsKey( Key.key ) );
		assertTrue( firstItem.containsKey( Key.path ) );
	}

	@DisplayName( "It tests the member function for StructFindValue" )
	@Test
	public void testMemberFunction() {

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
		       result = myStruct.findValue( "12" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 1 );
		instance.executeSource(
		    """
		    myStruct = {
		    	cow: "mammal",
		    	pig: "mammal",
		    	cat: "mammal"
		    };
		       result = myStruct.findValue( "mammal", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 3 );

		instance.executeSource(
		    """
		    myStruct = [
		    	cow: "farm",
		    	pig: "farm",
		    	cat: "house"
		    ];
		       result = myStruct.findValue( "farm", "all" );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof Array );
		assertEquals( variables.getAsArray( result ).size(), 2 );
		IStruct firstItem = StructCaster.cast( variables.getAsArray( result ).get( 0 ) );
		assertTrue( firstItem.containsKey( Key.owner ) );
		assertTrue( firstItem.containsKey( Key.key ) );
		assertTrue( firstItem.containsKey( Key.path ) );
		assertEquals( firstItem.get( Key.key ), "cow" );
		assertEquals( firstItem.get( Key.path ), ".cow" );
	}

	@DisplayName( "It tests the Correct ownership is assigned to entries returned" )
	@Test
	public void testOwnership() {

		instance.executeSource(
		    """
		    data = {
		    	alpha: "a",
		    	bravo: "needle",
		    	charlie: "needle",
		    	child: {
		    		delta: "d",
		    		echo: "NEEDLE",
		    		grandchild: {
		    			foxtrot: "nEeDlE",
		    			golf: "g"
		    		}
		    	}
		    };

		    matches = StructFindValue( data, "Needle", "all" );
		    // acf / lucee arrays are different order
		    result = matches.reduce( ( acc, el ) => {
		    	acc[ el.key ] = el;
		    	return acc;
		    }, {} );
		         """,
		    context );
		IStruct findings = variables.getAsStruct( result );
		assertTrue( findings.containsKey( Key.of( "bravo" ) ) );
		assertTrue( findings.containsKey( Key.of( "charlie" ) ) );
		assertTrue( findings.containsKey( Key.of( "echo" ) ) );
		assertTrue( findings.containsKey( Key.of( "foxtrot" ) ) );

		assertInstanceOf( IStruct.class, findings.getAsStruct( Key.of( "echo" ) ).get( Key.owner ) );
		assertInstanceOf( IStruct.class, findings.getAsStruct( Key.of( "foxtrot" ) ).get( Key.owner ) );

	}

	@DisplayName( "It tests that values can be located within arrays inside structs" )
	@Test
	public void testValuesInArrays() {
		instance.executeSource(
		    """
		       data = JSONDeserialize('{"children":[{"children":[{"children":[],"id":"1072"},{"children":[],"id":"1073"},{"children":[],"id":"1082"},{"children":[],"id":"2319"}],"id":"1068"},{"children":[{"children":[],"id":"1076"},{"children":[{"children":[],"id":"1091"},{"children":[],"id":"2324"}],"id":"2328"},{"children":[],"id":"2321"}],"id":"1069"},{"children":[{"children":[],"id":"1079"},{"children":[],"id":"2323"}],"id":"1071"},{"children":[{"children":[],"id":"2320"},{"children":[],"id":"2325"},{"children":[],"id":"1074"}],"id":"2322"},{"children":[],"id":"2609"},{"children":[{"children":[],"id":"1"},{"children":[],"id":"3"},{"children":[],"id":"5"}],"id":"-1"},{"children":[{"children":[],"id":"2677"}],"id":"2676"}],"id":"1067"}');

		       result = structFindValue( data, 3, "ALL" );
		    """,
		    context );
		Array findings = variables.getAsArray( result );
		assertThat( findings.size() ).isEqualTo( 1 );

		// Verify the structure of the found result
		IStruct foundItem = StructCaster.cast( findings.get( 0 ) );
		assertTrue( foundItem.containsKey( Key.owner ) );
		assertTrue( foundItem.containsKey( Key.key ) );
		assertTrue( foundItem.containsKey( Key.path ) );

		// Verify the path uses proper 1-based array indexing with bracket notation
		String path = foundItem.getAsString( Key.path );
		assertThat( path ).isEqualTo( ".children[6].children[2].id" );

		// Verify the key is correct
		assertThat( foundItem.getAsString( Key.key ) ).isEqualTo( "id" );

		// Test another value to ensure multiple results work correctly
		instance.executeSource(
		    """
		       result2 = structFindValue( data, "1", "ALL" );
		    """,
		    context );
		Array findings2 = variables.getAsArray( Key.of( "result2" ) );
		assertThat( findings2.size() ).isEqualTo( 1 );

		IStruct	foundItem2	= StructCaster.cast( findings2.get( 0 ) );
		String	path2		= foundItem2.getAsString( Key.path );
		assertThat( path2 ).isEqualTo( ".children[6].children[1].id" );
		assertThat( foundItem2.getAsString( Key.key ) ).isEqualTo( "id" );

		// Test finding multiple values in arrays
		instance.executeSource(
		    """
		       result3 = structFindValue( data, "1072", "ALL" );
		    """,
		    context );
		Array findings3 = variables.getAsArray( Key.of( "result3" ) );
		assertThat( findings3.size() ).isEqualTo( 1 );

		IStruct	foundItem3	= StructCaster.cast( findings3.get( 0 ) );
		String	path3		= foundItem3.getAsString( Key.path );
		assertThat( path3 ).isEqualTo( ".children[1].children[1].id" );
		assertThat( foundItem3.getAsString( Key.key ) ).isEqualTo( "id" );

	}

}
