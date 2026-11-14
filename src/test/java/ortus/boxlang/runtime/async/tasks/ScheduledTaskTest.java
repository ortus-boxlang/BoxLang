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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.management.InvalidAttributeValueException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.async.executors.BoxScheduledExecutor;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.util.DateTimeHelper;

class ScheduledTaskTest {

	ScheduledTask		task;

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupBeforeEach() {

		BoxExecutor executor = new BoxExecutor(
		    new BoxScheduledExecutor( 20 ),
		    "test",
		    AsyncService.ExecutorType.SCHEDULED,
		    20 );

		task		= new ScheduledTask( "test", executor ).setTimezone( "America/Chicago" );
		task		= Mockito.spy( task );

		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
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
			        } ) );
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

		@DisplayName( "can align every() + startOnTime() to the next period boundary instead of firing immediately" )
		@Test
		void testEveryWithStartOnTimeAlignsToBoundary() throws InvalidAttributeValueException {
			var t = task.every( 1800, TimeUnit.SECONDS );
			t.startOnTime( "00:00" );
			t.start();

			assertThat( t.getInitialDelay() ).isGreaterThan( 0L );
			assertThat( t.getInitialDelay() ).isAtMost( 1800L );
		}

		@DisplayName( "every() without startOnTime() still fires immediately (unchanged behavior)" )
		@Test
		void testEveryWithoutStartOnTimeStillFiresImmediately() {
			var t = task.every( 1800, TimeUnit.SECONDS );
			t.start();

			assertThat( t.getInitialDelay() ).isEqualTo( 0L );
		}

		@DisplayName( "explicit delay() takes precedence over startOnTime() alignment" )
		@Test
		void testExplicitDelayOverridesStartTimeAlignment() throws InvalidAttributeValueException {
			var t = task.every( 1800, TimeUnit.SECONDS );
			t.startOnTime( "00:00" );
			t.delay( 5, TimeUnit.SECONDS, true );
			t.start();

			assertThat( t.getInitialDelay() ).isEqualTo( 5L );
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
				        t.invoke( BoxRuntime.getInstance().getRuntimeContext(), "on" + dayOfTheWeek );

				        assertThat( task.getPeriod() ).isEqualTo( 604800 );
				        assertThat( task.getTimeUnit().toString().toLowerCase() ).isEqualTo( "seconds" );
			        } ) );
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

		@DisplayName( "clamps a day of the month constraint that doesn't exist in the running month to the last day" )
		@Test
		void testDayOfTheMonthConstraintClampsToLastDayOfShortMonth() {
			// April only has 30 days, so a constraint of the 31st must clamp to the 30th
			task.setDayOfTheMonth( 31 );

			LocalDateTime midMonth = LocalDateTime.of( 2026, 4, 15, 10, 0 );
			Mockito.when( task.getNow() ).thenReturn( midMonth );
			assertThat( task.isConstrained() ).isTrue();

			LocalDateTime lastDay = LocalDateTime.of( 2026, 4, 30, 10, 0 );
			Mockito.when( task.getNow() ).thenReturn( lastDay );
			assertThat( task.isConstrained() ).isFalse();

			// Every other day of the month must still be constrained, not just skipped entirely
			for ( int day = 1; day < 30; day++ ) {
				LocalDateTime otherDay = LocalDateTime.of( 2026, 4, day, 10, 0 );
				Mockito.when( task.getNow() ).thenReturn( otherDay );
				assertThat( task.isConstrained() ).isTrue();
			}
		}

		@DisplayName( "can have a last business day of the month constraint" )
		@Test
		@Disabled( "This fails every time CI runs on the last actual day of the actual month." )
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

	@Nested
	class Clustered {

		@DisplayName( "a clustered task refuses to start when clustering is disabled on the node" )
		@Test
		void testClusteredTaskRefusesWhenClusteringDisabled() {
			// The cluster service exists but is disabled, which is the shipped default: a task that opted in
			// must not silently run on every node instead
			BoxRuntime	runtime		= BoxRuntime.getInstance();
			boolean		wasEnabled	= runtime.getConfiguration().cluster.enabled;
			runtime.getConfiguration().cluster.enabled = false;
			try {
				BaseScheduler scheduler = new BaseScheduler( "no-cluster-" + UUID.randomUUID() );
				scheduler.task( "clustered-" + UUID.randomUUID() ).call( () -> {
				} ).every( 30, java.util.concurrent.TimeUnit.SECONDS ).setCluster( true );

				String				taskName	= scheduler.getTaskRecords().iterator().next().name;
				BoxRuntimeException	e			= assertThrows( BoxRuntimeException.class, () -> scheduler.startupTask( taskName ) );
				assertThat( e.getMessage() ).contains( "clustering is not enabled" );
				assertThat( e.getMessage() ).contains( "cluster.cacheName" );
			} finally {
				runtime.getConfiguration().cluster.enabled = wasEnabled;
			}
		}

		@DisplayName( "onOneServer() opts the task into cluster coordination" )
		@Test
		void testOnOneServerOptsIn() {
			assertThat( task.getCluster() ).isNull();
			task.onOneServer();
			assertThat( task.getCluster() ).isTrue();
			assertThat( task.getServerFixation() ).isTrue();
		}

		@DisplayName( "participation is a tri-state with an explicit override winning" )
		@Test
		void testParticipationTriState() {
			// No override and no cluster service: not clustered
			assertThat( TaskClusterClaim.isClustered( task, null ) ).isFalse();

			// Explicit opt-in clusters even without a cluster service configured
			task.setCluster( true );
			assertThat( TaskClusterClaim.isClustered( task, null ) ).isTrue();

			// Explicit opt-out wins over everything
			task.setCluster( false );
			assertThat( TaskClusterClaim.isClustered( task, null ) ).isFalse();
		}

		@DisplayName( "the overlap policy defaults to inheritance and can be set" )
		@Test
		void testOverlap() {
			assertThat( task.getOverlap() ).isNull();
			task.setOverlap( "run" );
			assertThat( task.getOverlap() ).isEqualTo( "run" );
		}

		@DisplayName( "an interval occurrence id is the period bucket of the due instant, not the wall clock" )
		@Test
		void testOccurrenceIdBucketsByDueInstant() {
			task.every( 10, TimeUnit.SECONDS );

			Instant	first	= Instant.parse( "2026-03-01T10:00:05Z" );
			Instant	late	= Instant.parse( "2026-03-01T10:00:05.900Z" );

			String	firstId	= TaskClusterClaim.occurrenceId( task, first );
			String	lateId	= TaskClusterClaim.occurrenceId( task, late );

			// A tick that runs 900ms late still belongs to the same occurrence
			assertThat( lateId ).isEqualTo( firstId );

			// The next period is a different occurrence
			assertThat( TaskClusterClaim.occurrenceId( task, first.plusSeconds( 10 ) ) ).isNotEqualTo( firstId );
		}

		@DisplayName( "occurrence ids are stable across nodes for the same due instant" )
		@Test
		void testOccurrenceIdIsDeterministic() {
			task.every( 30, TimeUnit.SECONDS );
			Instant due = Instant.parse( "2026-03-01T10:00:00Z" );

			assertThat( TaskClusterClaim.occurrenceId( task, due ) )
			    .isEqualTo( TaskClusterClaim.occurrenceId( task, due ) );
		}

		@DisplayName( "a calendar task's occurrence id identifies the fire time" )
		@Test
		void testOccurrenceIdForCalendarTask() throws InvalidAttributeValueException {
			task.everyDayAt( "02:00" );

			String	id1	= TaskClusterClaim.occurrenceId( task, Instant.parse( "2026-03-01T08:00:00Z" ) );
			String	id2	= TaskClusterClaim.occurrenceId( task, Instant.parse( "2026-03-02T08:00:00Z" ) );

			assertThat( id1 ).isNotEqualTo( id2 );
		}

		@DisplayName( "the eager delay stays within the configured random window" )
		@Test
		void testEagerDelayBounds() {
			ortus.boxlang.runtime.services.ClusterService		cluster	= claimTestCluster();
			ortus.boxlang.runtime.config.segments.ClusterConfig	config	= cluster.getConfig();

			for ( int i = 0; i < 25; i++ ) {
				long delay = TaskClusterClaim.computeDelayMillis( task, cluster, config );
				assertThat( delay ).isAtLeast( 0L );
				assertThat( delay ).isAtMost( TaskClusterClaim.RANDOM_DELAY_MILLIS );
			}
		}

		@DisplayName( "balanced mode falls back to the eager random delay when no peers are visible" )
		void testBalancedDelayWithNoPeers() {
			ortus.boxlang.runtime.services.ClusterService		cluster	= claimTestCluster();
			ortus.boxlang.runtime.config.segments.ClusterConfig	config	= cluster.getConfig();

			// This node is alone in its own cluster name, so there is nobody to be pushed back behind
			cluster.invalidateSnapshot();
			for ( int i = 0; i < 10; i++ ) {
				assertThat( TaskClusterClaim.computeDelayMillis( task, cluster, config ) ).isLessThan( 30L );
			}
		}

		@DisplayName( "a cluster floor rejects a task period it cannot service" )
		@Test
		void testMinimumPeriodFloor() {
			ortus.boxlang.runtime.config.segments.ClusterConfig config = new ortus.boxlang.runtime.config.segments.ClusterConfig();

			// 2 x 1000 + 1000 + 1000 margin = 4s eager
			assertThat( TaskClusterClaim.minimumPeriodSeconds() ).isEqualTo( 4 );

			task.every( 2, TimeUnit.SECONDS );
			assertThat( TaskClusterClaim.meetsMinimumPeriod( task, config ) ).isFalse();

			task.every( 10, TimeUnit.SECONDS );
			assertThat( TaskClusterClaim.meetsMinimumPeriod( task, config ) ).isTrue();
		}

		/**
		 * A cluster service for delay tests: enabled, its own cluster name so no peers leak in from
		 * other tests, over the default cache.
		 */
		private ortus.boxlang.runtime.services.ClusterService claimTestCluster() {
			ortus.boxlang.runtime.config.segments.ClusterConfig config = new ortus.boxlang.runtime.config.segments.ClusterConfig();
			config.enabled	= true;
			config.name		= "claim-test-" + java.util.UUID.randomUUID();
			config.nodeId	= "claim-node";
			return new ortus.boxlang.runtime.services.ClusterService(
			    ortus.boxlang.runtime.BoxRuntime.getInstance(),
			    config,
			    ortus.boxlang.runtime.BoxRuntime.getInstance().getCacheService().getDefaultCache()
			);
		}
	}

	@Test
	public void testCanExecuteBoxLangFunction() {
		instance.executeSource(
		    """
		    	result = "";
		    	arg = () => { result = "what" };
		    	task = ExecutorGet( "scheduled-tasks" ).newTask( "printTest" )
		    	task.call( arg )
		    	task.start();
		    	sleep(1000)
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "what" );

	}

	@DisplayName( "call( DynamicObject, String ) registers without infinite recursion and executes the named method" )
	@Test
	public void testCallDynamicObjectWithMethodName() {
		DynamicObject	dyno		= DynamicObject.of( new CallableTestTarget() );

		ScheduledTask	registered	= task.call( dyno, "run" );
		assertThat( registered ).isSameInstanceAs( task );

		task.run( true );

		@SuppressWarnings( "unchecked" )
		java.util.Optional<Object> lastResult = ( java.util.Optional<Object> ) task.getStats().get( "lastResult" );
		assertThat( lastResult.get() ).isEqualTo( "ran" );
	}

	/**
	 * A named, public class so JVM reflection can resolve its public methods —
	 * anonymous test-local classes are not public and fail reflective lookup.
	 */
	public static class CallableTestTarget {

		public String run() {
			return "ran";
		}
	}

	@DisplayName( "a cron tick that runs late still fires for the cron time it passed" )
	@Test
	void testCronLateTickStillFires() throws InterruptedException {
		// Fires on every even second
		task.cron( "*/2 * * * * *" );
		var predicate = task.getWhenPredicate();
		predicate.test( task );
		// Simulate a tick queued behind other tasks on a saturated executor
		Thread.sleep( 2500 );
		assertThat( predicate.test( task ) ).isTrue();
	}

	@DisplayName( "a cron tick does not fire when no cron time is due" )
	@Test
	void testCronNotDueDoesNotFire() {
		// Only fires on the 1st of January at midnight
		task.cron( "0 0 0 1 1 *" );
		assertThat( task.getWhenPredicate().test( task ) ).isFalse();
	}

	@DisplayName( "constraints can be judged against the time a tick was due instead of now" )
	@Test
	void testConstraintsCanBeJudgedAgainstAGivenTime() {
		// Friday 2026-10-30 23:59:50 is a weekday, Saturday 2026-10-31 00:00:05 is not
		task.setWeekdays( true );
		assertThat( task.isConstrained( LocalDateTime.of( 2026, 10, 30, 23, 59, 50 ) ) ).isFalse();
		assertThat( task.isConstrained( LocalDateTime.of( 2026, 10, 31, 0, 0, 5 ) ) ).isTrue();
	}

	@DisplayName( "a day of the month constraint is judged against the given time, not the real month" )
	@Test
	void testDayOfTheMonthConstraintAgainstAGivenTime() {
		task.setDayOfTheMonth( 31 );
		assertThat( task.isConstrained( LocalDateTime.of( 2026, 10, 31, 23, 59, 59 ) ) ).isFalse();
		assertThat( task.isConstrained( LocalDateTime.of( 2026, 11, 1, 0, 0, 1 ) ) ).isTrue();
	}

	@DisplayName( "a last business day constraint is judged against the given time, not the real month" )
	@Test
	void testLastBusinessDayConstraintAgainstAGivenTime() {
		// Oct 31 2026 is a Saturday, so the last business day of October is Friday the 30th
		task.setLastBusinessDay( true );
		assertThat( task.isConstrained( LocalDateTime.of( 2026, 10, 30, 23, 59, 50 ) ) ).isFalse();
		assertThat( task.isConstrained( LocalDateTime.of( 2026, 11, 1, 0, 0, 5 ) ) ).isTrue();
	}

	@DisplayName( "a late tick that was due on an allowed day still runs after midnight" )
	@Test
	void testLateTickDueOnAnAllowedDayStillRuns() {
		AtomicInteger runs = new AtomicInteger( 0 );
		task.call( ( Callable<Object> ) () -> {
			runs.incrementAndGet();
			return null;
		} );
		task.setWeekends( true );

		// The tick was due on Saturday 2026-10-31, even though it is running much later
		task.setNextDueInstant( LocalDateTime.of( 2026, 10, 31, 0, 0, 5 ).atZone( task.getTimezone() ).toInstant() );
		task.run();

		assertThat( runs.get() ).isEqualTo( 1 );
	}

	@DisplayName( "a tick that was due on a constrained day does not run, even if it runs on an allowed day" )
	@Test
	void testLateTickDueOnAConstrainedDayDoesNotRun() {
		AtomicInteger runs = new AtomicInteger( 0 );
		task.call( ( Callable<Object> ) () -> {
			runs.incrementAndGet();
			return null;
		} );
		task.setWeekends( true );

		// The tick was due on Wednesday 2026-10-28
		task.setNextDueInstant( LocalDateTime.of( 2026, 10, 28, 12, 0, 0 ).atZone( task.getTimezone() ).toInstant() );
		task.run();

		assertThat( runs.get() ).isEqualTo( 0 );
	}

	@DisplayName( "each periodic tick advances the due time by one period" )
	@Test
	void testEachTickAdvancesTheDueTime() {
		Instant first = Instant.parse( "2026-10-30T22:00:00Z" );
		task.every( 1, TimeUnit.HOURS );
		task.setNextDueInstant( first );

		assertThat( task.consumeDueInstant() ).isEqualTo( first );
		assertThat( task.consumeDueInstant() ).isEqualTo( first.plusSeconds( 3600 ) );
		assertThat( task.consumeDueInstant() ).isEqualTo( first.plusSeconds( 7200 ) );
	}

	@DisplayName( "the constraint time is the time the constraints are being judged against" )
	@Test
	void testConstraintTimeIsTheEvaluatedTime() {
		AtomicReference<LocalDateTime> seen = new AtomicReference<>();
		task.when( t -> {
			seen.set( t.getConstraintTime() );
			return true;
		} );

		LocalDateTime dueTime = LocalDateTime.of( 2026, 10, 30, 23, 59, 50 );
		task.isConstrained( dueTime );

		assertThat( seen.get() ).isEqualTo( dueTime );
	}

	@DisplayName( "a disabled task does not replay its cron fires when it is enabled again" )
	@Test
	void testDisabledCronTaskDoesNotReplayFires() {
		// Fires at the start of every minute
		task.cron( "0 * * * * *" );
		var predicate = task.getWhenPredicate();

		// A checkpoint over a minute old has a fire due in its window
		task.setCronCheckpoint( Instant.now().minusSeconds( 61 ) );
		assertThat( predicate.test( task ) ).isTrue();

		// Disabled ticks move the checkpoint, so the pause is not replayed
		task.setCronCheckpoint( Instant.now().minusSeconds( 61 ) );
		task.disable();
		task.run();
		task.enable();
		assertThat( predicate.test( task ) ).isFalse();
	}
}
