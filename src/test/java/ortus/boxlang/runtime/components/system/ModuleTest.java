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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ModuleTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	StringBuffer		buffer;

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
		buffer		= new StringBuffer();
		context.pushBuffer( buffer );
	}

	@AfterEach
	public void teardownEach() {
		context.popBuffer();
	}

	@Test
	public void testCanRunCustomTagTag() {

		instance.executeSource(
		    """
		    <cfset brad="wood">
		       <cfmodule template="src/test/java/ortus/boxlang/runtime/components/system/MyTag.cfm" foo="bar">
		               """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "alwaysMyTagstartbarwood" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "hey you guys" );
	}

	@Test
	public void testCanRunCustomTagBLTag() {

		instance.executeSource(
		    """
		    <bx:set brad="wood">
		       <bx:module template="src/test/java/ortus/boxlang/runtime/components/system/MyTag.cfm" foo="bar">
		               """,
		    context, BoxSourceType.BOXTEMPLATE );
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
		    context, BoxSourceType.CFSCRIPT );
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
		    context, BoxSourceType.CFTEMPLATE );
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
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( buffer.toString().replaceAll( "\\s", "" ) ).isEqualTo( "<b>Pizza</b>" );
	}

	@Test
	public void testCanRunCustomTagCustomAttributes() {

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/ortus/boxlang/runtime/components/system/MyTag3.cfm" attributeCollection="#{ template : "something" }#">
		            """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( buffer.toString().trim() ).isEqualTo( "Template: something" );
	}

	@Test
	public void testCanRunCustomTagUnderscore() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
		instance.executeSource(
		    """
		    <cf_brad foo="bar">
		              """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( buffer.toString().trim() ).isEqualTo( "This is the Brad tag bar" );
	}

	@Test
	public void testCanRunCustomTagUnderscoreBL() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
		instance.executeSource(
		    """
		    <bx:_brad foo="bar">
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( buffer.toString().trim() ).isEqualTo( "This is the Brad tag bar" );
	}

	@Test
	public void testCanRunCustomTagName() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
		instance.executeSource(
		    """
		    <cfmodule name="brad" foo="bar">
		              """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( buffer.toString().trim() ).isEqualTo( "This is the Brad tag bar" );
	}

	@Test
	public void testCanRunCustomTagNameBL() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
		instance.executeSource(
		    """
		    <bx:module name="brad" foo="bar">
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( buffer.toString().trim() ).isEqualTo( "This is the Brad tag bar" );
	}

}
