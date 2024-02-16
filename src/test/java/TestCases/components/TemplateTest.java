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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;

public class TemplateTest {

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

	@Test
	public void testSetComponent() {
		instance.executeSource(
		    """
		       <cfset result = "bar">
		    """, context, BoxScriptType.CFMARKUP );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
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

		                    """, context, BoxScriptType.CFMARKUP );

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

		                    """, context, BoxScriptType.CFMARKUP );

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

		                    """, context, BoxScriptType.CFMARKUP );

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

		                    """, context, BoxScriptType.CFMARKUP );

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

	@DisplayName( "component Island" )
	@Test
	public void testComponentIsland() {
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
		                      """, context, BoxScriptType.CFSCRIPT );

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
		                           """, context, BoxScriptType.CFMARKUP );

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
		                           """, context, BoxScriptType.CFMARKUP );

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
		                         """, context, BoxScriptType.CFMARKUP );

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
		                           """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "trycatch" );

	}

	@Test
	public void testTryCatchNoStatements() {
		instance.executeSource(
		    """
		    <cftry><cfcatch></cfcatch><cffinally></cffinally></cftry>
		                        """, context, BoxScriptType.CFMARKUP );
		// Just make sure it parses without error
	}

	@DisplayName( "component function" )
	@Test
	public void testFunction() {
		instance.executeSource(
		    """
		          <cffunction name="foo" returntype="string">
		          	<cfargument name="bar" type="string" required="true">
		       	<cfreturn bar & "baz">
		       </cffunction>
		    <cfset result = foo("bar")>
		                                 """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "barbaz" );
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
		                                   """, context, BoxScriptType.CFMARKUP );

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
		                                      """, context, BoxScriptType.CFMARKUP );

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
		                                      """, context, BoxScriptType.CFMARKUP );

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
		                                          """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "component include" )
	@Test
	public void testInclude() {
		instance.executeSource(
		    """
		    <cfinclude template="src/test/java/TestCases/components/MyInclude.cfm">
		                                    """, context, BoxScriptType.CFMARKUP );

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
		                                       """, context, BoxScriptType.CFMARKUP ) );

		assertThat( e.getMessage() ).contains( "zero" );
	}

	@Test
	public void testThrow() {
		assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <cfthrow>
		    """, context, BoxScriptType.CFMARKUP ) );
	}

	@Test
	public void testThrowMessage() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <cfthrow message="my message">
		    								""", context, BoxScriptType.CFMARKUP ) );

		assertThat( e.getMessage() ).isEqualTo( "my message" );
	}

	@Test
	public void testThrowObject() {
		Throwable e = assertThrows( MissingIncludeException.class, () -> instance.executeSource(
		    """
		    <cfthrow object="#new java:ortus.boxlang.runtime.types.exceptions.MissingIncludeException( "include message", "file.cfm" )#">
		    								""", context, BoxScriptType.CFMARKUP ) );

		assertThat( e.getMessage() ).isEqualTo( "include message" );
	}

	@Test
	public void testThrowMessageObject() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <cfthrow message="my wrapper exception" object="#new java:ortus.boxlang.runtime.types.exceptions.MissingIncludeException( "include message", "file.cfm" )#">
		    """,
		    context, BoxScriptType.CFMARKUP ) );

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
		    context, BoxScriptType.CFMARKUP ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.detail ).isEqualTo( "my detail" );
		assertThat( ce.errorCode ).isEqualTo( "42" );
		assertThat( ce.extendedInfo ).isInstanceOf( Array.class );
		assertThat( ce.type ).isEqualTo( "my.type" );

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
		                                                """, context, BoxScriptType.CFMARKUP ) );

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
		                                                   """, context, BoxScriptType.CFMARKUP ) );

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
		                                             """, context, BoxScriptType.CFMARKUP );

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
		                                                """, context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "You don't have any vegetables!" );
	}

	@Test
	public void testSwitchEmpty() {
		instance.executeSource(
		    """
		    <cfswitch expression="vegetable"></cfswitch>
		                                         """, context, BoxScriptType.CFMARKUP );
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
		    										""", context, BoxScriptType.CFMARKUP );

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
		    										""", context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );

	}

	@Test
	public void testClass() {
		instance.executeSource(
		    """
		    <cfset result = new src.test.java.TestCases.components.MyClass()>
		    """, context, BoxScriptType.CFMARKUP );

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
		    context, BoxScriptType.CFMARKUP ) );
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
		    context, BoxScriptType.CFSCRIPT );
	}

	@Test
	public void testNonExistentcComponentsInScript() {
		Throwable e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    brad {
		    }
		          """,
		    context, BoxScriptType.CFSCRIPT ) );
		assertThat( e.getMessage() ).contains( "[brad] was not located" );
	}

	@Test
	public void testUDFInJava() {

		Function func = new Lambda() {

			public Object _invoke( FunctionBoxContext context ) {
				return context.getScopeNearby( ArgumentsScope.name ).dereference( context, Key.of( "param1" ), false );
			}

			@Override
			public List<ImportDefinition> getImports() {
				return null;
			}

			@Override
			public Key getName() {
				return Key.of( "myFunc" );
			}

			@Override
			public Argument[] getArguments() {
				return new Argument[] { new Argument( false, "string", Key.of( "param1" ) ) };
			}

			@Override
			public String getReturnType() {
				return "any";
			}

			@Override
			public IStruct getAnnotations() {
				return Struct.EMPTY;
			}

			@Override
			public IStruct getDocumentation() {
				return Struct.EMPTY;
			}

			@Override
			public Access getAccess() {
				return Access.PUBLIC;
			}

			@Override
			public long getRunnableCompileVersion() {
				return 0;
			}

			@Override
			public LocalDateTime getRunnableCompiledOn() {
				return null;
			}

			@Override
			public Object getRunnableAST() {
				return null;
			}
		};

		variables.put( Key.of( "myFunc" ), func );

		instance.executeSource(
		    """
		    result = myFunc( "brad" );
		    println( result )
		       """,
		    context, BoxScriptType.CFSCRIPT );

	}

}
