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
package ortus.boxlang.runtime.bifs.global.array;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
@BoxBIF( alias = "ArrayDeleteNoCase" )
@BoxMember( type = BoxLangType.ARRAY, name = "deleteNoCase" )
public class ArrayDelete extends BIF {

	public static final Key	scopeOne	= Key.of( "one" );
	public static final Key	scopeAll	= Key.of( "all" );

	/**
	 * Constructor
	 */
	public ArrayDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiableArray", Key.array ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "string", Key.scope, "one" )
		};
	}

	/**
	 * Delete first occurance of item in array case sensitive
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to be deleted from.
	 *
	 * @argument.value The value to deleted.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray		= arguments.getAsArray( Key.array );
		Object	value			= arguments.get( Key.value );
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Key		scopeKey		= Key.of( arguments.getAsString( Key.scope ) );

		boolean	isCaseSensitive	= isCaseSensitive( bifMethodKey );

		while ( actualArray.findIndex( value, isCaseSensitive ) > 0 ) {
			int index = actualArray.findIndex( value, isCaseSensitive );
			if ( index > 0 ) {
				actualArray.remove( index - 1 );
			}

			if ( scopeKey.equals( scopeOne ) ) {
				break;
			}
		}

		return actualArray;
	}

	/**
	 * Check if the function is case sensitive by checking if the function name ends with "NoCase"
	 *
	 * @param functionName The function name
	 *
	 * @return True if the function is case sensitive, false otherwise
	 */
	private boolean isCaseSensitive( Key functionName ) {
		// Check if the functionName ends with "noCase" with no case sensitivity
		return StringUtils.endsWithIgnoreCase( functionName.getNameNoCase(), "NoCase" ) ? false : true;
	}

}
