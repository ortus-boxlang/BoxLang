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
package TestCases.phase2;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

@Disabled
public class ObjectLiteralTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			one		= new Key( "1" );
	static Key			two		= new Key( "2" );
	static Key			three	= new Key( "3" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "array" )
	@Test
	public void testArray() {

		instance.executeSource(
		    """
		    result = []
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Array ).isEqualTo( true );
		assertThat( ( ( Array ) variables.dereference( result, false ) ).size() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    result = [1,2,3];
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) variables.dereference( result, false );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.dereference( one, false ) ).isEqualTo( 1 );
		assertThat( arr.dereference( two, false ) ).isEqualTo( 2 );
		assertThat( arr.dereference( three, false ) ).isEqualTo( 3 );

		instance.executeSource(
		    """
		      result = [
		    "foo",
		    'bar'
		     ]
		      """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Array ).isEqualTo( true );
		arr = ( Array ) variables.dereference( result, false );
		assertThat( arr.size() ).isEqualTo( 2 );
		assertThat( arr.dereference( one, false ) ).isEqualTo( "foo" );
		assertThat( arr.dereference( two, false ) ).isEqualTo( "bar" );

		instance.executeSource(
		    """
		        result = [
		    [1,2],
		    [3,4],
		    "brad"
		     ];
		        """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Array ).isEqualTo( true );
		arr = ( Array ) variables.dereference( result, false );
		assertThat( arr.size() ).isEqualTo( 3 );

		assertThat( arr.dereference( one, false ) instanceof Array ).isEqualTo( true );
		Array arrSub1 = ( Array ) arr.dereference( one, false );
		assertThat( arrSub1.dereference( one, false ) ).isEqualTo( 1 );
		assertThat( arrSub1.dereference( two, false ) ).isEqualTo( 2 );

		assertThat( arr.dereference( two, false ) instanceof Array ).isEqualTo( true );
		Array arrSub2 = ( Array ) arr.dereference( two, false );
		assertThat( arrSub2.dereference( one, false ) ).isEqualTo( 3 );
		assertThat( arrSub2.dereference( two, false ) ).isEqualTo( 4 );

		assertThat( arr.dereference( two, false ) ).isEqualTo( "brad" );

	}

	@DisplayName( "unordered struct" )
	@Test
	public void testUnorderedStruct() {

		instance.executeSource(
		    """
		    result = {}
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    result = { "brad" : "wood" }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		Struct str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { "brad" = "wood" }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { "brad" : 'wood' }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { "brad" = 'wood' }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { brad : "wood" }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "BRAD" );

		instance.executeSource(
		    """
		    result = { brad = "wood" }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "BRAD" );

		instance.executeSource(
		    """
		       result = {
		      something : [
		    	"foo",
		    	"bar",
		    	{ 'luis': true }
		      ]
		      "else" : 42
		    };
		       """,
		    context );

		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.DEFAULT );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.size() ).isEqualTo( 2 );

		assertThat( str.dereference( Key.of( "something" ), false ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) str.dereference( Key.of( "something" ), false );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.dereference( one, false ) ).isEqualTo( "foo" );
		assertThat( arr.dereference( two, false ) ).isEqualTo( "bar" );
		assertThat( arr.dereference( three, false ) instanceof Struct ).isEqualTo( true );
		Struct strSub = ( Struct ) arr.dereference( three, false );
		assertThat( strSub.type ).isEqualTo( Struct.Type.DEFAULT );
		assertThat( strSub.size() ).isEqualTo( 1 );
		assertThat( strSub.dereference( Key.of( "luis" ), false ) ).isEqualTo( true );

		assertThat( str.dereference( Key.of( "else" ), false ) ).isEqualTo( 42 );

		// These may be in any order
		Key[]			keys			= str.keySet().toArray( new Key[ 0 ] );
		List<String>	possibleKeys	= List.of( "SOMETHING", "else" );
		assertThat( possibleKeys.contains( keys[ 0 ].getName() ) ).isEqualTo( "SOMETHING" );
		assertThat( possibleKeys.contains( keys[ 1 ].getName() ) ).isEqualTo( "else" );

	}

	@DisplayName( "ordered struct" )
	@Test
	public void testOrderedStruct() {

		instance.executeSource(
		    """
		    result = [:]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 0 );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );

		instance.executeSource(
		    """
		    result = [ "brad" : "wood" ]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		Struct str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = ["brad" = "wood"]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ "brad" : 'wood' ]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ "brad" = 'wood' ]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ brad : "wood" ]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "BRAD" );

		instance.executeSource(
		    """
		    result = [ brad = "wood" ]
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).size() ).isEqualTo( 1 );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "BRAD" );

		instance.executeSource(
		    """
		    result = [
		      something : [
		    	"foo",
		    	"bar",
		    	[ 'luis': true ]
		      ]
		      "else" : 42
		    ];
		       """,
		    context );

		assertThat( variables.dereference( result, false ) instanceof Struct ).isEqualTo( true );
		assertThat( ( ( Struct ) variables.dereference( result, false ) ).type ).isEqualTo( Struct.Type.LINKED );
		str = ( Struct ) variables.dereference( result, false );
		assertThat( str.size() ).isEqualTo( 2 );

		assertThat( str.dereference( Key.of( "something" ), false ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) str.dereference( Key.of( "something" ), false );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.dereference( one, false ) ).isEqualTo( "foo" );
		assertThat( arr.dereference( two, false ) ).isEqualTo( "bar" );
		assertThat( arr.dereference( three, false ) instanceof Struct ).isEqualTo( true );
		Struct strSub = ( Struct ) arr.dereference( three, false );
		assertThat( strSub.type ).isEqualTo( Struct.Type.LINKED );
		assertThat( strSub.size() ).isEqualTo( 1 );
		assertThat( strSub.dereference( Key.of( "luis" ), false ) ).isEqualTo( true );

		assertThat( str.dereference( Key.of( "else" ), false ) ).isEqualTo( 42 );

		// Must be in this order
		Key[] keys = str.keySet().toArray( new Key[ 0 ] );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "SOMETHING" );
		assertThat( keys[ 1 ].getName() ).isEqualTo( "else" );

	}

	@DisplayName( "boolean literal member chain" )
	@Test
	public void testBooleanLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = true.yesNoFormat()
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "Yes" );

		instance.executeSource(
		    """
		    result = false.yesNoFormat()
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "No" );

	}

	@DisplayName( "string literal member chain" )
	@Test
	public void testStringLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = "Brad".len()
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 4 );

	}

	@DisplayName( "array literal member chain" )
	@Test
	public void testArrayLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = [1,2,3].avg()
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 2 );

	}

	@DisplayName( "struct literal member chain" )
	@Test
	public void testStructLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = { brad : "wood", luis : "majano" }.keyArray()
		    """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) variables.dereference( result, false );
		assertThat( variables.dereference( result, false ) instanceof Array ).isEqualTo( true );
		// These must both exist, but in any order
		List<String> expectedValues = List.of( "BRAD", "LUIS" );
		assertThat( expectedValues.contains( arr.toArray()[ 0 ] ) ).isEqualTo( true );
		assertThat( expectedValues.contains( arr.toArray()[ 1 ] ) ).isEqualTo( true );

	}

}