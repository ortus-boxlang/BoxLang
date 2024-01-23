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
package TestCases.tags;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class TagTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	VariablesScope		variables;
	static Key			result	= new Key( "result" );
	static Key			foo		= new Key( "foo" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic tags" )
	@Test
	public void testBasicTags() {
		instance.executeSource(
		    """
		    <cfset foo = "bar">
		       <cfoutput>
		         	This is #foo# output!
		       </cfoutput>

		         """, context, BoxScriptType.CFMARKUP );

	}

	@DisplayName( "if statement" )
	@Test
	public void testIfStatement() {
		instance.executeSource(
		    """
		    <cfif false >
		       	<cfset result = "then block">
		    <cfelse>
		    	<cfset result = "else block">
		    </cfif>

		                    """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "else block" );

		instance.executeSource(
		    """
		       <cfif true >
		       	<cfset result = "then block">
		       <cfelseif false >
		       	<cfset result = "first elseif block">
		       <cfelseif false >
		       	<cfset result = "second elseif block">
		    <cfelse>
		    	<cfset result = "else block">
		    	</cfif>

		                    """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "then block" );

		instance.executeSource(
		    """
		       <cfif false >
		       	<cfset result = "then block">
		       <cfelseif true >
		       	<cfset result = "first elseif block">
		       <cfelseif false >
		       	<cfset result = "second	elseif block">
		    <cfelse>
		    	<cfset result = "else block">
		    	</cfif>

		                    """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "first elseif block" );

		instance.executeSource(
		    """
		       <cfif false >
		       	<cfset result = "then block">
		       <cfelseif false >
		       	<cfset result = "first elseif block">
		       <cfelseif true >
		       	<cfset result = "second elseif block">
		    <cfelse>
		    	<cfset result = "else block">
		    	</cfif>

		                    """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "second elseif block" );

		instance.executeSource(
		    """
		       <cfif false >
		       	<cfset result = "then block">
		       <cfelseif false >
		       	<cfset result = "first elseif block">
		       <cfelseif false >
		       	<cfset result = "second elseif block">
		    <cfelse>
		    	<cfset result = "else block">
		    	</cfif>

		                    """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "else block" );

	}

	@DisplayName( "script Island" )
	@Test
	public void testScriptIsland() {
		instance.executeSource(
		    """
		            pre script
		         <cfset result = "it didn't work">
		      <cfscript>
		      	i=0
		    i++
		    if ( i == 1 ) {
		    	result = 'it worked'
		    }
		      </cfscript>
		         post script
		                 """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "it worked" );

	}

	@DisplayName( "tag Island" )
	@Test
	public void testTagIsland() {
		instance.executeSource(
		    """
		    i=0
		    i++
		    ```
		    <cfset foo = "bar">
		    Test outpout
		    ```
		    if ( i == 1 ) {
		    	result = 'it worked'
		    }
		                   """, context, BoxScriptType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "it worked" );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
	}

}