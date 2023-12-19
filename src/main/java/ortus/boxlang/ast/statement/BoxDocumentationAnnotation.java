/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxFQN;

/**
 * Represent a javadoc style documentation
 */
public class BoxDocumentationAnnotation extends BoxNode {

	private final BoxFQN	key;
	private final BoxExpr	value;

	/**
	 * Creates a Documentation AST node
	 *
	 * @param key        fqn of the documentation
	 * @param value      expression representing the value
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 *
	 */
	public BoxDocumentationAnnotation( BoxFQN key, BoxExpr value, Position position, String sourceText ) {
		super( position, sourceText );
		this.key = key;
		this.key.setParent( this );
		this.value = value;
		this.value.setParent( this );
	}

	public BoxFQN getKey() {
		return key;
	}

	public BoxExpr getValue() {
		return value;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "key", key.toMap() );
		map.put( "value", value.toMap() );
		return map;
	}
}
