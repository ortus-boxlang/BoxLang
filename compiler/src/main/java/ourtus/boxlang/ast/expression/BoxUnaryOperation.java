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
package ourtus.boxlang.ast.expression;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.Position;

public class BoxUnaryOperation extends BoxExpr {

	private final BoxExpr			expr;
	private final BoxBinaryOperator	operator;

	public BoxUnaryOperation( BoxExpr expr, BoxBinaryOperator operator, Position position, String sourceText ) {
		super( position, sourceText );
		this.expr		= expr;
		this.operator	= operator;
	}

	public BoxExpr getExpr() {
		return expr;
	}

	public BoxBinaryOperator getOperator() {
		return operator;
	}

}
