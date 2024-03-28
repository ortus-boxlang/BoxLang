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
package ortus.boxlang.compiler.ast.expression;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.Position;

/**
 * AST Node representing a unary operator
 */
public class BoxUnaryOperation extends BoxExpression {

	private BoxExpression		expr;
	private BoxUnaryOperator	operator;

	/**
	 *
	 * @param expr       expression
	 * @param operator   operator to apply
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxUnaryOperation( BoxExpression expr, BoxUnaryOperator operator, Position position, String sourceText ) {
		super( position, sourceText );
		setExpr( expr );
		setOperator( operator );
	}

	public BoxExpression getExpr() {
		return expr;
	}

	public BoxUnaryOperator getOperator() {
		return operator;
	}

	void setExpr( BoxExpression expr ) {
		replaceChildren( this.expr, expr );
		this.expr = expr;
		this.expr.setParent( this );
	}

	void setOperator( BoxUnaryOperator operator ) {
		this.operator = operator;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "expr", expr.toMap() );
		map.put( "operator", enumToMap( operator ) );
		return map;
	}

}
