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
package ortus.boxlang.compiler.ast.sql.select.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * Abstract Node class representing SQL Param expression
 */
public class SQLParam extends SQLExpression {

	private final static Set<QueryColumnType>	numericTypes	= Set.of( QueryColumnType.BIGINT, QueryColumnType.DECIMAL, QueryColumnType.DOUBLE,
	    QueryColumnType.INTEGER, QueryColumnType.BIT );

	/**
	 * Null if positinal
	 */
	private String								name;

	// JDBC uses 1-based indexes!
	private int									index			= 0;

	/**
	 * Constructor. Index is 1-based!
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLParam( String name, int index, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setIndex( index );
	}

	/**
	 * Get the name of the function
	 *
	 * @return the name of the function
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the function
	 *
	 * @param name the name of the function
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Get the index. 1-based!
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Set the index. 1-based!
	 */
	public void setIndex( int index ) {
		this.index = index;
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( QoQSelectExecution QoQExec ) {
		return getType( QoQExec ) == QueryColumnType.BIT;
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( QoQSelectExecution QoQExec ) {
		return numericTypes.contains( getType( QoQExec ) );
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQSelectExecution QoQExec ) {
		return QueryColumnType.fromSQLType( QoQExec.getSelectStatementExecution().getParams().get( index ).type() );
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQSelectExecution QoQExec, int[] intersection ) {
		return QoQExec.getSelectStatementExecution().getParams().get( index ).value();
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

		if ( name != null ) {
			map.put( "name", name );
		} else {
			map.put( "name", null );
		}
		map.put( "index", index );
		return map;
	}

}
