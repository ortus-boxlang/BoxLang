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
package ortus.boxlang.runtime.async.tasks;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.management.InvalidAttributeValueException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import ortus.boxlang.runtime.async.executors.BoxScheduledExecutor;

class ScheduledTaskTest {

	ScheduledTask task;

	@BeforeEach
	public void setupBeforeEach() {
		task = new ScheduledTask( "test", new BoxScheduledExecutor( 20 ) )
		    .setTimezone( "America/Chicago" );
	}

	@DisplayName( "It can create the scheduled task with the right timezone" )
	@Test
	void testItCanCreateIt() {
		assertThat( task ).isNotNull();
		assertThat( task.getTimezone().getId() ).isEqualTo( "America/Chicago" );
	}

	@DisplayName( "can have truth based restrictions using when()" )
	@Test
	void testItCanHaveTruthBasedRestrictions() {
		task.when( ( task ) -> true );
		assertThat( task.getWhenPredicate() ).isNotNull();
	}

	@DisplayName( "can be disabled" )
	@Test
	void testItCanBeDisabled() {
		task.disable();
		assertThat( task.isDisabled() ).isTrue();
	}

	@Nested
	class LifeCycleMethods {

		@DisplayName( "can call before" )
		@Test
		void testItCanCallBefore() {
			task.before( ( task ) -> {
			} );
			assertThat( task.getBeforeTask() ).isNotNull();
		}

		@DisplayName( "can call after" )
		@Test
		void testItCanCallAfter() {
			task.after( ( task, result ) -> {
			} );
			assertThat( task.getAfterTask() ).isNotNull();
		}

		@DisplayName( "can call onTaskSuccess" )
		@Test
		void testItCanCallOnTaskSuccess() {
			task.onSuccess( ( task, result ) -> {
			} );
			assertThat( task.getOnTaskSuccess() ).isNotNull();
		}

		@DisplayName( "can call onFailure" )
		@Test
		void testItCanCallOnFailure() {
			task.onFailure( ( task, exception ) -> {
			} );
			assertThat( task.getOnTaskFailure() ).isNotNull();
		}
	}

	@Nested
	class MultipleFrequencyEveryMethods {

		@TestFactory
		@DisplayName( "Can register every <timeUnit> as a period of execution" )
		Stream<DynamicTest> testEveryTimeUnit() {
			return Arrays
			    .stream( TimeUnit.values() )
			    .map( timeUnit -> dynamicTest(
			        "Test for " + timeUnit.toString(),
			        () -> {
				        var t = task.every( 5, timeUnit );
				        assertEquals( 5, t.getPeriod() );
				        assertEquals( timeUnit, t.getTimeUnit() );
			        }
			    ) );
		}

		@DisplayName( "can register using everyMinute()" )
		@Test
		void testEveryMinute() {
			var t = task.everyMinute();
			assertThat( t.getPeriod() ).isEqualTo( 1 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "minutes" );
		}

		@DisplayName( "can register everyHour()" )
		@Test
		void testEveryHour() {
			var t = task.everyHour();
			assertThat( t.getPeriod() ).isEqualTo( 1 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "hours" );
		}

		@DisplayName( "can register everyHourAt()" )
		@Test
		void testEveryHourAt() {
			var t = task.everyHourAt( 15 );
			assertThat( t.getDelay() ).isNotEqualTo( 0 );
			assertThat( t.getPeriod() ).isEqualTo( 3600 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyDay()" )
		@Test
		void testEveryDay() throws InvalidAttributeValueException {
			var t = task.everyDay();
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyDayAt()" )
		@Test
		void testEveryDayAt() throws InvalidAttributeValueException {
			var t = task.everyDayAt( "04:00" );
			assertThat( t.getDelay() ).isNotEqualTo( 0 );
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyWeek()" )
		@Test
		void testCanRegisterEveryWeek() throws InvalidAttributeValueException {
			var t = task.everyWeek();
			assertThat( t.getPeriod() ).isEqualTo( 604800 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyWeekOn()" )
		@Test
		void testCanRegisterEveryWeekOn() throws InvalidAttributeValueException {
			var t = task.everyWeekOn( 4, "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 604800 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyMonth()" )
		@Test
		void testCanRegisterEveryMonth() throws InvalidAttributeValueException {
			var t = task.everyMonth();
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyMonthOn()" )
		@Test
		void testCanRegisterEveryMonthOn() throws InvalidAttributeValueException {
			var t = task.everyMonthOn( 10, "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyYear()" )
		@Test
		void testCanRegisterEveryYear() throws InvalidAttributeValueException {
			var t = task.everyYear();
			assertThat( t.getPeriod() ).isEqualTo( 31536000 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

		@DisplayName( "can register everyYearOn()" )
		@Test
		void testCanRegisterEveryYearOn() throws InvalidAttributeValueException {
			var t = task.everyYearOn( 4, 15, "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 31536000 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
		}

	}

	@Nested
	class MultipleFrequenciesWithConstraints {

		@DisplayName( "can register to fire onFirstBusinessDayOfTheMonth()" )
		@Test
		void testcanRegisterToFireOnFirstBusinessDayOfTheMonth() throws InvalidAttributeValueException {
			var t = task.onFirstBusinessDayOfTheMonth( "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
			assertThat( t.getTaskTime() ).isEqualTo( "09:00" );
			assertThat( t.getFirstBusinessDay() ).isTrue();
		}

		@DisplayName( "can register to fire onLastBusinessDayOfTheMonth()" )
		@Test
		void testcanRegisterToFireOnLastBusinessDayOfTheMonth() throws InvalidAttributeValueException {
			var t = task.onLastBusinessDayOfTheMonth( "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
			assertThat( t.getLastBusinessDay() ).isTrue();
		}

		@DisplayName( "can register to fire onWeekends()" )
		@Test
		void testcanRegisterToFireOnWeekends() throws InvalidAttributeValueException {
			var t = task.onWeekends( "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
			assertThat( t.getWeekends() ).isTrue();
			assertThat( t.getWeekdays() ).isFalse();
		}

		@DisplayName( "can register to fire onWeekdays()" )
		@Test
		void testcanRegisterToFireOnWeekdays() throws InvalidAttributeValueException {
			var t = task.onWeekdays( "09:00" );
			assertThat( t.getPeriod() ).isEqualTo( 86400 );
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
			assertThat( t.getWeekends() ).isFalse();
			assertThat( t.getWeekdays() ).isTrue();
		}

	}
}
