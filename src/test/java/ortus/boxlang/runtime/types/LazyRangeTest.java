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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LazyRangeTest {

	@DisplayName( "start-only lazy ranges support arithmetic membership and forward iteration" )
	@Test
	void testStartOnlyLazyRange() {
		LazyRange range = LazyRange.startingAt( 3 );

		assertThat( range.contains( 3 ) ).isTrue();
		assertThat( range.contains( 50 ) ).isTrue();
		assertThat( range.contains( 2 ) ).isFalse();

		Iterator<Integer> iterator = range.iterator();
		assertThat( iterator.next() ).isEqualTo( 3 );
		assertThat( iterator.next() ).isEqualTo( 4 );
		assertThat( iterator.next() ).isEqualTo( 5 );
		assertThat( iterator.next() ).isEqualTo( 6 );
	}

	@DisplayName( "end-only lazy ranges support arithmetic membership and backward iteration" )
	@Test
	void testEndOnlyLazyRange() {
		LazyRange range = LazyRange.endingAt( 3 );

		assertThat( range.contains( 3 ) ).isTrue();
		assertThat( range.contains( -50 ) ).isTrue();
		assertThat( range.contains( 4 ) ).isFalse();

		Iterator<Integer> iterator = range.iterator();
		assertThat( iterator.next() ).isEqualTo( 3 );
		assertThat( iterator.next() ).isEqualTo( 2 );
		assertThat( iterator.next() ).isEqualTo( 1 );
		assertThat( iterator.next() ).isEqualTo( 0 );
	}

	@DisplayName( "lazy ranges can initiate finite stream views via limit" )
	@Test
	void testLazyRangeStream() {
		LazyRange range = LazyRange.startingAt( 2 );

		assertThat( range.stream().limit( 4 ).toList() ).containsExactlyElementsIn( List.of( 2, 3, 4, 5 ) ).inOrder();
	}
}