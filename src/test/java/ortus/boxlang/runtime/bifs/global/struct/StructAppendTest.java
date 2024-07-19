
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import ortus.boxlang.runtime.types.IStruct;

public class StructAppendTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			target	= new Key( "target" );
	static Key			appends	= new Key( "appends" );

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

	@DisplayName( "It tests the BIF StructAppend with default overwrite" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    	target = {
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
		    	appends = {
		        	cow: {
		        		total: 10,
		        		home : "farm"
		        	},
		        	pig: {
		        		total: 2,
		        		home : "farm"
		        	},
		        	cat: {
		        		total: 1,
		        		home : "house"
		        	},
		        	dog: {
		        		total: 2,
		        		home : "house"
		        	}
		        };
		    	result = StructAppend( target, appends );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
		assertTrue( variables.getAsStruct( target ).containsKey( "dog" ) );
		assertTrue( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).containsKey( "total" ) );
		assertTrue( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).containsKey( "home" ) );
		assertEquals( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).get( Key.of( "total" ) ), 10 );
		assertEquals( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).get( Key.of( "home" ) ), "farm" );

	}

	@DisplayName( "It tests the BIF StructAppend without overwrite" )
	@Test
	public void testBifNoOverwrite() {
		instance.executeSource(
		    """
		    	target = {
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
		    	appends = {
		        	cow: {
		        		total: 10,
		        		home : "farm"
		        	},
		        	pig: {
		        		total: 2,
		        		home : "farm"
		        	},
		        	cat: {
		        		total: 1,
		        		home : "house"
		        	},
		        	dog: {
		        		total: 2,
		        		home : "house"
		        	}
		        };
		    	result = StructAppend( target, appends, false );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
		assertTrue( variables.getAsStruct( target ).containsKey( "dog" ) );
		assertTrue( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).containsKey( "total" ) );
		assertFalse( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).containsKey( "home" ) );
		assertEquals( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).get( Key.of( "total" ) ), 12 );

	}

	@DisplayName( "It tests the member function for Struct.Append" )
	@Test
	public void testMemberFunction() {

		instance.executeSource(
		    """
		    	target = {
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
		    	appends = {
		        	cow: {
		        		total: 10,
		        		home : "farm"
		        	},
		        	pig: {
		        		total: 2,
		        		home : "farm"
		        	},
		        	cat: {
		        		total: 1,
		        		home : "house"
		        	},
		        	dog: {
		        		total: 2,
		        		home : "house"
		        	}
		        };
		    	result = target.append( appends );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertTrue( variables.getAsStruct( target ).containsKey( "dog" ) );
		assertTrue( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).containsKey( "total" ) );
		assertTrue( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).containsKey( "home" ) );
		assertEquals( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).get( Key.of( "total" ) ), 10 );
		assertEquals( StructCaster.cast( variables.getAsStruct( target ).get( Key.of( "cow" ) ) ).get( Key.of( "home" ) ), "farm" );
	}

}
