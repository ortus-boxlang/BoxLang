
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

import static org.junit.Assert.assertThrows;
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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class StructFindTest {

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

	@DisplayName( "It tests the BIF StructFind" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    myStruct={ "foo" : "bar", "flea" : "flah" };
		    result = StructFind( myStruct, "flea" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "flah" );
		instance.executeSource(
		    """
		    myStruct={ "foo" : { "flea" : "flah" } };
		    result = StructFind( myStruct, "foo" );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
	}

	@DisplayName( "It tests the BIF StructFind with and without default values" )
	@Test
	public void testBifDefaultValues() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        myStruct={};
		        result = StructFind( myStruct, "flea" );
		        """,
		        context )
		);
		instance.executeSource(
		    """
		    myStruct={};
		    result = StructFind( myStruct, "foo", "bar" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "bar" );
	}

	@DisplayName( "It tests the member function for Struct.Find" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    myStruct={ "foo" : "bar", "flea" : "flah" };
		    result = myStruct.find( "flea" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "flah" );
		instance.executeSource(
		    """
		    myStruct={ "foo" : { "flea" : "flah" } };
		    result = myStruct.find( "foo" );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
	}

}
