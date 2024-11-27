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
package TestCases.asm.control;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

public class SwitchTest {

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
		// instance.useJavaBoxpiler();
	}

	@DisplayName( "Will not execute code that doesn't match a condition" )
	@Test
	public void testWillNotExecuteUnMatchedBranches() {
		instance.executeStatement(
		    """
		       result = 1;

		    switch( "test" ){
		    	case "testx": result = 4;
		    }
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "Will execute code that matches a condition" )
	@Test
	public void testWillExecuteMatchedBranches() {
		instance.executeStatement(
		    """
		       result = 1;

		    switch( "test" ){
		    	case "test": result = 4;
		    }
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}

	@DisplayName( "Will only execute code that matches a condition" )
	@Test
	public void testSkipUnmatchedBranches() {
		instance.executeStatement(
		    """
		         result = 1;

		      switch( "test" ){
		    case 4: result = 3;
		      	case "test": result = 4;
		      }
		             """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}

	@DisplayName( "Will execute every branch after a match" )
	@Test
	public void testFallThrough() {
		instance.executeStatement(
		    """
		           result = 1;

		        switch( "test" ){
		      case 4: result = 3;
		        	case "test": result = 4;
		    case "multiply": result = result  * 2;
		        }
		               """,
		    context );

		assertThat( variables.getAsNumber( result ).doubleValue() ).isEqualTo( 8 );
	}

	@DisplayName( "Will stop executing on a break statement" )
	@Test
	public void testBreak() {
		instance.executeStatement(
		    """
		           result = 1;

		        switch( "test" ){
		      case 4: result = 3;
		        	case "test": result = 4; break;
		    case "multiply": result = result  * 2;
		        }
		               """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}

	@DisplayName( "Will accept values of different types" )
	@Test
	public void testDifferntTypes() {
		instance.executeStatement(
		    """
		         result = 1;

		      switch( "4" ){
		    case 4: result = 3; break;
		      	case "test": result = 4; break;
		      }
		             """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 3 );
		;
	}

	@DisplayName( "Will execute the default case if no conditions match" )
	@Test
	public void testDefault() {
		instance.executeStatement(
		    """
		           result = 1;

		        switch( "x" ){
		      case 4: result = 3; break;
		        	case "test": result = 4; break;
		    default: result = "no match";
		        }
		               """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "no match" );
		;
	}

}
