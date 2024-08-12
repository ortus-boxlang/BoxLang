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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

public class InputBaseNTest {

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

	@DisplayName( "It can convert strings to base n" )
	@Test
	public void testItCanConvertStringsToBaseN() {
		instance.executeSource(
		    """
		    result = inputBaseN( "1010", 2 )
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 10 );

		instance.executeSource(
		    """
		    result = inputBaseN( "3FF", 16 )
		      """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 1023 );

		instance.executeSource(
		    """
		    result = inputBaseN( "125", 10 )
		      """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 125 );

		instance.executeSource(
		    """
		    result = inputBaseN( "1101010110010101101001101000010010101101110011011111111001110110011", 2 )
		      """,
		    context );
		assertThat( variables.getAsNumber( result ).toString() ).isEqualTo( "123123123123123123123" );

	}

	@DisplayName( "It can throw an error if the radix is < 2 and > 36" )
	@Test
	public void testItCanThrowAnErrorIfTheRadixIsLessThan2AndGreaterThan36() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeSource(
			    """
			    result = inputBaseN( "1010", 1 )
			    """,
			    context );
		} );

		assertThrows( BoxValidationException.class, () -> {
			instance.executeSource(
			    """
			    result = inputBaseN( "1010", 37 )
			    """,
			    context );
		} );
	}

}
