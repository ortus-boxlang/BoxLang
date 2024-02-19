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
package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.async.tasks.Scheduler;
import ortus.boxlang.runtime.scopes.Key;

public class SchedulerServiceTest {

	static SchedulerService	schedulerService;
	static BoxRuntime		runtime;

	@BeforeAll
	public static void setUp() {
		runtime				= BoxRuntime.getInstance( true );
		// Given: Initialize your SchedulerService before each test
		schedulerService	= runtime.getSchedulerService();
	}

	@AfterAll
	public static void tearDownAfterAll() {
		runtime.shutdown( true );
	}

	@DisplayName( "Test it can get an instance of the service" )
	@Test
	void testItCanGetInstance() {
		assertThat( schedulerService ).isNotNull();
	}

	@Test
	@DisplayName( "Given startup event, when started, then all schedulers should start and announce the startup event" )
	void testStartupEvent() {
		schedulerService.onStartup();
	}

	@Test
	@DisplayName( "Given a registered scheduler, when registered, then it should be added to the service" )
	void testRegisterScheduler() {
		// Given: Any necessary setup

		// When: A scheduler is registered
		IScheduler scheduler = new Scheduler( "test", runtime.getAsyncService() );
		schedulerService.registerScheduler( scheduler );

		assertThat( schedulerService.size() ).isEqualTo( 1 );
		assertThat( schedulerService.getScheduler( Key.of( "test" ) ) ).isEqualTo( scheduler );
		assertThat( schedulerService.hasScheduler( Key.of( "test" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Given a registered scheduler, when unregistered, then it should be removed from the service" )
	void testUnregisterScheduler() {
		// Given: A registered scheduler
		IScheduler scheduler = new Scheduler( "test", runtime.getAsyncService() );
		schedulerService.removeScheduler( Key.of( "test" ), true, 0L );

		schedulerService.registerScheduler( scheduler );

		// When: The scheduler is unregistered
		schedulerService.removeScheduler( Key.of( "test" ), true, 0L );

		assertThat( schedulerService.getScheduler( Key.of( "test" ) ) ).isNull();
		assertThat( schedulerService.hasScheduler( Key.of( "test" ) ) ).isFalse();
	}
}
