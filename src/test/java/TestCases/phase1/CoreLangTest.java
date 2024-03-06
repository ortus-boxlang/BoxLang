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

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;

public class CoreLangTest {

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
		assertThat( variables.get( result ) ).isEqualTo( "first" );

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
		assertThat( variables.get( result ) ).isEqualTo( "second" );

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
		assertThat( variables.get( result ) ).isEqualTo( "done" );

		instance.executeSource(
		    """
		    result = "default"

		    if( 1 == 2 )
		    	result = "not done"

		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "default" );

	}

	@DisplayName( "If blocks with no-body else statements" )
	@Test
	public void testElseNoBody() {

		instance.executeSource(
		    """
		       result = "default"

		       if( 2 == 1 ) {
		       	result = "done"
		    } else result = "else"

		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "else" );

		instance.executeSource(
		    """
		       if( 2 == 1 ) {
		       	result = "done"
		    } else result = "else"
		    result = "afterif"
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "afterif" );

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

		instance.executeSource(
		    """
		    result = "default";
		         try {
		         	1/0
		           } catch (any e) {
		    message = e.getMessage();
		    message2 = e.message;
		    result = "in catch";
		           } finally {
		         		result &= ' also finally';
		           }
		             """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "in catch also finally" );
		assertThat( variables.get( Key.of( "message" ) ) ).isEqualTo( "You cannot divide by zero." );
		assertThat( variables.get( Key.of( "message2" ) ) ).isEqualTo( "You cannot divide by zero." );

	}

	@DisplayName( "try catch with empty type" )
	@Test
	public void testTryCatchEmptyType() {

		instance.executeSource(
		    """
		         try {
		         	1/0
		           } catch ( e) {
		    message = e.getMessage();
		    message2 = e.message;
		    result = "in catch";
		           } finally {
		         		result &= ' also finally';
		           }
		             """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "in catch also finally" );
		assertThat( variables.get( Key.of( "message" ) ) ).isEqualTo( "You cannot divide by zero." );
		assertThat( variables.get( Key.of( "message2" ) ) ).isEqualTo( "You cannot divide by zero." );

	}

	@DisplayName( "try catch with interpolated type" )
	@Test
	public void testTryCatchWithInterpolatedType() {

		instance.executeSource(
		    """
		    bar = "test"
		           try {
		           	1/0
		             }
		     	catch( "foo#bar#baz" e ){

		    	}
		       catch ( e) {
		      message = e.getMessage();
		      message2 = e.message;
		      result = "in catch";
		             } finally {
		           		result &= ' also finally';
		             }
		               """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "in catch also finally" );
		assertThat( variables.get( Key.of( "message" ) ) ).isEqualTo( "You cannot divide by zero." );
		assertThat( variables.get( Key.of( "message2" ) ) ).isEqualTo( "You cannot divide by zero." );

	}

	@DisplayName( "nested try catch" )
	@Test
	public void testNestedTryCatch() {

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
		assertThat( variables.get( Key.of( "one" ) ) ).isEqualTo( "You cannot divide by zero." );
		assertThat( variables.get( Key.of( "two" ) ) )
		    .isEqualTo( "The key [bar] was not found in the struct. Valid keys are ([e, one])" );
		assertThat( variables.get( Key.of( "three" ) ) ).isEqualTo( "You cannot divide by zero." );

	}

	@DisplayName( "try multiple catches" )
	@Test
	public void testTryMultipleCatches() {

		instance.executeSource(
		    """
		    result = "default"
		       try {
		       	1/0
		       } catch (com.foo.bar e) {
		       	result = "catch1"
		       } catch ("com.foo.bar2" e) {
		       	result = "catch2"
		       } catch ( any myErr ) {
		       	result = "catchany"
		       }
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "catchany" );

	}

	@DisplayName( "try multiple catche types" )
	@Test
	public void testTryMultipleCatchTypes() {

		instance.executeSource(
		    """
		     result = "default"
		        try {
		        	1/0
		       } catch ( "com.foo.type" | java.lang.RuntimeException | "foo.bar" myErr ) {
		        	result = "catch3"
		    }
		          """,
		    context );
		// assertThat( variables.get( result ) ).isEqualTo( "catchany" );

	}

	@DisplayName( "try multiple catche types with any" )
	@Test
	public void testTryMultipleCatchTypesWithAny() {

		instance.executeSource(
		    """
		     result = "default"
		        try {
		        	1/0
		       } catch ( "com.foo.type" | java.lang.RuntimeException | any | "foo.bar" myErr ) {
		        	result = "catch3"
		    }
		          """,
		    context );
		// assertThat( variables.get( result ) ).isEqualTo( "catchany" );

	}

	@DisplayName( "try finally" )
	@Test
	public void testTryFinally() {

		assertThrows( BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		          result = "default"
		             try {
		             	1/0
		            } finally {
		        result = "finally"
		         }
		               """,
		        context )
		);
		assertThat( variables.get( result ) ).isEqualTo( "finally" );

	}

	// TODO: try/catch types
	// TODO: try/finally with no catch

	@DisplayName( "rethrow" )
	@Test
	public void testRethrow() {

		Throwable t = assertThrows( BoxRuntimeException.class,
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

	@DisplayName( "for in loop" )
	@Test
	public void testForInLoop() {

		instance.executeSource(
		    """
		       result=""
		    arr = [ "brad", "wood", "luis", "majano" ]
		       for( name in arr ) {
		       	result &= name;
		       }

		       result2=""
		    arr = [ "jorge", "reyes", "edgardo", "cabezas" ]
		       for( name in arr ) {
		       	result2 &= name;
		       }
		           """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bradwoodluismajano" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "jorgereyesedgardocabezas" );

		instance.executeSource(
		    """
		       result=""
		    arr = []
		       for( name in arr ) {
		       	result &= name;
		       }
		           """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "" );

		FunctionBoxContext functionBoxContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		instance.executeSource(
		    """
		       result=""
		    arr = [ "brad", "wood", "luis", "majano" ]
		       for( var foo["bar"].name in arr ) {
		       	result &= foo["bar"].name;
		       }

		           """,
		    functionBoxContext );
		assertThat( variables.get( result ) ).isEqualTo( "bradwoodluismajano" );

	}

	@DisplayName( "for in loop struct" )
	@Test
	public void testForInLoopStruct() {

		instance.executeSource(
		    """
		       result=""
		    str ={ foo : "bar", baz : "bum" }
		       for( key in str ) {
		       	result &= key&"="&str[ key ];
		       }
		           """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo=barbaz=bum" );

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
		assertThat( variables.get( result ) ).isEqualTo( 10 );

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
		assertThat( variables.get( result ) ).isEqualTo( 11 );

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
		assertThat( variables.get( result ) ).isEqualTo( 11 );

	}

	@DisplayName( "break sentinel" )
	@Test
	public void testBreakSentinel() {

		instance.executeSource(
		    """
		       result=0
		    i=0
		       for( i=0; i==i; i=i+1 ) {
		       	result=result+1
		     if( i > 10 ) {
		     	break;
		     }
		       }
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 12 );

	}

	@DisplayName( "while continue" )
	@Test
	public void testWhileContinue() {

		instance.executeSource(
		    """
		          result=0
		       while( true ) {
		    	result=result+1
		    	if( result < 10 ) {
		    		continue;
		    	}
		    	break;
		    }
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 10 );

	}

	@DisplayName( "Single inline while" )
	@Test
	public void testSingleInlineWhile() {

		instance.executeSource(
		    """
		    	result = 0;
		        while (true && result < 1) result=1;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );

	}

	@DisplayName( "Single inline while with parenthesis" )
	@Test
	public void testSingleInlineWhileWithParenthesis() {

		instance.executeSource(
		    """
		    	result = 0;
		        while (true && result < 1) (result=1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );

	}

	@DisplayName( "Single next line while" )
	@Test
	public void testSingleNextLineWhile() {

		instance.executeSource(
		    """
		      	result = 0;
		    while (true && result < 1)
		       	result=1;
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );

	}

	@DisplayName( "Single next line while with parenthesis" )
	@Test
	public void testSingleNextLineWhileWithParenthesis() {

		instance.executeSource(
		    """
		      	result = 0;
		    while (true && result < 1)
		       	(result=1);
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );

	}

	@DisplayName( "Single next line while only loop body" )
	@Test
	public void testSingleNextLineWhileOnlyLoopBody() {

		instance.executeSource(
		    """
		    result = 0;
		       other = 0;
		         while (true && result < 5)
		            	(result = result + 1);
		       other = other + 1;
		           """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "other" ) ) ).isEqualTo( 1 );

	}

	@DisplayName( "Multiple parnetheitcal statements" )
	@Test
	public void testMultipleParnetheticalStatements() {

		instance.executeSource(
		    """
		    (1+2);
		    (1+2);
		           """,
		    context );

	}

	@DisplayName( "Multiple parnetheitcal statements with over-nested parenthesis" )
	@Test
	public void testMultipleParnetheticalStatementsWithOverNestedParenthesis() {

		instance.executeSource(
		    """
		    ((((1+2))));
		    (1+2);
		           """,
		    context );
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
		assertThat( variables.get( Key.of( "test1" ) ) ).isEqualTo( true );

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

		assertThat( variables.get( Key.of( "test2" ) ) ).isEqualTo( "foo'bar" );
		assertThat( variables.get( Key.of( "test3" ) ) ).isEqualTo( "foo\"bar" );

	}

	@DisplayName( "String parsing quotation escaping" )
	@Test
	public void testStringParsingQuoteEscapes() {

		instance.executeSource(
		    """
		    // To escape a quote char, double it.
		    test4 = "Brad ""the guy"" Wood"
		    test5 = 'Luis ''the man'' Majano'
		      """,
		    context );

		assertThat( variables.get( Key.of( "test4" ) ) ).isEqualTo( "Brad \"the guy\" Wood" );
		assertThat( variables.get( Key.of( "test5" ) ) ).isEqualTo( "Luis 'the man' Majano" );
	}

	@DisplayName( "String parsing concatenation" )
	@Test
	public void testStringParsingConcatenation() {

		instance.executeSource(
		    """
		    // Expressions are always interpolated inside string literals in CFScript by using a hash/pound sign (`#`) such as
		    variables.timeVar = "12:00 PM"
		    variables.test6 = "Time is: #timeVar#"
		    variables.test7 = "Time is: " & timeVar
		    variables.test8 = 'Time is: #timeVar#'
		    variables.test9 = 'Time is: ' & timeVar
		     """,
		    context );
		assertThat( variables.get( Key.of( "test6" ) ) ).isEqualTo( "Time is: 12:00 PM" );
		assertThat( variables.get( Key.of( "test7" ) ) ).isEqualTo( "Time is: 12:00 PM" );

	}

	@DisplayName( "String parsing expression interpolation" )
	@Test
	public void testStringParsingExpressionInterpolation() {

		instance.executeSource(
		    """
		    variables.var = "brad"
		    varname = "var"
		    variables.result1 = "#var#foo"
		    variables.result2 = "foo#var#"
		    variables.result3 = "foo#var#bar"
		    variables.result4 = "foo#var#bar#var#baz#var#bum"
		    variables.result5 = "foo"
		    variables.result6 = "#var#"
		    variables.result7 = "foo #variables[ "var" ]# bar"
		    variables.result8 = "foo #variables[ "#varname#" ]# bar"
		    variables.result9 = "foo #variables[ 'var' ]# bar"
		    variables.result10 = "foo #variables[ '#varname#' ]# bar"
		     """,
		    context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "bradfoo" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "foobrad" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "foobradbar" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "foobradbarbradbazbradbum" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "foo" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "foo brad bar" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( "foo brad bar" );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "foo brad bar" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "foo brad bar" );

	}

	@DisplayName( "String parsing interpolation single" )
	@Test
	public void testStringParsingInterpolationSingle() {

		instance.executeSource(
		    """
		    variables.var = "brad"
		    varname = "var"
		    variables.result1 = '#var#foo'
		    variables.result2 = 'foo#var#'
		    variables.result3 = 'foo#var#bar'
		    variables.result4 = 'foo#var#bar#var#baz#var#bum'
		    variables.result5 = 'foo'
		    variables.result6 = '#var#'
		    variables.result7 = 'foo #variables[ 'var' ]# bar'
		    variables.result8 = 'foo #variables[ '#varname#' ]# bar'
		    variables.result9 = 'foo #variables[ "var" ]# bar'
		    variables.result10 = 'foo #variables[ "#varname#" ]# bar'
		     """,
		    context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "bradfoo" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "foobrad" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "foobradbar" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "foobradbarbradbazbradbum" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "foo" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "foo brad bar" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( "foo brad bar" );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "foo brad bar" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "foo brad bar" );

	}

	@DisplayName( "String parsing - escaped pound sign" )
	@Test
	public void testStringParsingEscapedPoundSign() {

		instance.executeSource(
		    """
		    // Pound signs in a string are escaped by doubling them
		    variables.test8 = "I have locker ##20"
		    // Also "I have locker #20" should throw a parsing syntax exception.
		     """,
		    context );
		assertThat( variables.get( Key.of( "test8" ) ) ).isEqualTo( "I have locker #20" );

		instance.executeSource(
		    """
		    variables.test8 = 'I have locker ##20'
		     """,
		    context );
		assertThat( variables.get( Key.of( "test8" ) ) ).isEqualTo( "I have locker #20" );

	}

	@DisplayName( "String parsing - escaped Java chars" )
	@Test
	public void testStringParsingEscapedJavaChars() {

		instance.executeSource(
		    """
		    result = "this is not \\t a tab"
		     """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "this is not \\t a tab" );

		instance.executeSource(
		    """
		    result = "foo "" bar '' baz"
		     """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo \" bar '' baz" );

		instance.executeSource(
		    """
		    result = 'foo "" bar '' baz'
		     """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo \"\" bar ' baz" );

		instance.executeSource(
		    """
		    result = 'foo 	 bar'
		     """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo \t bar" );

	}

	@DisplayName( "String parsing - escaped Java chars with interpolation" )
	@Test
	public void testStringParsingEscapedJavaCharsInter() {

		instance.executeSource(
		    """
		    brad="wood"
		       result = "this is not \\t a tab#brad#"
		        """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "this is not \\t a tabwood" );

		instance.executeSource(
		    """
		    brad="wood"
		      result = "foo "" bar '' baz#brad#"
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo \" bar '' bazwood" );

		instance.executeSource(
		    """
		    brad="wood"
		      result = 'foo "" bar '' baz#brad#'
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo \"\" bar ' bazwood" );

		instance.executeSource(
		    """
		    brad="wood"
		      result = 'foo 	 bar#brad#'
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "foo \t barwood" );

	}

	@DisplayName( "String parsing concat" )
	@Test
	public void testStringParsingConcat() {

		instance.executeSource(
		    """
		    variables.a = "brad"
		    variables.b = "luis"
		       variables.result = "a is #variables.a# and b is #variables.b#"

		        """,
		    context );
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "a is brad and b is luis" );

	}

	@DisplayName( "String parsing unclosed quotes" )
	@Test
	public void testStringParsingUnclosedQuotes() {

		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    foo = "unfinished
		     """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Untermimated" );

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    foo = 'unfinishedx
		     """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Untermimated" );
	}

	@DisplayName( "It should throw BoxRuntimeException" )
	@Test
	public void testBoxRuntimeException() {

		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    throw "test"
		     """,
		    context ) );
		assertThat( t.getMessage() ).contains( "test" );
	}

	@DisplayName( "String parsing unclosed pound" )
	@Test
	public void testStringParsingUnclosedPound() {

		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    	// should throw a parsing syntax exception.
		    result = "I have locker #20";
		    	""",
		    context
		)
		);
		assertThat( t.getMessage() ).contains( "Untermimated hash" );

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

		assertThat( variables.get( Key.of( "test9" ) ) ).isEqualTo( "Time is: 12:00 PM" );
		assertThat( variables.get( Key.of( "test10" ) ) ).isEqualTo( "BoxLang" );

	}

	@DisplayName( "String parsing expression in pounds" )
	@Test
	public void testStringParsingExpressionInPounds() {

		instance.executeSource(
		    """
		    result = "Box#5+6#Lang"
		      """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Box11Lang" );

	}

	@DisplayName( "switch" )
	@Test
	public void testSwtich() {

		instance.executeSource(
		    """
		      	result = ""
		      variables.foo = true;

		      switch( "12" ) {
		      case "brad":
		      	// case 1 logic
		      	result = "case1"
		      	more="than";
		      	one="statement"
		      	here="test";
		      	break;
		      case 42: {
		      	// case 2 logic
		      	result = "case2"
		      	break;
		      }
		      case 5+7:
		      	// case 3 logic
		      	result = "case3"
		      	more="than";
		      	one="statement"
		      	here="test";
		      	break;
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

		assertThat( variables.get( result ) ).isEqualTo( "case3" );

	}

	@DisplayName( "switch fall through case" )
	@Test
	public void testSwtichFallThroughCase() {

		instance.executeSource(
		    """
		       bradRan = false
		       luisRan = false
		       gavinRan = false
		       jorgeRan = false

		       switch( "luis" ) {
		       case "brad":
		         bradRan = true
		         break;
		    // This case will be entered
		       case "luis": {
		         luisRan = true
		       }
		    // Because there is no break, this case will also be entered
		       case "gavin":
		         gavinRan = true
		         break;
		    // But we'll never reach this one
		       case "jorge":
		       jorgeRan = true
		         break;
		       }

		             """,
		    context );

		assertThat( variables.get( Key.of( "bradRan" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "luisRan" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "gavinRan" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "jorgeRan" ) ) ).isEqualTo( false );
	}

	@DisplayName( "switch default" )
	@Test
	public void testSwitchDefault() {

		instance.executeSource(
		    """
		      	result = ""
		      	// must be boolean
		      variables.foo = false;

		      switch( "sdfsd"&"fsdf" & (5+4) ) {
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

		assertThat( variables.get( result ) ).isEqualTo( "case default" );

	}

	@DisplayName( "String as array" )
	@Test
	public void testStringAsArray() {

		instance.executeSource(
		    """
		       name = "brad"
		    result = name[3]
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "a" );

		instance.executeSource(
		    """
		    result = "brad"[3]
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "a" );

		instance.executeSource(
		    """
		    result = "brad".CASE_INSENSITIVE_ORDER
		         """,
		    context );

		assertThat( variables.get( result ) instanceof Comparator ).isTrue();

	}

	@DisplayName( "String as array" )
	@Test
	public void testStringAsArray2() {
		long	start	= TimeUnit.NANOSECONDS.toMillis( System.nanoTime() );
		long	startn	= System.nanoTime();

		/*
		 * for ( int i = 0; i < 50_000_000; i++ ) {
		 * new Struct();
		 * }
		 */

		for ( int i = 0; i < 3_000_000; i++ ) {
			instance.getConfiguration().asStruct();
		}

		// instance.getConfiguration().asStruct();
		// new Struct();
		System.out.println( TimeUnit.NANOSECONDS.toMillis( System.nanoTime() ) - start );
		System.out.println( System.nanoTime() - startn );

	}

	@Test
	public void testKeywords() {

		instance.executeSource(
		    """
		                          result = {
		            				class : ()->"class",
		            				abstract : ()->'abstract',
		            				abort : ()->'abort',
		            				admin : ()->'admin',
		            				any : ()->'any',
		            				array : ()->'array',
		            				as : ()->'as',
		            				assert : ()->'assert',
		            				boolean : ()->'boolean',
		            				break : ()->'break',
		            				case : ()->'case',
		            				castas : ()->'castas',
		            				catch : ()->'catch',
		            				class : ()->'class',
		            				component : ()->'component',
		            				contain : ()->'contain',
		            				contains : ()->'contains',
		            				continue : ()->'continue',
		            				default : ()->'default',
		            				does : ()->'does',
		            				do : ()->'do',
		            				else : ()->'else',
		            				elif : ()->'elif',
		            				false : ()->'false',
		            				finally : ()->'finally',
		            				for : ()->'for',
		            				function : ()->'function',
		            				greater : ()->'greater',
		            				if : ()->'if',
		            				in : ()->'in',
		            				import : ()->'import',
		            				include : ()->'include',
		            				interface : ()->'interface',
		            				instanceof : ()->'instanceof',
		            				is : ()->'is',
		            				java : ()->'java',
		            				less : ()->'less',
		            				local : ()->'local',
		            				lock : ()->'lock',
		            				mod : ()->'mod',
		            				message : ()->'message',
		            				new : ()->'new',
		            				null : ()->'null',
		            				numeric : ()->'numeric',
		            				package : ()->'package',
		            				param : ()->'param',
		            				private : ()->'private',
		            				property : ()->'property',
		            				public : ()->'public',
		            				query : ()->'query',
		            				remote : ()->'remote',
		            				required : ()->'required',
		            				request : ()->'request',
		            				return : ()->'return',
		            				rethrow : ()->'rethrow',
		            				savecontent : ()->'savecontent',
		            				setting : ()->'setting',
		            				static : ()->'static',
		            				string : ()->'string',
		            				struct : ()->'struct',
		            				//switch : ()->'switch',
		            				than : ()->'than',
		            				to : ()->'to',
		            				thread : ()->'thread',
		            				throw : ()->'throw',
		            				type : ()->'type',
		            				true : ()->'true',
		            				try : ()->'try',
		            				var : ()->'var',
		            				when : ()->'when',
		            				while : ()->'while',
		            				xor : ()->'xor',
		            				eq : ()->'eq',
		            				eqv : ()->'eqv',
		            				imp : ()->'imp',
		            				and : ()->'and',
		            				eq : ()->'eq',
		            				equal : ()->'equal',
		            				gt : ()->'gt',
		            				gte : ()->'gte',
		            				ge : ()->'ge',
		            				lt : ()->'lt',
		            				lte : ()->'lte',
		            				le : ()->'le',
		            				neq : ()->'neq',
		            				not : ()->'not',
		            				or : ()->'or'

		                       };


		              result.class;
		              result.abstract;
		              result.abort;
		              result.admin;
		              result.any;
		              result.array;
		              result.as;
		              result.assert;
		              result.boolean;
		              result.break;
		              result.case;
		              result.castas;
		              result.catch;
		              result.class;
		              result.component;
		              result.contain;
		              result.contains;
		              result.continue;
		              result.default;
		              result.does;
		              result.do;
		              result.else;
		              result.elif;
		              result.false;
		              result.finally;
		              result.for;
		              result.function;
		              result.greater;
		              result.if;
		              result.in;
		              result.import;
		              result.include;
		              result.interface;
		              result.instanceof;
		              result.is;
		              result.java;
		              result.less;
		              result.local;
		              result.lock;
		              result.mod;
		              result.message;
		              result.new;
		              result.null;
		              result.numeric;
		              result.package;
		              result.param;
		              result.private;
		              result.property;
		              result.public;
		              result.query;
		              result.remote;
		              result.required;
		              result.request;
		              result.return;
		              result.rethrow;
		              result.savecontent;
		              result.setting;
		              result.static;
		              result.string;
		              result.struct;
		          //    result.switch;
		              result.than;
		              result.to;
		              result.thread;
		              result.throw;
		              result.type;
		              result.true;
		              result.try;
		              result.var;
		              result.when;
		              result.while;
		              result.xor;
		              result.eq;
		              result.eqv;
		              result.imp;
		              result.and;
		              result.eq;
		              result.equal;
		              result.gt;
		              result.gte;
		              result.ge;
		              result.lt;
		              result.lte;
		              result.le;
		              result.neq;
		              result.not;
		              result.or;

		            result.class();
		            result.abstract();
		            result.abort();
		            result.admin();
		            result.any();
		            result.array();
		            result.as();
		            result.assert();
		            result.boolean();
		            result.break();
		            result.case();
		            result.castas();
		            result.catch();
		            result.class();
		            result.component();
		            result.contain();
		            result.contains();
		            result.continue();
		            result.default();
		            result.does();
		            result.do();
		            result.else();
		            result.elif();
		            result.false();
		            result.finally();
		            result.for();
		            result.function();
		            result.greater();
		            result.if();
		            result.in();
		            result.import();
		            result.include();
		            result.interface();
		            result.instanceof();
		            result.is();
		            result.java();
		            result.less();
		            result.local();
		            result.lock();
		            result.mod();
		            result.message();
		            result.new();
		            result.null();
		            result.numeric();
		            result.package();
		            result.param();
		            result.private();
		            result.property();
		            result.public();
		            result.query();
		            result.remote();
		            result.required();
		            result.request();
		            result.return();
		            result.rethrow();
		            result.savecontent();
		            result.setting();
		            result.static();
		            result.string();
		            result.struct();
		         //   result.switch();
		            result.than();
		            result.to();
		            result.thread();
		            result.throw();
		            result.type();
		            result.true();
		            result.try();
		            result.var();
		            result.when();
		            result.while();
		            result.xor();
		            result.eq();
		            result.eqv();
		            result.imp();
		            result.and();
		            result.eq();
		            result.equal();
		            result.gt();
		            result.gte();
		            result.ge();
		            result.lt();
		            result.lte();
		            result.le();
		            result.neq();
		            result.not();
		            result.or();

		         variables.addAll( result.getWrapped() );

		      class();
		      abstract();
		      abort();
		      admin();
		      any();
		      array();
		      as();
		      assert();
		      boolean();
		      break();
		      case();
		      castas();
		      catch();
		      class();
		      component();
		      contain();
		      contains();
		      continue();
		      default();
		      does();
		      do();
		      else();
		      elif();
		      false();
		      finally();
		      for();
		      function();
		      greater();
		      if();
		      in();
		      import();
		      include();
		      interface();
		      instanceof();
		      is();
		      java();
		      less();
		      local();
		      lock();
		      mod();
		      message();
		      new();
		      null();
		      numeric();
		      package();
		      param();
		      private();
		      property();
		      public();
		      query();
		      remote();
		      required();
		      request();
		      return();
		      rethrow();
		      savecontent();
		      setting();
		      static();
		      string();
		      struct();
		    //  switch();
		      than();
		      to();
		      thread();
		     // throw(); <-- Actual throw construct
		      type();
		      true();
		      try();
		      var();
		      when();
		      while();
		      xor();
		      eq();
		      eqv();
		      imp();
		      and();
		      eq();
		      equal();
		      gt();
		      gte();
		      ge();
		      lt();
		      lte();
		      le();
		      neq();
		      not();
		      or();
		                                   """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		var str = variables.getAsStruct( result );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "class" ), new Object[] {}, false ) ).isEqualTo( "class" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "abstract" ), new Object[] {}, false ) ).isEqualTo( "abstract" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "abort" ), new Object[] {}, false ) ).isEqualTo( "abort" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "admin" ), new Object[] {}, false ) ).isEqualTo( "admin" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "any" ), new Object[] {}, false ) ).isEqualTo( "any" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "array" ), new Object[] {}, false ) ).isEqualTo( "array" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "as" ), new Object[] {}, false ) ).isEqualTo( "as" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "assert" ), new Object[] {}, false ) ).isEqualTo( "assert" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "boolean" ), new Object[] {}, false ) ).isEqualTo( "boolean" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "break" ), new Object[] {}, false ) ).isEqualTo( "break" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "case" ), new Object[] {}, false ) ).isEqualTo( "case" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "castas" ), new Object[] {}, false ) ).isEqualTo( "castas" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "catch" ), new Object[] {}, false ) ).isEqualTo( "catch" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "class" ), new Object[] {}, false ) ).isEqualTo( "class" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "component" ), new Object[] {}, false ) ).isEqualTo( "component" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "contain" ), new Object[] {}, false ) ).isEqualTo( "contain" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "contains" ), new Object[] {}, false ) ).isEqualTo( "contains" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "continue" ), new Object[] {}, false ) ).isEqualTo( "continue" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "default" ), new Object[] {}, false ) ).isEqualTo( "default" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "does" ), new Object[] {}, false ) ).isEqualTo( "does" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "do" ), new Object[] {}, false ) ).isEqualTo( "do" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "else" ), new Object[] {}, false ) ).isEqualTo( "else" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "elif" ), new Object[] {}, false ) ).isEqualTo( "elif" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "false" ), new Object[] {}, false ) ).isEqualTo( "false" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "finally" ), new Object[] {}, false ) ).isEqualTo( "finally" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "for" ), new Object[] {}, false ) ).isEqualTo( "for" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "function" ), new Object[] {}, false ) ).isEqualTo( "function" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "greater" ), new Object[] {}, false ) ).isEqualTo( "greater" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "if" ), new Object[] {}, false ) ).isEqualTo( "if" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "in" ), new Object[] {}, false ) ).isEqualTo( "in" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "import" ), new Object[] {}, false ) ).isEqualTo( "import" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "include" ), new Object[] {}, false ) ).isEqualTo( "include" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "interface" ), new Object[] {}, false ) ).isEqualTo( "interface" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "instanceof" ), new Object[] {}, false ) ).isEqualTo( "instanceof" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "is" ), new Object[] {}, false ) ).isEqualTo( "is" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "java" ), new Object[] {}, false ) ).isEqualTo( "java" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "less" ), new Object[] {}, false ) ).isEqualTo( "less" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "local" ), new Object[] {}, false ) ).isEqualTo( "local" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "lock" ), new Object[] {}, false ) ).isEqualTo( "lock" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "mod" ), new Object[] {}, false ) ).isEqualTo( "mod" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "message" ), new Object[] {}, false ) ).isEqualTo( "message" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "new" ), new Object[] {}, false ) ).isEqualTo( "new" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "null" ), new Object[] {}, false ) ).isEqualTo( "null" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "numeric" ), new Object[] {}, false ) ).isEqualTo( "numeric" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "package" ), new Object[] {}, false ) ).isEqualTo( "package" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "param" ), new Object[] {}, false ) ).isEqualTo( "param" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "private" ), new Object[] {}, false ) ).isEqualTo( "private" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "property" ), new Object[] {}, false ) ).isEqualTo( "property" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "public" ), new Object[] {}, false ) ).isEqualTo( "public" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "query" ), new Object[] {}, false ) ).isEqualTo( "query" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "remote" ), new Object[] {}, false ) ).isEqualTo( "remote" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "required" ), new Object[] {}, false ) ).isEqualTo( "required" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "request" ), new Object[] {}, false ) ).isEqualTo( "request" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "return" ), new Object[] {}, false ) ).isEqualTo( "return" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "rethrow" ), new Object[] {}, false ) ).isEqualTo( "rethrow" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "savecontent" ), new Object[] {}, false ) ).isEqualTo( "savecontent" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "setting" ), new Object[] {}, false ) ).isEqualTo( "setting" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "static" ), new Object[] {}, false ) ).isEqualTo( "static" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "string" ), new Object[] {}, false ) ).isEqualTo( "string" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "struct" ), new Object[] {}, false ) ).isEqualTo( "struct" );
		// assertThat( str.dereferenceAndInvoke( context, Key.of( "switch" ), new Object[] {}, false ) ).isEqualTo( "switch" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "than" ), new Object[] {}, false ) ).isEqualTo( "than" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "to" ), new Object[] {}, false ) ).isEqualTo( "to" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "thread" ), new Object[] {}, false ) ).isEqualTo( "thread" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "throw" ), new Object[] {}, false ) ).isEqualTo( "throw" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "type" ), new Object[] {}, false ) ).isEqualTo( "type" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "true" ), new Object[] {}, false ) ).isEqualTo( "true" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "try" ), new Object[] {}, false ) ).isEqualTo( "try" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "var" ), new Object[] {}, false ) ).isEqualTo( "var" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "when" ), new Object[] {}, false ) ).isEqualTo( "when" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "while" ), new Object[] {}, false ) ).isEqualTo( "while" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "xor" ), new Object[] {}, false ) ).isEqualTo( "xor" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "eq" ), new Object[] {}, false ) ).isEqualTo( "eq" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "eqv" ), new Object[] {}, false ) ).isEqualTo( "eqv" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "imp" ), new Object[] {}, false ) ).isEqualTo( "imp" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "and" ), new Object[] {}, false ) ).isEqualTo( "and" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "eq" ), new Object[] {}, false ) ).isEqualTo( "eq" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "equal" ), new Object[] {}, false ) ).isEqualTo( "equal" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "gt" ), new Object[] {}, false ) ).isEqualTo( "gt" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "gte" ), new Object[] {}, false ) ).isEqualTo( "gte" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "ge" ), new Object[] {}, false ) ).isEqualTo( "ge" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "lt" ), new Object[] {}, false ) ).isEqualTo( "lt" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "lte" ), new Object[] {}, false ) ).isEqualTo( "lte" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "le" ), new Object[] {}, false ) ).isEqualTo( "le" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "neq" ), new Object[] {}, false ) ).isEqualTo( "neq" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "not" ), new Object[] {}, false ) ).isEqualTo( "not" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "or" ), new Object[] {}, false ) ).isEqualTo( "or" );

	}

}