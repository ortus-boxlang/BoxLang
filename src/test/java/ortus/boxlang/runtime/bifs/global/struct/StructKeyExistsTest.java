
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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class StructKeyExistsTest {

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

	@DisplayName( "It tests the BIF StructKeyExists" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    myStruct={ "foo" : "bar", "flea" : "flah" };
		    result = StructKeyExists( myStruct, "flea" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
		instance.executeSource(
		    """
		    myStruct={ "foo" : "bar", "flea" : "flah" };
		    result = StructKeyExists( myStruct, "flah" );
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the Member function Struct.keyExists" )
	@Test
	public void testMember() {
		instance.executeSource(
		    """
		    myStruct={ "foo" : "bar", "flea" : "flah" };
		    result = myStruct.keyExists( "flea" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
		instance.executeSource(
		    """
		    myStruct={ "foo" : "bar", "flea" : "flah" };
		    result = myStruct.keyExists( "flah" );
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests that null exists" )
	@Test
	public void testNull() {
		instance.executeSource(
		    """
		    myStruct={ "foo" : null };
		    result = myStruct.keyExists( "foo" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It can work on a query row object" )
	@Test
	public void testQueryRow() {
		instance.executeSource(
		    """
		    q = queryNew("col1,col2","string, integer", [ "foo", 42 ]);
		      	result = structKeyExists( q, "col1" );
		      """,
		    context );
		assertThat( variables.getAsBoolean( result ) ).isTrue();
	}

	@DisplayName( "It can work on a query row object that's empty and return an empty struct" )
	@Test
	public void testEmptyQueryRow() {
		instance.executeSource(
		    """
		    q = queryNew("col1,col2","string, integer");
		      	result = structKeyExists( q, "col1" );
		      """,
		    context );
		assertThat( variables.getAsBoolean( result ) ).isFalse();
	}

}
