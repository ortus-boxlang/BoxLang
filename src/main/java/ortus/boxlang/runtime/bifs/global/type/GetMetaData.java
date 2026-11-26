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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IType;

@BoxBIF( description = "Get metadata about a function or component" )
public class GetMetaData extends BIF {

	/**
	 * Constructor
	 */
	public GetMetaData() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.value )
		};
	}

	/**
	 * Gets metadata associated with a value. For BoxLang types which carry metadata (classes, functions, queries, structs and so on) this
	 * returns the <code>$bx.meta</code> struct of the value, for example the name, parameters and annotations of a function.
	 * For any other value, such as a string or number, the Java {@link Class} of the value is returned. Java objects wrapped in a
	 * DynamicObject are unwrapped first.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value The object to get metadata for.
	 *
	 * @return A metadata struct, or the Java class of the value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object value = arguments.get( Key.value );

		// DynamicObject instances need to be unwrapped to get the metadata
		value = DynamicObject.unWrap( value );

		// Functions have a legacy metadata view that matches CF engines
		if ( value instanceof IType bxObject ) {
			return bxObject.getBoxMeta().getMeta();
		}

		// All other types return the class of the value to match CF engines
		if ( value instanceof Class ) {
			return value;
		}

		return value.getClass();
	}

}
