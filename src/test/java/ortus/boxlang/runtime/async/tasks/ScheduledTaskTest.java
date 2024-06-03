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

import java.time.LocalDateTime;
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
import org.mockito.Mockito;

import ortus.boxlang.runtime.async.executors.BoxScheduledExecutor;
import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.util.DateTimeHelper;

class ScheduledTaskTest {

	ScheduledTask task;

	@BeforeEach
	public void setupBeforeEach() {

		ExecutorRecord executor = new ExecutorRecord(
		    new BoxScheduledExecutor( 20 ),
		    "test",
		    AsyncService.ExecutorType.SCHEDULED,
		    20
		);

		task	= new ScheduledTask( "test", executor ).setTimezone( "America/Chicago" );
		task	= Mockito.spy( task );
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

		@DisplayName( "can register tasks with no overlaps" )
		@Test
		void testItCanRegisterTasksWithNoOverlaps() {
			assertThat( task.getNoOverlaps() ).isFalse();
			var t = task.everyMinute().withNoOverlaps();
			assertThat( t.getPeriod() ).isEqualTo( 1 );
			assertThat( t.getNoOverlaps() ).isTrue();
			assertThat( t.getTimeUnit().toString().toLowerCase() ).isEqualTo( "minutes" );
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
			assertThat( t.getInitialDelay() ).isNotEqualTo( 0 );
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
			assertThat( t.getInitialDelay() ).isNotEqualTo( 0 );
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

		@TestFactory
		@DisplayName( "Can register every day of the week constraint" )
		Stream<DynamicTest> testEveryDayOfTheWeek() {
			String[] daysOfTheWeek = {
			    "mondays",
			    "tuesdays",
			    "wednesdays",
			    "thursdays",
			    "fridays",
			    "saturdays",
			    "sundays"
			};
			return Arrays
			    .stream( daysOfTheWeek )
			    .map( dayOfTheWeek -> dynamicTest(
			        "Can register to fire on " + dayOfTheWeek,
			        () -> {
				        var t = new DynamicObject( task );
				        t.invoke( "on" + dayOfTheWeek );

				        assertThat( task.getPeriod() ).isEqualTo( 604800 );
				        assertThat( task.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
			        }
			    ) );
		}
	}

	@Nested
	class MultipleConstraints {

		@DisplayName( "can have a truth value constraint" )
		@Test
		void testCanHaveATruthValueConstraint() {
			task.when( ( task ) -> false );
			assertThat( task.isConstrained() ).isTrue();
		}

		@DisplayName( "can have a day of the month constraint" )
		@Test
		void testCanHaveADayOfTheMonthConstraint() {
			var target = task
			    .getNow()
			    .plusDays( 3 )
			    .getDayOfMonth();

			task.setDayOfTheMonth( target );

			assertThat( task.isConstrained() ).isTrue();

			target = task.getNow().getDayOfMonth();
			task.setDayOfTheMonth( task.getNow().getDayOfMonth() );
			assertThat( task.isConstrained() ).isFalse();
		}

		@DisplayName( "can have a last business day of the month constraint" )
		@Test
		void testCanHaveLastBusinessDayOfMonthConstraint() {
			var	mockNow	= DateTimeHelper.now();
			var	t		= task.setLastBusinessDay( true );

			// If we are at the last day, increase it
			if ( mockNow.getDayOfMonth() == DateTimeHelper.getLastBusinessDayOfTheMonth().getDayOfMonth() ) {
				mockNow = mockNow.plusDays( -1 );
			}

			Mockito.when( task.getNow() ).thenReturn( mockNow );
			assertThat( t.isConstrained() ).isTrue();

			LocalDateTime lastDayOfTheMonth = DateTimeHelper.getLastBusinessDayOfTheMonth();
			Mockito.when( task.getNow() ).thenReturn( lastDayOfTheMonth );
			assertThat( t.isConstrained() ).isFalse();
		};

		@DisplayName( "can have a day of the week constraint" )
		@Test
		void testCanHaveADayOfTheWeekConstraint() {
			var mockNow = DateTimeHelper.now( task.getTimezone() );

			// Reduce date enough to do computations on it
			if ( mockNow.getDayOfWeek().getValue() > 6 ) {
				mockNow = mockNow.minusDays( 3 );
			}

			// Mock to today + 1 so it constraints it
			task.setDayOfTheWeek( mockNow.getDayOfWeek().getValue() + 1 );
			assertThat( task.isConstrained() ).isTrue();

			// Mock to today it it runs
			task.setDayOfTheWeek( DateTimeHelper.now( task.getTimezone() ).getDayOfWeek().getValue() );
			assertThat( task.isConstrained() ).isFalse();
		};

		@DisplayName( "can have a weekend constraint" )
		@Test
		void testCanHaveAWeekendConstraint() {
			task.setWeekends( true );

			// build a weekend date
			var	mockNow		= task.getNow();
			var	dayOfWeek	= mockNow.getDayOfWeek().getValue();

			if ( dayOfWeek < 6 ) {
				mockNow = mockNow.plusDays( 6 - dayOfWeek );
			}

			Mockito.when( task.getNow() ).thenReturn( mockNow );
			assertThat( task.isConstrained() ).isFalse();

			// Test non weekend
			mockNow = mockNow.minusDays( 3 );
			Mockito.when( task.getNow() ).thenReturn( mockNow );
			assertThat( task.isConstrained() ).isTrue();
		}

		@DisplayName( "can have a weekday constraint" )
		@Test
		void testCanHaveAWeekDayConstraint() {
			task.setWeekdays( true );

			// build a weekend date
			var	mockNow		= task.getNow();
			var	dayOfWeek	= mockNow.getDayOfWeek().getValue();

			if ( dayOfWeek >= 6 ) {
				mockNow = mockNow.minusDays( 3 );
			}

			Mockito.when( task.getNow() ).thenReturn( mockNow );
			assertThat( task.isConstrained() ).isFalse();

			// Test non weekend
			var weekendDay = mockNow.plusDays( 6 - mockNow.getDayOfWeek().getValue() );
			Mockito.when( task.getNow() ).thenReturn( weekendDay );
			assertThat( task.isConstrained() ).isTrue();
		}

		@DisplayName( "can have a startOn constraints" )
		@Test
		void testCanHaveAStartOnConstraint() {
			var targetDate = "2022-01-01";
			task.startOn( targetDate, "09:00" );
			assertThat( task.isConstrained() ).isFalse();

			var mockNow = task.getNow().plusDays( 1 );
			task.startOn( mockNow.format( DateTimeHelper.ISO_DATE_ONLY ) );
			assertThat( task.isConstrained() ).isTrue();
		}

		@DisplayName( "can have an endOn constraints" )
		@Test
		void testCanHaveAEndOnConstraint() {
			// End 5 days from today
			var targetDate = DateTimeHelper.dateTimeAdd( task.getNow(), 5, TimeUnit.DAYS );
			task.endOn( targetDate.format( DateTimeHelper.ISO_DATE_ONLY ), "09:00" );
			assertThat( task.isConstrained() ).isFalse();

			// End 1 day ago
			var mockNow = task.getNow().plusDays( -1 );
			task.endOn( mockNow.format( DateTimeHelper.ISO_DATE_ONLY ) );
			assertThat( task.isConstrained() ).isTrue();
		}
	}
}
