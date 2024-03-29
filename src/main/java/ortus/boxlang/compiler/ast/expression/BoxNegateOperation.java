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
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

public class BoxNegateOperation extends BoxExpression {

	private BoxExpression		expr;
	private BoxNegateOperator	operator;

	/**
	 * Negate (not)
	 *
	 * @param expr
	 * @param operator
	 * @param position
	 * @param sourceText
	 */
	public BoxNegateOperation( BoxExpression expr, BoxNegateOperator operator, Position position, String sourceText ) {
		super( position, sourceText );
		setExpr( expr );
		setOperator( operator );
	}

	public BoxExpression getExpr() {
		return expr;
	}

	public BoxNegateOperator getOperator() {
		return operator;
	}

	void setExpr( BoxExpression expr ) {
		replaceChildren( this.expr, expr );
		this.expr = expr;
		this.expr.setParent( this );
	}

	void setOperator( BoxNegateOperator operator ) {
		this.operator = operator;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "operator", enumToMap( operator ) );
		map.put( "expr", expr.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}
}
