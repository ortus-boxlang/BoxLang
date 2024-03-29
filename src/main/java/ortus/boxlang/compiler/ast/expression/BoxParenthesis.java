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
 * AST Node representing and expression within parenthesis
 */
public class BoxParenthesis extends BoxExpression {

	private BoxExpression expression;

	/**
	 * Creates the AST node
	 *
	 * @param expression expression within the parenthesis
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxParenthesis( BoxExpression expression, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
	}

	public BoxExpression getExpression() {
		return expression;
	}

	void setExpression( BoxExpression expression ) {
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
}
