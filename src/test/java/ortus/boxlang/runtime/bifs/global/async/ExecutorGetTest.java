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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.executors.BoxExecutor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ExecutorGetTest {

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

	@DisplayName( "Can get the default executor and submit a task to it" )
	@Test
	public void testGetDefaultExecutorAndSubmitTask() {
		// @formatter:off
		instance.executeSource( """
			executor = executorGet();
			future = executor.submit( () -> {
				return "Task Done!";
			} );
			result = future.get();
			println( result );
		""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "Task Done!" );
	}

	@DisplayName( "Can get the default executor (io-tasks) when no name is given" )
	@Test
	public void testGetDefaultExecutor() {
		// @formatter:off
		instance.executeSource( """
			result = executorGet();
		""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( BoxExecutor.class );
		BoxExecutor executor = ( BoxExecutor ) variables.get( result );
		assertThat( executor.name() ).isEqualTo( "io-tasks" );
	}

	@DisplayName( "Can get the io-tasks executor by name" )
	@Test
	public void testGetIoTasksExecutor() {
		// @formatter:off
		instance.executeSource( """
			result = executorGet( "io-tasks" );
		""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( BoxExecutor.class );
		BoxExecutor executor = ( BoxExecutor ) variables.get( result );
		assertThat( executor.name() ).isEqualTo( "io-tasks" );
	}

	@DisplayName( "Can get the cpu-tasks executor by name" )
	@Test
	public void testGetCpuTasksExecutor() {
		// @formatter:off
		instance.executeSource( """
			result = executorGet( "cpu-tasks" );
		""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( BoxExecutor.class );
		BoxExecutor executor = ( BoxExecutor ) variables.get( result );
		assertThat( executor.name() ).isEqualTo( "cpu-tasks" );
	}

	@DisplayName( "Can get the scheduled-tasks executor by name" )
	@Test
	public void testGetScheduledTasksExecutor() {
		// @formatter:off
		instance.executeSource( """
			result = executorGet( "scheduled-tasks" );
		""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( BoxExecutor.class );
		BoxExecutor executor = ( BoxExecutor ) variables.get( result );
		assertThat( executor.name() ).isEqualTo( "scheduled-tasks" );
	}

	@DisplayName( "Can get a custom executor by name after it has been created" )
	@Test
	public void testGetCustomExecutor() {
		// @formatter:off
		instance.executeSource( """
			executorNew( "test-get-custom", "fixed" );
			result = executorGet( "test-get-custom" );
			executorDelete( "test-get-custom" );
		""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( BoxExecutor.class );
		BoxExecutor executor = ( BoxExecutor ) variables.get( result );
		assertThat( executor.name() ).isEqualTo( "test-get-custom" );
	}

	@DisplayName( "Throws an exception when executor name does not exist" )
	@Test
	public void testGetNonExistentExecutorThrows() {
		assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			instance.executeSource( """
				result = executorGet( "non-existent-executor" );
			""", context );
			// @formatter:on
		} );
	}

}
