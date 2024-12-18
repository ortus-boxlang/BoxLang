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
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL unary operation
 */
public class SQLUnaryOperation extends SQLExpression {

	// NOT, ISNULL, ISNOTNULL
	private static final Set<SQLUnaryOperator>	booleanOperators	= Set.of( SQLUnaryOperator.NOT, SQLUnaryOperator.ISNULL, SQLUnaryOperator.ISNOTNULL );

	// math operators
	private static final Set<SQLUnaryOperator>	mathOperators		= Set.of( SQLUnaryOperator.PLUS, SQLUnaryOperator.MINUS );

	private SQLExpression						expression;

	private SQLUnaryOperator					operator;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLUnaryOperation( SQLExpression expression, SQLUnaryOperator operator, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setOperator( operator );
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
	 * Get the operator
	 */
	public SQLUnaryOperator getOperator() {
		return operator;
	}

	/**
	 * Set the operator
	 */
	public void setOperator( SQLUnaryOperator operator ) {
		this.operator = operator;
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( QoQSelectExecution QoQExec ) {
		return booleanOperators.contains( operator );
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQSelectExecution QoQExec ) {
		// If this is a boolean operation, then we're a bit
		if ( isBoolean( QoQExec ) ) {
			return QueryColumnType.BIT;
		}
		if ( mathOperators.contains( operator ) ) {
			return QueryColumnType.DOUBLE;
		}
		return QueryColumnType.OBJECT;
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( QoQSelectExecution QoQExec ) {
		return getType( QoQExec ) == QueryColumnType.DOUBLE;
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQSelectExecution QoQExec, int[] intersection ) {
		// Implement each unary operator
		switch ( operator ) {
			case ISNOTNULL :
				return expression.evaluate( QoQExec, intersection ) != null;
			case ISNULL :
				return expression.evaluate( QoQExec, intersection ) != null;
			case MINUS :
				ensureNumericOperand( QoQExec );
				return -evalAsNumber( expression, QoQExec, intersection );
			case NOT :
				ensureBooleanOperand( QoQExec );
				return ! ( ( boolean ) expression.evaluate( QoQExec, intersection ) );
			case PLUS :
				ensureNumericOperand( QoQExec );
				return expression.evaluate( QoQExec, intersection );
			default :
				throw new BoxRuntimeException( "Unknown binary operator: " + operator );

		}
	}

	/**
	 * Evaluate the expression aginst a partition of data
	 */
	public Object evaluateAggregate( QoQSelectExecution QoQExec, List<int[]> intersections ) {
		if ( intersections.isEmpty() ) {
			return null;
		}
		return evaluate( QoQExec, intersections.get( 0 ) );
	}

	/**
	 * Reusable helper method to ensure that the left and right operands are boolean expressions or bit columns
	 * 
	 * @return true if the left and right operands are boolean expressions or bit columns
	 */
	private void ensureBooleanOperand( QoQSelectExecution QoQExec ) {
		// These checks may or may not work. If we can't get away with this, then we can boolean cast the values
		// but SQL doesn't really have the same concept of truthiness and mostly expects to always get booleans from boolean columns or boolean expressions
		if ( !expression.isBoolean( QoQExec ) ) {
			throw new BoxRuntimeException( "Unary operation [" + operator.getSymbol() + "] must be a boolean expression or bit column" );
		}
	}

	/**
	 * Reusable helper method to ensure that the left and right operands are numeric expressions or numeric columns
	 */
	private void ensureNumericOperand( QoQSelectExecution QoQExec ) {
		if ( !expression.isNumeric( QoQExec ) ) {
			throw new BoxRuntimeException( "Unary operation [" + operator.getSymbol() + "] must be a numeric expression or numeric column" );
		}
	}

	/**
	 * Helper for evaluating an expression as a number
	 * 
	 * @param tableLookup
	 * @param expression
	 * @param i
	 * 
	 * @return
	 */
	private double evalAsNumber( SQLExpression expression, QoQSelectExecution QoQExec, int[] intersection ) {
		return ( ( Number ) expression.evaluate( QoQExec, intersection ) ).doubleValue();
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
		map.put( "operator", enumToMap( operator ) );
		return map;
	}

}
