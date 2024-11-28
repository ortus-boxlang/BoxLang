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

public class BoxTemplateTest {

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
		       <bx:set result = "bar">
		    """, context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@Test
	public void testSetComponentUnquotedExpression() {
		instance.getConfiguration().customTagsDirectory.add( "src/test/java/TestCases/components" );
		instance.executeSource(
		    """
		       <bx:set foo = "bar">
		       <bx:_echoTag result = #foo#>
		       <bx:_echoTag result2 = #foo&"brad"#>
		    """, context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "barbrad" );
	}

	@Test
	public void testIfStatementElse() {
		instance.executeSource(
		    """
		    <bx:if false >
		       	<bx:set result = "then block">
		    <bx:else>
		    	<bx:set result = "else block">
		    </bx:if>

		                    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "else block" );
	}

	@Test
	public void testIfStatementThen() {
		instance.executeSource(
		    """
		       <bx:if true >
		       	<bx:set result = "then block">
		       <bx:elseif false >
		       	<bx:set result = "first elseif block">
		       <bx:elseif false >
		       	<bx:set result = "second elseif block">
		    <bx:else>
		    	<bx:set result = "else block">
		    	</bx:if>

		                    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "then block" );
	}

	@Test
	public void testIfStatementFirst() {
		instance.executeSource(
		    """
		       <bx:if false >
		       	<bx:set result = "then block">
		       <bx:elseif true >
		       	<bx:set result = "first elseif block">
		       <bx:elseif false >
		       	<bx:set result = "second	elseif block">
		    <bx:else>
		    	<bx:set result = "else block">
		    	</bx:if>

		                    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "first elseif block" );
	}

	@Test
	public void testIfStatementSecond() {
		instance.executeSource(
		    """
		       <bx:if false >
		       	<bx:set result = "then block">
		       <bx:elseif false >
		       	<bx:set result = "first elseif block">
		       <bx:elseif true >
		       	<bx:set result = "second elseif block">
		    <bx:else>
		    	<bx:set result = "else block">
		    	</bx:if>

		                    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "second elseif block" );
	}

	@Test
	public void testIfStatementElse2() {
		instance.executeSource(
		    """
		       <bx:if false >
		       	<bx:set result = "then block">
		       <bx:elseif false >
		       	<bx:set result = "first elseif block">
		       <bx:elseif false >
		       	<bx:set result = "second elseif block">
		    <bx:else>
		    	<bx:set result = "else block">
		    	</bx:if>

		                    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "else block" );

	}

	@DisplayName( "script Island" )
	@Test
	public void testScriptIsland() {
		instance.executeSource(
		    """
		            pre script
		         <bx:set result = "it didn't work">
		      <bx:script>
		      	i=0
		    i++
		    if ( i == 1 ) {
		    	result = 'it worked'
		    }
		      </bx:script>
		         post script
		                 """, context, BoxSourceType.BOXTEMPLATE );

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

	@DisplayName( "component Island output order" )
	@Test
	public void testComponentIslandBufferOrder() {
		instance.executeSource(
		    """
		      setting enableOutputOnly=true;
		        	echo( "I am a script" )

		        	```
		        	<!--- Now I can do templating --->
		        	<bx:output>hello</bx:output>
		        	```

		        	// Now I am back in scripts
		        	echo( "scripts again" )
		    result = getBoxContext().getBuffer().toString()
		        """, context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "I am a scripthelloscripts again" );
	}

	@DisplayName( "component script Island inception" )
	@Test
	@Disabled( "This can't work without re-working the lexers to 'count' the island blocks." )
	public void testComponentScriptIslandInception() {
		instance.executeSource(
		    """
		       result = "one"
		    ```
		    	<bx:set result &= " two">
		    	<bx:script>
		    		result &= " three";
		    		```
		    			<bx:set result &= " four">
		    		```
		    		result &= " five"
		    	</bx:script>
		    	<bx:set result &= " six">
		    ```
		    result &= " seven"
		                      """, context, BoxSourceType.BOXSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "one two three four five six seven" );
	}

	@Test
	public void testTryCatch1() {
		instance.executeSource(
		    """
		    <bx:set result = "one">
		       <bx:try>
		       	<bx:set 1/0>
		       	<bx:catch type="any">
		       		<bx:set result = "two">
		       	</bx:catch>
		       	<bx:catch type="foo.bar">
		    		<bx:set result = "three">
		       	</bx:catch>
		       	<bx:finally>
		    		<bx:set result &= "finally">
		       	</bx:finally>
		       </bx:try>
		                           """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "twofinally" );

	}

	@Test
	public void testTryCatch2() {
		instance.executeSource(
		    """
		    <bx:set result = "one">
		       <bx:try>
		       	<bx:set 1/0>
		       	<bx:catch type="foo.bar">
		       		<bx:set result = "two">
		       	</bx:catch>
		       	<bx:catch type="any">
		    		<bx:set result = "three">
		       	</bx:catch>
		       	<bx:finally>
		    		<bx:set result &= "finally">
		       	</bx:finally>
		       </bx:try>
		                           """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "threefinally" );

	}

	@Test
	public void testTryCatchFinally() {
		instance.executeSource(
		    """
		     <bx:try>
		       <bx:set result = "try">
		     	<bx:finally>
		    <bx:set result &= "finally">
		     	</bx:finally>
		     </bx:try>
		                         """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "tryfinally" );

	}

	@Test
	public void testTryCatchNoFinally() {
		instance.executeSource(
		    """
		    <bx:set result = "try">
		       <bx:try>
		    <bx:set 1/0>
		      <bx:catch>
		      <bx:set result &= "catch">
		      </bx:catch>
		       </bx:try>
		                           """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "trycatch" );

	}

	@Test
	public void testTryCatchNoStatements() {
		instance.executeSource(
		    """
		    <bx:try><bx:catch></bx:catch><bx:finally></bx:finally></bx:try>
		                        """, context, BoxSourceType.BOXTEMPLATE );
		// Just make sure it parses without error
	}

	@Test
	public void testTryEmptyCatch() {
		instance.executeSource(
		    """
		       <bx:try>
		    	<bx:catch />
		    </bx:try>
		                           """, context, BoxSourceType.BOXTEMPLATE );
		// Just make sure it parses without error
	}

	@Test
	public void testTryEmptyCatchWithType() {
		instance.executeSource(
		    """
		       <bx:try>
		    	<bx:catch type="foo" />
		    </bx:try>
		                           """, context, BoxSourceType.BOXTEMPLATE );
		// Just make sure it parses without error
	}

	@DisplayName( "component function" )
	@Test
	public void testFunction() {
		// @formatter:off
		instance.executeSource(
		    """
				<bx:function name="foo" returntype="string" intent:depricated="true">
					<bx:argument name="bar" type="string" required="true">
					<bx:return bar & "baz">
				</bx:function>
				<bx:set result = foo("bar")>
				<bx:set md = foo.$bx.meta>
			""", context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "barbaz" );
		assertThat(
		    variables.getAsStruct( Key.of( "md" ) )
		        .getAsStruct( Key.annotations )
		        .getAsString( Key.of( "intent:depricated" ) )
		).isEqualTo( "true" );
	}

	@DisplayName( "component import" )
	@Test
	public void testImport() {
		instance.executeSource(
		    """
		    <bx:import prefix="java" name="java.lang.String">
		    <bx:import prefix="java" name="java.lang.String" alias="BString">

		    <bx:set result = new String("foo")>
		    <bx:set result2 = new BString("bar")>
		                                   """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( DynamicObject.unWrap( variables.get( result ) ) ).isEqualTo( "foo" );
		assertThat( DynamicObject.unWrap( variables.get( Key.of( "result2" ) ) ) ).isEqualTo( "bar" );
	}

	@DisplayName( "component while" )
	@Test
	public void testWhile() {
		instance.executeSource(
		    """
		     	<bx:set counter = 0>
		    <bx:while condition="counter < 10">
		    	<bx:set counter++>
		    </bx:while>
		    <bx:set result = counter>
		                                      """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( 10 );
	}

	@DisplayName( "component break" )
	@Test
	public void testBreak() {
		instance.executeSource(
		    """
		     	<bx:set counter = 0>
		    <bx:while condition="counter < 10">
		    	<bx:set counter++>
		    <bx:break>
		    </bx:while>
		    <bx:set result = counter>
		                                      """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "component continue" )
	@Test
	public void testContinue() {
		instance.executeSource(
		    """
		         	<bx:set counter = 0>
		      <bx:set result = 0>
		        <bx:while condition="counter < 10">
		        	<bx:set counter++>
		    <bx:continue>
		    <bx:set result++>
		        <bx:break>
		        </bx:while>
		                                          """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "component include" )
	@Test
	public void testInclude() {
		instance.executeSource(
		    """
		    <bx:include template="src/test/java/TestCases/components/MyInclude.cfm">
		                                    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "was included" );
	}

	@DisplayName( "component include attribute collection" )
	@Test
	public void testIncludeAttributeCollection() {
		instance.executeSource(
		    """
		    <bx:set attrs = { template : "src/test/java/TestCases/components/MyInclude.cfm" }>
		       <bx:include attributeCollection="#attrs#" >
		                                       """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "was included" );
	}

	@DisplayName( "component rethrow" )
	@Test
	public void testRethrow() {

		Throwable e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		       <bx:try>
		    	<bx:set 1/0>
		    	<bx:catch>
		    		<bx:rethrow>
		    	</bx:catch>
		    </bx:try>
		                                       """, context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "zero" );
	}

	@Test
	public void testImportTag() {

		instance.executeSource(
		    """
		    <bx:if true></bx:if>
		    <bx:import prefix="vw" taglib="views/">
		    <bx:if true></bx:if>
		    	""", context, BoxSourceType.BOXTEMPLATE );

	}

	@Test
	public void testThrow() {
		assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <bx:throw>
		    """, context, BoxSourceType.BOXTEMPLATE ) );
	}

	@Test
	public void testThrowMessage() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <bx:throw message="my message">
		    								""", context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "my message" );
	}

	@Test
	public void testThrowObject() {
		Throwable e = assertThrows( MissingIncludeException.class, () -> instance.executeSource(
		    """
		    <bx:throw object="#new java:ortus.boxlang.runtime.types.exceptions.MissingIncludeException( "include message", "file.cfm" )#">
		    								""", context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "include message" );
	}

	@Test
	public void testThrowMessageObject() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <bx:throw message="my wrapper exception" object="#new java:ortus.boxlang.runtime.types.exceptions.MissingIncludeException( "include message", "file.cfm" )#">
		    """,
		    context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "my wrapper exception" );
		assertThat( e.getCause() ).isNotNull();
		assertThat( e.getCause().getClass() ).isEqualTo( MissingIncludeException.class );
	}

	@Test
	public void testThrowEverythingBagel() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		        <bx:throw message="my message" detail="my detail" errorCode="42" extendedInfo="#[1,2,3,'brad']#" type="my.type" >
		    """,
		    context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.detail ).isEqualTo( "my detail" );
		assertThat( ce.errorCode ).isEqualTo( "42" );
		assertThat( ce.extendedInfo ).isInstanceOf( Array.class );
		assertThat( ce.type ).isEqualTo( "my.type" );

	}

	@Test
	public void testThrowAttributeCollection() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <bx:set attrs = {
		    	message : "my message",
		    	detail : "my detail",
		    	errorCode : "42",
		    	extendedInfo : "#[1,2,3,'brad']#",
		    	type : "my.type"
		    }>
		           <bx:throw attributeCollection="#attrs#" >
		       """,
		    context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.detail ).isEqualTo( "my detail" );
		assertThat( ce.errorCode ).isEqualTo( "42" );
		assertThat( ce.extendedInfo ).isInstanceOf( Array.class );
		assertThat( ce.type ).isEqualTo( "my.type" );
	}

	@Test
	public void testThrowingAnObjectViaAttributecollection() {
		CustomException ce = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    <bx:try>
		    	<bx:throw type="custom" message="my message" detail="my detail">
		    	<bx:catch>
		    		<bx:set myException = bxcatch>
		    	</bx:catch>
		    </bx:try>

		    <bx:set attrs = {object = myException}>
		    <bx:throw attributecollection="#attrs#">
		          """,
		    context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( ce.getMessage() ).isEqualTo( "my message" );
		assertThat( ce.getCause() ).isNull();
		assertThat( ce.detail ).isEqualTo( "my detail" );
		assertThat( ce.type ).isEqualTo( "custom" );
	}

	@Test
	public void testSwitchMultipleDefault() {

		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		        <bx:set result ="">
		           <bx:set vegetable = "carrot" />
		           <bx:switch expression="#vegetable#">
		       <bx:case value="carrot">
		       	<bx:set result ="Carrots are orange.">
		       </bx:case>
		    <bx:set foo = "bar">
		       <bx:defaultcase>
		       	<bx:set result ="You don't have any vegetables!">
		       </bx:defaultcase>
		           </bx:switch>
		                                                """, context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "case" );
	}

	@Test
	public void testSwitchNonCaseStatements() {
		Throwable e = assertThrows( ExpressionException.class, () -> instance.executeSource(
		    """
		           <bx:set result ="">
		              <bx:set vegetable = "carrot" />
		              <bx:switch expression="#vegetable#">
		          <bx:case value="carrot">
		          	<bx:set result ="Carrots are orange.">
		          </bx:case>
		    <bx:defaultcase>
		    	<bx:set result ="You don't have any vegetables!">
		    </bx:defaultcase>
		    <bx:defaultcase>
		    	<bx:set result ="You don't have any vegetables!">
		    </bx:defaultcase>
		              </bx:switch>
		                                                   """, context, BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "default" );
	}

	@Test
	public void testSwitchMatchCase() {
		instance.executeSource(
		    """
		     <bx:set result ="">
		        <bx:set vegetable = "carrot" />
		        <bx:switch expression="#vegetable#">
		    <bx:case value="carrot">
		    	<bx:set result ="Carrots are orange.">
		    </bx:case>
		    <bx:defaultcase>
		    	<bx:set result ="You don't have any vegetables!">
		    </bx:defaultcase>
		        </bx:switch>
		                                             """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );
	}

	@Test
	public void testSwitchMatchDefault() {
		instance.executeSource(
		    """
		    	<bx:output>
		        <bx:set result ="">
		           <bx:set vegetable = "sdf" />
		           <bx:switch expression="#vegetable#">
		    	sdfsdf
		       <bx:case value="carrot">
		       	<bx:set result ="Carrots are orange.">
		       </bx:case>
		    sfdsdf#sdfsdf#dfdsf
		       <bx:defaultcase>
		       	<bx:set result ="You don't have any vegetables!">
		       </bx:defaultcase>
		    sfddsf
		           </bx:switch>
		    	</bx:output>
		                                                """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "You don't have any vegetables!" );
	}

	@Test
	public void testSwitchEmpty() {
		instance.executeSource(
		    """
		    <bx:switch expression="vegetable"></bx:switch>
		                                         """, context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testSwitchList() {
		instance.executeSource(
		    """
		    <bx:set result ="">
		    	<bx:set vegetable = "bugsBunnySnack" />
		    	<bx:switch expression="#vegetable#">
		    <bx:case value="carrot,bugsBunnySnack">
		    	<bx:set result ="Carrots are orange.">
		    </bx:case>
		    <bx:defaultcase>
		    	<bx:set result ="You don't have any vegetables!">
		    </bx:defaultcase>
		    	</bx:switch>
		    										""", context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );
	}

	@Test
	public void testSwitchListDelimiter() {
		instance.executeSource(
		    """
		    <bx:set result ="">
		    	<bx:set vegetable = "bugsBunnySnack" />
		    	<bx:switch expression="#vegetable#">
		    <bx:case value="carrot:bugsBunnySnack" delimiter=":">
		    	<bx:set result ="Carrots are orange.">
		    </bx:case>
		    <bx:defaultcase>
		    	<bx:set result ="You don't have any vegetables!">
		    </bx:defaultcase>
		    	</bx:switch>
		    										""", context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "Carrots are orange." );

	}

	@Test
	public void testClass() {
		instance.executeSource(
		    """
		    <bx:set result = new src.test.java.TestCases.components.MyClass()>
		    """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isInstanceOf( IClassRunnable.class );

	}

	@Test
	public void testGenericComponentsDanglingEnd() {
		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    	<bx:brad outer=true foo="bar">
		          <bx:sdf attr="value" />
		          <bx:brad inner=false foo="bar">
		          	test
		        <bx:set foo = "bar">
		        again
		          </bx:brad>
		       trailing
		    </bx:brad>
		    </bx:brad>
		               """,
		    context, BoxSourceType.BOXTEMPLATE ) );
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

		                  """,
		    context, BoxSourceType.BOXSCRIPT );
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
		    <bx:function name="foo">
		    	<bx:output>
		    		<bx:savecontent variable="dummy">
		    			<bx:return "bar">
		    		</bx:savecontent>
		    	</bx:output>
		    </bx:function>
		    <bx:set result = foo()>
		                                                      """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@Test
	public void testBreakInComponentBody() {
		instance.executeSource(
		    """
		    <bx:set result = "">
		         <bx:set myArr = [1,2,3,4]>
		            <bx:loop array="#myArr#" item="i">
		            	<bx:output>
		            		<bx:savecontent variable="dummy">
		    			 	<bx:set result &= i>
		            			<bx:break>
		            		</bx:savecontent>
		            	</bx:output>
		       </bx:loop>
		       """, context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "1" );
	}

	@Test
	public void testContinueInComponentBody() {
		instance.executeSource(
		    """
		    <bx:set result = "">
		        <bx:set myArr = [1,2,3,4]>
		           <bx:loop array="#myArr#" item="i">
		           	<bx:output>
		           		<bx:savecontent variable="dummy">
		    				 <bx:set result &= i>
		           			<bx:continue>
		           		</bx:savecontent>
		           	</bx:output>
		      </bx:loop>
		      """, context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "1234" );
	}

	@Test
	public void testLoopCondition() {
		instance.executeSource(
		    """
		      <bx:set result = "">
		    <bx:set counter=0>
		    		 <bx:loop condition="counter LT 5">
		    			 <bx:set counter++>
		    	<bx:set result &= counter>
		    	</bx:loop>
		    	""", context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "12345" );
	}

	@Test
	public void testLoopConditionExpr() {
		instance.executeSource(
		    """
		      <bx:set result = "">
		    <bx:set counter=0>
		    		 <bx:loop condition="#counter LT 5#">
		    			 <bx:set counter++>
		    	<bx:set result &= counter>
		    	</bx:loop>
		    	""", context, BoxSourceType.BOXTEMPLATE );
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
		    	""", context, BoxSourceType.BOXSCRIPT );
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
		    	""", context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "12345" );
	}

	@Test
	public void testNestedComments() {
		instance.executeSource(
		    """
		    <bx:set fruit = "">
		    <bx:switch expression="#fruit#">
		    	<bx:case value="Apple">I like apples!</bx:case>
		    	<bx:case value="Orange,Citrus">I like oranges!</bx:case>
		    	<!---
		    		<bx:case value="Kiwi">
		    			<!--- nested comment --->
		    			I like kiwi!
		    		</bx:case>
		    	--->
		    	<bx:defaultcase>Fruit, what fruit?</bx:defaultcase>
		    </bx:switch>
		      """, context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testReturns() {
		// Only the first one actually returns, but I just want to ensure they compile
		instance.executeSource(
		    """
		    <bx:return>
		    <bx:return />
		    <bx:return expression>
		    <bx:return expression />
		    <bx:return 10/5 >
		    <bx:return 20 / 7 />
		       """, context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testSelfClosingElse() {
		instance.executeSource(
		    """
		       <bx:if true>
		    <bx:else />
		       </bx:if>
		    		  """, context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testAttributeKeywords() {
		instance.executeSource(
		    """
		    	<bx:function name="renderPerPage" cf component interface function argument return if else elseif set try catch finally import while break continue include property rethrow throw switch case defaultcase >
		    	</bx:function>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testImportInScriptIsland() {
		instance.executeSource(
		    """
		    <bx:script>
		    	import src.test.java.TestCases.components.MyClass

		    	new myClass();

		    	if( true ) {
		    		import src.test.java.TestCases.phase3.PropertyTest;
		    	}
		    	new PropertyTest();
		    </bx:script>
		    	  """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testComments() {
		instance.executeSource(
		    """
		    <!--- test comment --->
		    <bx:set foo = "bar">
		    <!--- test comment 2 --->
		      """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testNestedComments2() {
		instance.executeSource(
		    """
		    <!---a<!---b--->c--->
		      """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testTagCommentsInTags() {
		instance.executeSource(
		    """
		    <bx:while condition="false">
		    	<bx:break <!--- sdfsdf --->>
		    </bx:while>
		    			""", context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testTagCommentsInOutput() {
		instance.executeSource(
		    """
		    <bx:output <!--- query="rc.foo" --->>
		    </bx:output>
		    			""", context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testOutputSpace() {
		instance.executeSource(
		    """
		    <bx:output >
		    </bx:output >
		    			  """, context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testSetWhitespace() {
		instance.executeSource(
		    """
		       <bx:set
		    foo = "bar">
		       <bx:set foo = "bar">
		       <bx:set	foo = "bar">
		    				 """, context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testFlushOrder() {
		instance.executeSource(
		    """
		     	<bx:output>
		     	first
		     	#new src.test.java.TestCases.components.FlushUtils().hello()#
		     	</bx:output>
		    <bx:set result = getBoxContext().getBuffer().toString()>
		     """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "firstsecond" );
	}

	@Test
	public void testClosureInTag() {
		instance.executeSource(
		    """
		    <bx:set udf = () => "yeah">
		    <bx:set result = udf()>
		       """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "yeah" );
	}

	@Test
	public void testLambdaInTag() {
		instance.executeSource(
		    """
		    <bx:set udf = () -> "yeah">
		    <bx:set result = udf()>
		       """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "yeah" );
	}

	@Test
	public void testUnquotedAttributeValues() {
		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo="bar" foo2 = "bar2" brad=wood luis = majano >
		    <bx:set result = variables>

		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo2" ), "bar2" );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "brad" ), "wood" );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "luis" ), "majano" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=bar >
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=bar>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=bar/>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=bar />
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "bar" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo= >
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo= />
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=/>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo />
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo/>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo=800.123.1234>
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "800.123.1234" );

		instance.executeSource(
		    """
		    <bx:module template="src/test/java/TestCases/components/echoTag.cfm" foo= df234v~!@#$%^<[];':"\\{}|/&*()_-=` >
		    <bx:set result = variables>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsStruct( result ) ).containsEntry( Key.of( "foo" ), "df234v~!@#$%^<[];':\"\\{}|/&*()_-=`" );
	}

	@Test
	public void testUnquotedAttributeValuesOutput() {

		instance.executeSource(
		    """
		    <bx:output foo= />
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo />
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo=/>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo/>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo=bar></bx:output>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo=bar ></bx:output>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo=bar/>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:output foo=bar />
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testPoundInOutput() {
		instance.executeSource(
		    """
		       foo##bar
		       <bx:output>baz##bum</bx:output>
		    <bx:set result = getBoxContext().getBuffer().toString()>
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "foo##barbaz#bum" );
	}

	@Test
	public void testQueryInTemplateIsland() {
		try {
			var result = new Parser().parse(
			    """
			    	{
			    	```
			    		<bx:if foo></bx:if>
			    	```
			    }
			         """, BoxSourceType.BOXSCRIPT );
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
		       <bx:if true>=
		    </bx:if>
		    <bx:set output = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( Key.output ).trim() ).isEqualTo( "=" );
	}

	@Test
	public void testLessThanBeforeTag() {
		instance.executeSource(
		    """
		    <<bx:set result = "bar">
		    <bx:set output = getBoxContext().getBuffer().toString()>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
		assertThat( variables.getAsString( Key.output ).trim() ).isEqualTo( "<" );
	}

}
