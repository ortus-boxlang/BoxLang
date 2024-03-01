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
import static org.junit.Assert.assertThrows;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.IStruct;

class SchedulerTest {

	BaseScheduler		scheduler;

	@Spy
	@InjectMocks
	private BoxRuntime	runtime;

	@BeforeEach
	public void setupBeforeEach() {
		scheduler	= new BaseScheduler( "bdd" );
		scheduler	= Mockito.spy( scheduler );
	}

	@DisplayName( "It can create the scheduler" )
	@Test
	void testItCanCreateIt() {
		assertThat( scheduler ).isNotNull();
		assertThat( scheduler.getName() ).isEqualTo( "bdd" );
		assertThat( scheduler.getAsyncService() ).isNotNull();
		assertThat( scheduler.getExecutor() ).isNull();
		scheduler.startup();
		assertThat( scheduler.getExecutor() ).isNotNull();
		assertThat( scheduler.getExecutor().name() ).isEqualTo( "bdd-scheduler" );
	}

	@DisplayName( "It can set a new timezone" )
	@Test
	void testItCanSetANewTimezone() {
		scheduler.setTimezone( "America/Los_Angeles" );
		assertThat( scheduler.getTimezone().getId() ).isNotEqualTo( "America/New_York" );
		scheduler.setTimezone( "America/New_York" );
		assertThat( scheduler.getTimezone().getId() ).isEqualTo( "America/New_York" );
	}

	@DisplayName( "It can register a new task with no group and get it's record" )
	@Test
	void testItCanRegisterANewTaskAndGetItsRecord() {
		var task = scheduler.task( "bddtest" );
		assertThat( task ).isNotNull();
		assertThat( scheduler.hasTask( "bddtest" ) ).isTrue();

		var taskRecord = scheduler.getTaskRecord( "bddtest" );
		assertThat( taskRecord ).isNotNull();
		assertThat( taskRecord.disabled ).isFalse();
		assertThat( taskRecord.name ).isEqualTo( "bddtest" );
		assertThat( taskRecord.group ).isEqualTo( "" );
		assertThat( taskRecord.task ).isEqualTo( task );
		assertThat( taskRecord.future ).isNull();
		assertThat( taskRecord.registeredAt ).isNotNull();
		assertThat( taskRecord.scheduledAt ).isNull();
		assertThat( taskRecord.error ).isFalse();
		assertThat( taskRecord.errorMessage ).isEmpty();
		assertThat( taskRecord.stacktrace ).isEmpty();
	}

	@DisplayName( "It can register a new task with a group and get it's record" )
	@Test
	void testItCanRegisterANewTaskWithGroupAndGetItsRecord() {
		var task = scheduler.task( "bddtest", "admin" );
		assertThat( task ).isNotNull();
		assertThat( scheduler.hasTask( "bddtest" ) ).isTrue();

		var taskRecord = scheduler.getTaskRecord( "bddtest" );
		assertThat( taskRecord ).isNotNull();
		assertThat( taskRecord.disabled ).isFalse();
		assertThat( taskRecord.name ).isEqualTo( "bddtest" );
		assertThat( taskRecord.group ).isEqualTo( "admin" );
		assertThat( taskRecord.task ).isEqualTo( task );
		assertThat( taskRecord.future ).isNull();
		assertThat( taskRecord.registeredAt ).isNotNull();
		assertThat( taskRecord.scheduledAt ).isNull();
		assertThat( taskRecord.error ).isFalse();
		assertThat( taskRecord.errorMessage ).isEmpty();
		assertThat( taskRecord.stacktrace ).isEmpty();
	}

	@DisplayName( "It can throw an exception on getting a bogus task " )
	@Test
	void testItCanThrowAnExceptionOnGettingABogusTask() {
		assertThat( scheduler.hasTask( "bogus" ) ).isFalse();
		assertThrows( RuntimeException.class, () -> scheduler.getTaskRecord( "bogus" ) );
	}

	@DisplayName( "can remove a task" )
	@Test
	void testCanRemoveATask() {
		var task = scheduler.task( "bddtest" );
		assertThat( task ).isNotNull();
		assertThat( scheduler.hasTask( "bddtest" ) ).isTrue();

		scheduler.removeTask( "bddtest" );
		assertThat( scheduler.hasTask( "bddtest" ) ).isFalse();
	}

