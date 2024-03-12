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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class QueryColumnDataTest {

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

	@DisplayName( "It should return the query column data as an array" )
	@Test
	public void testReturnsQueryColumnData() {

		instance.executeSource(
		    """
		       query = queryNew("col1,col2","string,integer", [ {col1: "foo", col2: 42 }, {col1: "bar", col2: 38 } ]);
		    result = queryColumnData( query, "col1" )
		             """,
		    context );

		Array columnData = variables.getAsArray( result );

		assertThat( columnData.size() ).isEqualTo( 2 );
		assertThat( columnData.get( 0 ).toString() ).isEqualTo( "foo" );
		assertThat( columnData.get( 1 ).toString() ).isEqualTo( "bar" );
	}

	@DisplayName( "It throws an error if the column name does not exist" )
	@Test
	public void testThrowsErrorIfColumnNameDoesNotExist() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		          	query = queryNew("col1,col2","string,integer", [ {col1: "foo", col2: 42 }, {col1: "bar", col2: 38 } ]);
		        result = queryColumnData( query, "col3" )
		                 """,
		        context )
		);
	}

}
