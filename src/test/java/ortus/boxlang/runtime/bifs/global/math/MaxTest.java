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

public class MaxTest {

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

	@DisplayName( "It returns max" )
	@Test
	public void testItReturnsMax() {
		instance.executeSource(
		    """
		    result = max( 1,2);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2 );
		instance.executeSource(
		    """
		    result = max( -1, -2 );
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( -1 );
		instance.executeSource(
		    """
		    result = max( -2, 2 );
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2 );
		instance.executeSource(
		    """
		    result = max( 1, 2.3 );
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2.3 );
		instance.executeSource(
		    """
		    result = max( 123123123123123123123123, 46456456456456456456465456 );
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "46456456456456456456465456" );
	}

}
