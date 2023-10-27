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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScheduledTaskTest {

	ScheduledTask task;

	@BeforeEach
	public void setupBeforeEach() {

	}

	@DisplayName( "It can create the scheduled task" )
	@Test
	void testItCanCreateIt() {
		// task = new ScheduledTask();
		Map map = new HashMap();
		map.put( "test", DateTime );

		LocalDateTime test = ( LocalDateTime ) map.get( "test" );

		System.out.println( test != null ? test : "null value" );
		// assertThat( task ).isNotNull();
	}

}
