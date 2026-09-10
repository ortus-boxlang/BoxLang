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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

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

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		assertThat( ( ( Array ) variables.get( result ) ).size() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    result = [1,2,3];
		    """,
		    context );
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.dereference( context, one, false ) ).isEqualTo( 1 );
		assertThat( arr.dereference( context, two, false ) ).isEqualTo( 2 );
		assertThat( arr.dereference( context, three, false ) ).isEqualTo( 3 );

		instance.executeSource(
		    """
		    a = "alpha";
		    result = [a];
		    """,
		    context );
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 1 );
		assertThat( arr.dereference( context, one, false ) ).isEqualTo( "alpha" );

		instance.executeSource(
		    """
		      result = [
		    "foo",
		    'bar'
		     ]
		      """,
		    context );
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 2 );
		assertThat( arr.dereference( context, one, false ) ).isEqualTo( "foo" );
		assertThat( arr.dereference( context, two, false ) ).isEqualTo( "bar" );

		instance.executeSource(
		    """
		        result = [
		    [1,2],
		    [3,4],
		    "brad"
		     ];
		        """,
		    context );
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );

		assertThat( arr.dereference( context, one, false ) instanceof Array ).isEqualTo( true );
		Array arrSub1 = ( Array ) arr.dereference( context, one, false );
		assertThat( arrSub1.dereference( context, one, false ) ).isEqualTo( 1 );
		assertThat( arrSub1.dereference( context, two, false ) ).isEqualTo( 2 );

		assertThat( arr.dereference( context, two, false ) instanceof Array ).isEqualTo( true );
		Array arrSub2 = ( Array ) arr.dereference( context, two, false );
		assertThat( arrSub2.dereference( context, one, false ) ).isEqualTo( 3 );
		assertThat( arrSub2.dereference( context, two, false ) ).isEqualTo( 4 );

		assertThat( arr.dereference( context, three, false ) ).isEqualTo( "brad" );

	}

	@DisplayName( "array literal spread" )
	@Test
	public void testArrayLiteralSpread() {
		instance.executeSource(
		    """
		    values = [ 2, 3 ];
		    result = [ 1, ...values, 4, ...[ 5, 6 ] ];
		    """,
		    context );

		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 6 );
		assertThat( arr.dereference( context, Key.of( 1 ), false ) ).isEqualTo( 1 );
		assertThat( arr.dereference( context, Key.of( 2 ), false ) ).isEqualTo( 2 );
		assertThat( arr.dereference( context, Key.of( 3 ), false ) ).isEqualTo( 3 );
		assertThat( arr.dereference( context, Key.of( 4 ), false ) ).isEqualTo( 4 );
		assertThat( arr.dereference( context, Key.of( 5 ), false ) ).isEqualTo( 5 );
		assertThat( arr.dereference( context, Key.of( 6 ), false ) ).isEqualTo( 6 );
	}

	@DisplayName( "array literal spread rejects non-array values" )
	@Test
	public void testArrayLiteralSpreadRejectsNonArrayValues() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    result = [ ...123 ];
		    """,
		    context ) );
	}

	@DisplayName( "spread only bracket literal resolves to ordered struct when source is struct" )
	@Test
	public void testSpreadOnlyBracketLiteralResolvesToOrderedStructForStructSources() {
		instance.executeSource(
		    """
		    s1 = { a: 1, b: 2 };
		    result = [ ...s1 ];
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( str.size() ).isEqualTo( 2 );
		assertThat( str.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( str.get( Key.of( "b" ) ) ).isEqualTo( 2 );

		Key[] keys = str.keySet().toArray( new Key[ 0 ] );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "a" );
		assertThat( keys[ 1 ].getName() ).isEqualTo( "b" );
	}

	@DisplayName( "spread only bracket literal with struct sources preserves ordered struct override precedence" )
	@Test
	public void testSpreadOnlyBracketLiteralStructOverridePrecedence() {
		instance.executeSource(
		    """
		    left = [ a: 1, shared: "left" ];
		    right = [ b: 2, shared: "right" ];
		    result = [ ...left, ...right ];
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( str.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( str.get( Key.of( "b" ) ) ).isEqualTo( 2 );
		assertThat( str.get( Key.of( "shared" ) ) ).isEqualTo( "right" );

		Key[] keys = str.keySet().toArray( new Key[ 0 ] );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "a" );
		assertThat( keys[ 1 ].getName() ).isEqualTo( "shared" );
		assertThat( keys[ 2 ].getName() ).isEqualTo( "b" );
	}

	@DisplayName( "spread only bracket literal resolves to array when source is array" )
	@Test
	public void testSpreadOnlyBracketLiteralResolvesToArrayForArraySources() {
		instance.executeSource(
		    """
		    values = [ 1, 2 ];
		    result = [ ...values ];
		    """,
		    context );

		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 2 );
		assertThat( arr.dereference( context, Key.of( 1 ), false ) ).isEqualTo( 1 );
		assertThat( arr.dereference( context, Key.of( 2 ), false ) ).isEqualTo( 2 );
	}

	@DisplayName( "spread only bracket literal rejects mixed array and struct sources" )
	@Test
	public void testSpreadOnlyBracketLiteralRejectsMixedSourceTypes() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    arr = [ 1 ];
		    str = { a: 2 };
		    result = [ ...arr, ...str ];
		    """,
		    context ) );
	}

	@Test
	public void testfqnKey() {
		// Workaround for Lucee compat. I'm not inclined to support this in BL.
		instance.executeSource(
		    """
		       result = {
		    	foo.bar : "baz"
		    }
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).get( "foo.bar" ) ).isEqualTo( "baz" );
	}

	@DisplayName( "unordered struct" )
	@Test
	public void testUnorderedStruct() {

		instance.executeSource(
		    """
		    result = {}
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    result = { 1a : true }
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "1a" ) ) ).isEqualTo( true );

		instance.executeSource(
		    """
		    result = { 1a : true }
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "1a" ) ) ).isEqualTo( true );

		instance.executeSource(
		    """
		    result = { 42 : "wood" }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( 42 ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "42" );

		instance.executeSource(
		    """
		    result = { "brad" : "wood" }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    a = "wood";
		    b = "majano";
		    result = { a, b };
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 2 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "a" ) ) ).isEqualTo( "wood" );
		assertThat( str.get( Key.of( "b" ) ) ).isEqualTo( "majano" );

		instance.executeSource(
		    """
		    fooBar = "wood";
		    result = { fooBar }.keyList();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "fooBar" );

		instance.executeSource(
		    """
		    result = { "brad" = "wood" }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { "brad" : 'wood' }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { "brad" = 'wood' }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { brad : "wood" }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = { brad = "wood" }
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    someValue = "wood"
		      result = { brad = someValue }
		      """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

	}

	@DisplayName( "unordered struct spread from array uses 1 based keys" )
	@Test
	public void testUnorderedStructSpreadFromArrayUsesOneBasedKeys() {
		instance.executeSource(
		    """
		    values = [ "a", "b" ];
		    result = { ...values };
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.size() ).isEqualTo( 2 );
		assertThat( str.get( Key.of( 1 ) ) ).isEqualTo( "a" );
		assertThat( str.get( Key.of( 2 ) ) ).isEqualTo( "b" );
	}

	@DisplayName( "unordered struct spread merge precedence follows declaration order" )
	@Test
	public void testUnorderedStructSpreadMergePrecedenceFollowsDeclarationOrder() {
		instance.executeSource(
		    """
		    result = { 1 : "first", ...[ "x", "y" ], 2 : "override" };
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( 1 ) ) ).isEqualTo( "x" );
		assertThat( str.get( Key.of( 2 ) ) ).isEqualTo( "override" );
	}

	@DisplayName( "unordered struct spread merge precedence follows declaration order with struct sources" )
	@Test
	public void testUnorderedStructSpreadMergePrecedenceFromStructSources() {
		instance.executeSource(
		    """
		    left = { a: 1, shared: "left" };
		    right = { b: 2, shared: "right" };
		    result = { ...left, middle: 3, ...right, shared: "literal" };
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.size() ).isEqualTo( 4 );
		assertThat( str.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( str.get( Key.of( "middle" ) ) ).isEqualTo( 3 );
		assertThat( str.get( Key.of( "b" ) ) ).isEqualTo( 2 );
		assertThat( str.get( Key.of( "shared" ) ) ).isEqualTo( "literal" );
	}

	@DisplayName( "ordered struct spread from array uses 1 based keys" )
	@Test
	public void testOrderedStructSpreadFromArrayUsesOneBasedKeys() {
		instance.executeSource(
		    """
		    values = [ "a", "b" ];
		    result = [ ...values, tail: true ];
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( str.size() ).isEqualTo( 3 );
		assertThat( str.get( Key.of( 1 ) ) ).isEqualTo( "a" );
		assertThat( str.get( Key.of( 2 ) ) ).isEqualTo( "b" );
		assertThat( str.get( Key.of( "tail" ) ) ).isEqualTo( true );

		Key[] keys = str.keySet().toArray( new Key[ 0 ] );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "1" );
		assertThat( keys[ 1 ].getName() ).isEqualTo( "2" );
		assertThat( keys[ 2 ].getName() ).isEqualTo( "tail" );
	}

	@DisplayName( "ordered struct spread merge precedence follows declaration order" )
	@Test
	public void testOrderedStructSpreadMergePrecedenceFollowsDeclarationOrder() {
		instance.executeSource(
		    """
		    left = [ a: 1, shared: "left" ];
		    right = [ b: 2, shared: "right" ];
		    result = [ ...left, middle: 3, ...right, shared: "literal" ];
		    """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( str.size() ).isEqualTo( 4 );
		assertThat( str.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( str.get( Key.of( "middle" ) ) ).isEqualTo( 3 );
		assertThat( str.get( Key.of( "b" ) ) ).isEqualTo( 2 );
		assertThat( str.get( Key.of( "shared" ) ) ).isEqualTo( "literal" );

		Key[] keys = str.keySet().toArray( new Key[ 0 ] );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "a" );
		assertThat( keys[ 1 ].getName() ).isEqualTo( "shared" );
		assertThat( keys[ 2 ].getName() ).isEqualTo( "middle" );
		assertThat( keys[ 3 ].getName() ).isEqualTo( "b" );
	}

	@DisplayName( "ordered struct spread rejects non spreadable values" )
	@Test
	public void testOrderedStructSpreadRejectsNonSpreadableValues() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    result = [ a: 1, ...42 ];
		    """,
		    context ) );
	}

	@DisplayName( "Function in struct" )
	@Test
	public void testFunctionInStruct() {

		instance.executeSource(
		    """
		    function fooFunction( returnStatement = "not my func" ){
		    	return arguments.returnStatement;
		    }
		    function invokeFoo( returnStatement ){
		         return foo.execute( arguments.returnStatement );
		    }
		    foo = {
		    	execute : fooFunction
		    };
		    result = invokeFoo( "my func" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "my func" );
	}

	@DisplayName( "unordered struct complex" )
	@Test
	public void testUnorderedStructComplex() {

		instance.executeSource(
		    """
		       result = {
		      something : [
		    	"foo",
		    	"bar",
		    	{ 'luis': true }
		      ],
		      "else" : 42
		    };
		       """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.size() ).isEqualTo( 2 );

		assertThat( str.get( Key.of( "something" ) ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) str.get( Key.of( "something" ) );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.dereference( context, one, false ) ).isEqualTo( "foo" );
		assertThat( arr.dereference( context, two, false ) ).isEqualTo( "bar" );
		assertThat( arr.dereference( context, three, false ) instanceof IStruct ).isEqualTo( true );
		Struct strSub = ( Struct ) arr.dereference( context, three, false );
		assertThat( strSub.getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( strSub.size() ).isEqualTo( 1 );
		assertThat( strSub.get( Key.of( "luis" ) ) ).isEqualTo( true );

		assertThat( str.get( Key.of( "else" ) ) ).isEqualTo( 42 );

		// These may be in any order
		Key[]			keys			= str.keySet().toArray( new Key[ 0 ] );
		List<String>	possibleKeys	= List.of( "something", "else" );
		assertThat( possibleKeys.contains( keys[ 0 ].getName() ) ).isTrue();
		assertThat( possibleKeys.contains( keys[ 1 ].getName() ) ).isTrue();

	}

	@DisplayName( "ordered struct" )
	@Test
	public void testOrderedStruct() {

		instance.executeSource(
		    """
		    result = [:]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 0 );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );

		instance.executeSource(
		    """
		    result = [=];
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 0 );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );

		instance.executeSource(
		    """
		    result = [ "brad" : "wood" ]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		IStruct str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = ["brad" = "wood"]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ "brad" : 'wood' ]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ "brad" = 'wood' ]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ brad : "wood" ]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [ brad = "wood" ]
		    """,
		    context );
		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( ( ( IStruct ) variables.get( result ) ).size() ).isEqualTo( 1 );
		str = ( IStruct ) variables.get( result );
		assertThat( str.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( str.keySet().toArray( new Key[ 0 ] )[ 0 ].getName() ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    result = [
		      something : [
		    	"foo",
		    	"bar",
		    	[ 'luis': true ]
		      ],
		      "else" : 42
		    ];
		       """,
		    context );

		assertThat( variables.get( result ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( result ).getType() ).isEqualTo( Struct.TYPES.LINKED );
		str = ( IStruct ) variables.get( result );
		assertThat( str.size() ).isEqualTo( 2 );

		assertThat( str.get( Key.of( "something" ) ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) str.get( Key.of( "something" ) );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.dereference( context, one, false ) ).isEqualTo( "foo" );
		assertThat( arr.dereference( context, two, false ) ).isEqualTo( "bar" );
		assertThat( arr.dereference( context, three, false ) instanceof IStruct ).isEqualTo( true );
		Struct strSub = ( Struct ) arr.dereference( context, three, false );
		assertThat( strSub.getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( strSub.size() ).isEqualTo( 1 );
		assertThat( strSub.get( Key.of( "luis" ) ) ).isEqualTo( true );

		assertThat( str.get( Key.of( "else" ) ) ).isEqualTo( 42 );

		// Must be in this order
		Key[] keys = str.keySet().toArray( new Key[ 0 ] );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "something" );
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
		assertThat( variables.get( result ) ).isEqualTo( "Yes" );

		instance.executeSource(
		    """
		    result = false.yesNoFormat()
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "No" );

	}

	@DisplayName( "string literal member chain" )
	@Test
	public void testStringLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = "Brad".len()
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 );

	}

	@DisplayName( "array literal member chain" )
	@Test
	public void testArrayLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = [1,2,3].avg()
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2 );

	}

	@DisplayName( "struct literal member chain" )
	@Test
	public void testStructLiteralMemberChain() {

		instance.executeSource(
		    """
		    result = { brad : "wood", luis : "majano" }.keyArray()
		    """,
		    context );
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		Array arr = ( Array ) variables.get( result );
		assertThat( variables.get( result ) instanceof Array ).isEqualTo( true );
		// These must both exist, but in any order
		List<String> expectedValues = List.of( "brad", "luis" );
		assertThat( expectedValues.contains( arr.toArray()[ 0 ] ) ).isEqualTo( true );
		assertThat( expectedValues.contains( arr.toArray()[ 1 ] ) ).isEqualTo( true );

	}

	@Test
	public void testCFTranspileKeysUpperCase() {

		instance.executeSource(
		    """
		       foo = { brad : 'wood' }
		    result = foo.keyList()
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "BRAD" );

		instance.executeSource(
		    """
		    fooBar = 'wood';
		    result = { fooBar }.keyList()
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "fooBar" );

	}

}
