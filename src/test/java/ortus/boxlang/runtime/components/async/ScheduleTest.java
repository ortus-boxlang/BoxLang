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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.BaseScheduler;
import ortus.boxlang.runtime.async.tasks.ScheduledTask;
import ortus.boxlang.runtime.async.tasks.TaskRecord;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.SchedulerService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.ConfigSecretUtil;
import ortus.boxlang.runtime.util.EncryptionUtil;

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

	/**
	 * Verifies that concurrent schedule requests share one default scheduler instead of racing during registration.
	 */
	@DisplayName( "concurrent schedule requests create the default scheduler once" )
	@Test
	public void testConcurrentDefaultSchedulerCreation() throws Exception {
		int					callerCount	= 8;
		CountDownLatch		startGate	= new CountDownLatch( 1 );
		ExecutorService		executor	= Executors.newFixedThreadPool( callerCount );
		Set<BaseScheduler>	schedulers	= ConcurrentHashMap.newKeySet();
		try {
			Future<?>[] callers = new Future<?>[ callerCount ];
			for ( int index = 0; index < callerCount; index++ ) {
				callers[ index ] = executor.submit( () -> {
					startGate.await();
					schedulers.add( Schedule.getOrCreateScheduler( context, Schedule.DEFAULT_SCHEDULER_NAME ) );
					return null;
				} );
			}
			startGate.countDown();
			for ( Future<?> caller : callers ) {
				caller.get();
			}
		} finally {
			executor.shutdownNow();
		}

		assertThat( schedulers ).hasSize( 1 );
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

	@DisplayName( "create action registers a new task" )
	@Test
	public void testCreateRegistersNewTask() {
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

	@DisplayName( "create action throws if task already exists" )
	@Test
	public void testCreateThrowsIfTaskAlreadyExists() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="create" task="dupTask" url="http://localhost/test" interval="120">
		    <bx:schedule action="create" task="dupTask" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
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

		Path tasksFile = instance.getRuntimeHome().resolve( "config/tasks.json" );
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

	/**
	 * Verifies that an omitted port is not persisted as HTTP port 80, allowing the URL scheme to select its default.
	 */
	@DisplayName( "schedule leaves the port unset when no port is provided" )
	@Test
	public void testOmittedPortIsNotPersisted() {
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="httpsTask" url="https://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);

		IStruct task = ( IStruct ) instance.getSchedulerService().loadTasksFromDisk().stream()
		    .filter( entry -> entry instanceof IStruct && "httpsTask".equals( ( ( IStruct ) entry ).getAsString( Key.task ) ) )
		    .findFirst().orElseThrow();
		assertThat( task.get( Key.port ) ).isNull();
	}

	/**
	 * Verifies that an explicitly configured port is retained in the persisted task definition.
	 */
	@DisplayName( "schedule preserves an explicitly configured port" )
	@Test
	public void testExplicitPortIsPersisted() {
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="customPortTask" url="https://localhost/test" port="9443" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);

		IStruct task = ( IStruct ) instance.getSchedulerService().loadTasksFromDisk().stream()
		    .filter( entry -> entry instanceof IStruct && "customPortTask".equals( ( ( IStruct ) entry ).getAsString( Key.task ) ) )
		    .findFirst().orElseThrow();
		assertThat( task.getAsInteger( Key.port ) ).isEqualTo( 9443 );
	}

	@DisplayName( "tasks.json writes prefixed credentials and decrypts all prefixed values" )
	@Test
	public void testPrefixedTaskValues() throws Exception {
		String	prefixedToken	= ConfigSecretUtil.encryptWithPrefix( "task-token" );
		Array	tasks			= Array.of( Struct.of(
		    "task", "prefixedTask",
		    "username", "task-user",
		    "password", "task-password",
		    "proxyUser", "proxy-user",
		    "proxyPassword", "proxy-password",
		    "metadata", Struct.of( "token", prefixedToken ),
		    "values", Array.of( prefixedToken )
		) );

		svc.saveTasksToDisk( tasks );

		Array	persisted		= ( Array ) JSONUtil.fromJSON( instance.getRuntimeHome().resolve( "config/tasks.json" ).toFile(), true );
		IStruct	persistedTask	= ( IStruct ) persisted.get( 0 );
		assertThat( ConfigSecretUtil.isEncrypted( persistedTask.getAsString( Key.username ) ) ).isTrue();
		assertThat( ConfigSecretUtil.isEncrypted( persistedTask.getAsString( Key.password ) ) ).isTrue();
		assertThat( ConfigSecretUtil.isEncrypted( persistedTask.getAsString( Key.proxyUser ) ) ).isTrue();
		assertThat( ConfigSecretUtil.isEncrypted( persistedTask.getAsString( Key.proxyPassword ) ) ).isTrue();

		IStruct loadedTask = ( IStruct ) svc.loadTasksFromDisk().get( 0 );
		assertThat( loadedTask.getAsString( Key.username ) ).isEqualTo( "task-user" );
		assertThat( loadedTask.getAsString( Key.password ) ).isEqualTo( "task-password" );
		assertThat( ( ( IStruct ) loadedTask.get( "metadata" ) ).getAsString( Key.of( "token" ) ) ).isEqualTo( "task-token" );
		assertThat( ( ( Array ) loadedTask.get( "values" ) ).get( 0 ) ).isEqualTo( "task-token" );
	}

	@DisplayName( "tasks.json supports legacy bare encrypted credentials" )
	@Test
	public void testLegacyTaskCredentials() throws Exception {
		Array	legacyTasks	= Array.of( Struct.of(
		    "task", "legacyTask",
		    "username", encryptLegacyTaskCredential( "legacy-user" ),
		    "password", encryptLegacyTaskCredential( "legacy-password" ),
		    "proxyUser", encryptLegacyTaskCredential( "legacy-proxy-user" ),
		    "proxyPassword", encryptLegacyTaskCredential( "legacy-proxy-password" )
		) );
		Path	tasksFile	= instance.getRuntimeHome().resolve( "config/tasks.json" );
		Files.writeString( tasksFile, JSONUtil.getJSONBuilder( true ).asString( legacyTasks ) );

		IStruct loadedTask = ( IStruct ) svc.loadTasksFromDisk().get( 0 );
		assertThat( loadedTask.getAsString( Key.username ) ).isEqualTo( "legacy-user" );
		assertThat( loadedTask.getAsString( Key.password ) ).isEqualTo( "legacy-password" );
		assertThat( loadedTask.getAsString( Key.proxyUser ) ).isEqualTo( "legacy-proxy-user" );
		assertThat( loadedTask.getAsString( Key.proxyPassword ) ).isEqualTo( "legacy-proxy-password" );
	}

	/**
	 * Creates an unprefixed, UU-encoded credential matching the historical tasks.json format.
	 *
	 * @param value The credential plaintext.
	 *
	 * @return The historical encrypted credential value.
	 */
	private String encryptLegacyTaskCredential( String value ) {
		return EncryptionUtil.encrypt( value, instance.getConfiguration().security.secretAlgorithm, ConfigSecretUtil.getRuntimeSeed(),
		    EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING, null, null );
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

		Array	tasks	= instance.getSchedulerService().loadTasksFromDisk();
		boolean	found	= tasks.stream().anyMatch( entry -> {
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

		Array	tasks	= instance.getSchedulerService().loadTasksFromDisk();
		Object	paused	= tasks.stream()
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

		Array	tasks	= instance.getSchedulerService().loadTasksFromDisk();
		Object	paused	= tasks.stream()
		    .filter( e -> e instanceof IStruct && "resumeFlag".equals( ( ( IStruct ) e ).getAsString( Key.task ) ) )
		    .map( e -> ( ( IStruct ) e ).get( Key.paused ) )
		    .findFirst().orElse( null );
		assertThat( paused ).isNotNull();
		assertThat( paused.toString() ).isEqualTo( "false" );
	}

	// --------------------------------------------------------------------------
	// Reload action tests
	// --------------------------------------------------------------------------

	@DisplayName( "reload shuts down the scheduler and reloads tasks from disk" )
	@Test
	public void testReloadReloadsTasksFromDisk() {
		// Create two tasks — they are persisted to tasks.json
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="reloadTask1" url="http://localhost/test" interval="120">
		    <bx:schedule action="update" task="reloadTask2" url="http://localhost/test" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		// Remove the in-memory scheduler to simulate a fresh state
		svc.removeScheduler( SCHEDULER_KEY, true, 5 );
		assertThat( svc.hasScheduler( SCHEDULER_KEY ) ).isFalse();

		// reload should restore the scheduler from tasks.json
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="reload">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		assertThat( svc.hasScheduler( SCHEDULER_KEY ) ).isTrue();
		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "reloadTask1" ) ).isTrue();
		assertThat( scheduler.hasTask( "reloadTask2" ) ).isTrue();
	}

	@DisplayName( "reload with no tasks.json starts an empty scheduler without error" )
	@Test
	public void testReloadWithNoTasksOnDisk() {
		// Ensure no tasks.json exists
		Path tasksFile = instance.getRuntimeHome().resolve( "config/tasks.json" );
		try {
			java.nio.file.Files.deleteIfExists( tasksFile );
		} catch ( Exception e ) {
			// ignore
		}

		// reload must not throw even when tasks.json is absent
		instance.executeSource(
		    """
		    <bx:schedule action="reload">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);

		// No scheduler is created when there is nothing to load
		assertThat( svc.hasScheduler( SCHEDULER_KEY ) ).isFalse();
	}

	// --------------------------------------------------------------------------
	// Class-based task tests
	// --------------------------------------------------------------------------

	private static final String	CLASS_FIXTURE			= "src.test.java.ortus.boxlang.runtime.components.async.ScheduleClassFixture";
	private static final String	MINIMAL_CLASS_FIXTURE	= "src.test.java.ortus.boxlang.runtime.components.async.ScheduleMinimalClassFixture";
	private static final String	CUSTOM_METHOD_FIXTURE	= "src.test.java.ortus.boxlang.runtime.components.async.ScheduleCustomMethodFixture";

	private IClassRunnable getClassInstance( String taskName ) {
		BaseScheduler	scheduler	= ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		TaskRecord		record		= scheduler.getTaskRecord( taskName );
		DynamicObject	dyno		= ( DynamicObject ) record.task.getTask();
		return ( IClassRunnable ) dyno.getTargetInstance();
	}

	/**
	 * Register a class-based task directly via {@link Schedule#registerClassTask} without ever
	 * calling {@code ScheduledTask.start()} — this keeps run-order assertions deterministic by
	 * avoiding any race with the real scheduled executor's own immediate first fire (which
	 * would otherwise run concurrently with a manual {@code run(true)} call in these tests).
	 */
	private ScheduledTask registerClassTaskDirect( String taskName, String className, String method ) {
		BaseScheduler	scheduler	= Schedule.getOrCreateScheduler( context, Schedule.DEFAULT_SCHEDULER_NAME );
		IStruct			taskDef		= Struct.of( Key._CLASS, className, Key.method, method );
		return Schedule.registerClassTask( scheduler, taskName, "", context, taskDef );
	}

	@DisplayName( "It can create a task with a class instead of a url" )
	@Test
	public void testCreateWithClass() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="classTask" class="%s" interval="120">
		    """.formatted( CLASS_FIXTURE ),
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "classTask" ) ).isTrue();
	}

	@DisplayName( "update throws when both url and class are provided" )
	@Test
	public void testCreateThrowsWhenBothUrlAndClass() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" url="http://localhost/test" class="%s" interval="120">
		    """.formatted( CLASS_FIXTURE ),
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	@DisplayName( "update throws when neither url nor class are provided" )
	@Test
	public void testCreateThrowsWhenNeitherUrlNorClass() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:schedule action="update" task="myTask" interval="120">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		) );
	}

	@DisplayName( "running a class task fires before/run/after/onSuccess in order" )
	@Test
	public void testClassTaskLifecycleOrderOnSuccess() {
		ScheduledTask task = registerClassTaskDirect( "classTask", CLASS_FIXTURE, "run" );
		task.run( true );

		IClassRunnable	fixtureInstance	= getClassInstance( "classTask" );
		Array			callOrder		= ( Array ) fixtureInstance.getThisScope().get( Key.of( "callOrder" ) );

		// ScheduledTask.run() fires afterTask before onTaskSuccess on the success path
		assertThat( callOrder.toArray() ).asList().containsExactly( "before", "run", "after", "onSuccess" ).inOrder();
		assertThat( fixtureInstance.getThisScope().getAsString( Key.of( "lastResult" ) ) ).isEqualTo( "ran-ok" );
	}

	@DisplayName( "running a class task that throws fires before/run/onError/after and records the failure" )
	@Test
	public void testClassTaskLifecycleOrderOnError() {
		ScheduledTask	task			= registerClassTaskDirect( "classTask", CLASS_FIXTURE, "run" );
		IClassRunnable	fixtureInstance	= getClassInstance( "classTask" );
		fixtureInstance.getThisScope().put( Key.of( "shouldThrow" ), true );

		task.run( true );

		Array callOrder = ( Array ) fixtureInstance.getThisScope().get( Key.of( "callOrder" ) );
		assertThat( callOrder.toArray() ).asList().containsExactly( "before", "run", "onError", "after" ).inOrder();
		assertThat( fixtureInstance.getThisScope().getAsString( Key.of( "lastErrorMessage" ) ) ).isEqualTo( "boom" );
		assertThat( ( ( java.util.concurrent.atomic.AtomicInteger ) task.getStats().get( "totalFailures" ) ).get() ).isEqualTo( 1 );
	}

	@DisplayName( "a class task with no lifecycle methods runs fine — they are optional" )
	@Test
	public void testClassTaskWithoutLifecycleMethods() {
		ScheduledTask task = registerClassTaskDirect( "minimalTask", MINIMAL_CLASS_FIXTURE, "run" );
		task.run( true );

		IClassRunnable fixtureInstance = getClassInstance( "minimalTask" );
		assertThat( fixtureInstance.getThisScope().getAsInteger( Key.of( "runCount" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "a class task honors a custom method attribute instead of run()" )
	@Test
	public void testClassTaskCustomMethod() {
		ScheduledTask task = registerClassTaskDirect( "customMethodTask", CUSTOM_METHOD_FIXTURE, "purge" );
		task.run( true );

		IClassRunnable fixtureInstance = getClassInstance( "customMethodTask" );
		assertThat( fixtureInstance.getThisScope().getAsBoolean( Key.of( "executed" ) ) ).isTrue();
	}

	@DisplayName( "a class task's onError() composes with onException=\"pause\" instead of being overwritten" )
	@Test
	public void testClassTaskOnErrorComposesWithOnExceptionPause() {
		BaseScheduler	scheduler	= Schedule.getOrCreateScheduler( context, Schedule.DEFAULT_SCHEDULER_NAME );
		IStruct			taskDef		= Struct.of( Key._CLASS, CLASS_FIXTURE, Key.method, "run", Key.onException, "pause" );
		ScheduledTask	task		= Schedule.registerClassTask( scheduler, "classTask", "", context, taskDef );
		Schedule.applyTaskConfiguration( task, null, taskDef, instance.getRuntimeContext() );

		IClassRunnable fixtureInstance = getClassInstance( "classTask" );
		fixtureInstance.getThisScope().put( Key.of( "shouldThrow" ), true );

		task.run( true );

		// The class's own onError() still fired ...
		Array callOrder = ( Array ) fixtureInstance.getThisScope().get( Key.of( "callOrder" ) );
		assertThat( callOrder.toArray() ).asList().contains( "onError" );
		// ... and the onException="pause" behavior also fired, disabling the task
		assertThat( task.isDisabled() ).isTrue();
	}

	@DisplayName( "a class-based task survives a persistence reload" )
	@Test
	public void testClassTaskSurvivesReload() {
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="update" task="classReloadTask" class="%s" interval="120">
		    """.formatted( CLASS_FIXTURE ),
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		// Remove the in-memory scheduler to simulate a fresh state
		svc.removeScheduler( SCHEDULER_KEY, true, 5 );
		assertThat( svc.hasScheduler( SCHEDULER_KEY ) ).isFalse();

		// @formatter:off
		instance.executeSource(
		    """
		    <bx:schedule action="reload">
		    <bx:schedule action="pause" task="classReloadTask">
		    <bx:schedule action="run" task="classReloadTask">
		    """,
		    context, BoxSourceType.BOXTEMPLATE
		);
		// @formatter:on

		BaseScheduler scheduler = ( BaseScheduler ) svc.getScheduler( SCHEDULER_KEY );
		assertThat( scheduler.hasTask( "classReloadTask" ) ).isTrue();

		IClassRunnable	fixtureInstance	= getClassInstance( "classReloadTask" );
		Array			callOrder		= ( Array ) fixtureInstance.getThisScope().get( Key.of( "callOrder" ) );
		assertThat( callOrder.toArray() ).asList().contains( "run" );
	}
}
