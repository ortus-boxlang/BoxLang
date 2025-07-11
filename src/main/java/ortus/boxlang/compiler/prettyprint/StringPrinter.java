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
package ortus.boxlang.compiler.prettyprint;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

public class StringPrinter {

	private Visitor visitor;

	public StringPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void printStringLiteral( BoxStringLiteral node ) {
		var quote = visitor.config.isSingleQuote() ? "'" : "\"";

		visitor.printPreComments( node );
		visitor.print( quote + escapeString( node.getValue(), quote ) + quote );
		visitor.printPostComments( node );
	}

	public void printStringConcat( BoxStringConcat node ) {
		visitor.printPreComments( node );
		// TODO: Need to track more about original source
		int size = node.getValues().size();
		for ( int i = 0; i < size; i++ ) {
			var expr = node.getValues().get( i );
			expr.accept( visitor );
			if ( i < size - 1 ) {
				visitor.print( " & " );
			}
		}
		visitor.printPostComments( node );
	}

	public void printStringInterpolation( BoxStringInterpolation node ) {
		var quote = visitor.config.isSingleQuote() ? "'" : "\"";

		visitor.printPreComments( node );
		visitor.print( quote );
		processStringInterp( node, quote );
		visitor.print( quote );
		visitor.printPostComments( node );
	}

	public void printQuotedExpression( BoxExpression node ) {
		printQuotedExpression( node, "\"" );
	}

	public void printQuotedExpression( BoxExpression node, String quote ) {
		if ( node instanceof BoxStringLiteral str ) {
			visitor.print( escapeString( str.getValue(), quote ) );
		} else if ( node instanceof BoxStringInterpolation interp ) {
			processStringInterp( interp, quote );
		} else if ( node instanceof BoxFQN fqn ) {
			visitor.print( fqn.getValue() );
		} else {
			visitor.print( "#" );
			node.accept( visitor );
			visitor.print( "#" );
		}
	}

	public void processStringInterp( BoxStringInterpolation node, String quote ) {
		for ( var expr : node.getValues() ) {
			if ( expr instanceof BoxStringLiteral str ) {
				var value = str.getValue();
				if ( quote != null ) {
					visitor.print( escapeString( value, quote ) );
				} else {
					// If we're in an output component, we need to escape pound signs
					if ( node.getFirstAncestorOfType( BoxComponent.class, comp -> comp.getName().equalsIgnoreCase( "output" ) ) != null ) {
						value = value.replace( "#", "##" );
					}
					visitor.print( value );
				}
			} else {
				visitor.print( "#" );
				expr.accept( visitor );
				visitor.print( "#" );
			}
		}
	}

	private String escapeString( String value, String quote ) {
		return value
		    .replace( quote, quote + quote )
		    .replace( "#", "##" );

	}
}
