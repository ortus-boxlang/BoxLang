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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class OperatorsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			resultKey	= new Key( "result" );
	static Key			tmpKey		= new Key( "tmp" );

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

	@DisplayName( "string concat" )
	@Test
	public void testStringConcat() {
		Object result = instance.executeStatement( "'brad' & 'wood'", context );
		assertThat( result ).isEqualTo( "bradwood" );

	}

	@DisplayName( "multi string concat" )
	@Test
	public void testMutliStringConcat() {
		Object result = instance.executeStatement( "'foo' & 'bar' & 'baz' & 'bum'", context );
		assertThat( result ).isEqualTo( "foobarbazbum" );
	}

	@DisplayName( "concat and eq" )
	@Test
	public void testConcatAndEQ() {
		Object result = instance.executeStatement( "'$' & 'foo' == '$foo'", context );
		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "string contains" )
	@Test
	public void testStringContains() {
		Object result = instance.executeStatement( "\"Brad Wood\" contains \"Wood\"", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "\"Brad Wood\" contains \"luis\"", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "\"Brad Wood\" DOES NOT CONTAIN \"Luis\"", context );
		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "math addition" )
	@Test
	public void testMathAddition() {
		Object result = instance.executeStatement( "+5", context );
		assertThat( result ).isEqualTo( 5 );

		result = instance.executeStatement( "5+5", context );
		assertThat( result ).isEqualTo( 10 );

		result = instance.executeStatement( "'5'+'2'", context );
		assertThat( result ).isEqualTo( 7 );
	}

	@DisplayName( "math subtraction" )
	@Test
	public void testMathSubtraction() {
		Object result = instance.executeStatement( "6-5", context );
		assertThat( result ).isEqualTo( 1 );
	}

	@DisplayName( "math negation" )
	@Test
	public void testMathNegation() {
		Object result = instance.executeStatement( "-5", context );
		assertThat( result ).isEqualTo( -5 );

		result = instance.executeStatement( "-(5+5)", context );
		assertThat( result ).isEqualTo( -10 );

		result = instance.executeStatement( "-5+5", context );
		assertThat( result ).isEqualTo( 0 );
	}

	@DisplayName( "math addition var" )
	@Test
	public void testMathAdditionVar() {
		Object result = instance.executeStatement( "foo=5; +foo", context );
		assertThat( result ).isEqualTo( 5 );
	}

	@DisplayName( "math negation var" )
	@Test
	public void testMathNegationVar() {
		Object result = instance.executeStatement( "foo=5; -foo", context );
		assertThat( result ).isEqualTo( -5 );
	}

	@DisplayName( "math division" )
	@Test
	public void testMathDivision() {
		Object result = instance.executeStatement( "10/5", context );
		assertThat( result ).isEqualTo( 2 );
	}

	@DisplayName( "math int dividsion" )
	@Test
	public void testMathIntDivision() {
		Object result = instance.executeStatement( "10\\3", context );
		assertThat( result ).isEqualTo( 3 );
	}

	@DisplayName( "math multiplication" )
	@Test
	public void testMathMultiplication() {
		Object result = instance.executeStatement( "10*5", context );
		assertThat( result ).isEqualTo( 50 );
	}

	@DisplayName( "math power" )
	@Test
	public void testMathPower() {
		Object result = instance.executeStatement( "2^3", context );
		assertThat( result ).isEqualTo( 8 );
	}

	@DisplayName( "math plus plus literals" )
	@Test
	public void testMathPlusPlusLiterals() {
		Object result = instance.executeStatement( "5++", context );
		assertThat( result ).isEqualTo( 5 );

		result = instance.executeStatement( "++5", context );
		assertThat( result ).isEqualTo( 6 );

		result = instance.executeStatement( "result=5++", context );
		assertThat( result ).isEqualTo( 5 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );

		result = instance.executeStatement( "result=++5", context );
		assertThat( result ).isEqualTo( 6 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );

	}

	@DisplayName( "math plus plus parenthetical" )
	@Test
	public void testMathPlusPlusParenthetical() {
		Object result = instance.executeStatement( "(5)++", context );
		assertThat( result ).isEqualTo( 5 );

		result = instance.executeStatement( "++(5)", context );
		assertThat( result ).isEqualTo( 6 );

		result = instance.executeStatement( "result=(5)++", context );
		assertThat( result ).isEqualTo( 5 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );

		result = instance.executeStatement( "result=++(5)", context );
		assertThat( result ).isEqualTo( 6 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );

		instance.executeSource( """
		                        myvar = 5;
		                        result = ++(myvar);
		                        """, context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );
		assertThat( variables.get( Key.of( "myvar" ) ) ).isEqualTo( 6 );

		instance.executeSource( """
		                        myvar = 5;
		                        result = (myvar)++;
		                        """, context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "myvar" ) ) ).isEqualTo( 6 );

	}

	@DisplayName( "math plus plus other" )
	@Test
	public void testMathPlusPlusOther() {

		instance.executeSource( """
		                        function num() {
		                            return 5;
		                        }
		                        result = ++num();
		                        """, context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );

		instance.executeSource( """
		                        function num() {
		                            return 5;
		                        }
		                        result = num()++;
		                        """, context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );

	}

	@DisplayName( "math plus plus scoped" )
	@Test
	public void testMathPlusPlusScoped() {
		instance.executeSource(
		    """
		    tmp = 5;
		    result = variables.tmp++;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 6 );

		instance.executeSource(
		    """
		    tmp = 5;
		    result = ++variables.tmp;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 6 );
	}

	@DisplayName( "math plus plus invalid" )
	@Test
	public void testMathPlusPlusInvalid() {

		assertThrows( ExpressionException.class, () -> instance.executeSource( "variables++", context ) );

	}

	@DisplayName( "math plus plus unscoped" )
	@Test
	public void testMathPlusPlusUnScoped() {
		instance.executeSource(
		    """
		    tmp = 5;
		    result = tmp++;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 6 );

		instance.executeSource(
		    """
		    tmp = 5;
		    result = ++tmp;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 6 );

		instance.executeSource(
		    """
		       foo.bar.baz = 5;
		       result = foo.bar.baz++;
		    tmp = foo.bar.baz;
		       """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 6 );

		instance.executeSource(
		    """
		       foo.bar.baz = 5;
		       result = ++foo.bar.baz;
		    tmp = foo.bar.baz;
		       """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 6 );
	}

	@DisplayName( "math minus minus literals" )
	@Test
	public void testMathMinusMinusLiterals() {
		Object result = instance.executeStatement( "5--", context );
		assertThat( result ).isEqualTo( 5 );

		result = instance.executeStatement( "--5", context );
		assertThat( result ).isEqualTo( 4 );

		result = instance.executeStatement( "result=5--", context );
		assertThat( result ).isEqualTo( 5 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );

		result = instance.executeStatement( "result=--5", context );
		assertThat( result ).isEqualTo( 4 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );

	}

	@DisplayName( "math minus minus parenthetical" )
	@Test
	public void testMathMinusMinusParenthetical() {
		Object result = instance.executeStatement( "(5)--", context );
		assertThat( result ).isEqualTo( 5 );

		result = instance.executeStatement( "--(5)", context );
		assertThat( result ).isEqualTo( 4 );

		result = instance.executeStatement( "result=(5)--", context );
		assertThat( result ).isEqualTo( 5 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );

		result = instance.executeStatement( "result=--(5)", context );
		assertThat( result ).isEqualTo( 4 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );

		instance.executeSource( """
		                        myvar = 5;
		                        result = --(myvar);
		                        """, context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );
		assertThat( variables.get( Key.of( "myvar" ) ) ).isEqualTo( 4 );

		instance.executeSource( """
		                        myvar = 5;
		                        result = (myvar)--;
		                        """, context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "myvar" ) ) ).isEqualTo( 4 );

	}

	@DisplayName( "math minus minus scoped" )
	@Test
	public void testMathMinusMinusScoped() {
		instance.executeSource(
		    """
		    tmp = 5;
		    result = variables.tmp--;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 4 );

		instance.executeSource(
		    """
		    tmp = 5;
		    result = --variables.tmp;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 4 );
	}

	@DisplayName( "math minus minus unscoped" )
	@Test
	public void testMathMinusMinusUnScoped() {
		instance.executeSource(
		    """
		    tmp = 5;
		    result = tmp--;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 4 );

		instance.executeSource(
		    """
		    tmp = 5;
		    result = --tmp;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 4 );

		instance.executeSource(
		    """
		       foo.bar.baz = 5;
		       result = foo.bar.baz--;
		    tmp = foo.bar.baz;
		       """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 4 );

		instance.executeSource(
		    """
		       foo.bar.baz= 5;
		       result = --foo.bar.baz;
		    tmp = foo.bar.baz;
		       """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );
		assertThat( variables.get( tmpKey ) ).isEqualTo( 4 );
	}

	@DisplayName( "compound operator plus" )
	@Test
	public void compoundOperatorPlus() {
		instance.executeSource(
		    """
		    result = 5;
		    result += 5;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 10 );

		instance.executeSource(
		    """
		    function foo(){
		    	local.result = 5;
		    	local.result += 5;
		    	return result;
		    }
		    result = foo();
		       """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 10 );

		instance.executeSource(
		    """
		    result = 5;
		    variables.result += 5;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 10 );
	}

	@DisplayName( "compound operators minus" )
	@Test
	public void compoundOperatorMinus() {
		instance.executeSource(
		    """
		    result = 5;
		    result -= 4;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 1 );
	}

	@DisplayName( "compound operator multiply" )
	@Test
	public void compoundOperatorMultiply() {
		instance.executeSource(
		    """
		    result = 5;
		    result *= 5;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 25 );
	}

	@DisplayName( "compound operator divide" )
	@Test
	public void compoundOperatorDivide() {
		instance.executeSource(
		    """
		    result = 20;
		    result /= 5;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );
	}

	@DisplayName( "compound operator modulus" )
	@Test
	public void compoundOperatorModulus() {
		instance.executeSource(
		    """
		    result = 5;
		    result %= 4;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 1 );
	}

	@DisplayName( "modulus precedence" )
	@Test
	public void modulusPrecedence() {
		instance.executeSource(
		    """
		    result =  1 + 1 mod 2;
		    result2 =  1 + 1 % 2;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "compound operator concat" )
	@Test
	public void compoundOperatorConcat() {
		instance.executeSource(
		    """
		    result = "brad";
		    result &= "wood";
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( "bradwood" );
	}

	@DisplayName( "compound operator with var" )
	@Test
	public void compoundOperatorWithVar() {
		/*
		 * I personally think this should be invalid code, but Adobe and Lucee both allow it. Adobe turns all access to the variable into the local scope
		 * somehow, presumably some sort of variable hoisting. Lucee just straight up ignores the "var" keyword and modifies the variable in whatever scope
		 * it's already in. I've chosen Lucee's behavior for now because it's the least amount of work and this is an edge case.
		 */
		instance.executeSource(
		    """
		    result = "brad";
		    var result &= "wood";
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( "bradwood" );
	}

	@DisplayName( "logical and" )
	@Test
	public void testLogicalAnd() {
		Object result = instance.executeStatement( "true and true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true && true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true and false", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "true && false", context );
		assertThat( result ).isEqualTo( false );
	}

	@DisplayName( "logical or" )
	@Test
	public void testLogicalOr() {
		Object result = instance.executeStatement( "true or true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true || true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true or false", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true || false", context );
		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "logical not" )
	@Test
	public void testLogicalNot() {
		Object result = instance.executeStatement( "!true", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "!false", context );
		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "logical xor" )
	@Test
	public void testLogicalXOR() {
		Object result = instance.executeStatement( "true xor false", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true xor true", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "false xor false", context );
		assertThat( result ).isEqualTo( false );
	}

	@DisplayName( "elvis" )
	@Test
	public void testElvis() {

		instance.executeSource(
		    """
		    tmp = "brad"
		    result = tmp ?: 'default'
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( "brad" );

		Object result = instance.executeStatement( "null ?: 'default'", context );
		assertThat( result ).isEqualTo( "default" );

		result = instance.executeStatement( "foo ?: 'default'", context );
		assertThat( result ).isEqualTo( "default" );

		result = instance.executeStatement( "foo.bar ?: 'default'", context );
		assertThat( result ).isEqualTo( "default" );

		result = instance.executeStatement( "foo['bar'] ?: 'default'", context );
		assertThat( result ).isEqualTo( "default" );

		result = instance.executeStatement( "foo['bar'].baz ?: 'default'", context );
		assertThat( result ).isEqualTo( "default" );

		result = instance.executeStatement( "foo.bar() ?: 'default'", context );
		assertThat( result ).isEqualTo( "default" );

	}

	@DisplayName( "ternary" )
	@Test
	public void testTernary() {

		Object result = instance.executeStatement( "true ? 'itwastrue' : 'itwasfalse'", context );
		assertThat( result ).isEqualTo( "itwastrue" );

		result = instance.executeStatement( "FALSE ? 'itwastrue' : 'itwasfalse'", context );
		assertThat( result ).isEqualTo( "itwasfalse" );

		instance.executeSource(
		    """
		    tmp = true;
		    result = tmp ? 'itwastrue' : 'itwasfalse'
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( "itwastrue" );

	}

	@DisplayName( "It should lazily evaluate its true branche" )
	@Test
	public void testTernaryTrueLazyEvaluation() {

		Object result = instance.executeStatement( """
		                                           			"false" castas "boolean" ? "a" castas "boolean" : "0" castas "boolean";
		                                           """, context );
		assertThat( result ).isEqualTo( false );
	}

	@DisplayName( "It should lazily evaluate its false branche" )
	@Test
	public void testTernaryFalseLazyEvaluation() {

		Object result = instance.executeStatement( """
		                                           			"true" castas "boolean" ? "false" castas "boolean" : "x" castas "boolean";
		                                           """, context );
		assertThat( result ).isEqualTo( false );
	}

	@DisplayName( "It should properly handle comparison operators" )
	@Test
	public void testTernaryWithComparison() {

		Object result = instance.executeStatement( "4 < 5 ? 'itwastrue' : 'itwasfalse'", context );
		assertThat( result ).isEqualTo( "itwastrue" );

		result = instance.executeStatement( "4 > 5 ? 'itwastrue' : 'itwasfalse'", context );
		assertThat( result ).isEqualTo( "itwasfalse" );

		instance.executeSource(
		    """
		    tmp = true;
		    result = tmp == true ? 'itwastrue' : 'itwasfalse'
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( "itwastrue" );

	}

	@DisplayName( "instanceOf" )
	@Test
	public void testInstanceOf() {

		Object result = instance.executeStatement( "true instanceOf 'Boolean'", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'brad' instanceOf 'java.lang.String'", context );
		assertThat( result ).isEqualTo( true );

	}

	@DisplayName( "castAs" )
	@Test
	public void testCastAs() {
		assertThrows( KeyNotFoundException.class, () -> instance.executeStatement( "5 castAs sdf", context ) );

		Object result = instance.executeStatement( "5 castAs 'String'", context );
		assertThat( result ).isEqualTo( "5" );
		assertThat( result.getClass().getName() ).isEqualTo( "java.lang.String" );
	}

	@DisplayName( "assert" )
	@Test
	public void testAssert() {

		instance.executeStatement( "assert true", context );
		instance.executeStatement( "assert true;", context );

		instance.executeStatement( "assert 5==5", context );
		instance.executeStatement( "assert 5==5;", context );

		assertThrows( AssertionError.class, () -> instance.executeStatement( "assert 5==6", context ) );
		assertThrows( AssertionError.class, () -> instance.executeStatement( "assert false", context ) );

	}

	@DisplayName( "comparison equality" )
	@Test
	public void testComparisonEquality() {

		Object result = instance.executeStatement( "5==5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'5'==5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'brad'=='brad'", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'brad'==5", context );
		assertThat( result ).isEqualTo( false );

	}

	@DisplayName( "comparison strict equality" )
	@Test
	public void testComparisonStrictEquality() {

		Object result = instance.executeStatement( "5===5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'5'===5", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "'brad'==='brad'", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'brad'===5", context );
		assertThat( result ).isEqualTo( false );

	}

	@DisplayName( "comparison greater than" )
	@Test
	public void testGreaterThan() {

		Object result = instance.executeStatement( "6 > 5", context );
		assertThat( result ).isEqualTo( true );

		result	= instance.executeStatement( "6 GREATER THAN 5", context );

		result	= instance.executeStatement( "'B' > 'A'", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "'B' greater than 'A'", context );
		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "comparison greater than equal" )
	@Test
	public void testGreaterThanEqual() {

		Object result = instance.executeStatement( "10 >= 5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "10 GTE 5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "10 GREATER THAN OR EQUAL TO 5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "10 GE 5", context );
		assertThat( result ).isEqualTo( true );
	}

	@DisplayName( "comparison less than" )
	@Test
	public void testLessThan() {

		Object result = instance.executeStatement( "5 < 10", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LT 10", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LESS THAN 10", context );
		assertThat( result ).isEqualTo( true );

	}

	@DisplayName( "comparison less than equal" )
	@Test
	public void testLessThanEqual() {

		Object result = instance.executeStatement( "5 <= 10", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LTE 10", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LESS THAN OR EQUAL TO 10", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LE 10", context );
		assertThat( result ).isEqualTo( true );

	}

	@DisplayName( "parens" )
	@Test
	public void testParens() {

		Object result = instance.executeStatement( "1 + ( 2 * 3 )", context );
		assertThat( result ).isEqualTo( 7 );

		result = instance.executeStatement( "( 1 + 2 ) * 3", context );
		assertThat( result ).isEqualTo( 9 );

		result = instance.executeStatement( "( 1 + 2 * 3 )", context );
		assertThat( result ).isEqualTo( 7 );

	}

	@DisplayName( "Order of operations" )
	@Test
	public void testOrderOfOps() {

		Object result = instance.executeStatement( "1+2-3*4^5", context );
		assertThat( result ).isEqualTo( -3069 );

		result = instance.executeStatement( "2+3*4", context );
		assertThat( result ).isEqualTo( 14 );

		result = instance.executeStatement( "2-3+4", context );
		assertThat( result ).isEqualTo( 3 );

		result = instance.executeStatement( "2+3/4", context );
		assertThat( result ).isEqualTo( 2.75 );

		result = instance.executeStatement( "2-3/4", context );
		assertThat( result ).isEqualTo( 1.25 );

		result = instance.executeStatement( "2*2%3", context );
		assertThat( result ).isEqualTo( 1 );

		result = instance.executeStatement( "++5^--6", context );
		assertThat( result ).isEqualTo( 7776 );

	}

	@DisplayName( "It should handle equivalence" )
	@Test
	public void testEQVOperator() {

		Object result = instance.executeStatement( "true EQV true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "false EQV false", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "false EQV true", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "true EQV false", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "1 EQV 0", context );
		assertThat( result ).isEqualTo( false );

	}

	@DisplayName( "It should handle the implies operator" )
	@Test
	public void testIMPOperator() {

		Object result = instance.executeStatement( "true IMP false", context );
		assertThat( result ).isEqualTo( false );

		result = instance.executeStatement( "false IMP false", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "false IMP true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "true IMP true", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "1 IMP 0", context );
		assertThat( result ).isEqualTo( false );

	}

	@DisplayName( "It should handle not before parens" )
	@Test
	public void testNotBeforeParens() {

		Object result = instance.executeStatement( "NOT ( true ) ", context );
		assertThat( result ).isEqualTo( false );
	}

}
