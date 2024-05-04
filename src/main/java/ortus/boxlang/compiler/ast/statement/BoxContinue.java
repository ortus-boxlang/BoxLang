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
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a continue statement
 */
public class BoxContinue extends BoxStatement {

	private String label;

	/**
	 * Creates the AST node
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxContinue( Position position, String sourceText ) {
		this( null, position, sourceText );
	}

	/**
	 * Creates the AST node
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxContinue( String label, Position position, String sourceText ) {
		super( position, sourceText );
		setLabel( label );
	}

	/**
	 * Gets the label of the continue statement
	 *
	 * @return the label of the continue statement
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label of the continue statement
	 *
	 * @param label the label of the continue statement
	 */
	public void setLabel( String label ) {
		this.label = label;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		if ( label != null ) {
			map.put( "label", label );
		} else {
			map.put( "label", null );
		}
		return map;
	}
}
