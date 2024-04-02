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
package ortus.boxlang.compiler.ast.statement;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Represent a javadoc style documentation
 */
public class BoxDocumentationAnnotation extends BoxNode {

	private BoxFQN			key;
	private BoxExpression	value;

	/**
	 * Creates a Documentation AST node
	 *
	 * @param key        fqn of the documentation
	 * @param value      expression representing the value
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 *
	 */
	public BoxDocumentationAnnotation( BoxFQN key, BoxExpression value, Position position, String sourceText ) {
		super( position, sourceText );
		setKey( key );
		setValue( value );
	}

	public BoxFQN getKey() {
		return key;
	}

	public BoxExpression getValue() {
		return value;
	}

	public void setKey( BoxFQN key ) {
		replaceChildren( this.key, key );
		this.key = key;
		this.key.setParent( this );
	}

	public void setValue( BoxExpression value ) {
		replaceChildren( this.value, value );
		this.value = value;
		this.value.setParent( this );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "key", key.toMap() );
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
