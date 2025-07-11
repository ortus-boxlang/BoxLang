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

import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;

public class ArrayLiteralPrinter {

	private Visitor visitor;

	public ArrayLiteralPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxArrayLiteral arrayNode ) {
		visitor.printPreComments( arrayNode );

		var	currentDoc	= visitor.getCurrentDoc();

		var	arrayDoc	= visitor.pushDoc( DocType.GROUP );
		arrayDoc.append( "[" );

		var	values	= arrayNode.getValues();
		var	size	= values.size();

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			contentsDoc.append( Line.SOFT );

			for ( int i = 0; i < size; i++ ) {
				values.get( i ).accept( visitor );

				if ( i < size - 1 ) {
					contentsDoc.append( "," );
					contentsDoc.append( Line.LINE );
				}
			}

			visitor.printInsideComments( arrayNode, true );

			arrayDoc.append( visitor.popDoc() );
			arrayDoc.append( Line.SOFT );
		} else {
			visitor.printInsideComments( arrayNode, true );
			arrayDoc.append( Line.SOFT );
		}

		arrayDoc.append( "]" );
		currentDoc.append( visitor.popDoc() );

		visitor.printPostComments( arrayNode );
	}

}
