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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a for statement like:
 * <code>for(variable;expression;expression) body</code>
 */
public class BoxForIndex extends BoxStatement {

	private BoxExpression		initializer;
	private BoxExpression		condition;
	private BoxExpression		step;
	private List<BoxStatement>	body;

	/**
	 *
	 * @param initializer
	 * @param condition
	 * @param body
	 * @param position
	 * @param sourceText
	 */
	public BoxForIndex( BoxExpression initializer, BoxExpression condition, BoxExpression step, List<BoxStatement> body, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setInitializer( initializer );
		setCondition( condition );
		setStep( step );
		setBody( body );
	}

	public BoxExpression getInitializer() {
		return initializer;
	}

	public BoxExpression getCondition() {
		return condition;
	}

	public BoxExpression getStep() {
		return step;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public void setInitializer( BoxExpression initializer ) {
		replaceChildren( this.initializer, initializer );
		this.initializer = initializer;
		this.initializer.setParent( this );
	}

	public void setCondition( BoxExpression condition ) {
		replaceChildren( this.condition, condition );
		this.condition = condition;
		this.condition.setParent( this );
	}

	public void setStep( BoxExpression step ) {
		replaceChildren( this.step, step );
		this.step = step;
		this.step.setParent( this );
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "initializer", initializer.toMap() );
		map.put( "condition", condition.toMap() );
		map.put( "step", step.toMap() );
		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
