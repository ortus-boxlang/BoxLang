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
package TestCases.asm.phase1;

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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class AssignmentTest {

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

	@DisplayName( "Unscoped assignment" )
	@Test
	public void testUnscopedAssignment() {
		instance.executeSource(
		    """
		    foo = "test";
		    """,
		    context );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "It should not allow assignment via the equal keyword" )
	@Test
	public void testDontAssignUsingEqual() {
		instance.executeSource(
		    """
		    foo = 2;
		       foo equal "test";
		       """,
		    context );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "Nested dot assignment" )
	@Test
	public void testNestedDotAssignment() {
		instance.executeSource(
		    """
		    foo.bar = "test";
		    """,
		    context );
		assertThat( ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( "bar" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Multi multi identifier dot assignment" )
	@Test
	public void testmultimultiIdentifierAssignment() {
		instance.executeSource(
		    """
		    foo.bar.baz = "test";
		    """,
		    context );

		assertThat( ( ( IStruct ) ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( "bar" ) ) )
		    .get( Key.of( "baz" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket string assignment" )
	@Test
	public void testBracketStringAssignment() {
		instance.executeSource(
		    """
		    foo["bar"] = "test";
		    """,
		    context );
		assertThat( ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( "bar" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket string concat assignment" )
	@Test
	public void testBracketStringConcatAssignement() {
		instance.executeSource(
		    """
		    foo["b" & "ar"] = "test";
		    """,
		    context );
		assertThat( ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( "bar" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket number assignment" )
	@Test
	public void testBracketNumberAssignment() {
		instance.executeSource(
		    """
		    foo[ 7 ] = "test";
		    """,
		    context );
		assertThat( ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( "7" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket number expression assignment" )
	@Test
	public void testBracketNumberExpressionAssignment() {
		instance.executeSource(
		    """
		    foo[ 7 + 5 ] = "test";
		    """,
		    context );
		assertThat( ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( "12" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Bracket object assignment" )
	@Test
	public void testBracketObjectExpressionAssignment() {
		IStruct x = new Struct();
		x.assign( context, new Key( "bar" ), "baz" );
		instance.executeSource(
		    """
		    foo[ { bar : "baz" } ] = "test";
		    """,
		    context );
		assertThat( ( ( IStruct ) variables.get( Key.of( "foo" ) ) ).get( Key.of( x ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Mixed assignment" )
	@Test
	public void testBracketMixedAssignment() {
		instance.executeSource(
		    """
		    foo[ "a" & "aa" ][ 12 ].other[ 2 + 5 ] = "test";
		    """,
		    context );

		IStruct	foo		= ( IStruct ) variables.get( Key.of( "foo" ) );
		IStruct	aaa		= ( IStruct ) foo.get( Key.of( "aaa" ) );
		IStruct	twelve	= ( IStruct ) aaa.get( Key.of( "12" ) );
		IStruct	other	= ( IStruct ) twelve.get( Key.of( "other" ) );

		assertThat( other.get( Key.of( "7" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "simple quoted assignment" )
	@Test
	public void testSimpleQuotedAssignment() {
		instance.executeSource(
		    """
		    "result" = 5;
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 5 );
	}

	@DisplayName( "quoted assignment" )
	@Test
	public void testQuotedAssignment() {
		instance.executeSource(
		    """
		       "result" = "test";
		    name = "result2";
		    "variables.#name#" = "test2";
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "test" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "test2" );
	}

}
