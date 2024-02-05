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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

class StructTest {

	@DisplayName( "Test equals and hash code with no data" )
	@Test
	void testEqualsAndHashCode() {
		IStruct	struct1	= new Struct();
		IStruct	struct2	= new Struct();

		// Test equals()
		assertThat( struct1 ).isEqualTo( struct2 );

		// Test hashCode()
		assertThat( struct1.hashCode() ).isEqualTo( struct2.hashCode() );
	}

	@DisplayName( "Test equals and hash code with data" )
	@Test
	void testEqualsAndHashCodeWithData() {
		IStruct struct1 = new Struct();
		struct1.put( Key.of( "name" ), "boxlang" );
		IStruct struct2 = new Struct();
		struct2.put( Key.of( "name" ), "boxlang" );

		// Test equals()
		assertThat( struct1 ).isEqualTo( struct2 );

		// Test hashCode()
		assertThat( struct1.hashCode() ).isEqualTo( struct2.hashCode() );
	}

	@Test
	void testToString() {
		IStruct struct = new Struct();
		struct.put( Key.of( "name" ), "BoxLang" );
		struct.put( Key.of( "AgE" ), 1 );
		struct.put( Key.of( "Location" ), "Spain" );

		// Test toString()
		String actual = struct.toString();
		System.out.println( actual );
		assertThat( actual ).contains( "NAME=BoxLang" );
		assertThat( actual ).contains( "AGE=1" );
		assertThat( actual ).contains( "LOCATION=Spain" );
	}

	@Test
	void testToStringWithCase() {
		IStruct struct = new Struct();
		struct.put( Key.of( "name" ), "BoxLang" );
		struct.put( Key.of( "AgE" ), 1 );
		struct.put( Key.of( "Location" ), "Spain" );

		// Test toString()
		String actual = struct.toStringWithCase();
		System.out.println( actual );
		assertThat( actual ).contains( "name=BoxLang" );
		assertThat( actual ).contains( "AgE=1" );
		assertThat( actual ).contains( "Location=Spain" );
	}

	@Test
	void testCanHandleNull() {
		IStruct	struct	= new Struct();
		Key		key		= Key.of( "nully" );
		struct.put( key, null );

		assertThat( struct.get( key ) ).isEqualTo( null );
	}

