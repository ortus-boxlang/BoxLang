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
package ourtus.boxlang.ast;

import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.Position;

/**
 * Abstract class representing Expressions
 */
public abstract class BoxExpr extends BoxNode {
	/**
	 * Utility method to detect if an expression node is a terminal Literal
	 * @return true if it is false otherwise
	 */
	public boolean isLiteral() {
		return false;
	}
	/**
	 * Constructor
	 * @param position position of the expression in the source code
	 * @param sourceText source code of the expression
	 */
	public BoxExpr(Position position, String sourceText ) {
		super( position, sourceText );
	}
}
