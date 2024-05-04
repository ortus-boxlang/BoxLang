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
	private String				label;

	/**
	 *
	 * @param initializer
	 * @param condition
	 * @param body
	 * @param position
	 * @param sourceText
	 */
	public BoxForIndex( String label, BoxExpression initializer, BoxExpression condition, BoxExpression step, List<BoxStatement> body, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setInitializer( initializer );
		setCondition( condition );
		setStep( step );
		setBody( body );
		setLabel( label );
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
		if ( this.initializer != null ) {
			this.initializer.setParent( this );
		}
	}

	public void setCondition( BoxExpression condition ) {
		replaceChildren( this.condition, condition );
		this.condition = condition;
		if ( this.condition != null ) {
			this.condition.setParent( this );
		}
	}

	public void setStep( BoxExpression step ) {
		replaceChildren( this.step, step );
		this.step = step;
		if ( this.step != null ) {
			this.step.setParent( this );
		}
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	/**
	 * Gets the label of the statement
	 *
	 * @return the label of the statement
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label of the statement
	 *
	 * @param label the label of the statement
	 */
	public void setLabel( String label ) {
		this.label = label;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		if ( initializer != null ) {
			map.put( "initializer", initializer.toMap() );
		} else {
			map.put( "initializer", null );
		}
		if ( condition != null ) {
			map.put( "condition", condition.toMap() );
		} else {
			map.put( "condition", null );
		}
		if ( step != null ) {
			map.put( "step", step.toMap() );
		} else {
			map.put( "step", null );
		}
		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		if ( label != null ) {
			map.put( "label", label );
		} else {
			map.put( "label", null );
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
