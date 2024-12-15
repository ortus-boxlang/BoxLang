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
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.qoq.LikeOperation;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL binary operation
 */
public class SQLBinaryOperation extends SQLExpression {

	// EQUAL, NOTEQUAL, LESSTHAN, LESSTHANOREQUAL, GREATERTHAN, GREATERTHANOREQUAL, AND, OR
	private static final Set<SQLBinaryOperator>	booleanOperators	= Set.of( SQLBinaryOperator.EQUAL, SQLBinaryOperator.NOTEQUAL, SQLBinaryOperator.LESSTHAN,
	    SQLBinaryOperator.LESSTHANOREQUAL, SQLBinaryOperator.GREATERTHAN, SQLBinaryOperator.GREATERTHANOREQUAL, SQLBinaryOperator.AND, SQLBinaryOperator.OR,
	    SQLBinaryOperator.LIKE, SQLBinaryOperator.NOTLIKE );

	private static Set<SQLBinaryOperator>		mathOperators		= Set.of( SQLBinaryOperator.MINUS, SQLBinaryOperator.MULTIPLY, SQLBinaryOperator.DIVIDE,
	    SQLBinaryOperator.MODULO );

	private SQLExpression						left;

	private SQLExpression						right;

	private SQLBinaryOperator					operator;

