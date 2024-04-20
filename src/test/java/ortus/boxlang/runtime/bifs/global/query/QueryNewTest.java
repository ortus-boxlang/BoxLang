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
import ortus.boxlang.runtime.types.QueryColumnType;

public class QueryNewTest {

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

	@DisplayName( "It can create new" )
	@Test
	public void testCanCreateNew() {

		instance.executeSource(
		    """
		    result = queryNew("");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );

		instance.executeSource(
		    """
		       result = queryNew("col1,col2","string,integer");
		    columnList = result.columnList;
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( "columnList" ) ).isEqualTo( "col1,col2" );
	}

	@DisplayName( "It can create new with no type" )
	@Test
	public void testCreateNewWithNoTypes() {

		instance.executeSource(
		    """
		    result = queryNew("directory,name,type");
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		var cols = variables.getAsQuery( result ).getColumns();
		assertThat( cols.get( Key.of( "directory" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( cols.get( Key.of( "name" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( cols.get( Key.of( "type" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );

	}

	@DisplayName( "It can create new with simple array data" )
	@Test
	public void testCreateNewWithSimpleArrayData() {

		instance.executeSource(
		    """
		       result = queryNew("col1,col2","string,integer", [ "foo", 42 ]);
		    columnList = result.columnList;
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( "columnList" ) ).isEqualTo( "col1,col2" );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can create new with struct data" )
	@Test
	public void testCreateNewWithStructData() {

		instance.executeSource(
		    """
		       result = queryNew("col1,col2","string,integer", {col1: "foo", col2: 42 });
		    columnList = result.columnList;
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( "columnList" ) ).isEqualTo( "col1,col2" );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 1 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can create new with struct data as first arg" )
	@Test
	public void testCreateNewWithStructDataAsFirstArg() {
		// ACF does this
		instance.executeSource(
		    """
		         result = queryNew([
		    	["id": 10, "label": "ten"],
		    	["id": 20, "label": "twenty"]
		    ]);;
		      columnList = result.columnList;
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( "columnList" ) ).isEqualTo( "id,label" );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.get( Key.of( "id" ) ) ).isEqualTo( 10 );
		assertThat( row.get( Key.of( "label" ) ) ).isEqualTo( "ten" );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.get( Key.of( "id" ) ) ).isEqualTo( 20 );
		assertThat( row.get( Key.of( "label" ) ) ).isEqualTo( "twenty" );
	}

	@DisplayName( "It can create new with array of structs data" )
	@Test
	public void testCreateNewWithArrayOfStructsData() {

		instance.executeSource(
		    """
		       result = queryNew("col1,col2","string,integer", [
		    	{col1: "foo", col2: 42 },
		    	{col1: "bar", col2: 100 }
		    ]);
		       columnList = result.columnList;
		          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( "columnList" ) ).isEqualTo( "col1,col2" );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "bar" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 100 );
	}

	@DisplayName( "It can create new with array of arrays data" )
	@Test
	public void testCreateNewWithArrayOfArraysData() {

		instance.executeSource(
		    """
		       result = queryNew("col1,col2","string,integer", [
		    	["foo", 42 ],
		    	[ "bar", 100 ]
		    ]);
		       columnList = result.columnList;
		          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		assertThat( variables.get( "columnList" ) ).isEqualTo( "col1,col2" );
		Query qry = variables.getAsQuery( result );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct row = qry.getRowAsStruct( 0 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "foo" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 42 );
		row = qry.getRowAsStruct( 1 );
		assertThat( row.getAsString( Key.of( "col1" ) ) ).isEqualTo( "bar" );
		assertThat( row.getAsInteger( Key.of( "col2" ) ) ).isEqualTo( 100 );
	}
}
