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

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * An Adobe savecontent assignment. Retains the assignment target, operator,
 * modifiers and component body without rewriting the source into statements.
 */
public class BoxSaveContentAssignment extends BoxAssignment {

	private BoxComponent component;

	public BoxSaveContentAssignment( BoxExpression left, BoxAssignmentOperator op, List<BoxAssignmentModifier> modifiers,
	    BoxComponent component, Position position, String sourceText ) {
		super( left, op, null, modifiers, position, sourceText );
		setComponent( component );
	}

	public BoxComponent getComponent() {
		return this.component;
	}

	public void setComponent( BoxComponent component ) {
		replaceChildren( this.component, component );
		this.component = component;
		this.component.setParent( this );
	}

	@Override
	public Map<String, Object> toMap() {
		var map = super.toMap();
		map.put( "component", this.component.toMap() );
		return map;
	}

	@Override
	public void accept( VoidBoxVisitor visitor ) {
		visitor.visit( this );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor visitor ) {
		return visitor.visit( this );
	}
}
