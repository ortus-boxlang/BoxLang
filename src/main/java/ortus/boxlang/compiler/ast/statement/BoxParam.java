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

import ortus.boxlang.compiler.ast.BoxExpr;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;

/**
 * AST Node representing a param whose syntax won't fit in the generic component node
 * This is usually for the script syntax that uses an FQN for the variable name
 */
public class BoxParam extends BoxStatement {

	private final BoxExpr	variable;
	private final BoxExpr	type;
	private final BoxExpr	defaultValue;

	/**
	 * Creates the AST node
	 *
	 * @param variable     The variable to be paramed
	 * @param type         The type of the variable
	 * @param defaultValue The default value of the variable
	 * @param position     position of the statement in the source code
	 * @param sourceText   source code that originated the Node
	 */
	public BoxParam( BoxExpr variable, BoxExpr type, BoxExpr defaultValue, Position position, String sourceText ) {
		super( position, sourceText );
		this.variable = variable;
		this.variable.setParent( this );
		this.type = type;
		if ( type != null ) {
			this.type.setParent( this );
		}
		this.defaultValue = defaultValue;
		if ( defaultValue != null ) {
			this.defaultValue.setParent( this );
		}
	}

	public BoxExpr getVariable() {
		return variable;
	}

	public BoxExpr getType() {
		return type;
	}

	public BoxExpr getDefaultValue() {
		return defaultValue;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "variable", variable.toMap() );
		map.put( "type", type == null ? null : type.toMap() );
		map.put( "defaultValue", defaultValue == null ? null : defaultValue.toMap() );
		return map;
	}
}
