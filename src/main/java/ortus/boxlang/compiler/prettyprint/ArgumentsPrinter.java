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
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;

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
		int		length		= 2; // for "(" and ")"
		String	separator	= visitor.config.getArguments().getSeparator().getSymbol();

		for ( int i = 0; i < arguments.size(); i++ ) {
			var arg = arguments.get( i );

			// Named argument: name = value
			if ( arg.getName() != null ) {
				if ( arg.getName() instanceof BoxStringLiteral str ) {
					length += str.getValue().length();
				} else {
					String nameSource = arg.getName().getSourceText();
					if ( nameSource != null ) {
						length += calculateSingleLineSourceLength( nameSource );
					}
				}
				length += separator.length();
			}

			// Argument value
			String valueSource = arg.getValue().getSourceText();
			if ( valueSource != null ) {
				length += calculateSingleLineSourceLength( valueSource );
			}

			if ( i < arguments.size() - 1 ) {
				length += 2; // ", "
			}
		}

		return length;
	}

	/**
	 * Determine whether an argument list would become multiline solely because of
	 * the configured length threshold.
	 */
	boolean wouldBreakByLength( List<BoxArgument> arguments ) {
		return !visitor.config.getCFFormatCompatibility()
		    && calculateArgumentListLength( arguments ) >= visitor.config.getArguments().getMultilineLength();
	}

	private boolean hasBlockFunctionArgument( List<BoxArgument> arguments ) {
		return arguments.stream().anyMatch( argument -> {
			var value = argument.getValue();
			return ( value instanceof BoxLambda lambda && lambda.getBody() instanceof BoxStatementBlock )
			    || ( value instanceof BoxClosure closure && closure.getBody() instanceof BoxStatementBlock );
		} );
	}

	/**
	 * Normalize source whitespace before measuring it so multiline decisions do not
	 * depend on how the input was previously formatted.
	 */
	private int calculateSingleLineSourceLength( String source ) {
		return source.replaceAll( "\\s+", " " ).trim().length();
	}

	public void print( BoxNode parentNode, List<BoxArgument> arguments ) {
		print( parentNode, arguments, false );
	}

	public void print( BoxNode parentNode, List<BoxArgument> arguments, boolean suppressMultilineByLength ) {
		var		currentDoc			= visitor.getCurrentDoc();
		var		argumentsDoc		= visitor.pushDoc( DocType.GROUP );

		var		size				= arguments.size();
		var		assignmentOperator	= visitor.config.getArguments().getSeparator().getSymbol();
		var		padding				= visitor.config.getArguments().getPadding() || visitor.config.getParensPadding();
		boolean	multilineByCount;
		boolean	multilineByLength;
		if ( visitor.config.getCFFormatCompatibility() ) {
			multilineByCount	= size >= visitor.config.getArguments().getMultilineCount();
			multilineByLength	= false;
		} else {
			multilineByCount	= size > ( visitor.config.getArguments().getMultilineCount() - 1 );
			multilineByLength	= !suppressMultilineByLength && wouldBreakByLength( arguments );
		}
		var	multilineByStructure	= suppressMultilineByLength && hasBlockFunctionArgument( arguments );
		var	multiline				= multilineByCount || multilineByLength || multilineByStructure;

		int	maxArgumentNameLength	= 0;
		if ( multiline && visitor.config.getAlignConsecutiveAssignments() ) {
			for ( var arg : arguments ) {
				if ( arg.getName() == null ) {
					continue;
				}
				int nameLength;
				if ( arg.getName() instanceof BoxStringLiteral str ) {
					nameLength = str.getValue().length();
				} else {
					String nameSource = arg.getName().getSourceText();
					nameLength = nameSource != null ? nameSource.length() : 0;
				}
				maxArgumentNameLength = Math.max( maxArgumentNameLength, nameLength );
			}
		}

		argumentsDoc.append( "(" );

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			if ( multiline ) {
				contentsDoc.append( Line.LINE );
			} else if ( padding ) {
				contentsDoc.append( " " );
			} else {
				contentsDoc.append( Line.SOFT );
			}

			// Note: handling BoxArgument here, so that eventually we can
			// align named arguments if they print on multiple lines.
			for ( int i = 0; i < size; i++ ) {
				var arg = arguments.get( i );

				visitor.printPreComments( arg );
				if ( arg.getName() != null ) {
					int currentNameLength = 0;
					if ( arg.getName() instanceof BoxStringLiteral str ) {
						String value = str.getValue();
						contentsDoc.append( value );
						currentNameLength = value.length();
					} else {
						String nameSource = arg.getName().getSourceText();
						currentNameLength = nameSource != null ? nameSource.length() : 0;
						arg.getName().accept( visitor );
					}
					if ( multiline && visitor.config.getAlignConsecutiveAssignments() && maxArgumentNameLength > 0 ) {
						for ( int j = 0; j < ( maxArgumentNameLength - currentNameLength ); j++ ) {
							contentsDoc.append( " " );
						}
					}
					contentsDoc.append( assignmentOperator );
				}

				arg.getValue().accept( visitor );
				visitor.printPostComments( arg );

				if ( i < size - 1 ) {
					contentsDoc.append( "," );
					contentsDoc.append( Line.LINE );
				} else if ( multiline && visitor.config.getArguments().getCommaDangle() ) {
					// Add trailing comma for last argument when multiline and comma_dangle is enabled
					contentsDoc.append( "," );
				}
			}

			if ( multiline ) {
				contentsDoc.append( Line.BREAK_PARENT );
			}

			visitor.printInsideComments( parentNode, false );

			argumentsDoc.append( visitor.popDoc() );
			if ( multiline ) {
				argumentsDoc.append( Line.LINE );
			} else if ( padding ) {
				argumentsDoc.append( " " );
			} else {
				argumentsDoc.append( Line.SOFT );
			}
		} else {
			// Check if there are inside comments — if so, force multiline to avoid collapsing
			boolean hasInsideComments = false;
			for ( var comment : parentNode.getComments() ) {
				if ( comment.isInside( parentNode ) ) {
					hasInsideComments = true;
					break;
				}
			}

			if ( hasInsideComments ) {
				var contentsDoc = visitor.pushDoc( DocType.INDENT );
				contentsDoc.append( Line.HARD );
				visitor.printInsideComments( parentNode, false );
				argumentsDoc.append( visitor.popDoc() ).append( Line.HARD );
			} else {
				visitor.printInsideComments( parentNode, false );
				if ( visitor.config.getArguments().getEmptyPadding() ) {
					argumentsDoc.append( " " );
				}
			}
		}

		argumentsDoc.append( ")" );
		currentDoc.append( visitor.popDoc() );

	}

}
