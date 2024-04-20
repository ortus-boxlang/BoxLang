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
package TestCases.components;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;

public class CFTemplateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			foo		= new Key( "foo" );

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
	public void testSetComponent() {
		instance.executeSource(
		    """
		       <cfset result = "bar">
		    """, context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@Test
	public void testSetComponentUnquotedExpression() {
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/TestCases/components" );
		instance.executeSource(
		    """
		       <cfset foo = "bar">
		       <cf_echoTag result =#foo#>
		       <cf_echoTag result2 = #foo&"brad"#>
		    """, context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "barbrad" );
	}

	@Test
	public void testIfStatementElse() {
		instance.executeSource(
		    """
		    <cfif false >
		       	<cfset result = "then block">
		    <cfelse>
		    	<cfset result = "else block">
		    </cfif>

		                    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "else block" );
	}

	@Test
	public void testIfStatementThen() {
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

		                    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "then block" );
	}

	@Test
	public void testIfStatementFirst() {
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

		                    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "first elseif block" );
	}

	@Test
	public void testIfStatementSecond() {
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

		                    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "second elseif block" );
	}

	@Test
	public void testIfStatementElse2() {
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

		                    """, context, BoxSourceType.CFTEMPLATE );

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
		                 """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "it worked" );

	}

	@DisplayName( "component Island" )
	@Test
	public void testComponentIsland() {
		instance.executeSource(
		    """
		    i=0
		    i++
		    ```
		    <bx:set foo = "bar">
		    Test outpout
		    ```
		    if ( i == 1 ) {
		    	result = 'it worked'
		    }
		                   """, context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "it worked" );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
	}

	@DisplayName( "component script Island inception" )
	@Test
	@Disabled( "This can't work without re-working the lexers to 'count' the island blocks." )
	public void testComponentScriptIslandInception() {
		instance.executeSource(
		    """
		       result = "one"
		    ```
		    	<cfset result &= " two">
		    	<cfscript>
		    		result &= " three";
		    		```
		    			<cfset result &= " four">
		    		```
		    		result &= " five"
		    	</cfscript>
		    	<cfset result &= " six">
		    ```
		    result &= " seven"
		                      """, context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "one two three four five six seven" );
	}

	@Test
	public void testTryCatch1() {
		instance.executeSource(
		    """
		    <cfset result = "one">
		       <cftry>
		       	<cfset 1/0>
		       	<cfcatch type="any">
		       		<cfset result = "two">
		       	</cfcatch>
		       	<cfcatch type="foo.bar">
		    		<cfset result = "three">
		       	</cfcatch>
		       	<cffinally>
		    		<cfset result &= "finally">
		       	</cffinally>
		       </cftry>
		                           """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "twofinally" );

	}

	@Test
	public void testTryCatch2() {
		instance.executeSource(
		    """
		    <cfset result = "one">
		       <cftry>
		       	<cfset 1/0>
		       	<cfcatch type="foo.bar">
		       		<cfset result = "two">
		       	</cfcatch>
		       	<cfcatch type="any">
		    		<cfset result = "three">
		       	</cfcatch>
		       	<cffinally>
		    		<cfset result &= "finally">
		       	</cffinally>
		       </cftry>
		                           """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "threefinally" );

	}

	@Test
	public void testTryCatchFinally() {
		instance.executeSource(
		    """
		     <cftry>
		       <cfset result = "try">
		     	<cffinally>
		    <cfset result &= "finally">
		     	</cffinally>
		     </cftry>
		                         """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "tryfinally" );

	}

	@Test
	public void testTryCatchNoFinally() {
		instance.executeSource(
		    """
		    <cfset result = "try">
		       <cftry>
		    <cfset 1/0>
		      <cfcatch>
		      <cfset result &= "catch">
		      </cfcatch>
		       </cftry>
		                           """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "trycatch" );

	}

	@Test
	public void testTryCatchNoStatements() {
		instance.executeSource(
		    """
		    <cftry><cfcatch></cfcatch><cffinally></cffinally></cftry>
		                        """, context, BoxSourceType.CFTEMPLATE );
		// Just make sure it parses without error
	}

	@Test
	public void testTryEmptyCatch() {
		instance.executeSource(
		    """
		       <cftry>
		    	<cfcatch />
		    </cftry>
		                           """, context, BoxSourceType.CFTEMPLATE );
		// Just make sure it parses without error
	}

	@Test
	public void testTryEmptyCatchWithType() {
		instance.executeSource(
		    """
		       <cftry>
		    	<cfcatch type="foo" />
		    </cftry>
		                           """, context, BoxSourceType.CFTEMPLATE );
		// Just make sure it parses without error
	}

	@DisplayName( "component function" )
	@Test
	public void testFunction() {
		instance.executeSource(
		    """
		    	<cffunction name="foo" returntype="string" intent:depricated="true">
		    		<cfargument name="bar" type="string" required="true">
		    		<cfreturn bar & "baz">
		    	</cffunction>
		    	<cfset result = foo("bar")>
		    	<cfset md = getMetaData(foo)>
		    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "barbaz" );
		assertThat( variables.getAsStruct( Key.of( "md" ) ).getAsString( Key.of( "intent:depricated" ) ) ).isEqualTo( "true" );
	}

	@DisplayName( "component import" )
	@Test
	public void testImport() {
		instance.executeSource(
		    """
		    <cfimport prefix="java" name="java.lang.String">
		    <cfimport prefix="java" name="java.lang.String" alias="BString">

		    <cfset result = new String("foo")>
		    <cfset result2 = new BString("bar")>
		                                   """, context, BoxSourceType.CFTEMPLATE );

		assertThat( DynamicObject.unWrap( variables.get( result ) ) ).isEqualTo( "foo" );
		assertThat( DynamicObject.unWrap( variables.get( Key.of( "result2" ) ) ) ).isEqualTo( "bar" );
	}

	@DisplayName( "component while" )
	@Test
	public void testWhile() {
		instance.executeSource(
		    """
		     	<cfset counter = 0>
		    <cfwhile condition="counter < 10">
		    	<cfset counter++>
		    </cfwhile>
		    <cfset result = counter>
		                                      """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( 10 );
	}

	@DisplayName( "component break" )
	@Test
	public void testBreak() {
		instance.executeSource(
		    """
		     	<cfset counter = 0>
		    <cfwhile condition="counter < 10">
		    	<cfset counter++>
		    <cfbreak>
		    </cfwhile>
		    <cfset result = counter>
		                                      """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "component continue" )
	@Test
	public void testContinue() {
		instance.executeSource(
		    """
		         	<cfset counter = 0>
		      <cfset result = 0>
		        <cfwhile condition="counter < 10">
		        	<cfset counter++>
		    <cfcontinue>
		    <cfset result++>
		        <cfbreak>
		        </cfwhile>
		                                          """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "component include" )
	@Test
	public void testInclude() {
		instance.executeSource(
		    """
		    <cfinclude template="src/test/java/TestCases/components/MyInclude.cfm">
		                                    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "was included" );
	}

	@DisplayName( "component rethrow" )
	@Test
	public void testRethrow() {

		Throwable e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		       <cftry>
		    	<cfset 1/0>
		    	<cfcatch>
		    		<cfrethrow>
		    	</cfcatch>
		    </cftry>
		                                       """, context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "zero" );
	}

	@Test
	public void testImportTag() {

		instance.executeSource(
		    """
		    <cfif true></cfif>
		       <cfimport prefix="vw" taglib="views/">
		    <cfif true></cfif>
		         """, context, BoxSourceType.CFTEMPLATE );

	}

	@Test
	public void testThrow() {
		assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <cfthrow>
		    """, context, BoxSourceType.CFTEMPLATE ) );
	}

	@Test
	public void testThrowMessage() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <cfthrow message="my message">
		    								""", context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "my message" );
	}

	@Test
	public void testThrowObject() {
		Throwable e = assertThrows( MissingIncludeException.class, () -> instance.executeSource(
		    """
		    <cfthrow object="#new java:ortus.boxlang.runtime.types.exceptions.MissingIncludeException( "include message", "file.cfm" )#">
		    								""", context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "include message" );
	}

	@Test
	public void testThrowMessageObject() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <cfthrow message="my wrapper exception" object="#new java:ortus.boxlang.runtime.types.exceptions.MissingIncludeException( "include message", "file.cfm" )#">
		    """,
		    context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "my wrapper exception" );
		assertThat( e.getCause() ).isNotNull();
		assertThat( e.getCause().getClass() ).isEqualTo( MissingIncludeException.class );
	}

	@Test
	public void testThrowEverythingBagel() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <cfthrow message="my message" detail="my detail" errorCode="42" extendedInfo="#[1,2,3,'brad']#" type="my.type" >
		    """,
		    context, BoxSourceType.CFTEMPLATE ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.detail ).isEqualTo( "my detail" );
		assertThat( ce.errorCode ).isEqualTo( "42" );
		assertThat( ce.extendedInfo ).isInstanceOf( Array.class );
		assertThat( ce.type ).isEqualTo( "my.type" );

	}

	@Test
	public void testSwitchCommented() {

		instance.executeSource(
		    """
		    <cfset fruit = "">
		    <cfswitch expression="#fruit#">
		    	<cfcase value="Apple">I like apples!</cfcase>
		    	<cfcase value="Orange,Citrus">I like oranges!</cfcase>
		    	<!---<cfcase value="Kiwi">I like kiwi!</cfcase>--->
		    	<cfdefaultcase>Fruit, what fruit?</cfdefaultcase>
		    </cfswitch>
		                                                   """, context, BoxSourceType.CFTEMPLATE );

	}

	@Test
	public void testSwitchMultipleDefault() {

		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		        <cfset result ="">
		           <cfset vegetable = "carrot" />
		           <cfswitch expression="#vegetable#">
		       <cfcase value="carrot">
		       	<cfset result ="Carrots are orange.">
		       </cfcase>
		    <cfset foo = "bar">
		       <cfdefaultcase>
		       	<cfset result ="You don't have any vegetables!">
		       </cfdefaultcase>
		           </cfswitch>
		                                                """, context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "case" );
	}

	@Test
	public void testSwitchNonCaseStatements() {
		Throwable e = assertThrows( ExpressionException.class, () -> instance.executeSource(
		    """
		           <cfset result ="">
		              <cfset vegetable = "carrot" />
		              <cfswitch expression="#vegetable#">
		          <cfcase value="carrot">
		          	<cfset result ="Carrots are orange.">
		          </cfcase>
		    <cfdefaultcase>
		    	<cfset result ="You don't have any vegetables!">
		    </cfdefaultcase>
		    <cfdefaultcase>
		    	<cfset result ="You don't have any vegetables!">
		    </cfdefaultcase>
		              </cfswitch>
		                                                   """, context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "default" );
	}

	@Test
	public void testSwitchMatchCase() {
		instance.executeSource(
		    """
		     <cfset result ="">
		        <cfset vegetable = "carrot" />
		        <cfswitch expression="#vegetable#">
		    <cfcase value="carrot">
		    	<cfset result ="Carrots are orange.">
		    </cfcase>
		    <cfdefaultcase>
		    	<cfset result ="You don't have any vegetables!">
		    </cfdefaultcase>
		        </cfswitch>
		                                             """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );
	}

	@Test
	public void testSwitchMatchDefault() {
		instance.executeSource(
		    """
		    	<cfoutput>
		        <cfset result ="">
		           <cfset vegetable = "sdf" />
		           <cfswitch expression="#vegetable#">
		    	sdfsdf
		       <cfcase value="carrot">
		       	<cfset result ="Carrots are orange.">
		       </cfcase>
		    sfdsdf#sdfsdf#dfdsf
		       <cfdefaultcase>
		       	<cfset result ="You don't have any vegetables!">
		       </cfdefaultcase>
		    sfddsf
		           </cfswitch>
		    	</cfoutput>
		                                                """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "You don't have any vegetables!" );
	}

	@Test
	public void testSwitchEmpty() {
		instance.executeSource(
		    """
		    <cfswitch expression="vegetable"></cfswitch>
		                                         """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testSwitchList() {
		instance.executeSource(
		    """
		    <cfset result ="">
		    	<cfset vegetable = "bugsBunnySnack" />
		    	<cfswitch expression="#vegetable#">
		    <cfcase value="carrot,bugsBunnySnack">
		    	<cfset result ="Carrots are orange.">
		    </cfcase>
		    <cfdefaultcase>
		    	<cfset result ="You don't have any vegetables!">
		    </cfdefaultcase>
		    	</cfswitch>
		    										""", context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );
	}

	@Test
	public void testSwitchListDelimiter() {
		instance.executeSource(
		    """
		    <cfset result ="">
		    	<cfset vegetable = "bugsBunnySnack" />
		    	<cfswitch expression="#vegetable#">
		    <cfcase value="carrot:bugsBunnySnack" delimiter=":">
		    	<cfset result ="Carrots are orange.">
		    </cfcase>
		    <cfdefaultcase>
		    	<cfset result ="You don't have any vegetables!">
		    </cfdefaultcase>
		    	</cfswitch>
		    										""", context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );

	}

	@Test
	public void testClass() {
		instance.executeSource(
		    """
		    <cfset result = new src.test.java.TestCases.components.MyClass()>
		    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isInstanceOf( IClassRunnable.class );

	}

	@Test
	public void testGenericComponentsDanglingEnd() {
		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    	<cfbrad outer=true foo="bar">
		          <cfsdf attr="value" />
		          <cfbrad inner=false foo="bar">
		          	test
		        <cfset foo = "bar">
		        again
		          </cfbrad>
		       trailing
		    </cfbrad>
		    </cfbrad>
		               """,
		    context, BoxSourceType.CFTEMPLATE ) );
		assertThat( e.getMessage() ).contains( "end component" );

	}

	@Test
	public void testGenericComponentsInScript() {
		instance.executeSource(
		    """
		    http url="http://google.com" throwOnTimeout=true {
		    	foo = "bar";
		    	baz=true;
		    }

		    http url="http://google.com" throwOnTimeout=true;

		    cfhttp( url="http://google.com",  throwOnTimeout=true ){
		    	foo = "bar";
		    	baz=true;
		    }

		    cfhttp( url="http://google.com",  throwOnTimeout=true )
		                  """,
		    context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testNonExistentcComponentsInScript() {
		Throwable e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    brad {
		    }
		          """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( e.getMessage() ).contains( "[brad] was not located" );
	}

	@Test
	public void testReturnInComponentBody() {
		instance.executeSource(
		    """
		    <cffunction name="foo">
		    	<cfoutput>
		    		<cfsavecontent variable="dummy">
		    			<cfreturn "bar">
		    		</cfsavecontent>
		    	</cfoutput>
		    </cffunction>
		    <cfset result = foo()>
		                                                      """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@Test
	public void testBreakInComponentBody() {
		instance.executeSource(
		    """
		    <cfset result = "">
		         <cfset myArr = [1,2,3,4]>
		            <cfloop array="#myArr#" item="i">
		            	<cfoutput>
		            		<cfsavecontent variable="dummy">
		    			 	<cfset result &= i>
		            			<cfbreak>
		            		</cfsavecontent>
		            	</cfoutput>
		       </cfloop>
		       """, context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "1" );
	}

	@Test
	public void testContinueInComponentBody() {
		instance.executeSource(
		    """
		    <cfset result = "">
		        <cfset myArr = [1,2,3,4]>
		           <cfloop array="#myArr#" item="i">
		           	<cfoutput>
		           		<cfsavecontent variable="dummy">
		    				 <cfset result &= i>
		           			<cfcontinue>
		           		</cfsavecontent>
		           	</cfoutput>
		      </cfloop>
		      """, context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "1234" );
	}

	@Test
	public void testLoopCondition() {
		instance.executeSource(
		    """
		      <cfset result = "">
		    <cfset counter=0>
		             <cfloop condition="counter LT 5">
		             	<cfset counter++>
		    	<cfset result &= counter>
		        </cfloop>
		        """, context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopConditionExpr() {
		instance.executeSource(
		    """
		      <cfset result = "">
		    <cfset counter=0>
		             <cfloop condition="#counter LT 5#">
		             	<cfset counter++>
		    	<cfset result &= counter>
		        </cfloop>
		        """, context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopConditionScript() {
		instance.executeSource(
		    """
		      result = "";
		    counter=0;
		             loop condition="counter LT 5" {
		             	counter++
		    			result &= counter
		        }
		        """, context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopConditionExprScript() {
		instance.executeSource(
		    """
		      result = "";
		    counter=0;
		             loop condition="#counter LT 5#" {
		             	counter++
		    			result &= counter
		        }
		        """, context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "12345" );
	}

	@Test
	public void testExtraTextInFunctionArguments() {
		instance.executeSource(
		    """
		    	<cffunction name="example" output="false">>
		    		<cfargument name="test">foobar
		    		<cfargument name="test2">
		    		sdfsd
		    		<cfreturn "yo">
		    	</cffunction>
		    """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testNestedComments() {
		instance.executeSource(
		    """
		    <cfset fruit = "">
		    <cfswitch expression="#fruit#">
		    	<cfcase value="Apple">I like apples!</cfcase>
		    	<cfcase value="Orange,Citrus">I like oranges!</cfcase>
		    	<!---
		    		<cfcase value="Kiwi">
		    			<!--- nested comment --->
		    			I like kiwi!
		    		</cfcase>
		    	--->
		    	<cfdefaultcase>Fruit, what fruit?</cfdefaultcase>
		    </cfswitch>
		      """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testReturns() {
		// Only the first one actually returns, but I just want to ensure they compile
		instance.executeSource(
		    """
		    <cfreturn>
		    <cfreturn />
		    <cfreturn expression>
		    <cfreturn expression />
		    <cfreturn 10/5 >
		    <cfreturn 20 / 7 />
		       """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testTagCommentsInExpression() {
		instance.executeSource(
		    """
		    <cfset foo = 4>
		       <cfif (foo  <!--- + baz.bum_2 ---> ) LT 5>
		       </cfif>
		              """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testSelfClosingElse() {
		instance.executeSource(
		    """
		       <cfif true>
		    <cfelse />
		       </cfif>
		              """, context, BoxSourceType.CFTEMPLATE );
	}

}
