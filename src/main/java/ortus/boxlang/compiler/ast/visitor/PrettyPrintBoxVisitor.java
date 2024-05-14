/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.ast.visitor;

import java.util.Stack;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNegateOperation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxAssert;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Pretty print BoxLang AST nodes
 * 
 * TODO Items:
 * - Add configuration for indent size
 * - Add any other config settings such as white space inside paren, etc
 * - Modify AST to track pre annotations and inline annotations separately
 * - Test!
 */
public class PrettyPrintBoxVisitor extends VoidBoxVisitor {

	/**
	 * Buffer to hold the output
	 */

	private StringBuffer			buffer				= new StringBuffer();

	/**
	 * Indent string. Make this configurabl
	 */
	private String					indent				= "\t";

	private String					lineBreak			= "\n";

	/**
	 * Track how deeply we're indented
	 */
	private int						indentLevel			= 0;

	BoxNode							lastNodeToPrint		= new BoxNull( null, null );

	/**
	 * Using our existing BoxSourceType enum to track whether we're in a tag or script
	 * We'll only use the Box types here, never the BL types since this visitor only creates BL source code.
	 * Each visitor method decides if it needs to obey this. Many AST nodes print the same regardless of the source type
	 */
	private Stack<BoxSourceType>	currentSourceType	= new Stack<BoxSourceType>();

	/**
	 * Constructor
	 */
	public PrettyPrintBoxVisitor() {
		// Default to script
		currentSourceType.push( BoxSourceType.BOXSCRIPT );
	}

	private boolean isTemplate() {
		return currentSourceType.peek().equals( BoxSourceType.BOXTEMPLATE );
	}

	private void trimTrailingSpaceFromBuffer() {
		// If text after the last lineBreak contains nothing but whitespace, remove it the whitespace, leaving the last new line as the last char in the
		// buffer
		// Note, there may be more than one indent character after the last line break
		int lastLineBreak = buffer.lastIndexOf( lineBreak );
		if ( lastLineBreak != -1 ) {
			int lastChar = buffer.length() - 1;
			for ( int i = lastChar; i > lastLineBreak; i-- ) {
				if ( buffer.charAt( i ) != ' ' && buffer.charAt( i ) != '\t' ) {
					break;
				}
				buffer.deleteCharAt( i );
			}
		}
	}

	private void newLine() {
		trimTrailingSpaceFromBuffer();
		buffer.append( lineBreak );
		printIndent();
	}

	private void newLineIfNeeded() {
		if ( buffer.isEmpty() ) {
			return;
		}
		if ( isTemplate() ) {
			return;
		}

		trimTrailingSpaceFromBuffer();
		// only append line break if the end of the buffer doesn't already contain one
		if ( buffer.length() == 0 || !buffer.substring( buffer.length() - lineBreak.length() ).equals( lineBreak ) ) {
			buffer.append( lineBreak );
		}
		printIndent();
	}

	private void print( String s ) {
		buffer.append( s );
	}

	private void println( String s ) {
		buffer.append( s );
		newLine();
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
		for ( int i = 0; i < numLines; i++ ) {
			boolean last = i == numLines - 1;
			if ( !first && !last ) {
				print( " * " );
			} else if ( numLines > 1 && last ) {
				print( " " );
			}
			if ( last ) {
				print( lines[ i ] );
			} else {
				println( lines[ i ] );
			}
			first = false;
		}
	}

	private void printPreOnlyComments( BoxNode node ) {
		boolean printed = false;
		for ( var comment : node.getComments() ) {
			if ( comment.isBefore( node ) ) {
				if ( !comment.startsOnEndLineOf( lastNodeToPrint ) ) {
					newLineIfNeeded();
				}
				comment.accept( this );
				printed			= true;
				lastNodeToPrint	= comment;
			}
		}
		if ( printed && !node.startsOnEndLineOf( lastNodeToPrint ) ) {
			newLineIfNeeded();
		}
	}

	/**
	 * Prints pre and inside comments
	 * 
	 * @param node
	 */
	private void printPreComments( BoxNode node ) {
		boolean printed = false;
		for ( var comment : node.getComments() ) {
			if ( !comment.isAfter( node ) ) {
				if ( !comment.startsOnEndLineOf( lastNodeToPrint ) ) {
					newLineIfNeeded();
				}
				comment.accept( this );
				printed			= true;
				lastNodeToPrint	= comment;
			}
		}
		if ( printed && !node.startsOnEndLineOf( lastNodeToPrint ) ) {
			newLineIfNeeded();
		}
	}

	private void printInsideComments( BoxNode node ) {
		for ( var comment : node.getComments() ) {
			if ( comment.isInside( node ) ) {
				comment.accept( this );
				lastNodeToPrint = comment;
				newLine();
			}
		}
	}

