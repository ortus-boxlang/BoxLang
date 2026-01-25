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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;

public class ArgumentsPrinter {

	private Visitor visitor;

	public ArgumentsPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	/**
	 * Calculate the approximate length of the argument list if printed on a single line.
	 * Used to determine if the arguments should be printed multiline based on length threshold.
	 */
	private int calculateArgumentListLength( List<BoxArgument> arguments ) {
		int length = 2; // for "(" and ")"

		for ( int i = 0; i < arguments.size(); i++ ) {
			var arg = arguments.get( i );

			// Named argument: name = value
			if ( arg.getName() != null ) {
				if ( arg.getName() instanceof BoxStringLiteral str ) {
					length += str.getValue().length();
				} else {
					String nameSource = arg.getName().getSourceText();
					if ( nameSource != null ) {
						length += nameSource.length();
					}
				}
				length += 3; // " = "
			}

			// Argument value
			String valueSource = arg.getValue().getSourceText();
			if ( valueSource != null ) {
				length += valueSource.length();
			}

			if ( i < arguments.size() - 1 ) {
				length += 2; // ", "
			}
		}

		return length;
	}

	public void print( BoxNode parentNode, List<BoxArgument> arguments ) {
		var	currentDoc			= visitor.getCurrentDoc();
		var	argumentsDoc		= visitor.pushDoc( DocType.GROUP );

		var	size				= arguments.size();
		var	assignmentOperator	= " = "; // TODO: use config
		var	multiline			= size >= visitor.config.getArguments().getMultilineCount()
		    || calculateArgumentListLength( arguments ) >= visitor.config.getArguments().getMultilineLength();

		argumentsDoc.append( "(" );

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			contentsDoc.append( multiline || visitor.config.getParensPadding() ? Line.LINE : Line.SOFT );

			// Note: handling BoxArgument here, so that eventually we can
			// align named arguments if they print on multiple lines.
			for ( int i = 0; i < size; i++ ) {
				var arg = arguments.get( i );

				visitor.printPreComments( arg );
				if ( arg.getName() != null ) {
					if ( arg.getName() instanceof BoxStringLiteral str ) {
						contentsDoc.append( str.getValue() );
					} else {
						arg.getName().accept( visitor );
					}
					contentsDoc.append( assignmentOperator );
				}

				arg.getValue().accept( visitor );
				visitor.printPostComments( arg );

				if ( i < size - 1 ) {
					contentsDoc.append( "," );
					contentsDoc.append( Line.LINE );
				}
			}

			if ( multiline ) {
				contentsDoc.append( Line.BREAK_PARENT );
			}

			visitor.printInsideComments( parentNode, false );

			argumentsDoc.append( visitor.popDoc() );
			argumentsDoc.append( multiline || visitor.config.getParensPadding() ? Line.LINE : Line.SOFT );
		} else {
			visitor.printInsideComments( parentNode, false );
			// argumentsDoc.append( Line.SOFT );
		}

		argumentsDoc.append( ")" );
		currentDoc.append( visitor.popDoc() );

	}

}
