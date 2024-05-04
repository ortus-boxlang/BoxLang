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
 * AST Node representing a Do loop statement
 */
public class BoxDo extends BoxStatement {

	private BoxExpression		condition;
	private List<BoxStatement>	body;
	private String				label;

	/**
	 * Creates the AST node
	 *
	 * @param condition  the expression of the while statement
	 * @param body       list of the statements in the body if the loop
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxDo( String label, BoxExpression condition, List<BoxStatement> body, Position position, String sourceText ) {
		super( position, sourceText );
		setCondition( condition );
		setBody( body );
		setLabel( label );
	}

	public BoxExpression getCondition() {
		return condition;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public void setCondition( BoxExpression condition ) {
		replaceChildren( this.condition, condition );
		this.condition = condition;
		this.condition.setParent( this );
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

		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		map.put( "condition", condition.toMap() );
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
