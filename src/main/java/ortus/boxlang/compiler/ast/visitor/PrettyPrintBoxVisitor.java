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

import ortus.boxlang.compiler.ast.BoxBufferOutput;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxDocumentation;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxTemplate;
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
import ortus.boxlang.compiler.ast.expression.BoxNewOperation;
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
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxNew;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;

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

	private StringBuffer	buffer		= new StringBuffer();
	private String			indent		= "  ";
	private int				indentLevel	= 0;

	/**
	 * Constructor
	 */
	public PrettyPrintBoxVisitor() {
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
		// if buffer ends with a newline and indent characters, remove of them
		if ( buffer.length() >= indent.length() && buffer.substring( buffer.length() - indent.length() ).equals( indent ) ) {
			buffer.delete( buffer.length() - indent.length(), buffer.length() );
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
		node.getExpression().accept( this );
	}

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
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxDocumentation node ) {
		// TODO: Not sure this node is in use yet
		println( "documentation??" );
	}

	public void visit( BoxScriptIsland node ) {
		println( "<bx:script>" );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLine();
		}
		println( "</bx:script>" );
	}

	public void visit( BoxTemplate node ) {
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLine();
		}
	}

	public void visit( BoxTemplateIsland node ) {
		println( "```" );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
			newLine();
		}
		println( "```" );
	}

	public void visit( BoxArgument node ) {
		if ( node.getName() == null ) {
			node.getValue().accept( this );
		} else {
			node.getName().accept( this );
			print( " = " );
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
		println( "[ " );
		int size = node.getValues().size();
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

	public void visit( BoxNewOperation node ) {
		print( "new " );
		if ( node.getPrefix() != null ) {
			node.getPrefix().accept( this );
			print( ":" );
		}
		node.getExpression().accept( this );
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
		for ( var expr : node.getValues() ) {
			if ( expr instanceof BoxStringLiteral str ) {
				print( str.getValue() );
			} else {
				print( "#" );
				expr.accept( this );
				print( "#" );
			}
		}
		print( "\"" );
	}

	public void visit( BoxStringLiteral node ) {
		print( "\"" );
		print( node.getValue() );
		print( "\"" );
	}

	public void visit( BoxStructLiteral node ) {
		increaseIndent();
		if ( node.getType().equals( BoxStructType.Ordered ) ) {
			println( "[" );
		} else {
			println( "{" );
		}
		int size = node.getValues().size();
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
		print( "@" );
		node.getKey().accept( this );
		if ( node.getValue() != null ) {
			print( " " );
			node.getValue().accept( this );
		}
	}

	public void visit( BoxArgumentDeclaration node ) {
		// TODO: annotations need hoisted up to function declaration
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

	public void visit( BoxAssert node ) {
		print( "assert " );
		node.getExpression().accept( this );
		print( ";" );
	}

	public void visit( BoxBreak node ) {
		print( "break;" );
	}

	public void visit( BoxContinue node ) {
		print( "continue;" );
	}

	public void visit( BoxDo node ) {
		print( "do {" );
		newLine();
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		print( "} while (" );
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
		node.getExpression().accept( this );
		print( ";" );
	}

	public void visit( BoxForIn node ) {
		print( "for( " );
		if ( node.getHasVar() ) {
			print( "var " );
		}
		node.getVariable().accept( this );
		print( " in " );
		node.getExpression().accept( this );
		increaseIndent();
		print( " ) {" );
		newLine();
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxForIndex node ) {
		print( "for( " );
		node.getInitializer().accept( this );
		print( "; " );
		node.getCondition().accept( this );
		print( "; " );
		node.getStep().accept( this );
		increaseIndent();
		println( "++ ) {" );
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxFunctionDeclaration node ) {
		newLine();
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
		increaseIndent();
		if ( hasArgs )
			print( " " );
		println( ") {" );
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		println( "}" );
	}

	public void visit( BoxIfElse node ) {
		print( "if( " );
		node.getCondition().accept( this );
		increaseIndent();
		println( " ) {" );
		for ( var statement : node.getThenBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
		if ( !node.getElseBody().isEmpty() ) {
			if ( node.getThenBody().size() == 1 && node.getThenBody().get( 0 ) instanceof BoxIfElse ) {
				print( " else " );
				increaseIndent();
				node.getElseBody().get( 0 ).accept( this );
				decreaseIndent();
			} else {
				increaseIndent();
				print( " else {" );
				newLine();
				for ( var statement : node.getElseBody() ) {
					statement.accept( this );
					newLine();
				}
				decreaseIndent();
				print( "}" );
			}
		}
	}

	public void visit( BoxImport node ) {
		print( "import " );
		node.getExpression().accept( this );
		if ( node.getAlias() != null ) {
			print( " as " );
			node.getAlias().accept( this );
		}
		print( ";" );
	}

	public void visit( BoxNew node ) {
		print( "new " );
		node.getFqn().accept( this );
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

	public void visit( BoxParam node ) {
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

	public void visit( BoxProperty node ) {
		if ( node.getDocumentation() != null && !node.getDocumentation().isEmpty() ) {
			println( "/**" );
			for ( var doc : node.getDocumentation() ) {
				print( "  * " );
				doc.accept( this );
				newLine();
			}
			println( "*/" );
		}
		print( "property " );
		// TODO: Handle these accounting for shorcut syntax
		// also need to seperate pre and inline annotations
		for ( var anno : node.getAnnotations() ) {
			anno.getKey().accept( this );
			if ( anno.getValue() != null ) {
				print( "=" );
				anno.getValue().accept( this );
			}
		}
		println( ";" );
	}

	public void visit( BoxRethrow node ) {
		print( "rethrow;" );
	}

	public void visit( BoxReturn node ) {
		print( "return" );
		if ( node.getExpression() != null ) {
			print( " " );
			node.getExpression().accept( this );
		}
		print( ";" );
	}

	public void visit( BoxReturnType node ) {
		if ( node.getType().equals( BoxType.Fqn ) ) {
			print( node.getFqn() );
		} else {
			print( node.getType().toString().toLowerCase() );
		}
	}

	public void visit( BoxSwitch node ) {
		print( "switch (" );
		node.getCondition().accept( this );
		increaseIndent();
		println( ") {" );
		for ( var caseNode : node.getCases() ) {
			caseNode.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxSwitchCase node ) {
		if ( node.getCondition() == null ) {
			print( "defaultCase:" );
		} else {
			print( "case " );
			node.getCondition().accept( this );
			print( ":" );
		}
		newLine();
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
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

	public void visit( BoxTryCatch node ) {
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

	public void visit( BoxWhile node ) {
		print( "while (" );
		node.getCondition().accept( this );
		increaseIndent();
		println( ") {" );
		for ( var statement : node.getBody() ) {
			statement.accept( this );
			newLine();
		}
		decreaseIndent();
		print( "}" );
	}

	public void visit( BoxComponent node ) {
		print( node.getName() );
		print( " " );
		for ( var attr : node.getAttributes() ) {
			attr.accept( this );
			print( " " );
		}
		if ( node.getBody() != null && !node.getBody().isEmpty() ) {
			increaseIndent();
			print( "{" );
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
