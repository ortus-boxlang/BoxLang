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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DelimitedArrayTest {

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		DelimitedArray array = new DelimitedArray();
		assertThat( array.size() ).isEqualTo( 0 );
		array.add( "foo" );
		int i = array.append( "bar" );
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( i ).isEqualTo( 2 );

		array = new DelimitedArray( new String[] { "foo" } );
		assertThat( array.size() ).isEqualTo( 1 );
		assertThat( array.asString() ).isEqualTo( "foo" );

		array = DelimitedArray.of( new String[] { "foo", "|", "bar" } );
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.asString() ).isEqualTo( "foo|bar" );

		array = DelimitedArray.of( new String[] { "foo", "|", "bar", ",", "baz", ",", "bum" } );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "foo|bar,baz,bum" );
	}

	@Test
	void testAdd() {
		DelimitedArray array = new DelimitedArray();
		array.add( "foo" );
		array.add( "bar", "," );
		array.add( "baz", "|" );
		assertThat( array.size() ).isEqualTo( 3 );
		assertThat( array.asString() ).isEqualTo( "foo,bar|baz" );
	}

	@Test
	void testAddIndex() {
		DelimitedArray array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.withDelimiter( "z" ).add( 0, "42" );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "42z1a2b3" );

		array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.withDelimiter( "z" ).add( 2, "42" );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "1a2z42b3" );

		array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.withDelimiter( "z" ).add( 1, "42" );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "1z42a2b3" );
	}

	@Test
	void testAddIndexDelimiter() {
		DelimitedArray array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.add( 0, "42", "z" );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "42z1a2b3" );

		array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.add( 2, "42", "z" );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "1a2z42b3" );

		array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.add( 1, "42", "z" );
		assertThat( array.size() ).isEqualTo( 4 );
		assertThat( array.asString() ).isEqualTo( "1z42a2b3" );
	}

	@Test
	void testRemoveObject() {
		DelimitedArray array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.remove( "2" );
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.asString() ).isEqualTo( "1a3" );
	}

	@Test
	void testRemoveIndex() {
		DelimitedArray array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.remove( 1 );
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( array.asString() ).isEqualTo( "1a3" );
	}

	@Test
	void testClear() {
		DelimitedArray array = DelimitedArray.of( "1", "a", "2", "b", "3" );
		array.clear();
		assertThat( array.size() ).isEqualTo( 0 );
		assertThat( array.asString() ).isEqualTo( "" );
	}

	@Test
	void testReverse() {
		DelimitedArray	array		= DelimitedArray.of( "1", "a", "2", "b", "3" );
		DelimitedArray	reversed	= array.reverse();
		assertThat( reversed.size() ).isEqualTo( 3 );
		assertThat( reversed.asString() ).isEqualTo( "3b2a1" );
	}

	@Test
	void testHashCode() {
		DelimitedArray	array				= DelimitedArray.of( "1", "a", "2", "b", "3" );
		int				hashCode			= array.hashCode();
		DelimitedArray	anotherArray		= DelimitedArray.of( "1", "a", "2", "b", "3" );
		int				anotherHashCode		= anotherArray.hashCode();
		DelimitedArray	differentArray		= DelimitedArray.of( "1", ",", "2", ",", "4" );
		int				differentHashCode	= differentArray.hashCode();
		assertThat( hashCode ).isEqualTo( anotherHashCode );
		assertThat( hashCode ).isNotEqualTo( differentHashCode );
	}

	@Test
	void testAddAll() {
		DelimitedArray	array	= DelimitedArray.of( "1", "a", "2" );
		List<String>	toAdd	= List.of( "foo", "bar", "baz" );

		boolean			result	= array.withDelimiter( "," ).addAll( toAdd );

		assertThat( result ).isTrue();
		assertThat( array.size() ).isEqualTo( 5 );
		assertThat( array.asString() ).isEqualTo( "1a2,foo,bar,baz" );
	}

	@Test
	void testAddAllAtIndex() {
		DelimitedArray	array	= DelimitedArray.of( "1", "a", "2", "b", "3" );
		List<String>	toAdd	= List.of( "foo", "bar" );

		boolean			result	= array.withDelimiter( "," ).addAll( 1, toAdd );

		assertThat( result ).isTrue();
		assertThat( array.size() ).isEqualTo( 5 );
		assertThat( array.asString() ).isEqualTo( "1,foo,bara2b3" );
	}

	@Test
	void testRemoveAll() {
		DelimitedArray	array		= DelimitedArray.of( "0", ",", "1", ",", "2", ":", "3", "-", "1", "|", "5" );
		List<String>	toRemove	= List.of( "1", "3" );

		boolean			result		= array.removeAll( toRemove );

		assertThat( result ).isTrue();
		assertThat( array.size() ).isEqualTo( 3 );
		assertThat( array.asString() ).isEqualTo( "0,2:5" );
	}

	@Test
	void testRetainAll() {
		DelimitedArray	array		= DelimitedArray.of( "1", "a", "2", "b", "3", "c", "1" );
		List<String>	toRetain	= List.of( "1", "2" );

		boolean			result		= array.retainAll( toRetain );

		assertThat( result ).isTrue();
		assertThat( array.size() ).isEqualTo( 3 );
		assertThat( array.asString() ).isEqualTo( "1a2b1" );
	}

	@Test
	void testSort() {
		DelimitedArray array = DelimitedArray.of( "3", "z", "1", "a", "2" );

		array.sort( ( a, b ) -> ( ( String ) a ).compareTo( ( String ) b ) );

		assertThat( array.size() ).isEqualTo( 3 );
		assertThat( array.asString() ).isEqualTo( "1a2a3" );
	}

	@Test
	void testRemoveDuplicates() {
		DelimitedArray	array	= DelimitedArray.of( "apple", ",", "Apple", "|", "banana", ",", "apple" );

		Array			result	= array.removeDuplicates( true );

		assertThat( result ).isInstanceOf( DelimitedArray.class );
		DelimitedArray delimitedResult = ( DelimitedArray ) result;
		assertThat( delimitedResult.size() ).isEqualTo( 3 );
		assertThat( delimitedResult.asString() ).isEqualTo( "apple,Apple|banana" );
	}

	@Test
	void testGetSetData() {
		DelimitedArray	array		= DelimitedArray.of( "apple", ",", "Apple", "|", "banana", ",", "apple" );
		DelimitedArray	newArray	= new DelimitedArray();
		// Add all items to the new array
		for ( int i = 0; i < array.size(); i++ ) {
			newArray.add( array.getData( i ) );
		}

		assertThat( array.size() ).isEqualTo( newArray.size() );
		assertThat( array.asString() ).isEqualTo( newArray.asString() );
	}

}
