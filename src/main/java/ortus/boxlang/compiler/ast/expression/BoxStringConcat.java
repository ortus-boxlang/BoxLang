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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.Position;

/**
 * AST Node representing a string literal value
 */
public class BoxStringConcat extends BoxExpression {

	private List<BoxExpression> values;

	/**
	 * Creates the AST node
	 *
	 * @param parts      List of expression to interpolate
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxStringConcat( List<BoxExpression> parts, Position position, String sourceText ) {
		super( position, sourceText );
		setValues( parts );
	}

	public List<BoxExpression> getValues() {
		return values;
	}

	void setValues( List<BoxExpression> values ) {
		replaceChildren( this.values, values );
		this.values = values;
		this.values.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "values", values.stream().map( BoxExpression::toMap ).collect( Collectors.toList() ) );
		return map;
	}

}
