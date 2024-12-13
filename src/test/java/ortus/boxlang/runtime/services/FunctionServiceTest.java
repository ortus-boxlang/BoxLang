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
import org.mockito.Spy;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;

class FunctionServiceTest {

	FunctionService		service;

	@Spy
	private BoxRuntime	runtime	= BoxRuntime.getInstance();

	@BeforeEach
	public void setupBeforeEach() {
		service = new FunctionService( runtime );
		service.onConfigurationLoad();
		service.onStartup();
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

	@DisplayName( "It can get all global function names" )
	@Test
	void testItCanGetAllGlobalFunctionNames() {
		assertThat( service.getGlobalFunctionNames() ).isNotEmpty();
		assertThat( service.getGlobalFunctionNames() ).asList().contains( "Print" );
	}

	@DisplayName( "It can get a null for a non-existent global function" )
	@Test
	void testItCanGetANullForANonExistentGlobalFunction() {
		assertThat( service.getGlobalFunction( "nonexistent" ) ).isNull();
	}

	@DisplayName( "It can invoke a global function" )
	@Test
	void testItCanInvokeAGlobalFunction() {

		assertThat( service.hasGlobalFunction( "print" ) ).isTrue();

		Object result = service.getGlobalFunction( "print" )
		    .invoke(
		        new ScriptingRequestBoxContext(), new String[] { "Hello Unit Test" }, false, Key.of( "print" )
		    );

		assertThat( result ).isNull();
	}

}
