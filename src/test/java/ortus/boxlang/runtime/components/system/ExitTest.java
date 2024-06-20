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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

public class ExitTest {

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

	@DisplayName( "It can exit" )
	@Test
	public void testCanExitTag() {

		instance.executeSource(
		    """
		    <cfset result = "before">
		    <cfexit>
		    <cfset result = "after">
		            """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can exit" )
	@Test
	public void testCanExitBLTag() {

		instance.executeSource(
		    """
		    <bx:set result = "before">
		    <bx:exit>
		    <bx:set result = "after">
		            """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can exit script" )
	@Test
	public void testCanExitScript() {

		instance.executeSource(
		    """
		    result = "before"
		    exit;
		    result = "after"
		            """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can exit ACF script" )
	@Test
	public void testCanExitACFScript() {

		instance.executeSource(
		    """
		    result = "before"
		    cfexit();
		    result = "after"
		            """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It cannot catch exit" )
	@Test
	public void testCannotCatchExit() {

		instance.executeSource(
		    """
		    <cfset result = "before">
		    <cftry>
		    	<cfexit>
		    	<cfcatch type="any">
		    		<cfset result = "caught!">
		    	</cfcatch>
		    </cftry>
		    <cfset result = "after">
		                   """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@Test
	public void testCanExitInclude() {

		instance.executeSource(
		    """
		    request.exitMethod="exitTag"
		       include "src/test/java/ortus/boxlang/runtime/components/system/ExitTests/plainInclude.bxm";
		    result &= "afterinclude";
		                  """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforeafterinclude" );

		instance.executeSource(
		    """
		    request.exitMethod="exitTemplate"
		       include "src/test/java/ortus/boxlang/runtime/components/system/ExitTests/plainInclude.bxm";
		    result &= "afterinclude";
		                  """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforeafterinclude" );

		assertThrows( BoxValidationException.class, () -> instance.executeSource(
		    """
		    request.exitMethod="loop"
		       include "src/test/java/ortus/boxlang/runtime/components/system/ExitTests/plainInclude.bxm";
		                  """,
		    context ) );
	}

	@Test
	public void testCanExitModuleStart() {

		instance.executeSource(
		    """
		    	 request.loopCount=0;
		    result = "";
		       request.exitWhen="start"
		       request.exitMethod="exitTag"
		          module template="src/test/java/ortus/boxlang/runtime/components/system/ExitTests/module.bxm" {
		         result &= "body";
		       }
		       result &= "aftermodule";
		                     """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforestartaftermodule" );

		instance.executeSource(
		    """
		    	 request.loopCount=0;
		    result = "";
		       request.exitMethod="exitTemplate"
		          module template="src/test/java/ortus/boxlang/runtime/components/system/ExitTests/module.bxm" {
		         result &= "body";
		       }
		       result &= "aftermodule";
		                     """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforestartbodyendaftermodule" );

		assertThrows( BoxValidationException.class, () -> instance.executeSource(
		    """
		    	 request.loopCount=0;
		    result = "";
		       request.exitMethod="loop"
		          module template="src/test/java/ortus/boxlang/runtime/components/system/ExitTests/module.bxm" {
		         result &= "body";
		       }
		                     """,
		    context ) );
	}

	@Test
	public void testCanExitModuleEnd() {

		instance.executeSource(
		    """
		    	 request.loopCount=0;
		    result = "";
		       request.exitWhen="end"
		       request.exitMethod="exitTag"
		          module template="src/test/java/ortus/boxlang/runtime/components/system/ExitTests/module.bxm" {
		         result &= "body";
		       }
		       result &= "aftermodule";
		                     """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "startbodybeforeendaftermodule" );

		instance.executeSource(
		    """
		    	 request.loopCount=0;
		    result = "";
		       request.exitMethod="exitTemplate"
		          module template="src/test/java/ortus/boxlang/runtime/components/system/ExitTests/module.bxm" {
		         result &= "body";
		       }
		       result &= "aftermodule";
		                     """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "startbodybeforeendaftermodule" );

		instance.executeSource(
		    """
		    	 request.loopCount=0;
		    result = "";
		          request.exitMethod="loop"
		          module template="src/test/java/ortus/boxlang/runtime/components/system/ExitTests/module.bxm" {
		         result &= "body";
		       }
		       result &= "aftermodule";
		          }
		                        """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "startbodybeforeendbodyendaftermodule" );
	}

	@Test
	public void testCanExitUDF() {

		instance.executeSource(
		    """
		    result = "";
		    request.exitMethod="exitTag"
		       include "src/test/java/ortus/boxlang/runtime/components/system/ExitTests/UDFInclude.bxm";
		    result &= "afterinclude";
		                  """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforebeforeUDFafterafterinclude" );

		instance.executeSource(
		    """
		    result = "";
		    request.exitMethod="exitTemplate"
		       include "src/test/java/ortus/boxlang/runtime/components/system/ExitTests/UDFInclude.bxm";
		    result &= "afterinclude";
		                  """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforebeforeUDFafterafterinclude" );

		assertThrows( BoxValidationException.class, () -> instance.executeSource(
		    """
		    result = "";
		    request.exitMethod="loop"
		       include "src/test/java/ortus/boxlang/runtime/components/system/ExitTests/UDFInclude.bxm";
		                  """,
		    context ) );
	}

	@Test
	public void testCanExitClassMethod() {

		instance.executeSource(
		    """
		    request.result = "";
		       request.exitMethod="exitTag"
		          new src.test.java.ortus.boxlang.runtime.components.system.ExitTests.ExitClass();
		       request.result &= "aftermethod";
		       result = request.result;
		                     """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforebeforeUDFafteraftermethod" );

		instance.executeSource(
		    """
		    request.result = "";
		       request.exitMethod="exitTemplate"
		          new src.test.java.ortus.boxlang.runtime.components.system.ExitTests.ExitClass();
		       request.result &= "aftermethod";
		       result = request.result;
		                     """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "beforebeforeUDFafteraftermethod" );

		assertThrows( BoxValidationException.class, () -> instance.executeSource(
		    """
		    request.result = "";
		       request.exitMethod="loop"
		          new src.test.java.ortus.boxlang.runtime.components.system.ExitTests.ExitClass();
		       request.result &= "aftermethod";
		       result = request.result;
		                     """,
		    context ) );
	}

}
