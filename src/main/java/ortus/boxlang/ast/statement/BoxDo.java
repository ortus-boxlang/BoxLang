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
package ortus.boxlang.ast.statement;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing a Do loop statement
 */
public class BoxDo extends BoxStatement {

	private final BoxExpr				condition;
	private final List<BoxStatement>	body;

	/**
	 * Creates the AST node
	 *
	 * @param condition  the expression of the while statement
	 * @param body       list of the statements in the body if the loop
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxDo( BoxExpr condition, List<BoxStatement> body, Position position, String sourceText ) {
		super( position, sourceText );
		this.condition = condition;
		this.condition.setParent( this );
		this.body = Collections.unmodifiableList( body );
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	public BoxExpr getCondition() {
		return condition;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		map.put( "condition", condition.toMap() );
		return map;
	}
}
