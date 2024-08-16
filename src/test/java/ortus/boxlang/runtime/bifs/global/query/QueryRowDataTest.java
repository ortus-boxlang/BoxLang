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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class QueryRowDataTest {

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
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It should retrieve a row's data as a structure" )
	@Test
	public void testGetRowData() {
		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    queryAddRow(query, {col1: "bar", col2: 24 });
		    result = queryRowData(query, 2);
		    """,
		    context );

		assertThat( variables.getAsStruct( result ).get( "col1" ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( "col2" ) ).isEqualTo( 24 );
	}

	@DisplayName( "It should retrieve a row's data as a structure CF" )
	@Test
	public void testGetRowDataCF() {
		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    queryAddRow(query, {col1: "bar", col2: 24 });
		    result = queryGetRow(query, 2);
		    """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.getAsStruct( result ).get( "col1" ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( "col2" ) ).isEqualTo( 24 );
	}

	@DisplayName( "It should work with member function" )
	@Test
	public void testGetRowDataUsingMemberFunction() {
		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    queryAddRow(query, {col1: "bar", col2: 24 });
		    result = query.rowData(2);
		    """,
		    context );

		assertThat( variables.getAsStruct( result ).get( "col1" ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( "col2" ) ).isEqualTo( 24 );
	}

	@DisplayName( "It should work with member function CF" )
	@Test
	public void testGetRowDataUsingMemberFunctionCF() {
		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    queryAddRow(query, {col1: "bar", col2: 24 });
		    result = query.getRow(2);
		    """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.getAsStruct( result ).get( "col1" ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( "col2" ) ).isEqualTo( 24 );
	}

	@DisplayName( "It should throw an exception if row number is negative" )
	@Test
	public void testGetRowDataNegative() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        query = queryNew("col1,col2","string,integer");
		        queryAddRow(query, {col1: "foo", col2: 42 });
		           result = queryRowData(query, -1);
		        """,
		        context )
		);
	}

	@DisplayName( "It should throw an exception if row number is out of bounds" )
	@Test
	public void testGetRowDataOutOfBounds() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        query = queryNew("col1,col2","string,integer");
		        queryAddRow(query, {col1: "foo", col2: 42 });
		           result = queryRowData(query, 2);
		        """,
		        context )
		);
	}
}
