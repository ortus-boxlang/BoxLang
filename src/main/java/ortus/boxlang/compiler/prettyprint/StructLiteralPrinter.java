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
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;

public class StructLiteralPrinter {

	/** A run of whitespace. */

	private Visitor visitor;

	public StructLiteralPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxStructLiteral structNode ) {
		visitor.printPreComments( structNode );

		var		currentDoc				= visitor.getCurrentDoc();

		var		structDoc				= visitor.pushDoc( DocType.GROUP );

		var		isOrdered				= structNode.getType().equals( BoxStructType.Ordered );
		var		openBrace				= isOrdered ? "[" : "{";
		var		closeBrace				= isOrdered ? "]" : "}";
		var		separator				= visitor.config.getStruct().getSeparator().getSymbol();

		var		values					= structNode.getValues();
		var		size					= values.size();
		var		isMultiline				= shouldBeMultiline( structNode );
		int[]	alignmentMaxByPairIndex	= new int[ size / 2 ];
		if ( isMultiline && visitor.config.getAlignConsecutiveAssignments() ) {
			for ( int start = 0; start < size; ) {
				int end = size - 2;
				// A run of aligned keys ends at a value that prints on several lines. That is decided from how the value
				// will print, not from its source: a nested literal written on one line may print on several, and
				// deciding from the source made the next pass align differently.
				for ( int j = start; j < size; j += 2 ) {
					if ( printsMultiline( values.get( j + 1 ) ) ) {
						end = j;
						break;
					}
				}

				int runMaxKeyLength = 0;
				for ( int j = start; j <= end; j += 2 ) {
					String keyText = values.get( j ).getSourceText();
					if ( keyText != null ) {
						runMaxKeyLength = Math.max( runMaxKeyLength, keyText.length() );
					}
				}

				for ( int j = start; j <= end; j += 2 ) {
					alignmentMaxByPairIndex[ j / 2 ] = runMaxKeyLength;
				}

				start = end + 2;
			}
		}
		var	shouldHaveDanglingComma	= isMultiline
		    && visitor.config.getStruct().getMultiline().getCommaDangle();
		var	useLeadingComma			= isMultiline
		    && visitor.config.getStruct().getMultiline().getLeadingComma().getEnabled();

		structDoc.append( openBrace );

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			contentsDoc.append( visitor.config.getStruct().getPadding() ? Line.LINE : Line.SOFT );

			for ( int i = 0; i < size; i += 2 ) {

				if ( i >= 2 && useLeadingComma ) {
					contentsDoc.append( "," ).append( visitor.config.getStruct().getMultiline().getLeadingComma().getPadding() ? " " : "" );
				}

				var		key		= values.get( i );
				String	keyText	= key.getSourceText();
				if ( visitor.config.getStruct().getQuoteKeys() ) {
					String quote = visitor.config.getSingleQuote() ? "'" : "\"";
					visitor.printPreComments( key );
					contentsDoc.append( quote );
					visitor.stringPrinter.printQuotedExpression( key, quote );
					contentsDoc.append( quote );
					visitor.printPostComments( key );
				} else {
					key.accept( visitor );
				}
				int maxKeyLength = alignmentMaxByPairIndex[ i / 2 ];
				if ( isMultiline && visitor.config.getAlignConsecutiveAssignments() && maxKeyLength > 0 && keyText != null ) {
					contentsDoc.append( " ".repeat( Math.max( 0, maxKeyLength - keyText.length() ) ) );
				}
				contentsDoc.append( separator );

				values.get( i + 1 ).accept( visitor );

				if ( !useLeadingComma ) {
					if ( i < size - 2 ) {
						contentsDoc.append( "," ).append( isMultiline ? Line.HARD : Line.LINE );
					} else if ( shouldHaveDanglingComma ) {
						contentsDoc.append( "," );
					}
				} else if ( i < size - 2 ) {
					contentsDoc.append( isMultiline ? Line.HARD : Line.LINE );
				}
			}
			visitor.printInsideComments( structNode, false );

			structDoc.append( visitor.popDoc() ).append( visitor.config.getStruct().getPadding() ? Line.LINE : Line.SOFT );
		} else {
			// Check if there are inside comments — if so, force multiline to avoid collapsing
			boolean hasInsideComments = false;
			for ( var comment : structNode.getComments() ) {
				if ( comment.isInside( structNode ) ) {
					hasInsideComments = true;
					break;
				}
			}

			if ( hasInsideComments ) {
				// Force multiline: open brace, newline, indented comment, newline, close brace
				var contentsDoc = visitor.pushDoc( DocType.INDENT );
				contentsDoc.append( Line.HARD );
				visitor.printInsideComments( structNode, false );
				structDoc.append( visitor.popDoc() ).append( Line.HARD );
			} else {
				if ( isOrdered ) {
					structDoc.append( visitor.config.getStruct().getEmptyPadding() ? " : " : ":" );
				}
				visitor.printInsideComments( structNode, false );
				structDoc.append( visitor.config.getStruct().getEmptyPadding() ? Line.LINE : Line.SOFT );
			}
		}

		structDoc.append( closeBrace );
		currentDoc.append( visitor.popDoc() );

		visitor.printPostComments( structNode );
	}

	/**
	 * Whether a struct value will print on several lines: a struct or array literal that is laid out multi-line, or a
	 * closure or lambda with a block body.
	 *
	 * @param value the value expression
	 *
	 * @return true when it prints multi-line
	 */
	private boolean printsMultiline( BoxExpression value ) {
		if ( value instanceof BoxStructLiteral struct ) {
			if ( struct.getValues().isEmpty() || !shouldBeMultiline( struct ) ) {
				return false;
			}
			// Only the breaks between keys are hard: a multi-line struct with one key still prints on one line when it
			// fits, unless its value prints on several lines
			return struct.getValues().size() > 2 || printsMultiline( struct.getValues().get( 1 ) );
		}
		if ( value instanceof BoxArrayLiteral array ) {
			return !array.getValues().isEmpty() && visitor.config.getArray().getMultiline().getElementCount() < array.getValues().size();
		}
		if ( value instanceof BoxClosure closure ) {
			return closure.getBody() instanceof BoxStatementBlock;
		}
		if ( value instanceof BoxLambda lambda ) {
			return lambda.getBody() instanceof BoxStatementBlock;
		}
		return false;
	}

	private boolean shouldBeMultiline( BoxStructLiteral structNode ) {
		if ( visitor.config.getCFFormatCompatibility() && structNode.getSourceText() != null
		    && ( structNode.getSourceText().contains( "\n" ) || structNode.getSourceText().contains( "\r" ) ) ) {
			return true;
		}
		var	size		= structNode.getValues().size();
		// CFFormat uses strict "more than" semantics for element_count; BoxLang native mode uses "at least"
		var	isMultiline	= visitor.config.getStruct().getMultiline().getElementCount() > 0
		    && ( visitor.config.getCFFormatCompatibility()
		        ? size / 2 > visitor.config.getStruct().getMultiline().getElementCount()
		        : size / 2 >= visitor.config.getStruct().getMultiline().getElementCount() );
		if ( isMultiline || visitor.measuring ) {
			return isMultiline;
		}
		// The length is the width the struct prints at on one line, not its source length: the source changes with each
		// pass (the formatter adds spaces before separators and alignment), and measuring it made the next pass decide
		// differently. A struct holding a value that always prints on several lines (-1) prints on several lines too.
		int width = visitor.flatWidth( structNode );
		return width < 0 || visitor.config.getStruct().getMultiline().getMinLength() < width;
	}

}
