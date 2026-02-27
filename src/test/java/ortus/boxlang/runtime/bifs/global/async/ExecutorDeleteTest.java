/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ExecutorDeleteTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Can delete an executor" )
	@Test
	public void testDeleteExecutor() {
		// @formatter:off
		instance.executeSource("""
			executorNew( "test-delete", "cached" );
			result = executorHas( "test-delete" );
			executorDelete( "test-delete" );
			result2 = executorHas( "test-delete" );
		""", context);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
	}

	@DisplayName( "Can delete an executor that is running" )
	@Test
	public void testDeleteRunningExecutor() {
		// @formatter:off
		instance.executeSource("""
			executor = executorNew( "test-delete-running", "cached" );
			// Submit a task to the executor
			executor.submit( () -> {
				sleep( 100 );
				return 42;
			} );
			// Delete it while running
			result = executorDelete( "test-delete-running" );
			result2 = executorHas( "test-delete-running" );
		""", context);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
	}

	@DisplayName( "Deleting non-existent executor does not throw" )
	@Test
	public void testDeleteNonExistentExecutor() {
		// @formatter:off
		instance.executeSource("""
			result = executorDelete( "non-existent-executor" );
		""", context);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "Can delete and recreate executor with same name" )
	@Test
	public void testDeleteAndRecreateExecutor() {
		// @formatter:off
		instance.executeSource("""
			executorNew( "test-recreate", "fixed" );
			executorDelete( "test-recreate" );
			executorNew( "test-recreate", "cached" );
			result = executorHas( "test-recreate" );
		""", context);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

}
