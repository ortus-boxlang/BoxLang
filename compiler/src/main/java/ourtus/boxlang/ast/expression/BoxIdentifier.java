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
package ourtus.boxlang.ast.expression;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.Named;
import ourtus.boxlang.ast.Position;

/**
 * AST Node representing a switch case statement
 */
public class BoxIdentifier extends BoxExpr implements Named {

	private final String name;

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Creates the AST node
	 * @param name name of the identifier
	 * @param position position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxIdentifier(String name, Position position, String sourceText ) {
		super( position, sourceText );
		this.name = name;
	}

}
