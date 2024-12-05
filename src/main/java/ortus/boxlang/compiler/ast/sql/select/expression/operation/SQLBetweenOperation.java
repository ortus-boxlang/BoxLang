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
package ortus.boxlang.compiler.ast.sql.select.expression.operation;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL BETWEEN operation
 */
public class SQLBetweenOperation extends SQLExpression {

	private SQLExpression	expression;

	private SQLExpression	left;

	private SQLExpression	right;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLBetweenOperation( SQLExpression expression, SQLExpression left, SQLExpression right, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setLeft( left );
		setRight( right );
	}

	/**
	 * Get the expression
	 */
	public SQLExpression getExpression() {
		return expression;
	}

	/**
	 * Set the expression
	 */
	public void setExpression( SQLExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		this.expression.setParent( this );
	}

	/**
	 * Get the left
	 */
	public SQLExpression getLeft() {
		return left;
	}

	/**
	 * Set the left
	 */
	public void setLeft( SQLExpression left ) {
		replaceChildren( this.left, left );
		this.left = left;
		this.left.setParent( this );
	}

	/**
	 * Get the right
	 */
	public SQLExpression getRight() {
		return right;
	}

	/**
	 * Set the right
	 */
	public void setRight( SQLExpression right ) {
		replaceChildren( this.right, right );
		this.right = right;
		this.right.setParent( this );
	}

	/**
	 * Check if the expression evaluates to a boolean value
	 */
	public boolean isBoolean() {
		return true;
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( Map<SQLTable, Query> tableLookup, int i ) {
		throw new BoxRuntimeException( "not implemented" );
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "expression", expression.toMap() );
		map.put( "left", left.toMap() );
		map.put( "right", right.toMap() );
		return map;
	}

}
