
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

import java.util.function.Predicate;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxBIF( alias = "ListValueCountNoCase" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ListValueCount" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ListValueCountNoCase" )

public class ListValueCount extends BIF {

	private static final Key bifMethodNoCase = Key.of( "ListValueCountNoCase" );

	/**
	 * Constructor
	 */
	public ListValueCount() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "string", Key.value ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false )
		};
	}

	/**
	 * returns a count of the number of occurrences of a value in a list
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to be searched.
	 *
	 * @argument.value The value to locale
	 *
	 * @argument.delimiter The list delimiter(s)
	 *
	 * @argument.includeEmptyFields Whether to include empty fields in the search
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key					bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		String				comparator		= bifMethodKey.equals( bifMethodNoCase )
		    ? arguments.getAsString( Key.value ).toLowerCase()
		    : arguments.getAsString( Key.value );

		Predicate<Object>	test			= item -> bifMethodKey.equals( bifMethodNoCase )
		    ? StringCaster.cast( item ).toLowerCase().equals( comparator )
		    : StringCaster.cast( item ).equals( comparator );

		return ListUtil.asList(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    true
		).stream().filter( test ).count();
	}

}
