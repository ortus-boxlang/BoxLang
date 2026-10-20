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
import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class BigDecimalCasterTest {

	@DisplayName( "It can cast a BigDecimal" )
	@Test
	void testItCanCastBigDecimal() {
		assertThat( BigDecimalCaster.cast( new BigDecimal( "7.36800" ) ) ).isEqualTo( new BigDecimal( "7.36800" ) );
	}

	@DisplayName( "It can cast a BigInteger" )
	@Test
	void testItCanCastBigInteger() {
		assertThat( BigDecimalCaster.cast( new BigInteger( "736800" ) ) ).isEqualTo( new BigDecimal( "736800" ) );
	}

	@DisplayName( "It can cast a primitive Long" )
	@Test
	void testItCanCastLong() {
		assertThat( BigDecimalCaster.cast( 1L ) ).isEqualTo( BigDecimal.ONE );
	}

	@DisplayName( "It can cast a boxed Long" )
	@Test
	void testItCanCastBoxedLong() {
		assertThat( BigDecimalCaster.cast( Long.valueOf( 1 ) ) ).isEqualTo( BigDecimal.ONE );
	}

	@DisplayName( "It can cast a primitive int" )
	@Test
	void testItCanCastInt() {
		assertThat( BigDecimalCaster.cast( 1 ) ).isEqualTo( BigDecimal.ONE );
	}

	@DisplayName( "It can cast a boxed int" )
	@Test
	void testItCanCastBoxedInt() {
		assertThat( BigDecimalCaster.cast( Integer.valueOf( 1 ) ) ).isEqualTo( BigDecimal.ONE );
	}

	@DisplayName( "It can cast a primitive Double" )
	@Test
	void testItCanCastDouble() {
		assertThat( BigDecimalCaster.cast( 1.0 ) ).isEqualTo( new BigDecimal( "1.0" ) );
	}

	@DisplayName( "It can cast a boxed Double" )
	@Test
	void testItCanCastBoxedDouble() {
		assertThat( BigDecimalCaster.cast( Double.valueOf( 1 ) ) ).isEqualTo( new BigDecimal( "1.0" ) );
	}

	@DisplayName( "It can cast a primitive Float" )
	@Test
	void testItCanCastFloat() {
		assertThat( BigDecimalCaster.cast( 1.0f ) ).isEqualTo( new BigDecimal( "1.0" ) );
	}

	@DisplayName( "It can cast a null to a BigDecimal" )
	@Test
	void testItCanCastNull() {
		assertThat( BigDecimalCaster.cast( null ) ).isEqualTo( BigDecimal.ZERO );
	}

	@DisplayName( "It can cast a boolean to a BigDecimal" )
	@Test
	void testItCanCastBoolean() {
		assertThat( BigDecimalCaster.cast( true ) ).isEqualTo( BigDecimal.ONE );
		assertThat( BigDecimalCaster.cast( false ) ).isEqualTo( BigDecimal.ZERO );
		assertThat( BigDecimalCaster.cast( "true" ) ).isEqualTo( BigDecimal.ONE );
		assertThat( BigDecimalCaster.cast( "false" ) ).isEqualTo( BigDecimal.ZERO );
		assertThat( BigDecimalCaster.cast( "yes" ) ).isEqualTo( BigDecimal.ONE );
		assertThat( BigDecimalCaster.cast( "no" ) ).isEqualTo( BigDecimal.ZERO );
	}

	@DisplayName( "It can cast a string to a BigDecimal" )
	@Test
	void testItCanCastString() {
		assertThat( BigDecimalCaster.cast( "421" ) ).isEqualTo( new BigDecimal( "421" ) );
		assertThat( BigDecimalCaster.cast( "-42" ) ).isEqualTo( new BigDecimal( "-42" ) );
		assertThat( BigDecimalCaster.cast( "+42" ) ).isEqualTo( new BigDecimal( "42" ) );
		assertThat( BigDecimalCaster.cast( "4.2" ) ).isEqualTo( new BigDecimal( "4.2" ) );
		assertThat( BigDecimalCaster.cast( "42." ) ).isEqualTo( new BigDecimal( "42" ) );
		assertThat( BigDecimalCaster.cast( "7.36800" ) ).isEqualTo( new BigDecimal( "7.36800" ) );
		assertThat( BigDecimalCaster.cast( "0123" ) ).isEqualTo( new BigDecimal( "123" ) );
		assertThrows(
		    BoxCastException.class, () -> {
			    BigDecimalCaster.cast( "42.brad" );
		    }
		);
	}

	@DisplayName( "It can cast strings with surrounding whitespace to a BigDecimal" )
	@Test
	void testItCanCastWhitespaceStrings() {
		assertThat( BigDecimalCaster.cast( "  7.36800  " ) ).isEqualTo( new BigDecimal( "7.36800" ) );
		assertThat( BigDecimalCaster.cast( "	7.36800	" ) ).isEqualTo( new BigDecimal( "7.36800" ) );
		assertThat( BigDecimalCaster.cast( "  421  " ) ).isEqualTo( new BigDecimal( "421" ) );
		assertThat( BigDecimalCaster.cast( "  -42  " ) ).isEqualTo( new BigDecimal( "-42" ) );
		assertThat( BigDecimalCaster.cast( "  +42  " ) ).isEqualTo( new BigDecimal( "42" ) );
		assertThat( BigDecimalCaster.cast( "  4.2  " ) ).isEqualTo( new BigDecimal( "4.2" ) );
		assertThat( BigDecimalCaster.cast( "  42.  " ) ).isEqualTo( new BigDecimal( "42" ) );
	}

	@DisplayName( "It can cast strings with leading zeros and whitespace to a BigDecimal" )
	@Test
	void testItCanCastLeadingZeroWhitespaceStrings() {
		assertThat( BigDecimalCaster.cast( "  0123  " ) ).isEqualTo( new BigDecimal( "123" ) );
		assertThat( BigDecimalCaster.cast( "  05887  " ) ).isEqualTo( new BigDecimal( "5887" ) );
		assertThat( BigDecimalCaster.cast( "  007  " ) ).isEqualTo( new BigDecimal( "7" ) );
	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		CastAttempt<BigDecimal> attempt = BigDecimalCaster.attempt( 5 );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( new BigDecimal( "5" ) );

		final CastAttempt<BigDecimal> attempt2 = BigDecimalCaster.attempt( "Brad" );
		assertThat( attempt2.wasSuccessful() ).isFalse();
		assertThat( attempt2.getOrDefault( new BigDecimal( "42" ) ) ).isEqualTo( new BigDecimal( "42" ) );
	}

	@DisplayName( "It can attempt to cast strings with surrounding whitespace" )
	@Test
	void testItCanAttemptToCastWhitespaceStrings() {
		CastAttempt<BigDecimal> attempt = BigDecimalCaster.attempt( "  7.36800  " );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( new BigDecimal( "7.36800" ) );

		attempt = BigDecimalCaster.attempt( "  421  " );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( new BigDecimal( "421" ) );
	}
}
