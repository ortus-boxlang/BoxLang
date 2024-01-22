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

package ortus.boxlang.runtime.bifs.global.conversion;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ParseNumberTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "It parses a binary string to a numeric value" )
	@Test
	public void testParseBinaryString() {
		instance.executeSource(
		    """
		    result = parseNumber("1010", "bin");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 10 );
	}

	@DisplayName( "It parses an octal string to a numeric value" )
	@Test
	public void testParseOctalString() {
		instance.executeSource(
		    """
		    result = parseNumber("755", "oct");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 493 );
	}

	@DisplayName( "It parses a decimal string to a numeric value" )
	@Test
	public void testParseDecimalString() {
		instance.executeSource(
		    """
		    result = parseNumber("123.45", "dec");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 123.45 );
	}

	@DisplayName( "It parses a hexadecimal string to a numeric value" )
	@Test
	public void testParseHexString() {
		instance.executeSource(
		    """
		    result = parseNumber("1A4", "hex");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 420 );
	}

	@DisplayName( "It throws an exception for an invalid radix" )
	@Test
	public void testInvalidRadix() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = parseNumber("1010", "invalid");
		        """,
		        context )
		);
	}
}
