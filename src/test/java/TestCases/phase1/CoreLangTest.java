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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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

		assertThrows( NoFieldException.class, () -> instance.executeSource(
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
		    () -> instance.executeStatement( "throw new java:ortus.boxlang.runtime.types.exceptions.NoFieldException( 'My Message' );", context )
		);
	}

	@DisplayName( "try catch" )
	@Test
	public void testTryCatch() {

		// MT TODO: THere are a few problems in this one. We are still hard-coding variablesScope as the default scope instead of
		// context.getDefaultAssignmentScope()
		// and secondly, all of the code in the catch block must use catchContext, not context.
		// Thirdly, e.getMessage() is not looking up "e" in the catchContext via scopeFindNearby, but instead is using Java e variable directly
		instance.executeSource(
		    """
		         try {
		         	1/0
		           } catch (any e) {
		    message = e.getMessage();
		    //message2 = e.message;
		    result = "in catch";
		           } finally {
		         		result &= ' also finally';
		           }
		             """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "in catch also finally" );
		assertThat( variables.dereference( Key.of( "message" ), false ) ).isEqualTo( "You cannot divide by zero." );
		// assertThat( variables.dereference( Key.of( "message2" ), false ) ).isEqualTo( "You cannot divide by zero." );

	}

	@DisplayName( "nested try catch" )
	@Test
	public void testNestedTryCatch() {

		// MT TODO: There are two problems here (including all the issues in the above test). The first is that the Java exception names must be unique.
		// Instead of using the CFML names, I would generage ex1, ex2, ex3 or similar names dyanamically.
		// The second issue is that there needs to be a UNIQUE catchContext for each nested catch block. Again, I would generage dynamic names here. The code
		// in the
		// outer catch block should use the outer catchContext, and the code in the inner catch block should use the inner catchContext.
		instance.executeSource(
		    """
		    try {
		    	1/0
		    } catch (any e) {
		    	one = e.getMessage()

		    	try {
		    		foo=variables.bar
		    	} catch (any e) {
		    		two = e.getMessage()
		    	}

		    	three = e.getMessage()
		    }
		      """,
		    context );
		assertThat( variables.dereference( Key.of( "one" ), false ) ).isEqualTo( "You cannot divide by zero." );
		assertThat( variables.dereference( Key.of( "two" ), false ) )
		    .isEqualTo( "The key bar was not found in the struct. Valid keys are ([ONE, TWO, THREE])" );
		assertThat( variables.dereference( Key.of( "three" ), false ) ).isEqualTo( "You cannot divide by zero." );

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

	@DisplayName( "do while loop" )
	@Test
	@Timeout( value = 5, unit = TimeUnit.SECONDS )
	public void testDoWhileLoop() {

		instance.executeSource(
		    """
		     result = 1;
		     do {
		    result = variables.result + 1;
		     } while( result < 10 )
		     """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 10 );

	}

	@DisplayName( "break while" )
	@Test
	public void testBreakWhile() {

		instance.executeSource(
		    """
		    result = 1;
		    while( true ) {
		        result = result + 1;
		    	if( result > "10" ) {
		    		break;
		    	}
		    }
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 11 );

	}

	@DisplayName( "break do while" )
	@Test
	public void testBreakDoWhile() {

		instance.executeSource(
		    """
		       	result = 1;
		         do {
		    result = variables.result + 1;
		     		if( result > "10" ) {
		     			break;
		     		}
		         } while( true )
		         """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 11 );

	}

	@DisplayName( "break sentinel" )
	@Test
	public void testBreakSentinel() {

		instance.executeSource(
		    """
		       result=0
		    i=0
		       for( i=0; i==i; i++ ) {
		       	result++
		     if( i > 10 ) {
		     	break;
		     }
		       }
		       """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 12 );

	}

	@DisplayName( "while continue" )
	@Test
	public void testWhileContinue() {

		instance.executeSource(
		    """
		          result=0
		       while( true ) {
		    	result++
		    	if( result < 10 ) {
		    		continue;
		    	}
		    	break;
		    }
		          """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 10 );

	}

	@DisplayName( "String parsing 1" )
	@Test
	public void testStringParsing1() {

		instance.executeSource(
		    """
		    // Strings can use single quotes OR double quotes, so long as the “bookends” match.
		    test1 = "foo" == 'foo'
		      """,
		    context );
		assertThat( variables.dereference( Key.of( "test1" ), false ) ).isEqualTo( true );

	}

	@DisplayName( "String parsing 2" )
	@Test
	public void testStringParsing2() {

		instance.executeSource(
		    """
		    // A double quote-encased string doesn’t need to escape single quotes inside and vice versa
		    test2 = "foo'bar"
		    test3 = 'foo"bar'
		      """,
		    context );

		assertThat( variables.dereference( Key.of( "test2" ), false ) ).isEqualTo( "foo'bar" );
		assertThat( variables.dereference( Key.of( "test3" ), false ) ).isEqualTo( "foo\"bar" );

	}

	@DisplayName( "String parsing 3" )
	@Test
	public void testStringParsing3() {

		instance.executeSource(
		    """
		    // To escape a quote char, double it.
		    test4 = "Brad ""the guy"" Wood"
		    test5 = 'Luis ''the man'' Majano'
		      """,
		    context );

		assertThat( variables.dereference( Key.of( "test4" ), false ) ).isEqualTo( "Brad \"the guy\" Wood" );
		assertThat( variables.dereference( Key.of( "test5" ), false ) ).isEqualTo( "Luis 'the man' Majano" );
	}

	@DisplayName( "String parsing 4" )
	@Test
	public void testStringParsing4() {

		instance.executeSource(
		    """
		    // Expressions are always interpolated inside string literals in CFScript by using a hash/pound sign (`#`) such as
		    variables.timeVar = "12:00 PM"
		    variables.test6 = "Time is: #timeVar#"
		    variables.test7 ="Time is: " & timeVar
		     """,
		    context );
		assertThat( variables.dereference( Key.of( "test6" ), false ) ).isEqualTo( "Time is: 12:00 PM" );
		assertThat( variables.dereference( Key.of( "test7" ), false ) ).isEqualTo( "Time is: 12:00 PM" );

	}

	@DisplayName( "String parsing 5" )
	@Test
	public void testStringParsing5() {

		instance.executeSource(
		    """
		    // Pound signs in a string are escaped by doubling them
		    variables.test8 = "I have locker ##20"
		    // Also "I have locker #20" should throw a parsing syntax exception.

		     """,
		    context );
		assertThat( variables.dereference( Key.of( "test8" ), false ) ).isEqualTo( "I have locker #20" );

	}

	@DisplayName( "String parsing unclosed pound" )
	@Test
	public void testStringParsingUnclosedPount() {

		Throwable t = assertThrows( ApplicationException.class, () -> instance.executeSource(
		    """
		    	// should throw a parsing syntax exception.
		    resut = "I have locker #20";
		    	""",
		    context
		)
		);
		assertThat( t.getMessage() ).contains( "#" );

	}

	@DisplayName( "String parsing 6" )
	@Test
	public void testStringParsing6() {

		instance.executeSource(
		    """
		     // On an unrelated note, pound signs around CFScript expressions are superfluous and should be ignored by the parser.
		    timeVar = "12:00 PM"
		    test9 = "Time is: " & #timeVar#
		    result = "BoxLang"
		    test10 = #result#;
		      """,
		    context );

		assertThat( variables.dereference( Key.of( "test9" ), false ) ).isEqualTo( "Time is: 12:00 PM" );
		assertThat( variables.dereference( Key.of( "test10" ), false ) ).isEqualTo( "BoxLang" );

	}

	@DisplayName( "String parsing expression in pounds" )
	@Test
	public void testStringParsingExpressionInPounds() {

		// MT TODO: Needs to use Concat operator, not Java's `+`
		instance.executeSource(
		    """
		    result = "Box#5+6#Lang"
		      """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "Box11Lang" );

	}

	@DisplayName( "switch" )
	@Test
	public void testSwitch() {

		// MT TODO: This code runs on both Adobe and Lucee CF engines
		instance.executeSource(
		    """
		      	result = ""
		      variables.foo = true;

		      switch( "12" ) {
		      case "brad":
		      	// case 1 logic
		      	result = "case1"
		      	break;
		      case 42: {
		      	// case 2 logic
		      	result = "case2"
		      	break;
		      }
		      case 5+7:
		      	// case 3 logic
		      	result = "case3"
		      case variables.foo:
		      	// case 4 logic
		      	result = "case4"
		      	break;
		      default:
		      	// default case logic
		    result = "case default"
		      }
		          """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "case4" );

	}

	@DisplayName( "switch default" )
	@Test
	public void testSwitchDefault() {

		// MT TODO: This code runs on both Adobe and Lucee CF engines
		instance.executeSource(
		    """
		      	result = ""
		      	// must be boolean
		      variables.foo = false;

		      switch( "sdfsdfsdf" ) {
		      case "brad":
		      	// case 1 logic
		      	result = "case1"
		      	break;
		      case 42: {
		      	// case 2 logic
		      	result = "case2"
		      	break;
		      }
		      case 5+7:
		      	// case 3 logic
		      	result = "case3"
		      case variables.foo:
		      	// case 4 logic
		      	result = "case4"
		      	break;
		      default:
		      	// default case logic
		    result = "case default"
		      }
		          """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "case default" );

	}

}