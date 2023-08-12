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


import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DoubleCasterTest {

	@DisplayName( "It can cast a Double to a Double" )
	@Test
	void testItCanCastADouble() {
		assertThat( DoubleCaster.cast( Double.valueOf( 5 ) ) ).isEqualTo( 5 );
	}

	@DisplayName( "It can cast a null to a Double" )
	@Test
	void testItCanCastANull() {
		assertThat( DoubleCaster.cast( null ) ).isEqualTo( 0 );
	}

	@DisplayName( "It can cast a string to a Double" )
	@Test
	void testItCanCastAString() {
		assertThat( DoubleCaster.cast( "42" ) ).isEqualTo( 42 );
	}

	@DisplayName( "It can cast a char to a Double" )
	@Test
	void testItCanCastAChar() {
		assertThat( DoubleCaster.cast( '5' ) ).isEqualTo( 5 );
	}

	@DisplayName( "It can cast a char array to a Double" )
	@Test
	void testItCanCastACharArray() {
		assertThat( DoubleCaster.cast( "12345".getBytes() ) ).isEqualTo( 12345 );
	}

	@DisplayName( "It can cast a boolean to a Double" )
	@Test
	void testItCanCastABoolean() {
		assertThat( DoubleCaster.cast( true ) ).isEqualTo( 1 );
		assertThat( DoubleCaster.cast( false ) ).isEqualTo( 0 );
		assertThat( DoubleCaster.cast( "true" ) ).isEqualTo( 1 );
		assertThat( DoubleCaster.cast( "false" ) ).isEqualTo( 0 );
		assertThat( DoubleCaster.cast( "yes" ) ).isEqualTo( 1 );
		assertThat( DoubleCaster.cast( "no" ) ).isEqualTo( 0 );
	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		CastAttempt<Double> attempt = DoubleCaster.attempt( 5 );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( 5 );
		assertThat( attempt.ifSuccessful( (v)->System.out.println(v) ) );

		final CastAttempt<Double> attempt2 = DoubleCaster.attempt( "Brad" );
		assertThat( attempt2.wasSuccessful() ).isFalse();

		assertThrows(RuntimeException.class, () -> attempt2.get() );
		assertThat( attempt2.ifSuccessful( (v)->System.out.println(v) ) );
		assertThat( attempt2.getOrDefault( 42D ) ).isEqualTo( 42 );
		assertThat( attempt2.getOrSupply( ()->40D+2D ) ).isEqualTo( 42 );

	}

}
