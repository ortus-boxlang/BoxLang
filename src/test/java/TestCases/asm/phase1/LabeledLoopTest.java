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
package TestCases.asm.phase1;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class LabeledLoopTest {

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
		instance.useASMBoxPiler();
	}

	@AfterEach
	public void teardownEach() {
		instance.useJavaBoxpiler();
	}

	@Test
	public void testSimpleLabeledWhile() {

		instance.executeSource(
		    """
		    result = 0
		    	mylabel : while( true ) {
		    		result ++
		    		break mylabel;
		    		result ++
		    	}
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	public void testSimpleLabeledWhileContinue() {

		instance.executeSource(
		    """
		     result = 0
		     	mylabel : while( true ) {
		     		result ++
		    if( result > 2 ) break;
		     		continue mylabel;
		     		result ++
		     	}
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@Test
	public void testSimpleLabeledWhileTag() {

		instance.executeSource(
		    """
		      <bx:set result = 0>
		      	<bx:while label="mylabel" condition="true">
		    <bx:set result ++>
		      		<bx:break mylabel>
		      		<bx:set result ++>
		      	</bx:while>
		           """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	public void testSimpleLabeledWhileContinueTag() {

		instance.executeSource(
		    """
		      <bx:set result = 0>
		    	  <bx:while label="mylabel" condition="true">
		    <bx:set result ++>
		        	<bx:if result GT 2 >
		    			<bx:break>
		    		</bx:if>
		         		<bx:continue mylabel>
		    		  <bx:set result ++>
		    	  </bx:while>
		    	   """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@Test
	public void testSimpleLabeledDoWhile() {

		instance.executeSource(
		    """
		    result = 0
		    	mylabel : do  {
		    		result ++
		    		break mylabel;
		    		result ++
		    	} while( true )
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	public void testSimpleLabeledForIn() {

		instance.executeSource(
		    """
		    data = [1,2,3]
		       result = 0
		       	mylabel : for( x in data ) {
		       		result ++
		       		break mylabel;
		       		result ++
		       	}
		            """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	public void testSimpleLabeledForIndex() {

		instance.executeSource(
		    """
		    result = 0
		    	mylabel : for( ; ; ) {
		    		result ++
		    		break mylabel;
		    		result ++
		    	}
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	public void testSimpleLabeledLoop() {

		instance.executeSource(
		    """
		    result = 0
		    	loop condition="true" label="mylabel" {
		    		result ++
		    		break mylabel;
		    		result ++
		    	}
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	@Disabled
	public void testSwitchInLabeledWhile() {

		instance.executeSource(
		    """
		    result = 0
		    	mylabel : while( true ) {
		    		result ++
		    		switch( result ) {
		    			case 1:
		    				break;
		    			case 2:
		    				break mylabel;
		    			case 3:
		    				break;
		    		}
		    	}
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@Test
	@Disabled
	public void testNestedLabeledWhiles() {

		instance.executeSource(
		    """
		    result = 0
		    	outer : while( true ) {
		    		result ++
		    		inner : while( true ) {
		    			result ++
		    			break outer;
		    			result ++
		    		}
		    	}
		    	 """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@Test
	@Disabled
	public void testTagLoop() {

		instance.executeSource(
		    """
		    <cfloop index="i" from="1" to="10" label="xyz">
		        <cfoutput>#i#</cfoutput>
		        <br>
		        <cfif i Ge 5>
		            <cfcontinue xyz>
		        </cfif>
		        <cfoutput>#i#->#i#</cfoutput>
		        <br>
		    </cfloop>
		    			 """,
		    context, BoxSourceType.CFTEMPLATE );
	}

}
