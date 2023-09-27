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
package ortus.boxlang.ast.expression;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.Position;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing a string literal value
 */
public class BoxStringInterpolation extends BoxExpr {

	private final List<BoxExpr> values;

	public List<BoxExpr> getValues() {
		return values;
	}

	/**
	 * Creates the AST node
	 *
	 * @param parts      List of expression to interpolate
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxStringInterpolation( List<BoxExpr> parts, Position position, String sourceText ) {
		super( position, sourceText );
		this.values = Collections.unmodifiableList( parts );
		this.values.forEach( arg -> arg.setParent( this ) );
	}

}
