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

public class QueryAddColumnTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It should add a column to the query" )
	@Test
	public void testAddColumn() {
		instance.executeSource(
		    """
		       query = QueryNew( "name", "varchar" );
		       QueryAddRow( query, { name = "John" } );
		       QueryAddRow( query, { name = "Jane" } );

		       result = QueryAddColumn( query, "age", "integer", [ 30, 25 ] );
		       result1 = query.age[1];
		    result2 = query.age[2]
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 30 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 25 );
	}

	@DisplayName( "It should add row and set column value when passing in an array to an empty query" )
	@Test
	public void testAddColumnWithEmptyQuery() {
		instance.executeSource(
		    """
		       query = QueryNew( "name", "varchar" );
		       result = QueryAddColumn( query, "age", "integer", [ 30, 25 ] );
		       result1 = query.age[1];
		    result2 = query.age[2];
		    result3 = query.name[1];
		    recordCount = query.recordCount;
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 30 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 25 );
		assertThat( variables.get( Key.of( "result3" ) ) ).isNull();
		assertThat( variables.get( Key.of( "recordCount" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "It should add column to empty query but not add a row when passing in an empty array" )
	@Test
	public void testAddColumnWithEmptyArray() {
		instance.executeSource(
		    """
		       query = QueryNew( "name", "varchar" );
		       result = QueryAddColumn( query, "age", "integer", [] );
		    recordCount = query.recordCount;
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "recordCount" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "It should throw an error when we try adding a column that already exists" )
	@Test
	public void testAddColumnThatAlreadyExists() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        query = QueryNew( "name", "varchar" );
		        QueryAddRow( query, { name = "John" } );
		        QueryAddRow( query, { name = "Jane" } );

		        QueryAddColumn( query, "name", "varchar", [ "John", "Jane" ] );
		        """,
		        context
		    )
		);
	}

	@DisplayName( "It should throw an error when passing an invalid column type" )
	@Test
	public void testAddColumnWithInvalidColumnType() {
		assertThrows(
		    RuntimeException.class,
		    () -> instance.executeSource(
		        """
		        query = QueryNew( "name", "varchar" );
		        QueryAddRow( query, { name = "John" } );
		        QueryAddRow( query, { name = "Jane" } );

		        QueryAddColumn( query, "age", "invalid", [ 30, 25 ] );
		        """,
		        context
		    )
		);
	}

	@DisplayName( "Add a column by using the default data type an an array for the third argument" )
	@Test
	public void testAddColumnWithDefaultDataType() {
		// @formatter:off
		instance.executeSource(
		    """
		      	query = QueryNew( "name", "varchar" );
		      	result = QueryAddColumn( query, "age", [ 30, 25 ] );
		      	result1 = query.age[1];
		    	result2 = query.age[2];
		    """,
		    context );
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 30 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 25 );
	}

	@DisplayName( "Add a column by using the default data type and no array " )
	@Test
	public void testAddColumnWithDefaultDataTypeAndNoArray() {
		// @formatter:off
		instance.executeSource(
		    """
		      	query = QueryNew( "name", "varchar" );
		      	result = QueryAddColumn( query, "age" );
		      	result1 = query.age[1];
		    	result2 = query.age[2];
		    """,
		    context );
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "" );
	}

	@DisplayName( "Add a complex column for complex data" )
	@Test
	public void testAddColumnWithComplexData() {
		// @formatter:off
		instance.executeSource(
		    """
				result = QueryNew( "id", "varchar" );
				queryAddRow( result );
				queryAddColumn( result, "complex", [] );
				querySetCell( result, "id", "example1" );
				querySetCell( result, "complex", {"liftoff": [10,9,8]} );

				println( result );
		    """,
		    context );
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ) ).isNotNull();
		assertThat( variables.get( Key.of( "result" ) ).toString() ).contains( "liftoff" );

	}

}
