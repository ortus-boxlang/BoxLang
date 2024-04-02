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
package ortus.boxlang.runtime.bifs.global.string;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class CharTest {

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

	@DisplayName( "It converts a numeric UCS-2 code to a character" )
	@Test
	public void testCharConversion() {
		instance.executeSource(
		    """
		    result = char(65); // ASCII code for 'A'
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "A" );
	}

	@DisplayName( "It transpiles CF chr() to char()" )
	@Test
	public void testTranspileCF() {
		instance.executeSource(
		    """
		    result = chr(65); // ASCII code for 'A'
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "A" );
	}

	@DisplayName( "It returns an empty string for invalid UCS-2 code" )
	@Test
	public void testCharInvalidCode() {
		instance.executeSource(
		    """
		    result = char(100000); // Invalid UCS-2 code
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "" );
	}

	@DisplayName( "It throws an exception for negative UCS-2 code" )
	@Test
	public void testCharNegativeCode() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = char(-1); // Negative UCS-2 code
		        """,
		        context )
		);
	}
}
