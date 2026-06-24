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
import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;

/**
 * Shared PrettyPrint helper methods for formatting statement lists, blocks, template bodies, parenthesized expressions,
 * and annotation-style key/value pairs.
 * <p>
 * This class owns formatter behavior that is reused by multiple specialized printers. It writes directly to the active
 * {@link Visitor} document stack, so callers are responsible for invoking these helpers at the point where their output
 * belongs in the current document model.
 */
public class HelperPrinter {

	private Visitor visitor;

	/**
	 * Creates a helper printer bound to the active PrettyPrint visitor.
	 *
	 * @param visitor active visitor and document stack owner
	 */
	public HelperPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	/**
	 * Prints a sequence of script statements, preserving configured statement spacing, class member spacing, comments,
	 * and formatter-ignore regions.
	 * <p>
	 * When a formatter-ignore-start marker is encountered, statements are emitted from their original source text until
	 * a matching formatter-ignore-end pre-comment is found. Statements outside ignore regions are delegated back to the
	 * visitor for normal formatting.
	 *
	 * @param statements ordered statements to print; null or empty lists are ignored
	 */
	public void printStatements( List<BoxStatement> statements ) {
		if ( statements == null || statements.isEmpty() ) {
			return;
		}

		BoxStatement	lastStatement		= statements.get( statements.size() - 1 );
		BoxStatement	previousStatement	= null;
		boolean			ignoreMode			= false;

		// Get member spacing for class members (default is 1 blank line between functions)
		int				memberSpacing		= visitor.config.getClassConfig().getMemberSpacing();

		for ( var statement : statements ) {

			// --- Ignore-mode exit: statement has an ignore-end pre-comment ---
			if ( ignoreMode ) {
				boolean hasEndMarker = hasFormatterIgnoreEnd( statement );
				if ( hasEndMarker ) {
					// Exit ignore mode — format this statement normally.
					// printPreComments will emit the end-marker as a regular comment.
					ignoreMode = false;
				} else {
					// Still in ignore mode — emit raw
					emitRawStatement( statement );
					if ( statement != lastStatement ) {
						visitor.newLine();
					}
					previousStatement = statement;
					continue;
				}
			}

			// --- Ignore-mode entry: statement has an ignore-start pre-comment ---
			if ( !ignoreMode && hasFormatterIgnoreStart( statement ) ) {
				ignoreMode = true;
				emitRawStatement( statement );
				if ( statement != lastStatement ) {
					visitor.newLine();
				}
				previousStatement = statement;
				continue;
			}

			// --- Normal formatting ---

			// Check if this is a class member (function in a class or interface)
			boolean isClassMember = statement instanceof BoxFunctionDeclaration &&
			    ( statement.getParent() instanceof BoxClass || statement.getParent() instanceof BoxInterface );

			// if there is a previous statement, check for empty lines in source
			// if so, add a hard line break
			if ( previousStatement != null && statement.hasLinesBetweenWithComments( previousStatement ) ) {
				// Check if we should preserve blank lines before comments
				boolean	preserveBlankLines	= visitor.config.getComments().getPreserveBlankLines();
				boolean	hasPreComments		= statement.getComments().stream().anyMatch( c -> c.isBefore( statement ) );

				// If the statement has pre-comments, respect the preserve_blank_lines setting
				// Otherwise, always preserve blank lines between statements
				if ( !hasPreComments || preserveBlankLines ) {
					visitor.newLine();
				}
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

	/**
	 * Checks whether a statement has a formatter-ignore-start marker in its leading single-line comments.
	 *
	 * @param statement statement whose pre-comments should be inspected
	 *
	 * @return true when the statement begins a formatter-ignore region
	 */
	private boolean hasFormatterIgnoreStart( BoxStatement statement ) {
		for ( BoxComment comment : statement.getComments() ) {
			if ( comment.isBefore( statement ) && comment instanceof BoxSingleLineComment slc && slc.isFormatterIgnoreStart() ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a statement has a formatter-ignore-end marker in its leading single-line comments.
	 *
	 * @param statement statement whose pre-comments should be inspected
	 *
	 * @return true when the statement ends a formatter-ignore region
	 */
	private boolean hasFormatterIgnoreEnd( BoxStatement statement ) {
		for ( BoxComment comment : statement.getComments() ) {
			if ( comment.isBefore( statement ) && comment instanceof BoxSingleLineComment slc && slc.isFormatterIgnoreEnd() ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Emits a statement and its associated comments as raw (unformatted) text.
	 * Used when inside a formatter-ignore region to preserve original source formatting.
	 * Falls back to normal formatting if the node has no raw source text (e.g., transpiler-injected nodes).
	 *
	 * @param statement statement to emit without reformatting when raw source is available
	 */
	private void emitRawStatement( BoxStatement statement ) {
		Doc		currentDoc			= visitor.getCurrentDoc();

		// Emit pre-comments raw
		boolean	emittedPreComment	= false;
		for ( BoxComment comment : statement.getComments() ) {
			if ( comment.isBefore( statement ) ) {
				if ( emittedPreComment ) {
					currentDoc.append( Line.HARD );
				}
				currentDoc.append( comment.getSourceText() );
				emittedPreComment = true;
			}
		}

		// Emit statement source text raw, or fall back to formatted output
		String raw = visitor.extractRawSourceFromPosition( statement );
		if ( raw != null ) {
			if ( emittedPreComment ) {
				currentDoc.append( Line.HARD );
			}
			currentDoc.append( raw );
		} else {
			// No raw source available (e.g., transpiler-injected AST nodes) — format normally
			if ( emittedPreComment ) {
				currentDoc.append( Line.HARD );
			}
			statement.accept( visitor );
		}

		// Emit post-comments raw (same-line comments)
		for ( BoxComment comment : statement.getComments() ) {
			if ( comment.isAfter( statement ) ) {
				currentDoc.append( " " ).append( comment.getSourceText() );
			}
		}
	}

	/**
	 * Prints template body statements with template indent-content support.
	 * <p>
	 * When {@code template.indent_content} is enabled, structural whitespace-only buffer output nodes are filtered and
	 * replaced with {@link Line#HARD} separators. Meaningful body content is indented one level and a trailing hard line is
	 * appended so callers can print the closing tag at the outer indentation level. When indent-content is disabled,
	 * statements are visited as-is.
	 *
	 * @param statements template body statements to print
	 */
	public void printTemplateBody( List<? extends BoxStatement> statements ) {
		if ( !visitor.config.getTemplate().getIndentContent() ) {
			for ( var statement : statements ) {
				if ( statement != null ) {
					statement.accept( visitor );
				}
			}
			return;
		}

		boolean hasAnyMeaningful = statements.stream()
		    .anyMatch( s -> s != null && !isWhitespaceOnlyBuffer( s ) );

		if ( !hasAnyMeaningful ) {
			return;
		}

		var		indentDoc				= visitor.pushDoc( DocType.INDENT );
		boolean	lastWasBuffer			= false;
		// Tracks whether a whitespace-only newline buffer was filtered between
		// two consecutive BufferOutput nodes, indicating a meaningful line break
		// (e.g., #html.doctype()# followed by a newline then <html lang="en">).
		boolean	hadNewlineSinceBuffer	= false;
		visitor.stripBufferLeadingWhitespace = true;
		for ( var statement : statements ) {
			if ( statement == null )
				continue;

			// Skip whitespace-only buffers but remember that a newline separator was present
			if ( isWhitespaceOnlyBuffer( statement ) ) {
				hadNewlineSinceBuffer = true;
				continue;
			}

			boolean isBuffer = statement instanceof BoxBufferOutput;
			// Break before non-buffer statements, before the first BufferOutput in a
			// consecutive run, when a whitespace-newline buffer was filtered between two
			// consecutive BufferOutputs, or when the next buffer's string content starts
			// with a newline (the stripped newline represents a meaningful line break).
			if ( !isBuffer || !lastWasBuffer || hadNewlineSinceBuffer || bufferStartsWithNewline( statement ) ) {
				indentDoc.append( Line.HARD );
			}
			lastWasBuffer			= isBuffer;
			hadNewlineSinceBuffer	= false;
			statement.accept( visitor );
		}
		visitor.stripBufferLeadingWhitespace = false;
		var contentsDoc = visitor.popDoc();
		visitor.getCurrentDoc()
		    .append( contentsDoc )
		    .append( Line.HARD );
	}

	/**
	 * Determines whether a statement is a discardable template buffer that only contains structural whitespace.
	 * <p>
	 * Pure horizontal whitespace is preserved because it can be meaningful between inline template expressions. Only blank
	 * buffers containing a newline are treated as formatting structure.
	 *
	 * @param statement candidate template statement
	 *
	 * @return true when the statement is a whitespace-only buffer that can be replaced by formatter line structure
	 */
	private boolean isWhitespaceOnlyBuffer( BoxStatement statement ) {
		if ( ! ( statement instanceof BoxBufferOutput bufOutput ) ) {
			return false;
		}
		var expr = bufOutput.getExpression();
		if ( ! ( expr instanceof BoxStringLiteral str ) ) {
			return false;
		}
		// Only treat as discardable whitespace if it contains a newline (structural indent).
		// Pure horizontal spaces (e.g., between #expr1# #expr2#) are meaningful inline content.
		String value = str.getValue();
		return value.isBlank() && ( value.contains( "\n" ) || value.contains( "\r" ) );
	}

	/**
	 * Returns true if a {@link BoxBufferOutput} string literal starts with a newline character.
	 * <p>
	 * This preserves cases where consecutive buffer output represents content on a new source line, for example
	 * {@code "\n<html lang=\"en\">"}. The emitted {@link Line#HARD} compensates for the leading newline that is stripped
	 * during buffer normalization.
	 *
	 * @param statement candidate statement to inspect
	 *
	 * @return true when the statement is a buffer output whose string content begins with a newline
	 */
	private boolean bufferStartsWithNewline( BoxStatement statement ) {
		if ( ! ( statement instanceof BoxBufferOutput bufOutput ) ) {
			return false;
		}
		var expr = bufOutput.getExpression();
		if ( expr instanceof BoxStringLiteral str ) {
			String v = str.getValue();
			return v.startsWith( "\n" ) || v.startsWith( "\r" );
		}
		return false;
	}

	/**
	 * Prints a script or template block body for the supplied node.
	 * <p>
	 * Template nodes delegate to {@link #printTemplateBody(List)}. Script nodes emit braces according to
	 * {@code braces.style}, indent statements one level, include inside comments, and close the block at the caller's
	 * indentation level.
	 *
	 * @param node       block-owning AST node, used for brace preservation and inside comments
	 * @param statements statements contained by the block
	 */
	public void printBlock( BoxNode node, List<BoxStatement> statements ) {
		var currentDoc = visitor.getCurrentDoc();
		if ( visitor.isTemplate() ) {
			printTemplateBody( statements );
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
	 * Checks whether the original source placed the opening brace on a later line than the node header.
	 *
	 * @param node node whose source text should be inspected
	 *
	 * @return true when source text contains a newline before the first opening brace
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

	/**
	 * Prints an expression wrapped in parentheses while honoring the configured parenthesis padding behavior.
	 *
	 * @param node expression to print inside parentheses
	 */
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

	/**
	 * Prints annotation-style key/value pairs using normal line breaking and assignment alignment behavior.
	 *
	 * @param attrs  annotations or attributes to print
	 * @param padded true to leave a trailing break/space after the attributes for padded contexts such as headers
	 */
	public void printKeyValueAnnotations( List<BoxAnnotation> attrs, boolean padded ) {
		printKeyValueAnnotations( attrs, padded, false );
	}

	/**
	 * Prints annotation-style key/value pairs, optionally forcing each attribute onto its own line.
	 *
	 * @param attrs           annotations or attributes to print
	 * @param padded          true to leave a trailing break/space after the attributes for padded contexts such as headers
	 * @param forceLineBreaks true to emit hard line breaks between attributes, used by single-attribute-per-line modes
	 */
	public void printKeyValueAnnotations( List<BoxAnnotation> attrs, boolean padded, boolean forceLineBreaks ) {
		printKeyValueAnnotations( attrs, padded, forceLineBreaks, true );
	}

	/**
	 * Prints annotation-style key/value pairs such as class annotations, component attributes, or template tag attributes.
	 * <p>
	 * Values are emitted as quoted expressions via the string printer. When assignment alignment is enabled and allowed,
	 * keys are padded to the longest key length in the group before the {@code =}. Header contexts can pass
	 * {@code alignAssignments=false} to avoid padding declarations such as {@code extends="..."} to match longer sibling
	 * attributes.
	 *
	 * @param attrs            annotations or attributes to print
	 * @param padded           true to leave a trailing break/space after the attributes for padded contexts such as headers
	 * @param forceLineBreaks  true to emit hard line breaks between attributes
	 * @param alignAssignments true to apply configured consecutive assignment alignment when script mode allows it
	 */
	public void printKeyValueAnnotations( List<BoxAnnotation> attrs, boolean padded, boolean forceLineBreaks, boolean alignAssignments ) {
		var	currentDoc		= visitor.getCurrentDoc();
		var	attrsDoc		= visitor.pushDoc( DocType.GROUP );
		int	maxKeyLength	= 0;
		// Alignment only applies to script mode (e.g. struct literals), not template tag attributes
		if ( alignAssignments && visitor.config.getAlignConsecutiveAssignments() && !visitor.isTemplate() ) {
			for ( var attr : attrs ) {
				if ( attr.getValue() != null && attr.getKey() != null ) {
					String effectiveKey = getEffectiveKeyText( attr.getKey() );
					maxKeyLength = Math.max( maxKeyLength, effectiveKey != null ? effectiveKey.length() : 0 );
				}
			}
		}
		if ( !attrs.isEmpty() ) {
			var contentsDoc = visitor.pushDoc( DocType.INDENT );
			for ( var attr : attrs ) {
				// Use HARD line breaks when forceLineBreaks is true (single_attribute_per_line)
				contentsDoc.append( forceLineBreaks ? Line.HARD : Line.LINE );
				String keyText = getEffectiveKeyText( attr.getKey() );
				if ( keyText != null ) {
					contentsDoc.append( keyText );
				} else {
					attr.getKey().accept( visitor );
					keyText = "";
				}
				if ( attr.getValue() != null ) {
					if ( alignAssignments && visitor.config.getAlignConsecutiveAssignments() && maxKeyLength > 0 ) {
						contentsDoc.append( " ".repeat( Math.max( 0, maxKeyLength - keyText.length() ) ) );
					}
					contentsDoc.append( "=\"" );
					visitor.stringPrinter.printQuotedExpression( attr.getValue() );
					contentsDoc.append( "\"" );
				}
			}
			attrsDoc.append( visitor.popDoc() );
		}
		// When forceLineBreaks is true, add a trailing HARD to put the following { on its own line
		if ( !forceLineBreaks ) {
			attrsDoc.append( padded ? Line.LINE : Line.SOFT );
		} else if ( padded ) {
			// Only add trailing HARD/LINE for padded contexts (e.g. class declarations, not template attributes)
			attrsDoc.append( !attrs.isEmpty() ? Line.HARD : Line.LINE );
		}
		currentDoc.append( visitor.popDoc() );
	}

	/**
	 * Returns the effective key text for an annotation key node, preferring the current
	 * AST value (from BoxFQN.getValue()) over the original source text. This correctly
	 * handles cases where the transpiler has modified the key (e.g. cfsqltype -> sqltype).
	 *
	 * @param keyNode The annotation key expression node
	 *
	 * @return The effective key text to use for printing, or null if not determinable
	 */
	private String getEffectiveKeyText( BoxExpression keyNode ) {
		if ( keyNode instanceof BoxFQN fqn ) {
			// BoxFQN.getValue() reflects any transpiler mutations, while getSourceText()
			// retains the original source. Prefer the current logical value.
			return fqn.getValue() != null ? fqn.getValue() : fqn.getSourceText();
		}
		return keyNode.getSourceText();
	}

	/**
	 * Prints a control-flow statement body, optionally wrapping single statements in braces according to
	 * {@code braces.require_for_single_statement}.
	 * <p>
	 * Template mode prints the statement directly because template bodies do not use script braces. Script mode preserves
	 * existing statement blocks, wraps single statements when configured, or emits an indented single statement when braces
	 * are optional.
	 *
	 * @param node      parent node for future source-aware behavior
	 * @param statement statement body to print
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
			var		currentDoc		= visitor.getCurrentDoc();

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
			var	currentDoc	= visitor.getCurrentDoc();
			var	blockDoc	= visitor.pushDoc( DocType.INDENT );
			blockDoc.append( Line.HARD );

			statement.accept( visitor );

			currentDoc.append( visitor.popDoc() );
		}
	}
}