	@DisplayName( "Can create a struct from name-value pairs" )
	@Test
	void testCanStructOf() {
		IStruct struct = Struct.of(
		    "foo", "bar",
		    Key.of( "baz" ), "bum",
		    5D, "Brad"
		);

		assertThat( struct.size() ).isEqualTo( 3 );
		assertThat( struct.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( struct.get( Key.of( "baz" ) ) ).isEqualTo( "bum" );
		assertThat( struct.get( Key.of( "5" ) ) ).isEqualTo( "Brad" );

		assertThrows( Throwable.class, () -> Struct.of( "test" ) );
		assertThrows( Throwable.class, () -> Struct.of( null, "foo" ) );
		assertThrows( Throwable.class, () -> Struct.of( new HashMap<Object, Object>(), "foo" ) );

		struct = Struct.of();
		assertThat( struct.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can create a linked struct from name-value pairs" )
	@Test
	void testCanLinkedStructOf() {
		IStruct struct = Struct.linkedOf(
		    "foo", "bar",
		    Key.of( "baz" ), "bum",
		    5D, "Brad"
		);

		assertThat( struct.size() ).isEqualTo( 3 );
		assertThat( struct.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( struct.get( Key.of( "baz" ) ) ).isEqualTo( "bum" );
		assertThat( struct.get( Key.of( "5" ) ) ).isEqualTo( "Brad" );
		// Verify the keys are in insertion order
		assertThat( struct.keySet().toArray()[ 0 ] ).isEqualTo( Key.of( "foo" ) );
		assertThat( struct.keySet().toArray()[ 1 ] ).isEqualTo( Key.of( "baz" ) );
		assertThat( struct.keySet().toArray()[ 2 ] ).isEqualTo( Key.of( "5" ) );

		assertThrows( Throwable.class, () -> Struct.linkedOf( "test" ) );
		assertThrows( Throwable.class, () -> Struct.linkedOf( null, "foo" ) );
		assertThrows( Throwable.class, () -> Struct.linkedOf( new HashMap<Object, Object>(), "foo" ) );

		struct = Struct.linkedOf();
		assertThat( struct.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can create a sorted struct from name-value pairs" )
	@Test
	void testCanSortedStructOf() {
		Comparator<Key>	comp	= ( k1, k2 ) -> Integer.compare( k2.getName().length(), k1.getName().length() );
		IStruct			struct	= Struct.sortedOf(
		    comp,
		    "foo", "bar",
		    5D, "Brad",
		    Key.of( "luismajano" ), "test"
		);

		assertThat( struct.size() ).isEqualTo( 3 );
		assertThat( struct.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( struct.get( Key.of( "5" ) ) ).isEqualTo( "Brad" );
		assertThat( struct.get( Key.of( "luismajano" ) ) ).isEqualTo( "test" );
		// Verify the keys are in key length order
		assertThat( struct.keySet().toArray()[ 0 ] ).isEqualTo( Key.of( "luismajano" ) );
		assertThat( struct.keySet().toArray()[ 1 ] ).isEqualTo( Key.of( "foo" ) );
		assertThat( struct.keySet().toArray()[ 2 ] ).isEqualTo( Key.of( "5" ) );

		assertThrows( Throwable.class, () -> Struct.sortedOf( comp, "test" ) );
		assertThrows( Throwable.class, () -> Struct.sortedOf( comp, null, "foo" ) );
		assertThrows( Throwable.class, () -> Struct.sortedOf( comp, new HashMap<Object, Object>(), "foo" ) );

		struct = Struct.sortedOf( null );
		assertThat( struct.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can create a case-sensitive struct and perform key comparisons and deletions" )
	@Test
	void testCanCompareCaseSensitiveKeys() {
		IStruct struct = new Struct( Struct.TYPES.CASESENSITIVE );

		struct.put( "foo", "bar" );
		struct.put( "flea", "flah" );

		assertThat( struct.size() ).isEqualTo( 2 );
		System.out.println( struct.asString() );
		assertTrue( struct.containsKey( "foo" ) );
		assertFalse( struct.containsKey( "fOO" ) );
		assertTrue( struct.containsKey( "flea" ) );
		assertFalse( struct.containsKey( "Fleah" ) );
		struct.remove( "FOO" );
		assertThat( struct.size() ).isEqualTo( 2 );
		struct.remove( "foo" );
		assertThat( struct.size() ).isEqualTo( 1 );

	}

	@DisplayName( "Can create a default struct in the constructor" )
	@Test
	void testCanCreateDefaultStruct() {
		IStruct struct = new Struct();
		assertThat( struct.size() ).isEqualTo( 0 );
		assertThat( struct.getType() ).isEqualTo( Struct.TYPES.DEFAULT );
		assertThat( struct.getWrapped() ).isInstanceOf( ConcurrentHashMap.class );
	}

	@DisplayName( "Can create a linked struct in the constructor" )
	@Test
	void testCanCreateLinkedStruct() {
		IStruct struct = new Struct( Struct.TYPES.LINKED );
		assertThat( struct.size() ).isEqualTo( 0 );
		assertThat( struct.getType() ).isEqualTo( Struct.TYPES.LINKED );
		assertThat( struct.getWrapped() ).isNotInstanceOf( ConcurrentHashMap.class );
	}

	@DisplayName( "Can create a sorted struct in the constructor" )
	@Test
	void testCanCreateSortedStruct() {
		IStruct struct = new Struct( Struct.TYPES.SORTED );
		assertThat( struct.size() ).isEqualTo( 0 );
		assertThat( struct.getType() ).isEqualTo( Struct.TYPES.SORTED );
		assertThat( struct.getWrapped() ).isNotInstanceOf( ConcurrentHashMap.class );
	}

	@DisplayName( "Can create a sorted struct with a comparator" )
	@Test
	void testCanCreateSortedStructWithComparator() {
		IStruct struct = new Struct( ( k1, k2 ) -> Integer.compare( k2.getName().length(), k1.getName().length() ) );
		assertThat( struct.size() ).isEqualTo( 0 );
		assertThat( struct.getType() ).isEqualTo( Struct.TYPES.SORTED );
		struct.put( "/hello", "test" );
		struct.put( "/hello/again", "test" );
		struct.put( "/hello/again/and/again", "test" );

		// Verify the keys are ordered by length
		assertThat( struct.keySet().toArray()[ 0 ] ).isEqualTo( Key.of( "/hello/again/and/again" ) );
		assertThat( struct.keySet().toArray()[ 1 ] ).isEqualTo( Key.of( "/hello/again" ) );
		assertThat( struct.keySet().toArray()[ 2 ] ).isEqualTo( Key.of( "/hello" ) );
	}

	@DisplayName( "Can create a case-sensitive struct" )
	@Test
	void testCanCreateCaseSensitiveStruct() {
		IStruct struct = new Struct( Struct.TYPES.CASESENSITIVE );
		assertThat( struct.size() ).isEqualTo( 0 );
		assertThat( struct.getType() ).isEqualTo( Struct.TYPES.CASESENSITIVE );
		assertThat( struct.getWrapped() ).isInstanceOf( ConcurrentHashMap.class );
	}

}
