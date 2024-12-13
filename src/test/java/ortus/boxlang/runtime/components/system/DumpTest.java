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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class DumpTest {

	static BoxRuntime			instance;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	ByteArrayOutputStream		baos;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.setOut( new PrintStream( ( baos = new ByteArrayOutputStream() ), true ) );
		variables = context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can dump tag" )
	@Test
	public void testCanDumpTag() {
		// @formatter:off
		instance.executeSource(
		    """
		       	<cfdump var="My Value" format="html">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
		// If we change our cfdump template, this may break
		assertThat( baos.toString() ).contains( "String" );
	}

	@DisplayName( "It can dump BL tag" )
	@Test
	public void testCanDumpBLTag() {
		// @formatter:off
		instance.executeSource(
		    """
		    	<bx:dump var='My Value'>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
	}

	@DisplayName( "It can dump BL tag 2" )
	@Test
	public void testCanDumpBLTag2() {
		// @formatter:off
		instance.executeSource(
		    """
		    	<bx:try>
					<bx:throw message="inner" />
					<bx:catch type="any">
						<bx:dump var="#bxCatch#" />
					</bx:catch>
				</bx:try>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertThat( baos.toString() ).contains( "inner" );
	}

	@DisplayName( "It can dump script" )
	@Test
	public void testCanDumpScript() {
		// @formatter:off
		instance.executeSource(
		    """
		       	dump var="My Value" format="html";
		    """,
		    context );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
		// If we change our cfdump template, this may break
		assertThat( baos.toString() ).contains( "String" );
	}

	@DisplayName( "It can dump ACF script" )
	@Test
	public void testCanDumpACFScript() {
		// @formatter:off
		instance.executeSource(
		    """
		      	cfdump( var="My Value");
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
	}

	@DisplayName( "It can dump using the BIF" )
	@Test
	public void testCanDumpUsingBIF() {
		// @formatter:off
		instance.executeSource(
		    """
		      	dump( "My Value");
		    """,
		    context );
		// @formatter:on
		assertThat( baos.toString() ).contains( "My Value" );
	}

}
