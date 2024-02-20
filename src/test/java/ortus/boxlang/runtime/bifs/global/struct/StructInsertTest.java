
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
import ortus.boxlang.runtime.types.IStruct;

public class StructInsertTest {

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

	@DisplayName( "It tests the BIF StructIsert" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		       myStruct={};
		    result = structInsert( myStruct, "foo", "bar" );
		       """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );

	}

	@DisplayName( "It tests the BIF StructIsert" )
	@Test
	public void testBifOverwriteFalse() {
		instance.executeSource(
		    """
		         myStruct={
		    "foo" : "baz"
		      };
		      result = structInsert( myStruct, "foo", "bar" );
		         """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );

	}

	@DisplayName( "It tests the BIF StructIsert with overwrite" )
	@Test
	public void testBifOverwriteTrue() {
		instance.executeSource(
		    """
		         myStruct={
		    "foo" : "baz"
		      };
		      result = structInsert( myStruct, "foo", "bar", true );
		         """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
		assertThat( variables.getAsStruct( Key.of( "myStruct" ) ).get( Key.of( "foo" ) ) ).isEqualTo( "bar" );

	}

	@DisplayName( "It tests the member function for StructInsert" )
	@Test
	public void testMemberFunction() {

		instance.executeSource(
		    """
		    result = {}.insert( "foo", "bar" );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
		assertTrue( variables.getAsStruct( result ).containsKey( "foo" ) );

	}

}
