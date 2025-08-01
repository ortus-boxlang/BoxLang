
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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.QueryCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.ParseException;

public class DuplicateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			resultKey	= new Key( "result" );
	static Key			refKey		= new Key( "ref" );

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

	@DisplayName( "It tests the BIF Duplicate can duplicate a struct" )
	@Test
	public void testDuplicateStruct() {

		// @formatter:off
		instance.executeSource(
		    """
				ref = {};
				result = duplicate( ref );
				result.foo = "bar";
			""",
		    context
		);
		// @formatter:on
		assertTrue( variables.getAsStruct( refKey ).isEmpty() );

		// @formatter:off
		instance.executeSource(
		    """
				ref = {
					timestamp: now(),
					a : [ 1, 2, 3 ],
					foo : {
						bar : "baz",
						another : 123,
						blah : {
							"blerg" : true
						},
						nullValue = null
					}
				};
				result = duplicate( ref );
				result.foo.bar = "blah";
			""",
		    context
		);
		// @formatter:on

		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "baz" );

		// @formatter:off
		instance.executeSource(
		    """
		        ref = {
					"a": 10,
					"b": [
						20,30
					],
					"c": [
						{ "d": 40 }
					]
				};
		        result = duplicate( ref );
				result.z = 50;
				result.y = 50;
				// mutate the original
				ref.a = 100;
				ref.b.append(400);
				ref.c[ 1 ].d = 500;
				ref.c[ 1 ].e = 600;
		    """,
		    context
		);
		// @formatter:on

		ref		= StructCaster.cast( variables.get( refKey ) );
		result	= StructCaster.cast( variables.get( resultKey ) );
		assertEquals( ref.containsKey( "z" ), false );
		assertEquals( result.containsKey( "z" ), true );
		assertEquals( ref.containsKey( "y" ), false );
		assertEquals( result.containsKey( "y" ), true );
		assertEquals( ref.getAsInteger( Key.of( "a" ) ), 100 );
		assertEquals( result.getAsInteger( Key.of( "a" ) ), 10 );
		assertEquals( ref.getAsArray( Key.of( "b" ) ).size(), 3 );
		assertEquals( result.getAsArray( Key.of( "b" ) ).size(), 2 );
		assertEquals( StructCaster.cast( ref.getAsArray( Key.of( "c" ) ).get( 0 ) ).getAsInteger( Key.of( "d" ) ), 500 );
		assertEquals( StructCaster.cast( ref.getAsArray( Key.of( "c" ) ).get( 0 ) ).getAsInteger( Key.of( "e" ) ), 600 );
		assertEquals( StructCaster.cast( result.getAsArray( Key.of( "c" ) ).get( 0 ) ).get( Key.of( "d" ) ), 40 );
		assertEquals( StructCaster.cast( result.getAsArray( Key.of( "c" ) ).get( 0 ) ).containsKey( "e" ), false );
	}

	@DisplayName( "It tests the BIF Duplicate can duplicate a struct containing a closure" )
	@Test
	public void testDuplicateStructWithClosure() {
		instance.executeSource(
		    """
		           ref = {
		    timestamp: now(),
		        	foo : {
		        		bar : ()=>"baz"
		        	}
		        };
		           result = duplicate( ref );
		        result.foo.bar = "blah";
		           """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ) instanceof Function );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
	}

	@DisplayName( "It tests can duplicate a linked struct" )
	@Test
	public void testDuplicateStructLinked() {
		instance.executeSource(
		    """
		            ref = [
		    timestamp: now(),
		         	foo : [
		    	bar : "baz"
		    ]
		         ];
		            result = duplicate( ref );
		         result.foo.bar = "blah";
		            """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "baz" );
	}

	@DisplayName( "It tests can duplicate a sorted struct" )
	@Test
	public void testDuplicateStructSorted() {
		Comparator<Key>	comp	= ( k1, k2 ) -> Integer.compare( k2.getName().length(), k1.getName().length() );
		IStruct			sorted	= Struct.sortedOf(
		    comp,
		    "timestamp", new DateTime(),
		    "foo", new Struct( new HashMap<Key, Object>() {

			    {
				    put( Key.of( "bar" ), "baz" );
			    }
		    } )
		);
		variables.put( refKey, sorted );
		instance.executeSource(
		    """
		       result = duplicate( ref );
		    result.foo.bar = "blah";
		       """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "baz" );
	}

	@DisplayName( "It the BIF Duplicate can duplicate an array" )
	@Test
	public void testDuplicateArray() {

		instance.executeSource(
		    """
		          ref = [ "foo", "bar", "baz", { "foo" : "bar" }, now() ];
		          result = duplicate( ref );
		    result.deleteAt( 2 );
		       result[ 3 ].foo = "blah";
		          """,
		    context );

		Array	ref		= ArrayCaster.cast( variables.get( refKey ) );
		Array	result	= ArrayCaster.cast( variables.get( resultKey ) );
		assertEquals( ref.size(), 5 );
		assertEquals( result.size(), 4 );
		assertEquals( ref.getAt( 2 ), "bar" );
		assertEquals( result.getAt( 2 ), "baz" );
		assertEquals( StructCaster.cast( ref.getAt( 4 ) ).get( Key.of( "foo" ) ), "bar" );
		assertEquals( StructCaster.cast( result.getAt( 3 ) ).get( Key.of( "foo" ) ), "blah" );
		assertTrue( result.getAt( 4 ) instanceof DateTime );
	}

	@DisplayName( "It the BIF Duplicate can duplicate a DateTime Object" )
	@Test
	public void testDuplicateDateTime() {
		instance.executeSource(
		    """
		       ref = now();
		       result = duplicate( ref );
		    result = result.add( "d", 1 );
		       """,
		    context );

		DateTime	ref		= DateTimeCaster.cast( variables.get( refKey ) );
		DateTime	result	= DateTimeCaster.cast( variables.get( resultKey ) );

		assertEquals( ref.getWrapped().getYear(), result.getWrapped().getYear() );
		assertNotEquals( ref.getWrapped().getDayOfWeek(), result.getWrapped().getDayOfWeek() );
	}

	@DisplayName( "it can duplicate a query and assert it from TestBox issue" )
	@Test
	public void testTestBoxDuplicateIssue() {
		// @formatter:off
		instance.executeSource(
		    """
				function equalize( any expected, any actual ){
					// Null values
					if ( isNull( arguments.expected ) && isNull( arguments.actual ) ) {
						return true;
					}
					if ( isNull( arguments.expected ) || isNull( arguments.actual ) ) {
						return false;
					}

					// Numerics
					if (
						isNumeric( arguments.actual ) && isNumeric( arguments.expected ) && toString( arguments.actual ) eq toString(
							arguments.expected
						)
					) {
						return true;
					}

					// Simple values
					if (
						isSimpleValue( arguments.actual ) && isSimpleValue( arguments.expected ) && arguments.actual eq arguments.expected
					) {
						return true;
					}

					// Queries
					if ( isQuery( arguments.actual ) && isQuery( arguments.expected ) ) {
						// Check number of records
						if ( arguments.actual.recordCount != arguments.expected.recordCount ) {
							return false;
						}

						// Get both column lists and sort them the same
						var actualColumnList   = listSort( arguments.actual.columnList, "textNoCase" );
						var expectedColumnList = listSort( arguments.expected.columnList, "textNoCase" );

						// Check column lists
						if ( actualColumnList != expectedColumnList ) {
							return false;
						}

						// Loop over each row
						var i = 0;
						while ( ++i <= arguments.actual.recordCount ) {
							// Loop over each column
							for ( var column in listToArray( actualColumnList ) ) {
								// Compare each value
								if ( arguments.actual[ column ][ i ] != arguments.expected[ column ][ i ] ) {
									// At the first sign of trouble, bail!
									return false;
								}
							}
						}

						// We made it here so nothing looked wrong
						return true;
					}

					// Structs / Object
					if ( isStruct( arguments.actual ) && isStruct( arguments.expected ) ) {
						var actualKeys   = listSort( structKeyList( arguments.actual ), "textNoCase" );
						var expectedKeys = listSort( structKeyList( arguments.expected ), "textNoCase" );
						var key          = "";

						// Confirm both structs have the same keys
						if ( actualKeys neq expectedKeys ) {
							return false;
						}

						// Loop over each key
						for ( key in arguments.actual ) {
							// check for both nulls
							if ( isNull( arguments.actual[ key ] ) and isNull( arguments.expected[ key ] ) ) {
								continue;
							}
							// check if one is null mismatch
							if ( isNull( arguments.actual[ key ] ) OR isNull( arguments.expected[ key ] ) ) {
								return false;
							}
							// And make sure they match when actual values exist
							if ( !equalize( arguments.actual[ key ], arguments.expected[ key ] ) ) {
								return false;
							}
						}

						// If we made it here, we couldn't find anything different
						return true;
					}

					return false;
				}

				query = queryNew( "" );
				queryAddColumn( query, "id", [ 1, 2, 3, 4 ] );
				queryAddColumn(
					query,
					"data",
					[ "tahi", "rua", "toru", "wha" ]
				);
				struct = { query : query };

				equalize( struct, duplicate( struct ) );
		    """,
		    context );
		// @formatter:on
	}

	@DisplayName( "it can duplicate a query" )
	@Test
	public void testDuplicateQuery() {
		instance.executeSource(
		    """
		    ref = queryNew( "id,name", "integer,varchar", [
		        { "id": 1, "name": "Luis Majano" },
		        { "id": 2, "name": "Jon Clausen" },
		    ] );
		    result = duplicate( ref );
		    """,
		    context );

		Query	ref		= QueryCaster.cast( variables.get( refKey ) );
		Query	result	= QueryCaster.cast( variables.get( resultKey ) );

		assertEquals( ref.size(), result.size() );
		assertEquals( ref.getColumnList(), result.getColumnList() );
		for ( int i = 0; i < ref.size(); i++ ) {
			for ( Key columnName : ref.getColumns().keySet() ) {
				QueryColumn	a	= ref.getColumn( columnName );
				QueryColumn	b	= result.getColumn( columnName );
				assertEquals( a.getCell( i ), b.getCell( i ) );
			}
		}
	}

	@DisplayName( "It can duplicate a variety of other types" )
	@Test
	public void testDuplicateOthers() {
		instance.executeSource(
		    """
		    ref = "foo";
		    result = duplicate( ref );
		    """,
		    context );
		assertEquals( variables.get( refKey ), variables.get( resultKey ) );
		instance.executeSource(
		    """
		    ref = 12345.4567;
		    result = duplicate( ref );
		    """,
		    context );
		assertEquals( variables.get( refKey ), variables.get( resultKey ) );

		instance.executeSource(
		    """
		    ref = 1 + 2;
		    result = duplicate( ref );
		    """,
		    context );
		assertEquals( variables.get( refKey ), variables.get( resultKey ) );
	}

	@DisplayName( "It can duplicate XMLObjects" )
	@Test
	public void testDuplicateXML() {
		instance.executeSource(
		    """
		    module = '<mura name="filebrowser" contenttypes="" iconclass="mi-commenting"></mura>';
		    xml = xmlParse( module );
		    xml2 = duplicate(xml);

		      """,
		    context );
		assertTrue( variables.get( Key.of( "xml" ) ) instanceof XML );
		assertTrue( variables.get( Key.of( "xml2" ) ) instanceof XML );
		assertEquals( variables.getAsXML( Key.of( "xml" ) ).toString(), variables.getAsXML( Key.of( "xml2" ) ).toString() );

	}

	@DisplayName( "It can duplicate null" )
	@Test
	public void testDuplicateNull() {
		instance.executeSource(
		    """
		    ref = null;
		    result = duplicate( ref );
		    """,
		    context );
		assertNull( variables.get( refKey ) );
		assertNull( variables.get( resultKey ) );
	}

	@DisplayName( "It can duplicate a class" )
	@Test
	public void testDuplicateAClass() {
		instance.executeSource(
		    """
		          clazz = new src.test.java.TestCases.phase3.MyClass();
		       clazz.complex = [ foo : "bar" ]
		        request.foo = "bar2"
		          clazz2 = duplicate( clazz, false );
		       clazz2.complex.baz = "bum"
		    // struct is shared by reference
		      assert structKeyList( clazz.complex ) == "foo,baz";
		       assert structKeyList( clazz2.complex ) == "foo,baz";

		          // execute public method
		          result = clazz2.foo();

		          // private methods error
		          try {
		          	clazz2.bar()
		          	assert false;
		          } catch( BoxRuntimeException e ) {
		          	assert e.message contains "bar";
		          }

		          // pseduoconstructor should NOT run again
		          assert request.foo == "bar2"

		          // Can call public method that accesses private method, and variables, and request scope
		          assert result == "I work! whee true true bar2 true";

		          // This scope is reference to actual CFC instance
		          assert clazz2.$bx.$class.getName() == clazz2.getThis().$bx.$class.getName();

		          // Can call public methods on this
		          assert clazz2.runThisFoo() == "I work! whee true true bar2 true";

		          assert clazz2.thisVar == "thisValue";
		            		         """,
		    context );
	}

	@DisplayName( "It can duplicate a class deeply" )
	@Test
	public void testDuplicateAClassDeeply() {
		instance.executeSource(
		    """
		         clazz = new src.test.java.TestCases.phase3.MyClass();
		      clazz.complex = [ foo : "bar" ]
		       request.foo = "bar2"
		         clazz2 = duplicate( clazz, true );
		       clazz2.complex.baz = "bum"
		       assert structKeyList( clazz.complex ) == "foo";
		    assert structKeyList( clazz2.complex ) == "foo,baz";
		         // execute public method
		         result = clazz2.foo();

		         // private methods error
		         try {
		         	clazz2.bar()
		         	assert false;
		         } catch( BoxRuntimeException e ) {
		         	assert e.message contains "bar";
		         }

		         // pseduoconstructor should NOT run again
		         assert request.foo == "bar2"

		         // Can call public method that accesses private method, and variables, and request scope
		         assert result == "I work! whee true true bar2 true";

		         // This scope is reference to actual CFC instance
		         assert clazz2.$bx.$class.getName() == clazz2.getThis().$bx.$class.getName();

		         // Can call public methods on this
		         assert clazz2.runThisFoo() == "I work! whee true true bar2 true";

		         assert clazz2.thisVar == "thisValue";
		           		         """,
		    context );
	}

	@DisplayName( "It can test member functions" )
	@Test
	public void testMemberMethods() {
		//@formatter:off
		instance.executeSource(
		    """
			testStruct = { "foo": "bar" };
			testArray = [ "foo", "bar" ];
			testQuery = queryNew( [ "foo", "bar" ] );
			testDate = now();
			dupeStruct = testStruct.duplicate();
			assert dupeStruct == testStruct;
			dupeArray = testArray.duplicate();
			assert dupeArray == testArray;
			dupeQuery = testQuery.duplicate();
			assert dupeQuery.columnArray().len() == 2;
			dupeDate = testDate.duplicate();
			assert dupeDate == testDate;
		    """,
		    context );
		// @formatter:on

	}

	@Disabled( "Performance benchmark test on a struct" )
	@Test
	public void benchmarkStruct() {
		instance.executeSource(
		    """
		     ref = {
		       	foo : {
		       		bar : "baz",
		    		blah : {
		    			"blerg" : true
		    		}
		       	}
		    };
		                system = createObject( "java", "java.lang.System" );
		             start = system.currentTimeMillis();
		       duplicate( ref, true )
		             onceTime = ( system.currentTimeMillis() - start );
		       for( i=1; i <= 100000; i++ ){
		        duplicate( ref, true )
		             }
		             end = system.currentTimeMillis();
		          totalTime = ( end - start );
		                  """,
		    context );
	}

	@Disabled( "Performance benchmark test on an array" )
	@Test
	public void benchmarkArray() {
		instance.executeSource(
		    """
		     ref = [ "foo", "bar", "baz", { "foo" : "bar" } ];
		             system = createObject( "java", "java.lang.System" );
		          start = system.currentTimeMillis();
		    duplicate( ref, true )
		          onceTime = ( system.currentTimeMillis() - start );
		    for( i=1; i <= 100000; i++ ){
		     duplicate( ref, true )
		          }
		          end = system.currentTimeMillis();
		       totalTime = ( end - start );
		               """,
		    context );
	}

	@Test
	public void testDuplicateException() {
		instance.executeSource(
		    """
		    	result = duplicate( new java.lang.RuntimeException("boom") );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isInstanceOf( RuntimeException.class );
	}

	@Test
	public void testDuplicateParseException() {
		instance.executeSource(
		    """
		    try {
		       	getBoxContext().getRuntime().executeSource("$%^&*()");
		    } catch( e ){
		    	result = duplicate( e );
		    }
		       """,
		    context );
		assertThat( variables.get( resultKey ) ).isInstanceOf( ParseException.class );
		assertThat( ( ( Throwable ) variables.get( resultKey ) ).getMessage() ).contains( "'^' was unexpected" );

	}
}
