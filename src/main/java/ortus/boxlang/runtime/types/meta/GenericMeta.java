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
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;

/**
 * This class represents generic BoxLang metadata for a an object which has no object-specifc properties
 */
public class GenericMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private Object	target;
	public Class<?>	$class;
	public IStruct	meta;

	/**
	 * Constructor with a target object
	 * The target could be an instance or a class
	 */
	public GenericMeta( Object target ) {
		super();
		this.target = target;
		if ( target instanceof Class<?> targetClass ) {
			this.$class = targetClass;
		} else {
			this.$class = target.getClass();
		}
		this.meta = UnmodifiableStruct.EMPTY;

	}

	/**
	 * Get target object this metadata is for
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return this.meta;
	}

}
