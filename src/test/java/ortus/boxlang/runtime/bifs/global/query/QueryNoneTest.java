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

public class QueryNoneTest {

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

	@DisplayName( "It should return true if none of the values match" )
	@Test
	public void testReturnTrueIfAllMatch() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );

		    result = queryNone( query, ( row, i, query ) => {
		    	return row.id > 10;
		    } );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It should return false if any of the values match" )
	@Test
	public void testReturnFalseIfAnyDoNotMatch() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );

		    result = queryNone( query, ( row, i, query ) -> {
		    	return row.id > 1;
		    } );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "It should work using member function" )
	@Test
	public void testQueryEveryMemberFunction() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );

		    result = query.none( function( row, i, query ){
		    	return row.id > 10;
		    } );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can run in parallel" )
	@Test
	public void testQueryEveryParallel() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );

		    result = queryNone( query, function( row, i, query ){
		    	return row.id > 10;
		    }, true );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can run in parallel with max threads" )
	@Test
	public void testQueryEveryParallelWithMaxThreads() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );

		    result = queryNone( query, function( row, i, query ){
		    	return row.id > 10;
		    }, true, 2 );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can run in parallel with Virtual threads" )
	@Test
	public void testQueryEveryParallelWithVirtualThreads() {
		instance.executeSource(
		    """
		    query = QueryNew( "id,name", "integer,varchar" );
		    QueryAddRow( query, { id = 1, name = "John" } );
		    QueryAddRow( query, { id = 2, name = "Jane" } );
		    QueryAddRow( query, { id = 3, name = "Jim" } );
		    QueryAddRow( query, { id = 4, name = "Jill" } );
		    QueryAddRow( query, { id = 5, name = "Jack" } );

		    result = queryNone( query, function( row, i, query ){
		    	return row.id > 10;
		    }, true, true );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
	}
}
