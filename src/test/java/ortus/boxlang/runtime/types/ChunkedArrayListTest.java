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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ChunkedArrayListTest {

	// ----------------------------------------------------------
	// Construction and size
	// ----------------------------------------------------------

	@Test
	@DisplayName( "New list is empty" )
	void testNewListIsEmpty() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		assertThat( list.size() ).isEqualTo( 0 );
		assertThat( list.isEmpty() ).isTrue();
	}

	@Test
	@DisplayName( "Custom chunk size" )
	void testCustomChunkSize() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>( 8 );
		for ( int i = 0; i < 20; i++ ) {
			list.add( "item" + i );
		}
		assertThat( list.size() ).isEqualTo( 20 );
		for ( int i = 0; i < 20; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( "item" + i );
		}
	}

	@Test
	@DisplayName( "Non-power-of-2 chunk size rounds up automatically" )
	void testNonPowerOf2ChunkSizeRoundsUp() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>( 1000 );
		// 1000 rounds up to 1024 — should work fine
		for ( int i = 0; i < 2000; i++ ) {
			list.add( "item" + i );
		}
		assertThat( list.size() ).isEqualTo( 2000 );
		for ( int i = 0; i < 2000; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( "item" + i );
		}
	}

	@Test
	@DisplayName( "Zero and negative chunk size uses default" )
	void testInvalidChunkSizesUseDefault() {
		ChunkedArrayList<String> list1 = new ChunkedArrayList<>( 0 );
		list1.add( "a" );
		assertThat( list1.get( 0 ) ).isEqualTo( "a" );

		ChunkedArrayList<String> list2 = new ChunkedArrayList<>( -1 );
		list2.add( "b" );
		assertThat( list2.get( 0 ) ).isEqualTo( "b" );
	}

	@Test
	@DisplayName( "Initial capacity pre-allocates chunks" )
	void testInitialCapacity() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 16, 1000 );
		for ( int i = 0; i < 1000; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( 1000 );
		for ( int i = 0; i < 1000; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	@Test
	@DisplayName( "Initial capacity of 0 works (no pre-allocation)" )
	void testInitialCapacityZero() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>( 16, 0 );
		list.add( "a" );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
	}

	@Test
	@DisplayName( "Array constructor bulk-copies data" )
	void testArrayConstructor() {
		Integer[] data = new Integer[ 5000 ];
		for ( int i = 0; i < data.length; i++ ) {
			data[ i ] = i;
		}
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( data );
		assertThat( list.size() ).isEqualTo( 5000 );
		for ( int i = 0; i < 5000; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
		// Can still add after construction
		list.add( 9999 );
		assertThat( list.size() ).isEqualTo( 5001 );
		assertThat( list.get( 5000 ) ).isEqualTo( 9999 );
	}

	@Test
	@DisplayName( "Array constructor with empty array" )
	void testArrayConstructorEmpty() {
		String[]					data	= new String[ 0 ];
		ChunkedArrayList<String>	list	= new ChunkedArrayList<>( data );
		assertThat( list.size() ).isEqualTo( 0 );
		assertThat( list.isEmpty() ).isTrue();
		list.add( "a" );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
	}

	// ----------------------------------------------------------
	// add(E) and get(int)
	// ----------------------------------------------------------

	@Test
	@DisplayName( "add() and get() basic" )
	void testAddAndGet() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		assertThat( list.size() ).isEqualTo( 3 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
		assertThat( list.get( 2 ) ).isEqualTo( "c" );
	}

	@Test
	@DisplayName( "add() supports null elements" )
	void testAddNull() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( null );
		list.add( "a" );
		list.add( null );
		assertThat( list.size() ).isEqualTo( 3 );
		assertThat( list.get( 0 ) ).isNull();
		assertThat( list.get( 1 ) ).isEqualTo( "a" );
		assertThat( list.get( 2 ) ).isNull();
	}

	@Test
	@DisplayName( "add() across multiple chunks" )
	void testAddAcrossChunks() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 8 );
		for ( int i = 0; i < 55; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( 55 );
		for ( int i = 0; i < 55; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	@Test
	@DisplayName( "add() into second layer (beyond chunkSize^2)" )
	void testAddSecondLayer() {
		int							chunkSize	= 4;
		ChunkedArrayList<Integer>	list		= new ChunkedArrayList<>( chunkSize );
		int							count		= chunkSize * chunkSize + chunkSize + 2; // 22 items with chunkSize=4
		for ( int i = 0; i < count; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( count );
		for ( int i = 0; i < count; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	// ----------------------------------------------------------
	// get() bounds checking
	// ----------------------------------------------------------

	@Test
	@DisplayName( "get() throws on negative index" )
	void testGetNegativeIndex() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		assertThrows( IndexOutOfBoundsException.class, () -> list.get( -1 ) );
	}

	@Test
	@DisplayName( "get() throws on index == size" )
	void testGetIndexAtSize() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		assertThrows( IndexOutOfBoundsException.class, () -> list.get( 1 ) );
	}

	@Test
	@DisplayName( "get() throws on empty list" )
	void testGetEmptyList() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		assertThrows( IndexOutOfBoundsException.class, () -> list.get( 0 ) );
	}

	// ----------------------------------------------------------
	// set(int, E)
	// ----------------------------------------------------------

	@Test
	@DisplayName( "set() replaces and returns old value" )
	void testSet() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		String old = list.set( 0, "x" );
		assertThat( old ).isEqualTo( "a" );
		assertThat( list.get( 0 ) ).isEqualTo( "x" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
	}

	@Test
	@DisplayName( "set() to null" )
	void testSetNull() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.set( 0, null );
		assertThat( list.get( 0 ) ).isNull();
	}

	@Test
	@DisplayName( "set() throws on out of bounds" )
	void testSetOutOfBounds() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		assertThrows( IndexOutOfBoundsException.class, () -> list.set( 1, "x" ) );
		assertThrows( IndexOutOfBoundsException.class, () -> list.set( -1, "x" ) );
	}

	@Test
	@DisplayName( "set() across chunk boundary" )
	void testSetAcrossChunks() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 8 );
		for ( int i = 0; i < 15; i++ ) {
			list.add( i );
		}
		list.set( 7, 777 );
		list.set( 12, 1212 );
		assertThat( list.get( 7 ) ).isEqualTo( 777 );
		assertThat( list.get( 12 ) ).isEqualTo( 1212 );
		assertThat( list.get( 6 ) ).isEqualTo( 6 );
		assertThat( list.get( 8 ) ).isEqualTo( 8 );
	}

	// ----------------------------------------------------------
	// add(int, E) — insert
	// ----------------------------------------------------------

	@Test
	@DisplayName( "add(index, element) inserts at beginning" )
	void testInsertAtBeginning() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "b" );
		list.add( "c" );
		list.add( 0, "a" );
		assertThat( list.size() ).isEqualTo( 3 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
		assertThat( list.get( 2 ) ).isEqualTo( "c" );
	}

	@Test
	@DisplayName( "add(index, element) inserts in middle" )
	void testInsertInMiddle() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "c" );
		list.add( 1, "b" );
		assertThat( list.size() ).isEqualTo( 3 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
		assertThat( list.get( 2 ) ).isEqualTo( "c" );
	}

	@Test
	@DisplayName( "add(index, element) inserts at end" )
	void testInsertAtEnd() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( 1, "b" );
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
	}

	@Test
	@DisplayName( "add(index, element) on empty list at index 0" )
	void testInsertOnEmpty() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( 0, "a" );
		assertThat( list.size() ).isEqualTo( 1 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
	}

	@Test
	@DisplayName( "add(index, element) throws on invalid index" )
	void testInsertInvalidIndex() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		assertThrows( IndexOutOfBoundsException.class, () -> list.add( -1, "x" ) );
		assertThrows( IndexOutOfBoundsException.class, () -> list.add( 3, "x" ) );
	}

	// ----------------------------------------------------------
	// remove(int)
	// ----------------------------------------------------------

	@Test
	@DisplayName( "remove() from beginning" )
	void testRemoveFromBeginning() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		String removed = list.remove( 0 );
		assertThat( removed ).isEqualTo( "a" );
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( list.get( 0 ) ).isEqualTo( "b" );
		assertThat( list.get( 1 ) ).isEqualTo( "c" );
	}

	@Test
	@DisplayName( "remove() from middle" )
	void testRemoveFromMiddle() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		String removed = list.remove( 1 );
		assertThat( removed ).isEqualTo( "b" );
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "c" );
	}

	@Test
	@DisplayName( "remove() from end" )
	void testRemoveFromEnd() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		String removed = list.remove( 2 );
		assertThat( removed ).isEqualTo( "c" );
		assertThat( list.size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "remove() last element leaves empty list" )
	void testRemoveLastElement() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.remove( 0 );
		assertThat( list.size() ).isEqualTo( 0 );
		assertThat( list.isEmpty() ).isTrue();
	}

	@Test
	@DisplayName( "remove() throws on out of bounds" )
	void testRemoveOutOfBounds() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		assertThrows( IndexOutOfBoundsException.class, () -> list.remove( 1 ) );
		assertThrows( IndexOutOfBoundsException.class, () -> list.remove( -1 ) );
	}

	// ----------------------------------------------------------
	// clear()
	// ----------------------------------------------------------

	@Test
	@DisplayName( "clear() empties the list" )
	void testClear() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.clear();
		assertThat( list.size() ).isEqualTo( 0 );
		assertThat( list.isEmpty() ).isTrue();
	}

	@Test
	@DisplayName( "clear() then re-add" )
	void testClearAndReAdd() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>( 4 );
		for ( int i = 0; i < 10; i++ ) {
			list.add( "v" + i );
		}
		list.clear();
		list.add( "x" );
		assertThat( list.size() ).isEqualTo( 1 );
		assertThat( list.get( 0 ) ).isEqualTo( "x" );
	}

	// ----------------------------------------------------------
	// trimToSize()
	// ----------------------------------------------------------

	@Test
	@DisplayName( "trimToSize() after removing half the data" )
	void testTrimToSizeAfterRemovingHalf() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 8 );
		// Add 80 items = 10 chunks of 8
		for ( int i = 0; i < 80; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( 80 );
		// Remove the last 40
		for ( int i = 79; i >= 40; i-- ) {
			list.remove( i );
		}
		assertThat( list.size() ).isEqualTo( 40 );
		list.trimToSize();
		// Data should still be intact
		for ( int i = 0; i < 40; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
		// Can still add after trimming
		list.add( 999 );
		assertThat( list.size() ).isEqualTo( 41 );
		assertThat( list.get( 40 ) ).isEqualTo( 999 );
	}

	@Test
	@DisplayName( "trimToSize() on over-allocated list (QoQ pattern)" )
	void testTrimToSizeOverAllocated() {
		// Simulate QoQ pattern: pre-allocate large, fill partially, trim
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 16, 10000 );
		// Only add 500 items out of the 10000 pre-allocated
		for ( int i = 0; i < 500; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( 500 );
		list.trimToSize();
		// All data intact
		for ( int i = 0; i < 500; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	@Test
	@DisplayName( "trimToSize() on empty list" )
	void testTrimToSizeEmpty() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>( 8, 100 );
		list.trimToSize();
		assertThat( list.size() ).isEqualTo( 0 );
		assertThat( list.isEmpty() ).isTrue();
		// Can still add after
		list.add( "a" );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
	}

	@Test
	@DisplayName( "trimToSize() when already at exact size" )
	void testTrimToSizeExactFit() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 8 );
		// Add exactly 8 items (one full chunk)
		for ( int i = 0; i < 8; i++ ) {
			list.add( i );
		}
		list.trimToSize();
		assertThat( list.size() ).isEqualTo( 8 );
		for ( int i = 0; i < 8; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	// ----------------------------------------------------------
	// contains / indexOf / lastIndexOf
	// ----------------------------------------------------------

	@Test
	@DisplayName( "contains() finds elements" )
	void testContains() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		assertThat( list.contains( "a" ) ).isTrue();
		assertThat( list.contains( "b" ) ).isTrue();
		assertThat( list.contains( "c" ) ).isFalse();
	}

	@Test
	@DisplayName( "contains() with null" )
	void testContainsNull() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( null );
		list.add( "a" );
		assertThat( list.contains( null ) ).isTrue();
	}

	@Test
	@DisplayName( "indexOf() returns first occurrence" )
	void testIndexOf() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "a" );
		assertThat( list.indexOf( "a" ) ).isEqualTo( 0 );
		assertThat( list.indexOf( "b" ) ).isEqualTo( 1 );
		assertThat( list.indexOf( "c" ) ).isEqualTo( -1 );
	}

	@Test
	@DisplayName( "lastIndexOf() returns last occurrence" )
	void testLastIndexOf() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "a" );
		assertThat( list.lastIndexOf( "a" ) ).isEqualTo( 2 );
		assertThat( list.lastIndexOf( "b" ) ).isEqualTo( 1 );
		assertThat( list.lastIndexOf( "c" ) ).isEqualTo( -1 );
	}

	// ----------------------------------------------------------
	// toArray()
	// ----------------------------------------------------------

	@Test
	@DisplayName( "toArray() returns correct array" )
	void testToArray() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		Object[] arr = list.toArray();
		assertThat( arr ).asList().containsExactly( "a", "b", "c" ).inOrder();
	}

	@Test
	@DisplayName( "toArray() on empty list" )
	void testToArrayEmpty() {
		ChunkedArrayList<String>	list	= new ChunkedArrayList<>();
		Object[]					arr		= list.toArray();
		assertThat( arr ).isEmpty();
	}

	@Test
	@DisplayName( "toArray() across multiple chunks" )
	void testToArrayMultipleChunks() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 8 );
		for ( int i = 0; i < 17; i++ ) {
			list.add( i );
		}
		Object[] arr = list.toArray();
		assertThat( arr ).hasLength( 17 );
		for ( int i = 0; i < 17; i++ ) {
			assertThat( arr[ i ] ).isEqualTo( i );
		}
	}

	@Test
	@DisplayName( "toArray(T[]) with correct size" )
	void testToArrayTyped() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		String[] arr = list.toArray( new String[ 2 ] );
		assertThat( arr ).asList().containsExactly( "a", "b" ).inOrder();
	}

	@Test
	@DisplayName( "toArray(T[]) with smaller array allocates new" )
	void testToArrayTypedSmaller() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		String[] arr = list.toArray( new String[ 0 ] );
		assertThat( arr ).hasLength( 2 );
		assertThat( arr[ 0 ] ).isEqualTo( "a" );
		assertThat( arr[ 1 ] ).isEqualTo( "b" );
	}

	@Test
	@DisplayName( "toArray(T[]) with larger array sets null terminator" )
	void testToArrayTypedLarger() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		String[] arr = list.toArray( new String[ 5 ] );
		assertThat( arr[ 0 ] ).isEqualTo( "a" );
		assertThat( arr[ 1 ] ).isNull();
	}

	// ----------------------------------------------------------
	// Iterator
	// ----------------------------------------------------------

	@Test
	@DisplayName( "iterator() traverses all elements" )
	void testIterator() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		Iterator<String>	it		= list.iterator();
		List<String>		result	= new ArrayList<>();
		while ( it.hasNext() ) {
			result.add( it.next() );
		}
		assertThat( result ).containsExactly( "a", "b", "c" ).inOrder();
	}

	@Test
	@DisplayName( "iterator() on empty list" )
	void testIteratorEmpty() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		assertThat( list.iterator().hasNext() ).isFalse();
	}

	@Test
	@DisplayName( "iterator().next() throws NoSuchElementException when exhausted" )
	void testIteratorExhausted() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		Iterator<String> it = list.iterator();
		it.next();
		assertThrows( NoSuchElementException.class, () -> it.next() );
	}

	@Test
	@DisplayName( "for-each loop works" )
	void testForEach() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		list.add( 1 );
		list.add( 2 );
		list.add( 3 );
		int sum = 0;
		for ( int val : list ) {
			sum += val;
		}
		assertThat( sum ).isEqualTo( 6 );
	}

	// ----------------------------------------------------------
	// ListIterator
	// ----------------------------------------------------------

	@Test
	@DisplayName( "listIterator() traverses forward" )
	void testListIteratorForward() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		ListIterator<String> li = list.listIterator();
		assertThat( li.nextIndex() ).isEqualTo( 0 );
		assertThat( li.next() ).isEqualTo( "a" );
		assertThat( li.next() ).isEqualTo( "b" );
		assertThat( li.next() ).isEqualTo( "c" );
		assertThat( li.hasNext() ).isFalse();
	}

	@Test
	@DisplayName( "listIterator() traverses backward" )
	void testListIteratorBackward() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		ListIterator<String> li = list.listIterator( 3 );
		assertThat( li.previous() ).isEqualTo( "c" );
		assertThat( li.previous() ).isEqualTo( "b" );
		assertThat( li.previous() ).isEqualTo( "a" );
		assertThat( li.hasPrevious() ).isFalse();
	}

	@Test
	@DisplayName( "listIterator(index) starts at correct position" )
	void testListIteratorAtIndex() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		ListIterator<String> li = list.listIterator( 1 );
		assertThat( li.next() ).isEqualTo( "b" );
	}

	// ----------------------------------------------------------
	// subList
	// ----------------------------------------------------------

	@Test
	@DisplayName( "subList() returns correct view" )
	void testSubList() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		list.add( "d" );
		List<String> sub = list.subList( 1, 3 );
		assertThat( sub ).containsExactly( "b", "c" ).inOrder();
	}

	// ----------------------------------------------------------
	// addAll
	// ----------------------------------------------------------

	@Test
	@DisplayName( "addAll() appends collection" )
	void testAddAll() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.addAll( Arrays.asList( "b", "c", "d" ) );
		assertThat( list.size() ).isEqualTo( 4 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
		assertThat( list.get( 3 ) ).isEqualTo( "d" );
	}

	@Test
	@DisplayName( "addAll(index) inserts at position" )
	void testAddAllAtIndex() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "d" );
		list.addAll( 1, Arrays.asList( "b", "c" ) );
		assertThat( list.size() ).isEqualTo( 4 );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
		assertThat( list.get( 2 ) ).isEqualTo( "c" );
		assertThat( list.get( 3 ) ).isEqualTo( "d" );
	}

	// ----------------------------------------------------------
	// containsAll / removeAll / retainAll (from AbstractList)
	// ----------------------------------------------------------

	@Test
	@DisplayName( "containsAll() works" )
	void testContainsAll() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		assertThat( list.containsAll( Arrays.asList( "a", "c" ) ) ).isTrue();
		assertThat( list.containsAll( Arrays.asList( "a", "x" ) ) ).isFalse();
	}

	// ----------------------------------------------------------
	// remove(Object) — inherited from AbstractList
	// ----------------------------------------------------------

	@Test
	@DisplayName( "remove(Object) removes first occurrence" )
	void testRemoveObject() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "a" );
		boolean removed = list.remove( "a" );
		assertThat( removed ).isTrue();
		assertThat( list.size() ).isEqualTo( 2 );
		assertThat( list.get( 0 ) ).isEqualTo( "b" );
		assertThat( list.get( 1 ) ).isEqualTo( "a" );
	}

	@Test
	@DisplayName( "remove(Object) returns false for missing element" )
	void testRemoveObjectMissing() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		assertThat( list.remove( "x" ) ).isFalse();
		assertThat( list.size() ).isEqualTo( 1 );
	}

	// ----------------------------------------------------------
	// equals / hashCode
	// ----------------------------------------------------------

	@Test
	@DisplayName( "equals() matches ArrayList with same content" )
	void testEquals() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		List<String> arrayList = new ArrayList<>( Arrays.asList( "a", "b" ) );
		assertThat( list ).isEqualTo( arrayList );
		assertThat( arrayList ).isEqualTo( list );
	}

	@Test
	@DisplayName( "hashCode() matches ArrayList with same content" )
	void testHashCode() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		List<String> arrayList = new ArrayList<>( Arrays.asList( "a", "b" ) );
		assertThat( list.hashCode() ).isEqualTo( arrayList.hashCode() );
	}

	// ----------------------------------------------------------
	// Collections utility methods
	// ----------------------------------------------------------

	@Test
	@DisplayName( "Collections.sort() works" )
	void testSort() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		list.add( 3 );
		list.add( 1 );
		list.add( 2 );
		Collections.sort( list );
		assertThat( list.get( 0 ) ).isEqualTo( 1 );
		assertThat( list.get( 1 ) ).isEqualTo( 2 );
		assertThat( list.get( 2 ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Collections.reverse() works" )
	void testReverse() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>();
		list.add( "a" );
		list.add( "b" );
		list.add( "c" );
		Collections.reverse( list );
		assertThat( list.get( 0 ) ).isEqualTo( "c" );
		assertThat( list.get( 1 ) ).isEqualTo( "b" );
		assertThat( list.get( 2 ) ).isEqualTo( "a" );
	}

	@Test
	@DisplayName( "Collections.shuffle() doesn't crash" )
	void testShuffle() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		for ( int i = 0; i < 20; i++ ) {
			list.add( i );
		}
		Collections.shuffle( list );
		assertThat( list.size() ).isEqualTo( 20 );
	}

	// ----------------------------------------------------------
	// Stream support
	// ----------------------------------------------------------

	@Test
	@DisplayName( "stream() works" )
	void testStream() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			list.add( i );
		}
		int sum = list.stream().mapToInt( Integer::intValue ).sum();
		assertThat( sum ).isEqualTo( 45 );
	}

	@Test
	@DisplayName( "parallelStream() works" )
	void testParallelStream() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		for ( int i = 0; i < 100; i++ ) {
			list.add( i );
		}
		int sum = list.parallelStream().mapToInt( Integer::intValue ).sum();
		assertThat( sum ).isEqualTo( 4950 );
	}

	// ----------------------------------------------------------
	// Large data set (default chunk size)
	// ----------------------------------------------------------

	@Test
	@DisplayName( "10,000 items with default chunk size" )
	void testLargeDataSet() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		for ( int i = 0; i < 10_000; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( 10_000 );
		assertThat( list.get( 0 ) ).isEqualTo( 0 );
		assertThat( list.get( 999 ) ).isEqualTo( 999 );
		assertThat( list.get( 1000 ) ).isEqualTo( 1000 );
		assertThat( list.get( 5555 ) ).isEqualTo( 5555 );
		assertThat( list.get( 9999 ) ).isEqualTo( 9999 );
		Object[] arr = list.toArray();
		assertThat( arr ).hasLength( 10_000 );
		assertThat( arr[ 9999 ] ).isEqualTo( 9999 );
	}

	// ----------------------------------------------------------
	// Concurrent add()
	// ----------------------------------------------------------

	@Test
	@DisplayName( "concurrent add() from multiple threads preserves all data" )
	void testConcurrentAdd() throws InterruptedException {
		int							threadCount	= 8;
		int							perThread	= 5000;
		int							total		= threadCount * perThread;
		ChunkedArrayList<Integer>	list		= new ChunkedArrayList<>();
		CountDownLatch				startLatch	= new CountDownLatch( 1 );
		CountDownLatch				doneLatch	= new CountDownLatch( threadCount );

		for ( int t = 0; t < threadCount; t++ ) {
			final int threadId = t;
			new Thread( () -> {
				try {
					startLatch.await();
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
				}
				for ( int i = 0; i < perThread; i++ ) {
					list.add( threadId * perThread + i );
				}
				doneLatch.countDown();
			} ).start();
		}

		startLatch.countDown();
		doneLatch.await();

		assertThat( list.size() ).isEqualTo( total );

		// Verify all values present (order doesn't matter for concurrent adds)
		ConcurrentHashMap<Integer, Boolean> seen = new ConcurrentHashMap<>();
		for ( int i = 0; i < total; i++ ) {
			Integer val = list.get( i );
			assertThat( val ).isNotNull();
			seen.put( val, true );
		}
		assertThat( seen.size() ).isEqualTo( total );
	}

	@Test
	@DisplayName( "concurrent add() with small chunk size forces many chunk allocations" )
	void testConcurrentAddSmallChunks() throws InterruptedException {
		int							threadCount	= 4;
		int							perThread	= 500;
		int							total		= threadCount * perThread;
		ChunkedArrayList<Integer>	list		= new ChunkedArrayList<>( 16 );
		CountDownLatch				startLatch	= new CountDownLatch( 1 );
		CountDownLatch				doneLatch	= new CountDownLatch( threadCount );

		for ( int t = 0; t < threadCount; t++ ) {
			final int threadId = t;
			new Thread( () -> {
				try {
					startLatch.await();
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
				}
				for ( int i = 0; i < perThread; i++ ) {
					list.add( threadId * perThread + i );
				}
				doneLatch.countDown();
			} ).start();
		}

		startLatch.countDown();
		doneLatch.await();

		assertThat( list.size() ).isEqualTo( total );
		for ( int i = 0; i < total; i++ ) {
			assertThat( list.get( i ) ).isNotNull();
		}
	}

	// ----------------------------------------------------------
	// Concurrent add() + set()
	// ----------------------------------------------------------

	@Test
	@DisplayName( "concurrent add() and set() do not lose data" )
	void testConcurrentAddAndSet() throws InterruptedException {
		int							count	= 1000;
		ChunkedArrayList<Integer>	list	= new ChunkedArrayList<>( 64 );

		// Pre-populate
		for ( int i = 0; i < count; i++ ) {
			list.add( i );
		}

		CountDownLatch	startLatch	= new CountDownLatch( 1 );
		CountDownLatch	doneLatch	= new CountDownLatch( 2 );

		// Thread 1: adds more items
		new Thread( () -> {
			try {
				startLatch.await();
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
			for ( int i = 0; i < count; i++ ) {
				list.add( count + i );
			}
			doneLatch.countDown();
		} ).start();

		// Thread 2: sets existing items
		new Thread( () -> {
			try {
				startLatch.await();
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
			for ( int i = 0; i < count; i++ ) {
				list.set( i, i + 10000 );
			}
			doneLatch.countDown();
		} ).start();

		startLatch.countDown();
		doneLatch.await();

		assertThat( list.size() ).isEqualTo( count * 2 );
		// Verify the sets took effect
		for ( int i = 0; i < count; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i + 10000 );
		}
		// Verify the adds are all non-null
		for ( int i = count; i < count * 2; i++ ) {
			assertThat( list.get( i ) ).isNotNull();
		}
	}

	// ----------------------------------------------------------
	// Edge cases
	// ----------------------------------------------------------

	@Test
	@DisplayName( "works with chunkSize = 1" )
	void testChunkSizeOne() {
		ChunkedArrayList<String> list = new ChunkedArrayList<>( 1 );
		list.add( "a" );
		assertThat( list.get( 0 ) ).isEqualTo( "a" );
		assertThat( list.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "works with chunkSize = 2" )
	void testChunkSizeTwo() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 2 );
		for ( int i = 0; i < 20; i++ ) {
			list.add( i );
		}
		assertThat( list.size() ).isEqualTo( 20 );
		for ( int i = 0; i < 20; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	@Test
	@DisplayName( "Integer types box/unbox correctly" )
	void testIntegerBoxing() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		list.add( 42 );
		int val = list.get( 0 );
		assertThat( val ).isEqualTo( 42 );
	}

	@Test
	@DisplayName( "Mixed types via raw list" )
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	void testMixedTypes() {
		ChunkedArrayList list = new ChunkedArrayList<>();
		list.add( "string" );
		list.add( 123 );
		list.add( 3.14 );
		assertThat( list.get( 0 ) ).isEqualTo( "string" );
		assertThat( list.get( 1 ) ).isEqualTo( 123 );
		assertThat( list.get( 2 ) ).isEqualTo( 3.14 );
	}

	@Test
	@DisplayName( "Repeated add and remove cycles" )
	void testAddRemoveCycles() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>( 4 );
		for ( int cycle = 0; cycle < 3; cycle++ ) {
			for ( int i = 0; i < 20; i++ ) {
				list.add( i );
			}
			while ( list.size() > 0 ) {
				list.remove( list.size() - 1 );
			}
			assertThat( list.isEmpty() ).isTrue();
		}
	}

	@Test
	@DisplayName( "Collect stream into ChunkedArrayList via addAll" )
	void testStreamCollect() {
		ChunkedArrayList<Integer> list = new ChunkedArrayList<>();
		list.addAll( IntStream.range( 0, 100 ).boxed().toList() );
		assertThat( list.size() ).isEqualTo( 100 );
		for ( int i = 0; i < 100; i++ ) {
			assertThat( list.get( i ) ).isEqualTo( i );
		}
	}

	// ----------------------------------------------------------
	// Benchmark: 1M concurrent adds + random access, varying chunk sizes
	// ----------------------------------------------------------

	@Test
	@DisplayName( "benchmark: 1M concurrent adds + random reads across chunk sizes" )
	@Disabled( "Benchmark test - not meant for regular unit test runs" )
	void benchmarkChunkSizes() throws InterruptedException {
		int		itemCount	= 1_000_000;
		int		threadCount	= 8;
		int		perThread	= itemCount / threadCount;
		int[]	chunkSizes	= { 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192 };

		System.out.println( "\n========== ChunkedArrayList Benchmark: 1M items, " + threadCount + " threads ==========" );
		System.out.printf( "%-12s %15s %15s %15s%n", "ChunkSize", "Add (ms)", "Read (ms)", "Total (ms)" );
		System.out.println( "-------------------------------------------------------------" );

		for ( int cs : chunkSizes ) {
			// Warm up: small run to let JIT kick in
			warmUp( cs, threadCount, 10_000 );

			ChunkedArrayList<Integer>	list		= new ChunkedArrayList<>( cs );
			CountDownLatch				startLatch	= new CountDownLatch( 1 );
			CountDownLatch				doneLatch	= new CountDownLatch( threadCount );

			// --- Concurrent add phase ---
			for ( int t = 0; t < threadCount; t++ ) {
				final int threadId = t;
				new Thread( () -> {
					try {
						startLatch.await();
					} catch ( InterruptedException e ) {
						Thread.currentThread().interrupt();
					}
					for ( int i = 0; i < perThread; i++ ) {
						list.add( threadId * perThread + i );
					}
					doneLatch.countDown();
				} ).start();
			}

			long addStart = System.nanoTime();
			startLatch.countDown();
			doneLatch.await();
			long addMs = ( System.nanoTime() - addStart ) / 1_000_000;

			assertThat( list.size() ).isEqualTo( itemCount );

			// --- Random read phase ---
			Random	rng		= new Random( 42 );
			int[]	indices	= new int[ itemCount ];
			for ( int i = 0; i < itemCount; i++ ) {
				indices[ i ] = rng.nextInt( itemCount );
			}

			long	readStart	= System.nanoTime();
			long	checksum	= 0;
			for ( int i = 0; i < itemCount; i++ ) {
				Integer val = list.get( indices[ i ] );
				assertThat( val ).isNotNull();
				checksum += val;
			}
			long readMs = ( System.nanoTime() - readStart ) / 1_000_000;

			// Use checksum to prevent dead code elimination
			assertThat( checksum ).isNotEqualTo( Long.MIN_VALUE );

			System.out.printf( "%-12d %12d ms %12d ms %12d ms%n", cs, addMs, readMs, addMs + readMs );
		}

		System.out.println( "=============================================================\n" );
	}

	/**
	 * Small warm-up run to trigger JIT compilation before the real benchmark
	 */
	private void warmUp( int chunkSize, int threadCount, int itemCount ) throws InterruptedException {
		ChunkedArrayList<Integer>	list		= new ChunkedArrayList<>( chunkSize );
		CountDownLatch				startLatch	= new CountDownLatch( 1 );
		CountDownLatch				doneLatch	= new CountDownLatch( threadCount );
		int							perThread	= itemCount / threadCount;

		for ( int t = 0; t < threadCount; t++ ) {
			final int threadId = t;
			new Thread( () -> {
				try {
					startLatch.await();
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
				}
				for ( int i = 0; i < perThread; i++ ) {
					list.add( threadId * perThread + i );
				}
				doneLatch.countDown();
			} ).start();
		}
		startLatch.countDown();
		doneLatch.await();

		// Touch all values to warm up get path
		Random rng = new Random( 0 );
		for ( int i = 0; i < itemCount; i++ ) {
			list.get( rng.nextInt( itemCount ) );
		}
	}
}
