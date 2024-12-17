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
import ortus.boxlang.runtime.jdbc.qoq.QoQFunctionService;
import ortus.boxlang.runtime.jdbc.qoq.QoQFunctionService.QoQFunction;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * Abstract Node class representing SQL function call
 */
public class SQLFunction extends SQLExpression {

	private final static Set<QueryColumnType>	numericTypes	= Set.of( QueryColumnType.BIGINT, QueryColumnType.DECIMAL, QueryColumnType.DOUBLE,
	    QueryColumnType.INTEGER, QueryColumnType.BIT );

	private Key									name;

	private List<SQLExpression>					arguments;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLFunction( Key name, List<SQLExpression> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setArguments( arguments );
	}

	/**
	 * Get the name of the function
	 *
	 * @return the name of the function
	 */
	public Key getName() {
		return name;
	}

	/**
	 * Set the name of the function
	 *
	 * @param name the name of the function
	 */
	public void setName( Key name ) {
		this.name = name;
	}

	/**
	 * Get the arguments of the function
	 *
	 * @return the arguments of the function
	 */
	public List<SQLExpression> getArguments() {
		return arguments;
	}

	/**
	 * Set the arguments of the function
	 *
	 * @param arguments the arguments of the function
	 */
	public void setArguments( List<SQLExpression> arguments ) {
		replaceChildren( this.arguments, arguments );
		this.arguments = arguments;
		this.arguments.forEach( a -> a.setParent( this ) );
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
	 * Is function aggregate
	 */
	public boolean isAggregate() {
		return QoQFunctionService.getFunction( name ).isAggregate();
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQSelectExecution QoQExec ) {
		return QoQFunctionService.getFunction( name ).returnType( QoQExec, arguments );
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQSelectExecution QoQExec, int[] intersection ) {
		QoQFunction function = QoQFunctionService.getFunction( name );
		if ( function.requiredParams() > arguments.size() ) {
			throw new RuntimeException(
			    "QoQ Function " + name + "() expects at least" + function.requiredParams() + " arguments, but got " + arguments.size() );
		}
		if ( function.isAggregate() ) {
			throw new RuntimeException( "QoQ Function " + name + "() is an aggregate function and cannot be used in a non-aggregate context" );
		} else {
			return function.invoke( arguments.stream().map( a -> a.evaluate( QoQExec, intersection ) ).toList(), arguments );
		}
	}

	/**
	 * Evaluate the expression aginst a partition of data
	 */
	public Object evaluateAggregate( QoQSelectExecution QoQExec, List<int[]> intersections ) {
		if ( intersections.isEmpty() ) {
			return null;
		}
		QoQFunction function = QoQFunctionService.getFunction( name );
		if ( function.isAggregate() ) {
			return function.invokeAggregate(
			    arguments.stream().map( a -> buildAggregateValues( QoQExec, intersections, a ) ).toList(), arguments );
		} else {
			return function.invoke( arguments.stream().map( a -> a.evaluateAggregate( QoQExec, intersections ) ).toList(), arguments );
		}
	}

	protected Object[] buildAggregateValues( QoQSelectExecution QoQExec, List<int[]> intersections, SQLExpression argument ) {
		return intersections.stream().map( i -> argument.evaluate( QoQExec, i ) ).filter( v -> v != null ).toArray();
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

		map.put( "name", getName().getName() );
		map.put( "arguments", getArguments().stream().map( BoxNode::toMap ).toList() );
		return map;
	}

}
