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

public class IfTest {

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

	@DisplayName( "Will run the code inside of an if with a true condition" )
	@Test
	public void testTrueIfCondition() {
		instance.executeStatement(
		    """
		       result = 1;

		    if( true ){
		    	result = 2;
		    }
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "Will not run the code inside of an if with a true condition" )
	@Test
	public void testFalseIfCondition() {
		instance.executeStatement(
		    """
		       result = 1;

		    if( false ){
		    	result = 2;
		    }
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "Will run the code inside of an else with a true condition" )
	@Test
	public void testFalseIfElseCondition() {
		instance.executeStatement(
		    """
		          result = 1;

		       if( false ){
		       	result = 2;
		       }
		    else {
		    	result = 3;
		    }
		              """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "Will not run the code inside of an else with a true condition" )
	@Test
	public void testTrueIfElseCondition() {
		instance.executeStatement(
		    """
		          result = 1;

		       if( true ){
		       	result = 2;
		       }
		    else {
		    	result = 3;
		    }
		              """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "Will execute the first true branch of an if else statement" )
	@Test
	public void testExecuteFirstTrueBranch() {
		instance.executeStatement(
		    """
		          result = 1;

		       if( false ){
		       	result = 2;
		       }
		    else if( true ) {
		    	result = 3;
		    }
		              """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "Will the else if no branchs are true" )
	@Test
	public void testExecuteElseBranch() {
		instance.executeStatement(
		    """
		             result = 1;

		          if( false ){
		          	result = 2;
		          }
		       else if( false ) {
		       	result = 3;
		       }
		    else {
		    	result = 4;
		    }
		                 """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}

	@DisplayName( "Will execute a branch behind a true expression" )
	@Test
	public void testTrueExpressionExpression() {
		instance.executeStatement(
		    """
		       result = 1;

		    if( 2 + 2 > 3 ){
		    	result = 2;
		    }
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "Will not execute a branch behinda a false expression" )
	@Test
	public void testFalseExpression() {
		instance.executeStatement(
		    """
		       result = 1;

		    if( 2 + 2 == 3 ){
		    	result = 2;
		    }
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

}
