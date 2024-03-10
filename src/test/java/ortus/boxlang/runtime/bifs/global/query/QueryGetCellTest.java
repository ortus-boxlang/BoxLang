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

public class QueryGetCellTest {

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

	@DisplayName( "It should get the cell value" )
	@Test
	public void testGetCellValue() {

		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, queryAddRow(query, {col1: "foo", col2: 42 }));
		    result = queryGetCell(query, "col2", 1);
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "It should work using member function" )
	@Test
	public void testGetCellValueUsingMemberFunction() {

		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, queryAddRow(query, {col1: "foo", col2: 42 }));
		    result = query.getCell("col2", 1);
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

}
