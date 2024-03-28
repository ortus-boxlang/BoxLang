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

package ortus.boxlang.runtime.components.system;

import static com.google.common.truth.Truth.assertThat;

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

public class ParamTest {

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

	@DisplayName( "It can param tag" )
	@Test
	public void testCanParamTag() {

		instance.executeSource(
		    """
		    <cfparam name="result" default="my default">
		       """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param BL tag" )
	@Test
	public void testCanParamBLTag() {

		instance.executeSource(
		    """
		    <bx:param name="result" default="my default">
		       """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script" )
	@Test
	public void testCanParamScript() {

		instance.executeSource(
		    """
		    param name="result" default="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param ACF script" )
	@Test
	public void testCanParamACFScript() {

		instance.executeSource(
		    """
		    cfparam(  name="result", default="my default");
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut no type" )
	@Test
	public void testCanParamScriptShortcutNoType() {

		instance.executeSource(
		    """
		    param result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut scope no type" )
	@Test
	public void testCanParamScriptShortcutScopeNoType() {

		instance.executeSource(
		    """
		    param variables.result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut scope type" )
	@Test
	public void testCanParamScriptShortcutScopeType() {

		instance.executeSource(
		    """
		    param String variables.result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut scope type Only" )
	@Test
	public void testCanParamScriptShortcutScopeTypeOnly() {

		instance.executeSource(
		    """
		    variables.result="value";
		      param String variables.result;
		         """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "value" );
	}

	@DisplayName( "It can param script shortcut with type" )
	@Test
	public void testCanParamScriptShortcutWithType() {

		instance.executeSource(
		    """
		    param String result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut with type only" )
	@Test
	public void testCanParamScriptShortcutWithTypeOnly() {

		instance.executeSource(
		    """
		    result ="foo"
		       param String result;
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "foo" );
	}

}
