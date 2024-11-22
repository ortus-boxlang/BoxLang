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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL join
 */
public class SQLJoin extends SQLNode {

	private SQLJoinType		type;

	private SQLTable		table;

	private SQLExpression	on;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	protected SQLJoin( SQLJoinType type, SQLTable table, SQLExpression on, Position position, String sourceText ) {
		super( position, sourceText );
	}

	/**
	 * Get the join type
	 */
	public SQLJoinType getType() {
		return type;
	}

	/**
	 * Set the join type
	 */
	public void setType( SQLJoinType type ) {
		this.type = type;
	}

	/**
	 * Get the table
	 */
	public SQLTable getTable() {
		return table;
	}

	/**
	 * Set the table
	 */
	public void setTable( SQLTable table ) {
		replaceChildren( this.table, table );
		this.table = table;
		this.table.setParent( this );
	}

	/**
	 * Get the ON expression
	 */
	public SQLExpression getOn() {
		return on;
	}

	/**
	 * Set the ON expression
	 */
	public void setOn( SQLExpression on ) {
		if ( !on.isBoolean() ) {
			throw new BoxRuntimeException( "ON clause must be a boolean expression" );
		}
		replaceChildren( this.on, on );
		this.on = on;
		this.on.setParent( this );
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
}
