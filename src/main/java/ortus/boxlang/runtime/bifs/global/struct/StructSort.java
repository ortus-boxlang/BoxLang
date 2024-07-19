
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
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.FunctionCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructSort extends BIF {

	/**
	 * Constructor
	 */
	public StructSort() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( false, "any", Key.sortType, "text" ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "string", Key.path ),
		    new Argument( false, "function:Comparator", Key.callback )
		};
	}

	/**
	 * Sorts a struct according to the specified arguments and returns an array of struct keys
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to sort
	 *
	 * @argument.sortType An optional sort type to apply to that type - if a callback is given in this position it will be used as that argument
	 *
	 * @argument.sortOrder The sort order applicable to the sortType argument
	 *
	 * @argument.callback An optional callback to use as the sorting function. You can alternatively pass a Java Comparator.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct					target						= arguments.getAsStruct( Key.struct );
		CastAttempt<Function>	typeCastToFunctionAttempt	= FunctionCaster.attempt( arguments.get( Key.sortType ), "Comparator" );
		if ( typeCastToFunctionAttempt.wasSuccessful() ) {
			arguments.put( Key.callback, typeCastToFunctionAttempt.get() );
			arguments.put( Key.sortType, null );
		}

		if ( arguments.get( Key.callback ) == null ) {
			return StructUtil.sort(
			    target,
			    StringCaster.cast( arguments.get( Key.sortType ) ),
			    arguments.getAsString( Key.sortOrder ),
			    arguments.getAsString( Key.path )
			);
		} else {
			return StructUtil.sort(
			    target,
			    arguments.getAsFunction( Key.callback ),
			    context
			);
		}
	}

}
