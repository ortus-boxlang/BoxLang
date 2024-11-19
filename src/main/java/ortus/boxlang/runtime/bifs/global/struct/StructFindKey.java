
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

package ortus.boxlang.runtime.bifs.global.struct;

import java.util.Optional;
import java.util.stream.Stream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructFindKey extends BIF {

	/**
	 * Constructor
	 */
	public StructFindKey() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( true, "any", Key.key ),
		    new Argument( false, "string", Key.scope, "one" )
		};
	}

	/**
	 * Searches a struct for a given key and returns an array of values
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to search
	 *
	 * @argument.key The key to search for
	 *
	 * @argument.scope Either one (default), which finds the first instance or all to return all values
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key				scopeKey		= Key.of( arguments.get( Key.scope ) );
		Stream<IStruct>	searchStream	= StructUtil.findKey(
		    arguments.getAsStruct( Key.struct ),
		    arguments.getAsString( Key.key )
		);

		Object			result			= scopeKey.equals( StructUtil.scopeAll )
		    ? searchStream.toArray()
		    : searchStream.findFirst();
		return result instanceof Optional opt
		    ? opt.isEmpty()
		        ? new Array()
		        : new Array( new Object[] { opt.get() } )
		    : new Array( ( Object[] ) result );

	}

}
