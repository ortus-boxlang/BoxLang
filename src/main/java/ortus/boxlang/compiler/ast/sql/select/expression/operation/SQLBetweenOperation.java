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

import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQCompare;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * Abstract Node class representing SQL BETWEEN operation
 */
public class SQLBetweenOperation extends SQLExpression {

	private SQLExpression	expression;

	private SQLExpression	left;

	private SQLExpression	right;

	private boolean			not;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLBetweenOperation( SQLExpression expression, SQLExpression left, SQLExpression right, boolean not, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setLeft( left );
		setRight( right );
		setNot( not );
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
	 * Get the not
	 */
	public boolean isNot() {
		return not;
	}

	/**
	 * Set the not
	 */
	public void setNot( boolean not ) {
		this.not = not;
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( QoQSelectExecution QoQExec ) {
		return true;
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQSelectExecution QoQExec, int[] intersection ) {
		Object	leftValue		= left.evaluate( QoQExec, intersection );
		Object	rightValue		= right.evaluate( QoQExec, intersection );
		Object	expressionValue	= expression.evaluate( QoQExec, intersection );
		// The ^ not inverses the result if the not flag is true
		return doBetween( left.getType( QoQExec ), leftValue, rightValue, expressionValue ) ^ not;
	}

	/**
	 * Evaluate the expression aginst a partition of data
	 */
	public Object evaluateAggregate( QoQSelectExecution QoQExec, List<int[]> intersections ) {
		if ( intersections.isEmpty() ) {
			return false;
		}
		Object	leftValue		= left.evaluateAggregate( QoQExec, intersections );
		Object	rightValue		= right.evaluateAggregate( QoQExec, intersections );
		Object	expressionValue	= expression.evaluateAggregate( QoQExec, intersections );
		// The ^ not inverses the result if the not flag is true
		return doBetween( left.getType( QoQExec ), leftValue, rightValue, expressionValue ) ^ not;
	}

	/**
	 * Helper for evaluating an expression as a number
	 * 
	 * @param left  the left operand value
	 * @param right the right operand value
	 * @param value the value to check if it's between the left and right operands
	 * 
	 * @return true if the value is between the left and right operands
	 */
	private boolean doBetween( QueryColumnType type, Object left, Object right, Object value ) {
		int result = QoQCompare.invoke( type, left, value );
		if ( result == 1 ) {
			return false;
		}
		result = QoQCompare.invoke( type, value, right );
		return result != 1;
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
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
