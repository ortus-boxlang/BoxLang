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
package TestCases.asm.operator;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

public class UnaryOperatorTest {

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
		instance.useASMBoxPiler();
	}

	@AfterEach
	public void teardownEach() {
		instance.useJavaBoxpiler();
	}

	@DisplayName( "Can negate a boolean literal" )
	@Test
	public void testDecalreTrueBooleanLiteral() {
		var result = instance.executeStatement(
		    """
		    !true;
		        """,
		    context );

		assertThat( result ).isEqualTo( false );
	}

	@DisplayName( "Can negate a boolean literal (false)" )
	@Test
	public void testDecalreFalseBooleanLiteral() {
		var result = instance.executeStatement(
		    """
		    !false;
		        """,
		    context );

		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "Can pre increment a value" )
	@Test
	public void testPreincrement() {
		var result = instance.executeStatement(
		    """
		       val = 1;
		    ++val;
		           """,
		    context );

		assertThat( result ).isEqualTo( 2 );
	}

	@DisplayName( "Can post increment a value" )
	@Test
	public void testPostIncrement() {
		var res = instance.executeStatement(
		    """
		    result = 1;
		         result++;
		                """,
		    context );

		assertThat( res ).isEqualTo( 1 );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "Can pre decrement a value" )
	@Test
	public void testPreDecrement() {
		var result = instance.executeStatement(
		    """
		       val = 1;
		    --val;
		           """,
		    context );

		assertThat( result ).isEqualTo( 0 );
	}

	@DisplayName( "Can post decrement a value" )
	@Test
	public void testPostDecrement() {
		var res = instance.executeStatement(
		    """
		    result = 1;
		         result--;
		                """,
		    context );

		assertThat( res ).isEqualTo( 1 );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "Can bitwise complement a value" )
	@Test
	public void testBitwiseComplement() {
		var res = instance.executeStatement(
		    """
		    b~35;
		                """,
		    context );

		assertThat( res ).isEqualTo( -36 );
	}
}
