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

public class RoundTest {

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

	@DisplayName( "It rounds a number to the closest integer" )
	@Test
	public void testItRoundsToClosestInteger() {
		instance.executeSource(
		    """
		    result = round(0.3);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    result = round(1.7);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2 );

		instance.executeSource(
		    """
		    result = round(1.5);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2 );

		instance.executeSource(
		    """
		    result = round(123123123123123123123132132.8);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "123123123123123123123132133" );

		instance.executeSource(
		    """
		    result = round(123123123123123123123132132.2);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "123123123123123123123132132" );
	}

	@DisplayName( "It rounds a number to the closest integer as member" )
	@Test
	public void testItRoundsToClosestIntegerMember() {
		instance.executeSource(
		    """
		    result = (0.3).round();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 0 );

		instance.executeSource(
		    """
		    result = (1.7).round();
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 2 );
	}

	@DisplayName( "It rounds a number to the closest integer with precision" )
	@Test
	public void testItRoundsToClosestIntegerWithPrecision() {
		instance.executeSource(
		    """
		    result = round(0.37, 1);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 0.4 );

		instance.executeSource(
		    """
		    result = round(1.72, 1);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 1.7 );

		instance.executeSource(
		    """
		    result = round(1.123456789, 4);
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 1.1235 );

		instance.executeSource(
		    """
		    pi = 3.1415926535;
		    pi_rounded = round(pi, 2);
		    result = pi_rounded;
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 3.14 );
	}

}
