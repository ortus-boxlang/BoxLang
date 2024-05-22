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

package ortus.boxlang.runtime.bifs.global.conversion;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;

public class JSONDeserializeTest {

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

	@DisplayName( "It can deserialize null" )
	@Test
	public void testCanSerializeNull() {
		instance.executeSource(
		    """
		    result = JSONDeserialize( "null" )
		         """,
		    context );
		assertThat( variables.get( result ) ).isNull();
	}

	@DisplayName( "It can deserialize a string" )
	@Test
	public void testCanSerializeString() {
		instance.executeSource(
		    """
		    result = JSONDeserialize( '"Hello World"' )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can deserialize a number" )
	@Test
	public void testCanSerializeNumber() {
		instance.executeSource(
		    """
		    result = JSONDeserialize( "42" )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can deserialize a boolean" )
	@Test
	public void testCanSerializeBoolean() {
		instance.executeSource(
		    """
		    result = JSONDeserialize( "false" )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "It can deserialize a string using a member function" )
	@Test
	public void testCanSerializeStringUsingMemberFunction() {
		instance.executeSource(
		    """
		    result = '{
		    	"one" : "wood",
		    	"two" : null,
		    	"three" : "42.1",
		    	"four" : [1,2,3],
		    	"five" : {},
		    	"six" : true
		    }'.jsonDeserialize()
		         """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		IStruct struct = variables.getAsStruct( result );
		assertThat( struct.size() ).isEqualTo( 6 );
		assertThat( struct.get( "one" ) ).isEqualTo( "wood" );
		assertThat( struct.get( "two" ) ).isNull();
		assertThat( struct.get( "three" ) ).isEqualTo( "42.1" );
		assertThat( struct.get( "four" ) ).isInstanceOf( Array.class );

		Array arr = struct.getAsArray( Key.of( "four" ) );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( 1 );
		assertThat( arr.get( 1 ) ).isEqualTo( 2 );
		assertThat( arr.get( 2 ) ).isEqualTo( 3 );

		assertThat( struct.get( "five" ) ).isInstanceOf( Struct.class );
		assertThat( struct.get( "six" ) ).isEqualTo( true );

	}

	@DisplayName( "It can deserialize a array" )
	@Test
	public void testCanSerializeArray() {
		instance.executeSource(
		    """
		    result = JSONDeserialize( '[1,"brad",true,[],null]' )
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array array = variables.getAsArray( result );
		assertThat( array.size() ).isEqualTo( 5 );
		assertThat( array.get( 0 ) ).isEqualTo( 1 );
		assertThat( array.get( 1 ) ).isEqualTo( "brad" );
		assertThat( array.get( 2 ) ).isEqualTo( true );
		assertThat( array.get( 3 ) ).isInstanceOf( Array.class );
		assertThat( array.get( 4 ) ).isNull();

	}

	@DisplayName( "It can deserialize a struct" )
	@Test
	public void testCanSerializeStruct() {
		instance.executeSource(
		    """
		       result = JSONDeserialize( '{
		    	"one" : "wood",
		    	"two" : null,
		    	"three" : "42.1",
		    	"four" : [1,2,3],
		    	"five" : {},
		    	"six" : true
		    }' )
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		IStruct struct = variables.getAsStruct( result );
		assertThat( struct.size() ).isEqualTo( 6 );
		assertThat( struct.get( "one" ) ).isEqualTo( "wood" );
		assertThat( struct.get( "two" ) ).isNull();
		assertThat( struct.get( "three" ) ).isEqualTo( "42.1" );
		assertThat( struct.get( "four" ) ).isInstanceOf( Array.class );

		Array arr = struct.getAsArray( Key.of( "four" ) );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( 1 );
		assertThat( arr.get( 1 ) ).isEqualTo( 2 );
		assertThat( arr.get( 2 ) ).isEqualTo( 3 );

		assertThat( struct.get( "five" ) ).isInstanceOf( Struct.class );
		assertThat( struct.get( "six" ) ).isEqualTo( true );

	}

	@DisplayName( "It can deserialize a query serialized as columns" )
	@Test
	public void testCanSerializeQuerySerializedAsColumns() {
		instance.executeSource(
		    """
		    queryAsJSON = JSONSerialize( queryNew(
		      "col1,col2,col3",
		      "numeric,varchar,bit",
		      [
		       [1,"brad",true],
		       [2,"wood",false]
		      ]
		     ), "column" )
		       result = JSONDeserialize( queryAsJSON, false )
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertThat( query.size() ).isEqualTo( 2 );
		assertThat( query.getColumns().size() ).isEqualTo( 3 );
		assertThat( query.getColumns().get( Key.of( "col1" ) ) ).isNotNull();
		assertThat( query.getColumns().get( Key.of( "col2" ) ) ).isNotNull();
		assertThat( query.getColumns().get( Key.of( "col3" ) ) ).isNotNull();
		assertThat( query.getColumns().get( Key.of( "col1" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( query.getColumns().get( Key.of( "col2" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( query.getColumns().get( Key.of( "col3" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( query.getCell( Key.of( "col1" ), 0 ) ).isEqualTo( 1 );
		assertThat( query.getCell( Key.of( "col2" ), 0 ) ).isEqualTo( "brad" );
		assertThat( query.getCell( Key.of( "col3" ), 0 ) ).isEqualTo( true );
		assertThat( query.getCell( Key.of( "col1" ), 1 ) ).isEqualTo( 2 );
		assertThat( query.getCell( Key.of( "col2" ), 1 ) ).isEqualTo( "wood" );
		assertThat( query.getCell( Key.of( "col3" ), 1 ) ).isEqualTo( false );
	}

	@DisplayName( "It can deserialize a query serialized as row" )
	@Test
	public void testCanSerializeQuerySerializedAsRow() {
		instance.executeSource(
		    """
		    queryAsJSON = JSONSerialize( queryNew(
		      "col1,col2,col3",
		      "numeric,varchar,bit",
		      [
		       [1,"brad",true],
		       [2,"wood",false]
		      ]
		     ), "row" )
		       result = JSONDeserialize( queryAsJSON, false )
		            """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertThat( query.size() ).isEqualTo( 2 );
		assertThat( query.getColumns().size() ).isEqualTo( 3 );
		assertThat( query.getColumns().get( Key.of( "col1" ) ) ).isNotNull();
		assertThat( query.getColumns().get( Key.of( "col2" ) ) ).isNotNull();
		assertThat( query.getColumns().get( Key.of( "col3" ) ) ).isNotNull();
		assertThat( query.getColumns().get( Key.of( "col1" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( query.getColumns().get( Key.of( "col2" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( query.getColumns().get( Key.of( "col3" ) ).getType() ).isEqualTo( QueryColumnType.OBJECT );
		assertThat( query.getCell( Key.of( "col1" ), 0 ) ).isEqualTo( 1 );
		assertThat( query.getCell( Key.of( "col2" ), 0 ) ).isEqualTo( "brad" );
		assertThat( query.getCell( Key.of( "col3" ), 0 ) ).isEqualTo( true );
		assertThat( query.getCell( Key.of( "col1" ), 1 ) ).isEqualTo( 2 );
		assertThat( query.getCell( Key.of( "col2" ), 1 ) ).isEqualTo( "wood" );
		assertThat( query.getCell( Key.of( "col3" ), 1 ) ).isEqualTo( false );
	}

}
