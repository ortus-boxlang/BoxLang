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

import java.util.List;
import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing a function/method argument definition
 */
public class BoxArgumentDeclaration extends BoxStatement {

	private final String	name;
	private final BoxExpr	value;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 */

	/**
	 * Creates the AST node
	 *
	 * @param name         parameter name
	 * @param defaultValue optional default value
	 * @param position     position of the statement in the source code
	 * @param sourceText   source code that originated the Node
	 */

	public BoxArgumentDeclaration( String name, BoxExpr defaultValue, List<BoxAnnotation> annotations, Position position, String sourceText ) {
		super( position, sourceText );
		this.name	= name;
		this.value	= defaultValue;
		if ( this.value != null ) {
			this.value.setParent( this );
		}

		// this.expression.setParent( this );
	}

	public String getName() {
		return name;
	}

	public BoxExpr getValue() {
		return value;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name );
		if ( value != null ) {
			map.put( "value", value.toMap() );
		} else {
			map.put( "value", null );
		}

		return map;
	}
}
