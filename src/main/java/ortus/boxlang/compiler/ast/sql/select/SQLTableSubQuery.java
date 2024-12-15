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
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Class representing SQL table as a sub query
 */
public class SQLTableSubQuery extends SQLTable {

	private SQLSelectStatement selectStatement;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLTableSubQuery( SQLSelectStatement selectStatement, String alias, int index, Position position, String sourceText ) {
		super( alias, index, position, sourceText );
		setSelectStatement( selectStatement );
	}

	/**
	 * Get the select statement
	 */
	public SQLSelectStatement getSelectStatement() {
		return selectStatement;
	}

	/**
	 * Set the select statement
	 */
	public void setSelectStatement( SQLSelectStatement selectStatement ) {
		replaceChildren( this.selectStatement, selectStatement );
		this.selectStatement = selectStatement;
		this.selectStatement.setParent( this );
	}

	public boolean isCalled( Key name ) {
		return alias.equals( name );
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

		map.put( "selectStatement", selectStatement.toMap() );
		return map;
	}

}
