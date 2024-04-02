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
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
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
import ortus.boxlang.compiler.ast.expression.BoxNewOperation;
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
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;

/**
 * Base class for the BoxLang AST Nodes
 */
public abstract class VoidBoxVisitor {

	/**
	 * Constructor
	 */
	protected VoidBoxVisitor() {
	}

	private void visitChildren( BoxNode node ) {
		for ( BoxNode child : node.getChildren() ) {
			child.accept( this );
		}
	}

	public void visit( BoxScript node ) {
		visitChildren( node );
	}

	public void visit( BoxBufferOutput node ) {
		visitChildren( node );
	}

	public void visit( BoxClass node ) {
		visitChildren( node );
	}

	public void visit( BoxDocumentation node ) {
		visitChildren( node );
	}

	public void visit( BoxScriptIsland node ) {
		visitChildren( node );
	}

	public void visit( BoxTemplate node ) {
		visitChildren( node );
	}

	public void visit( BoxTemplateIsland node ) {
		visitChildren( node );
	}

	public void visit( BoxArgument node ) {
		visitChildren( node );
	}

	public void visit( BoxArrayAccess node ) {
		visitChildren( node );
	}

	public void visit( BoxArrayLiteral node ) {
		visitChildren( node );
	}

	public void visit( BoxAssignment node ) {
		visitChildren( node );
	}

	public void visit( BoxBinaryOperation node ) {
		visitChildren( node );
	}

	public void visit( BoxBooleanLiteral node ) {
		visitChildren( node );
	}

	public void visit( BoxClosure node ) {
		visitChildren( node );
	}

	public void visit( BoxComparisonOperation node ) {
		visitChildren( node );
	}

	public void visit( BoxDecimalLiteral node ) {
		visitChildren( node );
	}

	public void visit( BoxDotAccess node ) {
		visitChildren( node );
	}

	public void visit( BoxExpressionInvocation node ) {
		visitChildren( node );
	}

	public void visit( BoxFQN node ) {
		visitChildren( node );
	}

	public void visit( BoxFunctionInvocation node ) {
		visitChildren( node );
	}

	public void visit( BoxIdentifier node ) {
		visitChildren( node );
	}

	public void visit( BoxIntegerLiteral node ) {
		visitChildren( node );
	}

	public void visit( BoxLambda node ) {
		visitChildren( node );
	}

	public void visit( BoxMethodInvocation node ) {
		visitChildren( node );
	}

	public void visit( BoxNegateOperation node ) {
		visitChildren( node );
	}

	public void visit( BoxNewOperation node ) {
		visitChildren( node );
	}

	public void visit( BoxNull node ) {
		visitChildren( node );
	}

	public void visit( BoxParenthesis node ) {
		visitChildren( node );
	}

	public void visit( BoxScope node ) {
		visitChildren( node );
	}

	public void visit( BoxStringConcat node ) {
		visitChildren( node );
	}

	public void visit( BoxStringInterpolation node ) {
		visitChildren( node );
	}

	public void visit( BoxStringLiteral node ) {
		visitChildren( node );
	}

	public void visit( BoxStructLiteral node ) {
		visitChildren( node );
	}

	public void visit( BoxTernaryOperation node ) {
		visitChildren( node );
	}

	public void visit( BoxUnaryOperation node ) {
		visitChildren( node );
	}

	public void visit( BoxAnnotation node ) {
		visitChildren( node );
	}

	public void visit( BoxArgumentDeclaration node ) {
		visitChildren( node );
	}

	public void visit( BoxAssert node ) {
		visitChildren( node );
	}

	public void visit( BoxBreak node ) {
		visitChildren( node );
	}

	public void visit( BoxContinue node ) {
		visitChildren( node );
	}

	public void visit( BoxDo node ) {
		visitChildren( node );
	}

	public void visit( BoxDocumentationAnnotation node ) {
		visitChildren( node );
	}

	public void visit( BoxExpressionStatement node ) {
		visitChildren( node );
	}

	public void visit( BoxForIn node ) {
		visitChildren( node );
	}

	public void visit( BoxForIndex node ) {
		visitChildren( node );
	}

	public void visit( BoxFunctionDeclaration node ) {
		visitChildren( node );
	}

	public void visit( BoxIfElse node ) {
		visitChildren( node );
	}

	public void visit( BoxImport node ) {
		visitChildren( node );
	}

	public void visit( BoxNew node ) {
		visitChildren( node );
	}

	public void visit( BoxParam node ) {
		visitChildren( node );
	}

	public void visit( BoxProperty node ) {
		visitChildren( node );
	}

	public void visit( BoxRethrow node ) {
		visitChildren( node );
	}

	public void visit( BoxReturn node ) {
		visitChildren( node );
	}

	public void visit( BoxReturnType node ) {
		visitChildren( node );
	}

	public void visit( BoxSwitch node ) {
		visitChildren( node );
	}

	public void visit( BoxSwitchCase node ) {
		visitChildren( node );
	}

	public void visit( BoxThrow node ) {
		visitChildren( node );
	}

	public void visit( BoxTry node ) {
		visitChildren( node );
	}

	public void visit( BoxTryCatch node ) {
		visitChildren( node );
	}

	public void visit( BoxWhile node ) {
		visitChildren( node );
	}

	public void visit( BoxComponent node ) {
		visitChildren( node );
	}

}
