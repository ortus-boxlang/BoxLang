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
package ortus.boxlang.runtime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class BoxRuntimeTest {

	@Before
	public void setUp() {
		// Start the runtime before each test
		BoxRuntime.startup();
	}

	@After
	public void tearDown() {
		// Shutdown the runtime after each test
		BoxRuntime.shutdown();
	}

	@Test
	public void testGetInstance() {
		// Ensure getInstance() returns the same instance as startup()
		BoxRuntime instance1 = BoxRuntime.getInstance();
		BoxRuntime instance2 = BoxRuntime.startup();

		assertThat( instance1 ).isNotNull();
		assertThat( instance1 ).isSameInstanceAs( instance2 );
	}

	@Test
	public void testStartup() {
		// Ensure startup() returns the same instance as getInstance()
		BoxRuntime instance1 = BoxRuntime.getInstance();
		BoxRuntime instance2 = BoxRuntime.startup();

		assertThat( instance1 ).isNotNull();
		assertThat( instance1 ).isSameInstanceAs( instance2 );
		assertThat( BoxRuntime.isStarted() ).isTrue();
		assertThat( BoxRuntime.getStartTime().isPresent() ).isTrue();
	}

	@Test
	public void testShutdown() {
		// Ensure shutdown sets instance to null
		BoxRuntime.shutdown();
		assertThat( BoxRuntime.getInstance() ).isNull();
		assertThat( BoxRuntime.isStarted() ).isFalse();
		assertThat( BoxRuntime.getStartTime().isPresent() ).isFalse();
	}

}
