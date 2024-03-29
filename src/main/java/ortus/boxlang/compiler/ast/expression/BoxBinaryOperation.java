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

/**
 * AST Node representing access binary operation
 */
public class BoxBinaryOperation extends BoxExpression {

	private BoxExpression		left;
	private BoxExpression		right;
	private BoxBinaryOperator	operator;

	/**
	 * Creates the AST node
	 *
	 * @param left       left expression of binary operation
	 * @param operator   operator
	 * @param right      left expression of binary operation
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 *
	 * @see BoxBinaryOperator for the supported operators
	 */
	public BoxBinaryOperation( BoxExpression left, BoxBinaryOperator operator, BoxExpression right, Position position, String sourceText ) {
		super( position, sourceText );
		setLeft( left );
		setRight( right );
		setOperator( operator );
	}

	public BoxExpression getLeft() {
		return left;
	}

	public BoxExpression getRight() {
		return right;
	}

	public BoxBinaryOperator getOperator() {
		return operator;
	}

	void setLeft( BoxExpression left ) {
		replaceChildren( this.left, left );
		this.left = left;
		this.left.setParent( this );
	}

	void setRight( BoxExpression right ) {
		replaceChildren( this.right, right );
		this.right = right;
		this.right.setParent( this );
	}

	void setOperator( BoxBinaryOperator operator ) {
		this.operator = operator;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "left", left.toMap() );
		map.put( "operator", enumToMap( operator ) );
		map.put( "right", right.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

}
