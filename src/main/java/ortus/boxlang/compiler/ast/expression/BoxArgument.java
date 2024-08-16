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
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing an argument.
 * Argument can have a name like: <code>a=10</code>
 */
public class BoxArgument extends BoxExpression {

	private BoxExpression	name	= null;
	private BoxExpression	value;

	/**
	 * Creates the AST node for an anonymous argument
	 *
	 * @param value      expression representing the value of the argument
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxArgument( BoxExpression value, Position position, String sourceText ) {
		super( position, sourceText );
		setValue( value );
	}

	/**
	 * Creates the AST node for a named argument
	 *
	 * @param name       expression representing the name of the argument
	 * @param value      expression representing the value of the argument
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxArgument( BoxExpression name, BoxExpression value, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setValue( value );
	}

	public void setName( BoxExpression name ) {
		replaceChildren( this.name, name );
		this.name = name;
		if ( this.name != null ) {
			this.name.setParent( this );
		}
	}

	public void setValue( BoxExpression value ) {
		replaceChildren( this.value, value );
		this.value = value;
		this.value.setParent( this );
	}

	public BoxExpression getName() {
		return name;
	}

	public BoxExpression getValue() {
		return value;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		if ( name != null ) {
			map.put( "name", name.toMap() );
		} else {
			map.put( "name", null );
		}
		map.put( "value", value.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
