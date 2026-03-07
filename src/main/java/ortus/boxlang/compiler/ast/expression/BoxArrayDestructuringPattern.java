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
package ortus.boxlang.compiler.ast.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * An array destructuring pattern used on the left-hand side of assignments.
 */
public class BoxArrayDestructuringPattern extends BoxExpression {

	private List<BoxArrayDestructuringBinding> bindings;

	public BoxArrayDestructuringPattern( List<BoxArrayDestructuringBinding> bindings, Position position, String sourceText ) {
		super( position, sourceText );
		setBindings( bindings );
	}

	public List<BoxArrayDestructuringBinding> getBindings() {
		return bindings;
	}

	public void setBindings( List<BoxArrayDestructuringBinding> bindings ) {
		replaceChildren( this.bindings, bindings );
		this.bindings = bindings;
		this.bindings.forEach( binding -> binding.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "bindings", bindings.stream().map( BoxArrayDestructuringBinding::toMap ).collect( Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
