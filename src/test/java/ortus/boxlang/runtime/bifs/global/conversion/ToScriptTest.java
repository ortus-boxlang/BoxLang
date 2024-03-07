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

package ortus.boxlang.runtime.bifs.global.conversion;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ToScriptTest {

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

	@Test
	public void testCanScriptNumbers() {
		instance.executeSource(
		    """
		    result = toScript( 123, "myVar" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "myVar = 123;" );
	}

	@Test
	public void testCanScriptStrings() {
		instance.executeSource(
		    """
		    result = toScript( "Hello, World!", "myVar" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "myVar = \"Hello, World!\";" );
	}

	@Test
	public void testCanScriptArrays() {
		instance.executeSource(
		    """
		    result = toScript( [ 1, 2, 3 ], "myVar" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "myVar = [ 1, 2, 3 ];" );
	}

	@Test
	public void testCanScriptDate() {
		instance.executeSource(
		    """
		    myDate = parseDateTime( "2024-03-07T16:35:43.362397-06:00" );
		    result = toScript( myDate, "myVar" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "myVar = new Date('2024-03-07T16:35:43.362397-06:00');" );
	}

	@Test
	public void testCanScriptStruct() {
		instance.executeSource(
		    """
		    myStruct = { "name" : "Grant" };
		    result = toScript( myStruct, "myVar" )
		    """,
		    context );

		String myResult = variables.getAsString( result );

		assertThat( myResult ).contains( "myVar = {" );
		assertThat( myResult ).contains( "\"name\"" );
		assertThat( myResult ).contains( "\"Grant\"" );
	}

	@Test
	public void testCanScriptQuery() {
		instance.executeSource(
		    """
		       myQuery = queryNew( "col1", "varchar", { col1: "Grant" } );
		    result = toScript( myQuery, "myVar" )
		       """,
		    context );

		String myResult = variables.getAsString( result );

		assertThat( myResult ).contains( "myVar = {" );
		assertThat( myResult ).contains( "\"columns\"" );
		assertThat( myResult ).contains( "\"col1\"" );
		assertThat( myResult ).contains( "\"data\"" );
		assertThat( myResult ).contains( "\"Grant\"" );

	}
}
