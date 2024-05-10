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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class QuerySetRowTest {

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

	@DisplayName( "It should change the values of the row when passing in a struct" )
	@Test
	public void testSetRowWithStruct() {
		instance.executeSource(
		    """
		    query = QueryNew( "name", "varchar" );
		    QueryAddRow( query, { name = "John" } );
		    QueryAddRow( query, { name = "Jane" } );
		    QueryAddRow( query, { name = "Jim" } );
		    QueryAddRow( query, { name = "Jill" } );
		    QueryAddRow( query, { name = "Jack" } );

		    QuerySetRow( query, 3, { name = "Jill" } );
		    result = query.name[3];
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Jill" );
	}

	@DisplayName( "It should change the values of the row when passing in an array" )
	@Test
	public void testSetRowWithArray() {
		instance.executeSource(
		    """
		    query = QueryNew( "name", "varchar" );
		    QueryAddRow( query, { name = "John" } );
		    QueryAddRow( query, { name = "Jane" } );
		    QueryAddRow( query, { name = "Jim" } );
		    QueryAddRow( query, { name = "Jill" } );
		    QueryAddRow( query, { name = "Jack" } );

		    QuerySetRow( query, 3, [ "Hill" ] );
		    result = query.name[3];
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Hill" );
	}

	@DisplayName( "It should work with member function" )
	@Test
	public void testSetRowUsingMemberFunction() {
		instance.executeSource(
		    """
		    query = QueryNew( "name", "varchar" );
		    query.addRow( { name = "John" } );
		    query.addRow( { name = "Jane" } );
		    query.addRow( { name = "Jim" } );
		    query.addRow( { name = "Jill" } );
		    query.addRow( { name = "Jack" } );

		    query.setRow( 3, { name = "Jill" } );
		    result = query.name[3];
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Jill" );
	}

	@DisplayName( "It should only change the data we pass in" )
	@Test
	public void testSetRowOnlyChangesDataPassedIn() {
		instance.executeSource(
		    """
		    query = QueryNew( "name,age", "varchar,integer" );
		    QueryAddRow( query, { name = "John", age = 30 } );
		    QueryAddRow( query, { name = "Jane", age = 25 } );
		    QueryAddRow( query, { name = "Jim", age = 35 } );
		    QueryAddRow( query, { name = "Jill", age = 28 } );
		    QueryAddRow( query, { name = "Jack", age = 32 } );

		    QuerySetRow( query, 1, { age = 31 } );
		    name = query.name[1];
		    age = query.age[1];
		    """,
		    context );

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 31 );
	}

	@DisplayName( "It should append row if the row number is zero or negative" )
	@Test
	public void testSetRowAppendsRowIfZeroOrNegative() {
		instance.executeSource(
		    """
		    query = QueryNew( "name", "varchar" );
		    QueryAddRow( query, { name = "John" } );
		    QueryAddRow( query, { name = "Jane" } );
		    QueryAddRow( query, { name = "Jim" } );
		    QueryAddRow( query, { name = "Jill" } );
		    QueryAddRow( query, { name = "Jack" } );

		    QuerySetRow( query, 0, { name = "Hill" } );
		    QuerySetRow( query, -1, { name = "Bill" } );
		    result1 = query.name[6];
		    result2 = query.name[7];
		    """,
		    context );

		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "Hill" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "Bill" );
	}

	@DisplayName( "It should throw an exception if we pass it a silly data type" )
	@Test
	public void testSetRowThrowsExceptionIfSillyDataType() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        query = QueryNew( "name", "varchar" );
		        QueryAddRow( query, { name = "John" } );
		        QuerySetRow( query, 1, 42 );
		        """,
		        context )
		);
	}
}
