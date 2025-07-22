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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
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
	public void testcfloopQueryGroupedScript() {
		instance.executeSource(
		    """
		          myQry=queryNew("col1,col2","string,integer",[
		       	{col1: "foo", col2: 42 },
		       	{col1: "foo", col2: 100 },
		       	{col1: "bar", col2: 500 },
		       	{col1: "bar", col2: 9001 },
		       	{col1: "baz", col2: 12345 },
		       	{col1: "baz", col2: 67890 }
		       ]);
		       cfloop( query="myQry", group="col1" ) {
		    	echo( "* #col1# : #col2# : #currentRow#" )
		    }
		    result = getBoxContext().getBuffer().toString();
		          """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "* foo : 42 : 1* bar : 500 : 3* baz : 12345 : 5" );
	}

	@Test
	public void testcfloopQueryGroupNested() {
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
		    <cfoutput><cfloop query="myQry" group="col1">[#col1# : <cfloop>(#col2# : #currentRow#)</cfloop>]</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "[foo : (42 : 1)(100 : 2)][bar : (500 : 3)(9001 : 4)][baz : (12345 : 5)(67890 : 6)]" );
	}

	@Test
	public void testcfloopQueryGroupNestedList() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("col0,col1,col2","string,string,integer",[
		    	{ col0: "fizz", col1: "foo", col2: 42 },
		    	{ col0: "fizz", col1: "foo", col2: 100 },
		    	{ col0: "fizz", col1: "bar", col2: 500 },
		    	{ col0: "buzz", col1: "bar", col2: 9001 },
		    	{ col0: "buzz", col1: "baz", col2: 12345 },
		    	{ col0: "buzz", col1: "baz", col2: 67890 }
		    ])>
		    <cfoutput><cfloop query="myQry" group="col0,col1">[#col0#,#col1# : <cfloop>(#col2# : #currentRow#)</cfloop>]</cfloop></cfoutput><cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).trim() )
		    .isEqualTo(
		        "[fizz,foo : (42 : 1)(100 : 2)][fizz,bar : (500 : 3)][buzz,bar : (9001 : 4)][buzz,baz : (12345 : 5)(67890 : 6)]" );
	}

	@Test
	public void testcfloopQueryGroupDoubleNested() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		    	{department: "IT", jobTitle: "Developer", name: "Alice"},
		    	{department: "IT", jobTitle: "Developer", name: "Bob"},
		    	{department: "IT", jobTitle: "Manager", name: "Carol"},
		    	{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		    	{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		    	{department: "HR", jobTitle: "Manager", name: "Frank"}
		    ])>
		    <cfoutput>
		    	<cfloop query="myQry" group="department">
		    		[#department# :
		    			<cfloop group="jobTitle">
		    				(#jobTitle# :
		    					<cfloop>
		    						#name#
		    					</cfloop>
		    				)
		    			</cfloop>
		    		]
		    	</cfloop>
		    </cfoutput>
		    <cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:AliceBob)(Manager:Carol)][HR:(Recruiter:DaveEve)(Manager:Frank)]"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderData() {
		instance.executeSource(
		    """
		       <cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		    	{department: "IT", jobTitle: "Developer", name: "Alice"},
		    	{department: "IT", jobTitle: "Developer", name: "Bob"},
		    	{department: "HR", jobTitle: "Manager", name: "Frank"},
		    	{department: "IT", jobTitle: "Manager", name: "Carol"},
		    	{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		    	{department: "HR", jobTitle: "Recruiter", name: "Eve"}
		    ])>
		    <cfoutput>
		    	<cfloop query="myQry" group="department">
		    		[#department# :
		    			<cfloop group="jobTitle">
		    				(#jobTitle# :
		    					<cfloop>
		    						#name#
		    					</cfloop>
		    				)
		    			</cfloop>
		    		]
		    	</cfloop>
		    </cfoutput>
		    <cfset result = getBoxContext().getBuffer().toString()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:AliceBob)][HR:(Manager:Frank)][IT:(Manager:Carol)][HR:(Recruiter:DaveEve)]"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithContinue() {
		instance.executeSource(
		    """
		           <cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		      	{department: "IT", jobTitle: "Developer", name: "Alice"},
		      	{department: "IT", jobTitle: "Developer", name: "Bob"},
		      	{department: "IT", jobTitle: "Manager", name: "Carol"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		      	{department: "HR", jobTitle: "Manager", name: "Frank"}
		      ])>
		        <cfoutput>
		    <cfloop query="myQry" group="department">
		    	[#department# :
		    		<cfloop group="jobTitle">
		    			(#jobTitle# :
		    				<cfloop>
		    					#name#
		    					<cfcontinue>
		    					!
		    				</cfloop>
		    			<cfcontinue>
		    			)
		    		</cfloop>
		    	<cfcontinue>
		    	]
		    </cfloop>
		        </cfoutput>
		        <cfset result = getBoxContext().getBuffer().toString()>
		           """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:AliceBob(Manager:Carol[HR:(Recruiter:DaveEve(Manager:Frank"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithInnerBreak() {
		instance.executeSource(
		    """
		           <cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		      	{department: "IT", jobTitle: "Developer", name: "Alice"},
		      	{department: "IT", jobTitle: "Developer", name: "Bob"},
		      	{department: "IT", jobTitle: "Manager", name: "Carol"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		      	{department: "HR", jobTitle: "Manager", name: "Frank"}
		      ])>
		        <cfoutput>
		    <cfloop query="myQry" group="department">
		    	[#department# :
		    		<cfloop group="jobTitle">
		    			(#jobTitle# :
		    				<cfloop>
		    					#name#
		    					<cfbreak>
		    					!
		    				</cfloop>
		    			)
		    		</cfloop>
		    	]
		    </cfloop>
		        </cfoutput>
		        <cfset result = getBoxContext().getBuffer().toString()>
		           """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:Alice)(Manager:Carol)][HR:(Recruiter:Dave)(Manager:Frank)]"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithMiddleBreak() {
		instance.executeSource(
		    """
		           <cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		      	{department: "IT", jobTitle: "Developer", name: "Alice"},
		      	{department: "IT", jobTitle: "Developer", name: "Bob"},
		      	{department: "IT", jobTitle: "Manager", name: "Carol"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		      	{department: "HR", jobTitle: "Manager", name: "Frank"}
		      ])>
		        <cfoutput>
		    <cfloop query="myQry" group="department">
		    	[#department# :
		    		<cfloop group="jobTitle">
		    			(#jobTitle# :
		    				<cfloop>
		    					#name#!
		    				</cfloop>
		    			)
		    			<cfbreak>
		    		</cfloop>
		    	]
		    </cfloop>
		        </cfoutput>
		        <cfset result = getBoxContext().getBuffer().toString()>
		           """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:Alice!Bob!)][HR:(Recruiter:Dave!Eve!)]"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithOuterBreak() {
		instance.executeSource(
		    """
		           <cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		      	{department: "IT", jobTitle: "Developer", name: "Alice"},
		      	{department: "IT", jobTitle: "Developer", name: "Bob"},
		      	{department: "IT", jobTitle: "Manager", name: "Carol"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		      	{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		      	{department: "HR", jobTitle: "Manager", name: "Frank"}
		      ])>
		        <cfoutput>
		    <cfloop query="myQry" group="department">
		    	[#department# :
		    		<cfloop group="jobTitle">
		    			(#jobTitle# :
		    				<cfloop>
		    					#name#!
		    				</cfloop>
		    			)
		    		</cfloop>
		    	]
		    	<cfbreak>
		    </cfloop>
		        </cfoutput>
		        <cfset result = getBoxContext().getBuffer().toString()>
		           """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:Alice!Bob!)(Manager:Carol!)]"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithInnerReturn() {
		instance.executeSource(
		    """
		    	<cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		    		{department: "IT", jobTitle: "Developer", name: "Alice"},
		    		{department: "IT", jobTitle: "Developer", name: "Bob"},
		    		{department: "IT", jobTitle: "Manager", name: "Carol"},
		    		{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		    		{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		    		{department: "HR", jobTitle: "Manager", name: "Frank"}
		    	])>
		    	<cfoutput>
		    		<cfloop query="myQry" group="department">
		    			[#department# :
		    				<cfloop group="jobTitle">
		    					(#jobTitle# :
		    						<cfloop>
		    							#name#
		    							<cfset result = getBoxContext().getBuffer().toString()  >
		    							<cfreturn>
		    							!
		    						</cfloop>
		    					)
		    				</cfloop>
		    			]
		    		</cfloop>
		    	</cfoutput>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:Alice"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithMiddleReturn() {
		instance.executeSource(
		    """
		    	<cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		    		{department: "IT", jobTitle: "Developer", name: "Alice"},
		    		{department: "IT", jobTitle: "Developer", name: "Bob"},
		    		{department: "IT", jobTitle: "Manager", name: "Carol"},
		    		{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		    		{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		    		{department: "HR", jobTitle: "Manager", name: "Frank"}
		    	])>
		    	<cfoutput>
		    		<cfloop query="myQry" group="department">
		    			[#department# :
		    				<cfloop group="jobTitle">
		    					(#jobTitle# :
		    						<cfloop>
		    							#name#
		    							!
		    						</cfloop>
		    					)
		    		<cfset result = getBoxContext().getBuffer().toString()  >
		    		<cfreturn>
		    				</cfloop>
		    			]
		    		</cfloop>
		    	</cfoutput>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:Alice!Bob!)"
		    );
	}

	@Test
	public void testcfloopQueryGroupDoubleNestedOutOfOrderDataWithOuterReturn() {
		instance.executeSource(
		    """
		    	<cfset myQry=queryNew("department,jobTitle,name","string,string,string",[
		    		{department: "IT", jobTitle: "Developer", name: "Alice"},
		    		{department: "IT", jobTitle: "Developer", name: "Bob"},
		    		{department: "IT", jobTitle: "Manager", name: "Carol"},
		    		{department: "HR", jobTitle: "Recruiter", name: "Dave"},
		    		{department: "HR", jobTitle: "Recruiter", name: "Eve"},
		    		{department: "HR", jobTitle: "Manager", name: "Frank"}
		    	])>
		    	<cfoutput>
		    		<cfloop query="myQry" group="department">
		    			[#department# :
		    				<cfloop group="jobTitle">
		    					(#jobTitle# :
		    						<cfloop>
		    							#name#
		    							!
		    						</cfloop>
		    					)
		    				</cfloop>
		    			]
		    		<cfset result = getBoxContext().getBuffer().toString()  >
		    		<cfreturn>
		    		</cfloop>
		    	</cfoutput>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s+", "" ) )
		    .isEqualTo(
		        "[IT:(Developer:Alice!Bob!)(Manager:Carol!)]"
		    );
	}

	@DisplayName( "Test that loop can handle a group with one row column having one data set" )
	@Test
	public void testCfLoopSingleGrouping() {
		// @formatter:off
		instance.executeSource( """
					myQry = queryNew(
						"mygroup,myvalue",
						"string,integer", [
							{ mygroup:"A", myvalue: 10 },
							{ mygroup:"B", myvalue: 20 },
							{ mygroup:"B", myvalue: 30 },
							{ mygroup:"B", myvalue: 40 },
							{ mygroup:"B", myvalue: 50 },
							{ mygroup:"C", myvalue: 60 },
							{ mygroup:"D", myvalue: 70 },
							{ mygroup:"D", myvalue: 80 },
						]
					)
					bx:loop query=myQry group="mygroup"{
						writeoutput( " myGroup: #myGroup#" )
							bx:loop{
								writeoutput( " myValue: #myvalue#" )
							}
					}
					result = getBoxContext().getBuffer().toString()
				""",
				context );
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "result" ) ) )
		    .isEqualTo(
		        " myGroup: A myValue: 10 myGroup: B myValue: 20 myValue: 30 myValue: 40 myValue: 50 myGroup: C myValue: 60 myGroup: D myValue: 70 myValue: 80" );
	}

	@DisplayName( "Test that loop can handle a group with one row column having one data set, but with no inner loop" )
	@Disabled( "Brad to fix, this is not working." )
	@Test
	public void testCanLoopWithGroupAndNoInnerGroup() {
		// @formatter:off
		instance.executeSource( """
					myQry = queryNew(
						"mygroup,myvalue",
						"string,integer", [
							{ mygroup:"A", myvalue: 10 },
							{ mygroup:"B", myvalue: 20 },
							{ mygroup:"B", myvalue: 30 },
							{ mygroup:"B", myvalue: 40 },
							{ mygroup:"B", myvalue: 50 },
							{ mygroup:"C", myvalue: 60 },
							{ mygroup:"D", myvalue: 70 },
							{ mygroup:"D", myvalue: 80 },
						]
					)
					// This should output only the group name with the first record of each group
					bx:loop query=myQry group="mygroup"{
						writeoutput( "myGroup: #myGroup# myValue: #myValue# " )
					}
					result = getBoxContext().getBuffer().toString()
				""",
				context );
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "result" ) ) )
		    .isEqualTo(
		        "myGroup: A myValue: 10 myGroup: B myValue: 20 myGroup: C myValue: 60 myGroup: D myValue: 70" );
	}

	@Test
	@DisplayName( "It can handle multiple levels of grouping" )
	public void testCfLoopMultipleGroupings() {
		instance.executeSource(
		    """
		    <cfset myQry = queryNew("group1,group2,value", "string,string,integer", [
		      {group1:"A", group2:"X", value: 10},
		      {group1:"A", group2:"X", value: 20},
		      {group1:"A", group2:"Y", value: 30},
		      {group1:"B", group2:"Z", value: 40},
		      {group1:"B", group2:"Z", value: 50}
		    ])>
		    <cfoutput>
		      <cfloop query="myQry" group="group1">
		         [#group1#:
		            <cfloop group="group2">
		               (#group2# : #value#)
		            </cfloop>
		         ]
		      </cfloop>
		    </cfoutput>
		    <cfset result = getBoxContext().getBuffer().toString()>
		    """,
		    context, BoxSourceType.CFTEMPLATE
		);

		// The inner cfloop returns only the first row of each grouped subset.
		// For group A, group "X" returns the first value 10 and group "Y" returns 30.
		// For group B, group "Z" returns 40.
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) )
		    .isEqualTo( "[A:(X:10)(Y:30)][B:(Z:40)]" );
	}

	@Test
	public void testLoopTimes() {
		instance.executeSource(
		    """
		       result = "";
		    bx:loop times=5 {
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
		    bx:loop times=0 {
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
		    bx:loop times=5 index="i" {
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
		    bx:loop times=5 item="i" {
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
		    bx:loop condition=arguments.name == "brad" {
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
		    	bx:loop from="1" to="5" step="1" index="i" {
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
		    	bx:loop from="5" to="1" step="-1" index="i" {
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
		    	bx:loop from="1" to="5" step="0" index="i" {
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
		    	bx:loop from="1" to="10" step="1.5" index="i" {
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

	@Test
	public void testContinueInDoWhile() {
		instance.executeSource(
		    """
		    i = 0;
		         do {
		    i++;
		    continue;
		      }while( i < 5 )

		      result = i
		               """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( 5 );
	}

	@Test
	public void testBreakInDoWhile() {
		instance.executeSource(
		    """
		     i = 0;
		          do {
		    break;
		     i++;
		     continue;
		       }while( i < 5 )

		       result = i
		                """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@Test
	public void testDoInTry() {
		instance.executeSource(
		    """
		       try{
		    do{

		    }while( false )
		    }
		    catch( any e ){

		    }
		                  """,
		    context, BoxSourceType.BOXSCRIPT );
	}

	@Test
	public void testLoopXMLKeys() {
		instance.executeSource(
		    """
		    <bx:savecontent variable="sFileContent">
		    		<root>
		    			<sub>test</sub>
		    		</root>
		    </bx:savecontent>

		    <bx:set oXMLData = XMLParse(sFileContent)>

		    <bx:set keys = "">
		    <bx:loop collection="#oXMLData.root#" item="key">
		    	<bx:set keys &= key >
		    </bx:loop>

		    <bx:script>
		    	keyList = structKeyList( oXMLData.root )
		    	keyArray = structKeyArray( oXMLData.root ).toList()
		    	subExists = structKeyExists( oXMLData.root, "sub" )
		    	XMLNameExists = structKeyExists( oXMLData.root, "XMLName" )
		    	XMLAttributesExists = structKeyExists( oXMLData.root, "XMLAttributes" )
		    </bx:script>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( Key.of( "keys" ) ) ).isEqualTo( "sub" );
		assertThat( variables.getAsString( Key.of( "keyList" ) ) ).isEqualTo( "sub" );
		assertThat( variables.getAsString( Key.of( "keyArray" ) ) ).isEqualTo( "sub" );
		assertThat( variables.getAsBoolean( Key.of( "subExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "XMLNameExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "XMLAttributesExists" ) ) ).isTrue();
	}

	@Test
	public void testLoopOverClass() {
		instance.executeSource(
		    """
		    o = new src.test.java.ortus.boxlang.runtime.components.system.LoopMeAsCollection();
		    result = [];
		    bx:loop collection="#o#" item="key" {
		    	result.append( key );
		    }
		         """,
		    context );
		assertThat( variables.getAsArray( Key.of( "result" ) ) ).contains( "foo" );
		assertThat( variables.getAsArray( Key.of( "result" ) ) ).contains( "baz" );
	}

	@Test
	public void testUnscopeOutputAfterGroup() {
		instance.executeSource(
		    """
		        	myQry = queryNew("group1,group2,value", "string,string,integer", [
		        		{group1:"A", group2:"X", value: 10},
		        		{group1:"A", group2:"X", value: 20},
		        		{group1:"A", group2:"Y", value: 30},
		        		{group1:"B", group2:"Z", value: 40},
		        		{group1:"B", group2:"Z", value: 50}
		        	])
		        	savecontent variable="result" {
		    	cfloop(query=myQry, group="group1") {
		    		writeOutput("group1: #group1#");
		    		cfloop(group="group2") {
		    			writeOutput("group2: #group2#");
		    		}
		    		writeOutput("group1 (again): #group1#"); // fails
		    		writeOutput("group1 (again): #myQry.group1#"); // works
		    	}
		    }
		        """,
		    context, BoxSourceType.CFSCRIPT );
		String resultText = variables.getAsString( result ).replaceAll( "\\s+", "" );
		assertThat( resultText ).isEqualTo( "group1:Agroup2:Xgroup2:Ygroup1(again):Agroup1(again):Agroup1:Bgroup2:Zgroup1(again):Bgroup1(again):B" );
	}

	@Test
	public void testUnscopeOutputAfterLoop() {
		instance.executeSource(
		    """
		    myQry = queryNew("value", "integer", [
		    	{value: 10},
		    	{value: 20}
		    ])
		    savecontent variable="result" {
		    	cfloop(query=myQry) {
		    		writeOutput("-outer-#value#");
		    		cfloop(query=myQry) {
		    			writeOutput("-inner-#value#");
		    		}
		    		writeOutput("-outer-again-#value#");
		    	}
		    }
		           """,
		    context, BoxSourceType.CFSCRIPT );
		String resultText = variables.getAsString( result ).replaceAll( "\\s+", "" );
		assertThat( resultText ).isEqualTo( "-outer-10-inner-10-inner-20-outer-again-10-outer-20-inner-10-inner-20-outer-again-20" );
	}

}
