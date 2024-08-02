
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT, name = "keyTranslate" )

public class StructKeyTranslate extends BIF {

	/**
	 * Constructor
	 */
	public StructKeyTranslate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( false, "boolean", Key.deep, false ),
		    new Argument( false, "boolean", Key.retainKeys, false )
		};
	}

	/**
	 * Converts a struct with dot-notated keys in to an unflattened version
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to unflatten
	 *
	 * @argument.deep Whether to recurse in to nested keys - default false
	 *
	 * @argument.retainKeys Whether to retain the original dot-notated keys - default false
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct ref = arguments.getAsStruct( Key.struct );
		StructUtil.unFlattenKeys( ref, arguments.getAsBoolean( Key.deep ), arguments.getAsBoolean( Key.retainKeys ) );
		return ref;
	}

}
