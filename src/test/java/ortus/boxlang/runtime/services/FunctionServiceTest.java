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

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.ScriptingBoxContext;

class FunctionServiceTest {

	@DisplayName( "It can create the function service" )
	@Test
	void testItCanCreateIt() {
		FunctionService functionService = FunctionService.getInstance();
		assertThat( functionService ).isNotNull();
	}

	@DisplayName( "It can startup and register global functions" )
	@Test
	void testItCanStartup() {
		FunctionService functionService = FunctionService.getInstance();

		assertThat( functionService.getGlobalFunctionCount() ).isGreaterThan( 0 );
		assertThat( functionService.hasGlobalFunction( "print" ) ).isTrue();
	}

	@DisplayName( "It can invoke a global function" )
	@Test
	void testItCanInvokeAGlobalFunction() {
		FunctionService functionService = FunctionService.getInstance();

		assertThat( functionService.hasGlobalFunction( "print" ) ).isTrue();

		Optional<Object> result = functionService.getGlobalFunction( "print" )
		    .invoke(
		        new ScriptingBoxContext(), "Hello Unit Test"
		    );

		assertThat( result.isPresent() ).isTrue();
		assertThat( ( Boolean ) result.get() ).isTrue();
	}

}
