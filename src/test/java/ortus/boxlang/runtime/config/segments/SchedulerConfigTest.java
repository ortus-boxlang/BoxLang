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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

public class SchedulerConfigTest {

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
	@DisplayName( "Missing keys keep the defaults" )
	void testProcessKeepsDefaults() {
		SchedulerConfig config = new SchedulerConfig();
		config.process( Struct.of() );
		assertThat( config.executor ).isEqualTo( "scheduled-tasks" );
		assertThat( config.cacheName ).isEqualTo( "default" );
	}
}
