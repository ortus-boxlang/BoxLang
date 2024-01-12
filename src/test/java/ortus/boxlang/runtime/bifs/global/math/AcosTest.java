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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class AcosTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It returns arccosine" )
	@Test
	public void testItReturnsACosine() {
		instance.executeSource(
		    """
		    result = acos(1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0.0 );
		instance.executeSource(
		    """
		    result = acos(0.5);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Math.acos( 0.5 ) );
	}

	@DisplayName( "It returns arccosine member" )
	@Test
	public void testItReturnsACosineMember() {
		instance.executeSource(
		    """
		    result = (1).acos();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0.0 );
		instance.executeSource(
		    """
		    result = (0.5).acos();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( Math.acos( 0.5 ) );
	}

	@DisplayName( "It throws an exception when the input is out of range" )
	public void testItThrowsAnExceptionWhenTheInputIsOutOfRange() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = acos(1.1);
		        """,
		        context )
		);
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = acos(-1.1);
		        """,
		        context )
		);
	}
}
