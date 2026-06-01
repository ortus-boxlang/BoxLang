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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Range;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class IterableCasterTest {

	@DisplayName( "It can cast a List to an Iterable" )
	@Test
	void testItCanCastAList() {
		Iterable<Object>	result	= IterableCaster.cast( Arrays.asList( "Brad", "Wood" ) );
		Iterator<Object>	it		= result.iterator();
		assertThat( it.next() ).isEqualTo( "Brad" );
		assertThat( it.next() ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can cast an Object array to an Iterable" )
	@Test
	void testItCanCastAnArray() {
		Iterable<Object>	result	= IterableCaster.cast( new Object[] { "Brad", "Wood" } );
		Iterator<Object>	it		= result.iterator();
		assertThat( it.next() ).isEqualTo( "Brad" );
		assertThat( it.next() ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can cast a primitive int array to an Iterable" )
	@Test
	void testItCanCastPrimitiveArray() {
		Iterable<Object>	result	= IterableCaster.cast( new int[] { 1, 2, 3 } );
		Iterator<Object>	it		= result.iterator();
		assertThat( it.next() ).isEqualTo( 1 );
		assertThat( it.next() ).isEqualTo( 2 );
		assertThat( it.next() ).isEqualTo( 3 );
	}

	@DisplayName( "It can cast a Map to an Iterable of keys" )
	@Test
	void testItCanCastAMap() {
		Iterable<Object>	result	= IterableCaster.cast( Map.of( "Brad", "Wood", "Luis", "Majano" ) );
		List<Object>		keys	= new ArrayList<>();
		result.forEach( keys::add );
		assertThat( keys ).containsExactly( "Brad", "Luis" );
	}

	@DisplayName( "It can cast a Scope to an Iterable of key names" )
	@Test
	void testItCanCastAScope() {
		IScope scope = new VariablesScope();
		scope.putAll( Map.of( Key.of( "Brad" ), "Wood", Key.of( "Luis" ), "Majano" ) );
		Iterable<Object>	result	= IterableCaster.cast( scope );
		List<Object>		keys	= new ArrayList<>();
		result.forEach( keys::add );
		assertThat( keys ).containsExactly( "Brad", "Luis" );
	}

	@DisplayName( "It returns a Range directly without materializing it" )
	@Test
	void testItReturnsRangeDirectly() {
		Range<?>			range	= Range.of( 1, 5 );
		Iterable<Object>	result	= IterableCaster.cast( range );
		// The returned iterable should be the same Range instance, not a realized array
		assertThat( result ).isSameInstanceAs( range );
	}

	@DisplayName( "It can iterate a Range lazily" )
	@Test
	void testItCanIterateRangeLazily() {
		Iterable<Object>	result	= IterableCaster.cast( Range.of( 1, 5 ) );
		List<Object>		items	= new ArrayList<>();
		result.forEach( items::add );
		assertThat( items ).containsExactly( 1, 2, 3, 4, 5 ).inOrder();
	}

	@DisplayName( "It can cast a string to an Iterable (comma-delimited list)" )
	@Test
	void testItCanCastAString() {
		Iterable<Object>	result	= IterableCaster.cast( "Brad,Wood" );
		List<Object>		items	= new ArrayList<>();
		result.forEach( items::add );
		assertThat( items ).containsExactly( "Brad", "Wood" ).inOrder();
	}

	@DisplayName( "Casting null throws when fail is true" )
	@Test
	void testNullThrows() {
		assertThrows( BoxCastException.class, () -> IterableCaster.cast( null ) );
	}

	@DisplayName( "Casting null returns null when fail is false" )
	@Test
	void testNullReturnsNull() {
		assertNull( IterableCaster.cast( null, false ) );
	}

	@DisplayName( "Casting an unknown type throws when fail is true" )
	@Test
	void testUnknownTypeThrows() {
		assertThrows( BoxCastException.class, () -> IterableCaster.cast( new Object() ) );
	}

	@DisplayName( "Casting an unknown type returns null when fail is false" )
	@Test
	void testUnknownTypeReturnsNull() {
		assertNull( IterableCaster.cast( new Object(), false ) );
	}

	@DisplayName( "attempt() returns successful for valid input" )
	@Test
	void testAttemptSuccessful() {
		CastAttempt<Iterable<Object>> attempt = IterableCaster.attempt( Arrays.asList( "a", "b" ) );
		assertThat( attempt.wasSuccessful() ).isTrue();
	}

	@DisplayName( "attempt() returns unsuccessful for invalid input" )
	@Test
	void testAttemptUnsuccessful() {
		CastAttempt<Iterable<Object>> attempt = IterableCaster.attempt( new Object() );
		assertThat( attempt.wasSuccessful() ).isFalse();
	}

	@DisplayName( "It handles any Iterable implementation" )
	@Test
	void testItHandlesAnyIterable() {
		// A custom Iterable that is NOT a Collection
		Iterable<Object>	customIterable	= () -> List.<Object>of( "x", "y", "z" ).iterator();
		Iterable<Object>	result			= IterableCaster.cast( customIterable );
		assertThat( result ).isSameInstanceAs( customIterable );
		List<Object> items = new ArrayList<>();
		result.forEach( items::add );
		assertThat( items ).containsExactly( "x", "y", "z" ).inOrder();
	}
}
