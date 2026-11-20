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
package ortus.boxlang.runtime.config.segments;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class SchedulerConfigTest {

	@Test
	@DisplayName( "An unknown task overlap policy is rejected instead of quietly disabling overlap control" )
	void testUnknownOverlapPolicyIsRejected() {
		SchedulerConfig config = new SchedulerConfig();

		// A typo must not be accepted. The policy is compared to "skip" when a claim is arbitrated, so an
		// unrecognised value simply fails that comparison and the safety policy becomes "overlap everything" -
		// silently, in the one place a task was trying to protect itself.
		assertThrows( BoxRuntimeException.class, () -> config.process( Struct.of( Key.taskOverlapPolicy, "skpi" ) ) );

		// Both real policies still pass
		config.process( Struct.of( Key.taskOverlapPolicy, "run" ) );
		assertThat( config.taskOverlapPolicy ).isEqualTo( "run" );
		config.process( Struct.of( Key.taskOverlapPolicy, "SKIP" ) );
		assertThat( config.taskOverlapPolicy ).isEqualTo( "skip" );
	}

	@Test
	@DisplayName( "Defaults use the scheduled-tasks executor and the default cache" )
	void testDefaults() {
		SchedulerConfig config = new SchedulerConfig();
		assertThat( config.executor ).isEqualTo( "scheduled-tasks" );
		assertThat( config.cacheName ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "A custom executor and cacheName are applied when processed" )
	void testProcessAppliesExecutorAndCacheName() {
		SchedulerConfig config = new SchedulerConfig();
		config.process( Struct.of(
		    Key.executor, "cpu-tasks",
		    Key.cacheName, "myCache"
		) );
		assertThat( config.executor ).isEqualTo( "cpu-tasks" );
		assertThat( config.cacheName ).isEqualTo( "myCache" );
		assertThat( config.asStruct().get( Key.executor ) ).isEqualTo( "cpu-tasks" );
		assertThat( config.asStruct().get( Key.cacheName ) ).isEqualTo( "myCache" );
	}

	@Test
	@DisplayName( "Tasks are clustered by default, and the deployment can turn that off on a clustered node" )
	void testClusterTasksDefault() {
		SchedulerConfig config = new SchedulerConfig();

		// Default preserves the existing behaviour: a task with no opinion clusters on a clustered node
		assertThat( config.clusterTasks ).isTrue();

		config.process( Struct.of( Key.clusterTasks, false ) );
		assertThat( config.clusterTasks ).isFalse();

		// Explicit true still wins, and a partial config keeps what it had
		config.process( Struct.of( Key.clusterTasks, true ) );
		assertThat( config.clusterTasks ).isTrue();
		config.process( Struct.of() );
		assertThat( config.clusterTasks ).isTrue();

		assertThat( config.asStruct().get( Key.clusterTasks ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Missing keys keep the defaults" )
	void testProcessKeepsDefaults() {
		SchedulerConfig config = new SchedulerConfig();
		config.process( Struct.of() );
		assertThat( config.executor ).isEqualTo( "scheduled-tasks" );
		assertThat( config.cacheName ).isEqualTo( "default" );
	}
}
