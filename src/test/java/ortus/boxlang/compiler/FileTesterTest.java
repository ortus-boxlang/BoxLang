/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

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

public class FileTesterTest {

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

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void testValueListToQueryColumnDataToListDot() {
		instance.executeSource(
		    """
		       result = new src.test.java.ortus.boxlang.compiler.Assertion();
		    // result.startup();
		          """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void testTestbox() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		      function(){
		    /*

		    */
		      	// do nothing
		      }
		              """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void closureArg() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		       a = [ "null" ];

		    a.map( ( item ) => item.replaceNoCase( 'ModuleConfig.cfc', '' ) )
		    .each( ( path ) => {
		    	len(
		    		'testbox' & arguments.path
		    			.replaceNoCase( "", '' )
		    			.reReplace( '[\\/]', '.', 'all' )
		    			.reReplace( '\\.$', '', 'all' )
		    	);
		    } );
		                           """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void testSaveContent() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		    y = []
		       savecontent variable="x" {
		       	if( true ){
		       		loop array="#y#" index = "i" {

		       		}
		       	}
		       }
		                                     """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void testSuperInit() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		    a = new src.test.java.ortus.boxlang.compiler.Child();
		                                      """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void x() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		    a = expand ?: true
		                                      """,
		    context );
	}

	@DisplayName( "Test bddTest" )
	@Test
	public void testBDDTest() {
		instance.executeSource(
		    """
		    result = new src.test.java.ortus.boxlang.compiler.BDDTest();
		       """,
		    context );
	}

	@DisplayName( "Test hoisting" )
	@Test
	public void testHoisting() {
		instance.executeSource(
		    """
		    result = foo();
		    function foo() {
		       	return "bar";
		       }
		       """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void testCompileIssue1() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		    a = new src.test.java.ortus.boxlang.compiler.CompileIssue1();
		                                      """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void zzz() {
		// instance.useJavaBoxpiler();
		instance.executeSource(
		    """
		    a = 0
		           while( a < 5 ){
		       try{


		       }
		       catch( any e ){

		       }
		    	a++;
		        }
		                                            """,
		    context );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void switchBreak() {
		// instance.useJavaBoxpiler();
		// @formatter:off
		instance.executeSource("""
			a = 1;
			while(a < 10) {
				if(true) {
					break;
				}
			}
			""",
		    context );
		// @formatter:on
	}

}
