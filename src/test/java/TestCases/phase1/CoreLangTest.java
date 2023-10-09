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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;

public class CoreLangTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context.getScopeNearby( VariablesScope.name ).clear();
	}

	@DisplayName( "if" )
	@Test
	public void testIf() {

		instance.executeSource(
		    """
		    result = "default"
		    foo = "false"
		    if( 1 ) {
		    	result = "first"
		    } else if( !foo ) {
		    	result = "second"
		    }
		      """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "first" );

	}

	@DisplayName( "if else" )
	@Test
	public void testIfElse() {

		instance.executeSource(
		    """
		    if( false ) {
		    	result = "first"
		    } else {
		    	result = "second"
		    }
		      """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "second" );

	}

	@DisplayName( "if no body" )
	@Test
	public void testIfNoBody() {

		instance.executeSource(
		    """
		    result = "default"

		    if( 1 == 1 )
		    	result = "done"

		      """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "done" );

		instance.executeSource(
		    """
		    result = "default"

		    if( 1 == 2 )
		    	result = "not done"

		      """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "default" );

	}

	@DisplayName( "throw in source" )
	@Test
	public void testThrowSource() {

		assertThrows( NoFieldException.class, () ->

		instance.executeSource(
		    """
		    throw new java:ortus.boxlang.runtime.types.exceptions.NoFieldException( "My Message" );
		        """,
		    context )
		);
	}

	@DisplayName( "throw in statement" )
	@Test
	public void testThrowStatement() {
		assertThrows( NoFieldException.class,
		    () -> instance.executeStatement( "throw new java:ortus.boxlang.runtime.types.exceptions.NoFieldException( 'My Message' )", context )
		);
	}

	@DisplayName( "try catch" )
	@Test
	public void testTryCatch() {

		instance.executeSource(
		    """
		         try {
		         	1/0
		           } catch (any e) {
		    message = e.getMessage();
		    message2 = e.message;
		    result = "in catch"
		           } finally {
		         		result &= ' also finally'
		           }
		             """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "in catch also finally" );
		assertThat( variables.dereference( Key.of( "message" ), false ) ).isEqualTo( "You cannot divide by zero." );
		assertThat( variables.dereference( Key.of( "message2" ), false ) ).isEqualTo( "You cannot divide by zero." );

	}

	@DisplayName( "rethrow" )
	@Test
	public void testRethrow() {
		Throwable t = assertThrows( ApplicationException.class,
		    () -> instance.executeSource(
		        """
		             try {
		             	1/0
		               } catch (any e) {
		        rethrow;
		               }
		                 """,
		        context )
		);
		assertThat( t.getMessage() ).isEqualTo( "You cannot divide by zero." );
	}

	// TODO: try/catch types

	// TODO: for/in loop. Need struct/array literals

	@DisplayName( "sentinel loop" )
	@Test
	public void testSentinelLoop() {

		instance.executeSource(
		    // initialization of i isn't correct. It's
		    // context.scopeFindNearby(Key.of("i"), variablesScope).scope().get(Key.of("i"));
		    // instead of
		    // context.scopeFindNearby(Key.of("i"), context.getDefaultAssignmentScope()).scope().assign(Key.of("i"),0);
		    """
		    result=0
		    for( i=0; i<10; variables.i++ ) {
		    	result++
		    }
		        """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 10 );

	}

	@DisplayName( "while loop" )
	@Test
	public void testWhileLoop() {

		instance.executeSource(
		    """
		       keepGoing = true
		    age = 25
		       while( keepGoing == true && age > 21 ) {
		    	age --
		    }
		           """,
		    context );
		assertThat( variables.dereference( Key.of( "age" ), false ) ).isEqualTo( 21 );

	}

}
