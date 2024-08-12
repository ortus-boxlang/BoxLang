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

import java.util.List;

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
import ortus.boxlang.runtime.types.Query;

public class QueryReverseTest {

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

	@DisplayName( "It should reverse the query" )
	@Test
	public void testReverseQuery() {
		// @formatter:off
		instance.executeSource(
		    """
				result = QueryNew( "id,name", "integer,varchar" );
				QueryAddRow( result, { id = 1, name = "John" } );
				QueryAddRow( result, { id = 2, name = "Jane" } );
				QueryAddRow( result, { id = 3, name = "Jim" } );
				QueryAddRow( result, { id = 4, name = "Jill" } );
				QueryAddRow( result, { id = 5, name = "Jack" } );

		   		println( result )

				result = QueryReverse( result );

				println( "********************" )

				println( result )

		       """,
		    context );
		// @formatter:on

		Query			query	= ( Query ) variables.get( result );
		List<Object[]>	data	= query.getData();
		// Test that it's in reverse order
		assert data.get( 0 )[ 0 ].equals( 5 );
		assert data.get( 1 )[ 0 ].equals( 4 );
		assert data.get( 2 )[ 0 ].equals( 3 );
		assert data.get( 3 )[ 0 ].equals( 2 );
		assert data.get( 4 )[ 0 ].equals( 1 );

	}

}
