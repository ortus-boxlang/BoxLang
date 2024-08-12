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

public class AtnTest {

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

	@DisplayName( "It returns arctangent" )
	@Test
	public void testItReturnsAtn() {
		instance.executeSource(
		    """
		    result = atn(1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( StrictMath.atan( 1.0 ) );

		instance.executeSource(
		    """
		    result = atn(0.5);
		    """,
		    context );
		assertThat( variables.get( result ).toString() ).isEqualTo( "0.4603442826194848666034154678194800" );
	}

	@DisplayName( "It returns arctangent member" )
	@Test
	public void testItReturnsAtnMember() {
		instance.executeSource(
		    """
		    result = (1).atn();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( StrictMath.atan( 1.0 ) );

		instance.executeSource(
		    """
		    result = (0.5).atn();
		    """,
		    context );
		assertThat( variables.get( result ).toString() ).isEqualTo( "0.4603442826194848666034154678194800" );
	}
}
