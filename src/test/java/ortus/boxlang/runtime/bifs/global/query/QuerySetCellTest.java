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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.TimeCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Query;
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
		assertEquals( true, variables.get( Key.of( "bit1" ) ) );
		assertEquals( false, variables.get( Key.of( "bit2" ) ) );
		assertEquals( true, variables.get( Key.of( "bit3" ) ) );
	}

	// BL-640 - Test that time values in queries are handled correctly and allow for comparison
	// If a DateTime object is used in a query, it should be able to be compared to another DateTime object
	@DisplayName( "It tests the BIF DateDiff with a date within a query" )
	@Test
	public void testQueryDate() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00.100Z" ) );
		variables.put( "date2", new DateTime( "2024-01-21T00:00:00.100Z" ) );
		instance.executeSource(
		    """
		    	q = querynew( "id,created", "integer,timestamp" );
		    	queryAddRow( q );
		    	querySetCell( q, "id", 789 );
		    	querySetCell(
		    		q,
		    		"created",
		    		date1
		    	);
		    	result = abs(DateDiff('d', q.created, date2));
		    """,
		    context
		);

		Integer result = variables.getAsDouble( Key.of( "result" ) ).intValue();
		assertEquals( result, 1 );

		variables.put( "time1", TimeCaster.cast( "22:00:00" ) );
		variables.put( "time2", TimeCaster.cast( "23:00:00" ) );
		instance.executeSource(
		    """
		    	q = querynew( "id,created", "integer,time" );
		    	queryAddRow( q );
		    	querySetCell( q, "id", 789 );
		    	querySetCell(
		    		q,
		    		"created",
		    		time1
		    	);
		    	result = abs(DateDiff('h', q.created, time2));
		    """,
		    context
		);

		result = variables.getAsDouble( Key.of( "result" ) ).intValue();
		assertEquals( result, 1 );

	}

	@DisplayName( "queryNullIsString: False by default" )
	@Test
	public void testQueryNullIsStringDefault() {
		assertThat( Query.queryNullToEmpty ).isEqualTo( false );
	}

	@DisplayName( "queryNullIsString: Coerces empty strings to nulls on non-string columns" )
	@Test
	public void testQueryNullIsStringCompatSetting() {
		try {
			Query.queryNullToEmpty = true;
			// @formatter:off
			instance.executeSource(
			    """
			    result = queryNew("name,createdDate","string,date");
			    queryAddRow(result);
			    querySetCell(result, "name", "", 1);
			    querySetCell(result, "createdDate", "", 1);
			    """,
			    context
			);
			// @formatter:on

			Query query = variables.getAsQuery( result );
			assertThat( query.getCell( Key.of( "name" ), 0 ) ).isEqualTo( "" );
			assertThat( query.getCell( Key.of( "createdDate" ), 0 ) ).isEqualTo( null );
		} finally {
			Query.queryNullToEmpty = false;
		}
	}

	@DisplayName( "It should trim whitespace from column names" )
	@Test
	public void testColumnNameTrimming() {
		instance.executeSource(
		    """
		    myQry = querynew( "col1", "varchar", [[""]] );
		    myQry.setCell( " col1 ", "value" );
		    result = myQry.getCell( " col1 ", 1 );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "value" );
	}

	@DisplayName( "It should trim whitespace from column names across query BIFs" )
	@Test
	public void testColumnNameTrimmingAcrossBIFs() {
		instance.executeSource(
		    """
		    myQry = querynew( "col1", "varchar", [[""]] );

		    // setCell with padded column name
		    querySetCell( myQry, " col1 ", "trimmed", 1 );

		    // getCell with padded column name
		    cellVal = queryGetCell( myQry, " col1 ", 1 );

		    // columnExists with padded column name
		    exists = queryColumnExists( myQry, " col1 " );

		    // columnData with padded column name
		    data = myQry.columnData( " col1 " );

		    // addColumn with padded column name
		    queryAddColumn( myQry, " col2 ", "varchar", ["test"] );
		    col2Exists = queryColumnExists( myQry, "col2" );

		    // deleteColumn with padded column name
		    queryDeleteColumn( myQry, " col2 " );
		    col2Gone = !queryColumnExists( myQry, "col2" );
		    """,
		    context );

		assertThat( variables.get( Key.of( "cellVal" ) ) ).isEqualTo( "trimmed" );
		assertThat( variables.get( Key.of( "exists" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "col2Exists" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "col2Gone" ) ) ).isEqualTo( true );
	}

	@DisplayName( "It casts string values to integer column type via querySetCell" )
	@Test
	public void testCastsStringToInteger() {
		// @formatter:off
		instance.executeSource( """
			result = queryNew( "amount", "integer", [[0]] )
			querySetCell( result, "amount", "1500", 1 )
		""", context );
		// @formatter:on
		Query qry = variables.getAsQuery( result );
		assertThat( qry.getCell( Key.of( "amount" ), 0 ) ).isInstanceOf( Integer.class );
		assertThat( qry.getCell( Key.of( "amount" ), 0 ) ).isEqualTo( 1500 );
	}

	@DisplayName( "It casts string values to double column type via querySetCell" )
	@Test
	public void testCastsStringToDouble() {
		// @formatter:off
		instance.executeSource( """
			result = queryNew( "price", "double", [[0]] )
			querySetCell( result, "price", "19.99", 1 )
		""", context );
		// @formatter:on
		Query qry = variables.getAsQuery( result );
		assertThat( qry.getCell( Key.of( "price" ), 0 ) ).isInstanceOf( Double.class );
		assertThat( qry.getCell( Key.of( "price" ), 0 ) ).isEqualTo( 19.99 );
	}

	@DisplayName( "It casts values set via querySetCell so QoQ math works" )
	@Test
	public void testCastingEnablesQoQMathViaSetCell() {
		// @formatter:off
		instance.executeSource( """
			myQry = queryNew( "amount", "integer", [[0]] )
			querySetCell( myQry, "amount", "1500", 1 )
			result = queryExecute(
				"SELECT amount/100 as calc FROM myQry",
				[],
				{ dbType: "query" }
			)
		""", context );
		// @formatter:on
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		assertThat( ( ( Number ) qry.getCell( Key.of( "calc" ), 0 ) ).intValue() ).isEqualTo( 15 );
	}

}
