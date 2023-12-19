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
package ortus.boxlang.runtime.scopes;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Array;

public class KeyTest {

	@Test
	public void testGetNameNoCase() {
		Key key = new Key( "Test" );
		assertThat( key.getNameNoCase() ).isEqualTo( "TEST" );
	}

	@Test
	public void testGetName() {
		Key key = new Key( "Test" );
		assertThat( key.getName() ).isEqualTo( "Test" );
	}

	@Test
	public void testEquals() {
		Key	key1	= new Key( "Test" );
		Key	key2	= new Key( "test" );
		assertThat( key1 ).isEqualTo( key1 );
		assertThat( key1 ).isEqualTo( key2 );
		assertThat( key1.equals( key2 ) ).isTrue();
	}

	@Test
	public void testEqualsWithCase() {
		Key	key1	= new Key( "Test" );
		Key	key2	= new Key( "test" );
		Key	key3	= new Key( "Test" );
		assertThat( key1.equalsWithCase( key2 ) ).isFalse();
		assertThat( key1.equalsWithCase( key3 ) ).isTrue();
	}

	@Test
	public void testNotEquals() {
		Key	key1	= new Key( "Test2" );
		Key	key2	= new Key( "Test" );
		assertThat( key1 ).isNotEqualTo( key2 );
	}

	@Test
	public void testHashCode() {
		Key key = new Key( "Test" );
		assertThat( key.hashCode() ).isEqualTo( key.getNameNoCase().hashCode() );
	}

	@DisplayName( "Test the builder with one key" )
	@Test
	public void testOfBuilder() {
		Key key = Key.of( "Test" );
		assertThat( key.getName() ).isEqualTo( "Test" );
		assertThat( key.getNameNoCase() ).isEqualTo( "TEST" );
	}

	@DisplayName( "Test the builder with multiple keys" )
	@Test
	public void testOfBuilderMultiple() {
		Key[] keys = Key.of( "Test", "Test2", "Test3" );

		assertThat( keys.length ).isEqualTo( 3 );
		assertThat( keys[ 0 ].getName() ).isEqualTo( "Test" );
		assertThat( keys[ 1 ].getName() ).isEqualTo( "Test2" );
		assertThat( keys[ 2 ].getName() ).isEqualTo( "Test3" );
	}

	@DisplayName( "Test the original value" )
	@Test
	public void testTheOriginalValue() {
		Array	arr	= Array.of( 1, 2, 3 );
		Key		key	= Key.of( arr );
		System.out.println( key );
		assertThat( key.getOriginalValue() instanceof Array ).isTrue();
		assertThat( key.getOriginalValue() ).isEqualTo( arr );

	}
}
