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
import ortus.boxlang.runtime.types.Function;

@BoxBIF
@BoxBIF( alias = "ArrayFindNoCase" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.ARRAY, name = "findNoCase" )
@BoxBIF( alias = "ArrayContains" )
@BoxBIF( alias = "ArrayContainsNoCase" )
@BoxMember( type = BoxLangType.ARRAY, name = "contains" )
@BoxMember( type = BoxLangType.ARRAY, name = "containsNoCase" )
public class ArrayFind extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFind() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, Argument.ANY, Key.value ),
		    new Argument( false, Argument.BOOLEAN, Key.substringMatch, false )
		};
	}

	/**
	 * Array finders and contains functions with and without case sensitivity.
	 * Please note that "contain" methods return a boolean, while "find" methods return an index.
	 * If you use a function as the value, it will be used as a search closure or lambda. The signature of the function should be:
	 *
	 * <pre>
	 *    ( value, index ) => {
	 * 	  	return true; // if the value is found, else false
	 *   }
	 * </pre>
	 *
	 * Example:
	 *
	 * <pre>
	 *   array = [ 1, 2, 3, 4, 5 ];
	 *  index = array.find( ( value, index ) -> {
	 * 		return value == 3;
	 * } );
	 * </pre>
	 *
	 * We recommend you use BoxLang lambdas ({@code ->}) for this purpose, so they only act upon the value and index without any side effects.
	 * They will be faster and more efficient.
	 *
	 * @function.arrayFind This function searches the array for the specified value. Returns the index in the array of the first match, or 0 if there is
	 *                     no match.
	 *
	 * @function.arrayFindNoCase This function searches the array for the specified value. Returns the index in the array of the first match, or 0 if
	 *                           there is no match. The search is case insensitive.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to be searched.
	 *
	 * @argument.value The value to find or a closure to be used as a search function.
	 *
	 * @argument.substringMatch If true, the search will be a substring match. Default is false. This only works on simple values, not complex ones. For
	 *                          that just use a function filter.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Which function are we calling
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Array	actualArray		= arguments.getAsArray( Key.array );
		Object	value			= arguments.get( Key.value );
		Boolean	substringMatch	= arguments.getAsBoolean( Key.substringMatch );

		// This case might exist. If it does, we need to set it to false
		if ( substringMatch == null ) {
			substringMatch = false;
		}

		// Go search by function or by value
		int indexFound = value instanceof Function castedValueFunction
		    // Search by function
		    ? actualArray.findIndex( castedValueFunction, context )
		    // Search by value or by substring
		    : ( substringMatch ? actualArray.findIndexWithSubstring( value, isCaseSensitive( bifMethodKey ) )
		        : actualArray.findIndex( value, isCaseSensitive( bifMethodKey ) ) );

		// If the function is a boolean return function, return a boolean
		return isBooleanReturn( bifMethodKey ) ? indexFound > 0 : indexFound;
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

	/**
	 * Check if the function is a boolean return function
	 *
	 * @param functionName The function name
	 *
	 * @return True if the function returns boolean or not
	 */
	private boolean isBooleanReturn( Key functionName ) {
		// Check if the functionName ends with "noCase" with no case sensitivity
		return StringUtils.containsIgnoreCase( functionName.getNameNoCase(), "contains" ) ? true : false;
	}

}
