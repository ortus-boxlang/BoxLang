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
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;

/**
 * AST Node representing a switch case statement
 */
public class BoxSwitchCase extends BoxStatement {

	// condition == null is the default case
	private BoxExpression		condition;
	private BoxExpression		delimiter;
	private List<BoxStatement>	body;

	/**
	 * Creates the AST node
	 *
	 * @param condition  expression representing the condition to test, null for the default
	 * @param body       list of the statements to execute when the condition is true
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxSwitchCase( BoxExpression condition, BoxExpression delimiter, List<BoxStatement> body, Position position, String sourceText ) {
		super( position, sourceText );
		setCondition( condition );
		setDelimiter( delimiter );
		setBody( body );
	}

	public BoxExpression getCondition() {
		return condition;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public BoxExpression getDelimiter() {
		return delimiter;
	}

	void setCondition( BoxExpression condition ) {
		replaceChildren( this.condition, condition );
		this.condition = condition;
		if ( this.condition != null ) {
			this.condition.setParent( this );
		}
	}

	void setDelimiter( BoxExpression delimiter ) {
		replaceChildren( this.delimiter, delimiter );
		this.delimiter = delimiter;
		if ( this.delimiter != null ) {
			this.delimiter.setParent( this );
		}
	}

	void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		if ( condition != null ) {
			map.put( "condition", condition.toMap() );
		} else {
			map.put( "condition", null );
		}
		if ( delimiter != null ) {
			map.put( "delimiter", delimiter.toMap() );
		} else {
			map.put( "delimiter", null );
		}
		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		return map;
	}
}
