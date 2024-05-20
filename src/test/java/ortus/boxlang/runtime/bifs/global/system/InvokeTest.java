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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class InvokeTest {

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
	}

	@DisplayName( "It can invoke in current context" )
	@Test
	public void testInvokeCurrentContext() {
		instance.executeSource(
		    """
		    function foo() {
		    	return "bar";
		    }
		       result = invoke( "", "foo", [] );
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in existing Box Class Instance" )
	@Test
	public void testInvokeExistingClass() {
		instance.executeSource(
		    """
		    myClass = new src.test.java.ortus.boxlang.runtime.bifs.global.system.InvokeTest()
		         result = invoke( myClass, "foo", [] );
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in created Box Class Instance" )
	@Test
	public void testInvokeCreatedClass() {
		instance.executeSource(
		    """
		    result = invoke( "src.test.java.ortus.boxlang.runtime.bifs.global.system.InvokeTest", "foo", [] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "arguments as argumentCollection" )
	@Test
	public void testArgumentsAsArgumentCollection() {

		instance.executeSource(
		    """
		     function createArgs( a=1, b=2 ) {
		    	 variables.args = arguments;
		     }
		    		 function meh( x=3 ) {
		    			 variables.result = arguments;
		    		 }
		    createArgs()
		    		 invoke( instance="", methodName="meh", arguments=args );
		    	 """,
		    context );

		assertThat( variables.getAsStruct( result ).get( "a" ) ).isEqualTo( 1 );
		assertThat( variables.getAsStruct( result ).get( "b" ) ).isEqualTo( 2 );
		assertThat( variables.getAsStruct( result ).get( "x" ) ).isEqualTo( 3 );

	}

	@DisplayName( "arguments as argumentCollection2" )
	@Test
	public void testArgumentsAsArgumentCollection2() {

		instance.executeSource(
		    """
		    	function createArgs() {
		    		variables.args = arguments;
		    	}
		    	function meh( a ) {
		    		variables.result = arguments;
		    	}
		    	createArgs('hello world')
		    	invoke( instance="", methodName="meh", arguments=args );
		    """,
		    context );

		assertThat( variables.getAsStruct( result ).get( "a" ) ).isEqualTo( "hello world" );

	}

}
