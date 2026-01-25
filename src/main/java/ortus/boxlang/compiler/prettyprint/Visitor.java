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
import java.util.Stack;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
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
import ortus.boxlang.compiler.ast.expression.BoxFunctionalBIFAccess;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalMemberAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNegateOperation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.sql.select.SQLTableVariable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLCase;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLCaseWhenThen;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLCountFunction;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLFunction;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLOrderBy;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLParam;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLParenthesis;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLStarExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLBooleanLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNullLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLStringLiteral;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBetweenOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLBinaryOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLInOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLInSubQueryOperation;
import ortus.boxlang.compiler.ast.sql.select.expression.operation.SQLUnaryOperation;
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
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Pretty print BoxLang AST nodes.
 * Traverses the BoxLang AST and builds a Doc tree for formatting.
 */
public class Visitor extends VoidBoxVisitor {

	private Stack<BoxSourceType>	currentSourceType	= new Stack<>();
	private Stack<Doc>				docStack			= new Stack<>();

	Config							config;
	String							componentPrefix;

	CommentsPrinter					commentsPrinter;
	ClassPrinter					classPrinter;
	StringPrinter					stringPrinter;
	ComponentPrinter				componentPrinter;
	HelperPrinter					helperPrinter;
	ArgumentsPrinter				argumentsPrinter;
	ParametersPrinter				parametersPrinter;
	FunctionDeclarationPrinter		functionDeclaration;
	StructLiteralPrinter			structLiteralPrinter;
	ArrayLiteralPrinter				arrayLiteralPrinter;

	/**
	 * Constructor
	 * 
	 * @param sourceType The source type of the node being visited
	 * @param config     The configuration for printing
	 */
	public Visitor( BoxSourceType sourceType, Config config ) {
		this.config = config;
		currentSourceType.push( sourceType );
		pushDoc( DocType.ARRAY );

		this.componentPrefix		= switch ( sourceType ) {
										case BOXSCRIPT, BOXTEMPLATE -> "bx:";
										default -> "cf";
									};

		this.commentsPrinter		= new CommentsPrinter( this );
		this.classPrinter			= new ClassPrinter( this );
		this.stringPrinter			= new StringPrinter( this );
		this.componentPrinter		= new ComponentPrinter( this );
		this.helperPrinter			= new HelperPrinter( this );
		this.structLiteralPrinter	= new StructLiteralPrinter( this );
		this.arrayLiteralPrinter	= new ArrayLiteralPrinter( this );
		this.functionDeclaration	= new FunctionDeclarationPrinter( this );
		this.parametersPrinter		= new ParametersPrinter( this );
		this.argumentsPrinter		= new ArgumentsPrinter( this );
	}

	public Doc getRoot() {
		return docStack.firstElement();
	}

	boolean isTemplate() {
		return switch ( currentSourceType.peek() ) {
			case BOXTEMPLATE, CFTEMPLATE -> true;
			default -> false;
		};
	}

	Doc getCurrentDoc() {
		return docStack.peek();
	}

	Doc pushDoc( DocType docType ) {
		var doc = new Doc( docType );
		docStack.push( doc );
		return doc;
	}

	Doc popDoc() {
		return docStack.pop();
	}

	public void newLine() {
		getCurrentDoc().append( Line.HARD );
	}

	public void print( String text ) {
		getCurrentDoc().append( text );
	}

	boolean printPreComments( BoxNode node ) {
		return commentsPrinter.printPreComments( node );
	}

	boolean printPostComments( BoxNode node ) {
		return commentsPrinter.printPostComments( node );
	}

	boolean printInsideComments( BoxNode node, boolean indent ) {
		return commentsPrinter.printInsideComments( node, indent );
	}

	public void visit( BoxSingleLineComment node ) {
		commentsPrinter.print( node );

	}

	public void visit( BoxMultiLineComment node ) {
		commentsPrinter.print( node );
	}

