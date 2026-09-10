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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.casters.SetCaster;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableSet;

public class BoxSetTest {

	@DisplayName( "Default constructor builds an empty hash-backed set" )
	@Test
	void testDefaultConstructor() {
		BoxSet s = new BoxSet();
		assertThat( s.size() ).isEqualTo( 0 );
		assertThat( s.isEmpty() ).isTrue();
		assertThat( s.getType() ).isEqualTo( BoxSet.Type.DEFAULT );
	}

	@DisplayName( "Add deduplicates equal elements" )
	@Test
	void testAddDedupes() {
		BoxSet s = new BoxSet();
		assertThat( s.add( "a" ) ).isTrue();
		assertThat( s.add( "a" ) ).isFalse();
		assertThat( s.add( "b" ) ).isTrue();
		assertThat( s.size() ).isEqualTo( 2 );
	}

	@DisplayName( "of() builds from varargs and deduplicates" )
	@Test
	void testOfVarargs() {
		BoxSet s = BoxSet.of( 1, 2, 2, 3, 3, 3 );
		assertThat( s.size() ).isEqualTo( 3 );
		assertThat( s.contains( 1 ) ).isTrue();
		assertThat( s.contains( 2 ) ).isTrue();
		assertThat( s.contains( 3 ) ).isTrue();
	}

	@DisplayName( "Linked variant preserves insertion order" )
	@Test
	void testLinkedOrder() {
		BoxSet				s	= BoxSet.of( BoxSet.Type.LINKED, "c", "a", "b", "a" );
		Iterator<Object>	it	= s.iterator();
		assertThat( it.next() ).isEqualTo( "c" );
		assertThat( it.next() ).isEqualTo( "a" );
		assertThat( it.next() ).isEqualTo( "b" );
		assertThat( it.hasNext() ).isFalse();
	}

