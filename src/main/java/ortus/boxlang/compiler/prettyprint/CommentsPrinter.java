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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;

public class CommentsPrinter {

	private Visitor visitor;

	public CommentsPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	/**
	 * Prints pre comments
	 * 
	 * @param node
	 */
	boolean printPreComments( BoxNode node ) {
		BoxNode lastNodeToPrint = null;

		for ( var comment : node.getComments() ) {
			if ( comment.isBefore( node ) ) {
				if ( lastNodeToPrint != null ) {
					printCommentSpacing( lastNodeToPrint, comment );
				}

				comment.accept( visitor );
				lastNodeToPrint = comment;
			}
		}

		if ( lastNodeToPrint != null ) {
			printCommentSpacing( lastNodeToPrint, node );
		}

		return lastNodeToPrint != null;
	}

	boolean printPostComments( BoxNode node ) {
		var		currentDoc		= visitor.getCurrentDoc();
		boolean	printed			= false;
		BoxNode	lastNodeToPrint	= node;

		for ( var comment : node.getComments() ) {
			if ( comment.isAfter( node ) ) {
				var	isBlockComment	= comment instanceof BoxMultiLineComment;
				var	commentDoc		= visitor.pushDoc( isBlockComment ? DocType.ARRAY : DocType.LINE_SUFFIX );

				// if the last node to print is not the same line as this comment, we need to add a hard line break
				if ( !comment.startsOnEndLineOf( lastNodeToPrint ) ) {
					commentDoc.append( Line.HARD );
				} else if ( !isBlockComment ) {
					commentDoc.append( " " );
				}

				comment.accept( visitor );
				lastNodeToPrint = comment;

				currentDoc.append( visitor.popDoc() );
				if ( !isBlockComment ) {
					currentDoc.append( Line.BREAK_PARENT );
				}
				printed = true;
			}
		}
		return printed;
	}

	boolean printInsideComments( BoxNode node, boolean indent ) {
		var		currentDoc		= visitor.getCurrentDoc();

		Doc		commentsDoc		= null;
		BoxNode	lastNodeToPrint	= null;

		for ( var comment : node.getComments() ) {
			if ( comment.isInside( node ) ) {
				if ( commentsDoc == null ) {
					lastNodeToPrint	= findPreviousNode( node, comment );
					commentsDoc		= visitor.pushDoc( DocType.ARRAY );
				}
				if ( lastNodeToPrint != null ) {
					printCommentSpacing( lastNodeToPrint, comment );
				}

				comment.accept( visitor );
				lastNodeToPrint = comment;
			}
		}

		if ( commentsDoc != null ) {
			visitor.popDoc(); // pop the comments doc
			if ( indent ) {
				visitor.pushDoc( DocType.INDENT )
				    .append( Line.HARD )
				    .append( commentsDoc );
				currentDoc.append( visitor.popDoc() );
			} else {
				currentDoc.append( commentsDoc );
			}
		}

		return commentsDoc != null;
	}

	/**
	 * print comment line spacing
	 */
	void printCommentSpacing( BoxNode lastNodeToPrint, BoxNode node ) {
		var currentDoc = visitor.getCurrentDoc();
		// If we have printed a comment, and this one starts on the same line, we need to append a space
		if ( node.startsOnEndLineOf( lastNodeToPrint ) ) {
			currentDoc.append( " " );
		} else {
			// this node starts on a new line
			currentDoc.append( Line.HARD );

			// check to see if there is a gap of multiple lines in the source
			if ( node.hasLinesBetween( lastNodeToPrint ) ) {
				// if so, print an extra new line (eliminating line gaps greater than 1)
				currentDoc.append( Line.HARD );
			}
		}
	}

	/**
	 * Print multi-line output, respecting indentation
	 * This will trim existing whitespace off each line.
	 * 
	 * @param text The text to print
	 */
	public void printMultiLine( String text ) {
		String[]	lines		= text.split( "\\r?\\n", -1 );
		int			numLines	= lines.length;
		boolean		first		= true;
		var			currentDoc	= visitor.getCurrentDoc();
		for ( int i = 0; i < numLines; i++ ) {
			boolean last = i == numLines - 1;
			if ( !first && !last ) {
				currentDoc.append( " * " );
			} else if ( numLines > 1 && last ) {
				currentDoc.append( " " );
			}
			if ( last ) {
				currentDoc.append( lines[ i ] );
			} else {
				currentDoc.append( lines[ i ] ).append( Line.HARD );
			}
			first = false;
		}
	}

	public void print( BoxSingleLineComment node ) {
		visitor.getCurrentDoc()
		    .append( "// " )
		    .append( node.getCommentText() );

	}

	public void print( BoxMultiLineComment node ) {
		var currentDoc = visitor.getCurrentDoc();
		if ( visitor.isTemplate() ) {
			currentDoc
			    .append( "<!--- " )
			    .append( node.getCommentText() )
			    .append( " --->" );
		} else {
			currentDoc.append( "/*" );
			if ( !node.getCommentText().startsWith( "*" ) && !node.getCommentText().startsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			printMultiLine( node.getCommentText() );
			if ( !node.getCommentText().endsWith( "*" ) && !node.getCommentText().endsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			currentDoc.append( "*/" );
		}
	}

	public void print( BoxDocComment node ) {
		var currentDoc = visitor.getCurrentDoc();
		if ( visitor.isTemplate() ) {
			currentDoc
			    .append( "<!--- " )
			    .append( node.getCommentText() )
			    .append( " --->" );
		} else {
			currentDoc.append( "/**" );
			if ( !node.getCommentText().startsWith( "*" ) && !node.getCommentText().startsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			printMultiLine( node.getCommentText() );
			if ( !node.getCommentText().endsWith( "*" ) && !node.getCommentText().endsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			currentDoc.append( "*/" );
		}
	}

	private BoxNode findPreviousNode( BoxNode parentNode, BoxComment comment ) {
		BoxNode previousNode = null;
		// Note, children are in order, so we can just iterate until we find the comment
		// TODO: check if there are circumstances where this is not true
		for ( var node : parentNode.getChildren() ) {
			if ( node == comment ) {
				break;
			} else if ( node instanceof BoxComment ) {
				// skip comments
				continue;
			}
			previousNode = node;
		}
		return previousNode;
	}

}