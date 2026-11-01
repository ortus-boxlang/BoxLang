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

import ortus.boxlang.compiler.ast.BoxClass;
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
		return printTrailingComments( node, false );
	}

	/**
	 * Prints the comments of a leaf node (one with nothing inside it to print them next to, like an identifier) that
	 * the parser placed inside it, as trailing comments. A source position that spans too far (a method name in a
	 * multi-line call) puts a later comment "inside" the name; nothing printed those, so the comment was lost.
	 *
	 * @param node the leaf node
	 *
	 * @return true when a comment was printed
	 */
	boolean printLeafInsideComments( BoxNode node ) {
		return printTrailingComments( node, true );
	}

	private boolean printTrailingComments( BoxNode node, boolean inside ) {
		var		currentDoc		= visitor.getCurrentDoc();
		boolean	printed			= false;
		BoxNode	lastNodeToPrint	= node;

		for ( var comment : node.getComments() ) {
			if ( inside ? comment.isInside( node ) : comment.isAfter( node ) ) {
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
		return printInsideComments( node, indent, false );
	}

	/**
	 * Prints the comments inside a node (after its last child).
	 *
	 * @param node             the node
	 * @param indent           whether to indent the comments on a new line
	 * @param atStartOfNewLine true when the caller has already started a new line for them: the first comment then takes
	 *                         no spacing before it
	 *
	 * @return true when at least one comment was printed
	 */
	boolean printInsideComments( BoxNode node, boolean indent, boolean atStartOfNewLine ) {
		var		currentDoc		= visitor.getCurrentDoc();

		Doc		commentsDoc		= null;
		BoxNode	lastNodeToPrint	= null;

		for ( var comment : node.getComments() ) {
			if ( comment.isInside( node ) ) {
				if ( commentsDoc == null ) {
					lastNodeToPrint	= atStartOfNewLine ? null : findPreviousNode( node, comment );
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

		// In a template every character between tags is output, and source whitespace is its own text node: a comment
		// directly followed by a tag ("---><cfscript>") must stay directly followed by it
		if ( visitor.isTemplate() && startsRightAfter( node, lastNodeToPrint ) ) {
			return;
		}
		// If we have printed a comment, and this one starts on the same line, we need to append a space. Never after a
		// "//" comment, which runs to the end of the line: what follows would become part of it (a file with CR-only
		// line endings reports the next statement as being on the same line).
		if ( node.startsOnEndLineOf( lastNodeToPrint ) && ! ( lastNodeToPrint instanceof BoxSingleLineComment ) ) {
			currentDoc.append( " " );
		} else {
			// this node starts on a new line
			currentDoc.append( Line.HARD );

			// check to see if there is a gap of multiple lines in the source
			// only preserve blank lines if the config option is enabled
			if ( preserveBlankLines && node.hasLinesBetween( lastNodeToPrint ) && !hasPreAnnotations( node ) ) {
				// if so, print an extra new line (eliminating line gaps greater than 1)
				currentDoc.append( Line.HARD );
			}
		}
	}

	/**
	 * Whether a node starts exactly where another one ends, with nothing in between.
	 *
	 * @param node     the later node
	 * @param previous the earlier node
	 *
	 * @return true when they are adjacent in the source
	 */
	private boolean startsRightAfter( BoxNode node, BoxNode previous ) {
		if ( node.getPosition() == null || previous.getPosition() == null ) {
			return false;
		}
		var	start	= node.getPosition().getStart();
		var	end		= previous.getPosition().getEnd();
		return start.getLine() == end.getLine() && start.getColumn() == end.getColumn();
	}

	/**
	 * Determines whether a node has class-level annotations that occupy the source lines before the node declaration.
	 *
	 * @param node node being checked
	 *
	 * @return {@code true} when the node is a class with pre-annotations
	 */
	private boolean hasPreAnnotations( BoxNode node ) {
		return node instanceof BoxClass classNode && !classNode.getAnnotations().isEmpty();
	}

	/**
	 * Print multi-line output, respecting indentation
	 * This will trim existing whitespace off each line.
	 *
	 * @param text The text to print
	 */
	public void printMultiLine( String text ) {
		printMultiLine( text, null );
	}

	/**
	 * Prints the lines of a block comment, prefixing each middle line with {@code " * "}.
	 *
	 * @param text     the comment text
	 * @param rawLines the comment's source lines, or null. For a doc comment the prefix of each line is taken from its
	 *                 source line, because the parsed text reads {@code " ** x"} and {@code " * ** x"} the same while the
	 *                 documentation parser does not: the first yields {@code x}, the second {@code ** x}.
	 */
	public void printMultiLine( String text, String[] rawLines ) {
		String[]	lines		= text.split( "\\r?\\n", -1 );
		if ( rawLines != null && rawLines.length != lines.length ) {
			rawLines = null;
		}
		int			numLines	= lines.length;
		boolean		first		= true;
		var			currentDoc	= visitor.getCurrentDoc();
		for ( int i = 0; i < numLines; i++ ) {
			boolean last = i == numLines - 1;
			if ( !first && !last ) {
				currentDoc.append( linePrefix( lines[ i ], rawLines != null ? rawLines[ i ] : null ) );
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

	/**
	 * The prefix of a middle comment line.
	 *
	 * @param line    the parsed line text
	 * @param rawLine the line as written (doc comments), or null (plain comments)
	 *
	 * @return {@code " * "}, or a single space when the line itself starts with stars that must stay in place
	 */
	private String linePrefix( String line, String rawLine ) {
		if ( rawLine != null ) {
			// Doc comment: keep the star prefix as written. A line written as "** x" or "****" keeps its own stars in place
			// of the prefix; a line with a normal prefix ("* x", "*\t* x") gets " * ".
			return rawLine.strip().startsWith( "**" ) && line.startsWith( "*" ) ? " " : " * ";
		}
		// Plain comment: the parser keeps a line that starts with "**" (a banner row of stars) whole, so it takes the
		// place of the prefix; " * ****" would add a star on every pass
		return line.startsWith( "**" ) ? " " : " * ";
	}

	public void printSingleLineComment( BoxSingleLineComment node ) {
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

	public void printMultiLineComment( BoxMultiLineComment node ) {
		var		currentDoc	= visitor.getCurrentDoc();
		boolean	wrap		= visitor.config.getComments().getWrap();
		String	text		= node.getCommentText();

		if ( visitor.isTemplate() ) {
			currentDoc
			    .append( "<!--- " )
			    .append( text )
			    .append( " --->" );
		} else if ( text.isBlank() ) {
			// an empty comment prints as "/* */": padding its whitespace would grow it on every pass
			currentDoc.append( "/* */" );
		} else {
			currentDoc.append( "/*" );
			// A plain comment whose text starts with "*" (from "/* ****") keeps the space: "/***" would open a doc
			// comment, which attaches to the next declaration as its documentation (hint)
			if ( !text.startsWith( "\n" ) ) {
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
	 * A doc comment's source lines between {@code /**} and the closing {@code *}{@code /}, or null when unknown.
	 *
	 * @param node the doc comment
	 *
	 * @return the source lines
	 */
	private String[] rawDocLines( BoxDocComment node ) {
		String source = node.getSourceText();
		if ( source == null ) {
			return null;
		}
		source = source.strip();
		if ( !source.startsWith( "/**" ) || !source.endsWith( "*/" ) || source.length() < 5 ) {
			return null;
		}
		return source.substring( 3, source.length() - 2 ).split( "\\r?\\n", -1 );
	}

	public void printDocComment( BoxDocComment node ) {
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
				printMultiLine( text, rawDocLines( node ) );
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