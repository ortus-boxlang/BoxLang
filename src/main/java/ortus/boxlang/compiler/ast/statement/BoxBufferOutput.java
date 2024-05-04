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
package ortus.boxlang.compiler.ast.statement;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing an Expression statement
 */
public class BoxBufferOutput extends BoxStatement {

	/**
	 * This will always be a BoxStringLiteral or a BoxStringInterpolation
	 */
	private BoxExpression expression;

	/**
	 * Creates the AST node
	 *
	 * @param expression an expression to be executed as a statement
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxBufferOutput( BoxExpression expression, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
	}

	public BoxExpression getExpression() {
		return expression;
	}

	public void setExpression( BoxExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		this.expression.setParent( this );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "expression", expression.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
