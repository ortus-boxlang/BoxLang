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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class NumberCasterTest {

	@DisplayName( "It can cast a Double to a Number" )
	@Test
	void testItCanCastADouble() {
		Number result = NumberCaster.cast( Double.valueOf( 5 ) );
		assertThat( result ).isInstanceOf( Double.class );
		assertThat( result.doubleValue() ).isEqualTo( 5 );
	}

	@DisplayName( "It can cast a null to a Number" )
	@Test
	void testItCanCastANull() {
		Number result = NumberCaster.cast( null );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( 0 );
	}

	@DisplayName( "It can cast a string to a Number" )
	@Test
	void testItCanCastAString() {
		Number result = NumberCaster.cast( "421" );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( 421 );

		result = NumberCaster.cast( "-42" );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( -42 );

		result = NumberCaster.cast( "+42" );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( 42 );

		result = NumberCaster.cast( "4.2" );
		assertThat( result ).isInstanceOf( BigDecimal.class );
		assertThat( result.doubleValue() ).isEqualTo( 4.2 );

		result = NumberCaster.cast( "42." );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( 42 );

		assertThrows(
		    BoxRuntimeException.class, () -> {
			    NumberCaster.cast( "42.brad" );
		    }
		);
	}

	@Test
	void testItCanHandleFloatingPointMath() {
		Number result = NumberCaster.cast( ".42" );
		assertThat( result ).isInstanceOf( BigDecimal.class );
		assertThat( result.doubleValue() ).isEqualTo( 0.42 );
	}

	@DisplayName( "It can cast a char to a Number" )
	@Test
	void testItCanCastAChar() {
		Number result = NumberCaster.cast( '5' );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( 5 );
	}

	@DisplayName( "It can cast a char array to a Number" )
	@Test
	void testItCanCastACharArray() {
		Number result = NumberCaster.cast( "12345".getBytes() );
		assertThat( result ).isInstanceOf( Integer.class );
		assertThat( result.doubleValue() ).isEqualTo( 12345 );
	}

	@DisplayName( "It will NOT cast a boolean to a Number" )
	@Test
	void testItCanCastABoolean() {
		assertThrows( BoxCastException.class, () -> NumberCaster.cast( true ) );

		assertThrows( BoxCastException.class, () -> NumberCaster.cast( false ) );

		assertThrows( BoxCastException.class, () -> NumberCaster.cast( "true" ) );

		assertThrows( BoxCastException.class, () -> NumberCaster.cast( "false" ) );

		assertThrows( BoxCastException.class, () -> NumberCaster.cast( "yes" ) );

		assertThrows( BoxCastException.class, () -> NumberCaster.cast( "no" ) );

	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		CastAttempt<Number> attempt = NumberCaster.attempt( 5 );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( 5 );
		assertThat( attempt.ifSuccessful( System.out::println ) );

		final CastAttempt<Number> attempt2 = NumberCaster.attempt( "Brad" );
		attempt2.ifSuccessful( System.out::println );
		assertThat( attempt2.wasSuccessful() ).isFalse();

		assertThrows( BoxLangException.class, attempt2::get );
		assertThat( attempt2.ifSuccessful( System.out::println ) );
		assertThat( attempt2.getOrDefault( 42D ) ).isEqualTo( 42 );
		assertThat( attempt2.getOrSupply( () -> 40D + 2D ) ).isEqualTo( 42 );

	}

	@Disabled( "Ensures the performance benchmark for the attempt is met" )
	@Test
	void canMeetBenchmark() {
		long start = System.nanoTime();
		for ( int i = 1; i <= 100000; i++ ) {
			NumberCaster.attempt( "Brad" );
		}
		long end = System.nanoTime();
		assertThat( ( end - start ) / 1000000l ).isLessThan( 20l );
	}

}
