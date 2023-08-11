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

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompareTest {

	@DisplayName( "It can compare strings case insensitive" )
	@Test
	void testItCanCompareStringsCaseInsensitive() {
		assertThat( Compare.invoke( "Brad", "Brad" ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "Brad", "BRAD" ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "A", "B" ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( "A", "b" ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( "B", "A" ) ).isEqualTo( 1 );
		assertThat( Compare.invoke( "B", "a" ) ).isEqualTo( 1 );

		assertThat( Compare.invoke( "A", "a" ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "a", "A" ) ).isEqualTo( 0 );
	}

	@DisplayName( "It can compare strings case sensitive" )
	@Test
	void testItCanCompareStringsCaseSensitive() {
		assertThat( Compare.invoke( "Brad", "BRAD", true ) ).isGreaterThan( 0 );
		assertThat( Compare.invoke( "BRAD", "Brad", true ) ).isLessThan( 0 );
		assertThat( Compare.invoke( "A", "A", true ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "A", "a", true ) ).isLessThan( 0 );
		assertThat( Compare.invoke( "a", "A", true ) ).isGreaterThan( 0 );
	}

	@DisplayName( "It can compare numbers" )
	@Test
	void testItCanCompareNumbers() {
		assertThat( Compare.invoke( 1, 1 ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( 1, 2 ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( 2, 1 ) ).isEqualTo( 1 );

		assertThat( Compare.invoke( 1.5, 1.5 ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( 1.5, 1.7 ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( 2.8, 0.6 ) ).isEqualTo( 1 );
	}

	@DisplayName( "It can compare strings as numbers" )
	@Test
	void testItCanCompareStringsAsNumbers() {
		assertThat( Compare.invoke( "1", "1" ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "1", "2" ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( "2", "1" ) ).isEqualTo( 1 );

		assertThat( Compare.invoke( "1.5", "1.5" ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "1.5", "1.7" ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( "2.8", "0.6" ) ).isEqualTo( 1 );

		assertThat( Compare.invoke( "1.5", "1.500" ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( "1.5000", "1.7" ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( "2.8", "00000.600000" ) ).isEqualTo( 1 );
	}

}
