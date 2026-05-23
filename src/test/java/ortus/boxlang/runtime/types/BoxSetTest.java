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

import java.util.Iterator;
import java.util.List;

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

	@DisplayName( "SetCaster accepts arrays, lists, sets and dedupes" )
	@Test
	void testSetCaster() {
		BoxSet fromArray = SetCaster.cast( new Object[] { 1, 2, 2, 3 } );
		assertThat( fromArray.size() ).isEqualTo( 3 );

		BoxSet fromList = SetCaster.cast( List.of( "a", "b", "a" ) );
		assertThat( fromList.size() ).isEqualTo( 2 );

		Array	a			= new Array( new Object[] { 1, 1, 2, 3 } );
		BoxSet	fromBoxArr	= SetCaster.cast( a );
		assertThat( fromBoxArr.size() ).isEqualTo( 3 );

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

}
