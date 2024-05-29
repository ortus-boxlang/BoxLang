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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
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
		       invoke method="foo" returnVariable="result";
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in current context CF Tag" )
	@Test
	public void testInvokeCurrentContextCFTag() {
		instance.executeSource(
		    """
		       <cffunction name="foo">
		       	<cfreturn "bar">
		       </cffunction>
		    <cfinvoke method="foo" returnVariable="result">
		          """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in current context BL Tag" )
	@Test
	public void testInvokeCurrentContextBLTag() {
		instance.executeSource(
		    """
		       <bx:function name="foo">
		       	<bx:return "bar">
		       </bx:function>
		    <bx:invoke method="foo" returnVariable="result">
		          """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in current context CF Script" )
	@Test
	public void testInvokeCurrentContextCFScript() {
		instance.executeSource(
		    """
		     function foo() {
		    	 return "bar";
		     }
		    	invoke method="foo" returnVariable="result";
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in current context ACF Script" )
	@Test
	public void testInvokeCurrentContextACFScript() {
		instance.executeSource(
		    """
		     function foo() {
		    	 return "bar";
		     }
		    	cfinvoke( method="foo", returnVariable="result" );
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in existing Box Class Instance" )
	@Test
	public void testInvokeExistingClass() {
		instance.executeSource(
		    """
		    myClass = new src.test.java.ortus.boxlang.runtime.components.system.InvokeTest()
		         invoke class="#myClass#" method="foo" returnVariable="result" ;
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in created Box Class Instance" )
	@Test
	public void testInvokeCreatedClass() {
		instance.executeSource(
		    """
		    	 invoke class="src.test.java.ortus.boxlang.runtime.components.system.InvokeTest" method="foo" returnVariable="result" ;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can transpile component to class in CF" )
	@Test
	public void testTranspile() {
		instance.executeSource(
		    """
		    	 invoke component="src.test.java.ortus.boxlang.runtime.components.system.InvokeTest" method="foo" returnVariable="result" ;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke on a struct" )
	@Test
	public void testInvokeOnAStruct() {
		instance.executeSource(
		    """
		    	myStr = {
		        	foo = function() {
		        		return "bar";
		        	}
		        }
		    	invoke class="#myStr#" method="foo" returnVariable="result" ;
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can invoke in current context with array argumentCollection" )
	@Test
	public void testInvokeCurrentContextArrayArgCollection() {
		instance.executeSource(
		    """
		       <cffunction name="foo">
		    	   <cfreturn arguments[1]>
		       </cffunction>
		    <cfinvoke method="foo" argumentCollection="#[ "brad" ]#"  returnVariable="result">
		    	  """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "brad" );
	}

	@DisplayName( "It can invoke in current context with struct argumentCollection" )
	@Test
	public void testInvokeCurrentContextStructArgCollection() {
		instance.executeSource(
		    """
		       <cffunction name="foo">
		    	   <cfargument name="name">
		    	   <cfreturn arguments.name>
		       </cffunction>
		    <cfinvoke method="foo" argumentCollection="#{ name : "luis" }#" returnVariable="result">
		    	  """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "luis" );
	}

	@DisplayName( "It can invoke in current context with invoke argument" )
	@Test
	public void testInvokeCurrentContextInvokeArgument() {
		instance.executeSource(
		    """
		       <cffunction name="foo">
		    	   <cfargument name="name">
		    	   <cfreturn arguments.name>
		       </cffunction>
		    <cfinvoke method="foo" returnVariable="result">
		    	<cfinvokeArgument name="name" value="jorge" />
		    </cfinvoke>
		    	  """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "jorge" );
	}

	@DisplayName( "It can invoke in current context with invoke argument and struct" )
	@Test
	public void testInvokeCurrentContextInvokeArgumentAndStruct() {
		instance.executeSource(
		    """
		       <cffunction name="foo">
		    	   <cfargument name="name">
		    	   <cfargument name="name2">
		    	   <cfreturn arguments.name & arguments.name2>
		       </cffunction>
		    <cfinvoke method="foo" argumentCollection="#{ name : "luis",  name2 : "brad"  }#"  returnVariable="result">
		    	<cfinvokeArgument name="name2" value="jorge" />
		    </cfinvoke>
		    	  """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "luisjorge" );
	}

	@DisplayName( "It can invoke with invoke argument and array" )
	@Test
	public void testComboArgs() {
		instance.executeSource(
		    """
		       <cffunction name="foo">
		    		<cfreturn arguments>
		       </cffunction>
		    <cfinvoke method="foo" argumentCollection="#[ "luis" ]#"  returnVariable="result">
		    	<cfinvokeArgument name="name" value="jorge" />
		    </cfinvoke>
		    	  """,
		    context, BoxSourceType.CFTEMPLATE );
		System.out.println( variables.asString() );
		assertThat( variables.getAsStruct( result ).get( "name" ) ).isEqualTo( "jorge" );
		assertThat( variables.getAsStruct( result ).get( "1" ) ).isEqualTo( "luis" );
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
		    		 invoke method="meh" argumentCollection="#args#";
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
		    		println(arguments)
		    		variables.result = arguments;
		    	}
		    	createArgs('hello world')
		       		 invoke method="meh" argumentCollection="#args#";
		    """,
		    context );

		assertThat( variables.getAsStruct( result ).get( "a" ) ).isEqualTo( "hello world" );

	}

}
