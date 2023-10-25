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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ortus.boxlang.runtime.services.AsyncService.ExecutorRecord;

class AsyncServiceTest {

	static AsyncService asyncService;

	@BeforeEach
	public void setupBeforeEach() {
		asyncService = AsyncService.getInstance();
	}

	@DisplayName( "It can create the async service" )
	@Test
	void testItCanCreateIt() {
		assertThat( asyncService ).isNotNull();
	}

	@DisplayName( "It can get the executors map" )
	@Test
	void testItCanGetTheExecutors() {
		Map<?, ?> executors = asyncService.getExecutors();
		assertThat( executors ).isEmpty();
	}

	@ParameterizedTest
	@EnumSource( AsyncService.ExecutorType.class )
	@DisplayName( "It can register and work with executors" )
	void testItCanRegisterNewExecutors( AsyncService.ExecutorType executorType ) {
		String name = "tdd-" + executorType.name().toLowerCase();
		asyncService.newExecutor( name, executorType );

		Map<?, ?> executors = asyncService.getExecutors();

		assertThat( executors ).isNotEmpty();
		assertThat( asyncService.getExecutorNames() ).contains( name );
		assertThat( asyncService.hasExecutor( name ) ).isTrue();

		ExecutorRecord record = asyncService.getExecutor( name );
		assertThat( record.type() ).isEqualTo( executorType );
		assertThat( record.getStats().toString() ).contains( "POOL" );

		assertThat( asyncService.getExecutorStatusMap( name ) ).isNotEmpty();
		asyncService.deleteExecutor( name );
		assertThat( asyncService.hasExecutor( name ) ).isFalse();
	}

	@DisplayName( "It can shutdown all executors" )
	@Test
	void testItCanShutdownAllExecutors() {
		asyncService.newExecutor( "tdd", AsyncService.ExecutorType.FIXED );
		asyncService.newExecutor( "tdd2", AsyncService.ExecutorType.CACHED );
		asyncService.newExecutor( "tdd3", AsyncService.ExecutorType.SINGLE );

		asyncService.shutdownAllExecutors();

		assertThat( asyncService.getExecutor( "tdd" ).executor().isShutdown() ).isTrue();
		assertThat( asyncService.getExecutor( "tdd2" ).executor().isShutdown() ).isTrue();
		assertThat( asyncService.getExecutor( "tdd3" ).executor().isShutdown() ).isTrue();
	}

}
