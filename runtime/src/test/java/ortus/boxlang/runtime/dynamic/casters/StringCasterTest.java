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

import java.math.BigDecimal;

public class StringCasterTest {

	@DisplayName( "It can cast a string to a string" )
	@Test
	void testItCanCastAString() {
		assertThat( StringCaster.cast( "Brad" ) ).isEqualTo( "Brad" );
	}

	@DisplayName( "It can cast a null to a string" )
	@Test
	void testItCanCastANull() {
		assertThat( StringCaster.cast( null ) ).isEqualTo( "" );
	}

	@DisplayName( "It can cast a Boolean to a string" )
	@Test
	void testItCanCastABoolean() {
		assertThat( StringCaster.cast( Boolean.valueOf( "true" ) ) ).isEqualTo( "true" );
	}

	@DisplayName( "It can cast an int to a string" )
	@Test
	void testItCanCastAnInt() {
		assertThat( StringCaster.cast( 5 ) ).isEqualTo( "5" );
	}

	@DisplayName( "It can cast a Double to a string" )
	@Test
	void testItCanCastADouble() {
		assertThat( StringCaster.cast( Double.valueOf( "5" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Double.valueOf( "5.6" ) ) ).isEqualTo( "5.6" );
	}

	@DisplayName( "It can cast a Float to a string" )
	@Test
	void testItCanCastAFloat() {
		assertThat( StringCaster.cast( Float.valueOf( "5" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Float.valueOf( "5.0" ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( Float.valueOf( "5.7" ) ) ).isEqualTo( "5.7" );
	}

	@DisplayName( "It can cast a BigDecimal to a string" )
	@Test
	void testItCanCastABigDecimal() {
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5 ) ) ).isEqualTo( "5" );
		assertThat( StringCaster.cast( BigDecimal.valueOf( 5.6 ) ) ).isEqualTo( "5.6" );
	}

	@DisplayName( "It can cast a byte array to a string" )
	@Test
	void testItCanCastAByteArray() {
		assertThat( StringCaster.cast( "Brad".getBytes() ) ).isEqualTo( "Brad" );
	}



}
