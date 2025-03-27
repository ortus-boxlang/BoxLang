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

import java.time.Duration;

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

public class AbsTest {

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

	@DisplayName( "It returns abs" )
	@Test
	public void testItReturnsAbs() {
		instance.executeSource(
		    """
		    result = abs( 0 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
		instance.executeSource(
		    """
		    result = abs( 1 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It returns abs Negative" )
	@Test
	public void testItReturnsAbsNeg() {
		instance.executeSource(
		    """
		    result = abs( -1 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It returns abs member" )
	@Test
	public void testItReturnsAbsMember() {
		instance.executeSource(
		    """
		    result = (0 ).abs();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
		instance.executeSource(
		    """
		    result = ( 1 ).abs();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It returns abs member negative" )
	@Test
	public void testItReturnsAbsMemberNeg() {
		instance.executeSource(
		    """
		    result = ( -1 ).abs();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	public void testDuration() {
		instance.executeSource(
		    """
		    import java.time.Duration;
		    import java.time.temporal.ChronoUnit;

		    d = Duration.of( 0, ChronoUnit.SECONDS );
		    result = d.abs();
		            """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Duration.class );
	}

}
