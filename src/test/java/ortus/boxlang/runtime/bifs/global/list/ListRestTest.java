
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

public class ListRestTest {

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

	@DisplayName( "It tests the BIF ListRest" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    list = "a,b,c";
		    result = listRest( list );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "b,c" );

		instance.executeSource(
		    """
		    list = "a";
		    result = listRest( list );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "" );

		instance.executeSource(
		    """
		    list = "a|b|c";
		    result = listRest( list, "|" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "b|c" );

	}

	@DisplayName( "It tests the member function for ListRest" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    list = "a,b,c";
		    result = list.listRest();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "b,c" );
	}

	@DisplayName( "It can cast stringable objects" )
	@Test
	public void testStringable() {
		instance.executeSource(
		    """
		    list = createObject( "java", "java.net.URI" ).init( "https://google.com/?q=test" );
		    result = listRest( list, "?" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "q=test" );
	}

}
