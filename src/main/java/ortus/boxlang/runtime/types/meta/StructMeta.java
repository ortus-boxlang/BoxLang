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

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.immutable.IImmutable;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * This class represents BoxLang metadata for a Struct
 */
public class StructMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private Struct	target;
	public Class<?>	$class;
	public IStruct	meta;

	/**
	 * Constructor
	 */
	public StructMeta( Struct target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		// Assemble the metadata
		this.meta	= ImmutableStruct.of(
		    "type", this.target.type.name(),
		    "immutable", this.target instanceof IImmutable
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
