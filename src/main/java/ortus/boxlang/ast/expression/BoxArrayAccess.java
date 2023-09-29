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

/**
 * AST Node representing access with a square bracket like:
 * <code>variables['a']</code> or <code>a[10]</code>
 */
public class BoxArrayAccess extends BoxAccess {

	private BoxExpr	context;
	private BoxExpr	index;

	public BoxExpr getContext() {
		return context;
	}

	public void setContext( BoxExpr context ) {
		this.context = context;
	}

	public BoxExpr getIndex() {
		return index;
	}

	public void setIndex( BoxExpr index ) {
		this.index = index;
	}

	/**
	 * Creates the AST node
	 *
	 * @param context    expression representing the variable or a scope
	 * @param index      expression within the brackets
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxArrayAccess( BoxExpr context, BoxExpr index, Position position, String sourceText ) {
		super( position, sourceText );
		this.context	= context;
		this.index		= index;
		context.setParent( this );
		context.setParent( this );
	}

}
