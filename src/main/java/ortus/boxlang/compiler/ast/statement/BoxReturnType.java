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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a function return type
 */
public class BoxReturnType extends BoxNode {

	private BoxType	type;
	private String	fqn;

	/**
	 *
	 *
	 * @param type
	 * @param fqn
	 * @param position
	 * @param sourceText
	 */
	public BoxReturnType( BoxType type, String fqn, Position position, String sourceText ) {
		super( position, sourceText );
		setType( type );
		setFqn( fqn );
	}

	public BoxType getType() {
		return type;
	}

	public String getFqn() {
		return fqn;
	}

	public void setType( BoxType type ) {
		this.type = type;
	}

	public void setFqn( String fqn ) {
		this.fqn = fqn;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "type", enumToMap( type ) );
		map.put( "fqn", fqn );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
