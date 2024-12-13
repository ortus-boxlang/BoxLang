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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class GetSystemSetting extends BIF {

	/**
	 * Constructor
	 */
	public GetSystemSetting() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.key ),
		    new Argument( false, "String", Key.defaultValue )
		};
	}

	/**
	 * Retrieve a Java System property or environment value by name.
	 * <p>
	 * It looks at properties first then environment variables second.
	 * <p>
	 * Please note that the property or environment variable name is case-sensitive.
	 * <p>
	 * You can also pass a default value to return if the property or environment variable is not found.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.key The name of the system property or environment variable to retrieve
	 *
	 * @argument.defaultValue The default value to return if the property or environment variable is not found
	 */
	public String _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		key				= Key.of( arguments.getAsString( Key.key ) );
		String	defaultValue	= arguments.getAsString( Key.defaultValue );

		IStruct	properties		= context.computeAttachmentIfAbsent( Key.properties, attachmentKey -> new Struct( System.getProperties() ) );
		IStruct	env				= context.computeAttachmentIfAbsent( Key.environment, attachmentKey -> new Struct( System.getenv() ) );

		// Properties take precedence over environment variables
		String	value			= properties.getAsString( key );
		if ( value != null ) {
			return value;
		}

		// If the property was not found, try to get the environment variable
		value = env.getAsString( key );
		if ( value != null ) {
			return value;
		}

		// If still null, return the default value if it was provided else throw an exception
		if ( defaultValue == null ) {
			throw new BoxRuntimeException( "System property or environment variable not found: " + key.getName() );
		}

		return defaultValue;
	}

}