	@DisplayName( "Sorted variant orders via Compare.invoke" )
	@Test
	void testSortedOrder() {
		BoxSet				s	= BoxSet.of( BoxSet.Type.SORTED, 9, 1, 5, 3, 7 );
		Iterator<Object>	it	= s.iterator();
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 1 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 3 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 5 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 7 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 9 );
	}

	@DisplayName( "parseType handles aliases" )
	@Test
	void testParseType() {
		assertThat( BoxSet.parseType( null ) ).isEqualTo( BoxSet.Type.DEFAULT );
		assertThat( BoxSet.parseType( "default" ) ).isEqualTo( BoxSet.Type.DEFAULT );
		assertThat( BoxSet.parseType( "hash" ) ).isEqualTo( BoxSet.Type.DEFAULT );
		assertThat( BoxSet.parseType( "LINKED" ) ).isEqualTo( BoxSet.Type.LINKED );
		assertThat( BoxSet.parseType( "ordered" ) ).isEqualTo( BoxSet.Type.LINKED );
		assertThat( BoxSet.parseType( "sorted" ) ).isEqualTo( BoxSet.Type.SORTED );
		assertThat( BoxSet.parseType( "tree" ) ).isEqualTo( BoxSet.Type.SORTED );
	}

	@DisplayName( "Set algebra: union" )
	@Test
	void testUnion() {
		BoxSet	a		= BoxSet.of( 1, 2, 3 );
		BoxSet	b		= BoxSet.of( 3, 4, 5 );
		BoxSet	result	= a.union( b );
		assertThat( result.size() ).isEqualTo( 5 );
		assertThat( result.containsAll( List.of( 1, 2, 3, 4, 5 ) ) ).isTrue();
		// Originals untouched
		assertThat( a.size() ).isEqualTo( 3 );
		assertThat( b.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Set algebra: intersection" )
	@Test
	void testIntersection() {
		BoxSet	a		= BoxSet.of( 1, 2, 3, 4 );
		BoxSet	b		= BoxSet.of( 3, 4, 5, 6 );
		BoxSet	result	= a.intersection( b );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.contains( 3 ) ).isTrue();
		assertThat( result.contains( 4 ) ).isTrue();
	}

	@DisplayName( "Set algebra: difference" )
	@Test
	void testDifference() {
		BoxSet	a		= BoxSet.of( 1, 2, 3, 4 );
		BoxSet	b		= BoxSet.of( 3, 4, 5 );
		BoxSet	result	= a.difference( b );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.contains( 1 ) ).isTrue();
		assertThat( result.contains( 2 ) ).isTrue();
	}

	@DisplayName( "Set algebra: symmetric difference" )
	@Test
	void testSymmetricDifference() {
		BoxSet	a		= BoxSet.of( 1, 2, 3 );
		BoxSet	b		= BoxSet.of( 3, 4, 5 );
		BoxSet	result	= a.symmetricDifference( b );
		assertThat( result.size() ).isEqualTo( 4 );
		assertThat( result.contains( 3 ) ).isFalse();
		assertThat( result.containsAll( List.of( 1, 2, 4, 5 ) ) ).isTrue();
	}

	@DisplayName( "isSubsetOf / isSupersetOf / isDisjointFrom" )
	@Test
	void testRelations() {
		BoxSet	a		= BoxSet.of( 1, 2 );
		BoxSet	b		= BoxSet.of( 1, 2, 3 );
		BoxSet	other	= BoxSet.of( 9, 10 );
		assertThat( a.isSubsetOf( b ) ).isTrue();
		assertThat( b.isSupersetOf( a ) ).isTrue();
		assertThat( a.isDisjointFrom( other ) ).isTrue();
		assertThat( a.isDisjointFrom( b ) ).isFalse();
	}

	@DisplayName( "toArrayValue produces a BoxLang Array" )
	@Test
	void testToArrayValue() {
		BoxSet	s	= BoxSet.of( BoxSet.Type.LINKED, "x", "y", "z" );
		Array	a	= s.toArrayValue();
		assertThat( a.size() ).isEqualTo( 3 );
		assertThat( a.get( 0 ) ).isEqualTo( "x" );
		assertThat( a.get( 1 ) ).isEqualTo( "y" );
		assertThat( a.get( 2 ) ).isEqualTo( "z" );
	}

	@DisplayName( "SetCaster strict accepts only Sets; loose accepts arrays/lists/Arrays" )
	@Test
	void testSetCaster() {
		// Loose: arrays, lists, BoxLang Arrays all dedupe into a Set
		BoxSet fromArray = SetCaster.castLoose( new Object[] { 1, 2, 2, 3 } );
		assertThat( fromArray.size() ).isEqualTo( 3 );

		BoxSet fromList = SetCaster.castLoose( List.of( "a", "b", "a" ) );
		assertThat( fromList.size() ).isEqualTo( 2 );

		Array	a			= new Array( new Object[] { 1, 1, 2, 3 } );
		BoxSet	fromBoxArr	= SetCaster.castLoose( a );
		assertThat( fromBoxArr.size() ).isEqualTo( 3 );

		// Strict: arrays and lists are NOT sets — attempt fails.
		assertThat( SetCaster.attempt( new Object[] { 1, 2 } ).wasSuccessful() ).isFalse();
		assertThat( SetCaster.attempt( List.of( "a" ) ).wasSuccessful() ).isFalse();
		// Strict accepts actual Sets and wraps (no copy) so mutations propagate
		java.util.HashSet<Object> javaSet = new java.util.HashSet<>();
		javaSet.add( "x" );
		javaSet.add( "y" );
		BoxSet wrapped = SetCaster.cast( javaSet );
		assertThat( wrapped.size() ).isEqualTo( 2 );
		wrapped.add( "z" );
		assertThat( javaSet ).hasSize( 3 );

		// Round-trip via attempt() on null
		assertThat( SetCaster.attempt( null ).wasSuccessful() ).isFalse();
	}

	@DisplayName( "UnmodifiableSet throws on every mutator" )
	@Test
	void testUnmodifiable() {
		BoxSet			modifiable	= BoxSet.of( 1, 2, 3 );
		UnmodifiableSet	frozen		= modifiable.toUnmodifiable();
		assertThat( frozen.size() ).isEqualTo( 3 );
		assertThrows( UnmodifiableException.class, () -> frozen.add( 4 ) );
		assertThrows( UnmodifiableException.class, () -> frozen.remove( 1 ) );
		assertThrows( UnmodifiableException.class, () -> frozen.clear() );
		// toModifiable returns a fresh mutable copy
		BoxSet thawed = ( BoxSet ) frozen.toModifiable();
		thawed.add( 4 );
		assertThat( thawed.size() ).isEqualTo( 4 );
		assertThat( frozen.size() ).isEqualTo( 3 );
	}

	@DisplayName( "getBoxTypeName reflects variant" )
	@Test
	void testBoxTypeName() {
		assertThat( new BoxSet().getBoxTypeName() ).isEqualTo( "Set" );
		assertThat( new BoxSet( BoxSet.Type.LINKED ).getBoxTypeName() ).isEqualTo( "Set:Linked" );
		assertThat( new BoxSet( BoxSet.Type.SORTED ).getBoxTypeName() ).isEqualTo( "Set:Sorted" );
	}

	// ==========================================
	// NormalizedValue / Type-aware equality tests
	// ==========================================

	@DisplayName( "Strings are case-insensitive: 'Hello' and 'hello' are the same element" )
	@Test
	void testStringCaseInsensitive() {
		BoxSet s = new BoxSet();
		s.add( "Hello" );
		assertThat( s.add( "hello" ) ).isFalse();
		assertThat( s.add( "HELLO" ) ).isFalse();
		assertThat( s.size() ).isEqualTo( 1 );
		assertThat( s.contains( "hElLo" ) ).isTrue();
	}

	@DisplayName( "Character and char[] are treated as strings, case-insensitive" )
	@Test
	void testCharacterAndCharArray() {
		BoxSet s = new BoxSet();
		s.add( "a" );
		assertThat( s.add( 'A' ) ).isFalse();
		assertThat( s.add( new char[] { 'a' } ) ).isFalse();
		assertThat( s.size() ).isEqualTo( 1 );
		assertThat( s.contains( 'a' ) ).isTrue();
		assertThat( s.contains( new char[] { 'A' } ) ).isTrue();
	}

	@DisplayName( "Numerics: int, long, short, double, float all compare by value" )
	@Test
	void testNumericEquality() {
		BoxSet s = new BoxSet();
		s.add( 42 );
		// All these represent the same numeric value
		assertThat( s.add( 42L ) ).isFalse();
		assertThat( s.add( ( short ) 42 ) ).isFalse();
		assertThat( s.add( 42.0 ) ).isFalse();
		assertThat( s.add( 42.0f ) ).isFalse();
		assertThat( s.add( BigDecimal.valueOf( 42 ) ) ).isFalse();
		assertThat( s.add( BigInteger.valueOf( 42 ) ) ).isFalse();
		assertThat( s.size() ).isEqualTo( 1 );
		// Contains works with any numeric type
		assertThat( s.contains( 42L ) ).isTrue();
		assertThat( s.contains( 42.0 ) ).isTrue();
		assertThat( s.contains( new BigDecimal( "42.00" ) ) ).isTrue();
	}

	@DisplayName( "Numerics: AtomicInteger and AtomicLong compare by value" )
	@Test
	void testAtomicNumericEquality() {
		BoxSet s = new BoxSet();
		s.add( 100 );
		assertThat( s.add( new AtomicInteger( 100 ) ) ).isFalse();
		assertThat( s.add( new AtomicLong( 100 ) ) ).isFalse();
		assertThat( s.size() ).isEqualTo( 1 );
		assertThat( s.contains( new AtomicInteger( 100 ) ) ).isTrue();
	}

	@DisplayName( "Numerics: different values are distinct" )
	@Test
	void testNumericDistinct() {
		BoxSet s = new BoxSet();
		s.add( 1 );
		s.add( 2 );
		s.add( 3.14 );
		assertThat( s.size() ).isEqualTo( 3 );
		assertThat( s.contains( 1L ) ).isTrue();
		assertThat( s.contains( 2.0f ) ).isTrue();
		assertThat( s.contains( new BigDecimal( "3.14" ) ) ).isTrue();
		assertThat( s.contains( 99 ) ).isFalse();
	}

	@DisplayName( "DateTime: different date types representing the same instant are equal" )
	@Test
	void testDateTimeEquality() {
		DateTime			blDate	= new DateTime( java.time.ZonedDateTime.of( 2024, 6, 15, 12, 0, 0, 0, java.time.ZoneId.of( "UTC" ) ) );
		java.time.Instant	instant	= blDate.getWrapped().toInstant();

		BoxSet				s		= new BoxSet();
		s.add( blDate );
		assertThat( s.add( instant ) ).isFalse();
		assertThat( s.size() ).isEqualTo( 1 );
		assertThat( s.contains( instant ) ).isTrue();
	}

	@DisplayName( "Different normalized types are never equal" )
	@Test
	void testCrossTypeNotEqual() {
		BoxSet s = new BoxSet();
		s.add( "42" );
		s.add( 42 );
		// String "42" and numeric 42 are different BoxLangTypes, so both exist
		assertThat( s.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Remove works with case-insensitive strings" )
	@Test
	void testRemoveCaseInsensitive() {
		BoxSet s = new BoxSet();
		s.add( "Foo" );
		assertThat( s.remove( "FOO" ) ).isTrue();
		assertThat( s.isEmpty() ).isTrue();
	}

	@DisplayName( "Remove works with cross-numeric types" )
	@Test
	void testRemoveCrossNumeric() {
		BoxSet s = new BoxSet();
		s.add( 7 );
		assertThat( s.remove( 7L ) ).isTrue();
		assertThat( s.isEmpty() ).isTrue();
	}

	@DisplayName( "Intersection respects normalized equality" )
	@Test
	void testIntersectionNormalized() {
		BoxSet	a		= BoxSet.of( "Hello", "World", "Foo" );
		BoxSet	b		= BoxSet.of( "hello", "foo", "bar" );
		BoxSet	result	= a.intersection( b );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.contains( "HELLO" ) ).isTrue();
		assertThat( result.contains( "FOO" ) ).isTrue();
	}

	@DisplayName( "Union deduplicates across numeric types" )
	@Test
	void testUnionNormalized() {
		BoxSet	a		= BoxSet.of( 1, 2, 3 );
		BoxSet	b		= BoxSet.of( 2L, 3.0, 4 );
		BoxSet	result	= a.union( b );
		assertThat( result.size() ).isEqualTo( 4 );
	}

	@DisplayName( "Difference respects normalized equality" )
	@Test
	void testDifferenceNormalized() {
		BoxSet	a		= BoxSet.of( "A", "B", "C" );
		BoxSet	b		= BoxSet.of( "a", "c" );
		BoxSet	result	= a.difference( b );
		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.contains( "b" ) ).isTrue();
	}

	@DisplayName( "Sorted set works with mixed numeric types" )
	@Test
	void testSortedMixedNumerics() {
		BoxSet				s	= BoxSet.of( BoxSet.Type.SORTED, 9, 1L, ( short ) 5, 3.0, BigDecimal.valueOf( 7 ) );
		Iterator<Object>	it	= s.iterator();
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 1 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 3 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 5 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 7 );
		assertThat( ( ( Number ) it.next() ).intValue() ).isEqualTo( 9 );
	}

	@DisplayName( "ContainsAll works with normalized types" )
	@Test
	void testContainsAllNormalized() {
		BoxSet s = BoxSet.of( "Hello", "World", 42 );
		assertThat( s.containsAll( List.of( "hello", "world" ) ) ).isTrue();
		assertThat( s.containsAll( List.of( 42L, "HELLO" ) ) ).isTrue();
		assertThat( s.containsAll( List.of( "missing" ) ) ).isFalse();
	}

	@DisplayName( "isSubsetOf / isSupersetOf with normalized types" )
	@Test
	void testSubsetSupersetNormalized() {
		BoxSet	a	= BoxSet.of( 1, 2 );
		BoxSet	b	= BoxSet.of( 1L, 2.0, 3 );
		assertThat( a.isSubsetOf( b ) ).isTrue();
		assertThat( b.isSupersetOf( a ) ).isTrue();
	}

	@DisplayName( "isDisjointFrom with normalized types" )
	@Test
	void testDisjointNormalized() {
		BoxSet	a	= BoxSet.of( "Hello" );
		BoxSet	b	= BoxSet.of( "hello" );
		// Same value case-insensitively, so NOT disjoint
		assertThat( a.isDisjointFrom( b ) ).isFalse();
	}

	@DisplayName( "Case-sensitive set treats different cases as distinct" )
	@Test
	void testCaseSensitiveDistinct() {
		BoxSet s = new BoxSet( BoxSet.Type.DEFAULT, true, true );
		s.add( "Hello" );
		s.add( "hello" );
		s.add( "HELLO" );
		assertThat( s.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Case-insensitive set (default) deduplicates different cases" )
	@Test
	void testCaseInsensitiveDefault() {
		BoxSet s = new BoxSet( BoxSet.Type.DEFAULT, true, false );
		s.add( "Hello" );
		s.add( "hello" );
		s.add( "HELLO" );
		assertThat( s.size() ).isEqualTo( 1 );
	}

	@DisplayName( "Case-sensitive set contains is case-aware" )
	@Test
	void testCaseSensitiveContains() {
		BoxSet s = new BoxSet( BoxSet.Type.DEFAULT, true, true );
		s.add( "Hello" );
		assertThat( s.contains( "Hello" ) ).isTrue();
		assertThat( s.contains( "hello" ) ).isFalse();
		assertThat( s.contains( "HELLO" ) ).isFalse();
	}

	@DisplayName( "Case-sensitive set remove is case-aware" )
	@Test
	void testCaseSensitiveRemove() {
		BoxSet s = new BoxSet( BoxSet.Type.DEFAULT, true, true );
		s.add( "Hello" );
		s.add( "hello" );
		assertThat( s.size() ).isEqualTo( 2 );
		s.remove( "Hello" );
		assertThat( s.size() ).isEqualTo( 1 );
		assertThat( s.contains( "hello" ) ).isTrue();
		assertThat( s.contains( "Hello" ) ).isFalse();
	}

	@DisplayName( "Case-sensitive collection constructor deduplicates correctly" )
	@Test
	void testCaseSensitiveCollectionConstructor() {
		BoxSet s = new BoxSet( BoxSet.Type.DEFAULT, List.of( "a", "A", "b", "B" ), true );
		assertThat( s.size() ).isEqualTo( 4 );

		BoxSet ci = new BoxSet( BoxSet.Type.DEFAULT, List.of( "a", "A", "b", "B" ), false );
		assertThat( ci.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Case-sensitive does not affect numeric normalization" )
	@Test
	void testCaseSensitiveNumericUnchanged() {
		BoxSet s = new BoxSet( BoxSet.Type.DEFAULT, true, true );
		s.add( 1 );
		s.add( 1L );
		s.add( 1.0 );
		assertThat( s.size() ).isEqualTo( 1 );
	}

	@DisplayName( "stream() returns unwrapped original values" )
	@Test
	void testStreamReturnsUnwrappedValues() {
		BoxSet s = new BoxSet( BoxSet.Type.LINKED );
		s.add( "a" );
		s.add( "b" );
		s.add( "c" );
		List<Object> collected = s.stream().toList();
		assertThat( collected ).containsExactly( "a", "b", "c" ).inOrder();
	}

	@DisplayName( "parallelStream() returns unwrapped original values" )
	@Test
	void testParallelStreamReturnsUnwrappedValues() {
		BoxSet s = new BoxSet( BoxSet.Type.LINKED );
		for ( int i = 1; i <= 100; i++ ) {
			s.add( i );
		}
		long count = s.parallelStream().filter( v -> ( ( Number ) v ).intValue() % 2 == 0 ).count();
		assertThat( count ).isEqualTo( 50 );
	}

}
