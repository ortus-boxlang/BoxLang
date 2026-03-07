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

import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;

public class CommentsPrinter {

	private static final int	SINGLE_LINE_PREFIX_LENGTH	= 3;	// "// "
	private static final int	MULTI_LINE_PREFIX_LENGTH	= 4;	// "/* " or " * "
	private static final int	DEFAULT_INDENT_ESTIMATE		= 8;	// Estimate for typical indentation

	private Visitor				visitor;

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
		var		currentDoc			= visitor.getCurrentDoc();
		boolean	preserveBlankLines	= visitor.config.getComments().getPreserveBlankLines();

		// If we have printed a comment, and this one starts on the same line, we need to append a space
		if ( node.startsOnEndLineOf( lastNodeToPrint ) ) {
			currentDoc.append( " " );
		} else {
			// this node starts on a new line
			currentDoc.append( Line.HARD );

			// check to see if there is a gap of multiple lines in the source
			// only preserve blank lines if the config option is enabled
			if ( preserveBlankLines && node.hasLinesBetween( lastNodeToPrint ) ) {
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
		var		currentDoc	= visitor.getCurrentDoc();
		boolean	wrap		= visitor.config.getComments().getWrap();
		String	text		= node.getCommentText();

		if ( wrap ) {
			int				maxWidth	= getAvailableWidth( SINGLE_LINE_PREFIX_LENGTH );
			List<String>	lines		= wrapText( text, maxWidth );

			for ( int i = 0; i < lines.size(); i++ ) {
				if ( i > 0 ) {
					currentDoc.append( Line.HARD );
				}
				currentDoc.append( "// " ).append( lines.get( i ) );
			}
		} else {
			currentDoc.append( "// " ).append( text );
		}
	}

	public void print( BoxMultiLineComment node ) {
		var		currentDoc	= visitor.getCurrentDoc();
		boolean	wrap		= visitor.config.getComments().getWrap();
		String	text		= node.getCommentText();

		if ( visitor.isTemplate() ) {
			currentDoc
			    .append( "<!--- " )
			    .append( text )
			    .append( " --->" );
		} else {
			currentDoc.append( "/*" );
			if ( !text.startsWith( "*" ) && !text.startsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			if ( wrap ) {
				printMultiLineWrapped( text );
			} else {
				printMultiLine( text );
			}
			if ( !text.endsWith( "*" ) && !text.endsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			currentDoc.append( "*/" );
		}
	}

	public void print( BoxDocComment node ) {
		var		currentDoc	= visitor.getCurrentDoc();
		boolean	wrap		= visitor.config.getComments().getWrap();
		String	text		= node.getCommentText();

		if ( visitor.isTemplate() ) {
			currentDoc
			    .append( "<!--- " )
			    .append( text )
			    .append( " --->" );
		} else {
			currentDoc.append( "/**" );
			if ( !text.startsWith( "*" ) && !text.startsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			if ( wrap ) {
				printMultiLineWrapped( text );
			} else {
				printMultiLine( text );
			}
			if ( !text.endsWith( "*" ) && !text.endsWith( "\n" ) ) {
				currentDoc.append( " " );
			}
			currentDoc.append( "*/" );
		}
	}

	/**
	 * Calculate the available width for comment text based on maxLineLength,
	 * accounting for the comment prefix and estimated indentation.
	 *
	 * @param prefixLength The length of the comment prefix (e.g., "// " = 3)
	 *
	 * @return The available width for comment text
	 */
	private int getAvailableWidth( int prefixLength ) {
		int maxLineLength = visitor.config.getMaxLineLength();
		// Subtract prefix length and a conservative indentation estimate
		return Math.max( 20, maxLineLength - prefixLength - DEFAULT_INDENT_ESTIMATE );
	}

	/**
	 * Wrap text at word boundaries to fit within the specified width.
	 *
	 * @param text     The text to wrap
	 * @param maxWidth The maximum width for each line
	 *
	 * @return A list of wrapped lines
	 */
	private List<String> wrapText( String text, int maxWidth ) {
		List<String> lines = new ArrayList<>();
		if ( text == null || text.isEmpty() ) {
			lines.add( "" );
			return lines;
		}

		// Split by existing newlines first to preserve intentional line breaks
		String[] paragraphs = text.split( "\\r?\\n", -1 );
		for ( String paragraph : paragraphs ) {
			if ( paragraph.isEmpty() ) {
				lines.add( "" );
				continue;
			}

			// Wrap each paragraph
			wrapParagraph( paragraph.trim(), maxWidth, lines );
		}

		return lines;
	}

	/**
	 * Wrap a single paragraph (no newlines) at word boundaries.
	 *
	 * @param paragraph The paragraph text to wrap
	 * @param maxWidth  The maximum width for each line
	 * @param lines     The list to add wrapped lines to
	 */
	private void wrapParagraph( String paragraph, int maxWidth, List<String> lines ) {
		if ( paragraph.length() <= maxWidth ) {
			lines.add( paragraph );
			return;
		}

		String[]		words		= paragraph.split( "\\s+" );
		StringBuilder	currentLine	= new StringBuilder();

		for ( String word : words ) {
			if ( currentLine.length() == 0 ) {
				// First word on the line
				currentLine.append( word );
			} else if ( currentLine.length() + 1 + word.length() <= maxWidth ) {
				// Word fits on current line
				currentLine.append( " " ).append( word );
			} else {
				// Word doesn't fit, start a new line
				lines.add( currentLine.toString() );
				currentLine = new StringBuilder( word );
			}
		}

		// Add the last line
		if ( currentLine.length() > 0 ) {
			lines.add( currentLine.toString() );
		}
	}

	/**
	 * Print multi-line comment text with word wrapping.
	 *
	 * @param text The text to print
	 */
	private void printMultiLineWrapped( String text ) {
		var				currentDoc		= visitor.getCurrentDoc();
		int				maxWidth		= getAvailableWidth( MULTI_LINE_PREFIX_LENGTH );
		List<String>	wrappedLines	= wrapText( text, maxWidth );
		int				numLines		= wrappedLines.size();

		for ( int i = 0; i < numLines; i++ ) {
			boolean	first	= i == 0;
			boolean	last	= i == numLines - 1;

			// For wrapped comments, all continuation lines get " * " prefix
			if ( !first ) {
				currentDoc.append( " * " );
			}

			if ( last ) {
				currentDoc.append( wrappedLines.get( i ) );
			} else {
				currentDoc.append( wrappedLines.get( i ) ).append( Line.HARD );
			}
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