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
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This class represents BoxLang metadata for a Set object.
 * It exposes set-specific properties such as type, size, case-sensitivity,
 * and synchronization.
 */
public class SetMeta extends BoxMeta<BoxSet> {

	@SuppressWarnings( "unused" )
	private BoxSet			target;
	public Class<?>			$class;
	public IStruct			meta;

	/**
	 * --------------------------------------------------------------------------
	 * Public Keys
	 * --------------------------------------------------------------------------
	 */
	public static final Key	typeKey				= Key.of( "type" );
	public static final Key	caseSensitiveKey	= Key.of( "caseSensitive" );
	public static final Key	synchronizedKey		= Key.of( "synchronized" );

	/**
	 * Constructor
	 *
	 * @param target The BoxSet object this metadata is for
	 */
	public SetMeta( BoxSet target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		// Assemble the metadata
		Struct metaStruct = new Struct();
		metaStruct.put( typeKey, target.getType().name().toLowerCase() );
		metaStruct.put( caseSensitiveKey, target.isCaseSensitive() );
		metaStruct.put( synchronizedKey, target.isSynchronized() );
		this.meta = metaStruct;
	}

	/**
	 * Get target object this metadata is for
	 */
	public BoxSet getTarget() {
		return this.target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return this.meta;
	}

}
