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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class LoopTest {

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
	public void testcfloopQueryAsString() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput><cfloop query="myQry">* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		      """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );

	}

	@Test
	public void testcfloopQueryAsStringBL() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output><bx:loop query="myQry">* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</bx:loop></bx:output><bx:set result = getBoxContext().getBuffer().toString()>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );

	}

	@Test
	public void testcfloopQueryAsStringBLBreak() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output><bx:loop query="myQry"><bx:if currentRow LT 3><bx:continue></bx:if>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</bx:loop></bx:output><bx:set result = getBoxContext().getBuffer().toString()>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );

	}

	@Test
	public void testSingleRowQueryAsStringBL() {
		instance.executeSource( """
		                        <cfset myQry=queryNew("col1,col2","string,integer",[
		                         {col1: "foo", col2: 42 }
		                        ])>""", context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output><bx:loop query="myQry">* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</bx:loop></bx:output><bx:set result = getBoxContext().getBuffer().toString()>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1" );

	}

	@Test
	public void testcfloopQueryAsQuery() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );
		instance.executeSource(
		    """
		    <cfoutput><cfloop query="#myQry#">* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );
	}

	@Test
	public void testcfloopQueryStartRow() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );
		instance.executeSource(
		    """
		    <cfoutput><cfloop query=myQry startRow=2>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );
	}

	@Test
	public void testcfloopQueryMaxRows() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );
		instance.executeSource(
		    """
		    <cfoutput><cfloop query="#myQry#" endRow=2>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2" );
	}

	@Test
	public void testcfloopQueryStartAndMaxRows() {
		instance.executeSource( declareTestQuery, context, BoxSourceType.CFTEMPLATE );
		instance.executeSource(
		    """
		    <cfoutput><cfloop query="#myQry#" startRow=2 endRow=3>* #myQry.col1# : #myQry.col2# : #myQry.currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* bar : 100 : 2* baz : 500 : 3" );
	}

	@Test
	public void testcfloopQueryVariablesScoped() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("col1,col2","string,integer",[
		    	{col1: "foo", col2: 42 },
		    	{col1: "bar", col2: 100 },
		    	{col1: "baz", col2: 500 },
		    	{col1: "bum", col2: 9001 },
		    	{col1: "qux", col2: 12345 }
		    ])>
		    <cfoutput><cfloop query="myQry">* #variables.myQry.col1# : #variables.myQry.col2# : #myQry.currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() ).isEqualTo( "* foo : 42 : 1* bar : 100 : 2* baz : 500 : 3* bum : 9001 : 4* qux : 12345 : 5" );

	}

	@Test
	public void testcfloopQueryUnScoped() {
		instance.executeSource(
		    """
		          <cfset myQry=queryNew("col1,col2","string,integer",[
		       	{col1: "foo", col2: 42 },
		       	{col1: "bar", col2: 100 },
		       	{col1: "baz", col2: 500 },
		       	{col1: "bum", col2: 9001 },
		       	{col1: "qux", col2: 12345 }
		       ])>
		    <cfoutput><cfloop query="myQry">* #col1# : #col2# : #currentRow# : #recordCount# : #columnList#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "* foo : 42 : 1 : 5 : col1,col2* bar : 100 : 2 : 5 : col1,col2* baz : 500 : 3 : 5 : col1,col2* bum : 9001 : 4 : 5 : col1,col2* qux : 12345 : 5 : 5 : col1,col2" );

	}

	@Test
	public void testcfloopQueryGrouped() {
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
		    <cfoutput><cfloop query="myQry" group="col1">* #col1# : #col2# : #currentRow#</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "* foo : 42 : 1* bar : 500 : 3* baz : 12345 : 5" );

	}

	@Test
	public void testLoopTimes() {
		instance.executeSource(
		    """
		    result = "";
		    	loop times=5 {
		    		result &= "*";
		    	}
		          """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "*****" );
	}

	@Test
	public void testLoopZeroTimes() {
		instance.executeSource(
		    """
		    result = "";
		    	loop times=0 {
		    		result &= "*";
		    	}
		          """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "" );
	}

	@Test
	public void testLoopTimesIndex() {
		instance.executeSource(
		    """
		    result = "";
		    	loop times=5 index="i" {
		    		result &= i;
		    	}
		          """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopTimesItem() {
		instance.executeSource(
		    """
		    result = "";
		    	loop times=5 item="i" {
		    		result &= i;
		    	}
		          """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopListNoIndex() {
		instance.executeSource(
		    """
		    <cfset result = "">
		    <cfloop list="item1,item2,item3" item="thisItem">
		    	<cfset result &= thisItem >
		    </cfloop>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "item1item2item3" );

	}

	@Test
	public void testLoopArrayCollection() {
		instance.executeSource(
		    """
		    	<bx:set a = [1,2,3] >
		    	<bx:set result = "" >
		    	<bx:loop collection="#a#" item="interface">
		    		<bx:set result &= interface >
		    	</bx:loop>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "123" );
	}

	@Test
	public void testLoopFromTo() {
		instance.executeSource(
		    """
		    	<bx:set result = "" >
		    	<bx:loop from="1" to="10" index="i">
		    		<bx:set result &= i>
		    	</bx:loop>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "12345678910" );
	}

	@Test
	public void testLoopFromToStep() {
		instance.executeSource(
		    """
		    	<bx:set result = "" >
		    	<bx:loop from="1" to="10" index="i" step="2">
		    		<bx:set result &= i>
		    	</bx:loop>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "13579" );
	}

	@Test
	public void testLoopFromToStepContens() {
		instance.executeSource(
		    """
		    <!--- encrypt, decrypt methods --->
		    <cffunction name="contensEncrypt" access="public" returntype="string" hint="cfusion_encrypt counterpart" output="false">
		    		<cfargument name="sToEncrypt" type="string" required="true">
		    		<cfargument name="sEncKey" type="string" required="true">
		    		<cfset var iIndex = "">
		    		<cfset var sResult = "">

		    		<cfset arguments.sEncKey = repeatString(arguments.sEncKey, ceiling(len(arguments.sToEncrypt) / len(arguments.sEncKey)))>
		    		<cfloop from="1" to="#len(arguments.sToEncrypt)#" index="iIndex">
		    			<cfset sResult = sResult & rJustify(formatBaseN(bitXOR(asc(mid(arguments.sToEncrypt, iIndex, 1)), asc(mid(arguments.sEncKey, iIndex, 1))), 16), 2)>
		    		</cfloop>
		    		<cfreturn replace(sResult, " ", "0", "all")>
		    	</cffunction>


		    	<cffunction name="contensDecrypt" access="public" returntype="any" hint="cfusion_decrypt counterpart" output="false">
		    		<cfargument name="sToEncrypt" type="string" required="true">
		    		<cfargument name="sEncKey" type="string" required="true">
		    		<cfset var i = "">
		    		<cfset var sResult = "">

		    		<cftry>
		    			<cfset arguments.sEncKey = repeatString(arguments.sEncKey, ceiling(len(arguments.sToEncrypt) / 2 / len(arguments.sEncKey)))>
		    			<cfloop from="2" to="#len(arguments.sToEncrypt)#" index="i" step="2">
		    				<cfset sResult = sResult & chr(bitXOR(inputBaseN(mid(arguments.sToEncrypt, i-1, 2), 16), asc(mid(arguments.sEncKey, i/2, 1))))>
		    			</cfloop>
		    			<cfcatch>
		    				<cfreturn cfcatch>
		    			</cfcatch>
		    		</cftry>
		    		<cfreturn sResult>
		    	</cffunction>

		    	<cfset encr = contensEncrypt("test", "test")>
		    	<cfoutput>#contensDecrypt(encr, "test")#</cfoutput>
		    		""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testLoopCondition() {
		instance.executeSource(
		    """
		     function foo( required string name ) {
		    	 loop condition=arguments.name == "brad" {
		    return getFunctionCalledName();
		    		 break;
		    	 }
		     }

		     result = foo( "brad" );
		    				 """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "foo" );
	}

	@Test
	public void testLoopToFrom() {
		instance.executeSource(
		    """
		    	result = ""
		    	loop from="1" to="5" step="1" index="i" {
		    		result &= i;
		    	}
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopToFromNegativeStep() {
		instance.executeSource(
		    """
		    	result = ""
		    	loop from="5" to="1" step="-1" index="i" {
		    		result &= i;
		    	}
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "54321" );
	}

	@Test
	public void testLoopToFromZeroStep() {
		instance.executeSource(
		    """
		    	result = ""
		    	loop from="1" to="5" step="0" index="i" {
		    		result &= i;
		    	}
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "" );
	}

	@Test
	public void testLoopToFromDecimalStep() {
		instance.executeSource(
		    """
		    	result = ""
		    	loop from="1" to="10" step="1.5" index="i" {
		    		result = result.listAppend(i)
		    	}
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "1,2.5,4,5.5,7,8.5,10" );
	}

	@Test
	public void testLoopConditionMixCF() {
		instance.executeSource(
		    """
		    <cfset i = 1>
		    <cfset dataSize = 5>
		    <cfloop condition="#i# LTE #dataSize#">
		    	<cfoutput>#i#</cfoutput>
		    	<cfset i++>
		    </cfloop>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testLoopConditionMix() {
		instance.executeSource(
		    """
		    <bx:set i = 1>
		    <bx:set dataSize = 5>
		    <bx:loop condition="#i# LTE #dataSize#">
		    	<bx:output>#i#</bx:output>
		    	<bx:set i++>
		    </bx:loop>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

}
