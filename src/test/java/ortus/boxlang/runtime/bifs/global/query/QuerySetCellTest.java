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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

public class QuerySetCellTest {

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

	@DisplayName( "It should set the cell value" )
	@Test
	public void testGetCellValue() {

		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    result = querysetCell(query, "col2", 9000, 1);
		    """,
		    context );

		assertThat( variables.getAsQuery( result ).getCell( Key.of( "col2" ), 0 ) ).isEqualTo( 9000 );
	}

	@DisplayName( "It should work using member function" )
	@Test
	public void testGetCellValueUsingMemberFunction() {

		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    result = query.setCell("col2", 9000, 1);
		    """,
		    context );

		assertThat( variables.getAsQuery( result ).getCell( Key.of( "col2" ), 0 ) ).isEqualTo( 9000 );
	}

	@DisplayName( "It defaults to last row" )
	@Test
	public void testGetCellValueDefaultsToLastRow() {

		instance.executeSource(
		    """
		    query = queryNew("col1,col2","string,integer");
		    queryAddRow(query, {col1: "foo", col2: 42 });
		    query.setCell("col2", 9000);
		    queryAddRow(query, {col1: "bar", col2: 42 });
		    result = query.setCell("col2",9001);
		    """,
		    context );

		assertThat( variables.getAsQuery( result ).getCell( Key.of( "col2" ), 0 ) ).isEqualTo( 9000 );
		assertThat( variables.getAsQuery( result ).getCell( Key.of( "col2" ), 1 ) ).isEqualTo( 9001 );
	}

	@DisplayName( "It Set a cell with a specified row" )
	@Test
	public void testWillSetCellOnSpecifiedRow() {

		instance.executeSource(
		    """
		    result = queryNew( "alpha", "varchar" );
		    queryAddRow( result, 3 );
		    querySetCell( result, "alpha", "a1", 1 );
		    querySetCell( result, "alpha", "a2", 2 );
		    querySetCell( result, "alpha", "a3", 3 );
		    alpha1 = result.alpha[ 1 ];
		    alpha2 = result.alpha[ 2 ];
		    alpha3 = result.alpha[ 3 ];
		      """,
		    context );

		assertThat( variables.getAsQuery( result ).getData().size() ).isEqualTo( 3 );
		assertEquals( "a1", variables.getAsString( Key.of( "alpha1" ) ) );
		assertEquals( "a2", variables.getAsString( Key.of( "alpha2" ) ) );
		assertEquals( "a3", variables.getAsString( Key.of( "alpha3" ) ) );
	}

	@DisplayName( "It throws on invalid value type" )
	@Test
	public void testBitTypeThrow() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = queryNew( "myBit", "bit" )
		    	queryAddRow( result, 1 );
		    	querySetCell( result, "myBit", "c1", 1 );
		    """, context ) );
		assertDoesNotThrow( () -> instance.executeSource(
		    """
		    result = queryNew( "myBit", "bit" )
		    	queryAddRow( result, 1 );
		    	querySetCell( result, "myBit", 0, 1 );
		    """, context ) );
	}

	@DisplayName( "It casts to correct column type" )
	@Test
	public void testCellCasting() {
		instance.executeSource(
		    """
		    result = queryNew( "myBitColumn", "bit" );
		    queryAddRow( result, 3 );
		    querySetCell( result, "myBitColumn", "1", 1 );
		    querySetCell( result, "myBitColumn", 0, 2 );
		    querySetCell( result, "myBitColumn", true, 3 );
		    bit1 = result.myBitColumn[ 1 ];
		    bit2 = result.myBitColumn[ 2 ];
		    bit3 = result.myBitColumn[ 3 ];
		    	""",
		    context );

		assertThat( variables.getAsQuery( result ).getData().size() ).isEqualTo( 3 );
		assertEquals( 1, variables.get( Key.of( "bit1" ) ) );
		assertEquals( 0, variables.get( Key.of( "bit2" ) ) );
		assertEquals( 1, variables.get( Key.of( "bit3" ) ) );
	}

}
