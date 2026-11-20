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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Tests for the scheduler's rolling load window, which is what balanced mode ranks nodes by.
 */
class SchedulerLoadTrackingTest {

	BoxRuntime			runtime;
	SchedulerService	service;

	@BeforeEach
	void setUp() {
		runtime	= BoxRuntime.getInstance( true );
		service	= runtime.getSchedulerService();
		service.resetLoadWindow();
	}

	private long taskSeconds() {
		IStruct snapshot = service.getLoadSnapshot();
		return snapshot.getAsStruct( Key.scheduler ).getAsLong( Key.taskSeconds );
	}

	@DisplayName( "one task running the whole window reports 60 task-seconds" )
	@Test
	void testOneTaskForAFullWindow() {
		for ( int i = 0; i < 60; i++ ) {
			service.sampleLoad( 1 );
		}
		assertThat( taskSeconds() ).isEqualTo( 60L );
	}

	@DisplayName( "two tasks running the whole window report 120 task-seconds" )
	@Test
	void testTwoTasksForAFullWindow() {
		for ( int i = 0; i < 60; i++ ) {
			service.sampleLoad( 2 );
		}
		assertThat( taskSeconds() ).isEqualTo( 120L );
	}

	@DisplayName( "five 5-second runs report 25 task-seconds" )
	@Test
	void testFiveShortRuns() {
		for ( int i = 0; i < 25; i++ ) {
			service.sampleLoad( 1 );
		}
		assertThat( taskSeconds() ).isEqualTo( 25L );
	}

	@DisplayName( "the window rolls: old samples fall out" )
	@Test
	void testWindowRolls() {
		// A full window of activity
		for ( int i = 0; i < 60; i++ ) {
			service.sampleLoad( 1 );
		}
		assertThat( taskSeconds() ).isEqualTo( 60L );

		// Then a quiet window, which pushes all of it out
		for ( int i = 0; i < 60; i++ ) {
			service.sampleLoad( 0 );
		}
		assertThat( taskSeconds() ).isEqualTo( 0L );
	}

	@DisplayName( "the snapshot carries the in-flight count and window size" )
	@Test
	void testSnapshotShape() {
		service.sampleLoad( 3 );
		IStruct snapshot = service.getLoadSnapshot().getAsStruct( Key.scheduler );

		assertThat( snapshot.getAsLong( Key.taskSeconds ) ).isEqualTo( 3L );
		assertThat( snapshot.containsKey( Key.inFlight ) ).isTrue();
		assertThat( snapshot.getAsLong( Key.of( "windowSeconds" ) ) ).isEqualTo( 60L );
	}

	@DisplayName( "the contribution is namespaced under scheduler, not flattened" )
	@Test
	void testContributionIsNamespaced() {
		IStruct contribution = service.getLoadSnapshot();

		// One top-level key, "scheduler", so it cannot collide with another contributor's data
		assertThat( contribution.size() ).isEqualTo( 1 );
		assertThat( contribution.containsKey( Key.scheduler ) ).isTrue();
		assertThat( contribution.getAsStruct( Key.scheduler ).containsKey( Key.taskSeconds ) ).isTrue();
	}
}
