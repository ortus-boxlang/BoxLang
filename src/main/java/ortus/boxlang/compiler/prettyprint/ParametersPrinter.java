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

import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;

public class ParametersPrinter {

	private Visitor visitor;

	public ParametersPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	/**
	 * Calculate the approximate length of the parameter list if printed on a single line.
	 * Used to determine if the parameters should be printed multiline based on length threshold.
	 */
	private int calculateParameterListLength( List<BoxArgumentDeclaration> params ) {
		int length = 2; // for "(" and ")"

		for ( int i = 0; i < params.size(); i++ ) {
			var node = params.get( i );

			if ( node.getRequired() != null && node.getRequired() ) {
				length += 9; // "required "
			}

			if ( node.getType() != null ) {
				var	type			= node.getType();
				var	typeIsPrinted	= type != "Any" || node.getSourceText().contains( "Any " );
				if ( typeIsPrinted ) {
					length += type.length() + 1; // type + " "
				}
			}

			length += node.getName().length();

			if ( node.getValue() != null ) {
				length += 3; // " = "
				// Estimate the default value length from source text
				String valueSource = node.getValue().getSourceText();
				if ( valueSource != null ) {
					length += valueSource.length();
				}
			}

			if ( i < params.size() - 1 ) {
				length += 2; // ", "
			}
		}

		return length;
	}

	public void print( List<BoxArgumentDeclaration> params ) {
		var	currentDoc	= visitor.getCurrentDoc();

		var	paramsDoc	= visitor.pushDoc( DocType.GROUP );
		paramsDoc.append( "(" );

		var	size				= params.size();
		var	padding				= visitor.config.getFunction().getParameters().getPadding() || visitor.config.getParensPadding();
		var	multiline			= size >= visitor.config.getFunction().getParameters().getMultilineCount()
		    || calculateParameterListLength( params ) >= visitor.config.getFunction().getParameters().getMultilineLength();

		int	maxParamLeftLength	= 0;
		if ( multiline && visitor.config.getAlignConsecutiveAssignments() ) {
			for ( var node : params ) {
				if ( node.getValue() == null ) {
					continue;
				}
				int leftLength = 0;
				if ( node.getRequired() != null && node.getRequired() ) {
					leftLength += 9;
				}
				if ( node.getType() != null ) {
					var	type			= node.getType();
					var	typeIsPrinted	= type != "Any" || node.getSourceText().contains( "Any " );
					if ( typeIsPrinted ) {
						leftLength += type.length() + 1;
					}
				}
				leftLength			+= node.getName().length();
				maxParamLeftLength	= Math.max( maxParamLeftLength, leftLength );
			}
		}

		if ( size > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			if ( multiline ) {
				contentsDoc.append( Line.LINE );
			} else if ( padding ) {
				contentsDoc.append( " " );
			} else {
				contentsDoc.append( Line.SOFT );
			}

			for ( int i = 0; i < size; i++ ) {
				var node = params.get( i );
				visitor.printPreComments( node );
				if ( node.getRequired() != null && node.getRequired() ) {
					contentsDoc.append( "required " );
				}
				if ( node.getType() != null ) {
					var	type			= node.getType();
					var	typeIsPrinted	= type != "Any" || node.getSourceText().contains( "Any " );
					if ( typeIsPrinted ) {
						contentsDoc.append( type ).append( " " );
					}
				}
				contentsDoc.append( node.getName() );
				if ( node.getValue() != null ) {
					if ( multiline && visitor.config.getAlignConsecutiveAssignments() && maxParamLeftLength > 0 ) {
						int currentLeftLength = 0;
						if ( node.getRequired() != null && node.getRequired() ) {
							currentLeftLength += 9;
						}
						if ( node.getType() != null ) {
							var	type			= node.getType();
							var	typeIsPrinted	= type != "Any" || node.getSourceText().contains( "Any " );
							if ( typeIsPrinted ) {
								currentLeftLength += type.length() + 1;
							}
						}
						currentLeftLength += node.getName().length();
						for ( int j = 0; j < ( maxParamLeftLength - currentLeftLength ); j++ ) {
							contentsDoc.append( " " );
						}
					}
					contentsDoc.append( " = " );
					node.getValue().accept( visitor );
				}

				visitor.helperPrinter.printKeyValueAnnotations( node.getAnnotations(), false );

				visitor.printInsideComments( node, false );
				visitor.printPostComments( node );

				if ( i < size - 1 ) {
					contentsDoc.append( "," ).append( Line.LINE );
				} else if ( multiline && visitor.config.getFunction().getParameters().getCommaDangle() ) {
					// Add trailing comma for last parameter when multiline and comma_dangle is enabled
					contentsDoc.append( "," );
				}
			}

			if ( multiline ) {
				contentsDoc.append( Line.BREAK_PARENT );
			}

			paramsDoc.append( visitor.popDoc() );
			if ( multiline ) {
				paramsDoc.append( Line.LINE );
			} else if ( padding ) {
				paramsDoc.append( " " );
			} else {
				paramsDoc.append( Line.SOFT );
			}
		} else {
			if ( visitor.config.getFunction().getParameters().getEmptyPadding() ) {
				paramsDoc.append( " " );
			} else {
				paramsDoc.append( Line.SOFT );
			}
		}

		paramsDoc.append( ")" );
		currentDoc.append( visitor.popDoc() );

	}

}
