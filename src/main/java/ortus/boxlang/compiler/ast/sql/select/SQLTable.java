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

import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Class representing SQL table value. I may be a reference to a variable name or a sub-select
 */
public abstract class SQLTable extends SQLNode {

	protected Key	alias;

	/**
	 * Encounter order of the table in the query. This should match the position of the table in the tableLookup map later
	 */
	protected int	index;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLTable( String alias, int index, Position position, String sourceText ) {
		super( position, sourceText );
		setAlias( alias );
		setIndex( index );
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
	 * Get the table index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Set the table index
	 */
	public void setIndex( int index ) {
		this.index = index;
	}

	abstract public boolean isCalled( Key name );

}
