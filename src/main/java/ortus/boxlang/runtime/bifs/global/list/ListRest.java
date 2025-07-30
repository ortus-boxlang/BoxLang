
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ListRest" )

public class ListRest extends BIF {

	/**
	 * Constructor
	 */
	public ListRest() {
		super();
		declaredArguments = new Argument[] {
				new Argument(true, "string", Key.list),
				new Argument(false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER),
				new Argument(false, "boolean", Key.includeEmptyFields, false),
				new Argument(false, "boolean", Key.multiCharacterDelimiter, false),
				new Argument(false, "integer", Key.offset, 0)
		};
	}

	/**
	 * Returns the remainder of a list after removing the first item
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The delimited list to perform operations on
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the
	 *                              returned result
	 *
	 * @argument.multiCharacterDelimiter boolean whether the delimiter is
	 *                                   multi-character
	 */
	public Object _invoke(IBoxContext context, ArgumentsScope arguments) {
		String list = arguments.getAsString(Key.list);
		String delimiter = arguments.getAsString(Key.delimiter);
		Boolean includeEmptyFields = arguments.getAsBoolean(Key.includeEmptyFields);
		Boolean multiCharacterDelimiter = arguments.getAsBoolean(Key.multiCharacterDelimiter);
		Integer offset = arguments.getAsInteger(Key.offset);

		Array ref = ListUtil.asList(list, delimiter, includeEmptyFields, multiCharacterDelimiter);
		if (ref.size() >= 1) {
			ref.remove(0 + offset);
		}

		// When multiCharacterDelimiter is false and delimiter has multiple chars,
		// we need to figure out which actual delimiter to use for reconstruction
		String reconstructionDelimiter = delimiter;
		if (!multiCharacterDelimiter && delimiter.length() > 1) {
			// Find the first delimiter character that actually exists in the original
			// string
			for (int i = 0; i < delimiter.length(); i++) {
				String singleDelim = String.valueOf(delimiter.charAt(i));
				if (list.contains(singleDelim)) {
					reconstructionDelimiter = singleDelim;
					break;
				}
			}
		}

		return ListUtil.asString(ref, reconstructionDelimiter);
	}

}
