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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

public class QueryAddRowTest {

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

	@DisplayName( "It can add rows with simple array data" )
	@Test
	public void testAddRowsWithSimpleArrayData() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = queryAddRow(result, [ "foo", 42 ]);
		          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 1 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can add rows with struct data" )
	@Test
	public void testAddRowsWithStructData() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = queryAddRow(result, {col1: "foo", col2: 42 });
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 1 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can add rows with array of structs data" )
	@Test
	public void testAddRowsWithArrayOfStructsData() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = queryAddRow(result, [
		      	{col1: "foo", col2: 42 },
		      	{col1: "bar", col2: 100 }
		      ]);
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 2 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "bar" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 100 );
	}

	@DisplayName( "It can add rows with array of arrays data" )
	@Test
	public void testAddRowsWithArrayOfArraysData() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = queryAddRow(result, [
		      	["foo", 42 ],
		      	[ "bar", 100 ]
		      ]);
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 2 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "bar" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 100 );
	}

	@DisplayName( "It can add number of rows" )
	@Test
	public void testAddNumberOfRows() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = queryAddRow(result,25);
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 25 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 25 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( null );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( null );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( null );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( null );
		row = qry.getRowAsStruct( 24 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( null );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( null );
	}

	@DisplayName( "It can add rows with simple array data Member" )
	@Test
	public void testAddRowsWithSimpleArrayDataMember() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = result.addRow( [ "foo", 42 ]);
		          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 1 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can add rows with struct data Member" )
	@Test
	public void testAddRowsWithStructDataMember() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = result.addRow( {col1: "foo", col2: 42 });
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 1 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can add rows with array of structs data Member" )
	@Test
	public void testAddRowsWithArrayOfStructsDataMember() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = result.addRow( [
		      	{col1: "foo", col2: 42 },
		      	{col1: "bar", col2: 100 }
		      ]);
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 2 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "bar" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 100 );
	}

	@DisplayName( "It can add rows with array of arrays data Member" )
	@Test
	public void testAddRowsWithArrayOfArraysDataMember() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = result.addRow( [
		      	["foo", 42 ],
		      	[ "bar", 100 ]
		      ]);
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 2 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "bar" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 100 );
	}

	@DisplayName( "It can add number of rows Member" )
	@Test
	public void testAddNumberOfRowsMember() {

		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = result.addRow(25);
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 25 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 25 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( null );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( null );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( null );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( null );
		row = qry.getRowAsStruct( 24 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( null );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( null );
	}

	@DisplayName( "It can add empty row in Query" )
	@Test
	public void testAddEmptyRow() {
		instance.executeSource(
		    """
		    result = queryNew("col1,col2","string,integer");
		    lastRow = result.addRow();
		    recordCount = result.recordCount;
		    	  """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( Key.of( "lastRow" ) ) ).isEqualTo( 1 );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "recordCount" ) ) ).isEqualTo( 1 );
	}

	@Disabled( "This test is disabled as you go down a rabbit hole, we need to discuss this further" )
	@DisplayName( "It can validate cell types" )
	@Test
	public void testValidateCellTypes() {
		// @formatter:off
		assertThrows( RuntimeException.class, () -> instance.executeSource( """
			myQuery = queryNew( "title,pageLength,createdDate", "string,integer,date" )
			myQuery.addRow(  [ "The Fellowship Of the Ring", "not_an_integer", "not_a_date" ] )
			println( myQuery )
		""", context ) );
		// @formatter:on
	}

	@DisplayName( "It casts string values to integer when adding rows via array" )
	@Test
	public void testCastsStringToIntegerArray() {
		// @formatter:off
		instance.executeSource( """
			result = queryNew( "amount", "integer" )
			queryAddRow( result, [["1500"],["2500"]] )
		""", context );
		// @formatter:on
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		assertThat( qry.getCell( Key.of( "amount" ), 0 ) ).isInstanceOf( Integer.class );
		assertThat( qry.getCell( Key.of( "amount" ), 0 ) ).isEqualTo( 1500 );
		assertThat( qry.getCell( Key.of( "amount" ), 1 ) ).isEqualTo( 2500 );
	}

	@DisplayName( "It casts string values to integer when adding rows via struct" )
	@Test
	public void testCastsStringToIntegerStruct() {
		// @formatter:off
		instance.executeSource( """
			result = queryNew( "id,amount", "integer,double" )
			queryAddRow( result, { id: "42", amount: "99.99" } )
		""", context );
		// @formatter:on
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		assertThat( qry.getCell( Key.of( "id" ), 0 ) ).isInstanceOf( Integer.class );
		assertThat( qry.getCell( Key.of( "id" ), 0 ) ).isEqualTo( 42 );
		assertThat( qry.getCell( Key.of( "amount" ), 0 ) ).isInstanceOf( Double.class );
		assertThat( qry.getCell( Key.of( "amount" ), 0 ) ).isEqualTo( 99.99 );
	}

	@DisplayName( "It casts values added via addRow so QoQ math works" )
	@Test
	public void testCastingEnablesQoQMathViaAddRow() {
		// @formatter:off
		instance.executeSource( """
			myQry = queryNew( "amount", "integer" )
			queryAddRow( myQry, [["1500"]] )
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

	@DisplayName( "It casts values added via member function addRow" )
	@Test
	public void testCastingViaMemberFunction() {
		// @formatter:off
		instance.executeSource( """
			result = queryNew( "price", "double" )
			result.addRow( { price: "45.67" } )
		""", context );
		// @formatter:on
		Query qry = variables.getAsQuery( result );
		assertThat( qry.getCell( Key.of( "price" ), 0 ) ).isInstanceOf( Double.class );
		assertThat( qry.getCell( Key.of( "price" ), 0 ) ).isEqualTo( 45.67 );
	}

}
