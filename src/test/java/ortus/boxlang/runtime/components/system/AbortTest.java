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
import ortus.boxlang.runtime.types.exceptions.CustomException;

public class AbortTest {

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

	@DisplayName( "It can abort" )
	@Test
	public void testCanAbortTag() {

		instance.executeSource(
		    """
		    <cfset result = "before">
		    <cfabort>
		    <cfset result = "after">
		            """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can abort" )
	@Test
	public void testCanAbortBLTag() {

		instance.executeSource(
		    """
		    <bx:set result = "before">
		    <bx:abort>
		    <bx:set result = "after">
		            """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can abort script" )
	@Test
	public void testCanAbortScript() {

		instance.executeSource(
		    """
		    result = "before"
		    abort;
		    result = "after"
		            """,
		    context );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can abort ACF script" )
	@Test
	public void testCanAbortACFScript() {

		instance.executeSource(
		    """
		    result = "before"
		    cfabort();
		    result = "after"
		            """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

	@DisplayName( "It can abort with message" )
	@Test
	public void testCanAbortWithMessage() {

		CustomException e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <cfset result = "before">
		    <cfabort showError="This is my error">
		    <cfset result = "after">
		            """,
		    context, BoxSourceType.CFTEMPLATE ) );

		assertThat( variables.getAsString( result ) ).contains( "before" );
		assertThat( e.getMessage() ).isEqualTo( "This is my error" );
	}

	@DisplayName( "It can abort page" )
	@Test
	public void testCanAbortPage() {

		instance.executeSource(
		    """
		       <cfset result = "before">
		    <cfinclude template="src/test/java/ortus/boxlang/runtime/components/system/AbortTestInclude.cfm">
		    <cfset result &= " after include">
		               """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ) ).contains( "before inside include after include" );
	}

	@DisplayName( "It cannot catch abort" )
	@Test
	public void testCannotCatchAbort() {

		instance.executeSource(
		    """
		    <cfset result = "before">
		    <cftry>
		    	<cfabort>
		    	<cfcatch type="any">
		    		<cfset result = "caught!">
		    	</cfcatch>
		    </cftry>
		    <cfset result = "after">
		                   """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ) ).contains( "before" );
	}

}