	public void visit( BoxDocComment node ) {
		commentsPrinter.print( node );
	}

	public void visit( BoxScript node ) {
		printPreComments( node );
		helperPrinter.printStatements( node.getStatements() );
		printPostComments( node );
		newLine();
	}

	public void visit( BoxClass node ) {
		classPrinter.print( node, currentSourceType.peek() );
	}

	public void visit( BoxInterface node ) {
		classPrinter.print( node, currentSourceType.peek() );
	}

	public void visit( BoxScriptIsland node ) {
		printPreComments( node );
		boolean isTemplate = isTemplate();
		currentSourceType.push( BoxSourceType.BOXSCRIPT );
		if ( isTemplate ) {
			print( "<bx:script>" );
			pushDoc( DocType.INDENT );
			newLine();
		}

		helperPrinter.printStatements( node.getStatements() );

		if ( isTemplate ) {
			var contentsDoc = popDoc(); // pop the indent doc
			getCurrentDoc()
			    .append( contentsDoc )
			    .append( Line.HARD )
			    .append( "</bx:script>" );
		}
		currentSourceType.pop();
		printPostComments( node );
	}

	public void visit( BoxTemplate node ) {
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		printPreComments( node );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
		}
		printInsideComments( node, false );
		printPostComments( node );
		currentSourceType.pop();
	}

	public void visit( BoxTemplateIsland node ) {
		printPreComments( node );
		print( "```" );
		currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		for ( var statement : node.getStatements() ) {
			statement.accept( this );
		}
		currentSourceType.pop();
		print( "```" );
		printPostComments( node );
	}

	public void visit( BoxBufferOutput node ) {
		printPreComments( node );
		// printPreComments( node.getExpression() );
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
				stringPrinter.processStringInterp( sInt, null );
			} else {
				throw new BoxRuntimeException( "Unexpected expression in buffer output: " + node.getExpression().getClass().getName() );
			}
		} else {
			print( "echo( \"" );
			stringPrinter.printQuotedExpression( node.getExpression() );
			print( "\" )" );
		}
		printPostComments( node.getExpression() );
		printPostComments( node );
	}

	public void visit( BoxExpressionStatement node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "set " );
			node.getExpression().accept( this );
			print( ">" );
		} else {
			node.getExpression().accept( this );
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxAssignment node ) {
		var currentDoc = getCurrentDoc();

		if ( node.getModifiers().contains( BoxAssignmentModifier.VAR ) ) {
			currentDoc.append( "var " );
		}

		if ( node.getModifiers().contains( BoxAssignmentModifier.FINAL ) ) {
			currentDoc.append( "final " );
		}

		if ( node.getModifiers().contains( BoxAssignmentModifier.STATIC ) ) {
			currentDoc.append( "static " );
		}

		node.getLeft().accept( this );
		currentDoc.append( " " );
		currentDoc.append( node.getOp().getSymbol() );
		currentDoc.append( " " );
		node.getRight().accept( this );
	}

	public void visit( BoxBinaryOperation node ) {
		var	currentDoc		= getCurrentDoc();

		var	inGroup			= ( node.getParent() instanceof BoxBinaryOperation ||
		    node.getParent() instanceof BoxParenthesis || node.getParent() instanceof BoxIfElse );
		var	needsPadding	= config.getBinaryOperatorsPadding();

		if ( !inGroup ) {
			var	binaryDoc	= pushDoc( DocType.GROUP );
			var	indentDoc	= pushDoc( DocType.INDENT );
			node.getLeft().accept( this );
			indentDoc
			    .append( needsPadding ? Line.LINE : Line.SOFT )
			    .append( node.getOperator().getSymbol() )
			    .append( needsPadding ? Line.LINE : Line.SOFT );
			node.getRight().accept( this );
			binaryDoc.append( popDoc() );

			currentDoc.append( popDoc() );
		} else {
			node.getLeft().accept( this );
			currentDoc
			    .append( needsPadding ? Line.LINE : Line.SOFT )
			    .append( node.getOperator().getSymbol() )
			    .append( needsPadding ? Line.LINE : Line.SOFT );
			node.getRight().accept( this );
		}
	}

	public void visit( BoxComparisonOperation node ) {
		var	currentDoc	= getCurrentDoc();

		var	inGroup		= ( node.getParent() instanceof BoxParenthesis );

		if ( !inGroup ) {
			var	binaryDoc	= pushDoc( DocType.GROUP );
			var	indentDoc	= pushDoc( DocType.INDENT );
			node.getLeft().accept( this );
			indentDoc
			    .append( " " )
			    .append( node.getOperator().getSymbol() )
			    .append( Line.LINE );
			node.getRight().accept( this );
			binaryDoc.append( popDoc() );

			currentDoc.append( popDoc() );
		} else {
			node.getLeft().accept( this );
			currentDoc
			    .append( " " )
			    .append( node.getOperator().getSymbol() )
			    .append( Line.LINE );
			node.getRight().accept( this );
		}
	}

	public void visit( BoxParenthesis node ) {
		helperPrinter.printParensExpression( node.getExpression() );
	}

	public void visit( BoxFunctionDeclaration node ) {
		printPreComments( node );
		functionDeclaration.print( node, currentSourceType.peek() );
		printPostComments( node );
	}

	public void visit( BoxClosure node ) {
		printPreComments( node );
		// TODO: Make AST "remember" difference between original function(){} and ()=>{}
		// for now check the source text to see if it starts with "function"
		var isLambda = !node.getSourceText().startsWith( "function" );
		if ( !isLambda ) {
			print( "function" );
		}
		parametersPrinter.print( node.getArgs() );
		print( isLambda ? " => " : " " );

		// if the closure is a lambda, and the body is a single expression,
		// the AST has it as an expression statement, not just an expression
		// handle it here to avoid printing the semicolon
		if ( isLambda && node.getBody() instanceof BoxExpressionStatement exprStmt ) {
			printPreComments( exprStmt );
			exprStmt.getExpression().accept( this );
			printPostComments( exprStmt );
		} else {
			// otherwise, just visit the body
			node.getBody().accept( this );
		}
		printPostComments( node );
	}

	public void visit( BoxLambda node ) {
		printPreComments( node );

		var	args		= node.getArgs();
		var	arrowParens	= config.getFunction().getArrow().getParens();

		// Check if we can omit parentheses: "avoid" mode, single param, no explicit type, no default, not required, no annotations
		boolean canOmitParens = false;
		if ( arrowParens.equals( "avoid" ) && args.size() == 1 ) {
			var		arg				= args.get( 0 );
			// Check if type was explicitly specified (not just default "Any")
			boolean	hasExplicitType	= arg.getType() != null
			    && ( !arg.getType().equals( "Any" ) || ( arg.getSourceText() != null && arg.getSourceText().contains( "Any " ) ) );
			boolean	hasDefault		= arg.getValue() != null;
			boolean	isRequired		= arg.getRequired() != null && arg.getRequired();
			boolean	hasAnnotations	= arg.getAnnotations() != null && !arg.getAnnotations().isEmpty();

			canOmitParens = !hasExplicitType && !hasDefault && !isRequired && !hasAnnotations;
		}

		if ( canOmitParens ) {
			print( args.get( 0 ).getName() );
		} else {
			parametersPrinter.print( args );
		}

		print( " -> " );
		node.getBody().accept( this );
		printPostComments( node );
	}

	public void visit( BoxFunctionInvocation node ) {
		printPreComments( node );
		getCurrentDoc().append( node.getName() );
		argumentsPrinter.print( node, node.getArguments() );
		printPostComments( node );
	}

	public void visit( BoxMethodInvocation node ) {
		var							currentDoc	= getCurrentDoc();
		List<BoxMethodInvocation>	methodChain	= new ArrayList<>();
		BoxNode						currentNode	= node;

		while ( currentNode instanceof BoxMethodInvocation methodNode ) {
			methodChain.add( methodNode );
			currentNode = methodNode.getObj();
		}

		methodChain.getLast().getObj().accept( this );

		var	chainGroup	= pushDoc( DocType.GROUP );
		var	indentGroup	= pushDoc( DocType.INDENT );
		for ( int i = methodChain.size() - 1; i >= 0; i-- ) {
			var chainedNode = methodChain.get( i );
			indentGroup.append( Line.SOFT );
			printPreComments( chainedNode );

			if ( chainedNode.getUsedDotAccess() ) {
				if ( chainedNode.isSafe() ) {
					print( "?" );
				}
				print( "." );
				chainedNode.getName().accept( this );
			} else {
				print( "[ " );
				chainedNode.getName().accept( this );
				print( " ]" );
			}
			argumentsPrinter.print( chainedNode, chainedNode.getArguments() );
			printPostComments( chainedNode );
		}
		chainGroup.append( popDoc() );
		currentDoc.append( popDoc() );

	}

	public void visit( BoxStaticMethodInvocation node ) {
		printPreComments( node );
		node.getObj().accept( this );
		print( "::" );
		node.getName().accept( this );
		argumentsPrinter.print( node, node.getArguments() );
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

	public void visit( BoxStaticAccess node ) {
		printPreComments( node );
		node.getContext().accept( this );
		print( "::" );
		node.getAccess().accept( this );
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

	public void visit( BoxFunctionalBIFAccess node ) {
		printPreComments( node );
		print( "::" );
		print( node.getName() );
		printPostComments( node );
	}

	public void visit( BoxFunctionalMemberAccess node ) {
		printPreComments( node );
		print( "." );
		print( node.getName() );
		argumentsPrinter.print( node, node.getArguments() );
		printPostComments( node );
	}

	public void visit( BoxExpressionInvocation node ) {
		printPreComments( node );
		node.getExpr().accept( this );
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

	public void visit( BoxIdentifier node ) {
		printPreComments( node );
		print( node.getName() );
		printPostComments( node );
	}

	public void visit( BoxBooleanLiteral node ) {
		printPreComments( node );
		print( node.getValue() ? "true" : "false" );
		printPostComments( node );
	}

	public void visit( BoxIntegerLiteral node ) {
		printPreComments( node );
		print( node.getValue() );
		printPostComments( node );
	}

	public void visit( BoxDecimalLiteral node ) {
		printPreComments( node );
		print( node.getValue() );
		printPostComments( node );
	}

	public void visit( BoxNull node ) {
		printPreComments( node );
		print( "null" );
		printPostComments( node );
	}

	public void visit( BoxScope node ) {
		printPreComments( node );
		print( node.getName() );
		printPostComments( node );
	}

	public void visit( BoxFQN node ) {
		printPreComments( node );
		print( node.getValue() );
		printPostComments( node );
	}

	public void visit( BoxStringLiteral node ) {
		stringPrinter.printStringLiteral( node );
	}

	public void visit( BoxStringConcat node ) {
		stringPrinter.printStringConcat( node );
	}

	public void visit( BoxStringInterpolation node ) {
		stringPrinter.printStringInterpolation( node );
	}

	public void visit( BoxArrayLiteral node ) {
		arrayLiteralPrinter.print( node );
	}

	public void visit( BoxStructLiteral node ) {
		structLiteralPrinter.print( node );
	}

	public void visit( BoxNegateOperation node ) {
		printPreComments( node );
		print( "not " );
		node.getExpr().accept( this );
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

	public void visit( BoxTernaryOperation node ) {
		printPreComments( node );
		node.getCondition().accept( this );
		print( " ? " );
		node.getWhenTrue().accept( this );
		print( " : " );
		node.getWhenFalse().accept( this );
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
		argumentsPrinter.print( node, node.getArguments() );
		printPostComments( node );
	}

	// statement visitors

	public void visit( BoxAssert node ) {
		printPreComments( node );
		print( "assert " );
		node.getExpression().accept( this );
		print( ";" );
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

	public void visit( BoxStatementBlock node ) {
		printPreComments( node );
		helperPrinter.printBlock( node, node.getBody() );
		printPostComments( node );
	}

	public void visit( BoxStaticInitializer node ) {
		if ( !isTemplate() ) {
			printPreComments( node );
			print( "static " );
			helperPrinter.printBlock( node, node.getBody() );
			printPostComments( node );
		}
	}

	public void visit( BoxBreak node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "break" );
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
			print( "<" + componentPrefix + "continue" );
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

	public void visit( BoxForIn node ) {
		printPreComments( node );
		if ( node.getLabel() != null ) {
			print( node.getLabel() );
			print( ": " );
		}
		print( "for (" );
		if ( node.getHasVar() ) {
			print( "var " );
		}
		node.getVariable().accept( this );
		print( " in " );
		node.getExpression().accept( this );
		print( ") " );
		node.getBody().accept( this );
		printPostComments( node );
	}

	public void visit( BoxForIndex node ) {
		printPreComments( node );
		if ( node.getLabel() != null ) {
			print( node.getLabel() );
			print( ": " );
		}
		print( "for (" );
		if ( node.getInitializer() != null ) {
			node.getInitializer().accept( this );
		}

		print( ";" );
		if ( config.getForLoopSemicolons().getPadding() ) {
			print( " " );
		}

		if ( node.getCondition() != null ) {
			node.getCondition().accept( this );
		}

		print( ";" );
		if ( config.getForLoopSemicolons().getPadding() ) {
			print( " " );
		}

		if ( node.getStep() != null ) {
			node.getStep().accept( this );
		}
		print( ") " );
		node.getBody().accept( this );
		printPostComments( node );
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

	public void visit( BoxComponent node ) {
		componentPrinter.print( node );
	}

	public void visit( BoxParam node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "param" );
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
				stringPrinter.printQuotedExpression( node.getDefaultValue() );
				print( "\"" );
			}
			print( ">" );
		} else {
			print( "param " );
			if ( node.getType() != null ) {
				if ( node.getType() instanceof BoxStringLiteral str ) {
					print( str.getValue() );
				} else {
					node.getType().accept( this );
				}
				print( " " );
			}
			if ( node.getVariable() instanceof BoxStringLiteral str ) {
				print( str.getValue() );
			} else {
				node.getVariable().accept( this );
			}
			if ( node.getDefaultValue() != null ) {
				print( " = " );
				node.getDefaultValue().accept( this );
			}
			print( ";" );
		}
		printPostComments( node );
	}

	public void visit( BoxProperty node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "property" );
			helperPrinter.printKeyValueAnnotations( node.getAllAnnotations(), false );
			print( ">" );
		} else {
			for ( var anno : node.getAnnotations() ) {
				anno.accept( this );
				newLine();
			}
			print( "property" );
			// TODO: Handle these accounting for shorcut syntax
			// also need to seperate pre and inline annotations
			var	size			= node.getPostAnnotations().size();
			var	multiline		= size > config.getProperty().getMultiline().getElementCount();
			var	keyValuePadding	= config.getProperty().getKeyValue().getPadding();
			if ( node.getSourceText() == null
			    || ( node.getSourceText().length() > config.getProperty().getMultiline().getMinLength() ) ) {
				multiline = true;
			}

			Doc	currentDoc	= getCurrentDoc();
			Doc	propDoc		= currentDoc;

			if ( multiline ) {
				propDoc = pushDoc( DocType.INDENT );
			}

			for ( int i = 0; i < size; i++ ) {
				var anno = node.getPostAnnotations().get( i );

				if ( i < size ) {
					if ( multiline ) {
						propDoc.append( Line.HARD );
					} else {
						propDoc.append( " " );
					}
				}

				anno.getKey().accept( this );
				if ( anno.getValue() != null ) {
					propDoc.append( keyValuePadding ? " = " : "=" );
					anno.getValue().accept( this );
				}
			}
			propDoc.append( ";" );

			if ( multiline ) {
				popDoc();
				currentDoc.append( propDoc );
			}
		}
		printPostComments( node );
	}

	public void visit( BoxRethrow node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "rethrow>" );
		} else {
			print( "rethrow;" );
		}
		printPostComments( node );
	}

	public void visit( BoxReturn node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "return" );
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
				print( "<" + componentPrefix + "if " );
			}
			node.getCondition().accept( this );
			print( ">" );
			node.getThenBody().accept( this );

			if ( node.getElseBody() != null ) {
				if ( node.getElseBody() instanceof BoxStatementBlock elseBlock &&
				    elseBlock.getBody().size() == 1 &&
				    elseBlock.getBody().get( 0 ) instanceof BoxIfElse elseNode ) {
					print( "<" + componentPrefix + "else" );
					doBoxIfElse( elseNode, true );
				} else {
					print( "<" + componentPrefix + "else>" );
					node.getElseBody().accept( this );
					print( "</" + componentPrefix + "if>" );
				}
			} else {
				print( "</" + componentPrefix + "if>" );
			}
		} else {
			print( "if " );
			helperPrinter.printParensExpression( node.getCondition() );
			print( " " );
			node.getThenBody().accept( this );
			if ( node.getElseBody() != null ) {
				print( " else " );
				node.getElseBody().accept( this );
			}
		}
	}

	public void visit( BoxSwitch node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "switch expression=\"" );
			stringPrinter.printQuotedExpression( node.getCondition() );
			print( "\">" );
			for ( var caseNode : node.getCases() ) {
				newLine();
				caseNode.accept( this );
			}
			newLine();
			print( "</" + componentPrefix + "switch>" );
		} else {
			var currentDoc = getCurrentDoc();

			currentDoc.append( "switch " );
			helperPrinter.printParensExpression( node.getCondition() );
			currentDoc.append( " " ).append( "{" );

			var blockDoc = pushDoc( DocType.INDENT );
			blockDoc.append( Line.HARD );

			var cases = node.getCases();
			if ( cases != null && !cases.isEmpty() ) {
				var lastCase = cases.get( cases.size() - 1 );
				for ( var caseNode : cases ) {
					caseNode.accept( this );
					// if the statement is not the last one, append a hard line break
					if ( caseNode != lastCase ) {
						blockDoc.append( Line.HARD );
					}
				}
			}

			currentDoc
			    .append( popDoc() )
			    .append( Line.HARD )
			    .append( "}" );
		}

		printPostComments( node );
	}

	public void visit( BoxSwitchCase node ) {
		var currentDoc = getCurrentDoc();

		printPreComments( node );
		if ( isTemplate() ) {
			if ( node.getCondition() != null ) {
				print( "<" + componentPrefix + "case value=\"" );
				stringPrinter.printQuotedExpression( node.getCondition() );
				print( "\">" );
			} else if ( node.getBody() != null ) {
				print( "<" + componentPrefix + "defaultcase>" );
			} else {
				print( "<" + componentPrefix + "defaultcase/>" );
			}
			if ( node.getBody() != null ) {
				for ( var statement : node.getBody() ) {
					statement.accept( this );
				}
			}
			if ( node.getCondition() != null ) {
				print( "</" + componentPrefix + "case>" );
			} else if ( node.getBody() != null ) {
				print( "</" + componentPrefix + "defaultcase>" );
			}
		} else {
			if ( node.getCondition() == null ) {
				currentDoc.append( "default:" );
			} else {
				currentDoc.append( "case " );
				node.getCondition().accept( this );
				currentDoc.append( ":" );
			}
			if ( node.getBody().size() == 1 && node.getBody().get( 0 ) instanceof BoxStatementBlock ) {
				currentDoc.append( " " );
				node.getBody().get( 0 ).accept( this );
				newLine();
			} else {
				var caseDoc = pushDoc( DocType.INDENT );
				caseDoc.append( Line.HARD );
				helperPrinter.printStatements( node.getBody() );
				currentDoc.append( popDoc() );
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
		printPreComments( node );
		if ( isTemplate() ) {
			printPreComments( node );
			print( "<" + componentPrefix + "try>" );

			for ( var statement : node.getTryBody() ) {
				statement.accept( this );
			}
			for ( var catchNode : node.getCatches() ) {
				catchNode.accept( this );
			}
			if ( node.getFinallyBody() != null && !node.getFinallyBody().isEmpty() ) {
				print( "<" + componentPrefix + "finally>" );
				for ( var statement : node.getFinallyBody() ) {
					statement.accept( this );
				}
				print( "</" + componentPrefix + "finally>" );
			}
			print( "</" + componentPrefix + "try>" );
		} else {
			print( "try " );
			helperPrinter.printBlock( node, node.getTryBody() );
			if ( !node.getCatches().isEmpty() ) {
				for ( var catchNode : node.getCatches() ) {
					catchNode.accept( this );
				}
			}
			if ( node.getFinallyBody() != null && !node.getFinallyBody().isEmpty() ) {
				print( " finally " );
				helperPrinter.printBlock( node, node.getFinallyBody() );
			}
		}

		printPostComments( node );
	}

	public void visit( BoxTryCatch node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "catch" );
			if ( !node.getCatchTypes().isEmpty() ) {
				print( " type=\"" );
				// should only be one when in tags
				stringPrinter.printQuotedExpression( node.getCatchTypes().get( 0 ) );
				print( "\"" );
			}
			print( ">" );
			for ( var statement : node.getCatchBody() ) {
				statement.accept( this );
			}
			print( "</" + componentPrefix + "catch>" );
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
			print( ") " );
			helperPrinter.printBlock( node, node.getCatchBody() );
		}
		printPostComments( node );
	}

	public void visit( BoxWhile node ) {
		printPreComments( node );
		if ( isTemplate() ) {
			print( "<" + componentPrefix + "while condition=\"" );
			stringPrinter.printQuotedExpression( node.getCondition() );
			print( "\"" );
			if ( node.getLabel() != null ) {
				print( " label=\"" );
				print( node.getLabel() );
				print( "\"" );
			}
			print( ">" );
			node.getBody().accept( this );
			print( "</" + componentPrefix + "while>" );
		} else {
			if ( node.getLabel() != null ) {
				print( node.getLabel() );
				print( ": " );
			}
			print( "while " );
			helperPrinter.printParensExpression( node.getCondition() );
			print( " " );
			node.getBody().accept( this );
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
		print( " while " );
		helperPrinter.printParensExpression( node.getCondition() );
		print( ";" );
		printPostComments( node );
	}

	// SQL AST Nodes

	public void visit( SQLBooleanLiteral node ) {
		printPreComments( node );
		print( String.valueOf( node.getValue() ) );
		printPostComments( node );
	}

	public void visit( SQLNullLiteral node ) {
		printPreComments( node );
		print( "null" );
		printPostComments( node );
	}

	public void visit( SQLNumberLiteral node ) {
		printPreComments( node );
		print( String.valueOf( node.getValue() ) );
		printPostComments( node );
	}

	public void visit( SQLStringLiteral node ) {
		printPreComments( node );
		print( "'" );
		print( node.getValue().replace( "'", "''" ) );
		print( "'" );
		printPostComments( node );
	}

	public void visit( SQLBetweenOperation node ) {
		printPreComments( node );
		node.getExpression().accept( this );
		if ( node.isNot() ) {
			print( " not" );
		}
		print( " between " );
		node.getLeft().accept( this );
		print( " and " );
		node.getRight().accept( this );
		printPostComments( node );
	}

	public void visit( SQLBinaryOperation node ) {
		printPreComments( node );
		node.getLeft().accept( this );
		print( " " );
		print( node.getOperator().getSymbol() );
		print( " " );
		node.getRight().accept( this );
		printPostComments( node );
	}

	public void visit( SQLInOperation node ) {
		printPreComments( node );
		node.getExpression().accept( this );
		if ( node.isNot() ) {
			print( " not" );
		}
		print( " in (" );
		int size = node.getValues().size();
		if ( size > 0 ) {
			print( " " );
		}
		for ( int i = 0; i < size; i++ ) {
			node.getValues().get( i ).accept( this );
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

	public void visit( SQLInSubQueryOperation node ) {
		printPreComments( node );
		node.getExpression().accept( this );
		if ( node.isNot() ) {
			print( " not" );
		}
		print( " in (" );
		node.getSubQuery().accept( this );
		print( ")" );
		printPostComments( node );
	}

	public void visit( SQLUnaryOperation node ) {
		printPreComments( node );
		print( node.getOperator().getSymbol() );
		node.getExpression().accept( this );
		printPostComments( node );
	}

	public void visit( SQLCase node ) {
		printPreComments( node );
		print( "case" );
		if ( node.getInputExpression() != null ) {
			print( " " );
			node.getInputExpression().accept( this );
		}
		// increaseIndent();
		for ( var whenThen : node.getWhenThens() ) {
			whenThen.accept( this );
		}
		if ( node.getElseExpression() != null ) {
			print( " else " );
			node.getElseExpression().accept( this );
		}
		// decreaseIndent();
		print( " end" );
		printPostComments( node );
	}

	public void visit( SQLCaseWhenThen node ) {
		printPreComments( node );
		print( " when " );
		node.getWhenExpression().accept( this );
		print( " then " );
		node.getThenExpression().accept( this );
		printPostComments( node );
	}

	public void visit( SQLColumn node ) {
		printPreComments( node );
		// TODO, actually track in the SQLColumn node what we had for the original table reference
		if ( node.getTable() != null && node.getTable() instanceof SQLTableVariable stv ) {
			print( stv.getAlias() != null ? stv.getAlias().getName() : stv.getName().getName() );
			print( "." );
		}
		print( node.getName().getName() );
		printPostComments( node );
	}

	public void visit( SQLCountFunction node ) {
		printPreComments( node );
		print( "count( " );
		if ( node.isDistinct() ) {
			print( "distinct " );
		}
		node.getArguments().get( 0 ).accept( this );
		print( " )" );
		printPostComments( node );
	}

	public void visit( SQLFunction node ) {
		printPreComments( node );
		print( node.getName().getName() );
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

	public void visit( SQLOrderBy node ) {
		printPreComments( node );
		node.getExpression().accept( this );
		if ( !node.isAscending() ) {
			print( " desc" );
		}
		printPostComments( node );
	}

	public void visit( SQLParam node ) {
		printPreComments( node );
		if ( node.getName() != null ) {
			print( ":" );
			print( node.getName() );
		} else {
			// I need this to be a unique for each ordered param
			print( "? /* position: " );
			print( String.valueOf( node.getPosition() ) );
			print( " */" );

		}
		printPostComments( node );
	}

	public void visit( SQLParenthesis node ) {
		printPreComments( node );
		print( "( " );
		node.getExpression().accept( this );
		print( " )" );
		printPostComments( node );
	}

	public void visit( SQLStarExpression node ) {
		printPreComments( node );
		// TODO, actually track in the SQLColumn node what we had for the original table reference
		if ( node.getTable() != null && node.getTable() instanceof SQLTableVariable stv ) {
			print( stv.getAlias() != null ? stv.getAlias().getName() : stv.getName().getName() );
			print( "." );
		}
		print( "*" );
		printPostComments( node );
	}

}
