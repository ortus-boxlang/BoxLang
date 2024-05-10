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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class QuerySliceTest {

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

	@DisplayName( "It should slice the query from the offset and return a new query" )
	@Test
	public void testSliceQueryFromOffset() {
		instance.executeSource(
		    """
		       query = QueryNew( "name", "varchar" );
		       QueryAddRow( query, { name = "John" } );
		       QueryAddRow( query, { name = "Jane" } );
		       QueryAddRow( query, { name = "Jim" } );
		       QueryAddRow( query, { name = "Jill" } );
		       QueryAddRow( query, { name = "Jack" } );

		       result = QuerySlice( query, 1, 2 );
		    name1 = result.name[1];
		    name2 = result.name[2];
		       recordCount = result.recordCount;
		       """,
		    context );

		assertInstanceOf( Query.class, variables.get( result ) );
		assertThat( variables.get( Key.of( "name1" ) ) ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "name2" ) ) ).isEqualTo( "Jane" );
		assertThat( variables.get( Key.of( "recordCount" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "It should allow a zero or negative offset" )
	@Test
	public void testSliceQueryWithZeroOrNegativeOffset() {
		instance.executeSource(
		    """
		       query = QueryNew( "name", "varchar" );
		       QueryAddRow( query, { name = "John" } );
		       QueryAddRow( query, { name = "Jane" } );
		       QueryAddRow( query, { name = "Jim" } );
		       QueryAddRow( query, { name = "Jill" } );
		       QueryAddRow( query, { name = "Jack" } );

		       result = QuerySlice( query, -1, 2 );
		    name1 = result.name[1];
		    name2 = result.name[2];
		       recordCount = result.recordCount;
		       """,
		    context );

		assertThat( variables.get( Key.of( "name1" ) ) ).isEqualTo( "Jill" );
		assertThat( variables.get( Key.of( "name2" ) ) ).isEqualTo( "Jack" );
	}

	@DisplayName( "It should return all remaining rows when not providing a length" )
	@Test
	public void testSliceQueryWithoutLength() {
		instance.executeSource(
		    """
		       query = QueryNew( "name", "varchar" );
		       QueryAddRow( query, { name = "John" } );
		       QueryAddRow( query, { name = "Jane" } );
		       QueryAddRow( query, { name = "Jim" } );
		       QueryAddRow( query, { name = "Jill" } );
		       QueryAddRow( query, { name = "Jack" } );

		       result = QuerySlice( query, 3 );
		    name1 = result.name[2];
		    name2 = result.name[3];
		       recordCount = result.recordCount;
		       """,
		    context );

		assertThat( variables.get( Key.of( "name1" ) ) ).isEqualTo( "Jill" );
		assertThat( variables.get( Key.of( "name2" ) ) ).isEqualTo( "Jack" );
		assertThat( variables.get( Key.of( "recordCount" ) ) ).isEqualTo( 3 );
	}

	@DisplayName( "It should throw an exception if the offset plus length is greater than the record count" )
	@Test
	public void testSliceQueryThrowsExceptionIfOffsetPlusLengthGreaterThanRecordCount() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		           query = QueryNew( "name", "varchar" );
		           QueryAddRow( query, { name = "John" } );
		           QueryAddRow( query, { name = "Jane" } );
		           QueryAddRow( query, { name = "Jim" } );
		           QueryAddRow( query, { name = "Jill" } );
		           QueryAddRow( query, { name = "Jack" } );

		           result = QuerySlice( query, 3, 4 );
		        name1 = result.name[1];
		        name2 = result.name[2];
		           recordCount = result.recordCount;
		        """,
		        context )
		);
	}
}
