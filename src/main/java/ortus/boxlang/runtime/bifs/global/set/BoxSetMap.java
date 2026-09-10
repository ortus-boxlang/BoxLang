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
package ortus.boxlang.runtime.bifs.global.set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.SetUtil;

@BoxBIF( description = "Apply a transform to every element of a Set, deduplicating results into a new Set of the same variant." )
@BoxMember( type = BoxLangType.SET, name = "map" )
public class BoxSetMap extends BIF {

	public BoxSetMap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, "function:Function", Key.callback )
		};
	}

	/**
	 * Apply a transform function to every element of a Set and collect the deduplicated results into a new Set
	 * of the same variant. The source Set is not modified. The callback receives the element value, its 1-based
	 * ordinal position, and the original Set.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The source set to transform.
	 *
	 * @argument.callback Receives {@code (value, ordinal, set)} and returns the transformed value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return SetUtil.map( arguments.getAsSet( Key.set ), arguments.getAsFunction( Key.callback ), context );
	}

}
