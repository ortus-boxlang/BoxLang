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

import java.util.Set;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.immutable.IImmutable;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * This class represents BoxLang metadata for a Scope
 */
public class ScopeMeta extends BoxMeta {

	private IStruct	target;
	public Class<?>	$class;
	public IStruct	meta;

	/**
	 * Constructor
	 */
	public ScopeMeta( IStruct target, Set<Key> finalKeySet ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		// Assemble the metadata
		this.meta	= ImmutableStruct.of(
		    "type", this.target.getType().name(),
		    "immutable", this.target instanceof IImmutable,
		    "casesensitive", this.target.isCaseSensitive(),
		    "soft", this.target.isSoftReferenced(),
		    "ordered", this.target.getType().equals( IStruct.TYPES.LINKED ) || this.target.getType().equals( IStruct.TYPES.LINKED_CASE_SENSITIVE ),
		    "finalKeySet", finalKeySet
		);

	}

	/**
	 * Get target object this metadata is for
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return meta;
	}

}
