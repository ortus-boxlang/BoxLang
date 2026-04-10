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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class ASMTest {

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

	@Test
	public void testIntegerLiteralShouldNotMessUpStack() {
		instance.executeSource(
		    """
		       function doTest(){
		    	if( true ){
		    		if( true ){
		    			1
		    		}
		    	}
		    }
		          """,
		    context );
	}

	@Test
	public void testTernaryShouldNotMessUpStack() {
		instance.executeSource(
		    """
		       function doTest(){
		    	if( true ){
		    		if( true ){
		    			1 ? "a" : "b"
		    		}
		    	}
		    }
		          """,
		    context );
	}

	@Test
	public void testSimpleImport() {
		// @formatter:off
		instance.executeSource(
		    """
		    	import java.lang.System;
		    """, context );
		// @formatter:on
	}

	@Test
	public void testTryCatchAndFunction() {
		// @formatter:off
		instance.executeSource(
		    """
		    	try{

				}
				catch( any e ){
				}
				function doTest(){
				}
		    """, context );
		// @formatter:on
	}

	@Test
	public void testGetInstancePattern() {
		// Test that getInstance methods work correctly for functions and lambdas
		instance.executeSource(
		    """
		    	// Test function getInstance via compilation
		    	function testFunc() {
		    		return "function result";
		    	}

		    	// Test lambda getInstance via compilation
		    	lambda = () => {
		    		return "lambda result";
		    	};

		    	result = testFunc();
		    	lambdaResult = lambda();
		    """, context );

		// Verify the functions execute correctly (indicating getInstance works)
		var	result			= context.getScopeNearby( VariablesScope.name ).get( new Key( "result" ) );
		var	lambdaResult	= context.getScopeNearby( VariablesScope.name ).get( new Key( "lambdaResult" ) );

		assert result.equals( "function result" );
		assert lambdaResult.equals( "lambda result" );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large switch template should compile without recursive splitting" )
	@Test
	public void testLargeSwitchTemplateShouldCompileWithoutRecursiveSplitting() {
		ResolvedFilePath resolvedPath = ResolvedFilePath.of( Path.of( "overflow.cfm" ) );

		assertDoesNotThrow( () -> RunnableLoader.getInstance().getBoxpiler().compileTemplate( resolvedPath ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "very large switch template should compile" )
	@Test
	public void testVeryLargeSwitchTemplateShouldCompile() {
		assertDoesNotThrow( () -> RunnableLoader.getInstance().getBoxpiler().compileScript( buildVeryLargeSwitchTemplate(), BoxSourceType.CFTEMPLATE ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large 99-case switch template should compile" )
	@Test
	public void testLargeNinetyNineCaseSwitchTemplateShouldCompile() {
		assertDoesNotThrow(
		    () -> RunnableLoader.getInstance().getBoxpiler().compileScript( buildLargeNinetyNineCaseSwitchTemplate(), BoxSourceType.CFTEMPLATE ) );
	}

	private String buildVeryLargeSwitchTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );

		for ( int i = 0; i < 1200; i++ ) {
			source.append( "<cfcase value=\"case" ).append( i ).append( "\">\n" );
			source.append( "<cfset result = 'case" ).append( i ).append( "'>\n" );
			source.append( "<cfbreak>\n" );
			source.append( "</cfcase>\n" );
		}

		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );

		return source.toString();
	}

	private String buildLargeNinetyNineCaseSwitchTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );

		for ( int i = 0; i < 99; i++ ) {
			source.append( "<cfcase value=\"case" ).append( i ).append( "\">\n" );
			for ( int j = 0; j < 30; j++ ) {
				source.append( "<cfset result = 'case" ).append( i ).append( "-segment" ).append( j ).append( "'>\n" );
			}
			source.append( "<cfbreak>\n" );
			source.append( "</cfcase>\n" );
		}

		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );

		return source.toString();
	}

}
