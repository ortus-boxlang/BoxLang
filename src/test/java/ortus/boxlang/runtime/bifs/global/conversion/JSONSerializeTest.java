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

public class JSONSerializeTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It can serialize null" )
	@Test
	public void testCanSerializeNull() {
		instance.executeSource(
		    """
		    result = JSONSerialize( null )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "null" );
	}

	@DisplayName( "It can serialize a string" )
	@Test
	public void testCanSerializeString() {
		instance.executeSource(
		    """
		    result = JSONSerialize( "Hello World" )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "\"Hello World\"" );
	}

	@DisplayName( "It can serialize a number" )
	@Test
	public void testCanSerializeNumber() {
		instance.executeSource(
		    """
		    result = JSONSerialize( 42 )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "42" );
	}

	@DisplayName( "It can serialize a boolean" )
	@Test
	public void testCanSerializeBoolean() {
		instance.executeSource(
		    """
		    result = JSONSerialize( false )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "false" );
	}

	@DisplayName( "It can serialize a array" )
	@Test
	public void testCanSerializeArray() {
		instance.executeSource(
		    """
		    result = JSONSerialize( [1,"brad",true,[],null] )
		         """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "[1,\"brad\",true,[],null]" );
	}

	@DisplayName( "It can serialize a struct" )
	@Test
	public void testCanSerializeStruct() {
		instance.executeSource(
		    """
		       result = JSONSerialize( [
		    	"one" : "wood",
		    	"two" : null,
		    	"three" : 42,
		    	"four" : [1,2,3],
		    	"five" : {},
		    	"six" : true
		    ] )
		            """,
		    context );
		String expected = """
		                  {
		                      "one" : "wood",
		                      "two" : null,
		                      "three" : 42,
		                      "four" : [1,2,3],
		                      "five" : {},
		                      "six" : true
		                  }
		                  """;
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query as array of structs" )
	@Test
	public void testCanSerializeQueryArrayOfStructs() {
		instance.executeSource(
		    """
		         result = JSONSerialize( queryNew(
		    "col1,col2,col3",
		    "numeric,varchar,bit",
		    [
		    	[1,"brad",true],
		    	[2,"wood",false]
		    ]
		      ), "struct" )
		              """,
		    context );
		String expected = """
		                  [
		                  	{"col3":true, "col2":"brad", "col1":1},
		                  	{"col3":false, "col2":"wood", "col1":2}
		                  ]
		                                                    """;
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query as row" )
	@Test
	public void testCanSerializeQueryRow() {
		instance.executeSource(
		    """
		         result = JSONSerialize( queryNew(
		    "col1,col2,col3",
		    "numeric,varchar,bit",
		    [
		    	[1,"brad",true],
		    	[2,"wood",false]
		    ]
		      ), "row" )
		              """,
		    context );
		String expected = """
		                               {
		                  "columns" : ["col1","col2","col3"],
		                  "data" : [
		                  	[1,"brad",true],
		                  	[2,"wood",false]
		                  ]
		                   }
		                                                                 """;
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query as column" )
	@Test
	public void testCanSerializeQueryColumn() {
		instance.executeSource(
		    """
		         result = JSONSerialize( queryNew(
		    "col1,col2,col3",
		    "numeric,varchar,bit",
		    [
		    	[1,"brad",true],
		    	[2,"wood",false]
		    ]
		      ), "column" )
		              """,
		    context );
		String expected = """
		                                                     {
		                           			"rowCount":2,
		                                        "columns" : ["col1","col2","col3"],
		                                        "data" :
		                  {
		                  	"col1":[ 1, 2 ],
		                  	"col2":["brad", "wood" ],
		                  	"col3":[ true, false ]
		                  }
		                                         }
		                                                                                       """;
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a string Member" )
	@Test
	public void testCanSerializeStringMember() {
		instance.executeSource(
		    """
		    result = "Hello World".toJSON()
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "\"Hello World\"" );
	}

	@DisplayName( "It can serialize a list Member" )
	@Test
	public void testCanSerializeListMember() {
		instance.executeSource(
		    """
		    result = "Hello,World".listToJSON()
		         """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "[\"Hello\",\"World\"]".replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a number Member" )
	@Test
	public void testCanSerializeNumberMember() {
		instance.executeSource(
		    """
		    result = (42).toJSON()
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "42" );
	}

	@DisplayName( "It can serialize a boolean Member" )
	@Test
	public void testCanSerializeBooleanMember() {
		instance.executeSource(
		    """
		    result = false.toJSON()
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "false" );
	}

	@DisplayName( "It can serialize a array Member" )
	@Test
	public void testCanSerializeArrayMember() {
		instance.executeSource(
		    """
		    result = [1,"brad",true,[],null].toJSON()
		         """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "[1,\"brad\",true,[],null]" );
	}

	@DisplayName( "It can serialize a struct Member" )
	@Test
	public void testCanSerializeStructMember() {
		instance.executeSource(
		    """
		       result = [
		    	"one" : "wood",
		    	"two" : null,
		    	"three" : 42,
		    	"four" : [1,2,3],
		    	"five" : {},
		    	"six" : true
		    ].toJSON()
		            """,
		    context );
		String expected = """
		                  {
		                      "one" : "wood",
		                      "two" : null,
		                      "three" : 42,
		                      "four" : [1,2,3],
		                      "five" : {},
		                      "six" : true
		                  }
		                  """;
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query Member" )
	@Test
	public void testCanSerializeQueryMember() {
		instance.executeSource(
		    """
		         result = queryNew(
		    "col1,col2,col3",
		    "numeric,varchar,bit",
		    [
		    	[1,"brad",true],
		    	[2,"wood",false]
		    ]
		      ).toJSON( "struct")
		              """,
		    context );
		String expected = """
		                  [
		                  	{"col3":true, "col2":"brad", "col1":1},
		                  	{"col3":false, "col2":"wood", "col1":2}
		                  ]
		                                                    """;
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

}