	@DisplayName( "can throw can exception on removing a bogus task" )
	@Test
	void testCanThrowAnExceptionOnRemovingABogusTask() {
		assertThat( scheduler.hasTask( "bogus" ) ).isFalse();
		assertThrows( RuntimeException.class, () -> scheduler.removeTask( "bogus" ) );
	}

	@DisplayName( "It can register and run a task with life-cycle methods" )
	@Test
	void testItCanRegisterAndRunATaskWithLifeCycleMethods() throws InterruptedException {
		AtomicLong counter = new AtomicLong( 0 );

		scheduler
		    .task( "test1" )
		    .call( () -> {
			    var count = counter.incrementAndGet();
			    System.out.println( "test1: " + count );
			    return count;
		    } )
		    .before( ( task ) -> {
			    System.out.println( "before test1" );
			    assertThat( task ).isNotNull();
			    assertThat( task.getName() ).isEqualTo( "test1" );
		    } )
		    .after( ( task, results ) -> {
			    System.out.println( "after test1" );
			    assertThat( task ).isNotNull();
			    assertThat( task.getName() ).isEqualTo( "test1" );
			    assertThat( results.isPresent() ).isTrue();
			    assertThat( results.get() ).isEqualTo( 1L );
		    } )
		    .onSuccess( ( task, results ) -> {
			    System.out.println( "onSuccess test1" );
			    assertThat( task ).isNotNull();
			    assertThat( task.getName() ).isEqualTo( "test1" );
			    assertThat( results.isPresent() ).isTrue();
			    assertThat( results.get() ).isEqualTo( 1L );
		    } );

		scheduler
		    .task( "test3" )
		    .call( () -> {
			    var results = counter.incrementAndGet();
			    System.out.println( "Running test3 (#results#) from:#getThreadName()#" );
			    return results;
		    } )
		    .disable();

		// Startup the scheduler
		try {
			assertThat( scheduler.hasStarted() ).isFalse();
			scheduler.startup();
			assertThat( scheduler.hasStarted() ).isTrue();

			var record = scheduler.getTaskRecord( "test1" );
			assertThat( record.future ).isNotNull();
			assertThat( record.scheduledAt ).isInstanceOf( LocalDateTime.class );

			record = scheduler.getTaskRecord( "test3" );
			assertThat( record.disabled ).isTrue();
			assertThat( record.future ).isNull();
			assertThat( record.scheduledAt ).isNull();

			// Wait for them to execute
			Thread.sleep( 1000 );
			IStruct stats = scheduler.getTaskStats();

			assertThat( ( Boolean ) ( ( IStruct ) stats.get( "test1" ) ).get( "neverRun" ) ).isFalse();
			assertThat( ( Boolean ) ( ( IStruct ) stats.get( "test3" ) ).get( "neverRun" ) ).isTrue();

			assertThat(
			    ( ( AtomicInteger ) ( ( IStruct ) stats.get( "test1" ) ).get( "totalRuns" ) ).get()
			).isEqualTo( 1 );
			assertThat(
			    ( ( AtomicInteger ) ( ( IStruct ) stats.get( "test3" ) ).get( "totalRuns" ) ).get()
			).isEqualTo( 0 );
		} finally {
			// Sleep for 1000 ms to allow the scheduler to run
			Thread.sleep( 1000 );
			scheduler.shutdown();
			assertThat( scheduler.hasStarted() ).isFalse();
		}
	}

	// @Test
	// void testFullSchedulerLifecycle() {
	// IScheduler scheduler = new modules.test.config.Scheduler();
	// scheduler.configure();
	// assertThat( scheduler.hasStarted() ).isFalse();

	// // Startup the scheduler and wait a bit
	// scheduler.startup();
	// assertThat( scheduler.hasStarted() ).isTrue();

	// try {
	// System.out.println( "Sleeping for 3 seconds....." );
	// Thread.sleep( 3000 );
	// } catch ( InterruptedException e ) {
	// e.printStackTrace();
	// }

	// // Shutdown the scheduler
	// scheduler.shutdown( true );
	// assertThat( scheduler.hasStarted() ).isFalse();
	// }

}
