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
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL binary operation
 */
public class SQLBinaryOperation extends SQLExpression {

	// EQUAL, NOTEQUAL, LESSTHAN, LESSTHANOREQUAL, GREATERTHAN, GREATERTHANOREQUAL, AND, OR
	private static final Set<SQLBinaryOperator>	booleanOperators	= Set.of( SQLBinaryOperator.EQUAL, SQLBinaryOperator.NOTEQUAL, SQLBinaryOperator.LESSTHAN,
	    SQLBinaryOperator.LESSTHANOREQUAL, SQLBinaryOperator.GREATERTHAN, SQLBinaryOperator.GREATERTHANOREQUAL, SQLBinaryOperator.AND, SQLBinaryOperator.OR );

	private static Set<SQLBinaryOperator>		mathOperators		= Set.of( SQLBinaryOperator.MINUS, SQLBinaryOperator.MULTIPLY, SQLBinaryOperator.DIVIDE,
	    SQLBinaryOperator.MODULO );

	private SQLExpression						left;

	private SQLExpression						right;

	private SQLBinaryOperator					operator;

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
	 * Check if the expression evaluates to a boolean value
	 */
	public boolean isBoolean() {
		return booleanOperators.contains( operator );
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( Map<SQLTable, Query> tableLookup ) {
		// If this is a boolean operation, then we're a bit
		if ( isBoolean() ) {
			return QueryColumnType.BIT;
		}
		// All math operators but + return a number
		if ( mathOperators.contains( operator ) ) {
			return QueryColumnType.DOUBLE;
		}
		// Plus returns a string if the left and right operand were a string, otherwise it's a math operation.
		if ( operator == SQLBinaryOperator.PLUS ) {
			return QueryColumnType.isStringType( left.getType( tableLookup ) ) && QueryColumnType.isStringType( right.getType( tableLookup ) )
			    ? QueryColumnType.VARCHAR
			    : QueryColumnType.DOUBLE;
		}
		return QueryColumnType.OBJECT;
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

		map.put( "left", left.toMap() );
		map.put( "right", right.toMap() );
		map.put( "operator", enumToMap( operator ) );
		return map;
	}

}