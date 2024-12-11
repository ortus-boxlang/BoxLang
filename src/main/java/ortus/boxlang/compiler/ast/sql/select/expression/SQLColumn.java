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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;

/**
 * Abstract Node class representing SQL column reference
 */
public class SQLColumn extends SQLExpression {

	private SQLTable	table;

	private Key			name;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLColumn( SQLTable table, String name, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setTable( table );
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
	public void setName( String name ) {
		this.name = Key.of( name );
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
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( Map<SQLTable, Query> tableLookup ) {
		return tableLookup.get( table ).getColumns().get( name ).getType();
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( Map<SQLTable, Query> tableLookup, int i ) {
		return tableLookup.get( table ).getCell( name, i - 1 );
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param tableLookup lookup for tables
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( Map<SQLTable, Query> tableLookup ) {
		return getType( tableLookup ) == QueryColumnType.BIT;
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

		map.put( "name", name.getName() );
		if ( table != null ) {
			map.put( "table", table.toMap() );
		} else {
			map.put( "table", null );
		}
		return map;
	}

}
