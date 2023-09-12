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
package ourtus.boxlang.ast.statement;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.Position;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing a local declaration statement like:
 * <code>var a = b = "Hello"</code>
 */
public class BoxLocalDeclaration extends BoxStatement {

	private final List<BoxExpr>	identifiers;
	private BoxExpr				expression;

	/**
	 * Creates the AST node
	 *
	 * @param identifiers list of identifiers
	 * @param expression  expression representing the value to assign
	 * @param position    position of the statement in the source code
	 * @param sourceText  source code that originated the Node
	 */
	public BoxLocalDeclaration( List<BoxExpr> identifiers, BoxExpr expression, Position position, String sourceText ) {
		super( position, sourceText );
		this.expression = expression;
		this.expression.setParent( this );
		this.identifiers = Collections.unmodifiableList( identifiers );
		this.identifiers.forEach( id -> id.setParent( this ) );
	}

	public List<BoxExpr> getIdentifiers() {
		return identifiers;
	}

	public BoxExpr getExpression() {
		return expression;
	}

	public void setExpression( BoxExpr expression ) {
		this.expression = expression;
	}
}
