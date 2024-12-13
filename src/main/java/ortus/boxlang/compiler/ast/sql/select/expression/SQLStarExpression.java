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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecution;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL * expression
 */
public class SQLStarExpression extends SQLExpression {

	private SQLTable table;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLStarExpression( SQLTable table, Position position, String sourceText ) {
		super( position, sourceText );
		setTable( table );
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
		// This node has no parent/child relationship, it's just a reference
		this.table = table;
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQExecution QoQExec, int[] intersection ) {
		throw new BoxRuntimeException( "Cannot evaluate a * expression" );
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

		if ( table != null ) {
			map.put( "table", table.toMap() );
		} else {
			map.put( "table", null );
		}
		return map;
	}

}