	/**
	 * Only used for Like
	 */
	private SQLExpression						escape				= null;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLBinaryOperation( SQLExpression left, SQLExpression right, SQLBinaryOperator operator, Position position, String sourceText ) {
		super( position, sourceText );
		setLeft( left );
		setRight( right );
		setOperator( operator );
	}

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLBinaryOperation( SQLExpression left, SQLExpression right, SQLBinaryOperator operator, SQLExpression escape, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setLeft( left );
		setRight( right );
		setOperator( operator );
		setEscape( escape );
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
	 * Get the operator
	 */
	public SQLBinaryOperator getOperator() {
		return operator;
	}

	/**
	 * Set the operator
	 */
	public void setOperator( SQLBinaryOperator operator ) {
		this.operator = operator;
	}

	/**
	 * Get the escape
	 */
	public SQLExpression getEscape() {
		return escape;
	}

	/**
	 * Set the escape
	 */
	public void setEscape( SQLExpression escape ) {
		this.escape = escape;
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
		// All math operators but + return a number
		if ( mathOperators.contains( operator ) ) {
			return QueryColumnType.DOUBLE;
		}
		// Plus returns a string if the left and right operand were a string, otherwise it's a math operation.
		if ( operator == SQLBinaryOperator.PLUS ) {
			return QueryColumnType.isStringType( left.getType( QoQExec ) )
			    && QueryColumnType.isStringType( right.getType( QoQExec ) )
			        ? QueryColumnType.VARCHAR
			        : QueryColumnType.DOUBLE;
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
		Object	leftValue;
		Object	rightValue;
		Double	leftNum;
		Double	rightNum;
		int		compareResult;
		// Implement each binary operator
		switch ( operator ) {
			case DIVIDE :
				ensureNumericOperands( QoQExec );
				leftNum = evalAsNumber( left, QoQExec, intersection );
				rightNum = evalAsNumber( right, QoQExec, intersection );
				if ( leftNum == null || rightNum == null ) {
					return null;
				}
				if ( rightNum.doubleValue() == 0 ) {
					throw new BoxRuntimeException( "Division by zero" );
				}
				return leftNum / rightNum;
			case EQUAL :
				leftValue = left.evaluate( QoQExec, intersection );
				rightValue = right.evaluate( QoQExec, intersection );
				return EqualsEquals.invoke( leftValue, rightValue, true );
			case GREATERTHAN :
				return Compare.invoke( left.evaluate( QoQExec, intersection ), right.evaluate( QoQExec, intersection ), true ) == 1;
			case GREATERTHANOREQUAL :
				compareResult = Compare.invoke( left.evaluate( QoQExec, intersection ), right.evaluate( QoQExec, intersection ), true );
				return compareResult == 1 || compareResult == 0;
			case LESSTHAN :
				return Compare.invoke( left.evaluate( QoQExec, intersection ), right.evaluate( QoQExec, intersection ), true ) == -1;
			case LESSTHANOREQUAL :
				compareResult = Compare.invoke( left.evaluate( QoQExec, intersection ), right.evaluate( QoQExec, intersection ), true );
				return compareResult == -1 || compareResult == 0;
			case MINUS :
				ensureNumericOperands( QoQExec );
				leftNum = evalAsNumber( left, QoQExec, intersection );
				rightNum = evalAsNumber( right, QoQExec, intersection );
				if ( leftNum == null || rightNum == null ) {
					return null;
				}
				return leftNum - rightNum;
			case MODULO :
				ensureNumericOperands( QoQExec );
				leftNum = evalAsNumber( left, QoQExec, intersection );
				rightNum = evalAsNumber( right, QoQExec, intersection );
				if ( leftNum == null || rightNum == null ) {
					return null;
				}
				return leftNum % rightNum;
			case MULTIPLY :
				ensureNumericOperands( QoQExec );
				leftNum = evalAsNumber( left, QoQExec, intersection );
				rightNum = evalAsNumber( right, QoQExec, intersection );
				if ( leftNum == null || rightNum == null ) {
					return null;
				}
				return leftNum * rightNum;
			case NOTEQUAL :
				leftValue = left.evaluate( QoQExec, intersection );
				rightValue = right.evaluate( QoQExec, intersection );
				return !EqualsEquals.invoke( leftValue, rightValue, true );
			case AND :
				ensureBooleanOperands( QoQExec );
				leftValue = left.evaluate( QoQExec, intersection );
				// Short circuit, don't eval right if left is false
				if ( ( Boolean ) leftValue ) {
					return ( Boolean ) right.evaluate( QoQExec, intersection );
				} else {
					return false;
				}
			case OR :
				ensureBooleanOperands( QoQExec );
				if ( ( Boolean ) left.evaluate( QoQExec, intersection ) ) {
					return true;
				}
				if ( ( Boolean ) right.evaluate( QoQExec, intersection ) ) {
					return true;
				}
				return false;
			case PLUS :
				if ( left.isNumeric( QoQExec ) && right.isNumeric( QoQExec ) ) {
					leftNum		= evalAsNumber( left, QoQExec, intersection );
					rightNum	= evalAsNumber( right, QoQExec, intersection );
					if ( leftNum == null || rightNum == null ) {
						return null;
					}
					return leftNum + rightNum;
				} else {
					return Concat.invoke( left.evaluate( QoQExec, intersection ), right.evaluate( QoQExec, intersection ) );
				}
			case LIKE :
				return doLike( QoQExec, intersection );
			case NOTLIKE :
				return !doLike( QoQExec, intersection );
			case CONCAT :
				return Concat.invoke( left.evaluate( QoQExec, intersection ), right.evaluate( QoQExec, intersection ) );
			default :
				throw new BoxRuntimeException( "Unknown binary operator: " + operator );
		}
	}

	/**
	 * Implement LIKE so we can reuse for NOT LIKE
	 */
	private boolean doLike( QoQSelectExecution QoQExec, int[] intersection ) {
		String	leftValueStr	= StringCaster.cast( left.evaluate( QoQExec, intersection ) );
		String	rightValueStr	= StringCaster.cast( right.evaluate( QoQExec, intersection ) );
		String	escapeValue		= null;
		if ( escape != null ) {
			escapeValue = StringCaster.cast( escape.evaluate( QoQExec, intersection ) );
		}
		return LikeOperation.invoke( leftValueStr, rightValueStr, escapeValue );
	}

	/**
	 * Reusable helper method to ensure that the left and right operands are boolean expressions or bit columns
	 * 
	 * @return true if the left and right operands are boolean expressions or bit columns
	 */
	private void ensureBooleanOperands( QoQSelectExecution QoQExec ) {
		// These checks may or may not work. If we can't get away with this, then we can boolean cast the values
		// but SQL doesn't really have the same concept of truthiness and mostly expects to always get booleans from boolean columns or boolean expressions
		if ( !left.isBoolean( QoQExec ) ) {
			throw new BoxRuntimeException( "Left side of a boolean [" + operator.getSymbol() + "] operation must be a boolean expression or bit column" );
		}
		if ( !right.isBoolean( QoQExec ) ) {
			throw new BoxRuntimeException( "Right side of a boolean [" + operator.getSymbol() + "] operation must be a boolean expression or bit column" );
		}
	}

	/**
	 * Reusable helper method to ensure that the left and right operands are numeric expressions or numeric columns
	 */
	private void ensureNumericOperands( QoQSelectExecution QoQExec ) {
		if ( !left.isNumeric( QoQExec ) ) {
			throw new BoxRuntimeException( "Left side of a math [" + operator.getSymbol() + "] operation must be a numeric expression or numeric column" );
		}
		if ( !right.isNumeric( QoQExec ) ) {
			throw new BoxRuntimeException( "Right side of a math [" + operator.getSymbol() + "] operation must be a numeric expression or numeric column" );
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

		map.put( "left", left.toMap() );
		map.put( "right", right.toMap() );
		map.put( "operator", enumToMap( operator ) );
		if ( escape != null ) {
			map.put( "escape", escape.toMap() );
		} else {
			map.put( "escape", null );
		}
		return map;
	}

}