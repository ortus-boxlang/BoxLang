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
package ortus.boxlang.compiler.ast.sql.select;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLStarExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Abstract Node class representing SQL result column declaration
 */
public class SQLResultColumn extends SQLNode {

	private SQLExpression	expression;

	private Key				alias;

	private int				ordinalPosition;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLResultColumn( SQLExpression expression, String alias, int ordinalPosition, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setAlias( alias );
		setOrdinalPosition( ordinalPosition );
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
	 * Get the table alias
	 */
	public Key getAlias() {
		return alias;
	}

	/**
	 * Set the table alias
	 */
	public void setAlias( String alias ) {
		this.alias = ( alias == null ) ? null : Key.of( alias );
	}

	/**
	 * Get the ordinal position
	 */
	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	/**
	 * Set the ordinal position
	 */
	public void setOrdinalPosition( int ordinalPosition ) {
		this.ordinalPosition = ordinalPosition;
	}

	/**
	 * The name this result column will have in the final result set. This is either the alias or the column name.
	 * If it's any other expression, we name it column_0, column_1, column_2, etc based on the ordinal position in the overall result set.
	 */
	public Key getResultColumnName() {
		if ( alias != null ) {
			return alias;
		} else if ( expression instanceof SQLColumn c ) {
			return c.getName();
		} else {
			return Key.of( "column_" + ( ordinalPosition - 1 ) );
		}
	}

	/**
	 * Is this column a star column?
	 */
	public boolean isStarExpression() {
		return expression instanceof SQLStarExpression;
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
		if ( alias != null ) {
			map.put( "alias", alias );
		} else {
			map.put( "alias", null );
		}
		return map;
	}

}
