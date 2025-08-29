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

import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;

public class StructLiteralPrinter {

	private Visitor visitor;

	public StructLiteralPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxStructLiteral structNode ) {
		visitor.printPreComments( structNode );

		var	currentDoc	= visitor.getCurrentDoc();

		var	structDoc	= visitor.pushDoc( DocType.GROUP );

		var	isOrdered	= structNode.getType().equals( BoxStructType.Ordered );
		var	openBrace	= isOrdered ? "[" : "{";
		var	closeBrace	= isOrdered ? "]" : "}";
		var	colon		= ": "; // TODO: make configurable

		var	values		= structNode.getValues();
		var	size		= values.size();

		structDoc.append( openBrace );

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			contentsDoc.append( visitor.config.getBracketPadding() ? Line.LINE : Line.SOFT );

			for ( int i = 0; i < size; i += 2 ) {
				values.get( i ).accept( visitor );
				contentsDoc.append( colon );

				values.get( i + 1 ).accept( visitor );

				if ( i < size - 2 ) {
					contentsDoc.append( "," ).append( Line.LINE );
				}
			}
			visitor.printInsideComments( structNode, false );

			structDoc.append( visitor.popDoc() ).append( visitor.config.getBracketPadding() ? Line.LINE : Line.SOFT );
		} else {
			if ( isOrdered ) {
				structDoc.append( visitor.config.getBracketPadding() ? " : " : ":" );

			}
			visitor.printInsideComments( structNode, false );
			structDoc.append( Line.SOFT );
		}

		structDoc.append( closeBrace );
		currentDoc.append( visitor.popDoc() );

		visitor.printPostComments( structNode );
	}

}
