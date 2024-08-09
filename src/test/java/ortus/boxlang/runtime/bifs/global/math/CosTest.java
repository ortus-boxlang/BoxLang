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

public class CosTest {

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

	@DisplayName( "It returns cosine" )
	@Test
	public void testItReturnsCosine() {
		instance.executeSource(
		    """
		    result = cos( 0.3 );
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "0.9553364891256059809876433064346201717853546142578125" );
		instance.executeSource(
		    """
		    result = cos(-0.5);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "0.8775825618903727587394314468838274478912353515625" );
	}

	@DisplayName( "It returns cosine member" )
	@Test
	public void testItReturnsCosineMember() {
		instance.executeSource(
		    """
		    result = (1).cos();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( StrictMath.cos( 1 ) );
		instance.executeSource(
		    """
		    result = (-1).cos();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( StrictMath.cos( -1 ) );
		instance.executeSource(
		    """
		    result = (123123123123123123123).cos();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "0.75019958371567352362063729742658324539661407470703125" );
	}
}
