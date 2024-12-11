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

import java.util.Map;

import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.runtime.types.Query;
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
	 * Check if the expression evaluates to a boolean value
	 */
	public boolean isBoolean() {
		return false;
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param tableLookup lookup for tables
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( Map<SQLTable, Query> tableLookup ) {
		return isBoolean();
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param tableLookup lookup for tables
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( Map<SQLTable, Query> tableLookup ) {
		return false;
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( Map<SQLTable, Query> tableLookup ) {
		if ( isBoolean() ) {
			return QueryColumnType.BIT;
		}
		return QueryColumnType.OBJECT;
	}

	/**
	 * Evaluate the expression
	 */
	public abstract Object evaluate( Map<SQLTable, Query> tableLookup, int i );

}
