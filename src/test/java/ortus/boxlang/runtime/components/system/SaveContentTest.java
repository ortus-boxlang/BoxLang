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

public class SaveContentTest {

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

	@DisplayName( "It can capture content script" )
	@Test
	public void testCanCaptureContentScript() {

		instance.executeSource(
		    """
		       	echo( "before" );
		          saveContent variable="result" {
		       	echo( "Hello World" );
		       }
		    echo( "after" );
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content ACF script" )
	@Test
	public void testCanCaptureContentACFScript() {

		instance.executeSource(
		    """
		       	echo( "before" );
		          cfsaveContent( variable="result" ) {
		       	echo( "Hello World" );
		       }
		    echo( "after" );
		          """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content tag" )
	@Test
	public void testCanCaptureContentTag() {

		instance.executeSource(
		    """
		    before
		       <cfsaveContent variable="result" >
		       	Hello World
		      </cfsaveContent>
		    after
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture content BL tag" )
	@Test
	public void testCanCaptureContentBLTag() {

		instance.executeSource(
		    """
		    before
		       <bx:saveContent variable="result" >
		       	Hello World
		      </bx:saveContent>
		    after
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture trimmed content tag" )
	@Test
	public void testCanCaptureTrimmedContentTag() {

		instance.executeSource(
		    """
		    before
		       <cfsaveContent variable="result" trim=true >
		       	Hello World
		       </cfsaveContent>
		    after
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can capture appended content tag" )
	@Test
	public void testCanCaptureAppendedContentTag() {

		instance.executeSource(
		    """
		    <cfset result = "before-">
		       <cfsaveContent variable="variables.result" trim=true append="true" >
		       	Hello World
		       </cfsaveContent>

		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "before-Hello World" );
	}

}
