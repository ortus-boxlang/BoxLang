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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BigIntegerCasterTest {

	@DisplayName( "It can cast a BigDecimal" )
	@Test
	void testItCanCastBigDecimal() {
		assertThat( BigIntegerCaster.cast( new BigDecimal( 1 ) ) ).isEqualTo( BigInteger.ONE );
	}

	@DisplayName( "It can cast a primitive Long" )
	@Test
	void testItCanCastLong() {
		assertThat( BigIntegerCaster.cast( 1L ) ).isEqualTo( BigInteger.ONE );
	}

	@DisplayName( "It can cast a boxed Long" )
	@Test
	void testItCanCastBoxedLong() {
		assertThat( BigIntegerCaster.cast( Long.valueOf( 1 ) ) ).isEqualTo( BigInteger.ONE );
	}

	@DisplayName( "It can cast a primitive int" )
	@Test
	void testItCanCastInt() {
		assertThat( BigIntegerCaster.cast( 1 ) ).isEqualTo( BigInteger.ONE );
	}

	@DisplayName( "It can cast a boxed int" )
	@Test
	void testItCanCastBoxedInt() {
		assertThat( BigIntegerCaster.cast( Integer.valueOf( 1 ) ) ).isEqualTo( BigInteger.ONE );
	}

	@DisplayName( "It can cast a primitive Double" )
	@Test
	void testItCanCastDouble() {
		assertThat( BigIntegerCaster.cast( 1.0 ) ).isEqualTo( BigInteger.ONE );
	}

	@DisplayName( "It can cast a boxed Double" )
	@Test
	void testItCanCastBoxedDouble() {
		assertThat( BigIntegerCaster.cast( Double.valueOf( 1 ) ) ).isEqualTo( BigInteger.ONE );
	}
}
