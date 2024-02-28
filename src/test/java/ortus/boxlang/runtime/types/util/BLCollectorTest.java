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
package ortus.boxlang.runtime.types.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Array;

class BLCollectorTest {

	@Test
	void testToArray() {

		// IntStream.of( 1, 2, 3, 4, 5 ).collect( BLCollector.toArray() );
		Array result = Stream.of( "string1", "string2", "string3" ).collect( BLCollector.toArray() );
		assertThat( result.size() ).isEqualTo( 3 );
		assertThat( result.get( 0 ) ).isEqualTo( "string1" );
		assertThat( result.get( 1 ) ).isEqualTo( "string2" );
		assertThat( result.get( 2 ) ).isEqualTo( "string3" );

	}

}
