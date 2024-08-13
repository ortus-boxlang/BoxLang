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

public class BinaryPlusOperatorTest {

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

	@DisplayName( "Can add two positive int" )
	@Test
	public void testAddPostiiveInts() {
		Number result = ( Number ) instance.executeStatement(
		    """
		    1 + 1;
		        """,
		    context );

		assertThat( result.doubleValue() ).isEqualTo( 2 );
	}

	@DisplayName( "Can add a positive int on the left and a negative int on the right" )
	@Test
	public void testAddPostiiveToNegativeInts() {
		Number result = ( Number ) instance.executeStatement(
		    """
		    1 + -1;
		        """,
		    context );

		assertThat( result.doubleValue() ).isEqualTo( 0 );
	}

	@DisplayName( "Can add a positive int on the left and a negative int on the right" )
	@Test
	public void testAddNegativeToPositiveInts() {
		Number result = ( Number ) instance.executeStatement(
		    """
		    -1 + 1;
		        """,
		    context );

		assertThat( result.doubleValue() ).isEqualTo( 0 );
	}

	@DisplayName( "Can add two positive doubles" )
	@Test
	public void testAddPostiiveDoubles() {
		Number result = ( Number ) instance.executeStatement(
		    """
		    1.0 + 1.5;
		        """,
		    context );

		assertThat( result.doubleValue() ).isEqualTo( 2.5 );
	}

	@DisplayName( "Can add a positive double on the left and a negative double on the right" )
	@Test
	public void testAddPostiiveToNegativeDoubles() {
		Number result = ( Number ) instance.executeStatement(
		    """
		    1.0 + -1.0;
		        """,
		    context );

		assertThat( result.doubleValue() ).isEqualTo( 0.0 );
	}

	@DisplayName( "Can add a positive double on the left and a negative double on the right" )
	@Test
	public void testAddNegativeToPositiveDoubles() {
		Number result = ( Number ) instance.executeStatement(
		    """
		    -1.0 + 1.0;
		        """,
		    context );

		assertThat( result.doubleValue() ).isEqualTo( 0.0 );
	}

}
