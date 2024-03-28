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

import ortus.boxlang.compiler.ast.BoxExpr;
import ortus.boxlang.compiler.ast.Position;

public class BoxComparisonOperation extends BoxExpr {

	private final BoxExpr				left;
	private final BoxExpr				right;
	private final BoxComparisonOperator	operator;

	/**
	 * Comparision
	 *
	 * @param left
	 * @param operator
	 * @param right
	 * @param position
	 * @param sourceText
	 */
	public BoxComparisonOperation( BoxExpr left, BoxComparisonOperator operator, BoxExpr right, Position position, String sourceText ) {
		super( position, sourceText );
		this.left		= left;
		this.right		= right;
		this.operator	= operator;
		this.left.setParent( this );
		this.right.setParent( this );
	}

	public BoxExpr getLeft() {
		return left;
	}

	public BoxExpr getRight() {
		return right;
	}

	public BoxComparisonOperator getOperator() {
		return operator;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "left", left.toMap() );
		map.put( "operator", enumToMap( operator ) );
		map.put( "right", right.toMap() );
		return map;
	}
}
