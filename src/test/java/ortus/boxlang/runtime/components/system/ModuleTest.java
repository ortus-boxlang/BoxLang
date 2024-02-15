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
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ModuleTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );
	static StringBuffer	buffer	= new StringBuffer();

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.pushBuffer( buffer );
		variables = context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		context.popBuffer();
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
		buffer.setLength( 0 );
	}

	@Test
	public void testCanRunCustomTagTag() {

		instance.executeSource(
		    """
		    <cfset brad="wood">
		       <cfmodule template="src/test/java/ortus/boxlang/runtime/components/system/MyTag.cfm" foo="bar">
		               """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "alwaysMyTagstartbarwood" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "hey you guys" );
	}

	@Test
	public void testCanRunCustomTagScript() {

		instance.executeSource(
		    """
		    brad="wood";
		       module template="src/test/java/ortus/boxlang/runtime/components/system/MyTag.cfm" foo="bar";
		              """,
		    context );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "alwaysMyTagstartbarwood" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "hey you guys" );
	}

	@Test
	public void testCanRunCustomTagACFScript() {

		instance.executeSource(
		    """
		    brad="wood";
		       cfmodule( template="src/test/java/ortus/boxlang/runtime/components/system/MyTag.cfm", foo="bar" );
		              """,
		    context );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "alwaysMyTagstartbarwood" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "hey you guys" );
	}

	@Test
	public void testCanRunCustomTagWithEnd() {

		instance.executeSource(
		    """
		    <cfset brad="wood">
		       <cfmodule template="src/test/java/ortus/boxlang/runtime/components/system/MyTag.cfm" foo="bar">
		    	Pizza
		    </cfmodule>
		               """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "alwaysMyTagstartbarwoodalwaysazziPMyTagEnd" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "hey you guys" );
	}

	@Test
	public void testCanRunCustomTagWithEnd2() {

		instance.executeSource(
		    """
		       <cfmodule template="src/test/java/ortus/boxlang/runtime/components/system/MyTag2.cfm">
		    	Pizza
		    </cfmodule>
		               """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "<b>Pizza</b>" );
	}

	@Test
	public void testCanRunCustomTagCustomAttributes() {

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/ortus/boxlang/runtime/components/system/MyTag3.cfm" attributeCollection="#{ template : "something" }#">
		            """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( buffer.toString().trim() ).isEqualTo( "Template: something" );
	}

	@Test
	public void testCanRunCustomTagUnderscore() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
		instance.executeSource(
		    """
		    <cf_brad foo="bar">
		              """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( buffer.toString().trim() ).isEqualTo( "This is the Brad tag bar" );
	}

	@Test
	public void testCanRunCustomTagName() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
		instance.executeSource(
		    """
		    <cfmodule name="brad" foo="bar">
		              """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( buffer.toString().trim() ).isEqualTo( "This is the Brad tag bar" );
	}

}
