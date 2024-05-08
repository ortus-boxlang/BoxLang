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
 * AST Node representing a if statement
 */
public class BoxIfElse extends BoxStatement {

	private BoxExpression	condition;
	private BoxStatement	thenBody;
	private BoxStatement	elseBody;

	/**
	 * Creates the AST node
	 *
	 * @param condition  expression representing the condition to test
	 * @param thenBody   list of the statements to execute when the condition is true
	 * @param elseBody   list of the statements foe the else, empty if the else body is not present
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxIfElse( BoxExpression condition, BoxStatement thenBody, BoxStatement elseBody, Position position, String sourceText ) {
		super( position, sourceText );
		setCondition( condition );
		setThenBody( thenBody );
		setElseBody( elseBody );
	}

	public BoxExpression getCondition() {
		return condition;
	}

	public BoxStatement getThenBody() {
		return thenBody;
	}

	public BoxStatement getElseBody() {
		return elseBody;
	}

	public void setCondition( BoxExpression condition ) {
		replaceChildren( this.condition, condition );
		this.condition = condition;
		this.condition.setParent( this );
	}

	public void setThenBody( BoxStatement thenBody ) {
		replaceChildren( this.thenBody, thenBody );
		this.thenBody = thenBody;
		this.thenBody.setParent( this );
	}

	public void setElseBody( BoxStatement elseBody ) {
		replaceChildren( this.elseBody, elseBody );
		this.elseBody = elseBody;
		if ( this.elseBody != null ) {
			this.elseBody.setParent( this );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "condition", condition.toMap() );
		map.put( "thenBody", thenBody.toMap() );
		if ( this.elseBody != null ) {
			map.put( "elseBody", elseBody.toMap() );
		} else {
			map.put( "elseBody", null );
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
