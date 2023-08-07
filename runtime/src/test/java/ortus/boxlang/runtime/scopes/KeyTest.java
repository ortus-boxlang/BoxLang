
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

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

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
		Key key1 = new Key( "Test" );
		Key key2 = new Key( "test" );
		assertThat( key1 ).isEqualTo( key1 );
		assertThat( key1 ).isEqualTo( key2 );
	}

	@Test
	public void testEqualsWithCase() {
		Key key1 = new Key( "Test" );
		Key key2 = new Key( "test" );
		Key key3 = new Key( "Test" );
		assertThat( key1.equalsWithCase( key2 ) ).isFalse();
		assertThat( key1.equalsWithCase( key3 ) ).isTrue();
	}

	@Test
	public void testNotEquals() {
		Key key1 = new Key( "Test2" );
		Key key2 = new Key( "Test" );
		assertThat( key1 ).isNotEqualTo( key2 );
	}

	@Test
	public void testHashCode() {
		Key key = new Key( "Test" );
		assertThat( key.hashCode() ).isEqualTo( key.getNameNoCase().hashCode() );
	}

	@Test
	public void testOfBuilder() {
		Key key = Key.of( "Test" );
		assertThat( key.getName() ).isEqualTo( "Test" );
		assertThat( key.getNameNoCase() ).isEqualTo( "TEST" );
	}
}
