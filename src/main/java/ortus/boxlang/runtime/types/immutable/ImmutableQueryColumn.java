/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.types.immutable;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

public class ImmutableQueryColumn extends QueryColumn {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Add new column to query
	 *
	 * @param name  column name
	 * @param type  column type
	 * @param query query
	 * @param index column index (0-based)
	 */
	public ImmutableQueryColumn( Key name, QueryColumnType type, Query query, int index ) {
		super( name, type, query, index );
	}

	// Convenience methods

	/**
	 * Set the value of a cell in this column
	 *
	 * @param row   The row to set, 0-based index
	 * @param value The value to set
	 *
	 * @return This QueryColumn
	 */
	public QueryColumn setCell( int row, Object value ) {
		throw new UnmodifiableException( "Cannot set cell in immutable Query column" );
	}

	/***************************
	 * IReferencable implementation
	 ****************************/

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new UnmodifiableException( "Cannot assign to immutable Query column" );
	}

}
