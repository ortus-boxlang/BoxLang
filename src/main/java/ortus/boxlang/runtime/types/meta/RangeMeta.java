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
import ortus.boxlang.runtime.types.Range;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;

/**
 * This class represents BoxLang metadata for a Range object.
 * It exposes range-specific properties such as bounds, step, element type,
 * and iteration/boundedness characteristics.
 */
public class RangeMeta extends BoxMeta<Range<?>> {

	@SuppressWarnings( "unused" )
	private Range<?>		target;
	public Class<?>			$class;
	public IStruct			meta;

	/**
	 * --------------------------------------------------------------------------
	 * Public Keys
	 * --------------------------------------------------------------------------
	 */
	public static final Key	fromKey			= Key.of( "from" );
	public static final Key	toKey			= Key.of( "to" );
	public static final Key	stepKey			= Key.of( "step" );
	public static final Key	elementTypeKey	= Key.of( "elementType" );
	public static final Key	iterableKey		= Key.of( "iterable" );
	public static final Key	boundedKey		= Key.of( "bounded" );
	public static final Key	ascendingKey	= Key.of( "ascending" );

	/**
	 * Constructor
	 *
	 * @param target The Range object this metadata is for
	 */
	public RangeMeta( Range<?> target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		// Assemble the metadata
		this.meta	= UnmodifiableStruct.of(
		    fromKey, target.getFrom(),
		    toKey, target.getTo(),
		    stepKey, target.getStep(),
		    elementTypeKey, target.getFrom() != null
		        ? target.getFrom().getClass().getSimpleName()
		        : ( target.getTo() != null ? target.getTo().getClass().getSimpleName() : "Unknown" ),
		    iterableKey, target.isIterable(),
		    boundedKey, target.isBounded(),
		    ascendingKey, target.isAscending()
		);
	}

	/**
	 * Get target object this metadata is for
	 */
	public Range<?> getTarget() {
		return this.target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return this.meta;
	}

}
