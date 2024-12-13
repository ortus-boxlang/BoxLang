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
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Assigment as expression
 */
public class BoxAssignment extends BoxExpression {

	private BoxExpression				left;
	private BoxExpression				right;
	private BoxAssignmentOperator		op;
	private List<BoxAssignmentModifier>	modifiers;

	/**
	 * Constructor
	 *
	 * @param left       left side of the assignment
	 * @param right      right side of the assignment
	 * @param position   position of the expression in the source code
	 * @param sourceText source code of the expression
	 */
	public BoxAssignment( BoxExpression left, BoxAssignmentOperator op, BoxExpression right, List<BoxAssignmentModifier> modifiers, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setLeft( left );
		setRight( right );
		setOp( op );
		setModifiers( modifiers );
	}

	public BoxExpression getLeft() {
		return left;
	}

	public BoxExpression getRight() {
		return right;
	}

	public BoxAssignmentOperator getOp() {
		return op;
	}

	public List<BoxAssignmentModifier> getModifiers() {
		return modifiers;
	}

	public void setLeft( BoxExpression left ) {
		replaceChildren( this.left, left );
		this.left = left;
		this.left.setParent( this );
	}

	public void setRight( BoxExpression right ) {
		replaceChildren( this.right, right );
		this.right = right;
		if ( right != null ) {
			this.right.setParent( this );
		}
	}

	public void setOp( BoxAssignmentOperator op ) {
		this.op = op;
	}

	public void setModifiers( List<BoxAssignmentModifier> modifiers ) {
		this.modifiers = modifiers;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "modifiers", modifiers.stream().map( op -> enumToMap( op ) ).toList() );
		map.put( "left", left.toMap() );
		if ( op != null ) {
			map.put( "op", enumToMap( op ) );
		} else {
			map.put( "op", null );
		}
		if ( right != null ) {
			map.put( "right", right.toMap() );
		} else {
			map.put( "right", null );

		}
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
