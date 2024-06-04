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

}
