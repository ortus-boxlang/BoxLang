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

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
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
		instance.getConfiguration().customComponentsDirectory.add( "src/test/java/TestCases/components" );
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
		    <cfset foo = "bar">
		    Test ` output
		    ```
		    if ( i == 1 ) {
		    	result = 'it worked'
		    }
		                   """, context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "it worked" );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
	}

	@DisplayName( "component script Island inception" )
	@Test
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
		                      """, context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "one two three four five six seven" );
	}

	@DisplayName( "component script Island inception 2" )
	@Test
	public void testComponentScriptIslandInception2() {
		instance.executeSource(
		    """
		        <cfset result = "one">
		      <cfscript>
		      	result &= " two";
		    collection = [1]
		    for( foo in collection ) {
		    	```
		    		<cfset result &= " three">
		    	```
		    }
		      	result &= " four"
		      </cfscript>
		      <cfset result &= " five">
		                       """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "one two three four five" );
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
		     <cfset foo = [
		    	type = cfcatch.type,
		    	message = cfcatch.message,
		    	detail = cfcatch.detail
		    ]>
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
		    	<cfset md = foo.$bx.meta>
		    """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "barbaz" );
		assertThat(
		    variables.getAsStruct( Key.of( "md" ) )
		        .getAsStruct( Key.annotations )
		        .getAsString( Key.of( "intent:depricated" ) )
		).isEqualTo( "true" );
		assertThat( variables.getAsStruct( Key.of( "md" ) ).getAsBoolean( Key.output ) ).isTrue();
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

	@DisplayName( "component include attribute collection" )
	@Test
	public void testIncludeAttributeCollection() {
		instance.executeSource(
		    """
		    <cfset attrs = { template : "src/test/java/TestCases/components/MyInclude.cfm" }>
		       <cfinclude attributeCollection="#attrs#" >
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
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getErrorCode() ).isEqualTo( "42" );
		assertThat( ce.getExtendedInfo() ).isInstanceOf( Array.class );
		assertThat( ce.getType() ).isEqualTo( "my.type" );

	}

	@Test
	public void testThrowEverythingBagelACFScript() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        cfthrow( message="my message", detail="my detail", errorCode="42", extendedInfo="#[1,2,3,'brad']#", type="my.type" );
		    """,
		    context, BoxSourceType.CFSCRIPT ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getErrorCode() ).isEqualTo( "42" );
		assertThat( ce.getExtendedInfo() ).isInstanceOf( Array.class );
		assertThat( ce.getType() ).isEqualTo( "my.type" );
	}

	@Test
	public void testThrowAttributeCollection() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <cfset attrs = {
		    	message : "my message",
		    	detail : "my detail",
		    	errorCode : "42",
		    	extendedInfo : "#[1,2,3,'brad']#",
		    	type : "my.type"
		    }>
		           <cfthrow attributeCollection="#attrs#" >
		       """,
		    context, BoxSourceType.CFTEMPLATE ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getErrorCode() ).isEqualTo( "42" );
		assertThat( ce.getExtendedInfo() ).isInstanceOf( Array.class );
		assertThat( ce.getType() ).isEqualTo( "my.type" );
	}

	@Test
	public void testThrowAttributeCollectionACFScript() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    attrs = {
		    	message : "my message",
		    	detail : "my detail",
		    	errorCode : "42",
		    	extendedInfo : "#[1,2,3,'brad']#",
		    	type : "my.type"
		    };
		    cfthrow( attributeCollection="#attrs#" );
		       """,
		    context, BoxSourceType.CFSCRIPT ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getErrorCode() ).isEqualTo( "42" );
		assertThat( ce.getExtendedInfo() ).isInstanceOf( Array.class );
		assertThat( ce.getType() ).isEqualTo( "my.type" );
	}

	@Test
	public void testThrowingAnObjectViaAttributecollection() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <cftry>
		    	<cfthrow type="custom" message="my message" detail="my detail">
		    	<cfcatch>
		    		<cfset myException = cfcatch>
		    	</cfcatch>
		    </cftry>

		    <cfset attrs = {object = myException}>
		    <cfthrow attributecollection="#attrs#">
		          """,
		    context, BoxSourceType.CFTEMPLATE ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getType() ).isEqualTo( "custom" );
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
	public void testSwitchEmptyDefaultCase() {

		instance.executeSource(
		    """
		    <cfset fruit = "">
		    <cfswitch expression="#fruit#">
		    	<cfcase value="Apple">I like apples!</cfcase>
		    	<cfcase value="Orange,Citrus">I like oranges!</cfcase>
		    	<cfdefaultcase />
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

		    cfhttp( url="http://google.com" throwOnTimeout=true ){
		    	foo = "bar";
		    	baz=true;
		    }

		    cfhttp( url="http://google.com" throwOnTimeout=true )
		                     """,
		    context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testGenericComponentsInScriptStartWithCF() {
		instance.executeSource(
		    """
		     function cfProcessor() {}
		    cfProcessor();
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
		    context, BoxSourceType.CFSCRIPT ) );
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
	public void testTagCommentsInOutput() {
		instance.executeSource(
		    """
		    <cfoutput <!--- query="rc.foo" --->>
		    </cfoutput>
		                """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testOutputSpace() {
		instance.executeSource(
		    """
		    <cfoutput >
		    </cfoutput >
		                  """, context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testSetWhitespace() {
		instance.executeSource(
		    """
		       <cfset
		    foo = "bar">
		       <cfset foo = "bar">
		       <cfset	foo = "bar">
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

	@Test
	public void testAttributeKeywords() {
		instance.executeSource(
		    """
		    	<cffunction name="renderPerPage" cf component interface function argument return if else elseif set try catch finally import while break continue include property rethrow throw switch case defaultcase >
		    	</cffunction>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testImportInScriptIsland() {
		instance.executeSource(
		    """
		    <cfscript>
		    	import src.test.java.TestCases.components.MyClass

		    	new myClass();

		    	if( true ) {
		    		import src.test.java.TestCases.phase3.PropertyTestCF;
		    	}
		    	new PropertyTestCF();
		    </cfscript>
		    	  """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testComments() {
		instance.executeSource(
		    """
		    <!--- test comment --->
		    <cfset foo = "bar">
		    <!--- test comment 2 --->
		      """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testNestedComments2() {
		instance.executeSource(
		    """
		    <!---a<!---b--->c--->
		      """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testFlushOrder() {
		instance.executeSource(
		    """
		    	 <cfoutput>
		    	 first
		    	 #new src.test.java.TestCases.components.FlushUtils().hello()#
		    	 </cfoutput>
		    <cfset result = getBoxContext().getBuffer().toString()>
		     """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "firstsecond" );
	}

	@Test
	public void testClosureInTag() {
		instance.executeSource(
		    """
		    <cfset udf = () => "yeah">
		    <cfset result = udf()>
		       """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "yeah" );
	}

	@Test
	public void testTranspileVars() {
		instance.executeSource(
		    """
		    <cfscript>
		    	function handleError( required struct cfcatch ) {
		    		return arguments.cfcatch.message;
		    	}
		       </cfscript>
		       	    <cftry>
		       	    	<cfthrow type="custom" message="my message" detail="my detail">
		       	    	<cfcatch>
		       	    <!--- each of these need transpiled to bxcatch to work --->
		       	    		<cfset myException = cfcatch>
		       	    		<cfset structCount( cfcatch )>
		       	    		<cfset variables.cfcatch>
		       	    		<cfset variables["cfcatch"]>
		       	    		<cfset cfcatch.message>
		       	    		<cfset cfcatch["message"]>
		       	    		<cfset cfcatch.getMessage()>
		       				<cfset handleError( cfcatch ) >
		       	    	</cfcatch>
		       	    </cftry>
		       	      """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	@Disabled( "BL-198 need to support attribute collection for built in constructs" )
	public void testCfthrowAttributeCollection() {
		instance.executeSource(
		    """
		    <cftry>
		    	<cfthrow type="custom" message="my message" detail="my detail">
		    	<cfcatch>
		    		<cfset myException = cfcatch>
		    	</cfcatch>
		    </cftry>

		    <cftry>
		    	<cfset attrs = {object  = myException}>
		    	<cfthrow attributecollection="#attrs#">
		    	<cfcatch>
		    		<cfset result = cfcatch.message >
		    	</cfcatch>
		    </cftry>
		      """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my message" );
	}

	@Test
	public void testUnquotedAttributeValues() {
		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo="bar" foo2 = "bar2" brad=wood luis = majano >
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo2" ), "bar2" );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "brad" ), "wood" );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "luis" ), "majano" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=bar >
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=bar>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=bar/>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=bar />
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo= >
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo= />
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=/>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo />
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo/>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo=800.123.1234>
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "800.123.1234" );

		instance.executeSource(
		    """
		    <cfmodule template="src/test/java/TestCases/components/echoTag.cfm" foo= df234v~!@#$%^<[];':"\\{}|/&*()_-=` >
		    <cfset result = variables>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "df234v~!@#$%^<[];':\"\\{}|/&*()_-=`" );
	}

	@Test
	public void testUnquotedAttributeValuesOutput() {

		instance.executeSource(
		    """
		    <cfoutput foo= />
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo />
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo=/>
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo/>
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo=bar></cfoutput>
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo=bar ></cfoutput>
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo=bar/>
		          """,
		    context, BoxSourceType.CFTEMPLATE );

		instance.executeSource(
		    """
		    <cfoutput foo=bar />
		          """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testInvalidCodeUnclosedExpression() {

		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    <cfoutput query="qry">
		    	<cfif condition>
		    		<cfquery name="foo" datasource="bar">
		    		insert into table (col, col2)
		    			values(#form.
		    		</cfquery>
		    	</cfif>
		    </cfoutput>
		          """,
		    context, BoxSourceType.CFTEMPLATE ) );
		assertThat( e.getMessage() ).contains( "Unexpected end of expression" );

	}

	@Test
	public void testInvalidCodeUnclosedTag() {
		// The cfelse tag is technically "closed", but since the tokens up to and including "<cfelse" don't match any rules, the parser
		// just gives up, matching no rules, and leaving the modes on the stack (and the ">" token unconsumed)
		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    "<cfelse >",
		    context, BoxSourceType.CFTEMPLATE ) );
		assertThat( e.getMessage() ).contains( "Unclosed tag [cfelse]" );
	}

	@Test
	public void testPoundInOutput() {
		instance.executeSource(
		    """
		       foo##bar
		       <cfoutput>baz##bum</cfoutput>
		    <cfset result = getBoxContext().getBuffer().toString()>
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "foo##barbaz#bum" );
	}

	@Test
	public void testTrickyScriptBlocks() {
		instance.executeSource(
		    """
		    <cfscript>
		    	//</cfscript>
		    </cfscript>

		    <cfscript>
		    	/* <cfscript>*/
		    </cfscript>

		    <cfscript>
		    	result = "</cfscript>";
		    </cfscript>
		             """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "</cfscript>" );
	}

	@Test
	public void testCompleteExpressionInTagParens() {
		instance.executeSource(
		    """
		    	<cfset a = 0>
		    	<cfset b = -1>
		    	<cfset result = false>

		    	<cfif ( a > b ) >
		    		<cfset result = true>
		    	</cfif>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( true );

	}

	@Test
	public void testCompleteExpressionInTagBrackets() {
		instance.executeSource(
		    """
		    	<cfset a = 0>
		    	<cfset b = -1>
		    	<cfset result = false>

		    	<cfif !variables[ a > b ? "result" : "a" ] >
		    		<cfset result = true>
		    	</cfif>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( true );

	}

	@Test
	public void testCompleteExpressionInTagBraces() {
		instance.executeSource(
		    """
		    	<cfset a = 0>
		    	<cfset b = -1>
		    	<cfset result = false>

		    	<cfif { val : a > b }.val >
		    		<cfset result = true>
		    	</cfif>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@Test
	public void testCompleteExpressionInTagClosure() {
		instance.executeSource(
		    """
		    	<cfset result = false>

		    	<cfif (()=>true)() >
		    		<cfset result = true>
		    	</cfif>
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@Test
	public void testAttributeUnquotedHashed() {
		instance.executeSource(
		    """
		    <cfset mylist="item1,item2,item3">
		    <cfloop list=#myList# item="thisItem">
		    	<cfoutput>
		    	#thisItem#
		    	</cfoutput>
		    </cfloop>
		      """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testQueryInTemplateIsland() {
		try {
			var result = new Parser().parse(
			    """
			    	{
			    	```
			    		<cfif foo></cfif>
			    	```
			    }
			         """, BoxSourceType.CFSCRIPT );
			if ( !result.isCorrect() ) {
				throw new ParseException( result.getIssues(), "" );
			}
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	@Test
	public void testEqualAFterTag() {
		instance.executeSource(
		    """
		       <cfif true>=
		    </cfif>
		    <cfset output = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( Key.output ).trim() ).isEqualTo( "=" );
	}

	@Test
	public void testLessThanBeforeTag() {
		instance.executeSource(
		    """
		    <<cfset result = "bar">
		    <cfset output = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
		assertThat( variables.getAsString( Key.output ).trim() ).isEqualTo( "<" );
	}

	@Test
	public void testBreakInLoop() {
		instance.executeSource(
		    """
		    <cfloop condition="true">
		    	<cfbreak>
		    </cfloop>
		    <cfset result = "after loop">
		                  """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "after loop" );
	}

	@Test
	public void testTagFunctionAccess() {
		instance.executeSource(
		    """
		    <cffunction name="hello" output="false" access="remote">
		    	<cfreturn "hi">
		    </cffunction>
		    <cfset result = getMetaData( hello ).access>
		                    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "remote" );
	}

	@Test
	public void testQuerySpaceOutput() {
		instance.executeSource(
		    """
		    <cfset newTest = createObject("component", "src.test.java.TestCases.components.QuerySpaceOutput")>
		    <cfdump var="#newTest.getQuery(orderby="lock_name",sortOrder="asc")#" >
		                             """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testEmptyScriptBlock() {
		instance.executeSource(
		    """
		    <cfinclude template="src/test/java/TestCases/components/TestEmptyScriptBlock.cfm">
		      """,
		    context, BoxSourceType.CFTEMPLATE );
	}

}
