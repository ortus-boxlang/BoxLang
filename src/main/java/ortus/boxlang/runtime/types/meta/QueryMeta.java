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
package ortus.boxlang.runtime.types.meta;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * This class represents the metadata of a BoxLang Query object
 */
public class QueryMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private Query	target;
	public Class<?>	$class;
	public IStruct	meta;

	/**
	 * Constructor
	 *
	 * @param target The target object this metadata is for
	 */
	public QueryMeta( Query target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		// one might say this method call is a bit meta...
		IStruct metadata = target.getMetaData();
		metadata.put( Key.type, "Query" );
		this.meta = new ImmutableStruct( metadata );

	}

	/**
	 * Get target object this metadata is for
	 */
	public Query getTarget() {
		return this.target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return this.meta;
	}

}
