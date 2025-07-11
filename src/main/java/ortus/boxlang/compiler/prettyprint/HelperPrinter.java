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

import java.util.List;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;

public class HelperPrinter {

	private Visitor visitor;

	public HelperPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void printStatements( List<BoxStatement> statements ) {
		if ( statements == null || statements.isEmpty() ) {
			return;
		}

		BoxStatement	lastStatement		= statements.get( statements.size() - 1 );
		BoxStatement	previousStatement	= null;

		for ( var statement : statements ) {
			// if there is a previous statement, check for empty lines in source
			// if so, add a hard line break
			if ( previousStatement != null && statement.hasLinesBetweenWithComments( previousStatement ) ) {
				visitor.newLine();
			} else if ( statement instanceof BoxFunctionDeclaration &&
			    ( statement.getParent() instanceof BoxClass || statement.getParent() instanceof BoxInterface ) ) {
				// if the statement is a function declaration in a class
				// append an extra hard line break before visiting it
				visitor.newLine();
			}

			statement.accept( visitor );

			// if the statement is not the last one, append a hard line break
			if ( statement != lastStatement ) {
				visitor.newLine();
			}

			previousStatement = statement;
		}
	}

	public void printBlock( BoxNode node, List<BoxStatement> statements ) {
		var currentDoc = visitor.getCurrentDoc();
		if ( visitor.isTemplate() ) {
			for ( var statement : statements ) {
				statement.accept( visitor );
			}
		} else {
			currentDoc.append( "{" );
			var blockDoc = visitor.pushDoc( DocType.INDENT );
			blockDoc.append( Line.HARD );

			printStatements( statements );

			var	insideCommentsDoc	= visitor.pushDoc( DocType.ARRAY );
			var	printed				= visitor.printInsideComments( node, false );
			visitor.popDoc(); // pop inside comments doc

			if ( printed ) {
				if ( !statements.isEmpty() ) {
					blockDoc.append( Line.HARD );
				}
				blockDoc.append( insideCommentsDoc );
			}

			currentDoc
			    .append( visitor.popDoc() )
			    .append( Line.HARD )
			    .append( "}" );
		}
	}

	public void printParensExpression( BoxExpression node ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	parensDoc	= visitor.pushDoc( DocType.GROUP ).append( "(" );
		visitor.pushDoc( DocType.INDENT ).append( Line.SOFT );
		node.accept( visitor );
		parensDoc
		    .append( visitor.popDoc() )
		    .append( Line.SOFT )
		    .append( ")" );

		currentDoc.append( visitor.popDoc() );
	}

	public void printKeyValueAnnotations( List<BoxAnnotation> attrs, boolean padded ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	attrsDoc	= visitor.pushDoc( DocType.GROUP );
		if ( attrs.size() > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			for ( var attr : attrs ) {
				contentsDoc.append( Line.LINE );
				attr.getKey().accept( visitor );
				if ( attr.getValue() != null ) {
					contentsDoc.append( "=\"" );
					visitor.stringPrinter.printQuotedExpression( attr.getValue() );
					contentsDoc.append( "\"" );
				}
			}
			attrsDoc.append( visitor.popDoc() );
		}
		attrsDoc.append( padded ? Line.LINE : Line.SOFT );
		currentDoc.append( visitor.popDoc() );
	}
}
