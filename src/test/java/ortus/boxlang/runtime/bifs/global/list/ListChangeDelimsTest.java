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

package ortus.boxlang.runtime.bifs.global.list;

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

public class ListChangeDelimsTest {

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

	@DisplayName( "It can change the delimiter from the default comma" )
	@Test
	public void testChangeDefaultDelimiter() {
		instance.executeSource(
		    """
		    	list = "a,b,c";
		    	result = listChangeDelims( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b:c" );
		instance.executeSource(
		    """
		    list = "a,b,c,d";
		    result = listChangeDelims( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b:c:d" );
		instance.executeSource(
		    """
		    list = "";
		    result = listChangeDelims( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "" );
	}

	@DisplayName( "It can change the delimiter from one custom delimiter to another" )
	@Test
	public void testChangeCustomDelimiter() {
		instance.executeSource(
		    """
		    	list = "a:b:c";
		    	result = listChangeDelims( list, "-", ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a-b-c" );
		instance.executeSource(
		    """
		    list = "a:b,c";
		    result = listChangeDelims( list, "-", ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a-b,c" );
		instance.executeSource(
		    """
		    list = "a,b,c";
		    result = listChangeDelims( list, ":", "-" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b,c" );
	}

	@DisplayName( "It can count empty spaces as items" )
	@Test
	public void testIncludeEmptyFields() {
		instance.executeSource(
		    """
		    	list = "a,b,,c";
		    	result = listChangeDelims( list, ":", ",", true );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b::c" );
		instance.executeSource(
		    """
		    	list = "a,b,,c";
		    	result = listChangeDelims( list, ":", ",", false );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b:c" );
	}

	@DisplayName( "It can use the member function" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    	list = "a,b,c";
		    	result = list.listChangeDelims( ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b:c" );
		instance.executeSource(
		    """
		    	list = "a:b::c";
		    	result = list.listChangeDelims( "-", ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a-b-c" );
		instance.executeSource(
		    """
		    	list = "a:b,c::d";
		    	result = list.listChangeDelims( "-", ":", true );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a-b,c--d" );
	}
}
