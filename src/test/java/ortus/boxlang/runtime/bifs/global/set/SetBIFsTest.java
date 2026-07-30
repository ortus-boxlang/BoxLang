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

package ortus.boxlang.runtime.bifs.global.set;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxSet;

public class SetBIFsTest {

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

	@DisplayName( "setNew() builds an empty default set" )
	@Test
	public void testSetNewDefault() {
		instance.executeSource( "result = setNew();", context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxSet.class );
		assertThat( ( ( BoxSet ) variables.get( result ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "setNew(type, array) seeds a set, deduplicating" )
	@Test
	public void testSetNewFromArray() {
		instance.executeSource( "result = setNew( type=\"linked\", values=[1, 2, 2, 3, 3] );", context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 3 );
		assertThat( s.getType() ).isEqualTo( BoxSet.Type.LINKED );
	}

	@DisplayName( "setOf(...) builds from positional varargs" )
	@Test
	public void testSetOf() {
		instance.executeSource( "result = setOf( 1, 2, 2, 3 );", context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Array.toSet() round-trips" )
	@Test
	public void testArrayToSet() {
		instance.executeSource( "result = [1, 2, 2, 3].toSet();", context );
		assertThat( ( ( BoxSet ) variables.get( result ) ).size() ).isEqualTo( 3 );
	}

	@DisplayName( "[1,2,3] castAs Set requires a Set source (member .toSet() converts arrays)" )
	@Test
	public void testCastAsSet() {
		// castAs is strict — convert via .toSet() first.
		instance.executeSource(
		    """
		    s = [1, 2, 2].toSet();
		    result = s castAs "Set";
		    """,
		    context );
		assertThat( ( ( BoxSet ) variables.get( result ) ).size() ).isEqualTo( 2 );
	}

	@DisplayName( "set.add / set.append (alias) add elements" )
	@Test
	public void testAddAndAppend() {
		instance.executeSource(
		    """
		    s = setNew();
		    s.add( 1 );
		    s.append( 2 );
		    boxSetAdd( s, 3 );
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "set.contains / set.has (alias) test membership" )
	@Test
	public void testContainsAndHas() {
		instance.executeSource(
		    """
		    s = [1, 2, 3].toSet();
		    a = s.contains( 2 );
		    b = s.has( 2 );
		    c = s.has( 99 );
		    result = a & "," & b & "," & c;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "true,true,false" );
	}

	@DisplayName( "set.remove / set.delete (alias) drop elements" )
	@Test
	public void testRemoveAndDelete() {
		instance.executeSource(
		    """
		    s = [1, 2, 3].toSet();
		    s.remove( 2 );
		    s.delete( 3 );
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "set.size / set.len / set.length are equivalent" )
	@Test
	public void testSizeAliases() {
		instance.executeSource(
		    """
		    s = setOf( 1, 2, 3 );
		    result = s.size() & "," & s.len() & "," & s.length();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "3,3,3" );
	}

	@DisplayName( "set.isEmpty / set.clear" )
	@Test
	public void testIsEmptyAndClear() {
		instance.executeSource(
		    """
		    s = [1, 2].toSet();
		    before = s.isEmpty();
		    s.clear();
		    after = s.isEmpty();
		    result = before & "," & after;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "false,true" );
	}

	@DisplayName( "Set algebra BIFs and member methods agree" )
	@Test
	public void testSetAlgebra() {
		instance.executeSource(
		    """
		    a = [1, 2, 3].toSet();
		    b = [3, 4, 5].toSet();
		    u = boxSetUnion( a, b );
		    i = a.intersection( b );
		    d = a.difference( b );
		    x = a.symmetricDifference( b );
		    result = u.size() & "," & i.size() & "," & d.size() & "," & x.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "5,1,2,4" );
	}

	@DisplayName( "isSubsetOf / isSupersetOf / isDisjointFrom" )
	@Test
	public void testRelations() {
		instance.executeSource(
		    """
		    a = [1, 2].toSet();
		    b = [1, 2, 3].toSet();
		    c = [9, 10].toSet();
		    result = a.isSubsetOf( b ) & "," & b.isSupersetOf( a ) & "," & a.isDisjointFrom( c );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "true,true,true" );
	}

	@DisplayName( "Functional methods: each / map / filter / reduce" )
	@Test
	public void testFunctional() {
		instance.executeSource(
		    """
		    s = [1, 2, 3, 4, 5].toSet();
		    doubled = s.map( v -> v * 2 );
		    evens = s.filter( v -> v % 2 == 0 );
		    total = s.reduce( ( acc, v ) -> acc + v, 0 );
		    counter = [0];
		    s.each( function( v ) { counter[1] += v; } );
		    result = doubled.size() & "," & evens.size() & "," & total & "," & counter[1];
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "5,2,15,15" );
	}

	@DisplayName( "every / some / none / find" )
	@Test
	public void testPredicateMethods() {
		instance.executeSource(
		    """
		    s = [1, 2, 3, 4].toSet();
		    e = s.every( v -> v > 0 );
		    so = s.some( v -> v > 3 );
		    n = s.none( v -> v > 100 );
		    f = s.find( v -> v > 2 );
		    result = e & "," & so & "," & n & "," & ( f >= 3 ? "yes" : "no" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "true,true,true,yes" );
	}

	@DisplayName( "setToArray and setToList convert back" )
	@Test
	public void testConversion() {
		instance.executeSource(
		    """
		    s = setNew( type="linked", values=["a","b","c"] );
		    arr = s.toArray();
		    list = s.toList( "-" );
		    result = arr.len() & "," & list;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "3,a-b-c" );
	}

	@DisplayName( "addAll / removeAll / retainAll" )
	@Test
	public void testBulkMutators() {
		instance.executeSource(
		    """
		    s = [1, 2, 3].toSet();
		    s.addAll( [4, 5] );
		    afterAdd = s.size();
		    s.removeAll( [1, 2] );
		    afterRem = s.size();
		    s.retainAll( [3, 999] );
		    afterRet = s.size();
		    result = afterAdd & "," & afterRem & "," & afterRet;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "5,3,1" );
	}

	@DisplayName( "Linked variant preserves insertion order through toArray" )
	@Test
	public void testLinkedOrder() {
		instance.executeSource(
		    """
		    s = setNew( type="linked", values=["c","a","b","a"] );
		    result = s.toArray();
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( "c" );
		assertThat( arr.get( 1 ) ).isEqualTo( "a" );
		assertThat( arr.get( 2 ) ).isEqualTo( "b" );
	}

	@DisplayName( "Set literal: set{1,2,3} builds a default (hash) Set" )
	@Test
	public void testSetLiteralDefault() {
		instance.executeSource( "result = set{ 1, 2, 2, 3 };", context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 3 );
		assertThat( s.getType() ).isEqualTo( BoxSet.Type.DEFAULT );
	}

	@DisplayName( "Empty set literal: set{}" )
	@Test
	public void testEmptySetLiteral() {
		instance.executeSource( "result = set{};", context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Variable named 'set' still works as identifier" )
	@Test
	public void testSetAsIdentifier() {
		instance.executeSource(
		    """
		    set = "hello";
		    result = set;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "hello" );
	}

	@DisplayName( "String.listToSet() splits a list-delimited string" )
	@Test
	public void testStringListToSetDefault() {
		instance.executeSource( "result = \"a,b,c,a\".listToSet();", context );
		assertThat( ( ( BoxSet ) variables.get( result ) ).size() ).isEqualTo( 3 );
	}

	@DisplayName( "String.listToSet(delimiter) accepts a custom delimiter" )
	@Test
	public void testStringListToSetCustomDelimiter() {
		instance.executeSource( "result = \"a|b|c|b\".listToSet( delimiter=\"|\", type=\"linked\" );", context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 3 );
		assertThat( s.getType() ).isEqualTo( BoxSet.Type.LINKED );
	}

	@DisplayName( "Struct.keySet() builds a Set of the keys" )
	@Test
	public void testStructKeySet() {
		instance.executeSource(
		    """
		    s = { name: "Luis", age: 42, email: "x@y.z" }.keySet();
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "Struct.keySet() works on non-string keys" )
	@Test
	public void testStructKeySetNonStringKeys() {
		instance.executeSource(
		    """
		       threadClass = createObject( "java", "java.lang.Thread" );
		    for( mthread in threadClass.getAllStackTraces().keySet() ) {
		    	mthread.isAlive();
		    }
		       """,
		    context );
		// Does not error
	}

	@DisplayName( "Struct.keySet() inherits case-insensitivity from default struct" )
	@Test
	public void testStructKeySetCaseInsensitive() {
		instance.executeSource(
		    """
		    s = { name: "Luis", age: 42 }.keySet();
		    result = s.isCaseSensitive();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "Struct.keySet() inherits case-sensitivity from case-sensitive struct" )
	@Test
	public void testStructKeySetCaseSensitive() {
		instance.executeSource(
		    """
		    s = structNew( "casesensitive" );
		    s[ "Name" ] = "Luis";
		    s[ "name" ] = "Brad";
		    keys = s.keySet();
		    result = keys.isCaseSensitive();
		    hasName = keys.contains( "Name" );
		    hasname = keys.contains( "name" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasName" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasname" ) ) ).isEqualTo( true );
	}

	@DisplayName( "Struct.valueSet() dedupes values" )
	@Test
	public void testStructValueSet() {
		instance.executeSource(
		    """
		    s = { a: 1, b: 1, c: 2 }.valueSet();
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "JSON serialization renders a Set as a JSON array" )
	@Test
	public void testJsonSerialization() {
		instance.executeSource(
		    """
		    s = setNew( type="linked", values=["a","b","c"] );
		    result = jsonSerialize( s );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "[\"a\",\"b\",\"c\"]" );
	}

	@DisplayName( "Sorted variant orders elements" )
	@Test
	public void testSortedOrder() {
		instance.executeSource(
		    """
		    s = setNew( type="sorted", values=[9, 1, 5, 3] );
		    result = s.toArray();
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.get( 0 ) ).isEqualTo( 1 );
		assertThat( arr.get( 1 ) ).isEqualTo( 3 );
		assertThat( arr.get( 2 ) ).isEqualTo( 5 );
		assertThat( arr.get( 3 ) ).isEqualTo( 9 );
	}

	@DisplayName( "setNew() with caseSensitive=true keeps different cases as distinct" )
	@Test
	public void testSetNewCaseSensitiveTrue() {
		instance.executeSource(
		    """
		    s = setNew( values=["Hello", "hello", "HELLO"], caseSensitive=true );
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "setNew() with caseSensitive=false (default) deduplicates cases" )
	@Test
	public void testSetNewCaseSensitiveFalseDefault() {
		instance.executeSource(
		    """
		    s = setNew( values=["Hello", "hello", "HELLO"] );
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "setNew() caseSensitive=true still normalizes numerics" )
	@Test
	public void testSetNewCaseSensitiveNumericsStillNormalized() {
		instance.executeSource(
		    """
		    s = setNew( values=[1, 1.0], caseSensitive=true );
		    result = s.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "setNew() caseSensitive=true contains is case-aware" )
	@Test
	public void testSetNewCaseSensitiveContains() {
		instance.executeSource(
		    """
		    s = setNew( values=["Foo", "Bar"], caseSensitive=true );
		    result = s.contains( "foo" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "setNew() caseSensitive=false contains is case-insensitive" )
	@Test
	public void testSetNewCaseInsensitiveContains() {
		instance.executeSource(
		    """
		    s = setNew( values=["Foo", "Bar"], caseSensitive=false );
		    result = s.contains( "foo" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "setNew() isSynchronized=true (default) produces a synchronized set" )
	@Test
	public void testSetNewIsSynchronizedDefault() {
		instance.executeSource(
		    """
		    s = setNew();
		    result = s.isSynchronized();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "setNew() isSynchronized=false produces a non-synchronized set" )
	@Test
	public void testSetNewIsSynchronizedFalse() {
		instance.executeSource(
		    """
		    s = setNew( isSynchronized=false );
		    result = s.isSynchronized();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "setNew() isSynchronized=false with seed values still works" )
	@Test
	public void testSetNewIsSynchronizedFalseWithValues() {
		instance.executeSource(
		    """
		    s = setNew( values=[1,2,3], isSynchronized=false );
		    result = s.isSynchronized();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
		BoxSet s = ( BoxSet ) variables.get( Key.of( "s" ) );
		assertThat( s.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Operator + performs set union" )
	@Test
	public void testPlusOperatorUnion() {
		instance.executeSource(
		    """
		    a = set{ 1, 2, 3 };
		    b = set{ 3, 4, 5 };
		    result = a + b;
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 5 );
	}

	@DisplayName( "Operator - performs set difference" )
	@Test
	public void testMinusOperatorDifference() {
		instance.executeSource(
		    """
		    a = set{ 1, 2, 3 };
		    b = set{ 2, 3, 4 };
		    result = a - b;
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 1 );
		assertThat( s.contains( 1 ) ).isTrue();
	}

	@DisplayName( "Operator * performs set intersection" )
	@Test
	public void testStarOperatorIntersection() {
		instance.executeSource(
		    """
		    a = set{ 1, 2, 3 };
		    b = set{ 2, 3, 4 };
		    result = a * b;
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 2 );
		assertThat( s.contains( 2 ) ).isTrue();
		assertThat( s.contains( 3 ) ).isTrue();
	}

	@DisplayName( "Operator ^ performs set symmetric difference" )
	@Test
	public void testCaretOperatorSymmetricDifference() {
		instance.executeSource(
		    """
		    a = set{ 1, 2, 3 };
		    b = set{ 2, 3, 4 };
		    result = a ^ b;
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 2 );
		assertThat( s.contains( 1 ) ).isTrue();
		assertThat( s.contains( 4 ) ).isTrue();
	}

	@DisplayName( "Set operators still work as math when not using sets" )
	@Test
	public void testOperatorsFallThroughToMath() {
		instance.executeSource(
		    """
		    plus = 2 + 3;
		    minus = 5 - 2;
		    star = 4 * 3;
		    caret = 2 ^ 3;
		    result = plus & "," & minus & "," & star & "," & caret;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "5,3,12,8" );
	}

	@DisplayName( "Compound *= on a set performs intersection" )
	@Test
	public void testStarEqualCompoundIntersection() {
		instance.executeSource(
		    """
		    a = set{ 1, 2, 3 };
		    a *= set{ 2, 3, 4 };
		    result = a;
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Explicit a = a * b on a set performs intersection" )
	@Test
	public void testExplicitStarAssignmentIntersection() {
		instance.executeSource(
		    """
		    a = set{ 1, 2, 3 };
		    a = a * set{ 2, 3, 4 };
		    result = a;
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 2 );
		assertThat( s.contains( 2 ) ).isTrue();
		assertThat( s.contains( 3 ) ).isTrue();
	}

	@DisplayName( "set{ ...array } spreads an array into the set literal" )
	@Test
	public void testSetLiteralSpreadArray() {
		instance.executeSource(
		    """
		    arr = [3, 4, 5];
		    result = set{ 1, 2, ...arr };
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 5 );
		assertThat( s.contains( 1 ) ).isTrue();
		assertThat( s.contains( 5 ) ).isTrue();
	}

	@DisplayName( "set{ ...set } spreads another set into the set literal" )
	@Test
	public void testSetLiteralSpreadSet() {
		instance.executeSource(
		    """
		    other = set{ 2, 3 };
		    result = set{ 1, ...other, 4 };
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 4 );
	}

	@DisplayName( "set{ ...array } deduplicates spread elements" )
	@Test
	public void testSetLiteralSpreadDeduplicates() {
		instance.executeSource(
		    """
		    arr = [1, 2, 3];
		    result = set{ 1, 2, ...arr };
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 3 );
	}

	@DisplayName( "set{ ...range } spreads a range into the set literal" )
	@Test
	public void testSetLiteralSpreadRange() {
		instance.executeSource(
		    """
		    result = set{ ...(1..5) };
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 5 );
		assertThat( s.contains( 1 ) ).isTrue();
		assertThat( s.contains( 5 ) ).isTrue();
	}

	@DisplayName( "set{ ...range } with existing elements deduplicates" )
	@Test
	public void testSetLiteralSpreadRangeDeduplicates() {
		instance.executeSource(
		    """
		    result = set{ 3, 4, ...(1..5) };
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 5 );
	}

	@DisplayName( "setNew(values=range) expands a range into the set" )
	@Test
	public void testSetNewFromRange() {
		instance.executeSource(
		    """
		    result = setNew( values=1..5 );
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 5 );
		assertThat( s.contains( 1 ) ).isTrue();
		assertThat( s.contains( 5 ) ).isTrue();
	}

	@DisplayName( "setNew(type, range) expands a range positionally" )
	@Test
	public void testSetNewFromRangePositional() {
		instance.executeSource(
		    """
		    result = setNew( "linked", 1..5 );
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 5 );
		assertThat( s.getType() ).isEqualTo( BoxSet.Type.LINKED );
	}

	@DisplayName( "setOf() with a range argument adds the range as a single element" )
	@Test
	public void testSetOfWithRange() {
		instance.executeSource(
		    """
		    result = setOf( ...(1..3) );
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Query.toSet() produces a Set of row structs" )
	@Test
	public void testQueryToSet() {
		instance.executeSource(
		    """
		    q = queryNew( "name,age", "varchar,integer", [ [ "Alice", 30 ], [ "Bob", 25 ] ] );
		    result = q.toSet();
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 2 );
		assertThat( s.iterator().next() ).isInstanceOf( ortus.boxlang.runtime.types.IStruct.class );
	}

	@DisplayName( "Query column values as a Set via query.columnData().toSet()" )
	@Test
	public void testQueryColumnToSet() {
		instance.executeSource(
		    """
		    q = queryNew( "name", "varchar", [ [ "Alice" ], [ "Bob" ], [ "Alice" ] ] );
		    result = q.columnData( "name" ).toSet();
		    """,
		    context );
		BoxSet s = ( BoxSet ) variables.get( result );
		assertThat( s.size() ).isEqualTo( 2 );
		assertThat( s.contains( "Alice" ) ).isTrue();
		assertThat( s.contains( "Bob" ) ).isTrue();
	}

	@DisplayName( "duplicate() deep copies a Set" )
	@Test
	public void testDuplicateSet() {
		instance.executeSource(
		    """
		    original = setNew( type="linked", values=[1, 2, 3] );
		    copy = duplicate( original );
		    copy.add( 4 );
		    result = original.size();
		    copySize = copy.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "copySize" ) ) ).isEqualTo( 4 );
	}

}
