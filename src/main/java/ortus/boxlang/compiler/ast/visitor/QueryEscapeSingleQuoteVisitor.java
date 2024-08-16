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
package ortus.boxlang.compiler.ast.visitor;

import java.util.List;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

/**
 * I handle escaping single quotes in interpolated expressions inside a query component.
 * I could have been an extra if statement in the template parsers, but it just feels more tidy to place this
 * bit of logic here and it's easier to re-use across parsers this way.
 * 
 * Adobe and Lucee are all over the board in their implementation of this, and quite dissimilar from each other.
 * 
 * Lucee ONLY escapes interpolation in tag-based queries and can be tricked by wrapping the preserveSingleQuotes() function in another function.
 * 
 * Adobe appears to somehow intercept ALL lexically-descendant expressions of a query component that eval an expression and escape them,
 * but it has some sort of logic to not double-escape which appears to be the only way it doesn't escape things twice.
 * Adobe also escapes expressions not even being output in the query-- it will affect <cfset> calls or even <cfif> statements as well which seems quite wrong.
 * 
 * This visitor is a compromise between the two. It will escape all expressions in a query component (script or tag)
 * that are inside the top level of a BoxBufferOutput or writeOutput() BIF call unless they are wrapped in a preserveSingleQuotes() function invocation.
 * This visitor can also be tricked by wrapping the preserveSingleQuotes() function in another function or expression,
 * but that's a bit of an edge case and this simplifies things quite a bit.
 * 
 * Adobe, Lucee, and BoxLang all leave stand-alone string literals untouched.
 * 
 * Only use this visitor prior to compiling. Otherwise, the rewritten AST will show up in transpiled or pretty printed code.
 */
public class QueryEscapeSingleQuoteVisitor extends VoidBoxVisitor {

	/**
	 * Constructor
	 */
	public QueryEscapeSingleQuoteVisitor() {
	}

	/**
	 * Visit the query component and escape single quotes in interpolated expressions unless they are wrapped in
	 * the preserveSingleQuotes() BIF.
	 */
	public void visit( BoxComponent node ) {
		// Only apply to query components (tag or script)
		if ( node.getName().equalsIgnoreCase( "query" ) ) {
			// Tag based code only will have this node
			node.getDescendantsOfType( BoxBufferOutput.class )
			    .forEach( s -> escapeBufferOutput( s ) );

			// Tag or script based code could have this node (also aliased as echo())
			node.getDescendantsOfType( BoxFunctionInvocation.class,
			    f -> f.getName().equalsIgnoreCase( "writeoutput" ) || f.getName().equalsIgnoreCase( "echo" ) )
			    .forEach( s -> escapeWriteOutput( s ) );
		}
		super.visit( node );
	}

	/**
	 * Escape single quotes in interpolated output of a tag-based template.
	 * 
	 * @param s the buffer output statement
	 */
	private void escapeBufferOutput( BoxBufferOutput s ) {
		// If the text being output is just a BoxStringLiteral, then we leave it untouched
		if ( isStringConcat( s.getExpression() ) ) {
			escapeExpressions( ( BoxStringConcat ) s.getExpression() );
		}
	}

	/**
	 * Escape single quotes in the output of a writeOutput() or echo() function invocation
	 * so long as it's not a string literal. An interpolated, or concatenated string or a single expression gets escaped
	 * 
	 * @param s the writeOutput() or echo() function invocation
	 */
	private void escapeWriteOutput( BoxFunctionInvocation s ) {
		if ( s.getArguments().size() > 0 ) {
			BoxArgument		arg		= s.getArguments().get( 0 );
			BoxExpression	value	= arg.getValue();

			if ( isStringConcat( value ) ) {
				escapeExpressions( ( BoxStringConcat ) value );
			} else if ( !isStringLiteral( value ) && !isPreserveSingleQuotes( value ) ) {
				arg.setValue( escapeExpression( value ) );
			}
		}
	}

	/**
	 * Escape single quotes in a string concatenation. This can be a BoxStringConcat or a BoxStringInterpolation instance.
	 * 
	 * @param bsc
	 */
	private void escapeExpressions( BoxStringConcat bsc ) {
		bsc.setValues(
		    bsc.getValues()
		        .stream()
		        // Skip string literals and preserveSingleQuotes() function invocations
		        // Note, this can be tricked if the preserveSingleQuotes() function is wrapped in another function call or expression
		        .map( e -> {
			        if ( !isStringLiteral( e ) && !isPreserveSingleQuotes( e ) ) {
				        return escapeExpression( e );
			        } else {
				        return e;
			        }
		        } )
		        .toList()
		);
	}

	/**
	 * Escape single quotes in an expression.
	 * 
	 * @param e the expression to escape
	 * 
	 * @return the escaped expression
	 */
	private BoxExpression escapeExpression( BoxExpression e ) {
		return new BoxFunctionInvocation(
		    "replaceNoCase",
		    List.of(
		        new BoxArgument( e, null, null ),
		        new BoxArgument( new BoxStringLiteral( "'", null, null ), null, null ),
		        new BoxArgument( new BoxStringLiteral( "''", null, null ), null, null )
		    ),
		    e.getPosition(),
		    e.getSourceText() );
	}

	/**
	 * Is the node a string literal?
	 * 
	 * @param node the node to check
	 * 
	 * @return true if the node is a string literal
	 */
	private boolean isStringLiteral( BoxNode node ) {
		return node instanceof BoxStringLiteral;
	}

	/**
	 * Is the node a string concatenation?
	 * 
	 * @param node the node to check
	 * 
	 * @return true if the node is a string concatenation
	 */
	private boolean isStringConcat( BoxNode node ) {
		return node instanceof BoxStringConcat;
	}

	/**
	 * Is the node a preserveSingleQuotes() function invocation?
	 * 
	 * @param node the node to check
	 * 
	 * @return true if the node is a preserveSingleQuotes() function invocation
	 */
	private boolean isPreserveSingleQuotes( BoxNode node ) {
		return node instanceof BoxFunctionInvocation bfi && bfi.getName().equalsIgnoreCase( "preserveSingleQuotes" );
	}

}
