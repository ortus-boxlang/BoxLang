/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.struct;

import static com.google.common.truth.Truth.assertThat;

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

public class StructKeyArrayTest {

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

	@DisplayName( "It can get keys as an array" )
	@Test
	public void testCanGetKeysAsArray() {
		instance.executeSource(
		    """
		    myStruct = {};
		       result = structKeyArray(myStruct);
		       """,
		    context );

		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    myStruct = { 'name': 'Grant' };
		       result = structKeyArray(myStruct);
		       """,
		    context );

		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "name" );

		instance.executeSource(
		    """
		    myStruct = [ boxlang: true ];
		       result = structKeyArray(myStruct);
		       """,
		    context );

		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "boxlang" );
	}

	@DisplayName( "It can get keys as an array member functions" )
	@Test
	public void testCanGetKeysAsArrayMemberFunctions() {
		instance.executeSource(
		    """
		    myStruct = {};
		       result = myStruct.keyArray();
		       """,
		    context );

		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    myStruct = { 'name': 'Grant' };
		       result = myStruct.keyArray();
		       """,
		    context );

		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "name" );

		instance.executeSource(
		    """
		    myStruct = [ boxlang: true ];
		       result = myStruct.keyArray();
		       """,
		    context );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "boxlang" );
	}
}
