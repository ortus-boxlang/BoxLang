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
import org.mockito.InjectMocks;
import org.mockito.Spy;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;

class FunctionServiceTest {

	FunctionService		service;

	@Spy
	@InjectMocks
	private BoxRuntime	runtime;

	@BeforeEach
	public void setupBeforeEach() {
		service = new FunctionService( runtime );
	}

	@DisplayName( "It can create the function service" )
	@Test
	void testItCanCreateIt() {
		assertThat( service ).isNotNull();
	}

	@DisplayName( "It can startup and register global functions" )
	@Test
	void testItCanStartup() {

		assertThat( service.getGlobalFunctionCount() ).isGreaterThan( 0 );
		assertThat( service.hasGlobalFunction( "print" ) ).isTrue();
	}

	@DisplayName( "It can invoke a global function" )
	@Test
	void testItCanInvokeAGlobalFunction() {

		assertThat( service.hasGlobalFunction( "print" ) ).isTrue();

		Object result = service.getGlobalFunction( "print" )
		    .invoke(
		        new ScriptingBoxContext(), new String[] { "Hello Unit Test" }, false, Key.of( "print" )
		    );

		assertThat( ( Boolean ) result ).isTrue();
	}

}
