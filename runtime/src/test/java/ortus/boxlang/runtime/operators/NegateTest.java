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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NegateTest {

	@DisplayName( "It can mathematically negate a number" )
	@Test
	void testItCanNegateNumber() {
		assertThat( Negate.invoke( 5 ) ).isEqualTo( -5 );
		assertThat( Negate.invoke( -5 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It can mathematically negate a string" )
	@Test
	void testItCanNegateString() {
		assertThat( Negate.invoke( "5" ) ).isEqualTo( -5 );
		assertThat( Negate.invoke( "-5" ) ).isEqualTo( 5 );
	}

	@DisplayName( "It can mathematically negate a boolean" )
	@Test
	void testItCanNegateBoolean() {
		assertThat( Negate.invoke( true ) ).isEqualTo( -1 );
		assertThat( Negate.invoke( false ) == 0 ).isTrue();
		assertThat( Negate.invoke( "true" ) ).isEqualTo( -1 );
		assertThat( Negate.invoke( "false" ) == 0 ).isTrue();
		assertThat( Negate.invoke( "yes" ) ).isEqualTo( -1 );
		assertThat( Negate.invoke( "no" ) == 0 ).isTrue();
	}

}
