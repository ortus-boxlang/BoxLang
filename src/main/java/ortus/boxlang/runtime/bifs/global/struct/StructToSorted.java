
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
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF( description = "Create a sorted copy of a struct" )
@BoxMember( type = BoxLangType.STRUCT )

public class StructToSorted extends BIF {

	private static final StructNew structCreator = new StructNew();

	/**
	 * Constructor
	 */
	public StructToSorted() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( false, "any", Key.sortType, "text" ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "any", Key.localeSensitive, false ),
		    new Argument( false, "function:Comparator", Key.callback )
		};
	}

	/**
	 * Creates a new sorted struct containing the entries of the given struct, which is left unmodified. The sorting is driven either by a
	 * textual directive (sortType and sortOrder) or by a comparator function that receives two key names. If sortType is a function it is
	 * treated as the callback.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to sort.
	 *
	 * @argument.sortType The sort type: text (default), textNoCase or numeric. If a function is passed here it is used as the comparator callback.
	 *
	 * @argument.sortOrder The sort order, asc (default) or desc.
	 *
	 * @argument.localeSensitive Whether text sorting uses locale rules. Defaults to false.
	 *
	 * @argument.callback An optional comparator function (or Java Comparator) that receives two key names. Note that the default sortType of text
	 *                    takes precedence if the callback is not passed in the sortType position.
	 *
	 * @return A new sorted struct.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct target = arguments.getAsStruct( Key.struct );

		arguments.put( Key.type, target.isCaseSensitive() ? "ordered-casesensitive" : "ordered" );

		IStruct recipient = StructCaster.cast( structCreator.invoke( context, arguments ) );

		recipient.addAll( target.getWrapped() );

		return recipient;

	}

}
