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
package ortus.boxlang.runtime.config.segments;

import java.util.Set;

import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This config segment is used to configure the default query execution settings
 * for the BoxLang runtime. These defaults apply to all query operations
 * (e.g. {@code queryExecute}, {@code bx:query}) unless overridden per-query.
 */
public class QueriesConfig implements IConfigSegment {

	/**
	 * The default timeout for queries in seconds.
	 * {@code 0} means no timeout and is the default.
	 */
	public Integer						timeout					= 0;

	/**
	 * The default return type for queries.
	 * Valid values are: {@code query}, {@code array}, {@code struct}.
	 * Defaults to {@code query}.
	 */
	public String						returnType				= "query";

	/**
	 * The default number of rows to fetch from the database at once.
	 * {@code 0} means fetch all rows (driver default).
	 */
	public Integer						fetchSize				= 0;

	/**
	 * Maximum number of rows to return.
	 * {@code 0} means return all rows.
	 */
	public Integer						maxrows					= 0;

	/**
	 * The default named cache to use for query caching.
	 * This cache must be defined in the "caches" section of the configuration.
	 * Defaults to {@code default}.
	 */
	public String						cacheProvider			= "default";

	/**
	 * Allowed values for the returnType property
	 */
	private static final Set<String>	ALLOWED_RETURN_TYPES	= Set.of( "query", "array", "struct" );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default empty constructor
	 */
	public QueriesConfig() {
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		this.timeout		= PropertyHelper.processInteger( config, Key.timeout, this.timeout );
		this.returnType		= PropertyHelper.processString( config, Key.returnType, this.returnType, ALLOWED_RETURN_TYPES );
		this.fetchSize		= PropertyHelper.processInteger( config, Key.fetchSize, this.fetchSize );
		this.maxrows		= PropertyHelper.processInteger( config, Key.maxRows, this.maxrows );
		this.cacheProvider	= PropertyHelper.processString( config, Key.cacheProvider, this.cacheProvider );

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.ofNonConcurrent(
		    Key.timeout, this.timeout,
		    Key.returnType, this.returnType,
		    Key.fetchSize, this.fetchSize,
		    Key.maxRows, this.maxrows,
		    Key.cacheProvider, this.cacheProvider
		);
	}

}
