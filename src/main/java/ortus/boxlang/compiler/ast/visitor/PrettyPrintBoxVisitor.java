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
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
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
 * - Implement tag and script output for relevant nodes
 * - Track whether we're currenty in tag or script in a stack
 * - Add configuration for indent size
 * - Add any other config settings such as white space inside paren, etc
 * - Modify AST to track comments (this change goes in the parser)
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

	/**
	 * Track how deeply we're indented
	 */
	private int						indentLevel			= 0;

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

	private void newLine() {
		buffer.append( "\n" );
		printIndent();
	}

	private void print( String s ) {
		buffer.append( s );
	}

	private void println( String s ) {
		buffer.append( s );
		newLine();
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
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLine();
		}
	}

	public void visit( BoxBufferOutput node ) {
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
	}

	/**
	 * BoxLang Classes will always be in script. No tags!
	 */
	public void visit( BoxClass node ) {
		for ( var importNode : node.getImports() ) {
			importNode.accept( this );
			newLine();
		}
		// TODO: Replace this with actual comment AST Nodes
		if ( !node.getDocumentation().isEmpty() ) {
			println( "/**" );
			for ( var doc : node.getDocumentation() ) {
				print( "  * " );
				doc.accept( this );
				newLine();
			}
			println( "*/" );
		}
		// TODO: need to separate pre and inline annotations in AST
		for ( var importNode : node.getAnnotations() ) {
			importNode.accept( this );
			newLine();
		}
		increaseIndent();
		print( "class {" );
		newLine();
		for ( var property : node.getProperties() ) {
			property.accept( this );
			newLine();
		}
		newLine();
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxInterface node ) {
		for ( var importNode : node.getImports() ) {
			importNode.accept( this );
			newLine();
		}
		// TODO: Replace this with actual comment AST Nodes
		if ( !node.getDocumentation().isEmpty() ) {
			println( "/**" );
			for ( var doc : node.getDocumentation() ) {
				print( "  * " );
				doc.accept( this );
				newLine();
			}
			println( "*/" );
		}
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
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxDocComment node ) {
		// TODO: Not sure this node is in use yet
		println( "documentation??" );
	}

	public void visit( BoxScriptIsland node ) {
		boolean isTemplate = isTemplate();
		currentSourceType.push( BoxSourceType.BOXSCRIPT );
		if ( isTemplate ) {
			increaseIndent();
			println( "<bx:script>" );
		}
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLine();
		}
		if ( isTemplate ) {
			decreaseIndent();
			println( "</bx:script>" );
		}
		currentSourceType.pop();
	}

	public void visit( BoxTemplate node ) {
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
		}
		currentSourceType.pop();
	}

	public void visit( BoxTemplateIsland node ) {
		println( "```" );
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
		}
		currentSourceType.pop();
		println( "```" );
	}

	public void visit( BoxArgument node ) {
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
	}

	public void visit( BoxArrayAccess node ) {
		node.getContext().accept( this );
		print( "[ " );
		node.getAccess().accept( this );
		print( " ]" );
	}

	public void visit( BoxArrayLiteral node ) {
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
	}

	public void visit( BoxAssignment node ) {
		if ( node.getModifiers().contains( BoxAssignmentModifier.VAR ) ) {
			print( "var " );
		}
		node.getLeft().accept( this );
		print( " = " );
		node.getRight().accept( this );
	}

	public void visit( BoxBinaryOperation node ) {
		node.getLeft().accept( this );
		print( " " );
		print( node.getOperator().getSymbol() );
		print( " " );
		node.getRight().accept( this );
	}

	public void visit( BoxBooleanLiteral node ) {
		print( node.getValue() ? "true" : "false" );
	}

	public void visit( BoxClosure node ) {
		// TODO: Make AST "remember" differnce between original function(){} and ()=>{}
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
		if ( node.getBody().size() == 1 && node.getBody().get( 0 ) instanceof BoxExpressionStatement expr ) {
			print( " " );
			expr.accept( this );
		} else {
			increaseIndent();
			println( "{" );
			for ( var statement : node.getBody() ) {
				statement.accept( this );
				newLine();
			}
			decreaseIndent();
			println( "}" );
		}
	}

	public void visit( BoxComparisonOperation node ) {
		node.getLeft().accept( this );
		print( " " );
		print( node.getOperator().getSymbol() );
		print( " " );
		node.getRight().accept( this );
	}

	public void visit( BoxDecimalLiteral node ) {
		print( node.getValue() );
	}

	public void visit( BoxDotAccess node ) {
		node.getContext().accept( this );
		if ( node.isSafe() ) {
			print( "?" );
		}
		print( "." );
		node.getAccess().accept( this );
	}

	public void visit( BoxExpressionInvocation node ) {
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
	}

	public void visit( BoxFQN node ) {
		print( node.getValue() );
	}

	public void visit( BoxFunctionInvocation node ) {
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
		print( ")" );
	}

	public void visit( BoxIdentifier node ) {
		print( node.getName() );
	}

	public void visit( BoxIntegerLiteral node ) {
		print( node.getValue() );
	}

	public void visit( BoxLambda node ) {
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
		if ( node.getBody().size() == 1 && node.getBody().get( 0 ) instanceof BoxExpressionStatement expr ) {
			print( " " );
			expr.accept( this );
		} else {
			increaseIndent();
			println( "{" );
			for ( var statement : node.getBody() ) {
				statement.accept( this );
				newLine();
			}
			decreaseIndent();
			println( "}" );
		}
	}

	public void visit( BoxMethodInvocation node ) {
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
	}

	public void visit( BoxNegateOperation node ) {
		print( "not " );
		node.getExpr().accept( this );
	}

	public void visit( BoxNew node ) {
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
	}

	public void visit( BoxNull node ) {
		print( "null" );
	}

	public void visit( BoxParenthesis node ) {
		print( "(" );
		node.getExpression().accept( this );
		print( ")" );
	}

	public void visit( BoxScope node ) {
		print( node.getName() );
	}

	public void visit( BoxStringConcat node ) {
		// TODO: Need to track more about original source
		int size = node.getValues().size();
		for ( int i = 0; i < size; i++ ) {
			var expr = node.getValues().get( i );
			expr.accept( this );
			if ( i < size - 1 ) {
				print( " & " );
			}
		}
	}

	public void visit( BoxStringInterpolation node ) {
		// TODO: Track which quotes were used
		print( "\"" );
		processStringInterp( node, true );
		print( "\"" );
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
		print( "\"" );
		print( node.getValue().replace( "\"", "\"\"" ) );
		print( "\"" );
	}

	public void visit( BoxStructLiteral node ) {
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
	}

	public void visit( BoxTernaryOperation node ) {
		node.getCondition().accept( this );
		print( " ? " );
		node.getWhenTrue().accept( this );
		print( " : " );
		node.getWhenFalse().accept( this );
	}

	public void visit( BoxUnaryOperation node ) {
		String symbol = node.getOperator().getSymbol();
		if ( node.getOperator().isPre() ) {
			print( symbol );
			node.getExpr().accept( this );
		} else {
			node.getExpr().accept( this );
			print( symbol );
		}
	}

	public void visit( BoxAnnotation node ) {
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
	}

	public void visit( BoxArgumentDeclaration node ) {
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
		print( "assert " );
		node.getExpression().accept( this );
		print( ";" );
	}

	public void visit( BoxBreak node ) {
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
	}

	public void visit( BoxContinue node ) {
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
	}

	public void visit( BoxDo node ) {
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
	}

	public void visit( BoxDocumentationAnnotation node ) {
		node.getKey().accept( this );
		if ( node.getValue() != null ) {
			print( " " );
			node.getValue().accept( this );
		}
	}

	public void visit( BoxExpressionStatement node ) {
		// TODO: Does boxlang want to introduce a separate tag for ad-hoc expressions outside of "set"?
		if ( isTemplate() ) {
			print( "<bx:set " );
			node.getExpression().accept( this );
			print( " >" );
		} else {
			node.getExpression().accept( this );
			print( ";" );
		}
	}

	public void visit( BoxForIn node ) {
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
	}

	public void visit( BoxForIndex node ) {
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
	}

	public void visit( BoxFunctionDeclaration node ) {
		Boolean defaultInterfaceMethod = node.getFirstNodeOfType( BoxInterface.class ) != null;
		newLine();
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
					newLine();
				}
				decreaseIndent();
				println( "}" );
			} else {
				println( ";" );
			}
		}
	}

	public void visit( BoxIfElse node ) {
		doBoxIfElse( node, false );
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
	}

	public void visit( BoxParam node ) {
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
	}

	public void visit( BoxProperty node ) {
		if ( isTemplate() ) {
			print( "<bx:property" );
			for ( var anno : node.getAnnotations() ) {
				anno.accept( this );
			}
			print( ">" );
		} else {
			if ( node.getDocumentation() != null && !node.getDocumentation().isEmpty() ) {
				println( "/**" );
				for ( var doc : node.getDocumentation() ) {
					print( "  * " );
					doc.accept( this );
					newLine();
				}
				println( "*/" );
			}
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
	}

	public void visit( BoxRethrow node ) {
		if ( isTemplate() ) {
			print( "<bx:rethrow>" );
		} else {
			print( "rethrow;" );
		}
	}

	public void visit( BoxReturn node ) {
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
	}

	public void visit( BoxReturnType node ) {
		if ( node.getType().equals( BoxType.Fqn ) ) {
			print( node.getFqn() );
		} else {
			print( node.getType().toString().toLowerCase() );
		}
	}

	public void visit( BoxSwitch node ) {
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
	}

	public void visit( BoxSwitchCase node ) {
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
					newLine();
				}
				decreaseIndent();
			}
		}
	}

	public void visit( BoxThrow node ) {
		print( "throw" );
		if ( node.getExpression() != null ) {
			print( " " );
			node.getExpression().accept( this );
		}
		print( ";" );
	}

	public void visit( BoxTry node ) {
		if ( isTemplate() ) {
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
			increaseIndent();
			println( "try {" );
			for ( var statement : node.getTryBody() ) {
				statement.accept( this );
				newLine();
			}
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
					newLine();
				}
				decreaseIndent();
				print( "}" );
			}
		}
	}

	public void visit( BoxTryCatch node ) {
		if ( isTemplate() ) {
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
				newLine();
			}
			decreaseIndent();
			print( "}" );
		}
	}

	public void visit( BoxWhile node ) {
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
	}

	public void visit( BoxComponent node ) {
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
					newLine();
				}
				decreaseIndent();
				print( "}" );
			} else {
				print( ";" );
			}
		}
	}

	public void visit( BoxStatementBlock node ) {
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
				newLine();
			}
			decreaseIndent();
			print( "}" );
		}
	}

}
