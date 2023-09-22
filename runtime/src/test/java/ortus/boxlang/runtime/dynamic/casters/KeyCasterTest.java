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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class KeyCasterTest {

	@DisplayName( "It can cast a string to a key" )
	@Test
	void testItCanCastAString() {
		assertThat( KeyCaster.cast( "Brad" ) ).isEqualTo( Key.of( "Brad" ) );
	}

	@DisplayName( "It can cast a null to a key" )
	@Test
	void testItCanCastANull() {
		assertThrows( BoxLangException.class, () -> KeyCaster.cast( null ) );
	}

	@DisplayName( "It can cast a Boolean to a key" )
	@Test
	void testItCanCastABoolean() {
		assertThat( KeyCaster.cast( Boolean.valueOf( "true" ) ) ).isEqualTo( Key.of( "true" ) );
	}

	@DisplayName( "It can cast an int to a key" )
	@Test
	void testItCanCastAnInt() {
		assertThat( KeyCaster.cast( 5 ) ).isEqualTo( Key.of( "5" ) );
	}

	@DisplayName( "It can cast a Double to a key" )
	@Test
	void testItCanCastADouble() {
		assertThat( KeyCaster.cast( Double.valueOf( "5" ) ) ).isEqualTo( Key.of( "5" ) );
		assertThat( KeyCaster.cast( Double.valueOf( "5.0" ) ) ).isEqualTo( Key.of( "5" ) );
		assertThat( KeyCaster.cast( Double.valueOf( "1.2345678901234567" ) ) ).isEqualTo( Key.of( "1.2345678901234567" ) );
	}

	@DisplayName( "It can cast a Float to a key" )
	@Test
	void testItCanCastAFloat() {
		assertThat( KeyCaster.cast( Float.valueOf( "5" ) ) ).isEqualTo( Key.of( "5" ) );
		assertThat( KeyCaster.cast( Float.valueOf( "5.0" ) ) ).isEqualTo( Key.of( "5" ) );
		assertThat( KeyCaster.cast( Float.valueOf( "5.1" ) ) ).isEqualTo( Key.of( "5.1" ) );
		// Max float decimal precision
		assertThat( KeyCaster.cast( Float.valueOf( "1.2345678" ) ) ).isEqualTo( Key.of( "1.2345678" ) );
	}

	@DisplayName( "It can cast a BigDecimal to a key" )
	@Test
	void testItCanCastABigDecimal() {
		assertThat( KeyCaster.cast( BigDecimal.valueOf( 5 ) ) ).isEqualTo( Key.of( "5" ) );
		assertThat( KeyCaster.cast( BigDecimal.valueOf( 5.0 ) ) ).isEqualTo( Key.of( "5" ) );
		assertThat( KeyCaster.cast( new BigDecimal( "1.12345789123456789123456789123456789123456789" ) ) )
		    .isEqualTo( Key.of( "1.12345789123456789123456789123456789123456789" ) );
	}

	@DisplayName( "It can cast a byte array to a key" )
	@Test
	void testItCanCastAByteArray() {
		assertThat( KeyCaster.cast( "Brad".getBytes() ) ).isEqualTo( Key.of( "Brad" ) );
	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		CastAttempt<Key> attempt = KeyCaster.attempt( "brad" );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( Key.of( "brad" ) );
		assertThat( attempt.ifSuccessful( ( v ) -> System.out.println( v ) ) );
	}

}
