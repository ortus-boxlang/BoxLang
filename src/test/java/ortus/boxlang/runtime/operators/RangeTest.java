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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IRangeable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class RangeTest {

	// ======================== Helper IRangeable ========================

	record SteppableInt( int value ) implements IRangeable<SteppableInt> {

		@Override
		public int rangeCompare( SteppableInt other ) {
			return Integer.compare( this.value, other.value );
		}

		@Override
		public SteppableInt rangeAdvance( Number step ) {
			return new SteppableInt( this.value + step.intValue() );
		}
	}

	// ======================== Integer Ranges ========================

	@Nested
	@DisplayName( "Integer Ranges" )
	class IntegerRanges {

		@DisplayName( "It can create an ascending integer range" )
		@Test
		void testAscendingIntRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5 );
			assertThat( range.isIterable() ).isTrue();
			assertThat( range.isAscending() ).isTrue();
			assertThat( range.isBounded() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 1, 2, 3, 4, 5 ).inOrder();
		}

		@DisplayName( "It can create a descending integer range" )
		@Test
		void testDescendingIntRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 5, 1 );
			assertThat( range.isIterable() ).isTrue();
			assertThat( range.isAscending() ).isFalse();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 5, 4, 3, 2, 1 ).inOrder();
		}

		@DisplayName( "It can create a number range from numeric strings" )
		@Test
		void testNumericStringRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "1", "5" );
			assertThat( range.isIterable() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 1, 2, 3, 4, 5 ).inOrder();
		}

		@DisplayName( "It can coerce mixed Number + numeric string" )
		@Test
		void testMixedNumberAndString() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, "5" );
			assertThat( range.isIterable() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 1, 2, 3, 4, 5 ).inOrder();
		}

		@DisplayName( "It can coerce string Number + Number" )
		@Test
		void testStringLeftNumberRight() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "3", 1 );
			assertThat( range.isIterable() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 3, 2, 1 ).inOrder();
		}

		@DisplayName( "Single element range works" )
		@Test
		void testSingleElementRange() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 5, 5 );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 5 );
		}
	}

	// ======================== Character Ranges ========================

	@Nested
	@DisplayName( "Character Ranges" )
	class CharacterRanges {

		@DisplayName( "It can create a character range from single-char strings" )
		@Test
		void testCharRangeFromStrings() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "a", "e" );
			assertThat( range.isIterable() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 'a', 'b', 'c', 'd', 'e' ).inOrder();
		}

		@DisplayName( "It can create a descending character range" )
		@Test
		void testDescendingCharRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "z", "v" );
			assertThat( range.isIterable() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 'z', 'y', 'x', 'w', 'v' ).inOrder();
		}
	}

	// ======================== Comparable String Ranges ========================

	@Nested
	@DisplayName( "Comparable String Ranges" )
	class ComparableStringRanges {

		@DisplayName( "It creates a contains-only range for non-steppable Comparables" )
		@Test
		void testComparableOnlyRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "aaa", "zzz" );
			assertThat( range.isIterable() ).isFalse();
			assertThat( range.isBounded() ).isTrue();
			assertThrows( BoxRuntimeException.class, () -> range.iterator() );
		}
	}

	// ======================== IRangeable Ranges ========================

	@Nested
	@DisplayName( "IRangeable Ranges" )
	class IRangeableRanges {

		@DisplayName( "It can create a range with IRangeable objects" )
		@Test
		void testIRangeableRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( new SteppableInt( 1 ), new SteppableInt( 5 ) );
			assertThat( range.isIterable() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).hasSize( 5 );
			assertThat( values.get( 0 ) ).isEqualTo( new SteppableInt( 1 ) );
			assertThat( values.get( 4 ) ).isEqualTo( new SteppableInt( 5 ) );
		}

		@DisplayName( "It can create a descending IRangeable range" )
		@Test
		void testDescendingIRangeableRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( new SteppableInt( 5 ), new SteppableInt( 1 ) );
			assertThat( range.isIterable() ).isTrue();
			assertThat( range.isAscending() ).isFalse();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).hasSize( 5 );
			assertThat( values.get( 0 ) ).isEqualTo( new SteppableInt( 5 ) );
			assertThat( values.get( 4 ) ).isEqualTo( new SteppableInt( 1 ) );
		}

		@DisplayName( "IRangeable range contains works" )
		@Test
		void testIRangeableContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( new SteppableInt( 1 ), new SteppableInt( 10 ) );
			assertThat( range.contains( new SteppableInt( 5 ) ) ).isTrue();
			assertThat( range.contains( new SteppableInt( 1 ) ) ).isTrue();
			assertThat( range.contains( new SteppableInt( 10 ) ) ).isTrue();
			assertThat( range.contains( new SteppableInt( 0 ) ) ).isFalse();
			assertThat( range.contains( new SteppableInt( 11 ) ) ).isFalse();
		}
	}

	// ======================== DateTime Ranges ========================

	@Nested
	@DisplayName( "DateTime Ranges" )
	class DateTimeRanges {

		@DisplayName( "It can create a DateTime range" )
		@Test
		void testDateTimeRange() {
			DateTime								start	= new DateTime( "2024-01-01" );
			DateTime								end		= new DateTime( "2024-01-05" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end );
			assertThat( range.isIterable() ).isTrue();
			assertThat( range.isAscending() ).isTrue();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).hasSize( 5 );
		}

		@DisplayName( "It can create a descending DateTime range" )
		@Test
		void testDescendingDateTimeRange() {
			DateTime								start	= new DateTime( "2024-01-05" );
			DateTime								end		= new DateTime( "2024-01-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end );
			assertThat( range.isIterable() ).isTrue();
			assertThat( range.isAscending() ).isFalse();

			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).hasSize( 5 );
		}

		@DisplayName( "DateTime range contains works" )
		@Test
		void testDateTimeContains() {
			DateTime								start	= new DateTime( "2024-01-01" );
			DateTime								end		= new DateTime( "2024-01-31" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end );

			assertThat( range.contains( new DateTime( "2024-01-15" ) ) ).isTrue();
			assertThat( range.contains( new DateTime( "2024-01-01" ) ) ).isTrue();
			assertThat( range.contains( new DateTime( "2024-01-31" ) ) ).isTrue();
			assertThat( range.contains( new DateTime( "2023-12-31" ) ) ).isFalse();
			assertThat( range.contains( new DateTime( "2024-02-01" ) ) ).isFalse();
		}

		@DisplayName( "DateTime range with step in hours" )
		@Test
		void testDateTimeStepHours() {
			DateTime								start	= new DateTime( "2024-01-01T00:00:00" );
			DateTime								end		= new DateTime( "2024-01-01T12:00:00" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 6, "hours" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// 00:00, 06:00, 12:00
			assertThat( values ).hasSize( 3 );
		}

		@DisplayName( "DateTime range with step in minutes" )
		@Test
		void testDateTimeStepMinutes() {
			DateTime								start	= new DateTime( "2024-01-01T00:00:00" );
			DateTime								end		= new DateTime( "2024-01-01T00:30:00" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 10, "minutes" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// 00:00, 00:10, 00:20, 00:30
			assertThat( values ).hasSize( 4 );
		}

		@DisplayName( "DateTime range with step in weeks" )
		@Test
		void testDateTimeStepWeeks() {
			DateTime								start	= new DateTime( "2024-01-01" );
			DateTime								end		= new DateTime( "2024-01-29" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "week" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// Jan 1, 8, 15, 22, 29
			assertThat( values ).hasSize( 5 );
		}

		@DisplayName( "step(amount, unit) throws on non-DateTime range" )
		@Test
		void testStepUnitThrowsOnIntRange() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThrows( BoxRuntimeException.class, () -> range.step( 5, "minutes" ) );
		}

		@DisplayName( "DateTime range with step in months" )
		@Test
		void testDateTimeStepMonths() {
			DateTime								start	= new DateTime( "2024-01-15" );
			DateTime								end		= new DateTime( "2024-06-15" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "month" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// Jan 15, Feb 15, Mar 15, Apr 15, May 15, Jun 15
			assertThat( values ).hasSize( 6 );
		}

		@DisplayName( "DateTime range with step in months (case insensitive)" )
		@Test
		void testDateTimeStepMonthsCaseInsensitive() {
			DateTime								start	= new DateTime( "2024-01-01" );
			DateTime								end		= new DateTime( "2024-04-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "MONTHS" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// Jan, Feb, Mar, Apr
			assertThat( values ).hasSize( 4 );
		}

		@DisplayName( "DateTime range with step in years" )
		@Test
		void testDateTimeStepYears() {
			DateTime								start	= new DateTime( "2020-06-01" );
			DateTime								end		= new DateTime( "2024-06-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "year" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// 2020, 2021, 2022, 2023, 2024
			assertThat( values ).hasSize( 5 );
		}

		@DisplayName( "DateTime range with step in years using shorthand" )
		@Test
		void testDateTimeStepYearsShorthand() {
			DateTime								start	= new DateTime( "2020-01-01" );
			DateTime								end		= new DateTime( "2022-01-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "yyyy" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// 2020, 2021, 2022
			assertThat( values ).hasSize( 3 );
		}

		@DisplayName( "DateTime range with step in months shorthand m" )
		@Test
		void testDateTimeStepMonthsShorthandM() {
			DateTime								start	= new DateTime( "2024-03-01" );
			DateTime								end		= new DateTime( "2024-05-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "m" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// Mar, Apr, May
			assertThat( values ).hasSize( 3 );
		}

		@DisplayName( "DateTime range with step in 2 months" )
		@Test
		void testDateTimeStepTwoMonths() {
			DateTime								start	= new DateTime( "2024-01-01" );
			DateTime								end		= new DateTime( "2024-07-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 2, "months" );

			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			// Jan, Mar, May, Jul
			assertThat( values ).hasSize( 4 );
		}
	}

	// ======================== Contains ========================

	@Nested
	@DisplayName( "Contains" )
	class Contains {

		@DisplayName( "Integer range contains works" )
		@Test
		void testIntRangeContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.contains( 5 ) ).isTrue();
			assertThat( range.contains( 1 ) ).isTrue();
			assertThat( range.contains( 10 ) ).isTrue();
			assertThat( range.contains( 0 ) ).isFalse();
			assertThat( range.contains( 11 ) ).isFalse();
		}

		@DisplayName( "Contains returns false for null" )
		@Test
		void testContainsNull() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.contains( null ) ).isFalse();
		}

		@DisplayName( "Contains returns false for incompatible type" )
		@Test
		void testContainsIncompatibleType() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.contains( "not a number" ) ).isFalse();
		}

		@DisplayName( "Contains coerces numeric strings for number range" )
		@Test
		void testContainsCoercesNumericString() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.contains( "5" ) ).isTrue();
			assertThat( range.contains( "11" ) ).isFalse();
		}

		@DisplayName( "Character range contains coerces single-char strings" )
		@Test
		void testCharRangeContainsCoercion() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "a", "z" );
			assertThat( range.contains( 'm' ) ).isTrue();
			assertThat( range.contains( "m" ) ).isTrue();
			assertThat( range.contains( 'a' ) ).isTrue();
			assertThat( range.contains( 'z' ) ).isTrue();
			assertThat( range.contains( 'A' ) ).isFalse();
		}

		@DisplayName( "Comparable string range contains works" )
		@Test
		void testStringRangeContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "aaa", "zzz" );
			assertThat( range.contains( "foo" ) ).isTrue();
			assertThat( range.contains( "aaa" ) ).isTrue();
			assertThat( range.contains( "zzz" ) ).isTrue();
			assertThat( range.contains( "000" ) ).isFalse();
		}

		@DisplayName( "Descending range contains works" )
		@Test
		void testDescendingRangeContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 10, 1 );
			assertThat( range.contains( 5 ) ).isTrue();
			assertThat( range.contains( 1 ) ).isTrue();
			assertThat( range.contains( 10 ) ).isTrue();
			assertThat( range.contains( 0 ) ).isFalse();
			assertThat( range.contains( 11 ) ).isFalse();
		}

		@DisplayName( "Unbounded range contains everything non-null" )
		@Test
		void testUnboundedRangeContains() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( null, null );
			assertThat( range.contains( 42 ) ).isTrue();
			assertThat( range.contains( "anything" ) ).isTrue();
			assertThat( range.contains( null ) ).isFalse();
		}

		@DisplayName( "Half-bounded ascending range contains" )
		@Test
		void testHalfBoundedAscendingContains() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( 5, null );
			assertThat( range.contains( 5 ) ).isTrue();
			assertThat( range.contains( 100 ) ).isTrue();
			assertThat( range.contains( 4 ) ).isFalse();
		}

		@DisplayName( "Half-bounded to-only range contains" )
		@Test
		void testHalfBoundedToOnlyContains() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( null, 10 );
			assertThat( range.contains( 10 ) ).isTrue();
			assertThat( range.contains( -100 ) ).isTrue();
			assertThat( range.contains( 11 ) ).isFalse();
		}
	}

	// ======================== Copy-on-Write Modifiers ========================

	@Nested
	@DisplayName( "Copy-on-Write Modifiers" )
	class CopyOnWrite {

		@DisplayName( "It can use step() to change step" )
		@Test
		void testCustomStep() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 10 ).step( 2 );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 1, 3, 5, 7, 9 ).inOrder();
		}

		@DisplayName( "It can use step() with a negative value" )
		@Test
		void testNegativeStep() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 10, 1 ).step( -3 );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 10, 7, 4, 1 ).inOrder();
		}

		@DisplayName( "desc() on ascending range produces empty range" )
		@Test
		void testDesc() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5 ).desc();
			assertThat( range.isEmpty() ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).isEmpty();
		}

		@DisplayName( "step(-1) on ascending range produces empty range" )
		@Test
		void testNegativeStepSwapsBounds() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5 ).step( -1 );
			assertThat( range.isEmpty() ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).isEmpty();
		}

		@DisplayName( "step(2) on descending range produces empty range" )
		@Test
		void testPositiveStepOnDescendingSwapsBounds() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 5, 1 ).step( 2 );
			assertThat( range.isEmpty() ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).isEmpty();
		}

		@DisplayName( "asc() on descending range produces empty range" )
		@Test
		void testAsc() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 5, 1 ).asc();
			assertThat( range.isEmpty() ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).isEmpty();
		}

		@DisplayName( "step() returns a new instance" )
		@Test
		void testStepReturnsNewInstance() {
			ortus.boxlang.runtime.types.Range<?>	original	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	stepped		= original.step( 3 );
			assertThat( stepped ).isNotSameInstanceAs( original );
			assertThat( original.getStep() ).isEqualTo( 1 );
			assertThat( stepped.getStep() ).isEqualTo( 3 );
		}
	}

	// ======================== Boundary Query Methods ========================

	@Nested
	@DisplayName( "Boundary Queries" )
	class BoundaryQueries {

		@DisplayName( "isBounded returns true when both bounds present" )
		@Test
		void testIsBounded() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.isBounded() ).isTrue();
			assertThat( range.isUnbounded() ).isFalse();
			assertThat( range.isHalfBounded() ).isFalse();
			assertThat( range.hasFrom() ).isTrue();
			assertThat( range.hasTo() ).isTrue();
		}

		@DisplayName( "isUnbounded returns true when no bounds" )
		@Test
		void testIsUnbounded() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( null, null );
			assertThat( range.isUnbounded() ).isTrue();
			assertThat( range.isBounded() ).isFalse();
			assertThat( range.isHalfBounded() ).isFalse();
			assertThat( range.hasFrom() ).isFalse();
			assertThat( range.hasTo() ).isFalse();
		}

		@DisplayName( "isHalfBounded returns true for one-sided ranges" )
		@Test
		void testIsHalfBounded() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( 5, null );
			assertThat( range.isHalfBounded() ).isTrue();
			assertThat( range.isBounded() ).isFalse();
			assertThat( range.isUnbounded() ).isFalse();
			assertThat( range.hasFrom() ).isTrue();
			assertThat( range.hasTo() ).isFalse();
		}

		@DisplayName( "getFrom and getTo return correct values" )
		@Test
		void testGetFromAndTo() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.getFrom() ).isEqualTo( 1 );
			assertThat( range.getTo() ).isEqualTo( 10 );
		}

		@DisplayName( "getStep returns the step" )
		@Test
		void testGetStep() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 ).step( 3 );
			assertThat( range.getStep() ).isEqualTo( 3 );
		}
	}

	// ======================== toArray ========================

	@Nested
	@DisplayName( "toArray" )
	class ToArrayTests {

		@DisplayName( "It can materialize to a BoxLang array" )
		@Test
		void testToArray() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5 );
			ortus.boxlang.runtime.types.Array		arr		= range.toArray();
			assertThat( arr.size() ).isEqualTo( 5 );
			assertThat( arr.get( 0 ) ).isEqualTo( 1 );
			assertThat( arr.get( 4 ) ).isEqualTo( 5 );
		}

		@DisplayName( "toArray with custom step" )
		@Test
		void testToArrayWithStep() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 10 ).step( 3 );
			ortus.boxlang.runtime.types.Array		arr		= range.toArray();
			assertThat( arr.size() ).isEqualTo( 4 );
			assertThat( arr.get( 0 ) ).isEqualTo( 1 );
			assertThat( arr.get( 1 ) ).isEqualTo( 4 );
			assertThat( arr.get( 2 ) ).isEqualTo( 7 );
			assertThat( arr.get( 3 ) ).isEqualTo( 10 );
		}

		@DisplayName( "toArray throws on non-iterable range" )
		@Test
		void testToArrayThrowsNonIterable() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "aaa", "zzz" );
			assertThrows( BoxRuntimeException.class, () -> range.toArray() );
		}

		@DisplayName( "toArray throws on unbounded range" )
		@Test
		void testToArrayThrowsUnbounded() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( 1, null );
			assertThrows( BoxRuntimeException.class, () -> range.toArray() );
		}
	}

	// ======================== stream ========================

	@Nested
	@DisplayName( "stream" )
	class StreamTests {

		@DisplayName( "It can stream an integer range" )
		@Test
		void testStreamIntRange() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5 );
			List<?>									values	= range.stream().collect( Collectors.toList() );
			assertThat( values ).containsExactly( 1, 2, 3, 4, 5 ).inOrder();
		}

		@DisplayName( "It can stream a character range" )
		@Test
		void testStreamCharRange() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( "a", "e" );
			List<?>									values	= range.stream().collect( Collectors.toList() );
			assertThat( values ).containsExactly( 'a', 'b', 'c', 'd', 'e' ).inOrder();
		}

		@DisplayName( "Stream with filter works" )
		@Test
		void testStreamWithFilter() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 10 );
			List<?>									values	= range.stream()
			    .filter( v -> ( ( Number ) v ).intValue() % 2 == 0 )
			    .collect( Collectors.toList() );
			assertThat( values ).containsExactly( 2, 4, 6, 8, 10 ).inOrder();
		}

		@DisplayName( "Stream with map works" )
		@Test
		void testStreamWithMap() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5 );
			List<?>									values	= range.stream()
			    .map( v -> ( ( Number ) v ).intValue() * 2 )
			    .collect( Collectors.toList() );
			assertThat( values ).containsExactly( 2, 4, 6, 8, 10 ).inOrder();
		}

		@DisplayName( "Stream count works" )
		@Test
		void testStreamCount() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 100 );
			assertThat( range.stream().count() ).isEqualTo( 100 );
		}

		@DisplayName( "stream() throws on non-iterable range" )
		@Test
		void testStreamThrowsNonIterable() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( "aaa", "zzz" );
			assertThrows( BoxRuntimeException.class, () -> range.stream() );
		}
	}

	// ======================== Exclusive Ranges ========================

	@Nested
	@DisplayName( "Exclusive Ranges" )
	class ExclusiveRanges {

		// --- Left Exclusive (>..) ---

		@DisplayName( "left exclusive integer range excludes from value" )
		@Test
		void testLeftExclusiveIntContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5, true, false );
			assertThat( range.contains( 1 ) ).isFalse();
			assertThat( range.contains( 2 ) ).isTrue();
			assertThat( range.contains( 5 ) ).isTrue();
			assertThat( range.contains( null ) ).isFalse();
		}

		@DisplayName( "left exclusive integer range iterates from next value" )
		@Test
		void testLeftExclusiveIntIteration() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5, true, false );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 2, 3, 4, 5 ).inOrder();
		}

		@DisplayName( "left exclusive descending range excludes from and iterates correctly" )
		@Test
		void testLeftExclusiveDescending() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 5, 1, true, false );
			assertThat( range.contains( 5 ) ).isFalse();
			assertThat( range.contains( 4 ) ).isTrue();
			assertThat( range.contains( 1 ) ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 4, 3, 2, 1 ).inOrder();
		}

		// --- Right Exclusive (..<) ---

		@DisplayName( "right exclusive integer range excludes to value" )
		@Test
		void testRightExclusiveIntContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5, false, true );
			assertThat( range.contains( 1 ) ).isTrue();
			assertThat( range.contains( 4 ) ).isTrue();
			assertThat( range.contains( 5 ) ).isFalse();
			assertThat( range.contains( null ) ).isFalse();
		}

		@DisplayName( "right exclusive integer range iterates up to but not including to" )
		@Test
		void testRightExclusiveIntIteration() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5, false, true );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 1, 2, 3, 4 ).inOrder();
		}

		@DisplayName( "right exclusive descending range excludes to and iterates correctly" )
		@Test
		void testRightExclusiveDescending() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 5, 1, false, true );
			assertThat( range.contains( 5 ) ).isTrue();
			assertThat( range.contains( 2 ) ).isTrue();
			assertThat( range.contains( 1 ) ).isFalse();
			List<Object> values = new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 5, 4, 3, 2 ).inOrder();
		}

		// --- Both Exclusive (>..<) ---

		@DisplayName( "both exclusive integer range excludes both bounds" )
		@Test
		void testBothExclusiveIntContains() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5, true, true );
			assertThat( range.contains( 1 ) ).isFalse();
			assertThat( range.contains( 2 ) ).isTrue();
			assertThat( range.contains( 4 ) ).isTrue();
			assertThat( range.contains( 5 ) ).isFalse();
			assertThat( range.contains( null ) ).isFalse();
		}

		@DisplayName( "both exclusive integer range iterates without bounds" )
		@Test
		void testBothExclusiveIntIteration() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5, true, true );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 2, 3, 4 ).inOrder();
		}

		@DisplayName( "both exclusive descending range iterates correctly" )
		@Test
		void testBothExclusiveDescending() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 5, 1, true, true );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 4, 3, 2 ).inOrder();
		}

		// --- Edge Cases ---

		@DisplayName( "exclusive range where from == to is empty" )
		@Test
		void testExclusiveSameValueIsEmpty() {
			ortus.boxlang.runtime.types.Range<?> leftEx = Range.invoke( 3, 3, true, false );
			assertThat( leftEx.isEmpty() ).isTrue();

			ortus.boxlang.runtime.types.Range<?> rightEx = Range.invoke( 3, 3, false, true );
			assertThat( rightEx.isEmpty() ).isTrue();

			ortus.boxlang.runtime.types.Range<?> bothEx = Range.invoke( 3, 3, true, true );
			assertThat( bothEx.isEmpty() ).isTrue();
		}

		@DisplayName( "exclusive range with adjacent values: both exclusive is empty" )
		@Test
		void testExclusiveAdjacentValuesEmpty() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 2, true, true );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).isEmpty();
		}

		@DisplayName( "exclusive character range works" )
		@Test
		void testExclusiveCharRange() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 'a', 'e', true, true );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : range ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 'b', 'c', 'd' ).inOrder();
		}

		@DisplayName( "exclusive range preserves exclusivity through asc()" )
		@Test
		void testExclusivityPreservedAsc() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 5, true, true );
			ortus.boxlang.runtime.types.Range<?>	asc		= range.asc();
			assertThat( asc.isFromExclusive() ).isTrue();
			assertThat( asc.isToExclusive() ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : asc ) {
				values.add( v );
			}
			assertThat( values ).containsExactly( 2, 3, 4 ).inOrder();
		}

		@DisplayName( "exclusive range preserves exclusivity through desc()" )
		@Test
		void testExclusivityPreservedDesc() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 5, 1, true, true );
			ortus.boxlang.runtime.types.Range<?>	desc	= range.desc();
			assertThat( desc.isFromExclusive() ).isTrue();
			assertThat( desc.isToExclusive() ).isTrue();
		}

		@DisplayName( "exclusive range preserves exclusivity through step()" )
		@Test
		void testExclusivityPreservedStep() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 10, true, true );
			ortus.boxlang.runtime.types.Range<?>	stepped	= range.step( 2 );
			assertThat( stepped.isFromExclusive() ).isTrue();
			assertThat( stepped.isToExclusive() ).isTrue();
			List<Object> values = new ArrayList<>();
			for ( Object v : stepped ) {
				values.add( v );
			}
			// start at 2 (1 excluded), step by 2: 2, 4, 6, 8 (10 excluded)
			assertThat( values ).containsExactly( 2, 4, 6, 8 ).inOrder();
		}

		@DisplayName( "asString() uses correct exclusive operators" )
		@Test
		void testAsStringExclusive() {
			assertThat( Range.invoke( 1, 5, true, false ).asString() ).isEqualTo( "1>..5" );
			assertThat( Range.invoke( 1, 5, false, true ).asString() ).isEqualTo( "1..<5" );
			assertThat( Range.invoke( 1, 5, true, true ).asString() ).isEqualTo( "1>..<5" );
			assertThat( Range.invoke( 1, 5, false, false ).asString() ).isEqualTo( "1..5" );
		}

		@DisplayName( "equals() distinguishes inclusive from exclusive" )
		@Test
		void testEqualsExclusive() {
			ortus.boxlang.runtime.types.Range<?>	inclusive	= Range.invoke( 1, 5, false, false );
			ortus.boxlang.runtime.types.Range<?>	leftEx		= Range.invoke( 1, 5, true, false );
			ortus.boxlang.runtime.types.Range<?>	rightEx		= Range.invoke( 1, 5, false, true );
			ortus.boxlang.runtime.types.Range<?>	bothEx		= Range.invoke( 1, 5, true, true );

			assertThat( inclusive ).isNotEqualTo( leftEx );
			assertThat( inclusive ).isNotEqualTo( rightEx );
			assertThat( inclusive ).isNotEqualTo( bothEx );
			assertThat( leftEx ).isNotEqualTo( rightEx );
			assertThat( leftEx ).isNotEqualTo( bothEx );
		}

		@DisplayName( "right exclusive with step(2) stops correctly" )
		@Test
		void testRightExclusiveWithStep() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( 1, 10, false, true );
			ortus.boxlang.runtime.types.Range<?>	stepped	= range.step( 3 );
			List<Object>							values	= new ArrayList<>();
			for ( Object v : stepped ) {
				values.add( v );
			}
			// 1, 4, 7 (10 excluded)
			assertThat( values ).containsExactly( 1, 4, 7 ).inOrder();
		}

		@DisplayName( "left exclusive half-bounded range" )
		@Test
		void testLeftExclusiveHalfBounded() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( 5, null, true, false );
			assertThat( range.contains( 5 ) ).isFalse();
			assertThat( range.contains( 6 ) ).isTrue();
			assertThat( range.contains( 100 ) ).isTrue();
		}

		@DisplayName( "right exclusive half-bounded range" )
		@Test
		void testRightExclusiveHalfBounded() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( null, 10, false, true );
			assertThat( range.contains( 10 ) ).isFalse();
			assertThat( range.contains( 9 ) ).isTrue();
			assertThat( range.contains( -100 ) ).isTrue();
		}
	}

	// ======================== Range-in-Range Contains ========================

	@Nested
	@DisplayName( "Range-in-Range Contains" )
	class RangeInRangeContains {

		@DisplayName( "inner range fully within outer range" )
		@Test
		void testInnerFullyWithin() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 3, 7 );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "inner range equals outer range" )
		@Test
		void testInnerEqualsOuter() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 10 );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "inner range exceeds outer on high end" )
		@Test
		void testInnerExceedsHigh() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 5, 15 );
			assertThat( outer.contains( inner ) ).isFalse();
		}

		@DisplayName( "inner range exceeds outer on low end" )
		@Test
		void testInnerExceedsLow() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 5, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 7 );
			assertThat( outer.contains( inner ) ).isFalse();
		}

		@DisplayName( "inner exclusive fits inside outer inclusive at same bounds" )
		@Test
		void testInnerExclusiveFitsInOuter() {
			// outer: 1..10 (inclusive), inner: 1>..10 (left exclusive)
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 10, true, false );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "inner inclusive does NOT fit in outer exclusive at same bounds" )
		@Test
		void testInnerInclusiveNotInOuterExclusive() {
			// outer: 1>..10 (left exclusive), inner: 1..10 (inclusive) — inner includes 1 but outer doesn't
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10, true, false );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 10 );
			assertThat( outer.contains( inner ) ).isFalse();
		}

		@DisplayName( "both exclusive same bounds — inner fits" )
		@Test
		void testBothExclusiveSameBounds() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10, true, true );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 10, true, true );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "inner right-exclusive fits in outer right-exclusive same bounds" )
		@Test
		void testRightExclusiveSameBounds() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10, false, true );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 3, 10, false, true );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "inner inclusive high does NOT fit in outer right-exclusive same high" )
		@Test
		void testInnerInclusiveHighNotInOuterExclusive() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10, false, true );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 3, 10, false, false );
			assertThat( outer.contains( inner ) ).isFalse();
		}

		@DisplayName( "character range contains inner character range" )
		@Test
		void testCharRangeContains() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 'a', 'z' );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 'd', 'g' );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "unbounded outer contains any inner" )
		@Test
		void testUnboundedOuterContainsAny() {
			ortus.boxlang.runtime.types.Range<?>	outer	= ortus.boxlang.runtime.types.Range.of( null, null );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 100 );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "bounded outer does NOT contain unbounded inner" )
		@Test
		void testBoundedDoesNotContainUnbounded() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= ortus.boxlang.runtime.types.Range.of( null, null );
			assertThat( outer.contains( inner ) ).isFalse();
		}

		@DisplayName( "half-bounded outer contains matching half-bounded inner" )
		@Test
		void testHalfBoundedContainsHalfBounded() {
			// outer: 1.. (no upper), inner: 5.. (no upper, higher start)
			ortus.boxlang.runtime.types.Range<?>	outer	= ortus.boxlang.runtime.types.Range.of( 1, null );
			ortus.boxlang.runtime.types.Range<?>	inner	= ortus.boxlang.runtime.types.Range.of( 5, null );
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "half-bounded outer does NOT contain fully bounded inner outside" )
		@Test
		void testHalfBoundedRejectsOutOfRange() {
			// outer: 5.. (no upper), inner: 1..3 (below outer's start)
			ortus.boxlang.runtime.types.Range<?>	outer	= ortus.boxlang.runtime.types.Range.of( 5, null );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 1, 3 );
			assertThat( outer.contains( inner ) ).isFalse();
		}

		@DisplayName( "empty inner range is always contained" )
		@Test
		void testEmptyInnerIsContained() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			// 5..3 with step 1 is empty (ascending but from > to)
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 5, 3 );
			inner = inner.asc(); // force ascending step, from=5, to=3 → empty
			assertThat( inner.isEmpty() ).isTrue();
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "descending inner within outer" )
		@Test
		void testDescendingInnerWithin() {
			ortus.boxlang.runtime.types.Range<?>	outer	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	inner	= Range.invoke( 8, 3 ); // descending but bounds are 3..8
			assertThat( outer.contains( inner ) ).isTrue();
		}

		@DisplayName( "different element types returns false" )
		@Test
		void testDifferentTypesReturnsFalse() {
			ortus.boxlang.runtime.types.Range<?>	intRange	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	charRange	= Range.invoke( 'a', 'z' );
			assertThat( intRange.contains( charRange ) ).isFalse();
			assertThat( charRange.contains( intRange ) ).isFalse();
		}
	}

	// ======================== asString ========================

	@Nested
	@DisplayName( "asString" )
	class AsStringTests {

		@DisplayName( "asString shows basic range notation" )
		@Test
		void testAsStringBasic() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.asString() ).isEqualTo( "1..10" );
		}

		@DisplayName( "asString shows desc() for step -1" )
		@Test
		void testAsStringDesc() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 5, 1 );
			assertThat( range.asString() ).isEqualTo( "5..1.desc()" );
		}

		@DisplayName( "asString shows step() for non-default step" )
		@Test
		void testAsStringCustomStep() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 ).step( 3 );
			assertThat( range.asString() ).isEqualTo( "1..10.step(3)" );
		}

		@DisplayName( "asString shows step() for negative non-default step (no desc)" )
		@Test
		void testAsStringNegativeCustomStep() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 10, 1 ).step( -2 );
			assertThat( range.asString() ).isEqualTo( "10..1.step(-2)" );
		}

		@DisplayName( "asString handles half-bounded ranges" )
		@Test
		void testAsStringHalfBounded() {
			ortus.boxlang.runtime.types.Range<?> range = ortus.boxlang.runtime.types.Range.of( 5, null );
			assertThat( range.asString() ).isEqualTo( "5.." );
		}

		@DisplayName( "toString delegates to asString" )
		@Test
		void testToStringDelegatesToAsString() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 5 );
			assertThat( range.toString() ).isEqualTo( range.asString() );
		}

		@DisplayName( "asString shows unit in step" )
		@Test
		void testAsStringWithUnit() {
			DateTime								start	= new DateTime( "2024-01-01" );
			DateTime								end		= new DateTime( "2024-06-01" );
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( start, end ).step( 1, "month" );
			assertThat( range.asString() ).contains( ".step(1, \"month\")" );
		}
	}

	// ======================== Equality & HashCode ========================

	@Nested
	@DisplayName( "Equality & HashCode" )
	class EqualityAndHashCode {

		@DisplayName( "Equal ranges are equal" )
		@Test
		void testEquality() {
			ortus.boxlang.runtime.types.Range<?>	range1	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	range2	= Range.invoke( 1, 10 );
			assertThat( range1 ).isEqualTo( range2 );
			assertThat( range1.hashCode() ).isEqualTo( range2.hashCode() );
		}

		@DisplayName( "Unequal ranges are not equal" )
		@Test
		void testInequality() {
			ortus.boxlang.runtime.types.Range<?>	range1	= Range.invoke( 1, 10 );
			ortus.boxlang.runtime.types.Range<?>	range2	= Range.invoke( 1, 5 );
			assertThat( range1 ).isNotEqualTo( range2 );
		}

		@DisplayName( "Range is not equal to non-Range" )
		@Test
		void testNotEqualToOtherType() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.equals( "1..10" ) ).isFalse();
		}

		@DisplayName( "Range is equal to itself" )
		@Test
		void testEqualToSelf() {
			ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
			assertThat( range.equals( range ) ).isTrue();
		}
	}

	// ======================== Error Cases ========================

	@Nested
	@DisplayName( "Error Cases" )
	class ErrorCases {

		@DisplayName( "It throws on incompatible types" )
		@Test
		void testIncompatibleTypes() {
			assertThrows( BoxRuntimeException.class, () -> Range.invoke( new ArrayList<>(), "foo" ) );
		}

		@DisplayName( "iterator() throws on non-iterable range with helpful message" )
		@Test
		void testIteratorThrowsWithMessage() {
			ortus.boxlang.runtime.types.Range<?>	range	= Range.invoke( "aaa", "zzz" );
			BoxRuntimeException						ex		= assertThrows( BoxRuntimeException.class, () -> range.iterator() );
			assertThat( ex.getMessage() ).contains( "not iterable" );
			assertThat( ex.getMessage() ).contains( "String" );
		}
	}

	// ======================== BoxTypeName ========================

	@DisplayName( "getBoxTypeName returns Range" )
	@Test
	void testBoxTypeName() {
		ortus.boxlang.runtime.types.Range<?> range = Range.invoke( 1, 10 );
		assertThat( range.getBoxTypeName() ).isEqualTo( "Range" );
	}
}
