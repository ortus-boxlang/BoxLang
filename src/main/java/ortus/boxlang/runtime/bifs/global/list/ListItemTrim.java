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
package ortus.boxlang.runtime.bifs.global.list;

import java.util.Arrays;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DelimitedArray;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Trim whitespace from all items in a list" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listItemTrim" )
public class ListItemTrim extends BIF {

	/**
	 * Constructor
	 */
	public ListItemTrim() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument(
		        false,
		        "string",
		        Key.delimiter,
		        ListUtil.DEFAULT_DELIMITER
		    ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false )
		};
	}

	/**
	 * Trims leading and trailing whitespace from every element of a list and returns the result as a string. Existing delimiters are
	 *     preserved. Empty elements are removed unless includeEmptyFields is true.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list whose elements are trimmed.
	 *
	 * @argument.delimiter The list delimiter. Each character of the string is treated as a separate delimiter unless
	 *     multiCharacterDelimiter is true. Defaults to a comma.
	 *
	 * @argument.includeEmptyFields Whether empty elements (for example those created by consecutive delimiters) are kept. Defaults
	 *     to false.
	 *
	 * @argument.multiCharacterDelimiter When true, the delimiter is treated as one multi-character separator instead of a set of
	 *     single-character delimiters. Defaults to false.
	 *
	 * @return The list with each element trimmed.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String delimiter = arguments.getAsString( Key.delimiter );

		return Arrays.stream(
		    ListUtil
		        .asDelimitedList(
		            arguments.getAsString( Key.list ),
		            delimiter,
		            arguments.getAsBoolean( Key.includeEmptyFields ),
		            arguments.getAsBoolean( Key.multiCharacterDelimiter )
		        )
		        .toElementDelimiterPairs() )
		    .map( pair -> new DelimitedArray.ElementDelimiterPair( ( ( String ) pair.element() ).trim(), pair.delimiter() ) )
		    .collect( BLCollector.toArray( DelimitedArray.class ) )
		    .asString();
	}
}
