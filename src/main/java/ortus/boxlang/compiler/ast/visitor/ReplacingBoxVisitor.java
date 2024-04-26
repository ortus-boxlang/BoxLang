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

import java.util.List;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxDocumentation;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
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
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;

/**
 * Base class for the BoxLang AST Nodes that allows children to be replaced in-place
 */
public abstract class ReplacingBoxVisitor {

	/**
	 * Constructor
	 */
	protected ReplacingBoxVisitor() {
	}

	public BoxNode visit( BoxScript node ) {
		handleStatements( node.getStatements(), node );
		return node;
	}

	public BoxNode visit( BoxInterface node ) {
		handleStatements( node.getBody(), node );
		for ( int i = 0; i < node.getImports().size(); i++ ) {
			BoxImport	importNode	= node.getImports().get( i );
			BoxNode		newImport	= importNode.accept( this );
			if ( newImport != importNode ) {
				node.replaceChildren( newImport, importNode );
				node.getImports().set( i, ( BoxImport ) newImport );
			}
		}
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxAnnotation	annotationNode	= node.getAnnotations().get( i );
			BoxNode			newAnnotation	= annotationNode.accept( this );
			if ( newAnnotation != annotationNode ) {
				node.replaceChildren( newAnnotation, annotationNode );
				node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		for ( int i = 0; i < node.getPostAnnotations().size(); i++ ) {
			BoxAnnotation	annotationNode	= node.getPostAnnotations().get( i );
			BoxNode			newAnnotation	= annotationNode.accept( this );
			if ( newAnnotation != annotationNode ) {
				node.replaceChildren( newAnnotation, annotationNode );
				node.getPostAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		for ( int i = 0; i < node.getDocumentation().size(); i++ ) {
			BoxDocumentationAnnotation	documentationNode	= node.getDocumentation().get( i );
			BoxNode						newDocumentation	= documentationNode.accept( this );
			if ( newDocumentation != documentationNode ) {
				node.replaceChildren( newDocumentation, documentationNode );
				node.getDocumentation().set( i, ( BoxDocumentationAnnotation ) newDocumentation );
			}
		}
		return node;
	}

	public BoxNode visit( BoxBufferOutput node ) {
		BoxExpression	expr	= node.getExpression();
		BoxNode			newExpr	= expr.accept( this );
		if ( newExpr != expr ) {
			node.setExpression( ( BoxExpression ) newExpr );
		}
		return node;
	}

	public BoxNode visit( BoxClass node ) {
		handleStatements( node.getBody(), node );
		for ( int i = 0; i < node.getImports().size(); i++ ) {
			BoxImport	importNode	= node.getImports().get( i );
			BoxNode		newImport	= importNode.accept( this );
			if ( newImport != importNode ) {
				node.replaceChildren( newImport, importNode );
				node.getImports().set( i, ( BoxImport ) newImport );
			}
		}
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxAnnotation	annotationNode	= node.getAnnotations().get( i );
			BoxNode			newAnnotation	= annotationNode.accept( this );
			if ( newAnnotation != annotationNode ) {
				node.replaceChildren( newAnnotation, annotationNode );
				node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		for ( int i = 0; i < node.getDocumentation().size(); i++ ) {
			BoxDocumentationAnnotation	documentationNode	= node.getDocumentation().get( i );
			BoxNode						newDocumentation	= documentationNode.accept( this );
			if ( newDocumentation != documentationNode ) {
				node.replaceChildren( newDocumentation, documentationNode );
				node.getDocumentation().set( i, ( BoxDocumentationAnnotation ) newDocumentation );
			}
		}
		for ( int i = 0; i < node.getProperties().size(); i++ ) {
			BoxProperty	propertyNode	= node.getProperties().get( i );
			BoxNode		newProperty		= propertyNode.accept( this );
			if ( newProperty != propertyNode ) {
				node.replaceChildren( newProperty, propertyNode );
				node.getProperties().set( i, ( BoxProperty ) newProperty );
			}
		}
		return node;
	}

	public BoxNode visit( BoxDocumentation node ) {
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxNode	annotationNode	= node.getAnnotations().get( i );
			BoxNode	newAnnotation	= annotationNode.accept( this );
			if ( newAnnotation != annotationNode ) {
				node.replaceChildren( newAnnotation, annotationNode );
				node.getAnnotations().set( i, newAnnotation );
			}
		}
		return node;
	}

	public BoxNode visit( BoxScriptIsland node ) {
		handleStatements( node.getStatements(), node );
		return node;
	}

	public BoxNode visit( BoxTemplateIsland node ) {
		handleStatements( node.getStatements(), node );
		return node;
	}

	public BoxNode visit( BoxTemplate node ) {
		handleStatements( node.getStatements(), node );
		return node;
	}

	public BoxNode visit( BoxArgument node ) {
		BoxExpression name = node.getName();
		if ( name != null ) {
			BoxNode newName = name.accept( this );
			if ( newName != name ) {
				node.setName( ( BoxExpression ) newName );
			}
		}
		BoxExpression	value		= node.getValue();
		BoxNode			newValue	= value.accept( this );
		if ( newValue != value ) {
			node.setValue( ( BoxExpression ) newValue );
		}
		return node;
	}

	public BoxNode visit( BoxArrayAccess node ) {
		BoxExpression	context		= node.getContext();
		BoxNode			newContext	= context.accept( this );
		if ( newContext != context ) {
			node.setContext( ( BoxExpression ) newContext );
		}
		BoxExpression	access		= node.getAccess();
		BoxNode			newAccess	= access.accept( this );
		if ( newAccess != access ) {
			node.setAccess( ( BoxExpression ) newAccess );
		}
		return node;
	}

	public BoxNode visit( BoxArrayLiteral node ) {
		for ( int i = 0; i < node.getValues().size(); i++ ) {
			BoxExpression	value		= node.getValues().get( i );
			BoxNode			newValue	= value.accept( this );
			if ( newValue != value ) {
				node.replaceChildren( newValue, value );
				node.getValues().set( i, ( BoxExpression ) newValue );
			}
		}
		return node;
	}

	public BoxNode visit( BoxAssignment node ) {
		BoxExpression	left	= node.getLeft();
		BoxNode			newLeft	= left.accept( this );
		if ( newLeft != left ) {
			node.setLeft( ( BoxExpression ) newLeft );
		}
		BoxExpression	right		= node.getRight();
		BoxNode			newRight	= right.accept( this );
		if ( newRight != right ) {
			node.setRight( ( BoxExpression ) newRight );
		}
		return node;
	}

	public BoxNode visit( BoxBinaryOperation node ) {
		BoxExpression	left	= node.getLeft();
		BoxNode			newLeft	= left.accept( this );
		if ( newLeft != left ) {
			node.setLeft( ( BoxExpression ) newLeft );
		}
		BoxExpression	right		= node.getRight();
		BoxNode			newRight	= right.accept( this );
		if ( newRight != right ) {
			node.setRight( ( BoxExpression ) newRight );
		}
		return node;
	}

	public BoxNode visit( BoxBooleanLiteral node ) {
		return node;
	}

	public BoxNode visit( BoxClosure node ) {
		for ( int i = 0; i < node.getArgs().size(); i++ ) {
			BoxArgumentDeclaration	arg		= node.getArgs().get( i );
			BoxNode					newArg	= arg.accept( this );
			if ( newArg != arg ) {
				node.replaceChildren( newArg, arg );
				node.getArgs().set( i, ( BoxArgumentDeclaration ) newArg );
			}
		}
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxAnnotation	annotation		= node.getAnnotations().get( i );
			BoxNode			newAnnotation	= annotation.accept( this );
			if ( newAnnotation != annotation ) {
				node.replaceChildren( newAnnotation, annotation );
				node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxComparisonOperation node ) {
		BoxExpression	left	= node.getLeft();
		BoxNode			newLeft	= left.accept( this );
		if ( newLeft != left ) {
			node.setLeft( ( BoxExpression ) newLeft );
		}
		BoxExpression	right		= node.getRight();
		BoxNode			newRight	= right.accept( this );
		if ( newRight != right ) {
			node.setRight( ( BoxExpression ) newRight );
		}
		return node;
	}

	public BoxNode visit( BoxDecimalLiteral node ) {
		return node;
	}

	public BoxNode visit( BoxDotAccess node ) {
		BoxExpression	context		= node.getContext();
		BoxNode			newContext	= context.accept( this );
		if ( newContext != context ) {
			node.setContext( ( BoxExpression ) newContext );
		}
		BoxExpression	access		= node.getAccess();
		BoxNode			newAccess	= access.accept( this );
		if ( newAccess != access ) {
			node.setAccess( ( BoxExpression ) newAccess );
		}
		return node;
	}

	public BoxNode visit( BoxExpressionInvocation node ) {
		BoxExpression expr = node.getExpr();
		if ( expr != null ) {
			BoxNode newExpr = expr.accept( this );
			if ( newExpr != expr ) {
				node.setExpr( ( BoxExpression ) newExpr );
			}
		}
		for ( int i = 0; i < node.getArguments().size(); i++ ) {
			BoxArgument	argument	= node.getArguments().get( i );
			BoxNode		newArgument	= argument.accept( this );
			if ( newArgument != argument ) {
				node.replaceChildren( newArgument, argument );
				node.getArguments().set( i, ( BoxArgument ) newArgument );
			}
		}

		return node;
	}

	public BoxNode visit( BoxFQN node ) {
		return node;
	}

	public BoxNode visit( BoxFunctionInvocation node ) {
		for ( int i = 0; i < node.getArguments().size(); i++ ) {
			BoxArgument	argument	= node.getArguments().get( i );
			BoxNode		newArgument	= argument.accept( this );
			if ( newArgument != argument ) {
				node.replaceChildren( newArgument, argument );
				node.getArguments().set( i, ( BoxArgument ) newArgument );
			}
		}
		return node;
	}

	public BoxNode visit( BoxIdentifier node ) {
		return node;
	}

	public BoxNode visit( BoxIntegerLiteral node ) {
		return node;
	}

	public BoxNode visit( BoxLambda node ) {
		for ( int i = 0; i < node.getArgs().size(); i++ ) {
			BoxArgumentDeclaration	arg		= node.getArgs().get( i );
			BoxNode					newArg	= arg.accept( this );
			if ( newArg != arg ) {
				node.replaceChildren( newArg, arg );
				node.getArgs().set( i, ( BoxArgumentDeclaration ) newArg );
			}
		}
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxAnnotation	annotation		= node.getAnnotations().get( i );
			BoxNode			newAnnotation	= annotation.accept( this );
			if ( newAnnotation != annotation ) {
				node.replaceChildren( newAnnotation, annotation );
				node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxMethodInvocation node ) {
		BoxExpression	name	= node.getName();
		BoxNode			newName	= name.accept( this );
		if ( newName != name ) {
			node.setName( ( BoxExpression ) newName );
		}
		for ( int i = 0; i < node.getArguments().size(); i++ ) {
			BoxArgument	argument	= node.getArguments().get( i );
			BoxNode		newArgument	= argument.accept( this );
			if ( newArgument != argument ) {
				node.replaceChildren( newArgument, argument );
				node.getArguments().set( i, ( BoxArgument ) newArgument );
			}
		}
		BoxExpression	obj		= node.getObj();
		BoxNode			newObj	= obj.accept( this );
		if ( newObj != obj ) {
			node.setObj( ( BoxExpression ) newObj );
		}
		return node;
	}

	public BoxNode visit( BoxNegateOperation node ) {
		BoxExpression	expr	= node.getExpr();
		BoxNode			newExpr	= expr.accept( this );
		if ( newExpr != expr ) {
			node.setExpr( ( BoxExpression ) newExpr );
		}
		return node;
	}

	public BoxNode visit( BoxNew node ) {
		BoxExpression expression = node.getExpression();
		if ( expression != null ) {
			BoxNode newExpr = expression.accept( this );
			if ( newExpr != expression ) {
				node.setExpression( ( BoxExpression ) newExpr );
			}
		}
		BoxIdentifier prefix = node.getPrefix();
		if ( prefix != null ) {
			BoxNode newPrefix = prefix.accept( this );
			if ( newPrefix != prefix ) {
				node.setPrefix( ( BoxIdentifier ) newPrefix );
			}
		}
		for ( int i = 0; i < node.getArguments().size(); i++ ) {
			BoxArgument	argument	= node.getArguments().get( i );
			BoxNode		newArgument	= argument.accept( this );
			if ( newArgument != argument ) {
				node.replaceChildren( newArgument, argument );
				node.getArguments().set( i, ( BoxArgument ) newArgument );
			}
		}
		return node;
	}

	public BoxNode visit( BoxNull node ) {
		return node;
	}

	public BoxNode visit( BoxParenthesis node ) {
		BoxExpression	expression	= node.getExpression();
		BoxNode			newExpr		= expression.accept( this );
		if ( newExpr != expression ) {
			node.setExpression( ( BoxExpression ) newExpr );
		}
		return node;
	}

	public BoxNode visit( BoxScope node ) {
		return node;
	}

	public BoxNode visit( BoxStringConcat node ) {
		for ( int i = 0; i < node.getValues().size(); i++ ) {
			BoxExpression	value		= node.getValues().get( i );
			BoxNode			newValue	= value.accept( this );
			if ( newValue != value ) {
				node.replaceChildren( newValue, value );
				node.getValues().set( i, ( BoxExpression ) newValue );
			}
		}
		return node;
	}

	public BoxNode visit( BoxStringInterpolation node ) {
		for ( int i = 0; i < node.getValues().size(); i++ ) {
			BoxExpression	value		= node.getValues().get( i );
			BoxNode			newValue	= value.accept( this );
			if ( newValue != value ) {
				node.replaceChildren( newValue, value );
				node.getValues().set( i, ( BoxExpression ) newValue );
			}
		}
		return node;
	}

	public BoxNode visit( BoxStringLiteral node ) {
		return node;
	}

	public BoxNode visit( BoxStructLiteral node ) {
		for ( int i = 0; i < node.getValues().size(); i++ ) {
			BoxExpression	value		= node.getValues().get( i );
			BoxNode			newValue	= value.accept( this );
			if ( newValue != value ) {
				node.replaceChildren( newValue, value );
				node.getValues().set( i, ( BoxExpression ) newValue );
			}
		}
		return node;
	}

	public BoxNode visit( BoxTernaryOperation node ) {
		BoxExpression	condition	= node.getCondition();
		BoxNode			newCond		= condition.accept( this );
		if ( newCond != condition ) {
			node.setCondition( ( BoxExpression ) newCond );
		}
		BoxExpression	whenTrue	= node.getWhenTrue();
		BoxNode			newTrue		= whenTrue.accept( this );
		if ( newTrue != whenTrue ) {
			node.setWhenTrue( ( BoxExpression ) newTrue );
		}
		BoxExpression	whenFalse	= node.getWhenFalse();
		BoxNode			newFalse	= whenFalse.accept( this );
		if ( newFalse != whenFalse ) {
			node.setWhenFalse( ( BoxExpression ) newFalse );
		}
		return node;
	}

	public BoxNode visit( BoxUnaryOperation node ) {
		BoxExpression	expr	= node.getExpr();
		BoxNode			newExpr	= expr.accept( this );
		if ( newExpr != expr ) {
			node.setExpr( ( BoxExpression ) newExpr );
		}
		return node;
	}

	public BoxNode visit( BoxAnnotation node ) {
		BoxFQN	key		= node.getKey();
		BoxNode	newKey	= key.accept( this );
		if ( newKey != key ) {
			node.setKey( ( BoxFQN ) newKey );
		}
		BoxExpression value = node.getValue();
		if ( value != null ) {
			BoxNode newValue = value.accept( this );
			if ( newValue != value ) {
				node.setValue( ( BoxExpression ) newValue );
			}
		}
		return node;
	}

	public BoxNode visit( BoxArgumentDeclaration node ) {
		BoxExpression value = node.getValue();
		if ( value != null ) {
			BoxNode newValue = value.accept( this );
			if ( newValue != value ) {
				node.setValue( ( BoxExpression ) newValue );
			}
		}
		if ( node.getAnnotations() != null ) {
			for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
				BoxAnnotation	annotation		= node.getAnnotations().get( i );
				BoxNode			newAnnotation	= annotation.accept( this );
				if ( newAnnotation != annotation ) {
					node.replaceChildren( newAnnotation, annotation );
					node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
				}
			}
		}
		if ( node.getDocumentation() != null ) {
			for ( int i = 0; i < node.getDocumentation().size(); i++ ) {
				BoxDocumentationAnnotation	documentation		= node.getDocumentation().get( i );
				BoxNode						newDocumentation	= documentation.accept( this );
				if ( newDocumentation != documentation ) {
					node.replaceChildren( newDocumentation, documentation );
					node.getDocumentation().set( i, ( BoxDocumentationAnnotation ) newDocumentation );
				}
			}
		}
		return node;
	}

	public BoxNode visit( BoxAssert node ) {
		BoxExpression	expression	= node.getExpression();
		BoxNode			newExpr		= expression.accept( this );
		if ( newExpr != expression ) {
			node.setExpression( ( BoxExpression ) newExpr );
		}
		return node;
	}

	public BoxNode visit( BoxBreak node ) {
		return node;
	}

	public BoxNode visit( BoxContinue node ) {
		return node;
	}

	public BoxNode visit( BoxDo node ) {
		BoxExpression	condition	= node.getCondition();
		BoxNode			newCond		= condition.accept( this );
		if ( newCond != condition ) {
			node.setCondition( ( BoxExpression ) newCond );
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxDocumentationAnnotation node ) {
		BoxFQN	key		= node.getKey();
		BoxNode	newKey	= key.accept( this );
		if ( newKey != key ) {
			node.setKey( ( BoxFQN ) newKey );
		}
		BoxExpression	value		= node.getValue();
		BoxNode			newValue	= value.accept( this );
		if ( newValue != value ) {
			node.setValue( ( BoxExpression ) newValue );
		}
		return node;
	}

	public BoxNode visit( BoxExpressionStatement node ) {
		BoxExpression	expression	= node.getExpression();
		BoxNode			newExpr		= expression.accept( this );
		if ( newExpr != expression ) {
			node.setExpression( ( BoxExpression ) newExpr );
		}
		return node;
	}

	public BoxNode visit( BoxForIn node ) {
		BoxExpression	variable	= node.getVariable();
		BoxNode			newVar		= variable.accept( this );
		if ( newVar != variable ) {
			node.setVariable( ( BoxExpression ) newVar );
		}
		BoxExpression	expression	= node.getExpression();
		BoxNode			newExpr		= expression.accept( this );
		if ( newExpr != expression ) {
			node.setExpression( ( BoxExpression ) newExpr );
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxForIndex node ) {
		BoxExpression initializer = node.getInitializer();
		if ( initializer != null ) {
			BoxNode newInit = initializer.accept( this );
			if ( newInit != initializer ) {
				node.setInitializer( ( BoxExpression ) newInit );
			}
		}
		BoxExpression condition = node.getCondition();
		if ( condition != null ) {
			BoxNode newCond = condition.accept( this );
			if ( newCond != condition ) {
				node.setCondition( ( BoxExpression ) newCond );
			}
		}
		BoxExpression step = node.getStep();
		if ( step != null ) {
			BoxNode newStep = step.accept( this );
			if ( newStep != step ) {
				node.setStep( ( BoxExpression ) newStep );
			}
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxFunctionDeclaration node ) {
		for ( int i = 0; i < node.getArgs().size(); i++ ) {
			BoxArgumentDeclaration	arg		= node.getArgs().get( i );
			BoxNode					newArg	= arg.accept( this );
			if ( newArg != arg ) {
				node.replaceChildren( newArg, arg );
				node.getArgs().set( i, ( BoxArgumentDeclaration ) newArg );
			}
		}
		BoxReturnType type = node.getType();
		if ( type != null ) {
			BoxNode newType = type.accept( this );
			if ( newType != type ) {
				node.setType( ( BoxReturnType ) newType );
			}
		}
		if ( node.getBody() != null ) {
			handleStatements( node.getBody(), node );
		}
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxAnnotation	annotation		= node.getAnnotations().get( i );
			BoxNode			newAnnotation	= annotation.accept( this );
			if ( newAnnotation != annotation ) {
				node.replaceChildren( newAnnotation, annotation );
				node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		for ( int i = 0; i < node.getDocumentation().size(); i++ ) {
			BoxDocumentationAnnotation	documentation		= node.getDocumentation().get( i );
			BoxNode						newDocumentation	= documentation.accept( this );
			if ( newDocumentation != documentation ) {
				node.replaceChildren( newDocumentation, documentation );
				node.getDocumentation().set( i, ( BoxDocumentationAnnotation ) newDocumentation );
			}
		}
		return node;
	}

	public BoxNode visit( BoxIfElse node ) {
		BoxExpression	condition	= node.getCondition();
		BoxNode			newCond		= condition.accept( this );
		if ( newCond != condition ) {
			node.setCondition( ( BoxExpression ) newCond );
		}
		handleStatements( node.getThenBody(), node );
		handleStatements( node.getElseBody(), node );
		return node;
	}

	public BoxNode visit( BoxImport node ) {
		BoxExpression expression = node.getExpression();
		if ( expression != null ) {
			BoxNode newExpr = expression.accept( this );
			if ( newExpr != expression ) {
				node.setExpression( ( BoxExpression ) newExpr );
			}
		}
		BoxIdentifier alias = node.getAlias();
		if ( alias != null ) {
			BoxNode newAlias = alias.accept( this );
			if ( newAlias != alias ) {
				node.setAlias( ( BoxIdentifier ) newAlias );
			}
		}
		return node;
	}

	public BoxNode visit( BoxParam node ) {
		BoxExpression	variable	= node.getVariable();
		BoxNode			newVar		= variable.accept( this );
		if ( newVar != variable ) {
			node.setVariable( ( BoxExpression ) newVar );
		}
		BoxExpression type = node.getType();
		if ( type != null ) {
			BoxNode newType = type.accept( this );
			if ( newType != type ) {
				node.setType( ( BoxExpression ) newType );
			}
		}
		BoxExpression defaultValue = node.getDefaultValue();
		if ( defaultValue != null ) {
			BoxNode newDefault = defaultValue.accept( this );
			if ( newDefault != defaultValue ) {
				node.setDefaultValue( ( BoxExpression ) newDefault );
			}
		}
		return node;
	}

	public BoxNode visit( BoxProperty node ) {
		for ( int i = 0; i < node.getAnnotations().size(); i++ ) {
			BoxAnnotation	annotation		= node.getAnnotations().get( i );
			BoxNode			newAnnotation	= annotation.accept( this );
			if ( newAnnotation != annotation ) {
				node.replaceChildren( newAnnotation, annotation );
				node.getAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		for ( int i = 0; i < node.getPostAnnotations().size(); i++ ) {
			BoxAnnotation	annotation		= node.getPostAnnotations().get( i );
			BoxNode			newAnnotation	= annotation.accept( this );
			if ( newAnnotation != annotation ) {
				node.replaceChildren( newAnnotation, annotation );
				node.getPostAnnotations().set( i, ( BoxAnnotation ) newAnnotation );
			}
		}
		for ( int i = 0; i < node.getDocumentation().size(); i++ ) {
			BoxDocumentationAnnotation	documentation		= node.getDocumentation().get( i );
			BoxNode						newDocumentation	= documentation.accept( this );
			if ( newDocumentation != documentation ) {
				node.replaceChildren( newDocumentation, documentation );
				node.getDocumentation().set( i, ( BoxDocumentationAnnotation ) newDocumentation );
			}
		}
		return node;
	}

	public BoxNode visit( BoxRethrow node ) {
		return node;
	}

	public BoxNode visit( BoxReturn node ) {
		BoxExpression expression = node.getExpression();
		if ( expression != null ) {
			BoxNode newExpr = expression.accept( this );
			if ( newExpr != expression ) {
				node.setExpression( ( BoxExpression ) newExpr );
			}
		}
		return node;
	}

	public BoxNode visit( BoxReturnType node ) {
		return node;
	}

	public BoxNode visit( BoxSwitch node ) {
		BoxExpression	condition	= node.getCondition();
		BoxNode			newCond		= condition.accept( this );
		if ( newCond != condition ) {
			node.setCondition( ( BoxExpression ) newCond );
		}
		for ( int i = 0; i < node.getCases().size(); i++ ) {
			BoxSwitchCase	caseNode	= node.getCases().get( i );
			BoxNode			newCase		= caseNode.accept( this );
			if ( newCase != caseNode ) {
				node.replaceChildren( newCase, caseNode );
				node.getCases().set( i, ( BoxSwitchCase ) newCase );
			}
		}
		return node;
	}

	public BoxNode visit( BoxSwitchCase node ) {
		BoxExpression condition = node.getCondition();
		if ( condition != null ) {
			BoxNode newCond = condition.accept( this );
			if ( newCond != condition ) {
				node.setCondition( ( BoxExpression ) newCond );
			}
		}
		BoxExpression delimiter = node.getDelimiter();
		if ( delimiter != null ) {
			BoxNode newDelim = delimiter.accept( this );
			if ( newDelim != delimiter ) {
				node.setDelimiter( ( BoxExpression ) newDelim );
			}
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxThrow node ) {
		BoxExpression expression = node.getExpression();
		if ( expression != null ) {
			BoxNode newExpr = expression.accept( this );
			if ( newExpr != expression ) {
				node.setExpression( ( BoxExpression ) newExpr );
			}
		}
		BoxExpression type = node.getType();
		if ( type != null ) {
			BoxNode newType = type.accept( this );
			if ( newType != type ) {
				node.setType( ( BoxExpression ) newType );
			}
		}
		BoxExpression message = node.getMessage();
		if ( message != null ) {
			BoxNode newMsg = message.accept( this );
			if ( newMsg != message ) {
				node.setMessage( ( BoxExpression ) newMsg );
			}
		}
		BoxExpression detail = node.getDetail();
		if ( detail != null ) {
			BoxNode newDetail = detail.accept( this );
			if ( newDetail != detail ) {
				node.setDetail( ( BoxExpression ) newDetail );
			}
		}
		BoxExpression errorcode = node.getErrorCode();
		if ( errorcode != null ) {
			BoxNode newCode = errorcode.accept( this );
			if ( newCode != errorcode ) {
				node.setErrorCode( ( BoxExpression ) newCode );
			}
		}
		BoxExpression extendedinfo = node.getExtendedInfo();
		if ( extendedinfo != null ) {
			BoxNode newInfo = extendedinfo.accept( this );
			if ( newInfo != extendedinfo ) {
				node.setExtendedInfo( ( BoxExpression ) newInfo );
			}
		}
		return node;
	}

	public BoxNode visit( BoxTry node ) {
		handleStatements( node.getTryBody(), node );
		for ( int i = 0; i < node.getCatches().size(); i++ ) {
			BoxTryCatch	catchNode	= node.getCatches().get( i );
			BoxNode		newCatch	= catchNode.accept( this );
			if ( newCatch != catchNode ) {
				node.replaceChildren( newCatch, catchNode );
				node.getCatches().set( i, ( BoxTryCatch ) newCatch );
			}
		}
		handleStatements( node.getFinallyBody(), node );
		return node;
	}

	public BoxNode visit( BoxTryCatch node ) {
		BoxIdentifier	exception	= node.getException();
		BoxNode			newExc		= exception.accept( this );
		if ( newExc != exception ) {
			node.setException( ( BoxIdentifier ) newExc );
		}
		handleStatements( node.getCatchBody(), node );
		for ( int i = 0; i < node.getCatchTypes().size(); i++ ) {
			BoxExpression	type	= node.getCatchTypes().get( i );
			BoxNode			newType	= type.accept( this );
			if ( newType != type ) {
				node.replaceChildren( newType, type );
				node.getCatchTypes().set( i, ( BoxExpression ) newType );
			}
		}
		return node;
	}

	public BoxNode visit( BoxWhile node ) {
		BoxExpression	condition	= node.getCondition();
		BoxNode			newCond		= condition.accept( this );
		if ( newCond != condition ) {
			node.setCondition( ( BoxExpression ) newCond );
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	public BoxNode visit( BoxComponent node ) {
		if ( node.getAttributes() != null ) {
			for ( int i = 0; i < node.getAttributes().size(); i++ ) {
				BoxAnnotation	annotation		= node.getAttributes().get( i );
				BoxNode			newAnnotation	= annotation.accept( this );
				if ( newAnnotation != annotation ) {
					node.replaceChildren( newAnnotation, annotation );
					node.getAttributes().set( i, ( BoxAnnotation ) newAnnotation );
				}
			}
		}
		handleStatements( node.getBody(), node );
		return node;
	}

	private void handleStatements( List<BoxStatement> statements, BoxNode node ) {
		if ( statements == null ) {
			return;
		}
		for ( int i = 0; i < statements.size(); i++ ) {
			BoxStatement	statement		= statements.get( i );
			BoxNode			newStatement	= statement.accept( this );
			if ( newStatement != statement ) {
				node.replaceChildren( newStatement, statement );
				if ( newStatement != null ) {
					statements.set( i, ( BoxStatement ) newStatement );
				} else {
					statements.remove( i );
					i--;
				}
			}
		}
	}

}
