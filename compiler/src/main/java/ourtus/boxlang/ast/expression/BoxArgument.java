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
import ourtus.boxlang.ast.Position;

/**
 * AST Node representing an argument.
 * Argument can have a name like: <code>a=10</code>
 */
public class BoxArgument extends BoxExpr {

	private BoxExpr name = null;
	private final BoxExpr value;

	public void setName(BoxExpr name) {
		this.name = name;
	}
	public BoxExpr getName() {
		return name;
	}

	public BoxExpr getValue() {
		return value;
	}

	/**
	 * Creates the AST node for an anonymous argument
	 * @param value expression representing the value of the argument
	 * @param position position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxArgument(BoxExpr value, Position position, String sourceText ) {
		super( position, sourceText );
		this.value =  value;
		this.value.setParent(this);
	}

	/**
	 * Creates the AST node for a named argument
	 * @param name expression representing the name of the argument
	 * @param value expression representing the value of the argument
	 * @param position position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxArgument(BoxExpr name,BoxExpr value, Position position, String sourceText ) {
		super( position, sourceText );
		this.name =  name;
		this.value =  value;
		this.name.setParent(this);
		this.value.setParent(this);
	}
}
