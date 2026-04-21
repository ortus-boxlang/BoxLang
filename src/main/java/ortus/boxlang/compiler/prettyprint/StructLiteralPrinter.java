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
				for ( int j = start; j < size; j += 2 ) {
					String valueSource = values.get( j + 1 ).getSourceText();
					if ( valueSource != null && ( valueSource.contains( "\n" ) || valueSource.contains( "\r" ) ) ) {
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

				String keyText = values.get( i ).getSourceText();
				if ( visitor.config.getStruct().getQuoteKeys() ) {
					contentsDoc.append( visitor.config.getSingleQuote() ? "'" : "\"" );
				}
				values.get( i ).accept( visitor );
				if ( visitor.config.getStruct().getQuoteKeys() ) {
					contentsDoc.append( visitor.config.getSingleQuote() ? "'" : "\"" );
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
			if ( isOrdered ) {
				structDoc.append( visitor.config.getStruct().getEmptyPadding() ? " : " : ":" );

			}
			visitor.printInsideComments( structNode, false );
			structDoc.append( visitor.config.getStruct().getEmptyPadding() ? Line.LINE : Line.SOFT );
		}

		structDoc.append( closeBrace );
		currentDoc.append( visitor.popDoc() );

		visitor.printPostComments( structNode );
	}

	private boolean shouldBeMultiline( BoxStructLiteral structNode ) {
		if ( visitor.config.getCFFormatCompatibility() && structNode.getSourceText() != null
		    && ( structNode.getSourceText().contains( "\n" ) || structNode.getSourceText().contains( "\r" ) ) ) {
			return true;
		}
		try {
			if ( visitor.config.getStruct().getMultiline().getMinLength() < structNode.getSourceText().length() ) {
				return true;
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		var	size		= structNode.getValues().size();
		// CFFormat uses strict "more than" semantics for element_count; BoxLang native mode uses "at least"
		var	isMultiline	= visitor.config.getStruct().getMultiline().getElementCount() > 0
		    && ( visitor.config.getCFFormatCompatibility()
		        ? size / 2 > visitor.config.getStruct().getMultiline().getElementCount()
		        : size / 2 >= visitor.config.getStruct().getMultiline().getElementCount() );
		return isMultiline;
	}

}
