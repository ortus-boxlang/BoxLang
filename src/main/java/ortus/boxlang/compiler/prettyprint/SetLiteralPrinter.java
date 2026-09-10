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

import ortus.boxlang.compiler.ast.expression.BoxSetLiteral;

public class SetLiteralPrinter {

	private Visitor visitor;

	public SetLiteralPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxSetLiteral setNode ) {
		visitor.printPreComments( setNode );

		var	currentDoc	= visitor.getCurrentDoc();

		var	setDoc		= visitor.pushDoc( DocType.GROUP );
		setDoc.append( "set{" );

		var	values		= setNode.getValues();
		var	size		= values.size();
		var	multiline	= visitor.config.getArray().getMultiline().getElementCount() < size;

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			contentsDoc.append( multiline || visitor.config.getArray().getPadding() ? Line.LINE : Line.SOFT );

			for ( int i = 0; i < size; i++ ) {

				values.get( i ).accept( visitor );

				if ( i < size - 1 ) {
					contentsDoc.append( "," );
					contentsDoc.append( Line.LINE );
				}
			}

			visitor.printInsideComments( setNode, false );

			if ( multiline ) {
				contentsDoc.append( Line.BREAK_PARENT );
			}

			setDoc.append( visitor.popDoc() );
			setDoc.append( visitor.config.getArray().getPadding() ? Line.LINE : Line.SOFT );
		} else {
			// Check if there are inside comments — if so, force multiline to avoid collapsing
			boolean hasInsideComments = false;
			for ( var comment : setNode.getComments() ) {
				if ( comment.isInside( setNode ) ) {
					hasInsideComments = true;
					break;
				}
			}

			if ( hasInsideComments ) {
				var contentsDoc = visitor.pushDoc( DocType.INDENT );
				contentsDoc.append( Line.HARD );
				visitor.printInsideComments( setNode, false );
				setDoc.append( visitor.popDoc() ).append( Line.HARD );
			} else {
				visitor.printInsideComments( setNode, false );
				setDoc.append( visitor.config.getArray().getEmptyPadding() ? Line.LINE : Line.SOFT );
			}
		}

		setDoc.append( "}" );
		currentDoc.append( visitor.popDoc() );

		visitor.printPostComments( setNode );
	}

}
