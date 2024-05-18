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

package ortus.boxlang.runtime.bifs.global.math;

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

public class RandomizeTest {

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

	@DisplayName( "It returns a null" )
	@Test
	public void testItReturnsNull() {
		instance.executeSource(
		    """
		    result = randomize(12345);
		    """,
		    context );
		assertThat( variables.get( result ) ).isNull();
	}

	@DisplayName( "It should return a value equal to or greater than 0" )
	@Test
	public void testReturnsGE0() {
		instance.executeSource(
		    """
		    randomize(12345);
		    result = rand();
		    """,
		    context );
		assertThat( ( Double ) variables.get( result ) >= 0 ).isTrue();
	}

	@DisplayName( "It should return a value less than 1" )
	@Test
	public void testReturnsLT1() {
		instance.executeSource(
		    """
		    randomize(12345);
		    result = rand();
		    """,
		    context );
		assertThat( ( Double ) variables.get( result ) < 1 ).isTrue();
	}

	@DisplayName( "It should always return the same values when seeded" )
	@Test
	public void testReturnsSeededRand() {
		instance.executeSource(
		    """
		    randomize(12345);
		    result = rand();
		    """,
		    context );
		Object result1 = variables.get( result );
		instance.executeSource(
		    """
		    result = rand();
		    """,
		    context );
		Object result2 = variables.get( result );
		instance.executeSource(
		    """
		    randomize(12345);
		    result = rand();
		    """,
		    context );
		Object result3 = variables.get( result );
		instance.executeSource(
		    """
		    result = rand();
		    """,
		    context );
		Object result4 = variables.get( result );
		assertThat( ( Double ) result1 ).isEqualTo( result3 );
		assertThat( ( Double ) result2 ).isEqualTo( result4 );
	}

}
