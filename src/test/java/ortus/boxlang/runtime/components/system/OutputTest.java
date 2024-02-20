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

public class OutputTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result				= new Key( "result" );
	static String		declareTestQuery	= """
	                                          <cfset myQry=queryNew("col1,col2","string,integer",[
	                                           {col1: "foo", col2: 42 },
	                                           {col1: "bar", col2: 100 },
	                                           {col1: "baz", col2: 500 },
	                                           {col1: "bum", col2: 9001 },
	                                           {col1: "qux", col2: 12345 }
	                                          ])>
	                                          """;

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
	public void testCfoutputQueryAsString() {
		instance.executeSource( declareTestQuery, context, BoxScriptType.CFMARKUP );

		instance.executeSource(
		    """
		    <cfoutput query="myQry">* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		    """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );

	}

	@Test
	public void testCfoutputQueryAsQuery() {
		instance.executeSource( declareTestQuery, context, BoxScriptType.CFMARKUP );
		instance.executeSource(
		    """
		    <cfoutput query="#myQry#">* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );
	}

	@Test
	public void testCfoutputQueryStartRow() {
		instance.executeSource( declareTestQuery, context, BoxScriptType.CFMARKUP );
		instance.executeSource(
		    """
		    <cfoutput query=myQry startRow=2>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );
	}

	@Test
	public void testCfoutputQueryMaxRows() {
		instance.executeSource( declareTestQuery, context, BoxScriptType.CFMARKUP );
		instance.executeSource(
		    """
		    <cfoutput query="#myQry#" maxRows=2>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2" );
	}

	@Test
	public void testCfoutputQueryStartAndMaxRows() {
		instance.executeSource( declareTestQuery, context, BoxScriptType.CFMARKUP );
		instance.executeSource(
		    """
		    <cfoutput query="#myQry#" startRow=2 maxRows=2>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* bar : 100 : 2* baz : 500 : 3" );
	}

	@Test
	public void testCfoutputQueryVariablesScoped() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("col1,col2","string,integer",[
		    	{col1: "foo", col2: 42 },
		    	{col1: "bar", col2: 100 },
		    	{col1: "baz", col2: 500 },
		    	{col1: "bum", col2: 9001 },
		    	{col1: "qux", col2: 12345 }
		    ])>
		    <cfoutput query="myQry">* #variables.myQry.col1# : #variables.myQry.col2# : #myQry.currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );

	}

	@Test
	public void testCfoutputQueryUnScoped() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("col1,col2","string,integer",[
		    	{col1: "foo", col2: 42 },
		    	{col1: "bar", col2: 100 },
		    	{col1: "baz", col2: 500 },
		    	{col1: "bum", col2: 9001 },
		    	{col1: "qux", col2: 12345 }
		    ])>
		    <cfoutput query="myQry">* #col1# : #col2# : #currentRow# : #recordCount# : #columnList#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "* foo : 42 : 1 : 5 : col1,col2* bar : 100 : 2 : 5 : col1,col2* baz : 500 : 3 : 5 : col1,col2* bum : 9001 : 4 : 5 : col1,col2* qux : 12345 : 5 : 5 : col1,col2" );

	}

	@Test
	public void testCfoutputQueryGrouped() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("col1,col2","string,integer",[
		    	{col1: "foo", col2: 42 },
		    	{col1: "foo", col2: 100 },
		    	{col1: "bar", col2: 500 },
		    	{col1: "bar", col2: 9001 },
		    	{col1: "baz", col2: 12345 },
		    	{col1: "baz", col2: 67890 }
		    ])>
		    <cfoutput query="myQry" group="col1">* #col1# : #col2# : #currentRow#</cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "* foo : 42 : 1* bar : 500 : 3* baz : 12345 : 5" );

	}

	@Test
	public void testTextOutput() {
		IBoxContext context2 = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		instance.executeSource(
		    """
		       <cfset bar = "brad">
		       This is #foo# output!
		            <cfoutput>
		              	This is #bar# output!
		            </cfoutput>
		    <cfset result = getBoxContext().getBuffer().toString()>
		              """, context2, BoxScriptType.CFMARKUP );

		var variables = ( VariablesScope ) context2.getScopeNearby( VariablesScope.name );
		assertThat( variables.getAsString( result ) ).contains( "This is #foo# output!" );
		assertThat( variables.getAsString( result ) ).contains( "This is brad output!" );

	}

}
