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

import java.util.Map;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Get a system setting value" )
public class GetSystemSetting extends BIF {

	/**
	 * namespace.key
	 */
	public static char NAMESPACE_PREFIX_SEPARATOR = '.';

	/**
	 * Constructor
	 */
	public GetSystemSetting() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.key ),
		    new Argument( false, "Any", Key.defaultValue )
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
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String																	stringKey				= arguments.getAsString( Key.key );
		Key																		key						= Key.of( stringKey );
		Object																	defaultValue			= arguments.get( Key.defaultValue );

		// Get any registered system setting providers
		Map<Key, java.util.function.BiFunction<String, IBoxContext, Object>>	systemSettingProviders	= runtime.getConfiguration().systemSettingProviders;
		// Check for a : indicating a namespace
		int																		colonIndex				= stringKey.indexOf( NAMESPACE_PREFIX_SEPARATOR );
		java.util.function.BiFunction<String, IBoxContext, Object>				provider				= null;
		// If there is a provider for this namespace, give it a chance to resolve the setting
		if ( colonIndex > 0 && colonIndex < stringKey.length() - 1 ) {
			Key namespace = Key.of( stringKey.substring( 0, colonIndex ) );
			if ( ( provider = systemSettingProviders.get( namespace ) ) != null ) {
				String	namedspacedSettingName	= stringKey.substring( colonIndex + 1 );
				// I'm not passing the default value because I don't want override providers to return it, short circuting another provider
				Object	result					= provider.apply( namedspacedSettingName, context );
				// If we have a hit, return it here.
				if ( result != null ) {
					return result;
				}
			}
		}
		// check for an empty namespace provider, which sees all
		provider = null;
		if ( ( provider = systemSettingProviders.get( Key._EMPTY ) ) != null ) {
			// I'm not passing the default value because I don't want override providers to return it, short circuting another provider
			Object result = provider.apply( stringKey, context );
			// If we have a hit, return it here.
			if ( result != null ) {
				return result;
			}
		}

		// If there were no providers, or they all failed to find a setting, then do it ourselves.

		IStruct	properties	= context.computeAttachmentIfAbsent( Key.properties, attachmentKey -> new Struct( System.getProperties() ) );
		IStruct	env			= context.computeAttachmentIfAbsent( Key.environment, attachmentKey -> new Struct( System.getenv() ) );

		// Properties take precedence over environment variables
		String	value		= properties.getAsString( key );
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
