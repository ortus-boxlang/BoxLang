
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
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class StructFindValueTest {

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

}
