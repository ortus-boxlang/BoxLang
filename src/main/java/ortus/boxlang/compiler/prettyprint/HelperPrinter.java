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
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;

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

		// Get member spacing for class members (default is 1 blank line between functions)
		int memberSpacing = visitor.config.getClassConfig().getMemberSpacing();

		for ( var statement : statements ) {
			// Check if this is a class member (function in a class or interface)
			boolean isClassMember = statement instanceof BoxFunctionDeclaration &&
			    ( statement.getParent() instanceof BoxClass || statement.getParent() instanceof BoxInterface );

			// if there is a previous statement, check for empty lines in source
			// if so, add a hard line break
			if ( previousStatement != null && statement.hasLinesBetweenWithComments( previousStatement ) ) {
				visitor.newLine();
			} else if ( isClassMember ) {
				// For class members, add configured member_spacing blank lines
				for ( int i = 0; i < memberSpacing; i++ ) {
					visitor.newLine();
				}
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
			// Determine if opening brace should be on a new line based on braces.style config
			String	braceStyle		= visitor.config.getBraces().getStyle();
			boolean	braceOnNewLine	= false;

			if ( braceStyle.equals( "new-line" ) ) {
				braceOnNewLine = true;
			} else if ( braceStyle.equals( "preserve" ) ) {
				// Check if the original source had the brace on a new line
				braceOnNewLine = hasBraceOnNewLine( node );
			}
			// "same-line" (default) keeps braceOnNewLine as false

			if ( braceOnNewLine ) {
				currentDoc.append( Line.HARD );
			}
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

	/**
	 * Check if the original source code had the opening brace on a new line.
	 * Used for "preserve" brace style mode.
	 */
	private boolean hasBraceOnNewLine( BoxNode node ) {
		String sourceText = node.getSourceText();
		if ( sourceText == null ) {
			return false;
		}

		// Find the opening brace and check if there's a newline before it
		int braceIndex = sourceText.indexOf( '{' );
		if ( braceIndex <= 0 ) {
			return false;
		}

		// Check if there's a newline between the start and the brace
		String beforeBrace = sourceText.substring( 0, braceIndex );
		return beforeBrace.contains( "\n" ) || beforeBrace.contains( "\r" );
	}

	public void printParensExpression( BoxExpression node ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	parensDoc	= visitor.pushDoc( DocType.GROUP ).append( "(" );
		visitor.pushDoc( DocType.INDENT ).append( visitor.config.getParensPadding() ? Line.LINE : Line.SOFT );
		node.accept( visitor );
		parensDoc
		    .append( visitor.popDoc() )
		    .append( visitor.config.getParensPadding() ? Line.LINE : Line.SOFT )
		    .append( ")" );

		currentDoc.append( visitor.popDoc() );
	}

	public void printKeyValueAnnotations( List<BoxAnnotation> attrs, boolean padded ) {
		printKeyValueAnnotations( attrs, padded, false );
	}

	public void printKeyValueAnnotations( List<BoxAnnotation> attrs, boolean padded, boolean forceLineBreaks ) {
		var	currentDoc	= visitor.getCurrentDoc();
		var	attrsDoc	= visitor.pushDoc( DocType.GROUP );
		if ( attrs.size() > 0 ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			for ( var attr : attrs ) {
				// Use HARD line breaks when forceLineBreaks is true (single_attribute_per_line)
				contentsDoc.append( forceLineBreaks ? Line.HARD : Line.LINE );
				attr.getKey().accept( visitor );
				if ( attr.getValue() != null ) {
					contentsDoc.append( "=\"" );
					visitor.stringPrinter.printQuotedExpression( attr.getValue() );
					contentsDoc.append( "\"" );
				}
			}
			attrsDoc.append( visitor.popDoc() );
		}
		// When forceLineBreaks is true, don't add trailing soft line (it would break unnecessarily)
		if ( !forceLineBreaks ) {
			attrsDoc.append( padded ? Line.LINE : Line.SOFT );
		}
		currentDoc.append( visitor.popDoc() );
	}

	/**
	 * Print a statement body, optionally wrapping single statements in braces
	 * based on the braces.require_for_single_statement configuration.
	 *
	 * @param node      The parent node (for source info if needed)
	 * @param statement The statement to print
	 */
	public void printStatementBody( BoxNode node, BoxStatement statement ) {
		if ( visitor.isTemplate() ) {
			// Template mode doesn't use braces
			statement.accept( visitor );
			return;
		}

		boolean requireBraces = visitor.config.getBraces().getRequireForSingleStatement();

		// Check if the statement is a block statement
		if ( statement instanceof ortus.boxlang.compiler.ast.statement.BoxStatementBlock ) {
			// Already a block, just visit it normally
			statement.accept( visitor );
		} else if ( requireBraces ) {
			// Single statement and we need to wrap it in braces
			var currentDoc = visitor.getCurrentDoc();

			// Determine if opening brace should be on a new line based on braces.style config
			String	braceStyle		= visitor.config.getBraces().getStyle();
			boolean	braceOnNewLine	= false;

			if ( braceStyle.equals( "new-line" ) ) {
				braceOnNewLine = true;
			} else if ( braceStyle.equals( "preserve" ) ) {
				// For single statements being wrapped, default to same-line
				// since there was no original brace to preserve
				braceOnNewLine = false;
			}

			if ( braceOnNewLine ) {
				currentDoc.append( Line.HARD );
			}
			currentDoc.append( "{" );

			var blockDoc = visitor.pushDoc( DocType.INDENT );
			blockDoc.append( Line.HARD );

			statement.accept( visitor );

			currentDoc
			    .append( visitor.popDoc() )
			    .append( Line.HARD )
			    .append( "}" );
		} else {
			// Single statement without braces - need to indent it
			var currentDoc = visitor.getCurrentDoc();
			var blockDoc = visitor.pushDoc( DocType.INDENT );
			blockDoc.append( Line.HARD );

			statement.accept( visitor );

			currentDoc.append( visitor.popDoc() );
		}
	}
}
