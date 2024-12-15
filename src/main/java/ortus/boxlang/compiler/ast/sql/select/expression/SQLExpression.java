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

import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * Abstract Node class representing SQL expression
 */
public abstract class SQLExpression extends SQLNode {

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLExpression( Position position, String sourceText ) {
		super( position, sourceText );
	}

	/**
	 * Check if the expression is a literal
	 */
	public boolean isLiteral() {
		return false;
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( QoQSelectExecution QoQExec ) {
		return false;
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( QoQSelectExecution QoQExec ) {
		return false;
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQSelectExecution QoQExec ) {
		if ( isBoolean( QoQExec ) ) {
			return QueryColumnType.BIT;
		}
		return QueryColumnType.OBJECT;
	}

	/**
	 * Evaluate the expression
	 */
	public abstract Object evaluate( QoQSelectExecution QoQExec, int[] intersection );

}
