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
package ortus.boxlang.runtime.bifs.global.decision;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.QueryCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

@BoxBIF
@BoxBIF( alias = "structIsEmpty" )
@BoxBIF( alias = "arrayIsEmpty" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.STRUCT )
@BoxMember( type = BoxLangType.STRING_STRICT )
@BoxMember( type = BoxLangType.QUERY )
public class IsEmpty extends BIF {

	/**
	 * Constructor
	 */
	public IsEmpty() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value ),
		};
	}

	/**
	 * Determine whether a given value is empty. We check for emptiness of
	 * anything that can be casted to: Array, Struct, Query, or String.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value The value/object to check for emptiness.
	 *
	 * @return True if the value is empty, false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object object = arguments.get( Key.value );
		if ( object == null ) {
			return true;
		}
		CastAttempt<Array> arrayAttempt = ArrayCaster.attempt( object );
		if ( arrayAttempt.wasSuccessful() ) {
			return arrayAttempt.get().isEmpty();
		}
		CastAttempt<IStruct> structAttempt = StructCaster.attempt( object );
		if ( structAttempt.wasSuccessful() ) {
			return structAttempt.get().isEmpty();
		}
		CastAttempt<Query> queryAttempt = QueryCaster.attempt( object );
		if ( queryAttempt.wasSuccessful() ) {
			return queryAttempt.get().isEmpty();
		}
		CastAttempt<String> stringAttempt = StringCaster.attempt( object );
		if ( stringAttempt.wasSuccessful() ) {
			return stringAttempt.get().isEmpty();
		}
		return false;
	}

}
