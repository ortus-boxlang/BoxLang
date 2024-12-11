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
package ortus.boxlang.compiler.ast.sql.select.expression.literal;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.QoQExecution;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * Abstract Node class representing SQL number literal
 */
public class SQLNumberLiteral extends SQLExpression {

	private Number value;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLNumberLiteral( Number value, Position position, String sourceText ) {
		super( position, sourceText );
		setValue( value );
	}

	/**
	 * Get the value of the Number literal
	 *
	 * @return the value of the Number literal
	 */
	public Number getValue() {
		return value;
	}

	public void setValue( Number value ) {
		this.value = value;
	}

	/**
	 * Check if the expression is a literal
	 */
	public boolean isLiteral() {
		return true;
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQExecution QoQExec ) {
		return QueryColumnType.DOUBLE;
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQExecution QoQExec, int i ) {
		return value;
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( Map<SQLTable, Query> tableLookup ) {
		return true;
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

		map.put( "value", value );
		return map;
	}

}
