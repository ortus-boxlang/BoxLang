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

public class BooleanCasterTest {

	@DisplayName( "It can cast a boolean to a boolean" )
	@Test
	void testItCanCastABoolean() {
		assertThat( BooleanCaster.cast( true ) ).isTrue();
		assertThat( BooleanCaster.cast( false ) ).isFalse();
	}

	@DisplayName( "It can cast a number to a boolean" )
	@Test
	void testItCanCastANumber() {
		assertThat( BooleanCaster.cast( 1 ) ).isTrue();
		assertThat( BooleanCaster.cast( -1 ) ).isTrue();
		assertThat( BooleanCaster.cast( 0 ) ).isFalse();
		assertThat( BooleanCaster.cast( 123456.789 ) ).isTrue();
	}

	@DisplayName( "It can cast a string to a boolean" )
	@Test
	void testItCanCastAString() {
		assertThat( BooleanCaster.cast( "true" ) ).isTrue();
		assertThat( BooleanCaster.cast( "TRUE" ) ).isTrue();
		assertThat( BooleanCaster.cast( "false" ) ).isFalse();
		assertThat( BooleanCaster.cast( "FALSE" ) ).isFalse();
		assertThat( BooleanCaster.cast( "yes" ) ).isTrue();
		assertThat( BooleanCaster.cast( "no" ) ).isFalse();

		assertThrows(RuntimeException.class, () -> BooleanCaster.cast( "Brad" ) );
	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		CastAttempt<Boolean> attempt = BooleanCaster.attempt( true );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get() ).isEqualTo( true );
		assertThat( attempt.ifSuccessful( (v)->System.out.println(v) ) );

		final CastAttempt<Boolean> attempt2 = BooleanCaster.attempt( "Brad" );
		assertThat( attempt2.wasSuccessful() ).isFalse();

		assertThrows(RuntimeException.class, () -> attempt2.get() );
		assertThat( attempt2.ifSuccessful( (v)->System.out.println(v) ) );
		assertThat( attempt2.getOrDefault( false ) ).isEqualTo( false );
		assertThat( attempt2.getOrSupply( ()->1==2 ) ).isEqualTo( false );

	}

}
