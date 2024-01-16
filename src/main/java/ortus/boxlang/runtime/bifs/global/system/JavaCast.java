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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class JavaCast extends BIF {

	/**
	 * Constructor
	 */
	public JavaCast() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.type ),
		    new Argument( true, "any", Key.variable )
		};
	}

	/**
	 * Cast a variable to a specified Java type
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The name of a Java primitive or a Java class name.
	 * 
	 * @argument.variable The variable, Java object, or array to cast.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	type		= arguments.getAsString( Key.type );
		Object	variable	= arguments.get( Key.variable );

		if ( variable == null ) {
			return null;
		}

		switch ( type.toLowerCase() ) {
			case "boolean" :
				return GenericCaster.cast( variable, "boolean" );
			case "double" :
				return GenericCaster.cast( variable, "double" );
			case "float" :
				return GenericCaster.cast( variable, "float" );
			case "int" :
				return GenericCaster.cast( variable, "int" );
			case "long" :
				return GenericCaster.cast( variable, "long" );
			case "string" :
				return GenericCaster.cast( variable, "string" );
			case "null" :
				return GenericCaster.cast( variable, "null" );
			case "byte" :
				return GenericCaster.cast( variable, "byte" );
			case "bigdecimal" :
				return GenericCaster.cast( variable, "bigdecimal" );
			case "char" :
				return GenericCaster.cast( variable, "char" );
			case "short" :
				return GenericCaster.cast( variable, "short" );
			default :
				throw new BoxRuntimeException( "Unsupported Java cast type: " + type );
		}
	}
}
