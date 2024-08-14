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

package ortus.boxlang.runtime.bifs.global.query;

import org.junit.jupiter.api.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

import static com.google.common.truth.Truth.assertThat;

public class QueryClearTest {

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

	@DisplayName( "It should clear the query" )
	@Test
	public void testQueryClear() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );
		    QueryClear( query );
		    result = QueryRecordCount( query );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "It should clear query using member function" )
	@Test
	public void testQueryClearMemberFunction() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );
		    query.clear();
		    result = QueryRecordCount( query );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}
}
