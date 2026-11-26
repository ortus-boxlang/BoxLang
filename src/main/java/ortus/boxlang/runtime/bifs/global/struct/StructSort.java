
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

@BoxBIF( description = "Sort struct keys and return as an array" )
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
	 * Sorts the keys of a struct and returns them as an array of key names; the struct itself is not modified. Three modes are supported:
	 * <ul>
	 * <li>Text directive: sortType is text, textNoCase or numeric, combined with sortOrder (asc or desc). Keys are sorted by their values. An
	 * invalid combination throws a BoxRuntimeException.</li>
	 * <li>Path: if a path is given, each value must itself be a struct and the sort is performed on the value found at that path in each nested struct.</li>
	 * <li>Callback: a function (or Java Comparator) that receives two <strong>key names</strong> and returns a negative number, zero or a positive number.
	 * The callback may be passed in the sortType position.</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to sort.
	 *
	 * @argument.sortType The sort type: text (default), textNoCase or numeric. If a function is passed here it is used as the callback.
	 *
	 * @argument.sortOrder The sort order, asc (default) or desc.
	 *
	 * @argument.path An optional key path used to sort by a nested value within each struct entry (e.g. "department"). When provided, sortType is used only for case sensitivity (textNoCase) and sortOrder for direction.
	 *
	 * @argument.callback An optional comparator function used to sort the keys. It receives two key names. You can alternatively pass a Java Comparator.
	 *
	 * @return An array of the struct's key names in sorted order.
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
			    context,
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
