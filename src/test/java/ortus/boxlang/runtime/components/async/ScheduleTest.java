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
package ortus.boxlang.runtime.components.async;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.BaseScheduler;
import ortus.boxlang.runtime.async.tasks.TaskRecord;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ScheduleTest {

	static BoxRuntime		instance;
	static SchedulerService	svc;
	IBoxContext				context;
	IScope					variables;
	static Key				result				= new Key( "result" );

	static final Key		SCHEDULER_KEY		= Key.of( Schedule.DEFAULT_SCHEDULER_NAME );
	static final Key		MY_SCHEDULER_KEY	= Key.of( "myscheduler" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		svc			= instance.getSchedulerService();
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterEach
	public void teardownEach() {
		// Remove schedulers created during tests
		if ( svc.hasScheduler( SCHEDULER_KEY ) ) {
			svc.removeScheduler( SCHEDULER_KEY, true, 5 );
		}
		if ( svc.hasScheduler( MY_SCHEDULER_KEY ) ) {
			svc.removeScheduler( MY_SCHEDULER_KEY, true, 5 );
		}
		// Clean up tasks.json
		Path tasksFile = instance.getRuntimeHome().resolve( "config/tasks.json" );
		try {
			Files.deleteIfExists( tasksFile );
		} catch ( Exception e ) {
			// ignore
		}
	}

	// --------------------------------------------------------------------------
	// Update action tests
	// --------------------------------------------------------------------------

	@DisplayName( "It can create a task with a numeric interval" )
	@Test
	public void testCreateWithNumericInterval() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( svc.hasScheduler( SCHEDULER_KEY ) ).isTrue();
		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "myTask" ) ).isTrue();
	}

	@DisplayName( "It can create a task with a cron expression" )
	@Test
	public void testCreateWithCronTime() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="cronTask" url="http://localhost/test" cronTime="0 0 * * *">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( svc.hasScheduler( SCHEDULER_KEY ) ).isTrue();
		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "cronTask" ) ).isTrue();
		TaskRecord record = scheduler.getTaskRecord( "cronTask" );
		assertThat( record.task.getMeta().get( Key.cronExpression ) ).isNotNull();
	}

	@DisplayName( "It can create a task with interval=daily" )
	@Test
	public void testCreateWithDaily() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="dailyTask" url="http://localhost/test" interval="daily">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "dailyTask" ) ).isTrue();
	}

	@DisplayName( "It can create a task with isDaily=true" )
	@Test
	public void testCreateWithIsDaily() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="isDailyTask" url="http://localhost/test" isDaily="true">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "isDailyTask" ) ).isTrue();
	}

	@DisplayName( "It can create a task with interval=once" )
	@Test
	public void testCreateWithOnce() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="onceTask" url="http://localhost/test" interval="once">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "onceTask" ) ).isTrue();
	}

	@DisplayName( "update replaces an existing task (idempotent)" )
	@Test
	public void testUpdateIsIdempotent() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" url="http://localhost/test1" interval="120">
		    <bx:schedule action="update" task="myTask" url="http://localhost/test2" interval="240">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "myTask" ) ).isTrue();
		// Should only have one entry with this name
		long count = scheduler.getTasks().keySet().stream()
		    .filter( k -> k.equals( "myTask" ) )
		    .count();
		assertThat( count ).isEqualTo( 1L );
	}

	@DisplayName( "update throws when url is missing" )
	@Test
	public void testCreateMissingUrl() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	@DisplayName( "update throws when interval and cronTime are missing" )
	@Test
	public void testCreateMissingInterval() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" url="http://localhost/test">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	@DisplayName( "update throws when interval is less than 60 seconds" )
	@Test
	public void testIntervalTooShort() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" url="http://localhost/test" interval="30">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	@DisplayName( "update throws when task name is missing" )
	@Test
	public void testMissingTaskName() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="update" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	// --------------------------------------------------------------------------
	// Action aliases
	// --------------------------------------------------------------------------

	@DisplayName( "create action is an alias for update" )
	@Test
	public void testCreateIsAliasForUpdate() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="create" task="createTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "createTask" ) ).isTrue();
	}

	@DisplayName( "modify action is an alias for update" )
	@Test
	public void testModifyIsAliasForUpdate() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="modify" task="modifyTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "modifyTask" ) ).isTrue();
	}

	// --------------------------------------------------------------------------
	// Delete action tests
	// --------------------------------------------------------------------------

	@DisplayName( "It can delete a task" )
	@Test
	public void testDelete() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="deleteMe" url="http://localhost/test" interval="120">
		    <bx:schedule action="delete" task="deleteMe">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "deleteMe" ) ).isFalse();
	}

	@DisplayName( "delete throws for non-existent task" )
	@Test
	public void testDeleteNonExistent() {
		// Need a scheduler to exist first
		Schedule.getOrCreateScheduler( context, Schedule.DEFAULT_SCHEDULER_NAME );

		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="delete" task="nonExistentTask">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	// --------------------------------------------------------------------------
	// Pause/Resume tests
	// --------------------------------------------------------------------------

	@DisplayName( "It can pause a task" )
	@Test
	public void testPause() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="pauseMe" url="http://localhost/test" interval="120">
		    <bx:schedule action="pause" task="pauseMe">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler	scheduler	= ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		TaskRecord		record		= scheduler.getTaskRecord( "pauseMe" );
		assertThat( record.disabled ).isTrue();
	}

	@DisplayName( "It can resume a paused task" )
	@Test
	public void testResume() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="resumeMe" url="http://localhost/test" interval="120">
		    <bx:schedule action="pause" task="resumeMe">
		    <bx:schedule action="resume" task="resumeMe">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler	scheduler	= ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		TaskRecord		record		= scheduler.getTaskRecord( "resumeMe" );
		assertThat( record.disabled ).isFalse();
	}

	// --------------------------------------------------------------------------
	// List action tests
	// --------------------------------------------------------------------------

	@DisplayName( "It can list tasks into a result variable" )
	@Test
	public void testListWithResultVariable() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="listMe1" url="http://localhost/test" interval="120">
		    <bx:schedule action="update" task="listMe2" url="http://localhost/test" interval="120">
		    <bx:schedule action="list" result="myTasks">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Object taskList = variables.get( Key.of( "myTasks" ) );
		assertThat( taskList ).isInstanceOf( Array.class );
		Array arr = ( Array ) taskList;
		assertThat( arr.size() ).isAtLeast( 2 );
	}

	@DisplayName( "list returns an empty array when no tasks exist" )
	@Test
	public void testListEmptyResult() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="list" result="emptyList">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Object taskList = variables.get( Key.of( "emptyList" ) );
		assertThat( taskList ).isInstanceOf( Array.class );
	}

	// --------------------------------------------------------------------------
	// PauseAll / ResumeAll tests
	// --------------------------------------------------------------------------

	@DisplayName( "pauseall disables all tasks in the scheduler" )
	@Test
	public void testPauseAll() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="task1" url="http://localhost/test" interval="120">
		    <bx:schedule action="update" task="task2" url="http://localhost/test" interval="120">
		    <bx:schedule action="pauseall">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		for ( TaskRecord record : scheduler.getTasks().values() ) {
			assertThat( record.disabled ).isTrue();
		}
	}

	@DisplayName( "resumeall enables all tasks in the scheduler" )
	@Test
	public void testResumeAll() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="task1" url="http://localhost/test" interval="120">
		    <bx:schedule action="update" task="task2" url="http://localhost/test" interval="120">
		    <bx:schedule action="pauseall">
		    <bx:schedule action="resumeall">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		for ( TaskRecord record : scheduler.getTasks().values() ) {
			assertThat( record.disabled ).isFalse();
		}
	}

	@DisplayName( "pauseall with group only pauses tasks in that group" )
	@Test
	public void testPauseAllGroup() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="groupTask" url="http://localhost/test" interval="120" group="myGroup">
		    <bx:schedule action="update" task="otherTask" url="http://localhost/test" interval="120" group="otherGroup">
		    <bx:schedule action="pauseall" group="myGroup">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler	scheduler	= ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		TaskRecord		groupRecord	= scheduler.getTaskRecord( "groupTask" );
		TaskRecord		otherRecord	= scheduler.getTaskRecord( "otherTask" );
		assertThat( groupRecord.disabled ).isTrue();
		assertThat( otherRecord.disabled ).isFalse();
	}

	@DisplayName( "resumeall with group only resumes tasks in that group" )
	@Test
	public void testResumeAllGroup() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="groupTask" url="http://localhost/test" interval="120" group="myGroup">
		    <bx:schedule action="update" task="otherTask" url="http://localhost/test" interval="120" group="otherGroup">
		    <bx:schedule action="pauseall">
		    <bx:schedule action="resumeall" group="myGroup">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler	scheduler	= ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		TaskRecord		groupRecord	= scheduler.getTaskRecord( "groupTask" );
		TaskRecord		otherRecord	= scheduler.getTaskRecord( "otherTask" );
		assertThat( groupRecord.disabled ).isFalse();
		assertThat( otherRecord.disabled ).isTrue();
	}

	// --------------------------------------------------------------------------
	// Named scheduler tests
	// --------------------------------------------------------------------------

	@DisplayName( "It uses a named scheduler when specified" )
	@Test
	public void testNamedScheduler() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="namedTask" url="http://localhost/test" interval="120" scheduler="myscheduler">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( svc.hasScheduler( MY_SCHEDULER_KEY ) ).isTrue();
		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( MY_SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "namedTask" ) ).isTrue();
	}

	// --------------------------------------------------------------------------
	// Syntax tests
	// --------------------------------------------------------------------------

	@DisplayName( "It works with BX template syntax" )
	@Test
	public void testBXTemplateSyntax() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="bxTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "bxTask" ) ).isTrue();
	}

	@DisplayName( "It works with CF template syntax" )
	@Test
	public void testCFTemplateSyntax() {
		// @formatter:off
		instance.executeSource(
		    """
		    <cfschedule action="update" task="cfTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.CFTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "cfTask" ) ).isTrue();
	}

	// --------------------------------------------------------------------------
	// Persistence tests
	// --------------------------------------------------------------------------

	@DisplayName( "update writes task to tasks.json" )
	@Test
	public void testUpdateWritesToDisk() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="diskTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Path	tasksFile	= instance.getRuntimeHome().resolve( "config/tasks.json" );
		assertThat( Files.exists( tasksFile ) ).isTrue();

		Array tasks = instance.getSchedulerService().loadTasksFromDisk();
		assertThat( tasks.size() ).isAtLeast( 1 );
		boolean found = tasks.stream().anyMatch( entry -> {
			if ( entry instanceof IStruct ) {
				return "diskTask".equals( ( ( IStruct ) entry ).getAsString( Key.task ) );
			}
			return false;
		} );
		assertThat( found ).isTrue();
	}

	@DisplayName( "delete removes task from tasks.json" )
	@Test
	public void testDeleteRemovesFromDisk() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="removeFromDisk" url="http://localhost/test" interval="120">
		    <bx:schedule action="delete" task="removeFromDisk">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Array tasks = instance.getSchedulerService().loadTasksFromDisk();
		boolean found = tasks.stream().anyMatch( entry -> {
			if ( entry instanceof IStruct ) {
				return "removeFromDisk".equals( ( ( IStruct ) entry ).getAsString( Key.task ) );
			}
			return false;
		} );
		assertThat( found ).isFalse();
	}

	@DisplayName( "pause sets paused=true in tasks.json" )
	@Test
	public void testPauseSetsFlag() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="pauseFlag" url="http://localhost/test" interval="120">
		    <bx:schedule action="pause" task="pauseFlag">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Array tasks = instance.getSchedulerService().loadTasksFromDisk();
		Object paused = tasks.stream()
		    .filter( e -> e instanceof IStruct && "pauseFlag".equals( ( ( IStruct ) e ).getAsString( Key.task ) ) )
		    .map( e -> ( ( IStruct ) e ).get( Key.paused ) )
		    .findFirst().orElse( null );
		assertThat( paused ).isNotNull();
		assertThat( paused.toString() ).isEqualTo( "true" );
	}

	@DisplayName( "resume sets paused=false in tasks.json" )
	@Test
	public void testResumeClearsFlag() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="resumeFlag" url="http://localhost/test" interval="120">
		    <bx:schedule action="pause" task="resumeFlag">
		    <bx:schedule action="resume" task="resumeFlag">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		Array tasks = instance.getSchedulerService().loadTasksFromDisk();
		Object paused = tasks.stream()
		    .filter( e -> e instanceof IStruct && "resumeFlag".equals( ( ( IStruct ) e ).getAsString( Key.task ) ) )
		    .map( e -> ( ( IStruct ) e ).get( Key.paused ) )
		    .findFirst().orElse( null );
		assertThat( paused ).isNotNull();
		assertThat( paused.toString() ).isEqualTo( "false" );
	}
}
