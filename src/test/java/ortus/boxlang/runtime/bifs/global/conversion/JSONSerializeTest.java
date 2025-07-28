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
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
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

	@DisplayName( "It can serialize a BoxLang DateTime" )
	@Test
	public void testCanSerializeDateTimeObject() {
		instance.executeSource(
		    """
		    	setTimezone( "UTC" );
		       	result = JSONSerialize( createDate( 2024, 1, 1 ) )
		    """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "\"2024-01-01T00:00:00Z\"" );
	}

	@DisplayName( "It can serialize using the BoxLang DateTime methods" )
	@Test
	public void testCanSerializeDateTimeUsingMethods() {
		// @formatter:off
		instance.executeSource(
		    """
				import java.time.LocalDate;
				import java.sql.Date;

		       	result = jsonSerialize( {
					localDate: LocalDate.now(),
					sqlDate: Date.valueOf( "2024-01-01" )
				} )
				println( result )
		    """,
		    context );
		// @formatter:on

	}

	@DisplayName( "It can serialize a struct" )
	@Test
	public void testCanSerializeStruct() {
		// @formatter:off
		instance.executeSource(
		    """
				setTimezone( "UTC" );
				result = JSONSerialize( [
					"one" : "wood",
					"two" : null,
					"three" : 42,
					"four" : [1,2,3],
					"five" : {},
					"six" : true,
					"date" : createDate( 2024, 1, 1 )
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
				"six" : true,
				"date" : "2024-01-01T00:00:00Z"
			}
			""";
		// @formatter:on

		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query as array of structs" )
	@Test
	public void testCanSerializeQueryArrayOfStructs() {
		// @formatter:off
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
			{ "col1": 1, "col2": "brad", "col3": true},
			{ "col1": 2, "col2": "wood", "col3": false }
			]
		""";
		// @formatter:on
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query as row" )
	@Test
	public void testCanSerializeQueryRow() {
		// @formatter:off
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
		// @formatter:on
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query as column" )
	@Test
	public void testCanSerializeQueryColumn() {
		// @formatter:off
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

		// @formatter:on
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
		// @formatter:off
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
		// @formatter:on
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a query Member" )
	@Test
	public void testCanSerializeQueryMember() {
		// @formatter:off
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
				{"col1":1, "col2":"brad", "col3":true},
				{"col1":2, "col2":"wood", "col3":false}
			]
		""";
		// @formatter:on
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );
	}

	@DisplayName( "It can serialize a boxlang class" )
	@Test
	public void testCanSerializeBoxLangClass() {
		// @formatter:off
		instance.executeSource(
		    """
		    	person = new src.test.bx.Person()
				result = jsonSerialize( person )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).doesNotContain( "javaSystem" );
		assertThat( json ).doesNotContain( "anotherProp" );
		assertThat( json ).doesNotContain( "anotherProp2" );
	}

	@DisplayName( "It can serialize a boxlang class using the `toJson()` convention" )
	@Test
	public void testCanSerializeBoxLangClassWithCustomSerializer() {
		// @formatter:off
		instance.executeSource(
		    """
		    	person = new src.test.bx.PersonCustom()
				result = jsonSerialize( person )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).doesNotContain( "javaSystem" );
		assertThat( json ).doesNotContain( "anotherProp" );
		assertThat( json ).doesNotContain( "anotherProp2" );
		assertThat( json ).doesNotContain( "test" );
		assertThat( json ).doesNotContain( "tags" );
		assertThat( json ).doesNotContain( "createdDate" );
		assertThat( json ).doesNotContain( "modifiedDate" );
	}

	@DisplayName( "It can serialize a boxlang function" )
	@Test
	public void testCanSerializeBoxLangFunction() {
		// @formatter:off
		instance.executeSource(
		    """
		    	function foo() {
					return true;
				}
				result = jsonSerialize( {
					"f": () => {},
					"another" : foo
				} );
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
		System.out.println( json );
		assertThat( json ).isNotEmpty();
	}

	@DisplayName( "It can serialize a non-serializable class as a basic return" )
	@Test
	public void testCanSerializeNonSerializableClass() {
		// @formatter:off
		instance.executeSource(
		    """
		    	test = new src.test.bx.NotSerializable()
				result = jsonSerialize( test )
				println( result )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
		assertThat( json ).isEqualTo( "{}" );
	}

	@DisplayName( "It can handle recursion of structs" )
	@Test
	public void testCanHandleRecursion() {
		// @formatter:off
		instance.executeSource(
		    """
		    	foo = {}
				bar = { foo : foo }
				foo.bar = bar
				result = jsonSerialize( foo )
				println( result )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
	}

	@DisplayName( "It can handle recursion of Java Map" )
	@Test
	public void testCanHandleRecursionInJavaMap() {
		// @formatter:off
		instance.executeSource(
		    """
		    	foo = new java.util.HashMap()
				foo.put( "one", 1 )
				foo.put( "two", 2 )
				foo.put( "three", 3 )

				bar = new java.util.HashMap()
				bar.put( "foo", foo )

				foo.put( "bar", bar )

				result = jsonSerialize( foo )
				println( result )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
	}

	@DisplayName( "It can handle recursion of arrays" )
	@Test
	public void testCanHandleRecursionInArrays() {
		// @formatter:off
		instance.executeSource(
		    """
		    	foo = [ 1, 2, 3 ]
				bar = [ foo ]

				foo.append( bar )

				result = jsonSerialize( foo )
				println( result )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
	}

	@DisplayName( "It can handle recursion of Java List" )
	@Test
	public void testCanHandleRecursionInJavaList() {
		// @formatter:off
		instance.executeSource(
		    """
		    	foo = new java.util.ArrayList()
				foo.add( 1 )
				foo.add( 2 )
				foo.add( 3 )

				bar = new java.util.ArrayList()
				bar.add( foo )

				foo.add( bar )

				result = jsonSerialize( foo )
				println( result )
		    """,
		context );
		// @formatter:on

		var json = variables.getAsString( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).contains( "recursive" );
	}

	@DisplayName( "It can pretty print" )
	@Test
	public void testCanPrettyPrint() {
		// @formatter:off
		instance.executeSource(
		    """
		    	result = jsonSerialize( data: {
					"one" : "wood",
					"two" : null,
					"three" : 42,
					"four" : [1,2,3],
					"five" : {},
					"six" : true
				}, pretty=true )

				println( result )
			""",
		    context );
		// @formatter:on

		var json = variables.getAsString( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).contains( "wood" );
		assertThat( json ).contains( "null" );
		assertThat( json ).contains( "42" );
		assertThat( json ).contains( "[ 1, 2, 3 ]" );
		assertThat( json ).contains( "{ }" );
		assertThat( json ).contains( "true" );
	}

	@DisplayName( "It can serialize java arrays" )
	@Test
	public void testCanSerializeJavaArrays() {
		// @formatter:off
		instance.executeSource(
		    """
				array1 = [javacast("double",0.0) ,javacast("double",0.30420544445599146)];

				matrix = {   "test": 
					[javacast("double[]", array1)]
				}
		    	result = jsonSerialize( data: matrix )

				println( result )
			""",
		    context );
		// @formatter:on

		var json = variables.getAsString( result );
		assertThat( json ).isNotEmpty();
		assertThat( json ).isEqualTo( "{\"test\":[[0.0,0.30420544445599146]]}" );
	}

}
