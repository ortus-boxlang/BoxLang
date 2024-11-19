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
package ortus.boxlang.runtime.bifs.global.type;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF // Len()
@BoxBIF( alias = "StructCount" )
@BoxBIF( alias = "ArrayLen" )
@BoxBIF( alias = "StringLen" )
@BoxBIF( alias = "QueryRecordCount" )
@BoxMember( type = BoxLangType.STRING )
@BoxMember( type = BoxLangType.STRUCT, name = "count" )
@BoxMember( type = BoxLangType.STRUCT, name = "len" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.QUERY )
@BoxMember( type = BoxLangType.DATETIME, name = "len" )
@BoxMember( type = BoxLangType.DATE, name = "len" )
public class Len extends BIF {

	/**
	 * Constructor
	 */
	public Len() {
		super();
		declaredArguments = new Argument[] {
		    // Basically add a second param and check both. This really only matters
		    // if someone calls the BIF with named args, which is quite rare.
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Returns the absolute value of a number
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value The number to return the absolute value of
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	object	= arguments.get( Key.value );

		// Check if there is an argument called "struct", if so, use that
		Object	struct	= arguments.get( "struct" );
		if ( struct != null ) {
			object = struct;
		}

		// Check if there is an argument called "query", if so, use that
		Object query = arguments.get( "query" );
		if ( query != null ) {
			object = query;
		}

		// Check if there is an argument called "array", if so, use that
		Object array = arguments.get( "array" );
		if ( array != null ) {
			object = array;
		}

		// Check if there is an argument called "string", if so, use that
		Object string = arguments.get( "string" );
		if ( string != null ) {
			object = string;
		}

		if ( object == null ) {
			return 0;
		}

		if ( object instanceof Query q ) {
			return q.size();
		}

		// Dates are all handled inside the string caster, since they only have a "length" as a string
		CastAttempt<String> stringAttempt = StringCaster.attempt( object );
		if ( stringAttempt.wasSuccessful() ) {
			return stringAttempt.get().length();
		}

		CastAttempt<Array> arrayAttempt = ArrayCaster.attempt( object );
		if ( arrayAttempt.wasSuccessful() ) {
			return arrayAttempt.get().size();
		}

		CastAttempt<IStruct> structAttempt = StructCaster.attempt( object );
		if ( structAttempt.wasSuccessful() ) {
			return structAttempt.get().size();
		}

		throw new BoxRuntimeException( "Cannot determine length of object of type " + object.getClass().getName() );
	}

}
