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
package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IType;

@BoxBIF
@BoxMember( type = BoxLangType.XML )
@BoxMember( type = BoxLangType.STRING )
public class ToString extends BIF {

	/**
	 * Constructor
	 */
	public ToString() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "string", Key.encoding )
		};
	}

	/**
	 * Converts a value to a string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value Value to convert to a string; can be a simple value such as an integer, a binary object, or an XML document object.
	 * 
	 * @argument.encoding The character encoding (character set) of the string, used with binary data.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object obj = arguments.get( Key.value );
		// String caster will not cast null
		if ( obj == null ) {
			return "";
		}
		CastAttempt<String> castAttempt = StringCaster.attempt( obj, arguments.getAsString( Key.encoding ) );
		if ( castAttempt.wasSuccessful() ) {
			return castAttempt.get();
		}

		obj = DynamicObject.unWrap( obj );

		// Not done yet. The toString() BIF tries a bit harder than the string caster.
		// For known BoxTypes, we can call the asString() method and let them create a string represenation of themselves
		if ( obj instanceof IType type ) {
			return type.asString();
		}

		// We are never returning an error, which prolly isn't CF-compat, but it feels more useful so the BIF does what it says "on the tin".
		return obj.toString();

	}
}
