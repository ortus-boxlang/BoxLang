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
 * Abstract Node class representing SQL unary operation
 */
public class SQLUnaryOperation extends SQLExpression {

	// NOT, ISNULL, ISNOTNULL
	private static final Set<SQLUnaryOperator>	booleanOperators	= Set.of( SQLUnaryOperator.NOT, SQLUnaryOperator.ISNULL, SQLUnaryOperator.ISNOTNULL );

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
		if ( operator == SQLUnaryOperator.PLUS || operator == SQLUnaryOperator.MINUS ) {
			return QueryColumnType.DOUBLE;
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

		map.put( "expression", expression.toMap() );
		map.put( "operator", enumToMap( operator ) );
		return map;
	}

}
