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

public class LogTest {

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

	@DisplayName( "It returns natural logarithm" )
	@Test
	public void testItReturnsNaturalLogarithm() {
		instance.executeSource(
		    """
		    result = log(1);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( StrictMath.log( 1 ) );
		instance.executeSource(
		    """
		    result = log(0.5);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "-0.6931471805625540367844847419647936" );
	}

	@DisplayName( "It returns natural logarithm member" )
	@Test
	public void testItReturnsNaturalLogarithmMember() {
		instance.executeSource(
		    """
		    result = (1).log();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( StrictMath.log( 1 ) );
		instance.executeSource(
		    """
		    result = (0.5).log();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "-0.6931471805625540367844847419647936" );
		instance.executeSource(
		    """
		    result = (123123123123123123123123123).log();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "60.07522708756309744519394650363874" );
	}
}
