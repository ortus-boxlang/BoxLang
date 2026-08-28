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
package ortus.boxlang.compiler.ast.visitor;

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalBIFAccess;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalMemberAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

/**
 * Shares equal semantic strings within one AST without retaining parser input globally.
 */
public class SemanticStringCanonicalizer extends VoidBoxVisitor {

	private final Map<String, String> canonicalStrings = new HashMap<>();

	private SemanticStringCanonicalizer() {
	}

	/**
	 * Canonicalizes semantic strings retained by the supplied AST.
	 *
	 * @param root AST root
	 */
	public static void canonicalize( BoxNode root ) {
		if ( root != null ) {
			root.accept( new SemanticStringCanonicalizer() );
		}
	}

	private String canonicalize( String value ) {
		if ( value == null ) {
			return null;
		}
		String existing = this.canonicalStrings.putIfAbsent( value, value );
		return existing == null ? value : existing;
	}

	@Override
	public void visit( BoxIdentifier node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxScope node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxFQN node ) {
		node.setValue( canonicalize( node.getValue() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxFunctionInvocation node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxFunctionalBIFAccess node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxFunctionalMemberAccess node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxStringLiteral node ) {
		node.setValue( canonicalize( node.getValue() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxIntegerLiteral node ) {
		node.setValue( canonicalize( node.getValue() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxDecimalLiteral node ) {
		node.setValue( canonicalize( node.getValue() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxArgumentDeclaration node ) {
		node.setName( canonicalize( node.getName() ) );
		node.setType( canonicalize( node.getType() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxFunctionDeclaration node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxReturnType node ) {
		node.setFqn( canonicalize( node.getFqn() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxComponent node ) {
		node.setName( canonicalize( node.getName() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxBreak node ) {
		node.setLabel( canonicalize( node.getLabel() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxContinue node ) {
		node.setLabel( canonicalize( node.getLabel() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxDo node ) {
		node.setLabel( canonicalize( node.getLabel() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxForIn node ) {
		node.setLabel( canonicalize( node.getLabel() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxForIndex node ) {
		node.setLabel( canonicalize( node.getLabel() ) );
		super.visit( node );
	}

	@Override
	public void visit( BoxWhile node ) {
		node.setLabel( canonicalize( node.getLabel() ) );
		super.visit( node );
	}
}
