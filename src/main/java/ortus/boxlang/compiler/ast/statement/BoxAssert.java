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
package ortus.boxlang.compiler.ast.statement;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing an assert statement
 */
public class BoxAssert extends BoxStatement {

	private BoxExpression	expression;
	private BoxExpression	message;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAssert( BoxExpression expression, Position position, String sourceText ) {
		this( expression, null, position, sourceText );
	}

	/**
	 * Creates the AST node with an optional failure message
	 *
	 * @param expression argument expression to assert
	 * @param message    optional expression evaluated as the assertion failure message
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAssert( BoxExpression expression, BoxExpression message, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setMessage( message );
	}

	public BoxExpression getExpression() {
		return this.expression;
	}

	public void setExpression( BoxExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		this.expression.setParent( this );
	}

	public BoxExpression getMessage() {
		return this.message;
	}

	public void setMessage( BoxExpression message ) {
		replaceChildren( this.message, message );
		this.message = message;
		if ( this.message != null ) {
			this.message.setParent( this );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "expression", expression.toMap() );
		if ( this.message != null ) {
			map.put( "message", this.message.toMap() );
		} else {
			map.put( "message", null );
		}
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
