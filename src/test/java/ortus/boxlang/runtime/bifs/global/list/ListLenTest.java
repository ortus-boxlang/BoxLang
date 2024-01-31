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

public class ListLenTest {

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

	@DisplayName( "It can calculate the length of a list with the default comma delimiter" )
	@Test
	public void testLenDefault() {
		instance.executeSource(
		    """
		    	list = "a,b,c";
		    	result = listLen( list );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 3 );
		instance.executeSource(
		    """
		    list = "a,b,c,d";
		    result = listLen( list );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 4 );
		instance.executeSource(
		    """
		    list = "";
		    result = listLen( list );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "It can use a custom delimiter" )
	@Test
	public void testCustomDelimiter() {
		instance.executeSource(
		    """
		    	list = "a:b:c";
		    	result = listLen( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 3 );
		instance.executeSource(
		    """
		    list = "a:b,c";
		    result = listLen( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 2 );
		instance.executeSource(
		    """
		    list = "a,b,c";
		    result = listLen( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It can count empty spaces as items" )
	@Test
	public void testIncludeEmptyFields() {
		instance.executeSource(
		    """
		    	list = "a,b,,c";
		    	result = listLen( list, ",", true );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 4 );
		instance.executeSource(
		    """
		    	list = "a,b,,c";
		    	result = listLen( list, ",", false );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "It can use the member function" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    	list = "a,b,c";
		    	result = list.listLen();
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 3 );
		instance.executeSource(
		    """
		    	list = "a:b::c";
		    	result = list.listLen( ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 3 );
		instance.executeSource(
		    """
		    	list = "a:b,c::d";
		    	result = list.listLen( ":", true );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}
}
