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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxStringLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxStringLiteralTransformer extends AbstractTransformer {

	private static final int MAX_LITERAL_LENGTH = 30000; // 64KB limit

	public BoxStringLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform BoxStringLiteral argument
	 *
	 * @param node    a BoxStringLiteral instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser string Literal or concatenation expression
	 */
	@Override
	public Expression transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStringLiteral literal = ( BoxStringLiteral ) node;
		return transform( literal.getValue() );
	}

	/**
	 * Transform just the string portion (reuseable for other purposes)
	 *
	 * @param value The input string.
	 *
	 * @return generates a Java Parser string Literal or concatenation expression
	 */
	public static Expression transform( String value ) throws IllegalStateException {
		String escapedVal = escape( value );

		if ( escapedVal.length() > MAX_LITERAL_LENGTH ) {
			List<String> parts = splitStringIntoParts( value );
			return createArrayJoinMethodCall( parts );
		} else {
			return new StringLiteralExpr( escapedVal );
		}
	}

	/**
	 * Split a large string into parts
	 *
	 * @param str The input string.
	 * 
	 * @return A list of StringLiteralExpr parts.
	 **/
	private static List<String> splitStringIntoParts( String str ) {
		List<String>	parts	= new ArrayList<>();
		int				length	= str.length();
		for ( int i = 0; i < length; i += MAX_LITERAL_LENGTH ) {
			int		end		= Math.min( length, i + MAX_LITERAL_LENGTH );
			String	part	= str.substring( i, end );
			parts.add( part );
		}
		return parts;
	}

	/**
	 * Create a BinaryExpr that concatenates all the StringLiteralExpr parts
	 *
	 * @param parts List of StringLiteralExpr parts.
	 * 
	 * @return A BinaryExpr representing the concatenation of all parts.
	 **/
	private static Expression createArrayJoinMethodCall( List<String> parts ) {
		// Create a MethodCallExpr for String.join with an array of strings
		var args = parts.stream()
		    // Assumes the parts won't have so many escaped chars to put back over the limit
		    .map( part -> ( Expression ) new StringLiteralExpr( escape( part ) ) ) // Escape quotes and create StringLiteralExpr
		    .collect( Collectors.toCollection( NodeList::new ) ); // Collect into NodeList
		args.add( 0, new StringLiteralExpr( "" ) ); // Delimiter
		MethodCallExpr joinMethodCall = new MethodCallExpr( new NameExpr( "String" ), "join", args );

		return joinMethodCall;
	}

	/**
	 * Escape a give String to make it safe to be printed or stored.
	 *
	 * @param s The input String.
	 *
	 * @return The output String.
	 **/
	private static String escape( String s ) {
		return s.replace( "\\", "\\\\" )
		    .replace( "\t", "\\t" )
		    .replace( "\b", "\\b" )
		    .replace( "\n", "\\n" )
		    .replace( "\r", "\\r" )
		    .replace( "\f", "\\f" )
		    .replace( "\"", "\\\"" );
	}
}
