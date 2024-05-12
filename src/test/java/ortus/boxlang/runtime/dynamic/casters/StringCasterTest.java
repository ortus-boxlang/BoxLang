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
import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class StringCasterTest {

	@DisplayName( "It can cast a string to a string" )
	@Test
	void testItCanCastAString() {
		assertThat( StringCaster.cast( "Brad" ) ).isEqualTo( "Brad" );
	}

	@DisplayName( "It cannot cast a null to a string" )
	@Test
	void testItCanNotCastANull() {
		assertThrows( BoxCastException.class, () -> StringCaster.cast( null ) );
	}

	@DisplayName( "It can cast a Boolean to a string" )
	@Test
	void testItCanCastABoolean() {
		assertThat( StringCaster.cast( Boolean.valueOf( "true" ) ) ).isEqualTo( "true" );
	}

	@DisplayName( "It can cast an int to a string" )
	@Test
	void testItCanCastAnInt() {
		assertThat( StringCaster.cast( 0 ) ).isEqualTo( "0" );
		assertThat( StringCaster.cast( 5 ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( -5 ) ).isEqualTo( "-5" );
		assertThat( StringCaster.cast( 9999999 ) ).isEqualTo( "9999999" );
		assertThat( StringCaster.cast( -9999999 ) ).isEqualTo( "-9999999" );
	}

	@DisplayName( "It can cast a Double to a string" )
	@Test
	void testItCanCastADouble() {
		assertThat( StringCaster.cast( Double.valueOf( "5" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Double.valueOf( "5.0" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Double.valueOf( "5.1" ) ) ).isEqualTo( "5.1" );
		assertThat( StringCaster.cast( Double.valueOf( "5.2" ) ) ).isEqualTo( "5.2" );
		assertThat( StringCaster.cast( Double.valueOf( "5.3" ) ) ).isEqualTo( "5.3" );
		assertThat( StringCaster.cast( Double.valueOf( "5.4" ) ) ).isEqualTo( "5.4" );
		assertThat( StringCaster.cast( Double.valueOf( "5.5" ) ) ).isEqualTo( "5.5" );
		assertThat( StringCaster.cast( Double.valueOf( "5.6" ) ) ).isEqualTo( "5.6" );
		assertThat( StringCaster.cast( Double.valueOf( "5.7" ) ) ).isEqualTo( "5.7" );
		assertThat( StringCaster.cast( Double.valueOf( "5.8" ) ) ).isEqualTo( "5.8" );
		assertThat( StringCaster.cast( Double.valueOf( "5.9" ) ) ).isEqualTo( "5.9" );
		assertThat( StringCaster.cast( Double.valueOf( "1.2345678901234567" ) ) ).isEqualTo( "1.2345678901234567" );
	}

	@DisplayName( "It can cast a Float to a string" )
	@Test
	void testItCanCastAFloat() {
		assertThat( StringCaster.cast( Float.valueOf( "5" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Float.valueOf( "5.0" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Float.valueOf( "5.1" ) ) ).isEqualTo( "5.1" );
		assertThat( StringCaster.cast( Float.valueOf( "5.2" ) ) ).isEqualTo( "5.2" );
		assertThat( StringCaster.cast( Float.valueOf( "5.3" ) ) ).isEqualTo( "5.3" );
		assertThat( StringCaster.cast( Float.valueOf( "5.4" ) ) ).isEqualTo( "5.4" );
		assertThat( StringCaster.cast( Float.valueOf( "5.5" ) ) ).isEqualTo( "5.5" );
		assertThat( StringCaster.cast( Float.valueOf( "5.6" ) ) ).isEqualTo( "5.6" );
		assertThat( StringCaster.cast( Float.valueOf( "5.7" ) ) ).isEqualTo( "5.7" );
		assertThat( StringCaster.cast( Float.valueOf( "5.8" ) ) ).isEqualTo( "5.8" );
		assertThat( StringCaster.cast( Float.valueOf( "5.9" ) ) ).isEqualTo( "5.9" );
		// Max float decimal precision
		assertThat( StringCaster.cast( Float.valueOf( "1.2345678" ) ) ).isEqualTo( "1.2345678" );
	}

	@DisplayName( "It can cast a BigDecimal to a string" )
	@Test
	void testItCanCastABigDecimal() {
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5 ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.0 ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.1 ) ) ).isEqualTo( "5.1" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.2 ) ) ).isEqualTo( "5.2" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.3 ) ) ).isEqualTo( "5.3" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.4 ) ) ).isEqualTo( "5.4" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.5 ) ) ).isEqualTo( "5.5" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.6 ) ) ).isEqualTo( "5.6" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.7 ) ) ).isEqualTo( "5.7" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.8 ) ) ).isEqualTo( "5.8" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.9 ) ) ).isEqualTo( "5.9" );
		assertThat( StringCaster.cast( new BigDecimal( "1.12345789123456789123456789123456789123456789" ) ) )
		    .isEqualTo( "1.12345789123456789123456789123456789123456789" );
	}

	@DisplayName( "It can cast a byte array to a string" )
	@Test
	void testItCanCastAByteArray() {
		assertThat( StringCaster.cast( "Brad".getBytes() ) ).isEqualTo( "Brad" );
	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		CastAttempt<String> attempt = StringCaster.attempt( "brad" );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( "brad" );
		assertThat( attempt.ifSuccessful( ( v ) -> System.out.println( v ) ) );
	}

	@DisplayName( "It can cast a Locale to a string" )
	@Test
	void testItCanCastI18NLocaleToString() {
		assertThat( StringCaster.cast( Locale.CANADA ) ).isEqualTo( "en_CA" );
	}

	@DisplayName( "It can cast a TimeZone to a string" )
	@Test
	void testItCanCastATimezone() {
		assertThat(
		    StringCaster.cast( ZoneId.of( "UTC" ) )
		).isEqualTo( "UTC" );
	}

}
