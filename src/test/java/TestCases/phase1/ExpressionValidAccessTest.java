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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpressionValidAccessTest {

	static BoxRuntime	instance;
	static Set<String>	allExpressions		= new TreeSet<>();
	static Set<String>	validAccessLHS		= new HashSet<>();
	static Set<String>	validDotAccess		= new TreeSet<>();
	static Set<String>	validArrayAccess	= new TreeSet<>();
	static Set<String>	invalidDotAccess	= new TreeSet<>();
	static Set<String>	invalidArrayAccess	= new TreeSet<>();

	Parser				parser;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		parser = new Parser();
	}

	// Valid for dot access RHS (both of these are valid for LHS as well)
	static Set<String>	validDotAccessRHS	= Set.of(
	    "foo",
	    "foo(bar, baz)",
	    "true",
	    "false",
	    "null",
	    "42" );

	// Standalone expressions. (not valid for LHS or RHS of access expression
	// without paren wrapper)
	static Set<String>	standaloneExpr		= Set.of(
	    "function() {}",
	    "() => {}",
	    "() -> {}",
	    "foo++",
	    "bar--",
	    "foo ^ bar",
	    "foo * bar",
	    "foo + bar",
	    "foo b<< bar",
	    "foo b& bar",
	    "foo b^ bar",
	    "foo b| bar",
	    "foo eqv bar",
	    "foo < bar",
	    "foo > bar",
	    "foo == bar",
	    "foo XOR bar",
	    "foo & bar",
	    "foo DOES NOT CONTAIN bar",
	    "foo AND bar",
	    "foo OR bar",
	    "foo ?: bar",
	    "foo InstanceOf bar",
	    "foo CastAs bar",
	    "foo ? bar : baz",
	    "xc.y?.z",
	    "!foo",
	    "-foo",
	    "+foo",
	    "++foo",
	    "--foo"
	);

	static {

		// Valid for any access LHS
		validAccessLHS.addAll( Set.of(
		    // This needs to work, but the old grammar doesn't support it
		    // uncommment to test on new grammar
		    // "foo::bar",
		    "new foo.bar.Baz()",
		    "\"bar\"",
		    "[1,2,3]",
		    "{foo:bar}",
		    "42.5", // must be a decimal, or it won't work
		    "true",
		    "false",
		    "( foo )", // This can be any expression, but we don't need to test them all.
		    // if starting an access expression with one of these, you may have ambiguity
		    // without parenthesis, but they are valid
		    "null",
		    "foo",
		    "foo(bar, baz)",
		    "foo[bar]",
		    "xc.y?.z"
		) );

		allExpressions.addAll( validAccessLHS );
		allExpressions.addAll( validDotAccessRHS );
		allExpressions.addAll( standaloneExpr );

		// create every possible combination of valid LHS and RHS for array access and
		// place them in validArrayAccess
		validAccessLHS.forEach( lhs -> allExpressions.forEach( rhs -> validArrayAccess.add( lhs + "[ " + rhs + " ]" ) ) );

		// loop over StandaloneExpr and add all of them wrapped in ()
		// standaloneExpr.forEach( expr -> validAccessLHS.add( "( " + expr + " )" ) );

		// create every possible combination of valid LHS and RHS for dot expressions
		// and place them in validDotAccess
		validAccessLHS.forEach( lhs -> validDotAccessRHS.forEach( rhs -> validDotAccess.add( lhs + "." + rhs ) ) );

		// create all possible combinations of valid LHS with allExpressions NOT in
		// validDotAccessRHS

		validAccessLHS
		    .stream()
		    .filter( expr -> !expr.equals( "42" ) )
		    .forEach(
		        lhs -> allExpressions.stream()
		            .filter( expr -> !validDotAccessRHS.contains( expr ) )
		            // These are not valid the LHS of dot access, but they pass because stuff like ()=>{}.foo looks like the return value is a struct literal dereferencing the foo
		            .filter( expr -> !Set.of( "function() {}", "() => {}", "() -> {}" ).contains( expr ) )
		            .forEach( rhs -> invalidDotAccess.add( lhs + "." + rhs ) )
		    );

		// now add to that all allExpressions NOT in validAccessLHS pared with all validDotAccessRHS.
		allExpressions.stream().filter( expr -> !validAccessLHS.contains( expr ) )
		    // These are not valid the LHS of dot access, but they pass because stuff like ()=>{}.foo looks like the return value is a struct literal dereferencing the foo
		    .filter( expr -> !Set.of( "function() {}", "() => {}", "() -> {}" ).contains( expr ) )
		    .filter( expr -> !expr.startsWith( "42" ) )
		    .forEach( lhs -> validDotAccessRHS.stream().filter( expr -> !expr.startsWith( "42" ) ).forEach( rhs -> invalidDotAccess.add( lhs + "." + rhs ) ) );

		// now add all the invalid left and invalid right hand sides from above
		invalidDotAccess.addAll(
		    allExpressions.stream()
		        .filter( expr -> !validAccessLHS.contains( expr ) )
		        .flatMap( lhs -> allExpressions.stream().filter( expr -> !validDotAccessRHS.contains( expr ) ).map( rhs -> lhs + "." + rhs ) )
		        // .filter( expr -> !Set.of( "42.( foo )", "42.+foo", "42.-foo" ).contains( expr ) )
		        .collect( Collectors.toSet() )
		);

		// now add to that all allExpressions NOT in validAccessLHS paired with all validDotAccessRHS.
		allExpressions.stream().filter( expr -> !validAccessLHS.contains( expr ) )
		    // These are not valid the LHS of dot access, but they pass because stuff like ()=>{}.foo looks like the return value is a struct literal dereferencing the foo
		    .filter( expr -> !Set.of( "function() {}", "() => {}", "() -> {}" ).contains( expr ) )
		    // 42[ property ] is actually fine
		    .filter( expr -> !expr.startsWith( "42" ) )
		    .forEach( lhs -> invalidArrayAccess.add( lhs + "[ expr ]" ) );

	}

	static Stream<String> expressionProvider() {
		return allExpressions.stream()
		    .map( ExpressionValidAccessTest::replaceParentheses );
	}

	static Stream<String> validDotAccessProvider() {
		return validDotAccess.stream()
		    .map( ExpressionValidAccessTest::replaceParentheses );
	}

	static Stream<String> validArrayAccessProvider() {
		return validArrayAccess.stream()
		    .map( ExpressionValidAccessTest::replaceParentheses );
	}

	static Stream<String> invalidDotAccessProvider() {
		return invalidDotAccess.stream()
		    .map( ExpressionValidAccessTest::replaceParentheses );
	}

	static Stream<String> invalidArrayAccessProvider() {
		return invalidArrayAccess.stream()
		    .map( ExpressionValidAccessTest::replaceParentheses );
	}

	@ParameterizedTest( name = "Valid Expression {index} -- {0}" )
	@MethodSource( "expressionProvider" )
	public void testParseExpression( String expression ) {
		expression = unreplaceParentheses( expression );
		// System.out.println( "Valid Expression: " + expression );
		ParsingResult result = parser.parseExpression( expression );
		if ( !result.isCorrect() ) {
			throw new AssertionError( "Expression >> " + expression + " >> " + result.getIssues() );
		}
	}

	@ParameterizedTest( name = "Valid Dot Access {index} -- {0}" )
	@MethodSource( "validDotAccessProvider" )
	public void testParseValidDotAccess( String expression ) {
		expression = unreplaceParentheses( expression );
		// System.out.println( "Valid Dot Access: " + expression );
		ParsingResult result = parser.parseExpression( expression );
		if ( !result.isCorrect() || !isCorrectNode( expression, result.getRoot() ) ) {
			throw new AssertionError( "Valid Dot Access -- " + expression + " -- " + generateErrorDetail( result ) );
		}
	}

	@ParameterizedTest( name = "Valid Array Access {index} -- {0}" )
	@MethodSource( "validArrayAccessProvider" )
	public void testParseValidArrayAccess( String expression ) {
		expression = unreplaceParentheses( expression );
		// System.out.println( "Valid Array Access: " + expression );
		ParsingResult result = parser.parseExpression( expression );
		if ( !result.isCorrect() || !isCorrectNodeArray( expression, result.getRoot() ) ) {
			throw new AssertionError( "Valid Array Access -- " + expression + " -- " + generateErrorDetail( result ) );
		}
	}

	@ParameterizedTest( name = "Invalid Dot Access {index} -- {0}" )
	@MethodSource( "invalidDotAccessProvider" )
	public void testParseInvalidDotAccess( String expression ) {
		expression = unreplaceParentheses( expression );
		// System.out.println( "Invalid Dot Access: " + expression );
		ParsingResult result = parser.parseExpression( expression );
		if ( result.isCorrect() && isCorrectNode( expression, result.getRoot() ) ) {
			throw new AssertionError( "Invalid Dot Access -- " + expression + " -- PASSED -- " + generateErrorDetail( result ) );
		}
	}

	@ParameterizedTest( name = "Invalid Array Access {index} -- {0}" )
	@MethodSource( "invalidArrayAccessProvider" )
	public void testParseInvalidArrayAccess( String expression ) {
		expression = unreplaceParentheses( expression );
		// System.out.println( "Invalid Array Access: " + expression );
		ParsingResult result = parser.parseExpression( expression );
		if ( result.isCorrect() && isCorrectNodeArray( expression, result.getRoot() ) ) {
			throw new AssertionError( "Invalid Array Access -- " + expression + " -- PASSED" );
		}
	}

	private static String replaceParentheses( String expression ) {
		return expression.replace( "(", "[[" ).replace( ")", "]]" );
	}

	private static String unreplaceParentheses( String expression ) {
		return expression.replace( "[[", "(" ).replace( "]]", ")" );
	}

	/**
	 * Verify foo.bar is a BoxDotAccess and foo.bar() is a BoxMethodInvocation
	 *
	 * @param expression parsed expression
	 * @param node       parsed node
	 *
	 * @return true if the node is correct
	 */
	private boolean isCorrectNode( String expression, BoxNode node ) {
		boolean	isMethodInvocation	= expression.endsWith( "foo(bar, baz)" );
		boolean	RHSIsDecimal		= expression.endsWith( "42.5" );
		boolean	LHSIsDecimal		= expression.startsWith( "42.5" );
		boolean	RHSIsDotAccess		= expression.endsWith( "xc.y?.z" );
		boolean	LHSIsDotAccess		= expression.startsWith( "xc.y?.z" );
		if ( isMethodInvocation ) {
			return node instanceof BoxMethodInvocation;
		} else {
			if ( node instanceof BoxDotAccess bda ) {
				// If it's dot access and ended with 42.5 the RHS should be that full decimal
				if ( RHSIsDecimal && bda.getAccess() instanceof BoxDecimalLiteral bdl && bdl.getValue().equals( "42.5" ) ) {
					return true;
				} else if ( RHSIsDecimal ) {
					return false;
				}
				// if it starts with 42.5, that entire value needs to be the context of the dot access
				if ( LHSIsDecimal && bda.getContext() instanceof BoxDecimalLiteral bdl && bdl.getValue().equals( "42.5" ) ) {
					return true;
				} else if ( LHSIsDecimal ) {
					return false;
				}
				// if it's dot acess and ended with xc.y?.z the RHS should be a BoxDotAccess with that full bit
				if ( RHSIsDotAccess && bda.getAccess() instanceof BoxDotAccess bda2 && bda2.getAccess().toString().equals( "xc.y?.z" ) ) {
					return true;
				} else if ( RHSIsDotAccess ) {
					return false;
				}
				// if it starts with xc.y?.z, that entire value needs to be the context of the dot access
				if ( LHSIsDotAccess && bda.getContext() instanceof BoxDotAccess bda2 && bda2.toString().equals( "xc.y?.z" ) ) {
					return true;
				} else if ( LHSIsDotAccess ) {
					return false;
				}
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Verify foo[ bar ] is a BoxArrayAccess
	 *
	 * @param expression parsed expression
	 * @param node       parsed node
	 *
	 * @return true if the node is correct
	 */
	private boolean isCorrectNodeArray( String expression, BoxNode node ) {
		return node instanceof BoxArrayAccess;
	}

	/**
	 * Generate error detail
	 *
	 * @param result parsing result
	 *
	 * @return error detail
	 */
	private String generateErrorDetail( ParsingResult result ) {
		return result.isCorrect() ? "AST Node: " + result.getRoot().getClass().getSimpleName()
		    : "Parse error: " + result.getIssues().getFirst().getMessage();
	}

}
