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

import ortus.boxlang.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class IncludeTest {

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

	@DisplayName( "It can include template script" )
	@Test
	public void testCanIncludeTemplateScript() {

		instance.executeSource(
		    """
		    include template="src/test/java/ortus/boxlang/runtime/bifs/global/system/IncludeTest.cfs";
		     """,
		    context );
		assertThat( variables.get( result ).toString().contains( "IncludeTest.cfs" ) ).isTrue();
	}

	@DisplayName( "It can include template script shortcut" )
	@Test
	public void testCanIncludeTemplateScriptShortcut() {

		instance.executeSource(
		    """
		    include "src/test/java/ortus/boxlang/runtime/bifs/global/system/IncludeTest.cfs";
		     """,
		    context );
		assertThat( variables.get( result ).toString().contains( "IncludeTest.cfs" ) ).isTrue();
	}

	@DisplayName( "It can include template ACF script" )
	@Test
	public void testCanIncludeTemplateACFScript() {

		instance.executeSource(
		    """
		    cfinclude( template="src/test/java/ortus/boxlang/runtime/bifs/global/system/IncludeTest.cfs" )
		     """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ).toString().contains( "IncludeTest.cfs" ) ).isTrue();
	}

	@DisplayName( "It can include template tag" )
	@Test
	public void testCanIncludeTemplateTag() {

		instance.executeSource(
		    """
		    <cfinclude template="src/test/java/ortus/boxlang/runtime/bifs/global/system/IncludeTest.cfs">
		     """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ).toString().contains( "IncludeTest.cfs" ) ).isTrue();
	}

	@DisplayName( "It can include template BL tag" )
	@Test
	public void testCanIncludeTemplateBLTag() {

		instance.executeSource(
		    """
		    <bx:include template="src/test/java/ortus/boxlang/runtime/bifs/global/system/IncludeTest.cfs">
		     """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ).toString().contains( "IncludeTest.cfs" ) ).isTrue();
	}

}
