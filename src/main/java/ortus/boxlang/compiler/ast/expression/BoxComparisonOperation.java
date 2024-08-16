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
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

public class BoxComparisonOperation extends BoxExpression {

	private BoxExpression			left;
	private BoxExpression			right;
	private BoxComparisonOperator	operator;

	/**
	 * Comparision
	 *
	 * @param left
	 * @param operator
	 * @param right
	 * @param position
	 * @param sourceText
	 */
	public BoxComparisonOperation( BoxExpression left, BoxComparisonOperator operator, BoxExpression right, Position position, String sourceText ) {
		super( position, sourceText );
		if ( operator == null ) {
			throw new IllegalArgumentException( "Operator cannot be null. Source: " + sourceText );
		}
		setLeft( left );
		setOperator( operator );
		setRight( right );
	}

	public BoxExpression getLeft() {
		return left;
	}

	public BoxExpression getRight() {
		return right;
	}

	public BoxComparisonOperator getOperator() {
		return operator;
	}

	public void setLeft( BoxExpression left ) {
		replaceChildren( this.left, left );
		this.left = left;
		this.left.setParent( this );
	}

	public void setRight( BoxExpression right ) {
		replaceChildren( this.right, right );
		this.right = right;
		this.right.setParent( this );
	}

	public void setOperator( BoxComparisonOperator operator ) {
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

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
