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

import java.io.IOException;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.DocParser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.ParseException;

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

	@DisplayName( "if with single-token elseif" )
	@Test
	public void testIfSingleTokenElseIf() {

		instance.executeSource(
		    """
		          if( true ){
		          } elseif( true ){
		          }
		       6+7
		       elseif = "foo"
		    result = elseif;
		              """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "foo" );

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
		    context ) );
	}

	@DisplayName( "throw in statement" )
	@Test
	public void testThrowStatement() {
		assertThrows( NoFieldException.class,
		    () -> instance.executeStatement(
		        "throw new java:ortus.boxlang.runtime.types.exceptions.NoFieldException( 'My Message' );",
		        context ) );
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

	@DisplayName( "try catch with var in CF" )
	@Test
	public void testTryCatchWithVarCF() {

		instance.executeSource(
		    """
		    result = "default";
		         try {
		         	1/0
		           } catch (any var e) {
		    message = e.getMessage();
		    message2 = e.message;
		    result = "in catch";
		           } finally {
		         		result &= ' also finally';
		           }
		             """,
		    context, BoxSourceType.CFSCRIPT );
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

	@DisplayName( "try multiple catch types" )
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
		        context ) );
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
		        context ) );
		assertThat( t.getMessage() ).isEqualTo( "You cannot divide by zero." );
	}

	@DisplayName( "for in loop" )
	@Test
	public void testForInLoop1() {

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

	}

	@DisplayName( "for in loop" )
	@Test
	public void testForInLoop2() {

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

	}

	@DisplayName( "for in loop" )
	@Test
	public void testForInLoop3() {

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
		assertThat( functionBoxContext.getScopeNearby( LocalScope.name ).get( result ) ).isEqualTo( "bradwoodluismajano" );

	}

	@DisplayName( "for in loop with list" )
	@Test
	public void testForInLoopList() {

		instance.executeSource(
		    """
		    result = "";
		       for( item in "hello,world,test" )
		       	result &= item;
		                """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "helloworldtest" );
	}

	@DisplayName( "for in loop single statment" )
	@Test
	public void testForInLoopSingleStatement() {

		instance.executeSource(
		    """
		    	result=""
		    	arr = [ "brad", "wood", "luis", "majano" ]
		    	for( name in arr )
		    		result &= name;
		    """,
		    context );
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

	@DisplayName( "var sentinel" )
	@Test
	public void testVarSentinel() {

		instance.executeSource(
		    """
		    result=0
		    function runner() {
		    	i=0
		    	for( var i=0; i<10; i++ ) {
		    		result+=1
		    	}
		    }
		    runner()
		    	  """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 10 );

	}

	@DisplayName( "var assignment as expression" )
	@Test
	public void testVarAssignAsExpr() {

		instance.executeSource(
		    """
		       function runner( arg ) {
		       	return arg;
		       }
		    function foo() {
		    	variables.result = runner( (var brad = 5) )
		    	variables.result2 = runner( arg=(var brad = 5) )
		    }
		    foo()
		       	  """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 5 );

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

	@DisplayName( "sentinel but everything is missing" )
	@Test
	public void testSentinelButEverythingIsMissing() {

		instance.executeSource(
		    """
		    counter = 1;
		    for ( ; ; ) {
		    	writeOutput( counter );
		    	counter++;
		    	if( counter > 5 ) {
		    		break;
		    	}
		    }
		    result = counter;
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@DisplayName( "sentinel init only" )
	@Test
	public void testSentinelInitOnly() {

		instance.executeSource(
		    """
		    for ( counter = 1 ; ; ) {
		    	writeOutput( counter );
		    	counter++;
		    	if( counter > 5 ) {
		    		break;
		    	}
		    }
		    result = counter;
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@DisplayName( "sentinel condition only" )
	@Test
	public void testSentinelConditionOnly() {

		instance.executeSource(
		    """
		      counter = 1;
		         for (  ; counter <= 5 ; ) {
		         	writeOutput( counter );
		         	counter++;

		    assert counter < 100;
		         }
		         result = counter;
		              """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@DisplayName( "sentinel increment only" )
	@Test
	public void testSentinelIncrementOnly() {

		instance.executeSource(
		    """
		       counter = 1;
		    safety = 0;
		       for (  ; ; counter++ ) {
		       	writeOutput( counter );
		       	if( counter > 5 ) {
		       		break;
		       	}
		    	safety++;
		    	assert safety < 100;
		       }
		       result = counter;
		                    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}

	@DisplayName( "continue sentinel" )
	@Test
	public void testContinueSentinel() {

		instance.executeSource(
		    """
		    result=0
		    n = 10;
		    for ( i = 1; i <= n; ++i ) {
		    	if ( i > 5 ) {
		    		continue;
		    	}
		    	result = i;
		    }
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 5 );

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
		assertThat( t.getMessage() ).contains( "Unterminated" );

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    foo = 'unfinished
		     """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Unterminated" );
	}

	@Test
	public void testDeclareHugeStringLiteral() {
		String hugeString = "Hello World abc".repeat( 10000 );
		instance.executeSource(
		    hugeString,
		    context, BoxSourceType.CFTEMPLATE );
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

	@DisplayName( "It should throw BoxRuntimeException in CF" )
	@Test
	public void testBoxRuntimeExceptionCF() {

		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    throw "test"
		     """,
		    context, BoxSourceType.CFSCRIPT ) );
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
		    context ) );
		assertThat( t.getMessage() ).contains( "Unterminated hash" );

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

	@Test
	public void testSwitchMultipleCase() {

		instance.executeSource(
		    """
		      myVar = 'a';
		      result = '';

		      switch(myVar) {
		      	case 'a':
		      	case 'b':
		      	case 'c':
		     result &= 'fall through1';
		      	case 'd':
		      	case undefinedVar: // Lucee throws an exception here, ACF does not.
		      		result &= 'fall through2';
		    break;
		      	default:
		      		result &= 'default';
		      }

		        """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "fall through1fall through2" );
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

	@Test
	public void testKeywords() {

		instance.executeSource(
		    """
		    					  result = {
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
		      or();
		    							   """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		var str = variables.getAsStruct( result );
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
		assertThat( str.dereferenceAndInvoke( context, Key.of( "component" ), new Object[] {}, false ) )
		    .isEqualTo( "component" );
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
		assertThat( str.dereferenceAndInvoke( context, Key.of( "interface" ), new Object[] {}, false ) )
		    .isEqualTo( "interface" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "instanceof" ), new Object[] {}, false ) )
		    .isEqualTo( "instanceof" );
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
		assertThat( str.dereferenceAndInvoke( context, Key.of( "savecontent" ), new Object[] {}, false ) )
		    .isEqualTo( "savecontent" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "setting" ), new Object[] {}, false ) ).isEqualTo( "setting" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "static" ), new Object[] {}, false ) ).isEqualTo( "static" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "string" ), new Object[] {}, false ) ).isEqualTo( "string" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "struct" ), new Object[] {}, false ) ).isEqualTo( "struct" );
		// assertThat( str.dereferenceAndInvoke( context, Key.of( "switch" ), new
		// Object[] {}, false ) ).isEqualTo( "switch" );
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

	@Test
	public void testKeywordsCF() {

		instance.executeSource(
		    """
		                          result = {
		            				abstract : ()=>'abstract',
		            				abort : ()=>'abort',
		            				admin : ()=>'admin',
		            				any : ()=>'any',
		            				array : ()=>'array',
		            				as : ()=>'as',
		            				assert : ()=>'assert',
		            				boolean : ()=>'boolean',
		            				break : ()=>'break',
		            				case : ()=>'case',
		            				castas : ()=>'castas',
		            				catch : ()=>'catch',
		            				class : ()=>'class',
		            				component : ()=>'component',
		            				contain : ()=>'contain',
		            				contains : ()=>'contains',
		            				continue : ()=>'continue',
		            				default : ()=>'default',
		            				does : ()=>'does',
		            				do : ()=>'do',
		            				else : ()=>'else',
		            				elif : ()=>'elif',
		            				false : ()=>'false',
		            				finally : ()=>'finally',
		            				for : ()=>'for',
		            				function : ()=>'function',
		            				greater : ()=>'greater',
		            				if : ()=>'if',
		            				in : ()=>'in',
		            				import : ()=>'import',
		            				include : ()=>'include',
		            				interface : ()=>'interface',
		            				instanceof : ()=>'instanceof',
		            				is : ()=>'is',
		            				java : ()=>'java',
		            				less : ()=>'less',
		            				local : ()=>'local',
		            				lock : ()=>'lock',
		            				mod : ()=>'mod',
		            				message : ()=>'message',
		            				new : ()=>'new',
		            				null : ()=>'null',
		            				numeric : ()=>'numeric',
		            				package : ()=>'package',
		            				param : ()=>'param',
		            				private : ()=>'private',
		            				property : ()=>'property',
		            				public : ()=>'public',
		            				query : ()=>'query',
		            				remote : ()=>'remote',
		            				required : ()=>'required',
		            				request : ()=>'request',
		            				return : ()=>'return',
		            				rethrow : ()=>'rethrow',
		            				savecontent : ()=>'savecontent',
		            				setting : ()=>'setting',
		            				static : ()=>'static',
		            				string : ()=>'string',
		            				struct : ()=>'struct',
		            				//switch : ()=>'switch',
		            				than : ()=>'than',
		            				to : ()=>'to',
		            				thread : ()=>'thread',
		            				throw : ()=>'throw',
		            				type : ()=>'type',
		            				true : ()=>'true',
		            				try : ()=>'try',
		            				var : ()=>'var',
		            				when : ()=>'when',
		            				while : ()=>'while',
		            				xor : ()=>'xor',
		            				eq : ()=>'eq',
		            				eqv : ()=>'eqv',
		            				imp : ()=>'imp',
		            				and : ()=>'and',
		            				eq : ()=>'eq',
		            				equal : ()=>'equal',
		            				gt : ()=>'gt',
		            				gte : ()=>'gte',
		            				ge : ()=>'ge',
		            				lt : ()=>'lt',
		            				lte : ()=>'lte',
		            				le : ()=>'le',
		            				neq : ()=>'neq',
		            				not : ()=>'not',
		            				or : ()=>'or'

		                       };


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
		      or();
		                                   """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		var str = variables.getAsStruct( result );
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
		assertThat( str.dereferenceAndInvoke( context, Key.of( "component" ), new Object[] {}, false ) )
		    .isEqualTo( "component" );
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
		assertThat( str.dereferenceAndInvoke( context, Key.of( "interface" ), new Object[] {}, false ) )
		    .isEqualTo( "interface" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "instanceof" ), new Object[] {}, false ) )
		    .isEqualTo( "instanceof" );
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
		assertThat( str.dereferenceAndInvoke( context, Key.of( "savecontent" ), new Object[] {}, false ) )
		    .isEqualTo( "savecontent" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "setting" ), new Object[] {}, false ) ).isEqualTo( "setting" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "static" ), new Object[] {}, false ) ).isEqualTo( "static" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "string" ), new Object[] {}, false ) ).isEqualTo( "string" );
		assertThat( str.dereferenceAndInvoke( context, Key.of( "struct" ), new Object[] {}, false ) ).isEqualTo( "struct" );
		// assertThat( str.dereferenceAndInvoke( context, Key.of( "switch" ), new
		// Object[] {}, false ) ).isEqualTo( "switch" );
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

	@DisplayName( "BL structKeyExists" )
	@Test
	public void testBLStructKeyExists() {

		instance.executeSource(
		    """
		       str = {
		    	foo : 'bar',
		    	baz : null
		    };
		    result = structKeyExists( str, "foo" )
		    result2 = structKeyExists( str, "baz" )
		    	 """,
		    context );

		assertThat( variables.getAsBoolean( result ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "result2" ) ) ).isTrue();

	}

	@DisplayName( "CF transpile structKeyExists" )
	@Test
	public void testCFTranspileStructKeyExists() {

		instance.executeSource(
		    """
		       str = {
		    	foo : 'bar',
		    	baz : null
		    };
		    result = structKeyExists( str, "foo" )
		    result2 = structKeyExists( str, "baz" )
		    	 """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.getAsBoolean( result ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "result2" ) ) ).isFalse();

	}

	@Test
	public void numberKey() {

		instance.executeSource(
		    """
		    local.5 = {minimumMinor = 2, minimumPatch = 1, minimumBuild = 9};
		     """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( Key.of( "local" ) ) ).isInstanceOf( Struct.class );
		IStruct localStr = variables.getAsStruct( Key.of( "local" ) );
		assertThat( localStr.get( Key.of( 5 ) ) ).isInstanceOf( Struct.class );
		IStruct fiveStr = localStr.getAsStruct( Key.of( 5 ) );
		assertThat( fiveStr.get( Key.of( "minimumMinor" ) ) ).isEqualTo( 2 );
	}

	@Test
	public void testTagCommentInScript() {
		// This is a CF-only workaround
		instance.executeSource(
		    """
		       foo = "bar"
		    <!--- I really don't belong --->
		    baz = "bum";
		        """,
		    context, BoxSourceType.CFSCRIPT );

	}

	@Test
	public void breakFromFunction() {

		instance.executeSource(
		    """
		    test = [ 1,2,3,4,5,6,7,8 ];
		    result = "";
		    test.each( ( a ) => {
		    	result &= a;
		    	if( a > 4 ){
		    		break;
		    	}
		    	result &= "after";
		    } )
		     """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "1after2after3after4after5678" );
	}

	@Test
	public void continueFromFunction() {

		instance.executeSource(
		    """
		    test = [ 1,2,3,4,5,6,7,8 ];
		    result = "";
		    test.each( ( a ) => {
		    	result &= a;
		    	if( a > 4 ){
		    		continue;
		    	}
		    	result &= "after";
		    } )
		     """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "1after2after3after4after5678" );
	}

	@Test
	public void continueFromFunctionJustKiddingItsALoop() {

		instance.executeSource(
		    """
		    test = [ 1,2,3,4,5,6,7,8 ];
		    result = "";
		    test.each( ( a ) => {
		    	result &= a;
		    	if( a > 4 ){
		    		while( false ) {
		    			continue;
		    		}
		    	}
		    	result &= "after";
		    } )
		     """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "1after2after3after4after5after6after7after8after" );
	}

	@Test
	public void whileNoCurlies() {

		instance.executeSource(
		    """
		    result = "";
		    	   a=0;
		       do result=result.listAppend( a++ );
		       while (a<5);
		    			""",
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "0,1,2,3,4" );
	}

	@Test
	public void whileNoCurliesNoSemi() {

		instance.executeSource(
		    """
		    result = "";
		    	   a=0;
		       do result=result.listAppend( a++ )
		       while (a<5);
		    			""",
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "0,1,2,3,4" );
	}

	@Test
	public void statementBlocks() {

		instance.executeSource(
		    """
		    result = "I ran"
		    {
		    	result &= " in a block";
		    }
		    {}{}{}{}{}{}
		    {{{{{{{
		    	result &= " with a lot of braces";
		    }}}}}}}
		    result &= "!";

		    			""",
		    context, BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isEqualTo( "I ran in a block with a lot of braces!" );
	}

	@Test
	public void commentParse() {

		String			comment	= """
		                          /****
		                           * Global Getters *
		                           ****/
		                           """;
		ParsingResult	result;
		try {
			result = new DocParser().parse( null, comment );
			assertThat( result.getRoot().toString().trim() ).isEqualTo( comment.trim() );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void commentParseInline() {

		String			comment	= """
		                          /** foo */
		                           """;
		ParsingResult	result;
		try {
			result = new DocParser().parse( null, comment );
			assertThat( result.getRoot().toString().trim() ).isEqualTo( comment.trim() );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void commentParseSmoll() {

		String			comment	= """
		                          /**
		                           * foo */
		                           """;
		ParsingResult	result;
		try {
			result = new DocParser().parse( null, comment );
			assertThat( result.getRoot().toString().trim() ).isEqualTo( comment.trim() );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void unicode() {

		instance.executeSource(
		    """
		    	include "src/test/java/TestCases/phase1/unicode.cfm";
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "kōwhai" );

	}

	@Test
	public void testThrowStatment() {

		instance.executeSource(
		    """
		    f = function () {
		    	throw("m")
		    }
		    	""",
		    context, BoxSourceType.CFSCRIPT );

	}

	@Test
	public void testJavaProxyInitSetsInstance() {

		instance.executeSource(
		    """
		    x = createObject("java", "java.lang.StringBuffer");
		    x.init(javaCast("int", 500));
		    result = x.toString();

		    y = createObject("java", "java.lang.String");
		    y.init("test");
		    result2 = y.toString();
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "test" );

	}

	@Test
	public void testVarDecalarationCF() {

		instance.executeSource(
		    """
		    	function foo() {
		    		var foo
		    		var bar;
		    		return local;
		    	}
		    	result = foo();
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo" ) ) ).isNull();
		assertThat( variables.getAsStruct( result ).get( Key.of( "bar" ) ) ).isNull();
	}

	@Test
	public void testVarDecalarationBL() {

		instance.executeSource(
		    """
		    	function foo() {
		    		var foo
		    		var bar;
		    		return local;
		    	}
		    	result = foo();
		    """,
		    context );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo" ) ) ).isNull();
		assertThat( variables.getAsStruct( result ).get( Key.of( "bar" ) ) ).isNull();

	}

	@Test
	public void testNestedAmbiguousIf() {

		instance.executeSource(
		    """
		    	function foo( boolean condition ){
		    		if ( condition )
		    			return "first";
		    		else
		    			return "second";
		    	}
		    	result1 = foo( true )
		    	result2 = foo( false )
		    """,
		    context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "first" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "second" );
	}

	// Should we really allow this in Bl, or remove it in the transpiler?
	@Test
	public void testExtraHashesInAssignmentLHS() {

		instance.executeSource(
		    """
		    i = 1;
		    	#result# = "FORM." & #i#
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "FORM.1" );
	}

	@Test
	public void testExtraHashesInAssignmentLHSCF() {

		instance.executeSource(
		    """
		    i = 1;
		    	#result# = "FORM." & #i#
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "FORM.1" );
	}

	@Test
	public void testJavaMethodReference() {

		instance.executeSource(
		    """
		       	 import java:java.lang.String;
		       	 javaStatic = java.lang.String::valueOf;
		       	 result = javaStatic( "test" )

		       	 javaInstance = result.toUpperCase
		       	 result2 = javaInstance()

		       	 import java.util.Collections;
		       	 result3 = [ 1, 7, 3, 99, 0 ].sort( Collections.reverseOrder().compare  )

		       	 import java:java.lang.Math;
		       	 result4 = [ 1, 2.4, 3.9, 4.5 ].map( Math::floor )

		       // Use the compare method from the Java reverse order comparator to sort a BL array
		       [ 1, 7, 3, 99, 0 ].sort( Collections.reverseOrder()  )

		    import java.util.function.Predicate;
		    isBrad = Predicate.isEqual( "brad" ) castas "function:Predicate"
		    result5 = isBrad( "brad" )
		    result6 = isBrad( "luis" )
		    result7 = [ "brad", "luis", "jon" ].filter( isBrad )

		       	   """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test" );

		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "TEST" );

		Array result3 = variables.getAsArray( Key.of( "result3" ) );
		assertThat( result3 ).isEqualTo( Array.of( 99, 7, 3, 1, 0 ) );

		Array result4 = variables.getAsArray( Key.of( "result4" ) );
		assertThat( result4 ).isEqualTo( Array.of( 1.0, 2.0, 3.0, 4.0 ) );

		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( Array.of( "brad" ) );
	}

	@Test
	public void testFunctionalBIFAccess() {

		instance.executeSource(
		    """
		       	foo = ::ucase;
		             result = foo( "test" );

		          result2 = ["brad","luis","jon"].map( ::ucase );
		          result3 = [1.2, 2.3, 3.4].map( ::ceiling ).map( .intValue );
		          result4 = ["brad","luis","jon"].map( ::hash ); // MD5

		       result5 = (::reverse)( "darb" );

		    result6 = queryNew( "name,position", "varchar,varchar", [ ["Luis","CEO"], ["Jon","Architect"], ["Brad","Chaos Monkey"] ])
		         .reduce( ::arrayAppend, [] )

		          		  """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "TEST" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( Array.of( "BRAD", "LUIS", "JON" ) );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( Array.of( 2, 3, 4 ) );
		assertThat( variables.get( Key.of( "result4" ) ) )
		    .isEqualTo( Array.of( "884354eb56db3323cbce63a5e177ecac", "502ff82f7f1f8218dd41201fe4353687",
		        "006cb570acdab0e0bfc8e3dcb7bb4edf" ) );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo(
		    Array.of(
		        Struct.of( "NAME", "Luis", "POSITION", "CEO" ),
		        Struct.of( "NAME", "Jon", "POSITION", "Architect" ),
		        Struct.of( "NAME", "Brad", "POSITION", "Chaos Monkey" ) ) );
	}

	@Test
	public void testFunctionalMemberAccess() {

		instance.executeSource(
		    """
		      foo = .ucase;
		      result = foo( "test" );

		      result2 = ["brad","luis","jon"].map( .ucase );
		      result3 = [1.2, 2.3, 3.4].map( .ceiling ).map( .intValue );
		      result4 = [
		      	{
		      		myFunc : ()->"eric"
		      	},
		      	{
		      		myFunc : ()->"gavin"
		      	}
		      ].map( .myFunc )

		      result5 = (.reverse)( "darb" );

		      nameGetter = .name
		      data = { name : "brad", hair : "red" }

		      result6 = nameGetter( data ) // brad

		      result7 = queryNew(
		    "name,country",
		    "varchar,varchar",
		    [
		    	["Luis","El Salvador"],
		    	["Jon","US"],
		    	["Brad","US"],
		    	["Eric","US"],
		    	["Jorge","Switzerland"],
		    	["Majo","El Salvador"],
		    	["Jaime","El Salvador"],
		    	["Esme","Mexico"]
		    ])
		    .toStructArray()
		    .map( .name )

		      	""",
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "TEST" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( Array.of( "BRAD", "LUIS", "JON" ) );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( Array.of( 2, 3, 4 ) );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( Array.of( "eric", "gavin" ) );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result7" ) ) )
		    .isEqualTo( Array.of( "Luis", "Jon", "Brad", "Eric", "Jorge", "Majo", "Jaime", "Esme" ) );
	}

	@Test
	public void testFunctionalMemberAccessArgs() {

		instance.executeSource(
		    """
		       foo = .ucase();
		       result = foo( "test" );

		       foo = .left(1);
		       result2 = foo( "test" );

		       result3 = ["brad","luis","jon"].map( .left(1) );

		       function createFunc() {
		    	   local.prefix = "_";
		    	   return .reReplace('.*', prefix & argProducer() );
		       }
		       func = createFunc();
		       args = [ "first", "second", "third" ]
		       function argProducer() {
		    	   nextArg = args.first();
		    	   args.deleteAt( 1 );
		    	   return nextArg;
		       }
		       // args re-evaluated for each method invocation.  Lexical access to declaring context
		       result4 = ["brad","luis","jon"].map( func );

		    suffix = " Sr."
		       result5 = ["brad","luis","jon"].map( .concat(suffix) );
		    				 """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "TEST" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "t" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( Array.of( "b", "l", "j" ) );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( Array.of( "_first", "_second", "_third" ) );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( Array.of( "brad Sr.", "luis Sr.", "jon Sr." ) );
	}

	@Test
	public void testFunctionalMemberAccessArgsNamed() {

		instance.executeSource(
		    """

		    foo = .left(count=1);
		    result2 = foo( "test" );

		    result3 = ["brad","luis","jon"].map( .left(count=1) );

		    function createFunc() {
		     local.prefix = "_";
		     return .reReplace(regex='.*', substring=prefix & argProducer() );
		    }
		    func = createFunc();
		    args = [ "first", "second", "third" ]
		    function argProducer() {
		     nextArg = args.first();
		     args.deleteAt( 1 );
		     return nextArg;
		    }
		    // args re-evaluated for each method invocation.  Lexical access to declaring context
		    result4 = ["brad","luis","jon"].map( func );

		    	 """,
		    context );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "t" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( Array.of( "b", "l", "j" ) );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( Array.of( "_first", "_second", "_third" ) );
	}

	@Test
	public void testJavaStreams() {

		instance.executeSource(
		    """
		    import java.util.stream.Collectors;

		    result = myQry = queryNew(
		    "name,country",
		    "varchar,varchar",
		    [
		    	["Luis","El Salvador"],
		    	["Jon","US"],
		    	["Brad","US"],
		    	["Eric","US"],
		    	["Jorge","Switzerland"],
		    	["Majo","El Salvador"],
		    	["Jaime","El Salvador"],
		    	["Esme","Mexico"]
		    ])
		    .stream()
		    .parallel()
		    .collect(
		    	Collectors.groupingBy( .country,
		    	Collectors.mapping( .name,
		    		Collectors.toList()  )
		    	) );

		    // {El Salvador=[Luis, Majo, Jaime], Mexico=[Esme], Switzerland=[Jorge], US=[Jon, Brad, Eric]}
		    println( result )

		    	 """,
		    context );
	}

	@Test
	public void testBigNumberLiterals() {

		instance.executeSource(
		    """
		    result = 1111111111111111111 == 2222222222222222222;
		    	 """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@Test
	public void testBigDecimalToJavaMethod() {

		instance.executeSource(
		    """
		       import java.lang.Math;
		    num = 1.2;
		    type = num.getClass().getName();
		    	  result = Math.ceil( num );
		    		   """,
		    context );
		assertThat( variables.get( Key.of( "type" ) ) ).isEqualTo( "java.math.BigDecimal" );
		assertThat( variables.get( result ) ).isInstanceOf( Double.class );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@Test
	public void testArrayAccessOnMethod() {

		instance.executeSource(
		    """
		    function getData() {
		    	return [ "brad", "luis", "jon" ];
		    }
		    result = variables.getData()[ 2 ];
		    	 """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "luis" );
	}

	@Test
	public void testVariableNamedVar() {

		instance.executeSource(
		    """
		    foo.var= "bar";
		    result = foo.var.toString();
		    	 """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@Test
	// @Disabled( "BL-447 WIP" )
	public void testBigIntegerToJavaMethod() {

		instance.executeSource(
		    """
		    import java.math.BigInteger
		    a = BigInteger.valueOf( 54 )
		    result = a.add( 444 )
		      """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( BigInteger.class );
		assertThat( variables.get( result ) ).isEqualTo( BigInteger.valueOf( 498 ) );
	}

	@Test
	public void testAssginmentModifierCF() {

		instance.executeSource(
		    """
		    function func(){
		    	foo = "bar";
		    	var foo2 = "bar";
		    	final foo3 = "bar";
		    	final var foo4 = "bar";
		    	var final foo5 = "bar";
		    	return local;
		    }
		    result = func();
		    		  """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo2" ) ) ).isEqualTo( "bar" );
		assertThat( variables.get( Key.of( "foo3" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo4" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo5" ) ) ).isEqualTo( "bar" );
	}

	@Test
	public void testAssginmentModifier() {
		instance.executeSource(
		    """
		    function func(){
		    	foo = "bar";
		    	var foo2 = "bar";
		    	final foo3 = "bar";
		    	final var foo4 = "bar";
		    	var final foo5 = "bar";
		    	return local;
		    }
		    result = func();
		    		  """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo2" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo3" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo4" ) ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( result ).get( Key.of( "foo5" ) ) ).isEqualTo( "bar" );
	}

	@Test
	public void testNonFinalFunction() {
		instance.executeSource(
		    """
		       function func(){
		    	return "funca";
		       }
		    include "src/test/java/TestCases/phase1/functionDeclare.bxs";
		    result = func();
		    			 """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "funcb" );
	}

	@Test
	public void testFinalFunction() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final function func(){
		    }
		    include "src/test/java/TestCases/phase1/functionDeclare.bxs";
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot override final function" );
	}

	@Test
	public void testVarStaticModifierValidation() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    var foo = "bar"
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Scope [local] is not available in this context" );

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    static foo = "bar"
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Scope [static] is not available in this context" );
	}

	@Test
	public void testFinalModifier() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final foo = "bar"
		    foo = "baz"
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot reassign final key" );
		setupEach();

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final variables.foo = "bar"
		    foo = "baz"
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot reassign final key" );
		setupEach();

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final foo = "bar"
		    variables.foo = "baz"
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot reassign final key" );
		setupEach();

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final variables.foo = "bar"
		    variables.foo = "baz"
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot reassign final key" );
		setupEach();

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final foo = "bar"
		    structDelete( variables, "foo" )
		     """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot delete final key" );
		setupEach();

		instance.executeSource(
		    """
		    final foo = "bar"
		    variables.$bx.meta.finalKeySet.clear()
		    foo = "baz"
		     """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "baz" );
		setupEach();

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    final function foo() {}
		    foo = "bar"
		        """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Cannot override final function" );
	}

	@Test
	public void testFinalImmutable() {
		instance.executeSource(
		    """
		    final lockDown = [ 1, 2, 3 ].toImmutable()
		    """,
		    context, BoxSourceType.BOXSCRIPT );
	}

	@Test
	public void testAdobeDoubleDot() {
		instance.executeSource(
		    """
		    foo.bar = "baz"
		    result = foo..bar // Adobe allows this bad code
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "baz" );
	}

	@Test
	public void testAdobeMissingCommas() {
		instance.executeSource(
		    """
		    // Adobe allows this bad code with missing commas
		       cfhttp( url="http://www.google.com" method="get", timeout=20 result="result" )
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
	}

	@Test
	public void testVarsStartWithNumberCF() {
		instance.executeSource(
		    """
		    foo.50foo = "bar";
		    result = foo.50foo;
		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );

		instance.executeSource(
		    """
		    foo.50brad = ()->"wood";
		    result = foo.50brad();
		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "wood" );

		Throwable t = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    50foo = "bar";
		      """,
		    context, BoxSourceType.CFSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Identifier name cannot start with a number" );
	}

	@Test
	public void testVarsStartWithNumber() {
		instance.executeSource(
		    """
		    foo.50foo = "bar";
		    result = foo.50foo;
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );

		instance.executeSource(
		    """
		    foo.50brad = ()->"wood";
		    result = foo.50brad();
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "wood" );

		Throwable t = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    50foo = "bar";
		      """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Identifier name cannot start with a number" );
	}

	@Test
	public void testLiteralStatementsCF() {
		instance.executeSource(
		    """
		    50;
		    "brad";
		    true;
		    null;
		    5.6;
		    [1,2,3];
		    { foo : "bar" };
		    	   """,
		    context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testLiteralStatements() {
		instance.executeSource(
		    """
		    50;
		    "brad";
		    true;
		    null;
		    5.6;
		    [1,2,3];
		    { foo : "bar" };
		    	   """,
		    context, BoxSourceType.BOXSCRIPT );
	}

	@Test
	public void testNumericLiteralSeparators() {
		instance.executeSource(
		    """
		    		result1 = 5_000
		    		result2 = 5_000.000_4
		    		result3 = .1_2
		    		result4 = 1.2_3
		    		result5 = 1_2.3_4e5_6
		    		result6 = 1_2_3_4_5_6_8
		    		str = {
		    			1_0 : "brad"
		    		}
		    		result7 = str[ 10 ]
		    		result8 = str.1_0
		    		str2 = {
		    			'1_0' : "brad"
		    		}
		    		result9 = str2[ '1_0' ]
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 5000 );
		assertThat( variables.getAsNumber( Key.of( "result2" ) ).doubleValue() ).isEqualTo( 5000.0004 );
		assertThat( variables.getAsNumber( Key.of( "result3" ) ).doubleValue() ).isEqualTo( 0.12 );
		assertThat( variables.getAsNumber( Key.of( "result4" ) ).doubleValue() ).isEqualTo( 1.23 );
		assertThat( variables.getAsNumber( Key.of( "result5" ) ).doubleValue() ).isEqualTo( 12.34e56 );
		assertThat( variables.getAsNumber( Key.of( "result6" ) ).doubleValue() ).isEqualTo( 1234568 );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "brad" );

		assertThrows( KeyNotFoundException.class, () -> instance.executeSource(
		    """
		        result1 = _5
		    """,
		    context, BoxSourceType.BOXSCRIPT ) );

		Throwable t = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		        result1 = 5_
		    """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "Identifier name cannot start with a number" );
	}

	@Test
	public void testDeepNestedExpressions() {
		instance.executeSource(
		    """
		    foo = true;
		    	  result = NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo;
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		// If this completes without handing the parser, we're basically good
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@Test
	public void testDeepNestedExpressionsCF() {
		instance.executeSource(
		    """
		    foo = true;
		    	  result = NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo
		    OR NOT foo;
		      """,
		    context, BoxSourceType.CFSCRIPT );
		// If this completes without handing the parser, we're basically good
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@Test
	public void testLeadingZeros() {
		instance.executeSource(
		    """
		       result = 08;
		    result2 = 08.5;

		    	 """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( 8 );
		assertThat( variables.getAsNumber( Key.of( "result2" ) ).doubleValue() ).isEqualTo( 8.5 );
	}

	@Test
	public void testRethrowStructedThrowable() {
		Throwable t = assertThrows( CustomException.class, () -> instance.executeSource(
		    """
		    function reThrowMe( required struct err ) {
		    	throw object=err;
		    }
		    try {
		    	1/0;
		    } catch( any e ) {
		    	reThrowMe( e );
		    }
		    	 """,
		    context, BoxSourceType.BOXSCRIPT ) );
		assertThat( t.getMessage() ).contains( "zero" );
	}

	@Test
	public void testXMLInStringBuffer() {
	// @formatter:off
	instance.executeSource(
		"""
			buffer = createObject( "java", "java.lang.StringBuffer" ).init();
			buffer.append( '<cfcomponent>' );

			buffer.append( '<\\cfcomponent>' );

			result = buffer.toString();
			println( result );
		""",
		context, BoxSourceType.CFSCRIPT
	);
	// @formatter:on
	}

	@Test
	public void testExecuteTemplate() {
		instance.executeTemplate(
		    "src/test/java/TestCases/phase1/files/index.bxs",
		    new String[] {}
		);
	}

	@Test
	public void testExecuteClass() {
		instance.executeTemplate(
		    "src/test/java/TestCases/phase1/files/Runner.bx",
		    new String[] { "myArg" }
		);
	}

	@Test
	public void testHigherOrderClosure() {
	// @formatter:off
	instance.executeSource(
		"""
			fullName = ( fname ) => ( lname ) => "#fname# #lname#";
			result = fullName( "John" )( "Doe" );
		""",
		context, BoxSourceType.BOXSCRIPT
	);
	// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "John Doe" );
	}

	@Test
	public void testHigherOrderClosureCF() {
	// @formatter:off
	instance.executeSource(
		"""
			fullName = ( fname ) => ( lname ) => "#fname# #lname#";
			result = fullName( "John" )( "Doe" );
		""",
		context, BoxSourceType.CFSCRIPT
	);
	// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "John Doe" );
	}

	@Test
	public void testTagContextLineMapping() {
		// @formatter:off
		instance.executeSource(
			"""
			tagContext = [];
			try {
				include "src/test/java/TestCases/phase1/TagContextLineMapping.bxs";
				foo()
			} catch( any e ) {
				tagContext = e.tagContext;
				e.printStackTrace();
				println(tagContext)
			}
			""",
			context, BoxSourceType.CFSCRIPT
		);
		// @formatter:on
		assertThat( variables.get( Key.of( "tagContext" ) ) ).isInstanceOf( Array.class );
		Array tagContext = variables.getAsArray( Key.of( "tagContext" ) );
		assertThat( tagContext.size() ).isGreaterThan( 0 );
		assertThat( tagContext.get( 0 ) ).isInstanceOf( IStruct.class );
		IStruct tagContextStruct = ( IStruct ) tagContext.get( 0 );
		assertThat( tagContextStruct.get( Key.of( "line" ) ) ).isEqualTo( 2 );
		assertThat( tagContextStruct.getAsString( Key.of( "template" ) ) ).ignoringCase().contains( "TagContextLineMapping.bxs" );
		String code[] = tagContextStruct.getAsString( Key.of( "codePrintPlain" ) ).split( "\n" );
		assertThat( code[ 1 ] ).contains( "for( foo in null ) {" );
	}

	@Test
	public void testFindJavaConstructorWithNulls() {
	// @formatter:off
	instance.executeSource(
		"""
		import java.net.URI;
		new URI( null, null, "", null, null );
		""",
		context, BoxSourceType.BOXSCRIPT
	);
	// @formatter:on
	}

}
