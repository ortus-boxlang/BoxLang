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

public class ListItemTrimTest {

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

	@DisplayName( "It can trim each item in a list" )
	@Test
	public void testTrimDefaultDelimiter() {
		instance.executeSource(
		    """
		    list = "  a, b , c  ";
		    result = listItemTrim( list );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b,c" );
		instance.executeSource(
		    """
		    list = "a, b  ,c";
		    result = listItemTrim( list );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b,c" );
	}

	@DisplayName( "It can trim each list item using a custom delimiter" )
	@Test
	public void testItemTrimCustomDelimiter() {
		instance.executeSource(
		    """
		    list = "  a: b : c  ";
		    result = listItemTrim( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b:c" );

		instance.executeSource(
		    """
		    list = "a, b : c ";
		    result = listItemTrim( list, ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a, b:c" );

		instance.executeSource(
		    """
		    list = " a , b : c - d ";
		    result = listItemTrim( list, ",:-" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b:c-d" );

		instance.executeSource(
		    """
		    list = " a -and- b -and- c -and- d ";
		    result = listItemTrim( list, "-and-", true, true );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a-and-b-and-c-and-d" );
	}

	@DisplayName( "It can include empty fields when trimming items" )
	@Test
	public void testItemTrimIncludeEmpty() {
		instance.executeSource(
		    """
		    list = "  a, b , ,, c  ";
		    result = listItemTrim( list, ",", false );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b,,c" );
		instance.executeSource(
		    """
		    list = "  a, b , ,, c  ";
		    result = listItemTrim( list, ",", true );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b,,,c" );
	}

	@DisplayName( "It can us a member function to trim list items" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    list = "  a, b , c  ";
		    result = list.listItemTrim();
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a,b,c" );
		instance.executeSource(
		    """
		    list = "a: b  :c";
		    result = list.listItemTrim( ":" );
		    """,
		    context
		);
		assertThat( variables.get( result ) ).isEqualTo( "a:b:c" );
	}
}
