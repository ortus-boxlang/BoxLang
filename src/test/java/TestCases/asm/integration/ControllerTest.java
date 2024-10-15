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
package TestCases.asm.integration;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ControllerTest {

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
	public void testSwitchInLoopInFunc() {
		instance.executeStatement(
		    """
		         	function a(){
		    	for ( x = 1; x < 5; x++ ) {
		    		switch( x ){
		    		}
		    	}
		    }
		      """,
		    context );
	}

	@Test
	public void testSwitchWithCaseInLoopInFunc() {
		instance.executeStatement(
		    """
		           	function a(){
		      	for ( x = 1; x < 5; x++ ) {
		      		switch( x ){
		      			case "id": {

		      			}
		      		}
		      	}

		    }
		    a()
		        """,
		    context );
	}

	@Test
	public void testInterfaceImplementation() {
		instance.executeStatement(
		    """
		         	impl = new src.test.java.TestCases.asm.integration.Implementor();

		    impl.setName( "test" );

		    result = impl.getName();
		      """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "test" );
	}

	@Test
	public void testTryCatchLabelStack() {

		instance.executeStatement(
		    """
		       	controller = new src.test.java.TestCases.asm.integration.TryCatchLabelStack();
		    """,
		    context );
	}

	@Test
	public void testNestedLFunctionInComponent() {

		instance.executeStatement(
		    """
		    lock name = "what" timeout=300 {
		    	t = function(){
		    		return "test";
		    	}

		    	result = t();
		    }
		      """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "test" );
	}

	@Test
	public void testReturnFromComponentInFunction() {

		instance.executeStatement(
		    """
		      	function a(){
		    	lock name="what" timeout=300 {
		    		return "test";
		    	}
		    }

		    result = a();
		        """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "test" );
	}

	@Test
	public void testContinueInForIndex() {

		instance.executeStatement(
		    """
		              	a = [ "orange", "red", "yellow" ]

		          for( color in a ){

		    switch( color ){
		    	case "orange": result = color; break;
		    	default: continue;
		    }
		          }
		                """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "orange" );
	}

}
