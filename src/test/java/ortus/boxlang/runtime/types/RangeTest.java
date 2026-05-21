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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RangeTest {

	@DisplayName( "ascending ranges expose stable size and membership" )
	@Test
	void testAscendingRangeSizeAndContains() {
		Range range = new Range( 1, 5 );

		assertThat( range.size() ).isEqualTo( 5 );
		assertThat( range.contains( 1 ) ).isTrue();
		assertThat( range.contains( 3 ) ).isTrue();
		assertThat( range.contains( 5 ) ).isTrue();
		assertThat( range.contains( 0 ) ).isFalse();
		assertThat( range.contains( 6 ) ).isFalse();
		assertThat( range.contains( ( Object ) 2.5 ) ).isFalse();
	}

	@DisplayName( "descending ranges iterate inclusively without eager arrays" )
	@Test
	void testDescendingRangeIteration() {
		Range range = new Range( 5, 1 );

		assertThat( range ).containsExactly( 5, 4, 3, 2, 1 ).inOrder();
	}

	@DisplayName( "single-value ranges remain inclusive" )
	@Test
	void testSingleValueRange() {
		Range range = new Range( 3, 3 );

		assertThat( range.size() ).isEqualTo( 1 );
		assertThat( range ).containsExactly( 3 );
		assertThat( range.contains( 3 ) ).isTrue();
		assertThat( range.contains( 2 ) ).isFalse();
	}

	@DisplayName( "ranges can materialize explicitly to BoxLang arrays" )
	@Test
	void testToBoxArray() {
		Range range = new Range( 2, 4 );

		assertThat( range.toBoxArray() ).isEqualTo( Array.of( 2, 3, 4 ) );
	}

	@DisplayName( "ranges can initiate Java streams" )
	@Test
	void testStream() {
		Range range = new Range( 1, 5 );

		assertThat( range.stream().filter( value -> value >= 3 ).toList() ).containsExactly( 3, 4, 5 ).inOrder();
	}

	@DisplayName( "ranges can initiate Java parallel streams" )
	@Test
	void testParallelStream() {
		Range range = new Range( 1, 5 );

		assertThat( range.parallelStream().filter( value -> value >= 3 ).toList() ).containsExactlyElementsIn( List.of( 3, 4, 5 ) );
	}

	@DisplayName( "mutating range collection methods fail with the immutability contract" )
	@Test
	void testMutationMethodsFailPredictably() {
		Range							range			= new Range( 1, 3 );

		UnsupportedOperationException	addException	= assertThrows( UnsupportedOperationException.class, () -> range.add( 4 ) );
		assertThat( addException ).hasMessageThat().contains( "immutable" );

		UnsupportedOperationException clearException = assertThrows( UnsupportedOperationException.class, range::clear );
		assertThat( clearException ).hasMessageThat().contains( "immutable" );
	}
}