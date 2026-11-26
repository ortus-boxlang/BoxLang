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

import java.util.Set;

import org.apache.commons.lang3.Strings;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.FunctionCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Find the index of a value in an array" )
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
		    new Argument( false, Argument.BOOLEAN, Key.substringMatch, false, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Searches an array for a value and returns the one-based index of the first match, or 0 if there is no match.
	 * Strings are compared case sensitively by <code>ArrayFind</code> and <code>find()</code>, and case insensitively by the <code>NoCase</code>
	 * variants.
	 * <p>
	 * The "contains" variants (<code>ArrayContains</code>, <code>ArrayContainsNoCase</code> and the <code>contains()</code> and
	 * <code>containsNoCase()</code>
	 * member functions) do the same search but return a boolean instead of an index.
	 * <p>
	 * If the value is a function, it is used as the search predicate and is invoked for each element (value, index) until it returns true:
	 * <pre>
	 * array = [ 1, 2, 3, 4, 5 ]
	 * index = array.find( ( value, index ) -> value == 3 )
	 * </pre>
	 * We recommend BoxLang lambdas (<code>-&gt;</code>) for this purpose so the function only acts upon the value and index without side
	 * effects.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to be searched.
	 *
	 * @argument.value The value to find, or a function to be used as the search predicate.
	 *
	 * @argument.substringMatch If true, a string value matches any element containing it as a substring. Defaults to false. Only works on simple
	 *                          values; for complex values use a function.
	 *
	 * @return The one-based index of the first match or 0 if not found. The contains variants return true or false instead.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Which function are we calling
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Array	actualArray		= arguments.getAsArray( Key.array );
		Object	value			= arguments.get( Key.value );
		Boolean	substringMatch	= arguments.getAsBoolean( Key.substringMatch );

		// Go search by function or by value
		int		indexFound		= FunctionCaster.attempt( value, "Predicate" )
		    // Search by function
		    .map( targetFunction -> actualArray.findIndex( targetFunction, context ) )
		    // Search by value or by substring
		    .orElseGet( () -> substringMatch
		        ? actualArray.findIndexWithSubstring( value, isCaseSensitive( bifMethodKey ) )
		        : actualArray.findIndex( value, isCaseSensitive( bifMethodKey ) )
		    );

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
		return Strings.CI.endsWith( functionName.getName(), "NoCase" ) ? false : true;
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
		return Strings.CI.contains( functionName.getName(), "contains" ) ? true : false;
	}

}
