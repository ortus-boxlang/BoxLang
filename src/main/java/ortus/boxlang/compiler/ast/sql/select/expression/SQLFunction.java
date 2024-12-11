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
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.QoQExecution;
import ortus.boxlang.runtime.jdbc.qoq.QoQFunctionService;
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
	public boolean isBoolean( QoQExecution QoQExec ) {
		return getType( QoQExec ) == QueryColumnType.BIT;
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( QoQExecution QoQExec ) {
		return numericTypes.contains( getType( QoQExec ) );
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQExecution QoQExec ) {
		return QoQFunctionService.getFunction( name ).getReturnType();
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQExecution QoQExec, int i ) {
		return QoQFunctionService.getFunction( name ).invoke( arguments.stream().map( a -> a.evaluate( QoQExec, i ) ).toList() );
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
