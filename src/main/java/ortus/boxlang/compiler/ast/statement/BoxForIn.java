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
 * AST Node representing a for statement
 */
public class BoxForIn extends BoxStatement {

	private BoxExpression		variable;
	private BoxExpression		expression;
	private List<BoxStatement>	body;
	private Boolean				hasVar;

	/**
	 * Creates the AST node
	 *
	 * @param variable   for loop variable
	 * @param expression for loop collection
	 * @param body       list of the statement in the body of the loop
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxForIn( BoxExpression variable, BoxExpression expression, List<BoxStatement> body, Boolean hasVar, Position position, String sourceText ) {
		super( position, sourceText );
		setVariable( variable );
		setExpression( expression );
		setBody( body );
		setHasVar( hasVar );
	}

	public BoxExpression getVariable() {
		return variable;
	}

	public BoxExpression getExpression() {
		return expression;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public Boolean getHasVar() {
		return hasVar;
	}

	public void setVariable( BoxExpression variable ) {
		replaceChildren( this.variable, variable );
		this.variable = variable;
		this.variable.setParent( this );
	}

	public void setExpression( BoxExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		this.expression.setParent( this );
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	public void setHasVar( Boolean hasVar ) {
		this.hasVar = hasVar;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "hasVar", hasVar );
		map.put( "variable", variable.toMap() );
		map.put( "expression", expression.toMap() );
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
