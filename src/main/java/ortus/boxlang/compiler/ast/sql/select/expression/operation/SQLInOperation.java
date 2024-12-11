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
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.types.Query;

/**
 * Abstract Node class representing SQL IN operation
 */
public class SQLInOperation extends SQLExpression {

	private boolean				not;

	private SQLExpression		expression;

	private List<SQLExpression>	values;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLInOperation( SQLExpression expression, List<SQLExpression> values, boolean not, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setValues( values );
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
	 * Get the values
	 */
	public List<SQLExpression> getValues() {
		return values;
	}

	/**
	 * Set the values
	 */
	public void setValues( List<SQLExpression> values ) {
		replaceChildren( this.values, values );
		this.values = values;
		this.values.forEach( v -> v.setParent( this ) );
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
	 * Check if the expression evaluates to a boolean value
	 */
	public boolean isBoolean() {
		return true;
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( Map<SQLTable, Query> tableLookup, int i ) {
		Object value = expression.evaluate( tableLookup, i );
		for ( SQLExpression v : values ) {
			if ( EqualsEquals.invoke( value, v.evaluate( tableLookup, i ), true ) ) {
				return !not;
			}
		}
		return not;
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
		map.put( "values", values.stream().map( SQLExpression::toMap ).toList() );
		return map;
	}

}