	private void printPostComments( BoxNode node ) {
		lastNodeToPrint = node;
		for ( var comment : node.getComments() ) {
			if ( comment.isAfter( node ) ) {
				// Separarte something like
				// ... } // end comment
				print( " " );
				comment.accept( this );
				lastNodeToPrint = comment;
			}
		}
	}

	public String getOutput() {
		return buffer.toString();
	}

	private void increaseIndent() {
		indentLevel++;
	}

	private void decreaseIndent() {
		indentLevel--;
		if ( !isTemplate() ) {
			// if buffer ends with a newline and indent characters, remove of them
			if ( buffer.length() >= indent.length() && buffer.substring( buffer.length() - indent.length() ).equals( indent ) ) {
				buffer.delete( buffer.length() - indent.length(), buffer.length() );
			}
		}
	}

	private void printIndent() {
		for ( int i = 0; i < indentLevel; i++ ) {
			buffer.append( indent );
		}
	}

	public void visit( BoxScript node ) {
		printPreComments( node );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLineIfNeeded();
		}
		printPostComments( node );
	}

	public void visit( BoxBufferOutput node ) {
		printPreComments( node );
		printPreComments( node.getExpression() );
		// This node SHOULD only exist in templates
		if ( isTemplate() ) {
			if ( node.getExpression() instanceof BoxStringLiteral sLit ) {
				String value = sLit.getValue();
				// If we're in an output component, we need to escape pound signs
				if ( node.getFirstAncestorOfType( BoxComponent.class, comp -> comp.getName().equalsIgnoreCase( "output" ) ) != null ) {
					value = value.replace( "#", "##" );
				}
				print( value );
			} else if ( node.getExpression() instanceof BoxStringInterpolation sInt ) {
				processStringInterp( sInt, false );
			} else {
				throw new BoxRuntimeException( "Unexpected expression in buffer output: " + node.getExpression().getClass().getName() );
			}
		} else {
			print( "echo( \"" );
			doQuotedExpression( node.getExpression() );
			print( "\" )" );
		}
		printPostComments( node.getExpression() );
		printPostComments( node );
	}

	/**
	 * BoxLang Classes will always be in script. No tags!
	 */
	public void visit( BoxClass node ) {
		for ( var importNode : node.getImports() ) {
			importNode.accept( this );
			newLine();
		}
		printPreOnlyComments( node );
		// TODO: need to separate pre and inline annotations in AST
		for ( var anno : node.getAnnotations() ) {
			anno.accept( this );
			newLineIfNeeded();
		}
		increaseIndent();
		print( "class {" );
		newLine();
		for ( var property : node.getProperties() ) {
			property.accept( this );
			newLineIfNeeded();
		}
		newLine();
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLineIfNeeded();
		}
		printInsideComments( node );
		decreaseIndent();
		print( "}" );
		printPostComments( node );
	}

	public void visit( BoxStaticInitializer node ) {
		if ( !isTemplate() ) {
			printPreOnlyComments( node );
			increaseIndent();
			print( "static {" );
			newLine();
			for ( var statement : node.getBody() ) {
				statement.accept( this );
				newLineIfNeeded();
			}
			printInsideComments( node );
			decreaseIndent();
			print( "}" );
			printPostComments( node );
			newLine();
		}
	}

	public void visit( BoxInterface node ) {
		for ( var importNode : node.getImports() ) {
			importNode.accept( this );
			newLine();
			newLineIfNeeded();
		}
		printPreOnlyComments( node );
		for ( var anno : node.getAnnotations() ) {
			anno.accept( this );
			newLine();
		}
		increaseIndent();
		print( "interface" );
		// enter template mode so our inline annotatins are in the form name="value"
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		for ( var anno : node.getPostAnnotations() ) {
			anno.accept( this );
			newLine();
		}
		currentSourceType.pop();
		print( " {" );
		newLine();
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLineIfNeeded();
		}
		printInsideComments( node );
		decreaseIndent();
		print( "}" );
		printPostComments( node );
	}

	public void visit( BoxSingleLineComment node ) {
		if ( isTemplate() ) {
			print( "<!--- " );
			print( node.getCommentText() );
			print( " --->" );
		} else {
			print( "// " );
			println( node.getCommentText() );
		}
	}

	public void visit( BoxMultiLineComment node ) {
		if ( isTemplate() ) {
			print( "<!--- " );
			print( node.getCommentText() );
			print( " --->" );
		} else {
			print( "/*" );
			if ( !node.getCommentText().startsWith( "*" ) && !node.getCommentText().startsWith( "\n" ) ) {
				print( " " );
			}
			printMultiLine( node.getCommentText() );
			if ( !node.getCommentText().endsWith( "*" ) && !node.getCommentText().endsWith( "\n" ) ) {
				print( " " );
			}
			print( "*/" );
		}
	}

	public void visit( BoxDocComment node ) {
		if ( isTemplate() ) {
			print( "<!--- " );
			print( node.getCommentText() );
			print( " --->" );
		} else {
			print( "/**" );
			if ( !node.getCommentText().startsWith( "*" ) && !node.getCommentText().startsWith( "\n" ) ) {
				print( " " );
			}
			printMultiLine( node.getCommentText() );
			if ( !node.getCommentText().endsWith( "*" ) && !node.getCommentText().endsWith( "\n" ) ) {
				print( " " );
			}
			print( "*/" );
		}
	}

	public void visit( BoxScriptIsland node ) {
		printPreComments( node );
		boolean isTemplate = isTemplate();
		currentSourceType.push( BoxSourceType.BOXSCRIPT );
		if ( isTemplate ) {
			increaseIndent();
			println( "<bx:script>" );
		}
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLineIfNeeded();
		}
		if ( isTemplate ) {
			decreaseIndent();
			println( "</bx:script>" );
		}
		currentSourceType.pop();
		printPostComments( node );
	}

	public void visit( BoxTemplate node ) {
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		printPreOnlyComments( node );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
		}
		printInsideComments( node );
		printPostComments( node );
		currentSourceType.pop();
	}

	public void visit( BoxTemplateIsland node ) {
		printPreComments( node );
		println( "```" );
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
		}
		currentSourceType.pop();
		println( "```" );
		printPostComments( node );
	}

	public void visit( BoxArgument node ) {
		printPreComments( node );
		if ( node.getName() == null ) {
			node.getValue().accept( this );
		} else {
			if ( node.getName() instanceof BoxStringLiteral str ) {
				print( str.getValue() );
			} else {
				node.getName().accept( this );
			}
			print( "=" );
			node.getValue().accept( this );
		}
		printPostComments( node );
	}

	public void visit( BoxArrayAccess node ) {
		printPreComments( node );
		node.getContext().accept( this );
		print( "[ " );
		node.getAccess().accept( this );
		print( " ]" );
		printPostComments( node );
	}

	public void visit( BoxArrayLiteral node ) {
		printPreComments( node );
		increaseIndent();
		int size = node.getValues().size();
		if ( size > 0 )
			println( "[ " );
		else
			print( "[" );
		for ( int i = 0; i < size; i++ ) {
			node.getValues().get( i ).accept( this );
			if ( i < size - 1 ) {
				println( ", " );
			} else {
				newLine();
			}
		}
		decreaseIndent();
		print( "]" );
		printPostComments( node );
	}

	public void visit( BoxAssignment node ) {
		printPreComments( node );
		if ( node.getModifiers().contains( BoxAssignmentModifier.VAR ) ) {
			print( "var " );
		}
		node.getLeft().accept( this );
		print( " = " );
		node.getRight().accept( this );
		printPostComments( node );
	}

	public void visit( BoxBinaryOperation node ) {
		printPreComments( node );
		node.getLeft().accept( this );
		print( " " );
		print( node.getOperator().getSymbol() );
		print( " " );
		node.getRight().accept( this );
		printPostComments( node );
	}

	public void visit( BoxBooleanLiteral node ) {
		printPreComments( node );
		print( node.getValue() ? "true" : "false" );
		printPostComments( node );
	}

	public void visit( BoxClosure node ) {
		printPreComments( node );
		// TODO: Make AST "remember" difference between original function(){} and ()=>{}
		boolean hasArgs = !node.getArgs().isEmpty();
		print( "(" );
		if ( hasArgs )
			print( " " );
		int size = node.getArgs().size();
		for ( int i = 0; i < size; i++ ) {
			node.getArgs().get( i ).accept( this );
			if ( i < size - 1 ) {
				print( ", " );
			}
		}
		if ( hasArgs )
			print( " " );
		print( ") => " );
		node.getBody().accept( this );
		printPostComments( node );
	}

	public void visit( BoxComparisonOperation node ) {
		printPreComments( node );
		node.getLeft().accept( this );
		print( " " );
		print( node.getOperator().getSymbol() );
		print( " " );
		node.getRight().accept( this );
		printPostComments( node );
	}

	public void visit( BoxDecimalLiteral node ) {
		printPreComments( node );
		print( node.getValue() );
		printPostComments( node );
	}

	public void visit( BoxDotAccess node ) {
		printPreComments( node );
		node.getContext().accept( this );
		if ( node.isSafe() ) {
			print( "?" );
		}
		print( "." );
		node.getAccess().accept( this );
		printPostComments( node );
	}

	public void visit( BoxExpressionInvocation node ) {
		printPreComments( node );
		node.getExpr().accept( this );
		print( "(" );
		int size = node.getArguments().size();
		for ( int i = 0; i < size; i++ ) {
			node.getArguments().get( i ).accept( this );
			if ( i < size - 1 ) {
				print( ", " );
			}
		}
		print( ")" );
		printPostComments( node );
	}

	public void visit( BoxFQN node ) {
		printPreComments( node );
		print( node.getValue() );
		printPostComments( node );
	}

	public void visit( BoxFunctionInvocation node ) {
		printPreOnlyComments( node );
		print( node.getName() );
		boolean hasArgs = !node.getArguments().isEmpty();
		print( "(" );
		if ( hasArgs )
			print( " " );
		int size = node.getArguments().size();
		for ( int i = 0; i < size; i++ ) {
			node.getArguments().get( i ).accept( this );
			if ( i < size - 1 ) {
				print( ", " );
			}
		}
		if ( hasArgs )
			print( " " );
		printInsideComments( node );
		print( ")" );
		printPostComments( node );
	}

	public void visit( BoxIdentifier node ) {
		printPreComments( node );
		print( node.getName() );
		printPostComments( node );
	}

	public void visit( BoxIntegerLiteral node ) {
		printPreComments( node );
		print( node.getValue() );
		printPostComments( node );
	}

	public void visit( BoxLambda node ) {
		printPreComments( node );
		boolean hasArgs = !node.getArgs().isEmpty();
		print( "(" );
		if ( hasArgs )
			print( " " );
		int size = node.getArgs().size();
		for ( int i = 0; i < size; i++ ) {
			node.getArgs().get( i ).accept( this );
			if ( i < size - 1 ) {
				print( ", " );
			}
		}
		if ( hasArgs )
			print( " " );
		print( ") -> " );
		node.getBody().accept( this );
		printPostComments( node );
	}

	public void visit( BoxMethodInvocation node ) {
		printPreComments( node );
		node.getObj().accept( this );
		if ( node.isSafe() ) {
			print( "?" );
		}
		print( "." );
		node.getName().accept( this );
		boolean hasArgs = !node.getArguments().isEmpty();
		print( "(" );
		if ( hasArgs )
			print( " " );
		int size = node.getArguments().size();
		for ( int i = 0; i < size; i++ ) {
			node.getArguments().get( i ).accept( this );
			if ( i < size - 1 ) {
				print( ", " );
			}
		}
		if ( hasArgs )
			print( " " );
		print( ")" );
		printPostComments( node );
	}

	public void visit( BoxNegateOperation node ) {
		printPreComments( node );
		print( "not " );
		node.getExpr().accept( this );
		printPostComments( node );
	}

	public void visit( BoxNew node ) {
		printPreComments( node );
		print( "new " );
		if ( node.getPrefix() != null ) {
			node.getPrefix().accept( this );
			print( ":" );
		}
		node.getExpression().accept( this );
		print( "(" );
		int size = node.getArguments().size();
		if ( size > 0 ) {
			print( " " );
		}
		for ( int i = 0; i < size; i++ ) {
			node.getArguments().get( i ).accept( this );
			if ( i < size - 1 ) {
				print( ", " );
			}
		}
		if ( size > 0 ) {
			print( " " );
		}
		print( ")" );
		printPostComments( node );
	}

	public void visit( BoxNull node ) {
		printPreComments( node );
		print( "null" );
		printPostComments( node );
	}

	public void visit( BoxParenthesis node ) {
		printPreComments( node );
		print( "(" );
		node.getExpression().accept( this );
		print( ")" );
		printPostComments( node );
	}

	public void visit( BoxScope node ) {
		printPreComments( node );
		print( node.getName() );
		printPostComments( node );
	}

	public void visit( BoxStringConcat node ) {
		printPreComments( node );
		// TODO: Need to track more about original source
		int size = node.getValues().size();
		for ( int i = 0; i < size; i++ ) {
			var expr = node.getValues().get( i );
			expr.accept( this );
			if ( i < size - 1 ) {
				print( " & " );
			}
		}
		printPostComments( node );
	}

	public void visit( BoxStringInterpolation node ) {
		printPreComments( node );
		// TODO: Track which quotes were used
		print( "\"" );
		processStringInterp( node, true );
		print( "\"" );
		printPostComments( node );
	}

	/**
	 * I process string interpolation, but without assuming it was a quoted string
	 * 
	 * @param node The BoxStringInterpolation node
	 */
	public void processStringInterp( BoxStringInterpolation node, boolean isQuoted ) {
		for ( var expr : node.getValues() ) {
			if ( expr instanceof BoxStringLiteral str ) {
				if ( isQuoted ) {
					print( str.getValue().replace( "\"", "\"\"" ).replace( "#", "##" ) );
				} else {
					String value = str.getValue();
					// If we're in an output component, we need to escape pound signs
					if ( node.getFirstAncestorOfType( BoxComponent.class, comp -> comp.getName().equalsIgnoreCase( "output" ) ) != null ) {
						value = value.replace( "#", "##" );
					}
					print( value );
				}
			} else {
				print( "#" );
				expr.accept( this );
				print( "#" );
			}
		}
	}

	public void visit( BoxStringLiteral node ) {
		printPreComments( node );
		print( "\"" );
		print( node.getValue().replace( "\"", "\"\"" ) );
		print( "\"" );
		printPostComments( node );
	}

	public void visit( BoxStructLiteral node ) {
		printPreComments( node );
		increaseIndent();
		int size = node.getValues().size();
		if ( node.getType().equals( BoxStructType.Ordered ) ) {
			if ( size > 0 )
				println( "[ " );
			else
				print( "[" );
		} else {
			if ( size > 0 )
				println( "{ " );
			else
				print( "{" );
		}
		// Every other value is key/value
		for ( int i = 0; i < size; i = i + 2 ) {
			var key = node.getValues().get( i );
			key.accept( this );
			print( " : " );
			var value = node.getValues().get( i + 1 );
			value.accept( this );
			if ( i < size - 2 ) {
				println( ", " );
			} else {
				newLine();
			}
		}
		decreaseIndent();
		if ( node.getType().equals( BoxStructType.Ordered ) ) {
			print( "]" );
		} else {
			print( "}" );
		}
		printPostComments( node );
	}

	public void visit( BoxTernaryOperation node ) {
		printPreComments( node );
		node.getCondition().accept( this );
		print( " ? " );
		node.getWhenTrue().accept( this );
		print( " : " );
		node.getWhenFalse().accept( this );
		printPostComments( node );
	}

	public void visit( BoxUnaryOperation node ) {
		printPreComments( node );
		String symbol = node.getOperator().getSymbol();
		if ( node.getOperator().isPre() ) {
			print( symbol );
			node.getExpr().accept( this );
		} else {
			node.getExpr().accept( this );
			print( symbol );
		}
		printPostComments( node );
	}

	public void visit( BoxAnnotation node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			// inline in a tag like <bx:function name="foo" annotation="value" >
			print( " " );
			node.getKey().accept( this );
			if ( node.getValue() != null ) {
				print( "=\"" );
				doQuotedExpression( node.getValue() );
				print( "\"" );
			}
		} else {
			// In script is above the construct like @annotation value
			print( "@" );
			node.getKey().accept( this );
			if ( node.getValue() != null ) {
				print( " " );
				node.getValue().accept( this );
			}
		}
		printPostComments( node );
	}

	public void visit( BoxArgumentDeclaration node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:argument" );
			for ( var annotation : node.getAnnotations() ) {
				annotation.accept( this );
			}
			print( ">" );
		} else {
			// TODO: script annotations need hoisted up to function declaration
			if ( node.getRequired() != null && node.getRequired() ) {
				print( "required " );
			}
			if ( node.getType() != null ) {
				print( node.getType() );
				print( " " );
			}
			print( node.getName() );
			if ( node.getValue() != null ) {
				print( " = " );
				node.getValue().accept( this );
			}
		}
		printPostComments( node );
	}

	private void doQuotedExpression( BoxExpression node ) {
		if ( node instanceof BoxStringLiteral str ) {
			print( str.getValue().replace( "\"", "\"\"" ) );
		} else if ( node instanceof BoxStringInterpolation interp ) {
			processStringInterp( interp, true );
		} else if ( node instanceof BoxFQN fqn ) {
			print( fqn.getValue() );
		} else {
			print( "#" );
			node.accept( this );
			print( "#" );
		}
	}

	public void visit( BoxAssert node ) {
		printPreComments( node );
		print( "assert " );
		node.getExpression().accept( this );
		print( ";" );
		printPostComments( node );
	}

	public void visit( BoxBreak node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:break" );
			if ( node.getLabel() != null ) {
				print( " label=\"" );
				print( node.getLabel() );
				print( "\"" );
			}
			print( ">" );
		} else {
			print( "break" );
			if ( node.getLabel() != null ) {
				print( " " );
				print( node.getLabel() );
			}
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxContinue node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:continue" );
			if ( node.getLabel() != null ) {
				print( " label=\"" );
				print( node.getLabel() );
				print( "\"" );
			}
			print( ">" );
		} else {
			print( "continue" );
			if ( node.getLabel() != null ) {
				print( " " );
				print( node.getLabel() );
			}
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxDo node ) {
		printPreComments( node );
		// No template version of this
		if ( node.getLabel() != null ) {
			print( node.getLabel() );
			print( ": " );
		}
		print( "do " );
		node.getBody().accept( this );
		newLine();
		print( " while (" );
		node.getCondition().accept( this );
		print( ");" );
		printPostComments( node );
	}

	public void visit( BoxDocumentationAnnotation node ) {
		printPreComments( node );
		node.getKey().accept( this );
		if ( node.getValue() != null ) {
			print( " " );
			node.getValue().accept( this );
		}
		printPostComments( node );
	}

	public void visit( BoxExpressionStatement node ) {
		printPreComments( node );
		// TODO: Does boxlang want to introduce a separate tag for ad-hoc expressions outside of "set"?
		if ( isTemplate() ) {
			print( "<bx:set " );
			node.getExpression().accept( this );
			print( " >" );
		} else {
			node.getExpression().accept( this );
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxForIn node ) {
		printPreComments( node );
		if ( node.getLabel() != null ) {
			print( node.getLabel() );
			print( ": " );
		}
		print( "for( " );
		if ( node.getHasVar() ) {
			print( "var " );
		}
		node.getVariable().accept( this );
		print( " in " );
		node.getExpression().accept( this );
		print( " ) " );
		node.getBody().accept( this );
		newLine();
		printPostComments( node );
	}

	public void visit( BoxForIndex node ) {
		printPreComments( node );
		if ( node.getLabel() != null ) {
			print( node.getLabel() );
			print( ": " );
		}
		print( "for( " );
		if ( node.getInitializer() != null ) {
			node.getInitializer().accept( this );
		} else {
			print( " " );
		}
		print( "; " );
		if ( node.getCondition() != null ) {
			node.getCondition().accept( this );
		} else {
			print( " " );
		}
		print( "; " );
		if ( node.getStep() != null ) {
			node.getStep().accept( this );
		} else {
			print( " " );
		}
		print( " ) " );
		node.getBody().accept( this );
		newLine();
		printPostComments( node );
	}

	public void visit( BoxFunctionDeclaration node ) {
		newLine();
		printPreComments( node );
		Boolean defaultInterfaceMethod = node.getFirstNodeOfType( BoxInterface.class ) != null;
		if ( isTemplate() ) {
			print( "<bx:function" );
			for ( var annotation : node.getAnnotations() ) {
				annotation.accept( this );
			}
			print( ">" );
			increaseIndent();
			newLine();
			// These's args are indented based on the indent level, which may or may not actually
			// match the whitespace in the template. For template code, we don't really use the indent counter.
			// We most just let the buffer outpout nodes provide all indentation from the original source.
			for ( var args : node.getArgs() ) {
				args.accept( this );
				newLine();
			}
			newLine();
			if ( node.getBody() != null ) {
				for ( var statement : node.getBody() ) {
					statement.accept( this );
				}
			}
			decreaseIndent();
			print( "</bx:function>" );
		} else {
			if ( defaultInterfaceMethod ) {
				print( "default " );
			}
			if ( node.getAccessModifier() != null ) {
				print( node.getAccessModifier().toString().toLowerCase() );
				print( " " );
			}
			if ( node.getType() != null ) {
				node.getType().accept( this );
				print( " " );
			}
			print( "function " );
			print( node.getName() );
			boolean hasArgs = !node.getArgs().isEmpty();
			print( "(" );
			if ( hasArgs )
				print( " " );
			int size = node.getArgs().size();
			for ( int i = 0; i < size; i++ ) {
				node.getArgs().get( i ).accept( this );
				if ( i < size - 1 ) {
					print( ", " );
				}
			}
			if ( hasArgs )
				print( " " );

			print( ")" );
			if ( node.getBody() != null ) {
				increaseIndent();
				println( " {" );
				for ( var statement : node.getBody() ) {
					statement.accept( this );
					newLineIfNeeded();
				}
				decreaseIndent();
				println( "}" );
			} else {
				println( ";" );
			}
		}
		printPostComments( node );
	}

	public void visit( BoxIfElse node ) {
		printPreComments( node );
		doBoxIfElse( node, false );
		printPostComments( node );
	}

	private void doBoxIfElse( BoxIfElse node, boolean elseif ) {
		if ( isTemplate() ) {
			if ( elseif ) {
				print( "if " );
			} else {
				print( "<bx:if " );
			}
			node.getCondition().accept( this );
			print( " >" );
			increaseIndent();
			node.getThenBody().accept( this );
			decreaseIndent();
			if ( node.getElseBody() != null ) {
				if ( node.getElseBody() instanceof BoxIfElse elseNode ) {
					print( "<bx:else" );
					doBoxIfElse( elseNode, true );
				} else {
					print( "<bx:else>" );
					increaseIndent();
					node.getElseBody().accept( this );
					decreaseIndent();
					print( "</bx:if>" );
				}
			} else {
				print( "</bx:if>" );
			}
		} else {
			print( "if( " );
			node.getCondition().accept( this );
			print( " ) " );
			node.getThenBody().accept( this );
			newLine();
			if ( node.getElseBody() != null ) {
				print( " else " );
				node.getElseBody().accept( this );
				newLine();
			}
		}
	}

	public void visit( BoxImport node ) {
		printPreComments( node );
		// work around for unsupported taglib imports
		if ( node.getExpression() == null ) {
			return;
		}
		if ( isTemplate() ) {
			// TODO: See about just changing the type of this
			BoxFQN	fqn			= ( BoxFQN ) node.getExpression();
			String	prefix		= null;
			String	className	= null;
			if ( fqn.getValue().contains( ":" ) ) {
				String[] parts = fqn.getValue().split( ":" );
				prefix		= parts[ 0 ];
				className	= parts[ 1 ];
			} else {
				className = fqn.getValue();
			}
			print( "<bx:import" );
			if ( prefix != null ) {
				print( " prefix=\"" );
				print( prefix );
				print( "\"" );
			}

			print( " name=\"" );
			print( className );
			print( "\"" );

			if ( node.getAlias() != null ) {
				print( " alias=\"" );
				node.getAlias().accept( this );
				print( "\"" );
			}
			print( ">" );
		} else {
			print( "import " );
			node.getExpression().accept( this );
			if ( node.getAlias() != null ) {
				print( " as " );
				node.getAlias().accept( this );
			}
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxParam node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:param" );
			if ( node.getType() != null ) {
				print( " type=\"" );
				node.getType().accept( this );
				print( "\"" );
			}
			print( " name=\"" );
			node.getVariable().accept( this );
			print( "\"" );
			if ( node.getDefaultValue() != null ) {
				print( " default=\"" );
				doQuotedExpression( node.getDefaultValue() );
				print( "\"" );
			}
			print( ">" );
		} else {
			print( "param " );
			if ( node.getType() != null ) {
				node.getType().accept( this );
				print( " " );
			}
			node.getVariable().accept( this );
			if ( node.getDefaultValue() != null ) {
				print( " = " );
				node.getDefaultValue().accept( this );
			}
			println( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxProperty node ) {
		newLine();
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:property" );
			for ( var anno : node.getAnnotations() ) {
				anno.accept( this );
			}
			print( ">" );
		} else {
			for ( var anno : node.getAnnotations() ) {
				anno.accept( this );
				newLine();
			}
			print( "property" );
			// TODO: Handle these accounting for shorcut syntax
			// also need to seperate pre and inline annotations
			for ( var anno : node.getPostAnnotations() ) {
				print( " " );
				anno.getKey().accept( this );
				if ( anno.getValue() != null ) {
					print( "=" );
					anno.getValue().accept( this );
				}
			}
			println( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxRethrow node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:rethrow>" );
		} else {
			print( "rethrow;" );
		}
		printPostComments( node );
	}

	public void visit( BoxReturn node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:return" );
			if ( node.getExpression() != null ) {
				print( " " );
				node.getExpression().accept( this );
			}
			print( ">" );
		} else {
			print( "return" );
			if ( node.getExpression() != null ) {
				print( " " );
				node.getExpression().accept( this );
			}
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxReturnType node ) {
		printPreComments( node );
		if ( node.getType().equals( BoxType.Fqn ) ) {
			print( node.getFqn() );
		} else {
			print( node.getType().toString().toLowerCase() );
		}
		printPostComments( node );
	}

	public void visit( BoxSwitch node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:switch expression=\"" );
			doQuotedExpression( node.getCondition() );
			print( "\">" );
			increaseIndent();
			for ( var caseNode : node.getCases() ) {
				caseNode.accept( this );
				newLine();
			}
			decreaseIndent();
			print( "</bx:switch>" );
		} else {
			print( "switch ( " );
			node.getCondition().accept( this );
			increaseIndent();
			println( " ) {" );
			for ( var caseNode : node.getCases() ) {
				caseNode.accept( this );
			}
			decreaseIndent();
			print( "}" );
		}
		printPostComments( node );
	}

	public void visit( BoxSwitchCase node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			if ( node.getCondition() != null ) {
				print( "<bx:case value=\"" );
				doQuotedExpression( node.getCondition() );
				print( "\">" );
			} else {
				print( "<bx:defaultcase>" );
			}
			increaseIndent();
			for ( var statement : node.getBody() ) {
				statement.accept( this );
			}
			decreaseIndent();
			print( "</bx:case>" );
		} else {
			if ( node.getCondition() == null ) {
				print( "default:" );
			} else {
				print( "case " );
				node.getCondition().accept( this );
				print( ":" );
			}
			if ( node.getBody().size() == 1 && node.getBody().get( 0 ) instanceof BoxStatementBlock ) {
				print( " " );
				node.getBody().get( 0 ).accept( this );
				newLine();
			} else {
				increaseIndent();
				newLine();
				for ( var statement : node.getBody() ) {
					statement.accept( this );
					newLineIfNeeded();
				}
				decreaseIndent();
			}
		}
		printPostComments( node );
	}

	public void visit( BoxThrow node ) {
		printPreComments( node );
		print( "throw" );
		if ( node.getExpression() != null ) {
			print( " " );
			node.getExpression().accept( this );
		}
		print( ";" );
		printPostComments( node );
	}

	public void visit( BoxTry node ) {
		if ( isTemplate() ) {
			printPreComments( node );
			print( "<bx:try>" );
			increaseIndent();
			for ( var statement : node.getTryBody() ) {
				statement.accept( this );
			}
			for ( var catchNode : node.getCatches() ) {
				catchNode.accept( this );
			}
			if ( node.getFinallyBody() != null && !node.getFinallyBody().isEmpty() ) {
				print( "<bx:finally>" );
				increaseIndent();
				for ( var statement : node.getFinallyBody() ) {
					statement.accept( this );
				}
				decreaseIndent();
				print( "</bx:finally>" );
			}
			decreaseIndent();
			print( "</bx:try>" );
		} else {
			printPreOnlyComments( node );
			increaseIndent();
			println( "try {" );
			for ( var statement : node.getTryBody() ) {
				statement.accept( this );
				newLineIfNeeded();
			}
			printInsideComments( node );
			decreaseIndent();
			print( "}" );
			if ( !node.getCatches().isEmpty() ) {
				for ( var catchNode : node.getCatches() ) {
					catchNode.accept( this );
				}
			}
			if ( node.getFinallyBody() != null && !node.getFinallyBody().isEmpty() ) {
				increaseIndent();
				print( "finally {" );
				newLine();
				for ( var statement : node.getFinallyBody() ) {
					statement.accept( this );
					newLineIfNeeded();
				}
				decreaseIndent();
				print( "}" );
			}
		}
		printPostComments( node );
	}

	public void visit( BoxTryCatch node ) {
		if ( isTemplate() ) {
			printPreComments( node );
			print( "<bx:catch" );
			if ( !node.getCatchTypes().isEmpty() ) {
				print( " type=\"" );
				// should only be one when in tags
				doQuotedExpression( node.getCatchTypes().get( 0 ) );
				print( "\"" );
			}
			print( "\">" );
			increaseIndent();
			for ( var statement : node.getCatchBody() ) {
				statement.accept( this );
			}
			decreaseIndent();
			print( "</bx:catch>" );
		} else {
			printPreOnlyComments( node );
			print( " catch (" );
			int numCatchTypes = node.getCatchTypes().size();
			for ( int i = 0; i < numCatchTypes; i++ ) {
				var type = node.getCatchTypes().get( i );
				type.accept( this );
				if ( i < numCatchTypes - 1 ) {
					print( " | " );
				}
			}
			print( " " );
			node.getException().accept( this );
			increaseIndent();
			print( ") {" );
			newLine();
			for ( var statement : node.getCatchBody() ) {
				statement.accept( this );
				newLineIfNeeded();
			}
			printInsideComments( node );
			decreaseIndent();
			print( "}" );
		}
		printPostComments( node );
	}

	public void visit( BoxWhile node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:while condition=\"" );
			doQuotedExpression( node.getCondition() );
			print( "\"" );
			if ( node.getLabel() != null ) {
				print( " label=\"" );
				print( node.getLabel() );
				print( "\"" );
			}
			print( ">" );
			increaseIndent();
			node.getBody().accept( this );
			decreaseIndent();
			print( "</bx:while>" );
		} else {
			if ( node.getLabel() != null ) {
				print( node.getLabel() );
				print( ": " );
			}
			print( "while (" );
			node.getCondition().accept( this );
			print( ") " );
			node.getBody().accept( this );
			newLine();
		}
		printPostComments( node );
	}

	public void visit( BoxComponent node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<bx:" );
			print( node.getName() );
			for ( var attr : node.getAttributes() ) {
				print( " " );
				attr.getKey().accept( this );
				print( "=\"" );
				doQuotedExpression( attr.getValue() );
				print( "\"" );
			}
			if ( node.getBody() != null ) {
				if ( node.getBody().isEmpty() ) {
					// existing, but empty body gives us <bx:componentName />
					// This is important for custom tags that expect to execute twice-- start and end
					print( "/>" );
				} else {
					// existing body with statements gives us <bx:componentName> statements... </bx:componentName>
					print( ">" );
					increaseIndent();
					for ( var statement : node.getBody() ) {
						statement.accept( this );
					}
					decreaseIndent();
					print( "</bx:" );
					print( node.getName() );
					print( ">" );
				}
			} else {
				// not existing body gives us <bx:componentName>
				print( ">" );
			}
		} else {
			print( node.getName() );
			for ( var attr : node.getAttributes() ) {
				print( " " );
				attr.getKey().accept( this );
				print( "=" );
				attr.getValue().accept( this );
			}
			if ( node.getBody() != null && !node.getBody().isEmpty() ) {
				increaseIndent();
				print( " {" );
				newLine();
				for ( var statement : node.getBody() ) {
					statement.accept( this );
					newLineIfNeeded();
				}
				decreaseIndent();
				print( "}" );
			} else {
				print( ";" );
			}
		}
		printPostComments( node );
	}

	public void visit( BoxStatementBlock node ) {
		printPreOnlyComments( node );
		if ( isTemplate() ) {
			for ( var statement : node.getBody() ) {
				statement.accept( this );
			}
		} else {
			increaseIndent();
			print( "{" );
			newLine();
			for ( var statement : node.getBody() ) {
				statement.accept( this );
				newLineIfNeeded();
			}
			printInsideComments( node );
			decreaseIndent();
			print( "}" );
		}
		printPostComments( node );
	}

}
