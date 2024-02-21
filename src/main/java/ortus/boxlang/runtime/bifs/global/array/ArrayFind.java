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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxBIF( alias = "ArrayFindNoCase" )
@BoxBIF( alias = "ArrayContains" )
@BoxBIF( alias = "ArrayContainsNoCase" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.ARRAY, name = "findNoCase" )
@BoxMember( type = BoxLangType.ARRAY, name = "contains" )
@BoxMember( type = BoxLangType.ARRAY, name = "containsNoCase" )
public class ArrayFind extends BIF {

	private static final Array	caseSensitiveFunctions	= new Array(
	    new Object[] {
	        Key.find,
	        Key.arrayFind,
	        Key.of( "arrayContains" ),
	        Key.of( "contains" ),
	        Key.of( "listFind" ),
	        Key.of( "listContains" )
	    }
	);

	private static final Array	simpleValueFunctions	= new Array(
	    new Object[] {
	        Key.of( "arrayContains" ),
	        Key.of( "arrayContainsNoCase" ),
	        Key.of( "contains" ),
	        Key.of( "listContains" ),
	        Key.of( "listContainsNoCase" )
	    }
	);

	/**
	 * Constructor
	 */
	public ArrayFind() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Return int position of value in array, case sensitive
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to be searched.
	 *
	 * @argument.value The value to found.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key bifMethodKey = arguments.getAsKey( BIF.__functionName );
		if ( isSimpleValueFunction( bifMethodKey ) && arguments.get( Key.value ) instanceof Function ) {
			throw new BoxRuntimeException(
			    String.format(
			        "Closures are not a valid search value argument for the function [%s]",
			        bifMethodKey.getName()
			    )
			);
		}
		Array	actualArray	= arguments.getAsArray( Key.array );
		Object	value		= arguments.get( Key.value );

		return value instanceof Function
		    ? actualArray.findIndex( ( Function ) value, context )
		    : actualArray.findIndex( value, isCaseSensitive( bifMethodKey ) );
	}

	private boolean isCaseSensitive( Key functionName ) {
		return caseSensitiveFunctions.stream().filter( fn -> functionName.equals( fn ) ).findFirst().orElse( null ) != null;
	}

	private boolean isSimpleValueFunction( Key functionName ) {
		return simpleValueFunctions.stream().filter( fn -> functionName.equals( fn ) ).findFirst().orElse( null ) != null;
	}

}
