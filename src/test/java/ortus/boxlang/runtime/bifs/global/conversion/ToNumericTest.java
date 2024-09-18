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

package ortus.boxlang.runtime.bifs.global.conversion;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ToNumericTest {

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

	@Test
	public void testCanCastDoubleAsNumber() {
		instance.executeSource(
		    """
		    result = toNumeric("123.45")
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 123.45 );
	}

	@Test
	public void testCanCastStringAsNumber() {
		instance.executeSource(
		    """
		    result = toNumeric("29.5")
		    """,
		    context );
		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 29.5 );
	}

	@Test
	public void testCanCastBinaryValueAsNumber() {
		instance.executeSource(
		    """
		    result = toNumeric("0110", "bin")
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@Test
	public void testCanCastHexValueAsNumber() {
		instance.executeSource(
		    """
		    result = toNumeric("000C", "hex")
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 12 );
	}

	@Test
	public void testCanCanOctalValueAsNumber() {
		instance.executeSource(
		    """
		    result = toNumeric("24", "oct")
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 20 );
	}

	@Test
	public void testCanCastDecValueAsNumber() {
		instance.executeSource(
		    """
		    result = toNumeric("29", "dec")
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 29 );
	}
}
