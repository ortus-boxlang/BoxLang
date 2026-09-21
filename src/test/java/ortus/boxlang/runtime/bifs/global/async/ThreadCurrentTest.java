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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ThreadCurrentTest {

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

	@DisplayName( "It can return the current thread object" )
	@Test
	public void testThreadCurrentReturnsThread() {
		// @formatter:off
		instance.executeSource(
		    """
				result = threadCurrent();
		    """,
		    context,
		    BoxSourceType.CFSCRIPT
		);
		// @formatter:on

		Object threadObj = variables.get( result );
		assertThat( threadObj ).isNotNull();
		assertThat( threadObj ).isInstanceOf( Thread.class );
	}

	@DisplayName( "It can return a live thread" )
	@Test
	public void testThreadCurrentReturnsLiveThread() {
		// @formatter:off
		instance.executeSource(
		    """
				result = threadCurrent();
		    """,
		    context,
		    BoxSourceType.CFSCRIPT
		);
		// @formatter:on

		Thread threadObj = ( Thread ) variables.get( result );
		assertThat( threadObj.isAlive() ).isTrue();
	}

	@DisplayName( "It can match the current thread" )
	@Test
	public void testThreadCurrentMatchesCurrentThread() {
		// @formatter:off
		instance.executeSource(
		    """
				result = threadCurrent();
		    """,
		    context,
		    BoxSourceType.CFSCRIPT
		);
		// @formatter:on

		Thread threadObj = ( Thread ) variables.get( result );
		// The returned thread should equal the JVM thread executing this test
		assertThat( threadObj ).isEqualTo( Thread.currentThread() );
		assertThat( threadObj.getName() ).isEqualTo( Thread.currentThread().getName() );
		assertThat( threadObj.getId() ).isEqualTo( Thread.currentThread().getId() );
	}

	@DisplayName( "It can get the thread name" )
	@Test
	public void testThreadCurrentHasName() {
		// @formatter:off
		instance.executeSource(
		    """
				result = threadCurrent().getName();
		    """,
		    context,
		    BoxSourceType.CFSCRIPT
		);
		// @formatter:on

		String threadName = ( String ) variables.get( result );
		assertThat( threadName ).isNotNull();
		assertThat( threadName ).isNotEmpty();
	}

}
