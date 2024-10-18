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

package ortus.boxlang.runtime.bifs.global.string;

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

public class ReReplaceTest {

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
	public void testReplaceOne() {
		instance.executeSource(
		    """
		    result = ReReplace( "test 123!", "[^a-z0-9]", '', "one" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test123!" );
	}

	@Test
	public void testReplaceAll() {
		instance.executeSource(
		    """
		    result = ReReplace( "test 123!", "[^a-z0-9]", '', "all" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test123" );
	}

	@Test
	public void testReplaceOneMember() {
		instance.executeSource(
		    """
		    result = "test 123!".ReReplace( "[^a-z0-9]", '', "one" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test123!" );
	}

	@Test
	public void testReplaceAllMember() {
		instance.executeSource(
		    """
		    result = "test 123!".ReReplace( "[^a-z0-9]", '', "all" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test123" );
	}

	@Test
	public void testReplaceBackReferenceAll() {
		instance.executeSource(
		    """
		    result = reReplace("123abc456_000def999", "[0-9]+([a-z]+)[0-9]+", "*\\1*", "all");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "*abc*_*def*" );
	}

	@Test
	public void testReplaceBackReferenceOne() {
		instance.executeSource(
		    """
		    result = reReplace("123abc456_000def999", "[0-9]+([a-z]+)[0-9]+", "*\\1*", "one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "*abc*_000def999" );
	}

	@Test
	public void testCaseStartStop() {
		instance.executeSource(
		    """
		    result = REReplace("HELLO", "([[:upper:]]*)", "Don't shout\\scream \\L\\1");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Don't shout\\scream hello" );

		instance.executeSource(
		    """
		    result = REReplace("first@SECOND@THIRD", "(.*)@(.*)@(.*)", "\\U\\1\\E@\\L\\2\\E@\\3");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "FIRST@second@THIRD" );
	}

	@Test
	public void testCaseSwapOneChar() {
		instance.executeSource(
		    """
		    result = REReplace("first@SECOND@THIRD", "(.*)@(.*)@(.*)", "\\u\\1@\\l\\2@\\3");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "First@sECOND@THIRD" );

		instance.executeSource(
		    """
		    result = "zachary".reReplace("^(.)(.*)$", "\\u\\1\\2");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Zachary" );

		instance.executeSource(
		    """
		    result = "zachary".reReplace("^(.)(.*)$", "\\U\\1\\2");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ZACHARY" );

	}

	@Test
	public void testEscapeChar() {
		instance.executeSource(
		    """
		    result = reReplacenocase('script_name',"[/\\\\]index\\.cfm","");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "script_name" );

	}

	@Test
	public void testPerlStyleCurlyLooseness() {
		// @formatter:off
		instance.executeSource(
		    """
				input   = "String with {{TOKEN}}";
				result = reReplacenocase( input, "{{[A-Z]+}}", "brad"  );
		    """,
		    context );
			assertThat( variables.get( result ) ).isEqualTo("String with brad");
		// @formatter:on
		// @formatter:off
		instance.executeSource(
		    """
				input   = "String with {{TOKEN}}";
				result = reReplacenocase( input, "\\{\\{[A-Z]+\\}\\}", "brad"  );
		    """,
		    context );
			assertThat( variables.get( result ) ).isEqualTo("String with brad");
		// @formatter:on
	}

	@DisplayName( "Doesn't throw exception if the string is null" )
	@Test
	public void testNullString() {
		instance.executeSource(
		    """
		    result = reReplace( null, "[^a-z0-9]", '', "one" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "" );
	}

}
