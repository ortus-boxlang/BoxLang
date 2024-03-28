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

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.Position;

/**
 * Abstract class representing Aceess Operations
 */
public abstract class BoxAccess extends BoxExpression {

	private BoxExpression	context;
	private boolean			safe;
	private BoxExpression	access;

	/**
	 * Creates the AST node
	 *
	 * @param context    expression representing the object
	 * @param access     expression after the dot
	 * @param safe       boolean save operation
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAccess( BoxExpression context, Boolean safe, BoxExpression access, Position position, String sourceText ) {
		super( position, sourceText );
		setContext( context );
		setAccess( access );
		setSafe( safe );
	}

	public BoxExpression getContext() {
		return context;
	}

	public void setContext( BoxExpression context ) {
		replaceChildren( this.context, context );
		this.context = context;
		this.context.setParent( this );
	}

	public BoxExpression getAccess() {
		return access;
	}

	public void setAccess( BoxExpression access ) {
		replaceChildren( this.access, access );
		this.access = access;
		this.access.setParent( this );
	}

	public Boolean isSafe() {
		return safe;
	}

	public void setSafe( Boolean safe ) {
		this.safe = safe;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "context", context.toMap() );
		map.put( "access", access.toMap() );
		map.put( "safe", safe );
		return map;
	}
}
