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

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a param whose syntax won't fit in the generic component node
 * This is usually for the script syntax that uses an FQN for the variable name
 */
public class BoxParam extends BoxStatement {

	private BoxExpression	variable;
	private BoxExpression	type;
	private BoxExpression	defaultValue;

	/**
	 * Creates the AST node
	 *
	 * @param variable     The variable to be paramed
	 * @param type         The type of the variable
	 * @param defaultValue The default value of the variable
	 * @param position     position of the statement in the source code
	 * @param sourceText   source code that originated the Node
	 */
	public BoxParam( BoxExpression variable, BoxExpression type, BoxExpression defaultValue, Position position, String sourceText ) {
		super( position, sourceText );
		setVariable( variable );
		setType( type );
		setDefaultValue( defaultValue );
	}

	public BoxExpression getVariable() {
		return variable;
	}

	public BoxExpression getType() {
		return type;
	}

	public BoxExpression getDefaultValue() {
		return defaultValue;
	}

	void setVariable( BoxExpression variable ) {
		replaceChildren( this.variable, variable );
		this.variable = variable;
		this.variable.setParent( this );
	}

	void setType( BoxExpression type ) {
		replaceChildren( this.type, type );
		this.type = type;
		if ( type != null ) {
			this.type.setParent( this );
		}
	}

	void setDefaultValue( BoxExpression defaultValue ) {
		replaceChildren( this.defaultValue, defaultValue );
		this.defaultValue = defaultValue;
		if ( defaultValue != null ) {
			this.defaultValue.setParent( this );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "variable", variable.toMap() );
		map.put( "type", type == null ? null : type.toMap() );
		map.put( "defaultValue", defaultValue == null ? null : defaultValue.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}
}
