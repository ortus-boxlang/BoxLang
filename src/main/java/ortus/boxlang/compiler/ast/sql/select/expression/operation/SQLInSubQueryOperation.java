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
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;

/**
 * Node class representing SQL IN operation based on a sub query
 */
public class SQLInSubQueryOperation extends SQLExpression {

	private boolean				not;

	private SQLExpression		expression;

	// Must be an independant sub query for now. Cannot correlate to the parent query
	private SQLSelectStatement	subquery;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLInSubQueryOperation( SQLExpression expression, SQLSelectStatement subquery, boolean not, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setSubQuery( subquery );
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
	 * Get the sub query
	 */
	public SQLSelectStatement getSubQuery() {
		return subquery;
	}

	/**
	 * Set the sub query
	 */
	public void setSubQuery( SQLSelectStatement subquery ) {
		replaceChildren( this.subquery, subquery );
		this.subquery = subquery;
		this.subquery.setParent( this );
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
		Object	value				= expression.evaluate( QoQExec, intersection );
		Query	subResult			= QoQExec.getIndepententSubQuery( subquery );
		Key		firstAndOnlyColName	= subResult.getColumns().keySet().iterator().next();
		for ( Object v : subResult.getColumnData( firstAndOnlyColName ) ) {
			if ( EqualsEquals.invoke( value, v, true ) ) {
				return !not;
			}
		}
		return not;
	}

	/**
	 * Evaluate the expression aginst a partition of data
	 */
	public Object evaluateAggregate( QoQSelectExecution QoQExec, List<int[]> intersections ) {
		if ( intersections.isEmpty() ) {
			return false;
		}
		return evaluate( QoQExec, intersections.get( 0 ) );
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

		map.put( "not", not );
		map.put( "expression", expression.toMap() );
		map.put( "subquery", subquery.toMap() );
		return map;
	}

}
